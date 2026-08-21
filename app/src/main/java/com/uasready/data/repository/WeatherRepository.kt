package com.uasready.data.repository

import com.uasready.domain.model.HourlyForecastInterval
import com.uasready.domain.model.PrecipitationType
import com.uasready.domain.model.WeatherForecast
import com.uasready.domain.model.WeatherObservation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface WeatherRepository {
    suspend fun getWeatherData(latitude: Double, longitude: Double): Result<Pair<WeatherObservation, WeatherForecast>>
}

class LiveWeatherRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : WeatherRepository {

    override suspend fun getWeatherData(
        latitude: Double,
        longitude: Double
    ): Result<Pair<WeatherObservation, WeatherForecast>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m,visibility" +
                    "&hourly=temperature_2m,precipitation_probability,precipitation,weather_code,visibility,wind_speed_10m,wind_gusts_10m,wind_direction_10m" +
                    "&wind_speed_unit=mph&precipitation_unit=inch&temperature_unit=fahrenheit&forecast_days=2"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "UASReady-Android-App/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Weather API HTTP Error: ${response.code}"))
            }

            val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty weather response"))
            val json = JSONObject(bodyString)

            val currentJson = json.getJSONObject("current")
            val tempF = currentJson.getDouble("temperature_2m")
            val apparentTempF = currentJson.getDouble("apparent_temperature")
            val windSpeedMph = currentJson.getDouble("wind_speed_10m")
            val windGustMph = currentJson.optDouble("wind_gusts_10m", windSpeedMph * 1.3)
            val windDir = currentJson.getInt("wind_direction_10m")
            val humidity = currentJson.getInt("relative_humidity_2m")
            val precipIn = currentJson.getDouble("precipitation")
            val weatherCode = currentJson.getInt("weather_code")
            val rawVisibilityMeters = currentJson.optDouble("visibility", 16000.0)
            val visibilitySM = (rawVisibilityMeters / 1609.34).coerceAtMost(10.0)
            val pressureHpa = currentJson.getDouble("surface_pressure")
            val pressureInHg = pressureHpa * 0.02953

            val precipType = mapWeatherCodeToPrecipType(weatherCode)
            val conditionsDesc = mapWeatherCodeToDescription(weatherCode)
            val tstormProb = if (precipType == PrecipitationType.THUNDERSTORM) 85 else 0

            val now = System.currentTimeMillis()
            val observation = WeatherObservation(
                temperatureF = tempF,
                apparentTemperatureF = apparentTempF,
                windSpeedMph = windSpeedMph,
                windGustMph = windGustMph,
                windDirectionDegrees = windDir,
                visibilityStatuteMiles = visibilitySM,
                cloudCoverPercent = if (weatherCode > 2) 75 else 20,
                cloudCeilingFt = if (weatherCode in 51..99) 1200.0 else null,
                precipitationProbabilityPercent = if (precipIn > 0) 90 else 5,
                precipitationRateInchesPerHour = precipIn,
                precipitationType = precipType,
                relativeHumidityPercent = humidity,
                pressureInHg = pressureInHg,
                thunderstormProbabilityPercent = tstormProb,
                conditionsDescription = conditionsDesc,
                timestampEpochMs = now,
                sourceName = "Open-Meteo / NOAA Live Telemetry",
                isStale = false
            )

            // Parse hourly forecast
            val hourlyJson = json.getJSONObject("hourly")
            val timesArray = hourlyJson.getJSONArray("time")
            val tempsArray = hourlyJson.getJSONArray("temperature_2m")
            val windSpeedsArray = hourlyJson.getJSONArray("wind_speed_10m")
            val windGustsArray = hourlyJson.getJSONArray("wind_gusts_10m")
            val windDirsArray = hourlyJson.getJSONArray("wind_direction_10m")
            val precipProbsArray = hourlyJson.getJSONArray("precipitation_probability")
            val precipRatesArray = hourlyJson.getJSONArray("precipitation")
            val weatherCodesArray = hourlyJson.getJSONArray("weather_code")
            val visibilitiesArray = hourlyJson.optJSONArray("visibility")

            val forecastIntervals = mutableListOf<HourlyForecastInterval>()
            val count = Math.min(timesArray.length(), 24) // next 24 hours
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")

            for (i in 0 until count) {
                val timeStr = timesArray.getString(i)
                val timeEpoch = try {
                    sdf.parse(timeStr)?.time ?: (now + i * 3600 * 1000L)
                } catch (e: Exception) {
                    now + i * 3600 * 1000L
                }

                val hCode = weatherCodesArray.getInt(i)
                val hPrecipType = mapWeatherCodeToPrecipType(hCode)
                val hVisMeters = visibilitiesArray?.optDouble(i, 16000.0) ?: 16000.0
                val hVisSM = (hVisMeters / 1609.34).coerceAtMost(10.0)
                val hPrecipProb = precipProbsArray.getInt(i)

                forecastIntervals.add(
                    HourlyForecastInterval(
                        timestampEpochMs = timeEpoch,
                        temperatureF = tempsArray.getDouble(i),
                        windSpeedMph = windSpeedsArray.getDouble(i),
                        windGustMph = windGustsArray.getDouble(i),
                        windDirectionDegrees = windDirsArray.getInt(i),
                        visibilityStatuteMiles = hVisSM,
                        cloudCeilingFt = if (hCode in 51..99) 1500.0 else null,
                        precipitationProbabilityPercent = hPrecipProb,
                        precipitationRateInchesPerHour = precipRatesArray.getDouble(i),
                        precipitationType = hPrecipType,
                        thunderstormProbabilityPercent = if (hPrecipType == PrecipitationType.THUNDERSTORM) 80 else (hPrecipProb / 3),
                        conditionsDescription = mapWeatherCodeToDescription(hCode)
                    )
                )
            }

            val forecast = WeatherForecast(
                intervals = forecastIntervals,
                generatedAtEpochMs = now,
                sourceName = "Open-Meteo / NOAA 24-Hour Forecast"
            )

            Result.success(Pair(observation, forecast))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapWeatherCodeToPrecipType(code: Int): PrecipitationType {
        return when (code) {
            0, 1, 2, 3 -> PrecipitationType.NONE
            51, 53, 55, 56, 57 -> PrecipitationType.DRIZZLE
            61, 63, 65, 80, 81, 82 -> PrecipitationType.RAIN
            66, 67 -> PrecipitationType.FREEZING_RAIN
            71, 73, 75, 77, 85, 86 -> PrecipitationType.SNOW
            95, 96, 99 -> PrecipitationType.THUNDERSTORM
            else -> PrecipitationType.NONE
        }
    }

    private fun mapWeatherCodeToDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63 -> "Moderate Rain"
            65 -> "Heavy Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snow Fall"
            80, 81, 82 -> "Rain Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Severe Thunderstorm with Hail"
            else -> "Fair"
        }
    }
}

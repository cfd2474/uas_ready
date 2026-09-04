package com.taksolutions.uasready.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PrecipitationType {
    NONE,
    RAIN,
    DRIZZLE,
    SNOW,
    FREEZING_RAIN,
    SLEET,
    HAIL,
    THUNDERSTORM
}

@Serializable
data class WeatherObservation(
    val temperatureF: Double,
    val apparentTemperatureF: Double,
    val windSpeedMph: Double,
    val windGustMph: Double,
    val windDirectionDegrees: Int,
    val visibilityStatuteMiles: Double,
    val cloudCoverPercent: Int,
    val cloudCeilingFt: Double?, // null if clear sky or unlimited
    val precipitationProbabilityPercent: Int,
    val precipitationRateInchesPerHour: Double,
    val precipitationType: PrecipitationType = PrecipitationType.NONE,
    val relativeHumidityPercent: Int,
    val pressureInHg: Double,
    val thunderstormProbabilityPercent: Int = 0,
    val conditionsDescription: String,
    val timestampEpochMs: Long,
    val sourceName: String = "NOAA/NWS Live",
    val isStale: Boolean = false
)

@Serializable
data class HourlyForecastInterval(
    val timestampEpochMs: Long,
    val temperatureF: Double,
    val windSpeedMph: Double,
    val windGustMph: Double,
    val windDirectionDegrees: Int,
    val visibilityStatuteMiles: Double,
    val cloudCeilingFt: Double?,
    val precipitationProbabilityPercent: Int,
    val precipitationRateInchesPerHour: Double,
    val precipitationType: PrecipitationType = PrecipitationType.NONE,
    val thunderstormProbabilityPercent: Int = 0,
    val conditionsDescription: String
)

@Serializable
data class WeatherForecast(
    val intervals: List<HourlyForecastInterval>,
    val generatedAtEpochMs: Long,
    val sourceName: String = "NOAA/NWS Hourly Forecast"
)

package com.uasready.data.repository

import com.uasready.domain.model.GeomagneticStormScale
import com.uasready.domain.model.GnssRiskLevel
import com.uasready.domain.model.SpaceWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

interface SpaceWeatherRepository {
    suspend fun getSpaceWeather(): Result<SpaceWeather>
}

class LiveSpaceWeatherRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : SpaceWeatherRepository {

    override suspend fun getSpaceWeather(): Result<SpaceWeather> = withContext(Dispatchers.IO) {
        try {
            val url = "https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "UASReady-Android-App/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("NOAA SWPC HTTP Error: ${response.code}"))
            }

            val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty NOAA SWPC response"))
            val jsonArray = JSONArray(bodyString)

            // Last row contains the latest planetary Kp observation
            if (jsonArray.length() <= 1) {
                return@withContext Result.failure(Exception("Invalid NOAA Kp data format"))
            }

            val latestRow = jsonArray.getJSONArray(jsonArray.length() - 1)
            val currentKp = latestRow.getDouble(1)

            // Check previous rows to get recent trend/max
            var maxRecentKp = currentKp
            val startIndex = Math.max(1, jsonArray.length() - 8)
            for (i in startIndex until jsonArray.length()) {
                val row = jsonArray.getJSONArray(i)
                val kp = row.getDouble(1)
                if (kp > maxRecentKp) {
                    maxRecentKp = kp
                }
            }

            val stormScale = SpaceWeather.calculateStormScale(currentKp)
            val gnssRisk = SpaceWeather.calculateGnssRisk(currentKp)

            val spaceWeather = SpaceWeather(
                currentKpIndex = currentKp,
                forecastMaxKpIndex = maxRecentKp,
                geomagneticStormScale = stormScale,
                gnssRiskLevel = gnssRisk,
                activeAlerts = if (stormScale != GeomagneticStormScale.NONE) {
                    listOf("NOAA SWPC Geomagnetic Storm Watch: ${stormScale.name}")
                } else {
                    emptyList()
                },
                timestampEpochMs = System.currentTimeMillis(),
                sourceName = "NOAA Space Weather Prediction Center (SWPC)",
                isStale = false
            )

            Result.success(spaceWeather)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

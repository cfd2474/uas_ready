package com.taksolutions.uasready.data.repository

import android.util.Log
import com.taksolutions.uasready.domain.model.GeomagneticStormScale
import com.taksolutions.uasready.domain.model.GnssRiskLevel
import com.taksolutions.uasready.domain.model.SpaceWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface SpaceWeatherRepository {
    suspend fun getSpaceWeather(): Result<SpaceWeather>
}

class LiveSpaceWeatherRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) : SpaceWeatherRepository {

    companion object {
        private const val TAG = "SpaceWeatherRepo"
        private const val PRIMARY_URL = "https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json"
        private const val FALLBACK_URL = "https://services.swpc.noaa.gov/json/planetary_k_index_1m.json"
    }

    override suspend fun getSpaceWeather(): Result<SpaceWeather> = withContext(Dispatchers.IO) {
        // Try Primary SWPC K-Index endpoint
        val primaryResult = fetchFromSwpcUrl(PRIMARY_URL)
        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }

        Log.w(TAG, "Primary SWPC feed failed (${primaryResult.exceptionOrNull()?.message}), trying fallback...")
        val fallbackResult = fetchFromSwpcUrl(FALLBACK_URL)
        if (fallbackResult.isSuccess) {
            return@withContext fallbackResult
        }

        // Return the failure from the primary attempt
        Log.e(TAG, "All NOAA SWPC endpoints failed", fallbackResult.exceptionOrNull())
        primaryResult
    }

    private fun fetchFromSwpcUrl(url: String): Result<SpaceWeather> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "UASReady-Android-App/1.0 (Public Safety sUAS)")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("NOAA SWPC HTTP Error: ${response.code} from $url"))
            }

            val bodyString = response.body?.string()
                ?: return Result.failure(Exception("Empty NOAA SWPC response from $url"))

            val jsonArray = JSONArray(bodyString)
            if (jsonArray.length() == 0) {
                return Result.failure(Exception("Empty JSON array from NOAA SWPC"))
            }

            val parsedKps = mutableListOf<Double>()

            for (i in 0 until jsonArray.length()) {
                val element = jsonArray.get(i)
                when (element) {
                    is JSONObject -> {
                        // Keys can be "Kp", "kp", or "kp_index"
                        val kp = when {
                            element.has("Kp") -> element.optDouble("Kp", Double.NaN)
                            element.has("kp") -> element.optDouble("kp", Double.NaN)
                            element.has("kp_index") -> element.optDouble("kp_index", Double.NaN)
                            else -> Double.NaN
                        }
                        if (!kp.isNaN()) {
                            parsedKps.add(kp)
                        }
                    }
                    is JSONArray -> {
                        // Skip header row if first element is string "Kp"
                        if (element.length() >= 2) {
                            val kp = element.optDouble(1, Double.NaN)
                            if (!kp.isNaN()) {
                                parsedKps.add(kp)
                            }
                        }
                    }
                }
            }

            if (parsedKps.isEmpty()) {
                return Result.failure(Exception("No valid Kp values could be parsed from NOAA response ($url)"))
            }

            val currentKp = parsedKps.last()
            val recentWindow = parsedKps.takeLast(8)
            val maxRecentKp = recentWindow.maxOrNull() ?: currentKp

            val stormScale = SpaceWeather.calculateStormScale(currentKp)
            val gnssRisk = SpaceWeather.calculateGnssRisk(currentKp)

            val spaceWeather = SpaceWeather(
                currentKpIndex = currentKp,
                forecastMaxKpIndex = maxRecentKp,
                geomagneticStormScale = stormScale,
                gnssRiskLevel = gnssRisk,
                activeAlerts = if (stormScale != GeomagneticStormScale.NONE) {
                    listOf("NOAA SWPC Geomagnetic Storm Alert: ${stormScale.name} (Kp ${String.format("%.1f", currentKp)})")
                } else {
                    emptyList()
                },
                timestampEpochMs = System.currentTimeMillis(),
                sourceName = "NOAA Space Weather Prediction Center (SWPC)",
                isStale = false
            )

            Log.i(TAG, "Successfully parsed NOAA SWPC Kp: current=$currentKp, maxRecent=$maxRecentKp, storm=${stormScale.name}, gnssRisk=${gnssRisk.name}")
            Result.success(spaceWeather)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching/parsing NOAA SWPC from $url: ${e.message}", e)
            Result.failure(e)
        }
    }
}


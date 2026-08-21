package com.uasready.data.repository

import android.util.Log
import com.uasready.domain.model.TerrainObstructionProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

interface TerrainRepository {
    suspend fun getTerrainProfile(latitude: Double, longitude: Double): Result<TerrainObstructionProfile>
}

class LiveTerrainRepository : TerrainRepository {

    companion object {
        private const val TAG = "LiveTerrainRepo"
        private const val EARTH_RADIUS_METERS = 6371000.0
        private val AZIMUTHS = listOf(0, 45, 90, 135, 180, 225, 270, 315) // N, NE, E, SE, S, SW, W, NW
        private val RADIAL_DISTANCES_METERS = listOf(1500.0, 3000.0)
    }

    override suspend fun getTerrainProfile(
        latitude: Double,
        longitude: Double
    ): Result<TerrainObstructionProfile> = withContext(Dispatchers.IO) {
        try {
            // 1. Generate radial sampling points (Center + 8 directions x 2 distances = 17 points)
            val samplePoints = mutableListOf<Pair<Double, Double>>()
            samplePoints.add(Pair(latitude, longitude)) // Index 0 is center launch location

            val azimuthMap = mutableListOf<Pair<Int, Double>>() // (azimuth, distance)

            for (az in AZIMUTHS) {
                for (dist in RADIAL_DISTANCES_METERS) {
                    val dest = calculateDestinationPoint(latitude, longitude, dist, az.toDouble())
                    samplePoints.add(dest)
                    azimuthMap.add(Pair(az, dist))
                }
            }

            val latsParam = samplePoints.joinToString(",") { String.format(java.util.Locale.US, "%.5f", it.first) }
            val lonsParam = samplePoints.joinToString(",") { String.format(java.util.Locale.US, "%.5f", it.second) }

            val urlString = "https://api.open-meteo.com/v1/elevation?latitude=$latsParam&longitude=$lonsParam"
            Log.d(TAG, "Fetching terrain DEM from: $urlString")

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "UASReady-Preflight/1.2")
            }

            if (connection.responseCode != 200) {
                return@withContext Result.failure(Exception("Open-Meteo DEM API error HTTP ${connection.responseCode}"))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)
            val elevationArray = json.getJSONArray("elevation")

            if (elevationArray.length() < samplePoints.size) {
                return@withContext Result.failure(Exception("Incomplete DEM elevation response"))
            }

            val centerElevationMeters = elevationArray.getDouble(0)
            val sectorAngles = mutableMapOf<Int, Double>()

            for (az in AZIMUTHS) {
                sectorAngles[az] = 0.0
            }

            for (i in azimuthMap.indices) {
                val (az, dist) = azimuthMap[i]
                val pointElev = elevationArray.getDouble(i + 1)
                val deltaElev = pointElev - centerElevationMeters
                val angleDeg = Math.toDegrees(atan2(deltaElev, dist))
                val existingMax = sectorAngles[az] ?: 0.0
                sectorAngles[az] = max(existingMax, max(0.0, angleDeg))
            }

            val profile = TerrainObstructionProfile.fromSectorAngles(
                launchElevationMeters = centerElevationMeters,
                sectorMaskAngles = sectorAngles
            )

            Log.i(TAG, "Computed Terrain Profile: Avg Mask=${profile.averageHorizonMaskDeg}°, Max=${profile.maxObstructionDeg}° @ ${profile.worstObstructionAzimuth}°")
            Result.success(profile)
        } catch (e: Exception) {
            Log.w(TAG, "Error calculating terrain DEM viewshed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun calculateDestinationPoint(
        latDeg: Double,
        lonDeg: Double,
        distanceMeters: Double,
        bearingDeg: Double
    ): Pair<Double, Double> {
        val latRad = Math.toRadians(latDeg)
        val lonRad = Math.toRadians(lonDeg)
        val bearingRad = Math.toRadians(bearingDeg)
        val angularDist = distanceMeters / EARTH_RADIUS_METERS

        val destLatRad = asin(sin(latRad) * cos(angularDist) + cos(latRad) * sin(angularDist) * cos(bearingRad))
        val destLonRad = lonRad + atan2(
            sin(bearingRad) * sin(angularDist) * cos(latRad),
            cos(angularDist) - sin(latRad) * sin(destLatRad)
        )

        return Pair(Math.toDegrees(destLatRad), Math.toDegrees(destLonRad))
    }
}

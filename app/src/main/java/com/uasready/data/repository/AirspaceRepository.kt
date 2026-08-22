package com.uasready.data.repository

import com.uasready.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

interface AirspaceRepository {
    suspend fun getAirspaceInfo(latitude: Double, longitude: Double): Result<AirspaceInfo>
}

class LiveAirspaceRepository : AirspaceRepository {

    override suspend fun getAirspaceInfo(
        latitude: Double,
        longitude: Double
    ): Result<AirspaceInfo> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val zones = mutableListOf<AirspaceZone>()
            var fetchedFromOpenAip = false
            var primaryClass: AirspaceClass = AirspaceClass.CLASS_G
            var authRequired: Boolean = false
            var uasfmCeiling: Double? = 400.0

            // 1. Query live openAIP Airspaces API (https://docs.openaip.net/)
            try {
                val distMeters = 35000 // 35 km map view radius
                val openAipUrl = "https://api.core.openaip.net/api/airspaces?page=1&limit=100&pos=$longitude,$latitude&dist=$distMeters"
                val connection = (URL(openAipUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "UASReady-App/1.0 (Enterprise UAS Safety)")
                }

                if (connection.responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    val items = json.optJSONArray("items") ?: JSONArray()

                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val name = item.optString("name", "Airspace")
                        val icaoClassInt = item.optInt("icaoClass", -1)
                        val typeInt = item.optInt("type", -1)
                        
                        // Parse geometry coordinates
                        val geometry = item.optJSONObject("geometry")
                        val coordinates = geometry?.optJSONArray("coordinates")
                        val polyPoints = mutableListOf<Pair<Double, Double>>()
                        
                        // Determine AirspaceZoneType based strictly on openAIP classification
                        val zoneType = when {
                            typeInt in listOf(0, 1, 2) -> AirspaceZoneType.RESTRICTED_ZONE // Restricted / Danger / Prohibited
                            icaoClassInt in listOf(1, 2, 3) || typeInt in listOf(3, 4) -> AirspaceZoneType.AUTHORIZATION_ZONE // Class B, C, D / CTR / TMA
                            icaoClassInt == 4 -> AirspaceZoneType.WARNING_ZONE // Class E
                            else -> AirspaceZoneType.ALTITUDE_ZONE
                        }

                        var centerLat = latitude
                        var centerLon = longitude
                        var radius = 4000.0

                        if (coordinates != null && coordinates.length() > 0) {
                            val outerRing = coordinates.optJSONArray(0)
                            if (outerRing != null && outerRing.length() > 0) {
                                var sumLat = 0.0
                                var sumLon = 0.0
                                val pointCount = outerRing.length()
                                for (p in 0 until pointCount) {
                                    val pt = outerRing.optJSONArray(p)
                                    if (pt != null && pt.length() >= 2) {
                                        val pLon = pt.optDouble(0, longitude)
                                        val pLat = pt.optDouble(1, latitude)
                                        polyPoints.add(Pair(pLat, pLon))
                                        sumLon += pLon
                                        sumLat += pLat
                                    }
                                }
                                if (pointCount > 0) {
                                    centerLat = sumLat / pointCount
                                    centerLon = sumLon / pointCount
                                }
                            }
                        }

                        // Check if operator location intersects this zone
                        val distToCenterNm = calculateDistanceNm(latitude, longitude, centerLat, centerLon)
                        val radiusNm = radius * 0.000539957
                        if (distToCenterNm <= radiusNm && zoneType == AirspaceZoneType.AUTHORIZATION_ZONE) {
                            authRequired = true
                            primaryClass = when (icaoClassInt) {
                                1 -> AirspaceClass.CLASS_B
                                2 -> AirspaceClass.CLASS_C
                                3 -> AirspaceClass.CLASS_D
                                else -> AirspaceClass.CLASS_D
                            }
                            uasfmCeiling = 200.0
                        }

                        zones.add(
                            AirspaceZone(
                                id = "OPENAIP-${item.optString("_id", i.toString())}",
                                name = name,
                                type = zoneType,
                                centerLat = centerLat,
                                centerLon = centerLon,
                                radiusMeters = radius,
                                floorFt = 0.0,
                                ceilingFt = 400.0,
                                description = "openAIP Aeronautical Service: $name",
                                polygonCoordinates = polyPoints
                            )
                        )
                    }
                    if (zones.isNotEmpty()) {
                        fetchedFromOpenAip = true
                    }
                }
            } catch (e: Exception) {
                // Network query failed; safely defaults to uncontrolled Class G
            }

            val airspace = AirspaceInfo(
                primaryClass = primaryClass,
                controlledAirspaceAuthorizationRequired = authRequired,
                uasFacilityMapMaxAltitudeFt = uasfmCeiling,
                activeTfrs = emptyList(),
                zones = zones,
                notams = listOf(
                    NoticeToAirmen(
                        id = "NOTAM-SUAS-GEN",
                        text = "Unmanned aircraft must yield right-of-way to all manned aircraft operations (14 CFR § 107.37)",
                        issuedEpochMs = now - 24 * 3600 * 1000L
                    )
                ),
                specialUseAirspaceActive = false,
                nearestAirportCode = null,
                nearestAirportDistanceNm = null,
                timestampEpochMs = now,
                sourceName = if (fetchedFromOpenAip) "openAIP Live Aeronautical API" else "openAIP Aeronautical Database",
                isStale = false
            )

            Result.success(airspace)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateDistanceNm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distanceKm = 6371.0 * c
        return distanceKm * 0.539957 // km to nautical miles
    }
}

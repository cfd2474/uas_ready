package com.uasready.data.repository

import android.util.Log
import com.uasready.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.*

interface AirspaceRepository {
    suspend fun getAirspaceInfo(latitude: Double, longitude: Double): Result<AirspaceInfo>
}

class LiveAirspaceRepository : AirspaceRepository {

    companion object {
        private const val TAG = "AirspaceRepo"
        private const val FAA_CLASS_AIRSPACE_URL =
            "https://services6.arcgis.com/ssFJjBXIUyZDrSYZ/arcgis/rest/services/Class_Airspace/FeatureServer/0/query"
        private const val FAA_SUA_URL =
            "https://services6.arcgis.com/ssFJjBXIUyZDrSYZ/arcgis/rest/services/Special_Use_Airspace/FeatureServer/0/query"
        private const val SEARCH_RADIUS_DEG = 0.55 // ~30-33 Nautical Miles (55-60 km)
    }

    override suspend fun getAirspaceInfo(
        latitude: Double,
        longitude: Double
    ): Result<AirspaceInfo> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val zones = mutableListOf<AirspaceZone>()
            var sourceName = "FAA Aeronautical Airspace Service"
            var primaryClass: AirspaceClass = AirspaceClass.CLASS_G
            var authRequired = false
            var isInsideRestricted = false

            val minLon = longitude - SEARCH_RADIUS_DEG
            val minLat = latitude - SEARCH_RADIUS_DEG
            val maxLon = longitude + SEARCH_RADIUS_DEG
            val maxLat = latitude + SEARCH_RADIUS_DEG

            // 1. Query Official FAA Live Class Airspace FeatureServer (Class B, C, D, E)
            try {
                val classQueryUrl = String.format(
                    Locale.US,
                    "%s?where=CLASS+IN+('B','C','D','E')&geometry=%.5f,%.5f,%.5f,%.5f&geometryType=esriGeometryEnvelope&inSR=4326&spatialRel=esriSpatialRelIntersects&outFields=OBJECTID,NAME,CLASS,LOCAL_TYPE,SECTOR,UPPER_VAL,UPPER_UOM,LOWER_VAL,LOWER_UOM&returnGeometry=true&f=geojson",
                    FAA_CLASS_AIRSPACE_URL, minLon, minLat, maxLon, maxLat
                )

                val conn = (URL(classQueryUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "UASReady-Android-App/1.0")
                }

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val geoJson = JSONObject(responseText)
                    val features = geoJson.optJSONArray("features") ?: JSONArray()

                    for (i in 0 until features.length()) {
                        val feature = features.optJSONObject(i) ?: continue
                        val props = feature.optJSONObject("properties") ?: JSONObject()
                        val rawName = props.optString("NAME", "Controlled Airspace")
                        val airClassStr = props.optString("CLASS", "").uppercase()
                        val localType = props.optString("LOCAL_TYPE", "")
                        val sector = props.optString("SECTOR", "")
                        val lowerVal = props.optDouble("LOWER_VAL", 0.0)
                        val upperVal = props.optDouble("UPPER_VAL", 0.0)
                        val lowerUom = props.optString("LOWER_UOM", "FT")
                        val upperUom = props.optString("UPPER_UOM", "FT")
                        val objectId = props.optInt("OBJECTID", i)

                        val isSurfaceE = airClassStr == "E" && (lowerVal <= 0.0 || localType.contains("E2", true) || localType.contains("E3", true) || localType.contains("E4", true) || localType.contains("SURFACE", true))

                        val (zoneType, airClass) = when {
                            airClassStr.contains("B") -> Pair(AirspaceZoneType.AUTHORIZATION_ZONE, AirspaceClass.CLASS_B)
                            airClassStr.contains("C") -> Pair(AirspaceZoneType.AUTHORIZATION_ZONE, AirspaceClass.CLASS_C)
                            airClassStr.contains("D") -> Pair(AirspaceZoneType.AUTHORIZATION_ZONE, AirspaceClass.CLASS_D)
                            isSurfaceE -> Pair(AirspaceZoneType.WARNING_ZONE, AirspaceClass.CLASS_E_SURFACE)
                            airClassStr.contains("E") -> Pair(AirspaceZoneType.WARNING_ZONE, AirspaceClass.CLASS_E)
                            else -> Pair(AirspaceZoneType.AUTHORIZATION_ZONE, AirspaceClass.CLASS_D)
                        }

                        val formattedName = when {
                            sector.isNotBlank() && sector != "null" -> "$rawName - $sector (Class $airClassStr)"
                            else -> "$rawName (Class $airClassStr)"
                        }

                        val altDescription = when {
                            lowerVal > 0.0 && upperVal > 0.0 -> String.format(Locale.US, "Altitudes: %.0f %s to %.0f %s MSL", lowerVal, lowerUom, upperVal, upperUom)
                            upperVal > 0.0 -> String.format(Locale.US, "Altitudes: Surface to %.0f %s MSL", upperVal, upperUom)
                            else -> "FAA Controlled Airspace Sector"
                        }

                        val polygons = extractPolygonsFromFeature(feature)
                        for ((polyIdx, polyPoints) in polygons.withIndex()) {
                            if (polyPoints.size >= 3) {
                                val isLaunchInZone = isPointInsidePolygon(latitude, longitude, polyPoints)
                                if (isLaunchInZone) {
                                    authRequired = true
                                    // Set primary class prioritizing B > C > D > Surface E > E
                                    if (primaryClassPriority(airClass) > primaryClassPriority(primaryClass)) {
                                        primaryClass = airClass
                                    }
                                }

                                val centerLat = polyPoints.map { it.first }.average()
                                val centerLon = polyPoints.map { it.second }.average()

                                zones.add(
                                    AirspaceZone(
                                        id = "FAA-CLASS-$objectId-$polyIdx",
                                        name = formattedName,
                                        type = zoneType,
                                        centerLat = centerLat,
                                        centerLon = centerLon,
                                        radiusMeters = 5000.0,
                                        floorFt = max(0.0, lowerVal),
                                        ceilingFt = if (upperVal > 0.0) upperVal else 400.0,
                                        description = "$formattedName: $altDescription",
                                        polygonCoordinates = polyPoints
                                    )
                                )
                            }
                        }
                    }
                    Log.i(TAG, "Loaded ${zones.size} controlled airspace sectors from FAA Class_Airspace")
                }
            } catch (e: Exception) {
                Log.w(TAG, "FAA Class Airspace query error: ${e.message}")
            }

            // 2. Query Official FAA Live Special Use Airspace FeatureServer (Prohibited, Restricted, Warning, Alert, MOA)
            try {
                val suaQueryUrl = String.format(
                    Locale.US,
                    "%s?where=1%%3D1&geometry=%.5f,%.5f,%.5f,%.5f&geometryType=esriGeometryEnvelope&inSR=4326&spatialRel=esriSpatialRelIntersects&outFields=OBJECTID,NAME,TYPE_CODE,CLASS,UPPER_VAL,UPPER_UOM,LOWER_VAL,LOWER_UOM&returnGeometry=true&f=geojson",
                    FAA_SUA_URL, minLon, minLat, maxLon, maxLat
                )

                val conn = (URL(suaQueryUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "UASReady-Android-App/1.0")
                }

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val geoJson = JSONObject(responseText)
                    val features = geoJson.optJSONArray("features") ?: JSONArray()

                    for (i in 0 until features.length()) {
                        val feature = features.optJSONObject(i) ?: continue
                        val props = feature.optJSONObject("properties") ?: JSONObject()
                        val rawName = props.optString("NAME", "Special Use Airspace")
                        val typeCode = props.optString("TYPE_CODE", "").uppercase()
                        val lowerVal = props.optDouble("LOWER_VAL", 0.0)
                        val upperVal = props.optDouble("UPPER_VAL", 0.0)
                        val lowerUom = props.optString("LOWER_UOM", "FT")
                        val upperUom = props.optString("UPPER_UOM", "FT")
                        val objectId = props.optInt("OBJECTID", i)

                        val isProhibitedOrRestricted = typeCode == "P" || typeCode == "R" ||
                                rawName.contains("PROHIBITED", true) || rawName.contains("RESTRICTED", true)

                        val zoneType = when {
                            isProhibitedOrRestricted -> AirspaceZoneType.RESTRICTED_ZONE
                            else -> AirspaceZoneType.SPECIAL_USE
                        }

                        val typeLabel = when (typeCode) {
                            "P" -> "Prohibited Area"
                            "R" -> "Restricted Area"
                            "W" -> "Warning Area"
                            "A" -> "Alert Area"
                            "MOA" -> "Military Operations Area (MOA)"
                            else -> "Special Use Airspace ($typeCode)"
                        }

                        val formattedName = "$rawName ($typeLabel)"
                        val altDescription = when {
                            lowerVal > 0.0 && upperVal > 0.0 -> String.format(Locale.US, "Altitudes: %.0f %s to %.0f %s MSL", lowerVal, lowerUom, upperVal, upperUom)
                            upperVal > 0.0 -> String.format(Locale.US, "Altitudes: Surface to %.0f %s MSL", upperVal, upperUom)
                            else -> "Special Use Airspace: Active Military/Government Operations"
                        }

                        val polygons = extractPolygonsFromFeature(feature)
                        for ((polyIdx, polyPoints) in polygons.withIndex()) {
                            if (polyPoints.size >= 3) {
                                val isLaunchInZone = isPointInsidePolygon(latitude, longitude, polyPoints)
                                if (isLaunchInZone) {
                                    if (isProhibitedOrRestricted) {
                                        isInsideRestricted = true
                                        primaryClass = AirspaceClass.SPECIAL_USE
                                    }
                                }

                                val centerLat = polyPoints.map { it.first }.average()
                                val centerLon = polyPoints.map { it.second }.average()

                                zones.add(
                                    AirspaceZone(
                                        id = "FAA-SUA-$objectId-$polyIdx",
                                        name = formattedName,
                                        type = zoneType,
                                        centerLat = centerLat,
                                        centerLon = centerLon,
                                        radiusMeters = 5000.0,
                                        floorFt = max(0.0, lowerVal),
                                        ceilingFt = if (upperVal > 0.0) upperVal else 400.0,
                                        description = "$formattedName: $altDescription",
                                        polygonCoordinates = polyPoints
                                    )
                                )
                            }
                        }
                    }
                    Log.i(TAG, "Total combined airspace zones loaded: ${zones.size}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "FAA SUA query error: ${e.message}")
            }

            val airspace = AirspaceInfo(
                primaryClass = primaryClass,
                controlledAirspaceAuthorizationRequired = authRequired || isInsideRestricted,
                uasFacilityMapMaxAltitudeFt = 400.0,
                activeTfrs = emptyList(),
                zones = zones,
                notams = listOf(
                    NoticeToAirmen(
                        id = "NOTAM-SUAS-GEN",
                        text = "Unmanned aircraft must yield right-of-way to all manned aircraft operations (14 CFR § 107.37)",
                        issuedEpochMs = now - 24 * 3600 * 1000L
                    )
                ),
                specialUseAirspaceActive = isInsideRestricted,
                nearestAirportCode = null,
                nearestAirportDistanceNm = null,
                timestampEpochMs = now,
                sourceName = if (zones.isNotEmpty()) sourceName else "Uncontrolled Airspace (Class G)",
                isStale = false
            )

            Log.i(TAG, "Successfully resolved ${zones.size} live FAA airspace polygons. Primary: $primaryClass, AuthRequired: $authRequired")
            Result.success(airspace)
        } catch (e: Exception) {
            Log.e(TAG, "Airspace resolution error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun extractPolygonsFromFeature(feature: JSONObject): List<List<Pair<Double, Double>>> {
        val result = mutableListOf<List<Pair<Double, Double>>>()
        val geometry = feature.optJSONObject("geometry") ?: return result
        val geomType = geometry.optString("type", "")
        val coords = geometry.optJSONArray("coordinates") ?: return result

        try {
            when (geomType) {
                "Polygon" -> {
                    // coords = [ [ [lon, lat], ... ], ... ] (Outer ring is index 0)
                    if (coords.length() > 0) {
                        val outerRing = coords.optJSONArray(0)
                        if (outerRing != null) {
                            val pts = mutableListOf<Pair<Double, Double>>()
                            for (p in 0 until outerRing.length()) {
                                val pt = outerRing.optJSONArray(p)
                                if (pt != null && pt.length() >= 2) {
                                    val pLon = pt.optDouble(0)
                                    val pLat = pt.optDouble(1)
                                    pts.add(Pair(pLat, pLon))
                                }
                            }
                            if (pts.size >= 3) result.add(pts)
                        }
                    }
                }
                "MultiPolygon" -> {
                    // coords = [ [ [ [lon, lat], ... ] ] ]
                    for (polyIdx in 0 until coords.length()) {
                        val poly = coords.optJSONArray(polyIdx) ?: continue
                        if (poly.length() > 0) {
                            val outerRing = poly.optJSONArray(0) ?: continue
                            val pts = mutableListOf<Pair<Double, Double>>()
                            for (p in 0 until outerRing.length()) {
                                val pt = outerRing.optJSONArray(p)
                                if (pt != null && pt.length() >= 2) {
                                    val pLon = pt.optDouble(0)
                                    val pLat = pt.optDouble(1)
                                    pts.add(Pair(pLat, pLon))
                                }
                            }
                            if (pts.size >= 3) result.add(pts)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting polygon geometry: ${e.message}")
        }
        return result
    }

    private fun primaryClassPriority(airClass: AirspaceClass): Int {
        return when (airClass) {
            AirspaceClass.SPECIAL_USE -> 6
            AirspaceClass.CLASS_B -> 5
            AirspaceClass.CLASS_C -> 4
            AirspaceClass.CLASS_D -> 3
            AirspaceClass.CLASS_E_SURFACE -> 2
            AirspaceClass.CLASS_E -> 1
            AirspaceClass.CLASS_G -> 0
        }
    }

    private fun isPointInsidePolygon(lat: Double, lon: Double, poly: List<Pair<Double, Double>>): Boolean {
        if (poly.size < 3) return false
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val (latI, lonI) = poly[i]
            val (latJ, lonJ) = poly[j]
            if (((latI > lat) != (latJ > lat)) &&
                (lon < (lonJ - lonI) * (lat - latI) / (latJ - latI) + lonI)
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
}



package com.taksolutions.uasready.data.repository

import android.util.Log
import com.taksolutions.uasready.domain.geo.GeoMath
import com.taksolutions.uasready.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Source of raw airspace candidates around a point. Classification happens in AirspaceClassifier. */
interface AirspaceRepository {
    suspend fun getAirspaceSnapshot(latitude: Double, longitude: Double): Result<AirspaceSnapshot>
}

/** The FAA ArcGIS org-wide request quota was exceeded (see docs/PLATFORM_REFERENCE.md). */
class AirspaceQuotaExceededException(message: String) : IOException(message)

/**
 * Live FAA airspace lookup (Class_Airspace + Special_Use_Airspace ArcGIS services).
 *
 * Safety contract: if EITHER FAA query fails (network error, non-200, ArcGIS error object,
 * truncated result, unparseable body), the whole lookup returns [Result.failure] so the engine
 * reports DATA_UNAVAILABLE. A failed lookup must never be reported as uncontrolled airspace.
 *
 * Field semantics follow docs/PLATFORM_REFERENCE.md: LOWER_CODE=SFC → AGL; LOWER_CODE=MSL → MSL;
 * LOWER_UOM=FL → flight level (value × 100 ft MSL).
 */
class LiveAirspaceRepository(
    private val classAirspaceUrl: String = FAA_CLASS_AIRSPACE_URL,
    private val specialUseAirspaceUrl: String = FAA_SUA_URL,
    private val quotaRetryDelayMs: Long = 3_000L
) : AirspaceRepository {

    companion object {
        private const val TAG = "AirspaceRepo"
        const val FAA_CLASS_AIRSPACE_URL =
            "https://services6.arcgis.com/ssFJjBXIUyZDrSYZ/arcgis/rest/services/Class_Airspace/FeatureServer/0/query"
        const val FAA_SUA_URL =
            "https://services6.arcgis.com/ssFJjBXIUyZDrSYZ/arcgis/rest/services/Special_Use_Airspace/FeatureServer/0/query"
        /** Latitude half-width: 0.55° ≈ 33 NM, covering the 30 NM display radius. */
        private const val SEARCH_RADIUS_LAT_DEG = 0.55
        private const val TIMEOUT_MS = 20_000
        /** No sUAS operation reaches an AGL floor at or above this; such zones are dropped at the source. */
        private const val MAX_RELEVANT_AGL_FLOOR_FT = 400.0
        /** Surface Class E designations. E5/E6 transition/enroute areas start at 700/1200 ft AGL. */
        private val SURFACE_CLASS_E_TYPES = setOf("CLASS_E2", "CLASS_E3", "CLASS_E4")
    }

    override suspend fun getAirspaceSnapshot(
        latitude: Double,
        longitude: Double
    ): Result<AirspaceSnapshot> = withContext(Dispatchers.IO) {
        try {
            val lonHalf = GeoMath.longitudeHalfWidthDeg(SEARCH_RADIUS_LAT_DEG, latitude)
            val envelope = Envelope(
                minLon = longitude - lonHalf, minLat = latitude - SEARCH_RADIUS_LAT_DEG,
                maxLon = longitude + lonHalf, maxLat = latitude + SEARCH_RADIUS_LAT_DEG
            )
            val zones = coroutineScope {
                val classZones = async { retryOnQuota { fetchClassAirspace(envelope) } }
                val suaZones = async { retryOnQuota { fetchSpecialUseAirspace(envelope) } }
                classZones.await() + suaZones.await()
            }
            Log.i(TAG, "Fetched ${zones.size} FAA airspace candidate zones around $latitude,$longitude")
            Result.success(
                AirspaceSnapshot(
                    centerLat = latitude,
                    centerLon = longitude,
                    zones = zones,
                    fetchedEpochMs = System.currentTimeMillis(),
                    sourceName = "FAA Aeronautical Airspace Service"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Airspace resolution failed; reporting DATA_UNAVAILABLE: ${e.message}", e)
            Result.failure(e)
        }
    }

    private data class Envelope(val minLon: Double, val minLat: Double, val maxLon: Double, val maxLat: Double) {
        fun query() = "$minLon,$minLat,$maxLon,$maxLat"
    }

    /** The quota is shared org-wide and refills continuously; one short retry often succeeds. */
    private suspend fun <T> retryOnQuota(block: () -> T): T = try {
        block()
    } catch (e: AirspaceQuotaExceededException) {
        Log.w(TAG, "FAA quota exceeded; retrying once in $quotaRetryDelayMs ms")
        delay(quotaRetryDelayMs)
        block()
    }

    /** GET [url] and parse the body as JSON. Throws on any transport, HTTP, ArcGIS, or parse failure. */
    private fun fetchGeoJson(url: String, label: String): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "UASReady-Android-App/1.0")
        }
        try {
            val code = conn.responseCode
            if (code == 429) throw AirspaceQuotaExceededException("$label: HTTP 429")
            if (code != 200) throw IOException("$label query failed: HTTP $code")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            // ArcGIS reports query errors (including quota) as an "error" object in the body.
            json.optJSONObject("error")?.let { err ->
                val message = "$label query error ${err.optInt("code")}: ${err.optString("message", "unknown")}"
                if (err.optInt("code") == 429) throw AirspaceQuotaExceededException(message)
                throw IOException(message)
            }
            if (!json.has("features")) throw IOException("$label response has no features array")
            // A truncated result would silently drop airspace; treat it as a failure.
            val truncated = json.optBoolean("exceededTransferLimit", false) ||
                json.optJSONObject("properties")?.optBoolean("exceededTransferLimit", false) == true
            if (truncated) throw IOException("$label result truncated (exceededTransferLimit)")
            return json
        } finally {
            conn.disconnect()
        }
    }

    private data class Floor(val ft: Double, val reference: AltitudeReference)

    /** Parse an FAA floor; null when the floor can never matter to sUAS (AGL ≥ 400 ft). */
    private fun parseFloor(props: JSONObject): Floor? {
        val value = props.optString("LOWER_VAL").toDoubleOrNull() ?: 0.0
        val uom = props.optString("LOWER_UOM", "FT").uppercase()
        val code = props.optString("LOWER_CODE", "").uppercase()
        val floor = when {
            uom == "FL" || code == "STD" -> Floor(value * 100.0, AltitudeReference.MSL)
            code == "MSL" -> Floor(value, AltitudeReference.MSL)
            else -> Floor(maxOf(0.0, value), AltitudeReference.AGL) // SFC (or blank) = above ground
        }
        return if (floor.reference == AltitudeReference.AGL && floor.ft >= MAX_RELEVANT_AGL_FLOOR_FT) null else floor
    }

    private fun describeFloor(f: Floor) = when {
        f.reference == AltitudeReference.AGL && f.ft <= 0.0 -> "Surface"
        f.reference == AltitudeReference.AGL -> String.format(Locale.US, "%.0f ft AGL", f.ft)
        else -> String.format(Locale.US, "%.0f ft MSL", f.ft)
    }

    private fun describeCeiling(props: JSONObject): String? {
        val upper = props.optString("UPPER_VAL").toDoubleOrNull() ?: return null
        if (upper <= 0.0) return null
        val uom = props.optString("UPPER_UOM", "FT").uppercase()
        val code = props.optString("UPPER_CODE", "").uppercase()
        return when {
            uom == "FL" || code == "STD" -> String.format(Locale.US, "FL%03.0f", upper)
            code == "SFC" -> String.format(Locale.US, "%.0f ft AGL", upper)
            else -> String.format(Locale.US, "%.0f ft MSL", upper)
        }
    }

    private fun fetchClassAirspace(env: Envelope): List<AirspaceZone> {
        val url = "$classAirspaceUrl?where=CLASS+IN+('B','C','D','E')&geometry=${env.query()}" +
            "&geometryType=esriGeometryEnvelope&inSR=4326&spatialRel=esriSpatialRelIntersects" +
            "&outFields=OBJECTID,NAME,CLASS,LOCAL_TYPE,SECTOR,UPPER_VAL,UPPER_UOM,UPPER_CODE,LOWER_VAL,LOWER_UOM,LOWER_CODE" +
            "&maxAllowableOffset=0.001&returnGeometry=true&f=geojson"
        val features = fetchGeoJson(url, "FAA Class_Airspace").optJSONArray("features") ?: JSONArray()
        val zones = mutableListOf<AirspaceZone>()

        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            val props = feature.optJSONObject("properties") ?: JSONObject()
            val classStr = props.optString("CLASS", "").uppercase()
            val localType = props.optString("LOCAL_TYPE", "").uppercase()

            val airspaceClass = when (classStr) {
                "B" -> AirspaceClass.CLASS_B
                "C" -> AirspaceClass.CLASS_C
                "D" -> AirspaceClass.CLASS_D
                "E" -> AirspaceClass.CLASS_E_SURFACE
                else -> continue
            }
            val floor = parseFloor(props) ?: continue
            // Only surface Class E matters below 400 ft; E5/E6 start at 700/1200 ft AGL.
            if (airspaceClass == AirspaceClass.CLASS_E_SURFACE &&
                localType !in SURFACE_CLASS_E_TYPES &&
                !(floor.reference == AltitudeReference.AGL && floor.ft <= 0.0)
            ) continue

            val rawName = props.optString("NAME", "Controlled Airspace")
            val sector = props.optString("SECTOR", "")
            val name = if (sector.isNotBlank() && sector != "null") "$rawName - $sector (Class $classStr)" else "$rawName (Class $classStr)"
            val ceiling = describeCeiling(props)
            val altitudes = "Altitudes: ${describeFloor(floor)}" + (ceiling?.let { " to $it" } ?: "")
            zones += toZones(feature, "FAA-CLASS-${props.optInt("OBJECTID", i)}", name,
                if (airspaceClass == AirspaceClass.CLASS_E_SURFACE) AirspaceZoneType.WARNING_ZONE else AirspaceZoneType.AUTHORIZATION_ZONE,
                floor, props, "$name: $altitudes", airspaceClass = airspaceClass, specialUseType = null)
        }
        Log.i(TAG, "Class_Airspace: ${zones.size} candidate zones")
        return zones
    }

    private fun fetchSpecialUseAirspace(env: Envelope): List<AirspaceZone> {
        val url = "$specialUseAirspaceUrl?where=1=1&geometry=${env.query()}" +
            "&geometryType=esriGeometryEnvelope&inSR=4326&spatialRel=esriSpatialRelIntersects" +
            "&outFields=OBJECTID,NAME,TYPE_CODE,CLASS,UPPER_VAL,UPPER_UOM,UPPER_CODE,LOWER_VAL,LOWER_UOM,LOWER_CODE" +
            "&maxAllowableOffset=0.001&returnGeometry=true&f=geojson"
        val features = fetchGeoJson(url, "FAA Special_Use_Airspace").optJSONArray("features") ?: JSONArray()
        val zones = mutableListOf<AirspaceZone>()

        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            val props = feature.optJSONObject("properties") ?: JSONObject()
            val floor = parseFloor(props) ?: continue
            val typeCode = props.optString("TYPE_CODE", "").uppercase()
            val specialUseType = when (typeCode) {
                "P" -> SpecialUseType.PROHIBITED
                "R" -> SpecialUseType.RESTRICTED
                else -> null
            }
            val typeLabel = when (typeCode) {
                "P" -> "Prohibited Area"
                "R" -> "Restricted Area"
                "W" -> "Warning Area"
                "A" -> "Alert Area"
                "D" -> "Danger Area"
                "MOA" -> "Military Operations Area (MOA)"
                else -> "Special Use Airspace ($typeCode)"
            }
            val name = "${props.optString("NAME", "Special Use Airspace")} ($typeLabel)"
            val ceiling = describeCeiling(props)
            val altitudes = "Altitudes: ${describeFloor(floor)}" + (ceiling?.let { " to $it" } ?: "")
            zones += toZones(feature, "FAA-SUA-${props.optInt("OBJECTID", i)}", name,
                if (specialUseType != null) AirspaceZoneType.RESTRICTED_ZONE else AirspaceZoneType.SPECIAL_USE,
                floor, props, "$name: $altitudes", airspaceClass = null, specialUseType = specialUseType)
        }
        Log.i(TAG, "Special_Use_Airspace: ${zones.size} candidate zones")
        return zones
    }

    private fun toZones(
        feature: JSONObject, idPrefix: String, name: String, type: AirspaceZoneType, floor: Floor,
        props: JSONObject, description: String, airspaceClass: AirspaceClass?, specialUseType: SpecialUseType?
    ): List<AirspaceZone> = extractPolygonsFromFeature(feature).withIndex().map { (idx, poly) ->
        AirspaceZone(
            id = "$idPrefix-$idx",
            name = name,
            type = type,
            centerLat = poly.map { it.first }.average(),
            centerLon = poly.map { it.second }.average(),
            radiusMeters = 5000.0,
            floorFt = floor.ft,
            ceilingFt = props.optString("UPPER_VAL").toDoubleOrNull()?.takeIf { it > 0.0 },
            description = description,
            polygonCoordinates = poly,
            floorReference = floor.reference,
            airspaceClass = airspaceClass,
            specialUseType = specialUseType
        )
    }

    /** Outer rings of Polygon / MultiPolygon geometry as (lat, lon) lists. */
    private fun extractPolygonsFromFeature(feature: JSONObject): List<List<Pair<Double, Double>>> {
        val result = mutableListOf<List<Pair<Double, Double>>>()
        val geometry = feature.optJSONObject("geometry") ?: return result
        val coords = geometry.optJSONArray("coordinates") ?: return result
        val polygons: List<JSONArray> = when (geometry.optString("type", "")) {
            "Polygon" -> listOf(coords)
            "MultiPolygon" -> (0 until coords.length()).mapNotNull { coords.optJSONArray(it) }
            else -> emptyList()
        }
        for (poly in polygons) {
            val outerRing = poly.optJSONArray(0) ?: continue
            val pts = (0 until outerRing.length()).mapNotNull { p ->
                outerRing.optJSONArray(p)?.takeIf { it.length() >= 2 }?.let { Pair(it.optDouble(1), it.optDouble(0)) }
            }
            if (pts.size >= 3) result += pts
        }
        return result
    }
}

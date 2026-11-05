package com.taksolutions.uasready.data.repository

import android.util.Log
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** The FAA ArcGIS org-wide request quota was exceeded (see docs/PLATFORM_REFERENCE.md). */
class AirspaceQuotaExceededException(message: String) : IOException(message)

/**
 * Shared transport for FAA ArcGIS FeatureServer queries.
 *
 * Contract (docs/PLATFORM_REFERENCE.md "ArcGIS Online quota and error behaviour"): ArcGIS reports
 * query errors, including the org-wide quota, as an `error` object in an HTTP 200 body, and can
 * silently truncate results. Both are failures here, never an empty result.
 */
class ArcGisQueryClient(
    private val quotaRetryDelayMs: Long = 3_000L,
    private val timeoutMs: Int = 20_000
) {
    /** The quota is shared org-wide and refills continuously; one short retry often succeeds. */
    suspend fun <T> retryOnQuota(block: () -> T): T = try {
        block()
    } catch (e: AirspaceQuotaExceededException) {
        Log.w(TAG, "FAA quota exceeded; retrying once in $quotaRetryDelayMs ms")
        delay(quotaRetryDelayMs)
        block()
    }

    /** GET [url] and parse the body. Throws on any transport, HTTP, ArcGIS, truncation or parse failure. */
    fun fetchJson(url: String, label: String): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "UASReady-Android-App/1.0")
        }
        try {
            val code = conn.responseCode
            if (code == 429) throw AirspaceQuotaExceededException("$label: HTTP 429")
            if (code != 200) throw IOException("$label query failed: HTTP $code")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            json.optJSONObject("error")?.let { err ->
                val message = "$label query error ${err.optInt("code")}: ${err.optString("message", "unknown")}"
                if (err.optInt("code") == 429) throw AirspaceQuotaExceededException(message)
                throw IOException(message)
            }
            if (!json.has("features")) throw IOException("$label response has no features array")
            // A truncated result would silently drop restrictions; treat it as a failure.
            val truncated = json.optBoolean("exceededTransferLimit", false) ||
                json.optJSONObject("properties")?.optBoolean("exceededTransferLimit", false) == true
            if (truncated) throw IOException("$label result truncated (exceededTransferLimit)")
            return json
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val TAG = "ArcGis"

        /** Outer rings of a GeoJSON Polygon / MultiPolygon feature as (lat, lon) lists. */
        fun outerRings(feature: JSONObject): List<List<Pair<Double, Double>>> {
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
}

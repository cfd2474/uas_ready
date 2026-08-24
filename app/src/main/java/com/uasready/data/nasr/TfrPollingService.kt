package com.uasready.data.nasr

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TfrPollingService(
    private val dbHelper: NasrDatabaseHelper
) {
    companion object {
        private const val TAG = "TfrPollingService"
        const val TFR_LIST_URL = "https://tfr.faa.gov/tfr2/list.jsp"
        const val TFR_GEOJSON_URL = "https://tfr.faa.gov/geoserver/tfr/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=tfr:tfr_notams&outputFormat=application/json"
    }

    var lastPollTimestampEpochMs: Long = 0L
        private set

    /**
     * Polls authoritative FAA TFR services and updates local SQLite storage.
     */
    suspend fun pollTfrs(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val tfrList = mutableListOf<ParsedTfr>()
            val now = System.currentTimeMillis()

            // 1. Fetch FAA TFR GeoJSON / WFS feed
            try {
                val conn = (URL(TFR_GEOJSON_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "UASReady-Android-App/1.0")
                }

                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val features = json.optJSONArray("features")
                    if (features != null) {
                        for (i in 0 until features.length()) {
                            val feat = features.optJSONObject(i) ?: continue
                            val props = feat.optJSONObject("properties") ?: JSONObject()
                            val notamId = props.optString("NOTAMKEY", props.optString("NOTAM_ID", "TFR-$i"))
                            val issueDate = props.optString("DATEISSUED", props.optString("ISSUE_DATE", "202608"))
                            val type = props.optString("TYPE", "TFR")
                            val desc = props.optString("TXT_DESCR", props.optString("DESCRIPTION", "Temporary Flight Restriction"))
                            val floor = props.optDouble("ALT_LOWER", 0.0)
                            val ceiling = props.optDouble("ALT_UPPER", 18000.0)

                            val geom = feat.optJSONObject("geometry")
                            val coords = geom?.optJSONArray("coordinates")
                            val poly = mutableListOf<Pair<Double, Double>>()
                            if (coords != null && coords.length() > 0) {
                                val ring = coords.optJSONArray(0)
                                if (ring != null) {
                                    for (p in 0 until ring.length()) {
                                        val pt = ring.optJSONArray(p)
                                        if (pt != null && pt.length() >= 2) {
                                            poly.add(Pair(pt.optDouble(1), pt.optDouble(0)))
                                        }
                                    }
                                }
                            }

                            val isHazard = type.contains("91.137", true) || desc.contains("91.137", true) || desc.contains("FIRE", true) || desc.contains("HAZARD", true)
                            val startMs = now - 3600000L
                            val endMs = now + 86400000L * 3

                            tfrList.add(
                                ParsedTfr(
                                    notamId = notamId,
                                    issueDate = issueDate,
                                    type = type,
                                    description = desc,
                                    floorFt = floor,
                                    ceilingFt = ceiling,
                                    startEpochMs = startMs,
                                    endEpochMs = endMs,
                                    polygonCoordinates = poly,
                                    isHazard91137 = isHazard
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "TFR GeoJSON poll fallback: ${e.message}")
            }

            // 2. Insert into database
            if (tfrList.isNotEmpty()) {
                dbHelper.cleanExpiredTfrs(now)
                dbHelper.insertTfrs(tfrList)
                Log.i(TAG, "Successfully polled and updated ${tfrList.size} FAA TFRs")
            }

            lastPollTimestampEpochMs = now
            Result.success(tfrList.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll FAA TFRs: ${e.message}", e)
            Result.failure(e)
        }
    }
}

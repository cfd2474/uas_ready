package com.taksolutions.uasready.data.repository

import android.content.Context
import android.util.Log
import org.json.JSONArray
import kotlin.math.*

data class AirportCtaf(
    val ident: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val frequencyMhz: String,
    val type: String
)

data class AirportCtafResult(
    val ident: String,
    val name: String,
    val frequencyMhz: String,
    val type: String,
    val distanceNm: Double
)

object CtafLookupHelper {
    private const val TAG = "CtafLookupHelper"
    private var cachedAirports: List<AirportCtaf>? = null

    @Synchronized
    fun initialize(context: Context) {
        if (cachedAirports != null) return
        try {
            val start = System.currentTimeMillis()
            val jsonText = context.assets.open("airports_ctaf.json").bufferedReader().use { it.readText() }
            val array = JSONArray(jsonText)
            val list = ArrayList<AirportCtaf>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AirportCtaf(
                        ident = obj.getString("id"),
                        name = obj.getString("name"),
                        lat = obj.getDouble("lat"),
                        lon = obj.getDouble("lon"),
                        frequencyMhz = obj.getString("freq"),
                        type = obj.getString("type")
                    )
                )
            }
            cachedAirports = list
            Log.i(TAG, "Loaded ${list.size} airports with CTAF/TWR frequencies in ${System.currentTimeMillis() - start} ms")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load airports_ctaf.json: ${e.message}", e)
        }
    }

    fun findNearestCtaf(lat: Double, lon: Double): AirportCtafResult? {
        val airports = cachedAirports ?: return null
        if (airports.isEmpty() || (lat == 0.0 && lon == 0.0)) return null

        var nearest: AirportCtaf? = null
        var minDistanceNm = Double.MAX_VALUE

        for (apt in airports) {
            val dLat = Math.toRadians(apt.lat - lat)
            val dLon = Math.toRadians(apt.lon - lon)
            val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat)) * cos(Math.toRadians(apt.lat)) * sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            val distNm = 3440.065 * c

            if (distNm < minDistanceNm) {
                minDistanceNm = distNm
                nearest = apt
            }
        }

        return nearest?.let {
            AirportCtafResult(
                ident = it.ident,
                name = it.name,
                frequencyMhz = it.frequencyMhz,
                type = it.type,
                distanceNm = minDistanceNm
            )
        }
    }
}

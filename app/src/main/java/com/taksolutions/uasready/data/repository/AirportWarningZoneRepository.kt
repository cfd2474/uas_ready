package com.taksolutions.uasready.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.taksolutions.uasready.domain.model.AirportWarningZone
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

interface AirportWarningZoneRepository {
    fun getWarningZones(lat: Double, lon: Double, radiusDeg: Double = 0.55): List<AirportWarningZone>
}

class LiveAirportWarningZoneRepository(
    private val context: Context? = null,
    private val dbFileOverride: File? = null
) : AirportWarningZoneRepository {

    companion object {
        private const val TAG = "AirportWarningRepo"
        private const val DB_NAME = "airport_warning_zones.db"

        fun unpackCoordinates(blob: ByteArray): List<Pair<Double, Double>> {
            if (blob.size < 4) return emptyList()
            val buffer = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN)
            val numRings = buffer.short.toInt()
            if (numRings <= 0) return emptyList()

            val numPts = buffer.short.toInt()
            if (numPts <= 0) return emptyList()

            val coords = ArrayList<Pair<Double, Double>>(numPts)
            for (i in 0 until numPts) {
                if (buffer.remaining() < 8) break
                val pLat = buffer.float.toDouble()
                val pLon = buffer.float.toDouble()
                coords.add(Pair(pLat, pLon))
            }
            return coords
        }
    }

    private var database: SQLiteDatabase? = null

    @Synchronized
    private fun getOrOpenDb(): SQLiteDatabase? {
        if (database != null && database?.isOpen == true) {
            return database
        }

        try {
            val dbFile = if (dbFileOverride != null) {
                dbFileOverride
            } else if (context != null) {
                val target = context.getDatabasePath(DB_NAME)
                if (!target.exists()) {
                    target.parentFile?.mkdirs()
                    Log.i(TAG, "Copying $DB_NAME from assets to ${target.absolutePath}...")
                    context.assets.open(DB_NAME).use { input ->
                        FileOutputStream(target).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.i(TAG, "Copied $DB_NAME successfully (${target.length()} bytes)")
                }
                target
            } else {
                null
            }

            if (dbFile != null && dbFile.exists()) {
                database = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open $DB_NAME: ${e.message}", e)
        }
        return database
    }

    override fun getWarningZones(lat: Double, lon: Double, radiusDeg: Double): List<AirportWarningZone> {
        val db = getOrOpenDb() ?: return emptyList()
        val results = ArrayList<AirportWarningZone>()

        val minLat = (lat - radiusDeg).toString()
        val maxLat = (lat + radiusDeg).toString()
        val minLon = (lon - radiusDeg).toString()
        val maxLon = (lon + radiusDeg).toString()

        val query = """
            SELECT ident, name, level, ring_m, color, lat, lon, geometry
            FROM airport_warning_zones
            WHERE min_lat <= ? AND max_lat >= ? AND min_lon <= ? AND max_lon >= ?
        """.trimIndent()

        try {
            db.rawQuery(query, arrayOf(maxLat, minLat, maxLon, minLon)).use { cursor ->
                val identIdx = cursor.getColumnIndexOrThrow("ident")
                val nameIdx = cursor.getColumnIndexOrThrow("name")
                val levelIdx = cursor.getColumnIndexOrThrow("level")
                val ringIdx = cursor.getColumnIndexOrThrow("ring_m")
                val colorIdx = cursor.getColumnIndexOrThrow("color")
                val latIdx = cursor.getColumnIndexOrThrow("lat")
                val lonIdx = cursor.getColumnIndexOrThrow("lon")
                val geomIdx = cursor.getColumnIndexOrThrow("geometry")

                while (cursor.moveToNext()) {
                    val ident = cursor.getString(identIdx)
                    val name = cursor.getString(nameIdx)
                    val level = cursor.getInt(levelIdx)
                    val ringM = cursor.getInt(ringIdx)
                    val color = cursor.getString(colorIdx)
                    val cLat = cursor.getDouble(latIdx)
                    val cLon = cursor.getDouble(lonIdx)
                    val geomBlob = cursor.getBlob(geomIdx)

                    val coords = unpackCoordinates(geomBlob)
                    if (coords.isNotEmpty()) {
                        results.add(
                            AirportWarningZone(
                                ident = ident,
                                name = name,
                                level = level,
                                zoneName = if (level == 3) "Enhanced Warning" else "Warning",
                                ringRadiusMeters = ringM,
                                colorHex = color,
                                centerLat = cLat,
                                centerLon = cLon,
                                polygonCoordinates = coords
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying airport warning zones: ${e.message}", e)
        }

        return results
    }
}

package com.uasready.data.nasr

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.uasready.domain.model.AirspaceClass
import com.uasready.domain.model.AirspaceZone
import com.uasready.domain.model.AirspaceZoneType
import com.uasready.domain.model.TemporaryFlightRestriction
import java.io.File
import kotlin.math.max
import kotlin.math.min

class NasrDatabaseHelper(context: Context, dbName: String = DB_NAME) : SQLiteOpenHelper(context, dbName, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "nasr_airspace.db"
        const val DB_VERSION = 7
        private const val TAG = "NasrDbHelper"
        private const val MASTER_ASSET_PATH = "databases/nasr_airspace.db.gz"

        @Volatile
        private var instance: NasrDatabaseHelper? = null

        fun getInstance(context: Context): NasrDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: run {
                    ensureMasterDatabaseExtracted(context.applicationContext)
                    NasrDatabaseHelper(context.applicationContext).also { instance = it }
                }
            }
        }

        fun resetInstance() {
            synchronized(this) {
                try {
                    instance?.close()
                } catch (_: Exception) {}
                instance = null
            }
        }

        /**
         * Verifies that the on-device database exists and contains the authoritative master dataset
         * (at least 300,000 UASFM grid cells and 15,000 airports). If missing or incomplete,
         * it synchronously extracts the master pre-compiled asset.
         */
        @Synchronized
        fun ensureMasterDatabaseExtracted(context: Context): Boolean {
            val dbFile = context.getDatabasePath(DB_NAME)

            // Check if existing DB file is valid with full data
            if (dbFile.exists() && dbFile.length() > 30_000_000L) {
                var isValid = false
                try {
                    SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                        val uasfmCount = db.rawQuery("SELECT COUNT(*) FROM uasfm_grid", null).use { c ->
                            if (c.moveToFirst()) c.getInt(0) else 0
                        }
                        val airportCount = db.rawQuery("SELECT COUNT(*) FROM airports", null).use { c ->
                            if (c.moveToFirst()) c.getInt(0) else 0
                        }
                        Log.i(TAG, "Database health check on disk: $uasfmCount UASFM cells, $airportCount airports")
                        if (uasfmCount >= 300_000 && airportCount >= 15_000) {
                            isValid = true
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Existing database file invalid: ${e.message}")
                    isValid = false
                }
                if (isValid) {
                    return true
                }
            }

            Log.i(TAG, "Extracting authoritative master database from $MASTER_ASSET_PATH...")
            return try {
                resetInstance()
                context.deleteDatabase(DB_NAME)
                dbFile.parentFile?.mkdirs()

                val tempFile = File(dbFile.parentFile, "$DB_NAME.tmp")
                if (tempFile.exists()) tempFile.delete()

                context.assets.open(MASTER_ASSET_PATH).use { rawIn ->
                    java.util.zip.GZIPInputStream(rawIn).use { gzIn ->
                        java.io.FileOutputStream(tempFile).use { out ->
                            gzIn.copyTo(out)
                        }
                    }
                }

                if (tempFile.renameTo(dbFile) || run {
                    tempFile.copyTo(dbFile, overwrite = true)
                    tempFile.delete()
                    true
                }) {
                    Log.i(TAG, "Master FAA database asset extracted successfully (${dbFile.length()} bytes)")
                    true
                } else {
                    Log.e(TAG, "Failed to rename temp database file to $dbFile")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract master FAA database asset: ${e.message}", e)
                false
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            // 1. Meta Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS meta (
                    key TEXT PRIMARY KEY,
                    value TEXT
                )
                """.trimIndent()
            )

            // 2. Airports Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS airports (
                    rowid INTEGER PRIMARY KEY AUTOINCREMENT,
                    facility_id TEXT UNIQUE,
                    icao_id TEXT,
                    name TEXT,
                    city TEXT,
                    state TEXT,
                    lat REAL,
                    lon REAL,
                    elevation_ft REAL,
                    use_type TEXT,
                    ctaf_freq TEXT,
                    unicom_freq TEXT,
                    tower_freq TEXT,
                    atis_freq TEXT
                )
                """.trimIndent()
            )

            // 3. Runways Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS runways (
                    id TEXT PRIMARY KEY,
                    facility_id TEXT,
                    base_end_id TEXT,
                    recip_end_id TEXT,
                    length_ft REAL,
                    width_ft REAL,
                    surface TEXT,
                    true_bearing REAL
                )
                """.trimIndent()
            )

            // 4. Frequencies Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS frequencies (
                    id TEXT PRIMARY KEY,
                    facility_id TEXT,
                    type TEXT,
                    freq_mhz TEXT,
                    name TEXT
                )
                """.trimIndent()
            )

            // 5. Airspace Boundaries Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS airspace (
                    rowid INTEGER PRIMARY KEY AUTOINCREMENT,
                    id TEXT UNIQUE,
                    name TEXT,
                    class TEXT,
                    type TEXT,
                    floor_ft REAL,
                    floor_datum TEXT,
                    ceiling_ft REAL,
                    ceiling_datum TEXT,
                    geom_wkb BLOB,
                    min_lat REAL,
                    max_lat REAL,
                    min_lon REAL,
                    max_lon REAL
                )
                """.trimIndent()
            )

            // 6. UASFM Grids Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS uasfm_grid (
                    rowid INTEGER PRIMARY KEY AUTOINCREMENT,
                    id TEXT UNIQUE,
                    icao_id TEXT,
                    ceiling_ft REAL,
                    geom_wkb BLOB,
                    min_lat REAL,
                    max_lat REAL,
                    min_lon REAL,
                    max_lon REAL
                )
                """.trimIndent()
            )

            // 7. Special Use Airspace Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS sua (
                    rowid INTEGER PRIMARY KEY AUTOINCREMENT,
                    id TEXT UNIQUE,
                    name TEXT,
                    type TEXT,
                    floor_ft REAL,
                    ceiling_ft REAL,
                    schedule_desc TEXT,
                    geom_wkb BLOB,
                    min_lat REAL,
                    max_lat REAL,
                    min_lon REAL,
                    max_lon REAL
                )
                """.trimIndent()
            )

            // 8. National Security UAS Restrictions Table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS national_security_restrictions (
                    rowid INTEGER PRIMARY KEY AUTOINCREMENT,
                    id TEXT UNIQUE,
                    proponent TEXT,
                    branch TEXT,
                    base TEXT,
                    facility TEXT,
                    airspace_class TEXT,
                    reason TEXT,
                    poc TEXT,
                    floor_ft REAL,
                    ceiling_ft REAL,
                    geom_wkb BLOB,
                    min_lat REAL,
                    max_lat REAL,
                    min_lon REAL,
                    max_lon REAL
                )
                """.trimIndent()
            )

            // 9. Active TFRs Table (Runtime volatile)
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS tfr_active (
                    rowid INTEGER PRIMARY KEY AUTOINCREMENT,
                    notam_id TEXT,
                    issue_date TEXT,
                    type TEXT,
                    description TEXT,
                    floor_ft REAL,
                    ceiling_ft REAL,
                    start_epoch INTEGER,
                    end_epoch INTEGER,
                    geom_wkb BLOB,
                    min_lat REAL,
                    max_lat REAL,
                    min_lon REAL,
                    max_lon REAL,
                    UNIQUE(notam_id, issue_date)
                )
                """.trimIndent()
            )

            // Try creating R*Tree Virtual Index Tables
            try {
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS rtree_airports USING rtree(id, min_lat, max_lat, min_lon, max_lon)")
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS rtree_airspace USING rtree(id, min_lat, max_lat, min_lon, max_lon)")
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS rtree_uasfm USING rtree(id, min_lat, max_lat, min_lon, max_lon)")
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS rtree_sua USING rtree(id, min_lat, max_lat, min_lon, max_lon)")
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS rtree_tfr USING rtree(id, min_lat, max_lat, min_lon, max_lon)")
            } catch (rtreeEx: Exception) {
                Log.w(TAG, "R*Tree virtual table creation fallback: ${rtreeEx.message}")
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        try {
            // Verify if nationwide CONUS seed data (e.g. KLAS) exists in the database
            var hasKlas = false
            db.rawQuery("SELECT COUNT(*) FROM airports WHERE icao_id = 'KLAS'", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    hasKlas = cursor.getInt(0) > 0
                }
            }
            if (!hasKlas) {
                Log.i(TAG, "Nationwide CONUS data (KLAS) not found in DB. Re-creating schema & loading full CONUS dataset...")
                onUpgrade(db, 1, DB_VERSION)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking DB seed status on open: ${e.message}")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Atomic drop / recreate on schema change
        Log.i(TAG, "Upgrading database from version $oldVersion to $newVersion...")
        db.execSQL("DROP TABLE IF EXISTS meta")
        db.execSQL("DROP TABLE IF EXISTS airports")
        db.execSQL("DROP TABLE IF EXISTS runways")
        db.execSQL("DROP TABLE IF EXISTS frequencies")
        db.execSQL("DROP TABLE IF EXISTS airspace")
        db.execSQL("DROP TABLE IF EXISTS uasfm_grid")
        db.execSQL("DROP TABLE IF EXISTS sua")
        db.execSQL("DROP TABLE IF EXISTS tfr_active")
        db.execSQL("DROP TABLE IF EXISTS rtree_airports")
        db.execSQL("DROP TABLE IF EXISTS rtree_airspace")
        db.execSQL("DROP TABLE IF EXISTS rtree_uasfm")
        db.execSQL("DROP TABLE IF EXISTS rtree_sua")
        db.execSQL("DROP TABLE IF EXISTS rtree_tfr")
        onCreate(db)
    }

    // --- Bulk Insertion Helpers ---

    fun insertAirports(airports: List<NasrAirport>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (apt in airports) {
                val cv = ContentValues().apply {
                    put("facility_id", apt.facilityId)
                    put("icao_id", apt.icaoId)
                    put("name", apt.name)
                    put("city", apt.city)
                    put("state", apt.state)
                    put("lat", apt.latitude)
                    put("lon", apt.longitude)
                    put("elevation_ft", apt.elevationFt)
                    put("use_type", apt.useType)
                    put("ctaf_freq", apt.ctafFreq)
                    put("unicom_freq", apt.unicomFreq)
                    put("tower_freq", apt.towerFreq)
                    put("atis_freq", apt.atisFreq)
                }
                val rowId = db.insertWithOnConflict("airports", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                if (rowId > 0) {
                    try {
                        db.execSQL(
                            "INSERT OR REPLACE INTO rtree_airports VALUES (?, ?, ?, ?, ?)",
                            arrayOf(rowId, apt.latitude - 0.001, apt.latitude + 0.001, apt.longitude - 0.001, apt.longitude + 0.001)
                        )
                    } catch (_: Exception) {}
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertAirspaces(airspaces: List<NasrAirspaceFeature>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (feat in airspaces) {
                val bbox = GeometryUtils.calculateBoundingBox(feat.polygonCoordinates)
                val wkb = GeometryUtils.encodePolygonToWkb(feat.polygonCoordinates)
                val cv = ContentValues().apply {
                    put("id", feat.id)
                    put("name", feat.name)
                    put("class", feat.airspaceClass.name)
                    put("type", feat.zoneType.name)
                    put("floor_ft", feat.floorFt)
                    put("floor_datum", feat.floorDatum)
                    put("ceiling_ft", feat.ceilingFt)
                    put("ceiling_datum", feat.ceilingDatum)
                    put("geom_wkb", wkb)
                    put("min_lat", bbox.minLat)
                    put("max_lat", bbox.maxLat)
                    put("min_lon", bbox.minLon)
                    put("max_lon", bbox.maxLon)
                }
                val rowId = db.insertWithOnConflict("airspace", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                if (rowId > 0) {
                    try {
                        db.execSQL(
                            "INSERT OR REPLACE INTO rtree_airspace VALUES (?, ?, ?, ?, ?)",
                            arrayOf(rowId, bbox.minLat, bbox.maxLat, bbox.minLon, bbox.maxLon)
                        )
                    } catch (_: Exception) {}
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertUasfmGrids(grids: List<NasrUasfmGrid>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (grid in grids) {
                val bbox = GeometryUtils.calculateBoundingBox(grid.polygonCoordinates)
                val wkb = GeometryUtils.encodePolygonToWkb(grid.polygonCoordinates)
                val cv = ContentValues().apply {
                    put("id", grid.id)
                    put("icao_id", grid.icaoId)
                    put("ceiling_ft", grid.ceilingFt)
                    put("geom_wkb", wkb)
                    put("min_lat", bbox.minLat)
                    put("max_lat", bbox.maxLat)
                    put("min_lon", bbox.minLon)
                    put("max_lon", bbox.maxLon)
                }
                val rowId = db.insertWithOnConflict("uasfm_grid", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                if (rowId > 0) {
                    try {
                        db.execSQL(
                            "INSERT OR REPLACE INTO rtree_uasfm VALUES (?, ?, ?, ?, ?)",
                            arrayOf(rowId, bbox.minLat, bbox.maxLat, bbox.minLon, bbox.maxLon)
                        )
                    } catch (_: Exception) {}
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertSuaFeatures(suaList: List<NasrSua>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (sua in suaList) {
                val bbox = GeometryUtils.calculateBoundingBox(sua.polygonCoordinates)
                val wkb = GeometryUtils.encodePolygonToWkb(sua.polygonCoordinates)
                val cv = ContentValues().apply {
                    put("id", sua.id)
                    put("name", sua.name)
                    put("type", sua.type)
                    put("floor_ft", sua.floorFt)
                    put("ceiling_ft", sua.ceilingFt)
                    put("schedule_desc", sua.scheduleDesc)
                    put("geom_wkb", wkb)
                    put("min_lat", bbox.minLat)
                    put("max_lat", bbox.maxLat)
                    put("min_lon", bbox.minLon)
                    put("max_lon", bbox.maxLon)
                }
                val rowId = db.insertWithOnConflict("sua", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                if (rowId > 0) {
                    try {
                        db.execSQL(
                            "INSERT OR REPLACE INTO rtree_sua VALUES (?, ?, ?, ?, ?)",
                            arrayOf(rowId, bbox.minLat, bbox.maxLat, bbox.minLon, bbox.maxLon)
                        )
                    } catch (_: Exception) {}
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertTfrs(tfrList: List<ParsedTfr>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (tfr in tfrList) {
                val bbox = if (tfr.polygonCoordinates.isNotEmpty()) {
                    GeometryUtils.calculateBoundingBox(tfr.polygonCoordinates)
                } else if (tfr.centerLat != null && tfr.centerLon != null) {
                    val deg = (tfr.radiusNm * 1852.0) / 111139.0
                    GeometryUtils.BoundingBox(tfr.centerLat - deg, tfr.centerLat + deg, tfr.centerLon - deg, tfr.centerLon + deg)
                } else {
                    GeometryUtils.BoundingBox(0.0, 0.0, 0.0, 0.0)
                }
                val wkb = if (tfr.polygonCoordinates.isNotEmpty()) GeometryUtils.encodePolygonToWkb(tfr.polygonCoordinates) else null

                val cv = ContentValues().apply {
                    put("notam_id", tfr.notamId)
                    put("issue_date", tfr.issueDate)
                    put("type", tfr.type)
                    put("description", tfr.description)
                    put("floor_ft", tfr.floorFt)
                    put("ceiling_ft", tfr.ceilingFt)
                    put("start_epoch", tfr.startEpochMs)
                    put("end_epoch", tfr.endEpochMs)
                    put("geom_wkb", wkb)
                    put("min_lat", bbox.minLat)
                    put("max_lat", bbox.maxLat)
                    put("min_lon", bbox.minLon)
                    put("max_lon", bbox.maxLon)
                }
                val rowId = db.insertWithOnConflict("tfr_active", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                if (rowId > 0 && wkb != null) {
                    try {
                        db.execSQL(
                            "INSERT OR REPLACE INTO rtree_tfr VALUES (?, ?, ?, ?, ?)",
                            arrayOf(rowId, bbox.minLat, bbox.maxLat, bbox.minLon, bbox.maxLon)
                        )
                    } catch (_: Exception) {}
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun cleanExpiredTfrs(nowEpochMs: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        db.delete("tfr_active", "end_epoch < ?", arrayOf(nowEpochMs.toString()))
    }

    fun setMetaValue(key: String, value: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        db.insertWithOnConflict("meta", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getMetaValue(key: String): String? {
        val db = readableDatabase
        db.rawQuery("SELECT value FROM meta WHERE key = ?", arrayOf(key)).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

    // --- Spatial Query Methods ---

    /**
     * Queries airports within bounding box around (lat, lon) with radius in nautical miles.
     * Default radius is 100 NM to provide expansive regional situational awareness across CONUS.
     */
    fun queryAirportsNearby(lat: Double, lon: Double, radiusNm: Double = 100.0): List<NasrAirport> {
        val degRadius = radiusNm / 60.0
        return queryAirportsInBoundingBox(
            minLat = lat - degRadius,
            maxLat = lat + degRadius,
            minLon = lon - degRadius,
            maxLon = lon + degRadius,
            limit = 500
        ).sortedBy { GeometryUtils.calculateDistanceNm(lat, lon, it.latitude, it.longitude) }
    }

    /**
     * Queries airports in an arbitrary geographic bounding box across CONUS without distance clamping.
     */
    fun queryAirportsInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double, limit: Int = 1000): List<NasrAirport> {
        val db = readableDatabase
        val list = mutableListOf<NasrAirport>()
        val sLat = minOf(minLat, maxLat)
        val nLat = maxOf(minLat, maxLat)
        val wLon = minOf(minLon, maxLon)
        val eLon = maxOf(minLon, maxLon)

        val query = """
            SELECT facility_id, icao_id, name, city, state, lat, lon, elevation_ft, use_type, ctaf_freq, unicom_freq, tower_freq, atis_freq
            FROM airports
            WHERE lat BETWEEN ? AND ? AND lon BETWEEN ? AND ?
            LIMIT ?
        """.trimIndent()

        db.rawQuery(query, arrayOf(sLat.toString(), nLat.toString(), wLon.toString(), eLon.toString(), limit.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    NasrAirport(
                        facilityId = cursor.getString(0) ?: "",
                        icaoId = cursor.getString(1) ?: "",
                        name = cursor.getString(2) ?: "",
                        city = cursor.getString(3) ?: "",
                        state = cursor.getString(4) ?: "",
                        latitude = cursor.getDouble(5),
                        longitude = cursor.getDouble(6),
                        elevationFt = cursor.getDouble(7),
                        useType = cursor.getString(8) ?: "PU",
                        ctafFreq = cursor.getString(9),
                        unicomFreq = cursor.getString(10),
                        towerFreq = cursor.getString(11),
                        atisFreq = cursor.getString(12)
                    )
                )
            }
        }
        return list
    }

    /**
     * Finds the nearest airport to given coordinates across the entire CONUS database with no distance limits.
     */
    fun findNearestAirport(lat: Double, lon: Double): NasrAirport? {
        val db = readableDatabase
        // Stepwise radial expansion: 60 NM, 300 NM, 1000 NM, entire DB
        for (radiusDeg in listOf(1.0, 5.0, 16.0)) {
            val candidates = queryAirportsInBoundingBox(
                minLat = lat - radiusDeg,
                maxLat = lat + radiusDeg,
                minLon = lon - radiusDeg,
                maxLon = lon + radiusDeg,
                limit = 300
            )
            if (candidates.isNotEmpty()) {
                return candidates.minByOrNull { GeometryUtils.calculateDistanceNm(lat, lon, it.latitude, it.longitude) }
            }
        }
        val allQuery = "SELECT facility_id, icao_id, name, city, state, lat, lon, elevation_ft, use_type, ctaf_freq, unicom_freq, tower_freq, atis_freq FROM airports"
        val list = mutableListOf<NasrAirport>()
        db.rawQuery(allQuery, null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    NasrAirport(
                        facilityId = cursor.getString(0) ?: "",
                        icaoId = cursor.getString(1) ?: "",
                        name = cursor.getString(2) ?: "",
                        city = cursor.getString(3) ?: "",
                        state = cursor.getString(4) ?: "",
                        latitude = cursor.getDouble(5),
                        longitude = cursor.getDouble(6),
                        elevationFt = cursor.getDouble(7),
                        useType = cursor.getString(8) ?: "PU",
                        ctafFreq = cursor.getString(9),
                        unicomFreq = cursor.getString(10),
                        towerFreq = cursor.getString(11),
                        atisFreq = cursor.getString(12)
                    )
                )
            }
        }
        return list.minByOrNull { GeometryUtils.calculateDistanceNm(lat, lon, it.latitude, it.longitude) }
    }

    /**
     * Queries airspace polygons intersecting bounding box around (lat, lon).
     * Expanded radius default to 150 NM to ensure full regional coverage across CONUS.
     */
    fun queryAirspaceNearby(lat: Double, lon: Double, radiusNm: Double = 150.0): List<AirspaceZone> {
        val degRadius = radiusNm / 60.0
        return queryAirspaceInBoundingBox(
            minLat = lat - degRadius,
            maxLat = lat + degRadius,
            minLon = lon - degRadius,
            maxLon = lon + degRadius,
            limit = 500
        )
    }

    /**
     * Queries airspace polygons in an arbitrary geographic bounding box across CONUS without distance clamping.
     */
    fun queryAirspaceInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double, limit: Int = 1000): List<AirspaceZone> {
        val db = readableDatabase
        val list = mutableListOf<AirspaceZone>()
        val sLat = minOf(minLat, maxLat)
        val nLat = maxOf(minLat, maxLat)
        val wLon = minOf(minLon, maxLon)
        val eLon = maxOf(minLon, maxLon)

        val query = """
            SELECT id, name, class, type, floor_ft, ceiling_ft, geom_wkb
            FROM airspace
            WHERE min_lat <= ? AND max_lat >= ? AND min_lon <= ? AND max_lon >= ?
            LIMIT ?
        """.trimIndent()

        db.rawQuery(query, arrayOf(nLat.toString(), sLat.toString(), eLon.toString(), wLon.toString(), limit.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val name = cursor.getString(1)
                val clsStr = cursor.getString(2)
                val typeStr = cursor.getString(3)
                val floor = cursor.getDouble(4)
                val ceiling = cursor.getDouble(5)
                val wkb = cursor.getBlob(6)

                val polygon = if (wkb != null) GeometryUtils.decodeWkbToPolygon(wkb) else emptyList()
                val zoneType = try { AirspaceZoneType.valueOf(typeStr) } catch (_: Exception) { AirspaceZoneType.AUTHORIZATION_ZONE }

                val cLat = if (polygon.isNotEmpty()) polygon.map { it.first }.average() else (sLat + nLat) / 2.0
                val cLon = if (polygon.isNotEmpty()) polygon.map { it.second }.average() else (wLon + eLon) / 2.0

                list.add(
                    AirspaceZone(
                        id = id,
                        name = name,
                        type = zoneType,
                        centerLat = cLat,
                        centerLon = cLon,
                        radiusMeters = 5000.0,
                        floorFt = floor,
                        ceilingFt = ceiling,
                        description = "FAA NASR: $name ($clsStr)",
                        polygonCoordinates = polygon
                    )
                )
            }
        }
        return list
    }

    /**
     * Queries UASFM grid squares around (lat, lon).
     * Expanded radius default to 75 NM.
     */
    fun queryUasfmGridsNearby(lat: Double, lon: Double, radiusNm: Double = 75.0): List<AirspaceZone> {
        val degRadius = radiusNm / 60.0
        return queryUasfmInBoundingBox(
            minLat = lat - degRadius,
            maxLat = lat + degRadius,
            minLon = lon - degRadius,
            maxLon = lon + degRadius,
            limit = 2000
        )
    }

    /**
     * Queries UASFM grid squares in an arbitrary geographic bounding box across CONUS without distance clamping.
     */
    fun queryUasfmInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double, limit: Int = 6000): List<AirspaceZone> {
        val db = readableDatabase
        val list = mutableListOf<AirspaceZone>()
        val sLat = minOf(minLat, maxLat)
        val nLat = maxOf(minLat, maxLat)
        val wLon = minOf(minLon, maxLon)
        val eLon = maxOf(minLon, maxLon)

        val query = """
            SELECT id, icao_id, ceiling_ft, geom_wkb, min_lat, max_lat, min_lon, max_lon
            FROM uasfm_grid
            WHERE min_lat <= ? AND max_lat >= ? AND min_lon <= ? AND max_lon >= ?
            LIMIT ?
        """.trimIndent()

        db.rawQuery(query, arrayOf(nLat.toString(), sLat.toString(), eLon.toString(), wLon.toString(), limit.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val icao = cursor.getString(1)
                val ceiling = cursor.getDouble(2)
                val wkb = cursor.getBlob(3)
                val cMinLat = cursor.getDouble(4)
                val cMaxLat = cursor.getDouble(5)
                val cMinLon = cursor.getDouble(6)
                val cMaxLon = cursor.getDouble(7)

                val polygon = if (wkb != null) {
                    GeometryUtils.decodeWkbToPolygon(wkb)
                } else {
                    listOf(
                        Pair(cMinLat, cMinLon),
                        Pair(cMinLat, cMaxLon),
                        Pair(cMaxLat, cMaxLon),
                        Pair(cMaxLat, cMinLon),
                        Pair(cMinLat, cMinLon)
                    )
                }

                val cLat = (cMinLat + cMaxLat) / 2.0
                val cLon = (cMinLon + cMaxLon) / 2.0

                list.add(
                    AirspaceZone(
                        id = "NASR-UASFM-$id",
                        name = "$icao UAS Facility Grid (${ceiling.toInt()} ft AGL)",
                        type = AirspaceZoneType.ALTITUDE_ZONE,
                        centerLat = cLat,
                        centerLon = cLon,
                        radiusMeters = 800.0,
                        floorFt = 0.0,
                        ceilingFt = ceiling,
                        description = "$icao UAS Facility Map: Max auto-approved LAANC ceiling is ${ceiling.toInt()} ft AGL.",
                        polygonCoordinates = polygon
                    )
                )
            }
        }
        return list
    }

    /**
     * Queries National Security UAS Flight Restrictions (14 CFR § 99.7 / § 2209) in a bounding box.
     */
    fun queryNationalSecurityRestrictionsInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double, limit: Int = 500): List<AirspaceZone> {
        val db = readableDatabase
        val list = mutableListOf<AirspaceZone>()
        val sLat = minOf(minLat, maxLat)
        val nLat = maxOf(minLat, maxLat)
        val wLon = minOf(minLon, maxLon)
        val eLon = maxOf(minLon, maxLon)

        val query = """
            SELECT id, proponent, branch, base, facility, airspace_class, reason, poc, floor_ft, ceiling_ft, geom_wkb, min_lat, max_lat, min_lon, max_lon
            FROM national_security_restrictions
            WHERE min_lat <= ? AND max_lat >= ? AND min_lon <= ? AND max_lon >= ?
            LIMIT ?
        """.trimIndent()

        try {
            db.rawQuery(query, arrayOf(nLat.toString(), sLat.toString(), eLon.toString(), wLon.toString(), limit.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val proponent = cursor.getString(1) ?: ""
                    val branch = cursor.getString(2) ?: ""
                    val base = cursor.getString(3) ?: ""
                    val facility = cursor.getString(4) ?: ""
                    val airspaceClass = cursor.getString(5) ?: ""
                    val reason = cursor.getString(6) ?: "National Security UAS Flight Restriction"
                    val poc = cursor.getString(7) ?: ""
                    val floor = cursor.getDouble(8)
                    val ceiling = cursor.getDouble(9)
                    val wkb = cursor.getBlob(10)
                    val cMinLat = cursor.getDouble(11)
                    val cMaxLat = cursor.getDouble(12)
                    val cMinLon = cursor.getDouble(13)
                    val cMaxLon = cursor.getDouble(14)

                    val polygon = if (wkb != null) GeometryUtils.decodeWkbToPolygon(wkb) else emptyList()
                    val cLat = if (polygon.isNotEmpty()) polygon.map { it.first }.average() else (cMinLat + cMaxLat) / 2.0
                    val cLon = if (polygon.isNotEmpty()) polygon.map { it.second }.average() else (cMinLon + cMaxLon) / 2.0

                    val labelName = listOf(proponent, branch, base, facility).filter { it.isNotBlank() }.joinToString(" - ")
                    val desc = "FAA 14 CFR § 99.7 National Security Restriction (${if (airspaceClass.isNotBlank()) airspaceClass else "Airspace"}): $reason" + if (poc.isNotBlank()) "\nPOC: $poc" else ""

                    list.add(
                        AirspaceZone(
                            id = "NS-$id",
                            name = if (labelName.isNotBlank()) labelName else "National Security UAS Restriction",
                            type = AirspaceZoneType.RESTRICTED_ZONE,
                            centerLat = cLat,
                            centerLon = cLon,
                            radiusMeters = 3000.0,
                            floorFt = floor,
                            ceilingFt = ceiling,
                            description = desc,
                            polygonCoordinates = polygon
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Table national_security_restrictions query fallback: ${e.message}")
        }
        return list
    }

    /**
     * Queries Special Use Airspace around (lat, lon).
     * Expanded radius default to 200 NM.
     */
    fun querySuaNearby(lat: Double, lon: Double, radiusNm: Double = 200.0): List<AirspaceZone> {
        val degRadius = radiusNm / 60.0
        return querySuaInBoundingBox(
            minLat = lat - degRadius,
            maxLat = lat + degRadius,
            minLon = lon - degRadius,
            maxLon = lon + degRadius,
            limit = 1000
        )
    }

    /**
     * Queries Special Use Airspace in an arbitrary geographic bounding box across CONUS without distance clamping.
     */
    fun querySuaInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double, limit: Int = 1500): List<AirspaceZone> {
        val db = readableDatabase
        val list = mutableListOf<AirspaceZone>()
        val sLat = minOf(minLat, maxLat)
        val nLat = maxOf(minLat, maxLat)
        val wLon = minOf(minLon, maxLon)
        val eLon = maxOf(minLon, maxLon)

        val query = """
            SELECT id, name, type, floor_ft, ceiling_ft, schedule_desc, geom_wkb
            FROM sua
            WHERE min_lat <= ? AND max_lat >= ? AND min_lon <= ? AND max_lon >= ?
            LIMIT ?
        """.trimIndent()

        db.rawQuery(query, arrayOf(nLat.toString(), sLat.toString(), eLon.toString(), wLon.toString(), limit.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val name = cursor.getString(1)
                val type = cursor.getString(2)
                val floor = cursor.getDouble(3)
                val ceiling = cursor.getDouble(4)
                val schedule = cursor.getString(5) ?: ""
                val wkb = cursor.getBlob(6)
                val polygon = if (wkb != null) GeometryUtils.decodeWkbToPolygon(wkb) else emptyList()

                val cLat = if (polygon.isNotEmpty()) polygon.map { it.first }.average() else (sLat + nLat) / 2.0
                val cLon = if (polygon.isNotEmpty()) polygon.map { it.second }.average() else (wLon + eLon) / 2.0

                val zoneType = when (type.uppercase()) {
                    "RESTRICTED", "PROHIBITED" -> AirspaceZoneType.RESTRICTED_ZONE
                    else -> AirspaceZoneType.SPECIAL_USE
                }

                list.add(
                    AirspaceZone(
                        id = "NASR-SUA-$id",
                        name = "$name ($type)",
                        type = zoneType,
                        centerLat = cLat,
                        centerLon = cLon,
                        radiusMeters = 8000.0,
                        floorFt = floor,
                        ceilingFt = ceiling,
                        description = "Special Use Airspace: $name ($type). Floor: ${floor.toInt()} ft, Ceiling: ${ceiling.toInt()} ft. $schedule",
                        polygonCoordinates = polygon
                    )
                )
            }
        }
        return list
    }

    /**
     * Queries active TFRs around (lat, lon).
     * Expanded radius default to 250 NM.
     */
    fun queryActiveTfrsNearby(lat: Double, lon: Double, radiusNm: Double = 250.0, nowMs: Long = System.currentTimeMillis()): List<ParsedTfr> {
        val degRadius = radiusNm / 60.0
        return queryActiveTfrsInBoundingBox(
            minLat = lat - degRadius,
            maxLat = lat + degRadius,
            minLon = lon - degRadius,
            maxLon = lon + degRadius,
            nowMs = nowMs,
            limit = 500
        )
    }

    /**
     * Queries active TFRs in an arbitrary geographic bounding box across CONUS without distance clamping.
     */
    fun queryActiveTfrsInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double, nowMs: Long = System.currentTimeMillis(), limit: Int = 1000): List<ParsedTfr> {
        val db = readableDatabase
        val list = mutableListOf<ParsedTfr>()
        val sLat = minOf(minLat, maxLat)
        val nLat = maxOf(minLat, maxLat)
        val wLon = minOf(minLon, maxLon)
        val eLon = maxOf(minLon, maxLon)

        val query = """
            SELECT notam_id, issue_date, type, description, floor_ft, ceiling_ft, start_epoch, end_epoch, min_lat, max_lat, min_lon, max_lon, geom_wkb
            FROM tfr_active
            WHERE end_epoch >= ? AND min_lat <= ? AND max_lat >= ? AND min_lon <= ? AND max_lon >= ?
            LIMIT ?
        """.trimIndent()

        db.rawQuery(query, arrayOf(nowMs.toString(), nLat.toString(), sLat.toString(), eLon.toString(), wLon.toString(), limit.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                val notamId = cursor.getString(0)
                val issueDate = cursor.getString(1)
                val type = cursor.getString(2)
                val desc = cursor.getString(3)
                val floor = cursor.getDouble(4)
                val ceiling = cursor.getDouble(5)
                val start = cursor.getLong(6)
                val end = cursor.getLong(7)
                val cLat = (cursor.getDouble(8) + cursor.getDouble(9)) / 2.0
                val cLon = (cursor.getDouble(10) + cursor.getDouble(11)) / 2.0
                val wkb = cursor.getBlob(12)
                val polygon = if (wkb != null) GeometryUtils.decodeWkbToPolygon(wkb) else emptyList()
                val isHazard = type.contains("91.137", true) || desc.contains("91.137", true) || desc.contains("FIRE", true) || desc.contains("HAZARD", true)

                list.add(
                    ParsedTfr(
                        notamId = notamId,
                        issueDate = issueDate,
                        type = type,
                        description = desc,
                        floorFt = floor,
                        ceilingFt = ceiling,
                        startEpochMs = start,
                        endEpochMs = end,
                        centerLat = cLat,
                        centerLon = cLon,
                        polygonCoordinates = polygon,
                        isHazard91137 = isHazard
                    )
                )
            }
        }
        return list
    }

    fun getUasfmCount(): Int {
        return try {
            val db = readableDatabase
            db.rawQuery("SELECT COUNT(*) FROM uasfm_grid", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        } catch (_: Exception) {
            0
        }
    }

    fun getAirportCount(): Int {
        return try {
            val db = readableDatabase
            db.rawQuery("SELECT COUNT(*) FROM airports", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Checks whether database contains valid nationwide CONUS master data.
     */
    fun hasAirportData(): Boolean {
        return hasNationwideAirportData()
    }

    fun hasNationwideAirportData(): Boolean {
        return getAirportCount() >= 15000 && getUasfmCount() >= 300000
    }

    /**
     * Drops all tables and re-creates empty database schema.
     */
    fun resetDatabase() {
        val db = writableDatabase
        onUpgrade(db, DB_VERSION, DB_VERSION)
    }
}

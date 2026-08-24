package com.uasready.data.nasr

import android.content.Context
import android.util.Log
import com.uasready.data.repository.AirspaceRepository
import com.uasready.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NasrAirspaceRepository(
    private val context: Context,
    private val dbHelper: NasrDatabaseHelper = NasrDatabaseHelper(context),
    private val tfrPoller: TfrPollingService = TfrPollingService(dbHelper)
) : AirspaceRepository {

    companion object {
        private const val TAG = "NasrAirspaceRepo"
    }

    init {
        // Ensure database is populated with current authoritative seed data if empty
        try {
            NasrSeedData.populateDatabaseIfEmpty(dbHelper)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing NASR seed data: ${e.message}", e)
        }
    }

    override suspend fun getAirspaceInfo(latitude: Double, longitude: Double): Result<AirspaceInfo> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()

            // 1. Check DB initialization
            if (!dbHelper.hasAirportData()) {
                NasrSeedData.populateDatabaseIfEmpty(dbHelper)
            }

            // 1b. Live FAA UAS Facility Map V5 sync from ArcGIS FeatureServer
            try {
                val uasfmUrl = "https://services6.arcgis.com/ssFJjBXIUyZDrSYZ/arcgis/rest/services/FAA_UAS_FacilityMap_Data_V5/FeatureServer/0/query?where=1%3D1&geometry=$longitude,$latitude&geometryType=esriGeometryPoint&inSR=4326&spatialRel=esriSpatialRelIntersects&distance=40000&units=esriSRUnit_Meter&outFields=OBJECTID,CEILING,APT1_ICAO,APT1_NAME,AIRSPACE_1&returnGeometry=true&f=geojson"
                val conn = (URL(uasfmUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 3500
                    readTimeout = 3500
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "UASReady-App/1.0")
                }

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val geoJson = JSONObject(responseText)
                    val features = geoJson.optJSONArray("features") ?: JSONArray()
                    val liveGrids = mutableListOf<NasrUasfmGrid>()

                    for (i in 0 until features.length()) {
                        val feature = features.optJSONObject(i) ?: continue
                        val props = feature.optJSONObject("properties") ?: JSONObject()
                        val ceiling = props.optDouble("CEILING", 400.0)
                        val icao = props.optString("APT1_ICAO", "UASFM")
                        val objId = props.optInt("OBJECTID", i)

                        val geom = feature.optJSONObject("geometry")
                        val coords = geom?.optJSONArray("coordinates")
                        val polyPoints = mutableListOf<Pair<Double, Double>>()

                        if (coords != null && coords.length() > 0) {
                            val ring = coords.optJSONArray(0)
                            if (ring != null) {
                                for (p in 0 until ring.length()) {
                                    val pt = ring.optJSONArray(p)
                                    if (pt != null && pt.length() >= 2) {
                                        polyPoints.add(Pair(pt.optDouble(1), pt.optDouble(0)))
                                    }
                                }
                            }
                        }

                        if (polyPoints.size >= 3) {
                            liveGrids.add(
                                NasrUasfmGrid(
                                    id = "$icao-LIVE-$objId",
                                    icaoId = icao,
                                    ceilingFt = ceiling,
                                    polygonCoordinates = polyPoints
                                )
                            )
                        }
                    }

                    if (liveGrids.isNotEmpty()) {
                        dbHelper.insertUasfmGrids(liveGrids)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Live FAA UASFM query skipped / offline: ${e.message}")
            }

            // 2. Query nearby Airspace boundaries, UASFM grids, SUA, TFRs, and Airports with nationwide CONUS coverage
            val nearbyAirspaces = dbHelper.queryAirspaceNearby(latitude, longitude, radiusNm = 150.0)
            val nearbyUasfm = dbHelper.queryUasfmGridsNearby(latitude, longitude, radiusNm = 75.0)
            val nearbySua = dbHelper.querySuaNearby(latitude, longitude, radiusNm = 200.0)
            val nearbyAirports = dbHelper.queryAirportsNearby(latitude, longitude, radiusNm = 120.0)
            val nearbyTfrs = dbHelper.queryActiveTfrsNearby(latitude, longitude, radiusNm = 250.0, nowMs = now)

            val allZones = mutableListOf<AirspaceZone>()
            allZones.addAll(nearbyAirspaces)
            allZones.addAll(nearbyUasfm)
            allZones.addAll(nearbySua)

            // Convert active TFRs to AirspaceZone overlays
            val domainTfrs = mutableListOf<TemporaryFlightRestriction>()
            for (tfr in nearbyTfrs) {
                domainTfrs.add(tfr.toDomainModel())
                if (tfr.polygonCoordinates.isNotEmpty()) {
                    allZones.add(
                        AirspaceZone(
                            id = "TFR-${tfr.notamId}",
                            name = if (tfr.isHazard91137) "14 CFR § 91.137 TFR (${tfr.notamId})" else "TFR ${tfr.notamId} (${tfr.type})",
                            type = AirspaceZoneType.RESTRICTED_ZONE,
                            centerLat = tfr.centerLat ?: latitude,
                            centerLon = tfr.centerLon ?: longitude,
                            radiusMeters = tfr.radiusNm * 1852.0,
                            floorFt = tfr.floorFt,
                            ceilingFt = tfr.ceilingFt,
                            description = "FAA TFR: ${tfr.description}",
                            polygonCoordinates = tfr.polygonCoordinates
                        )
                    )
                }
            }

            // 3. Point-in-polygon resolution for exact aircraft location
            var primaryClass = AirspaceClass.CLASS_G
            var authRequired = false
            var uasfmCeiling: Double? = null
            var suaActive = false
            var suaName: String? = null

            // Evaluate Airspace containment
            for (zone in nearbyAirspaces) {
                if (zone.polygonCoordinates.isNotEmpty() && GeometryUtils.isPointInsidePolygon(latitude, longitude, zone.polygonCoordinates)) {
                    authRequired = true
                    val nameUpper = zone.name.uppercase()
                    primaryClass = when {
                        nameUpper.contains("CLASS B") -> AirspaceClass.CLASS_B
                        nameUpper.contains("CLASS C") -> AirspaceClass.CLASS_C
                        nameUpper.contains("CLASS D") -> AirspaceClass.CLASS_D
                        nameUpper.contains("CLASS E") -> AirspaceClass.CLASS_E_SURFACE
                        else -> AirspaceClass.CLASS_D
                    }
                }
            }

            // Evaluate UASFM grid cell containment
            for (grid in nearbyUasfm) {
                if (grid.polygonCoordinates.isNotEmpty() && GeometryUtils.isPointInsidePolygon(latitude, longitude, grid.polygonCoordinates)) {
                    uasfmCeiling = grid.ceilingFt
                    break
                }
            }

            // If in controlled airspace without a matched UASFM grid, default ceiling to 0 ft (LAANC auto-approval not available)
            if (authRequired && uasfmCeiling == null) {
                uasfmCeiling = 0.0
            }

            // Evaluate SUA containment
            for (sua in nearbySua) {
                if (sua.polygonCoordinates.isNotEmpty() && GeometryUtils.isPointInsidePolygon(latitude, longitude, sua.polygonCoordinates)) {
                    suaActive = true
                    suaName = sua.name
                }
            }

            // 4. Nearest Airport & CTAF across CONUS (unconstrained distance fallback)
            val nearestApt = nearbyAirports.firstOrNull() ?: dbHelper.findNearestAirport(latitude, longitude)
            val nearestDistNm = nearestApt?.let { GeometryUtils.calculateDistanceNm(latitude, longitude, it.latitude, it.longitude) }

            // 5. AIRAC Cycle Metadata & Staleness check
            val expireEpochStr = dbHelper.getMetaValue("expire_epoch_ms")
            val expireEpochMs = expireEpochStr?.toLongOrNull() ?: (now + 14 * 86400000L)
            val isExpired = now > expireEpochMs
            val cycleName = dbHelper.getMetaValue("airac_cycle") ?: "2608"

            val notams = mutableListOf<NoticeToAirmen>()
            notams.add(
                NoticeToAirmen(
                    id = "NASR-AIRAC-$cycleName",
                    text = "FAA NASR AIRAC Cycle $cycleName active. All UAS operations must yield right-of-way to manned aircraft (14 CFR § 107.37).",
                    issuedEpochMs = now - 86400000L
                )
            )

            if (nearestApt != null && nearestApt.effectiveCtaf != null) {
                notams.add(
                    NoticeToAirmen(
                        id = "CTAF-${nearestApt.icaoId}",
                        text = "Nearest Airport: ${nearestApt.name} (${nearestApt.icaoId}) - CTAF: ${nearestApt.effectiveCtaf} MHz",
                        issuedEpochMs = now
                    )
                )
            }

            val airspace = AirspaceInfo(
                primaryClass = primaryClass,
                controlledAirspaceAuthorizationRequired = authRequired,
                uasFacilityMapMaxAltitudeFt = uasfmCeiling,
                activeTfrs = domainTfrs,
                zones = allZones,
                notams = notams,
                specialUseAirspaceActive = suaActive,
                specialUseName = suaName,
                nearestAirportCode = nearestApt?.icaoId,
                nearestAirportDistanceNm = nearestDistNm,
                timestampEpochMs = now,
                sourceName = "FAA NASR Cycle $cycleName",
                isStale = isExpired
            )

            Log.i(TAG, "Resolved ${allZones.size} zones (${domainTfrs.size} TFRs) from FAA NASR DB for ($latitude, $longitude). Primary Class: $primaryClass, UASFM: $uasfmCeiling ft")
            Result.success(airspace)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving NASR airspace info: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun pollActiveTfrs(): Result<Int> {
        return tfrPoller.pollTfrs()
    }

    fun getNearbyAirports(lat: Double, lon: Double, radiusNm: Double = 100.0): List<NasrAirport> {
        return dbHelper.queryAirportsNearby(lat, lon, radiusNm)
    }

    fun getAirportsInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<NasrAirport> {
        return dbHelper.queryAirportsInBoundingBox(minLat, maxLat, minLon, maxLon)
    }

    fun getAirspacesInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<AirspaceZone> {
        return dbHelper.queryAirspaceInBoundingBox(minLat, maxLat, minLon, maxLon)
    }

    fun getUasfmInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<AirspaceZone> {
        return dbHelper.queryUasfmInBoundingBox(minLat, maxLat, minLon, maxLon)
    }

    fun getSuaInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<AirspaceZone> {
        return dbHelper.querySuaInBoundingBox(minLat, maxLat, minLon, maxLon)
    }

    fun getAiracCycleInfo(): AiracCycleInfo {
        val now = System.currentTimeMillis()
        val cycle = dbHelper.getMetaValue("airac_cycle") ?: "2608"
        val effectiveMs = dbHelper.getMetaValue("effective_epoch_ms")?.toLongOrNull() ?: (now - 10 * 86400000L)
        val expireMs = dbHelper.getMetaValue("expire_epoch_ms")?.toLongOrNull() ?: (now + 18 * 86400000L)
        val lastChecked = dbHelper.getMetaValue("last_checked_epoch_ms")?.toLongOrNull() ?: now
        val lastUpdated = dbHelper.getMetaValue("last_updated_epoch_ms")?.toLongOrNull() ?: now
        val isExpired = now > expireMs
        val daysUntilExpiry = ((expireMs - now) / 86400000L).coerceAtLeast(0).toInt()

        return AiracCycleInfo(
            cycleName = cycle,
            effectiveEpochMs = effectiveMs,
            expireEpochMs = expireMs,
            lastCheckedEpochMs = lastChecked,
            lastUpdatedEpochMs = lastUpdated,
            isExpired = isExpired,
            daysUntilExpiry = daysUntilExpiry
        )
    }
}

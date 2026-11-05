package com.taksolutions.uasready.data.repository

import android.util.Log
import com.taksolutions.uasready.domain.geo.GeoMath
import com.taksolutions.uasready.domain.model.DcSfraZone
import com.taksolutions.uasready.domain.model.NationalSecurityUasSite
import com.taksolutions.uasready.domain.model.SecurityAirspaceSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/** National-security UAS restrictions (14 CFR 99.7 sites, US–Mexico border strip, DC SFRA/FRZ) around a point. */
interface SecurityAirspaceRepository {
    suspend fun getSecurityAirspace(latitude: Double, longitude: Double): Result<SecurityAirspaceSnapshot>
}

/**
 * Live FAA lookup. See docs/PLATFORM_REFERENCE.md "FAA National Security UAS Flight Restrictions"
 * and "DC SFRA / FRZ".
 *
 * 1. Point intersection on the national-security layer — decides "inside" at full resolution.
 * 2. Envelope query on the same layer with simplified geometry — proximity caution and map.
 * 3. Point intersection on Boundary_Airspace, DC SFRA / DC FRZ sectors only.
 *
 * Safety contract: all-or-nothing. If any query fails the result is a failure and the app reports
 * national-security restrictions as unverified. Results are not cached: these restrictions are
 * added at short notice (the border strip appeared on 2026-10-16).
 */
class LiveSecurityAirspaceRepository(
    private val nationalSecurityUrl: String = FAA_NATIONAL_SECURITY_URL,
    private val boundaryAirspaceUrl: String = FAA_BOUNDARY_AIRSPACE_URL,
    private val client: ArcGisQueryClient = ArcGisQueryClient(),
    private val clock: () -> Long = System::currentTimeMillis
) : SecurityAirspaceRepository {

    companion object {
        private const val TAG = "SecurityAirspace"
        const val FAA_NATIONAL_SECURITY_URL =
            "https://services6.arcgis.com/ssFJjBXIUyZDrSYZ/arcgis/rest/services/DoD_Mar_13/FeatureServer/0/query"
        const val FAA_BOUNDARY_AIRSPACE_URL =
            "https://services6.arcgis.com/ssFJjBXIUyZDrSYZ/arcgis/rest/services/Boundary_Airspace/FeatureServer/0/query"
        /** ≈ 33 NM, matching the airspace display radius. */
        private const val NEARBY_RADIUS_LAT_DEG = 0.55
        /** ≈ 50 m; keeps a border strip to tens of KB. Used only for the map and the 1 NM proximity caution. */
        private const val MAP_SIMPLIFY_DEG = "0.0005"
        private const val SITE_FIELDS = "OBJECTID,Proponent,Branch,Base,Facility,FAA_ID,POC,Floor,Ceiling"
        private const val BORDER_WAIVER_MARKER = "SWBDRONEZONEWAIVERS"
        private const val DC_SECTOR_FILTER = "SECTOR IN ('DC SFRA','DC FRZ')"
    }

    override suspend fun getSecurityAirspace(latitude: Double, longitude: Double): Result<SecurityAirspaceSnapshot> =
        withContext(Dispatchers.IO) {
            try {
                val point = "geometry=$longitude,$latitude&geometryType=esriGeometryPoint&inSR=4326&spatialRel=esriSpatialRelIntersects"
                val lonHalf = GeoMath.longitudeHalfWidthDeg(NEARBY_RADIUS_LAT_DEG, latitude)
                val envelope = "geometry=${longitude - lonHalf},${latitude - NEARBY_RADIUS_LAT_DEG},${longitude + lonHalf},${latitude + NEARBY_RADIUS_LAT_DEG}" +
                    "&geometryType=esriGeometryEnvelope&inSR=4326&spatialRel=esriSpatialRelIntersects"
                val snapshot = coroutineScope {
                    val atPoint = async {
                        client.retryOnQuota {
                            features(client.fetchJson("$nationalSecurityUrl?where=1%3D1&$point&outFields=$SITE_FIELDS&returnGeometry=false&f=json",
                                "FAA National Security UAS (point)")).map { toSite(it.optJSONObject("attributes") ?: JSONObject(), emptyList()) }
                        }
                    }
                    val nearby = async {
                        client.retryOnQuota {
                            features(client.fetchJson("$nationalSecurityUrl?where=1%3D1&$envelope&outFields=$SITE_FIELDS&returnGeometry=true" +
                                "&outSR=4326&maxAllowableOffset=$MAP_SIMPLIFY_DEG&f=geojson", "FAA National Security UAS (nearby)"))
                                .map { toSite(it.optJSONObject("properties") ?: JSONObject(), ArcGisQueryClient.outerRings(it)) }
                        }
                    }
                    val dc = async {
                        client.retryOnQuota {
                            val where = URLEncoder.encode(DC_SECTOR_FILTER, "UTF-8")
                            val sectors = features(client.fetchJson("$boundaryAirspaceUrl?where=$where&$point&outFields=SECTOR&returnGeometry=false&f=json",
                                "FAA Boundary_Airspace (DC SFRA)")).map { it.optJSONObject("attributes")?.optString("SECTOR").orEmpty().uppercase() }
                            when {
                                "DC FRZ" in sectors -> DcSfraZone.FRZ
                                "DC SFRA" in sectors -> DcSfraZone.SFRA
                                else -> null
                            }
                        }
                    }
                    SecurityAirspaceSnapshot(atPoint.await(), nearby.await(), dc.await(), clock())
                }
                Log.i(TAG, "National security: atPoint=${snapshot.sitesAtLaunchPoint.map { it.faaId }} nearby=${snapshot.nearbySites.size} dc=${snapshot.dcSfraZone}")
                Result.success(snapshot)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "National-security UAS restrictions unavailable: ${e.message}")
                Result.failure(e)
            }
        }

    private fun features(json: JSONObject): List<JSONObject> {
        val array = json.optJSONArray("features") ?: JSONArray()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }

    private fun toSite(a: JSONObject, polygons: List<List<Pair<Double, Double>>>): NationalSecurityUasSite {
        fun field(name: String) = a.optString(name, "").trim().takeIf { it.isNotEmpty() && it != "null" }
        val borderStrip = field("POC")?.contains(BORDER_WAIVER_MARKER, ignoreCase = true) == true
        val name = listOfNotNull(field("Base"), field("Facility")?.takeIf { f -> f != field("Base") && f.length > 2 })
            .joinToString(" – ").ifEmpty { "National security site" }
            .let { base ->
                // The border strips carry an ARTCC code as Base ("ZLA"); CBP's southwest-border
                // waiver address identifies them (docs/PLATFORM_REFERENCE.md).
                if (borderStrip) "US–Mexico border 1 NM UAS zone ($base)" else base
            }
        return NationalSecurityUasSite(
            faaId = field("FAA_ID") ?: "OBJECTID ${a.optInt("OBJECTID")}",
            name = name,
            agency = listOfNotNull(field("Proponent"), field("Branch")).joinToString(" "),
            floorText = field("Floor") ?: "Surface",
            ceilingText = field("Ceiling") ?: "400' AGL",
            contact = field("POC") ?: "",
            isMexicoBorderStrip = borderStrip,
            polygons = polygons
        )
    }
}

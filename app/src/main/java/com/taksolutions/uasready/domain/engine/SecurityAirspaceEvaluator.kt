package com.taksolutions.uasready.domain.engine

import com.taksolutions.uasready.domain.geo.GeoMath
import com.taksolutions.uasready.domain.model.*

/** What national-security UAS restrictions mean for one launch point. */
data class SecurityAirspaceEvaluation(
    /** Sites containing the launch point (full-resolution server intersection). Polygons stripped. */
    val sitesAtLaunchPoint: List<NationalSecurityUasSite>,
    /** Other sites within [SecurityAirspaceEvaluator.cautionRadiusNm]: the aircraft could fly into them. */
    val sitesNearby: List<NationalSecurityUasSite>,
    val dcSfraZone: DcSfraZone?,
    val mapZones: List<AirspaceZone>
)

/**
 * Pure. Every site in the FAA layer is surface–400 ft AGL (docs/PLATFORM_REFERENCE.md), so any
 * sUAS altitude is affected and planned altitude is not consulted.
 */
class SecurityAirspaceEvaluator(
    val cautionRadiusNm: Double = 1.0,
    /**
     * The FAA border-strip polygons measure ≈ 0.87 NM wide in CA/AZ (about one statute mile), but
     * the NOTAMs prohibit UAS "WI 1NM INLAND". Within this distance of a strip counts as inside.
     */
    val borderStripMarginNm: Double = 0.15
) {

    fun evaluate(snapshot: SecurityAirspaceSnapshot, latitude: Double, longitude: Double): SecurityAirspaceEvaluation {
        val widenedBorder = snapshot.nearbySites.filter { site ->
            site.isMexicoBorderStrip && site.polygons.any { GeoMath.distanceToPolygonNm(latitude, longitude, it) <= borderStripMarginNm }
        }
        val atLaunch = (snapshot.sitesAtLaunchPoint + widenedBorder).distinctBy { it.faaId }
        val insideIds = atLaunch.map { it.faaId }.toSet()
        val nearby = snapshot.nearbySites
            .filter { it.faaId !in insideIds }
            .filter { site -> site.polygons.any { GeoMath.distanceToPolygonNm(latitude, longitude, it) <= cautionRadiusNm } }
            .distinctBy { it.faaId }
            .map { it.copy(polygons = emptyList()) }
        val mapZones = snapshot.nearbySites.flatMap { site ->
            site.polygons.withIndex().map { (i, ring) ->
                AirspaceZone(
                    id = "NSUAS-${site.faaId}-$i",
                    name = site.name,
                    type = AirspaceZoneType.RESTRICTED_ZONE,
                    centerLat = ring.map { it.first }.average(),
                    centerLon = ring.map { it.second }.average(),
                    radiusMeters = 0.0,
                    floorFt = 0.0,
                    ceilingFt = 400.0,
                    description = "National security UAS restriction (14 CFR 99.7), ${site.floorText} to ${site.ceilingText}" +
                        (if (site.agency.isNotBlank()) " — ${site.agency}" else ""),
                    polygonCoordinates = ring,
                    floorReference = AltitudeReference.AGL
                )
            }
        }
        return SecurityAirspaceEvaluation(
            sitesAtLaunchPoint = atLaunch.map { it.copy(polygons = emptyList()) },
            sitesNearby = nearby,
            dcSfraZone = snapshot.dcSfraZone,
            mapZones = mapZones
        )
    }
}

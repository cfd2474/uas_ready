package com.taksolutions.uasready.ui.presentation

import com.taksolutions.uasready.domain.geo.GeoMath
import com.taksolutions.uasready.domain.model.AirportWarningZone
import com.taksolutions.uasready.domain.model.AirspaceZone
import com.taksolutions.uasready.domain.model.AirspaceZoneType

/** What a tapped map point lies inside, among the layers currently shown. */
data class InspectionHits(
    val zones: List<AirspaceZone>,
    val warningZones: List<AirportWarningZone>
)

/** Pure: the map's tap inspection, independent of the map library. */
object AirspaceInspector {

    fun inspect(
        latitude: Double,
        longitude: Double,
        zones: List<AirspaceZone>,
        enabledTypes: Set<AirspaceZoneType>,
        warningZones: List<AirportWarningZone>,
        showWarningZones: Boolean
    ): InspectionHits = InspectionHits(
        zones = zones.filter { it.type in enabledTypes && contains(it, latitude, longitude) },
        warningZones = if (showWarningZones) {
            warningZones.filter { GeoMath.isPointInPolygon(latitude, longitude, it.polygonCoordinates) }
        } else emptyList()
    )

    /** Polygon zones by ring; zones without a ring are circles of [AirspaceZone.radiusMeters]. */
    fun contains(zone: AirspaceZone, latitude: Double, longitude: Double): Boolean =
        if (zone.polygonCoordinates.size >= 3) GeoMath.isPointInPolygon(latitude, longitude, zone.polygonCoordinates)
        else GeoMath.distanceMeters(latitude, longitude, zone.centerLat, zone.centerLon) <= zone.radiusMeters
}

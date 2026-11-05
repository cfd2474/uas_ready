package com.taksolutions.uasready.domain.engine

import com.taksolutions.uasready.domain.geo.GeoMath
import com.taksolutions.uasready.domain.model.*

/**
 * Decides which airspace applies to a flight at a launch point.
 *
 * A zone is relevant when its floor is below the planned altitude above ground:
 *  - AGL floors (FAA LOWER_CODE = SFC) compare directly.
 *  - MSL floors (incl. flight levels converted to MSL) compare against ground elevation.
 *    When ground elevation is unknown, MSL floors are treated as possibly low (conservative:
 *    may over-report airspace, never under-report) and the result carries groundElevationFt = null.
 *
 * Pure function of its inputs; no I/O.
 */
class AirspaceClassifier {

    fun classify(
        snapshot: AirspaceSnapshot,
        latitude: Double,
        longitude: Double,
        groundElevationFt: Double?,
        plannedAltitudeAglFt: Double
    ): AirspaceInfo {
        val relevant = snapshot.zones.filter { floorAglFt(it, groundElevationFt) < plannedAltitudeAglFt }
        val containing = relevant.filter { GeoMath.isPointInPolygon(latitude, longitude, it.polygonCoordinates) }

        val controlledClass = containing.mapNotNull { it.airspaceClass }.maxByOrNull { priority(it) }
        val specialUse = containing.filter { it.specialUseType != null }
            .minByOrNull { if (it.specialUseType == SpecialUseType.PROHIBITED) 0 else 1 }

        val primaryClass = when {
            specialUse != null -> AirspaceClass.SPECIAL_USE
            controlledClass != null -> controlledClass
            else -> AirspaceClass.CLASS_G
        }

        return AirspaceInfo(
            primaryClass = primaryClass,
            controlledAirspaceAuthorizationRequired = controlledClass != null || specialUse != null,
            uasFacilityMapMaxAltitudeFt = null,
            activeTfrs = emptyList(),
            tfrDataSourced = false,
            zones = relevant,
            specialUseAirspaceActive = specialUse != null,
            specialUseName = specialUse?.name,
            specialUseType = specialUse?.specialUseType,
            controlledClass = controlledClass,
            groundElevationFt = groundElevationFt,
            timestampEpochMs = snapshot.fetchedEpochMs,
            sourceName = snapshot.sourceName,
            isStale = snapshot.isCached
        )
    }

    /** Floor height above ground at the launch point; 0 when an MSL floor cannot be evaluated. */
    fun floorAglFt(zone: AirspaceZone, groundElevationFt: Double?): Double = when (zone.floorReference) {
        AltitudeReference.AGL -> zone.floorFt
        AltitudeReference.MSL -> if (groundElevationFt != null) zone.floorFt - groundElevationFt else 0.0
    }

    private fun priority(c: AirspaceClass) = when (c) {
        AirspaceClass.CLASS_B -> 5
        AirspaceClass.CLASS_C -> 4
        AirspaceClass.CLASS_D -> 3
        AirspaceClass.CLASS_E_SURFACE -> 2
        AirspaceClass.CLASS_E -> 1
        AirspaceClass.CLASS_G, AirspaceClass.SPECIAL_USE -> 0
    }
}

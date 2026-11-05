package com.taksolutions.uasready.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AirspaceClass {
    CLASS_B,
    CLASS_C,
    CLASS_D,
    CLASS_E_SURFACE,
    CLASS_E,
    CLASS_G,
    SPECIAL_USE
}

@Serializable
enum class AirspaceZoneType {
    RESTRICTED_ZONE,       // Red: TFRs, Prohibited P-areas, Strict No-Fly
    AUTHORIZATION_ZONE,    // Blue: Class B, C, D Controlled Airspace (LAANC authorization required)
    WARNING_ZONE,          // Amber: 5 NM Airport vicinity buffer, Class E surface, Wildlife
    ALTITUDE_ZONE,         // Cyan: UAS Facility Map altitude restriction grids
    SPECIAL_USE            // Orange: Military Operations Areas (MOA), Alert areas
}

/** What an airspace floor value is measured from. Flight levels are converted to MSL feet. */
@Serializable
enum class AltitudeReference {
    AGL, // above ground level (FAA LOWER_CODE = SFC)
    MSL  // above mean sea level (FAA LOWER_CODE = MSL, or LOWER_UOM = FL converted ×100)
}

/** Special use airspace types that restrict flight at the launch point. */
@Serializable
enum class SpecialUseType {
    PROHIBITED, // FAA TYPE_CODE P — flight prohibited (14 CFR 73.83)
    RESTRICTED  // FAA TYPE_CODE R — flight requires controlling/using agency permission when active (14 CFR 73.13)
}

@Serializable
data class AirspaceZone(
    val id: String,
    val name: String,
    val type: AirspaceZoneType,
    val centerLat: Double,
    val centerLon: Double,
    val radiusMeters: Double,
    /** Floor in feet, measured from [floorReference]. */
    val floorFt: Double = 0.0,
    val ceilingFt: Double? = null,
    val description: String = "",
    val polygonCoordinates: List<Pair<Double, Double>> = emptyList(),
    val floorReference: AltitudeReference = AltitudeReference.AGL,
    /** Controlled airspace class for FAA Class_Airspace zones; null for special use zones. */
    val airspaceClass: AirspaceClass? = null,
    /** Set for Prohibited/Restricted special use zones; null for MOA/Alert/Warning and class zones. */
    val specialUseType: SpecialUseType? = null
)

/**
 * Raw airspace candidates around a point, as fetched from a source, before deciding what applies
 * at the launch point. Classification needs ground elevation and planned altitude, which the
 * source does not know — see AirspaceClassifier.
 */
@Serializable
data class AirspaceSnapshot(
    val centerLat: Double,
    val centerLon: Double,
    val zones: List<AirspaceZone>,
    val fetchedEpochMs: Long,
    val sourceName: String,
    /** True when served from the on-device cache because the live source was unavailable. */
    val isCached: Boolean = false
)

@Serializable
data class TemporaryFlightRestriction(
    val id: String,
    val description: String,
    val type: String, // VIP, Hazard, Security, Stadium
    val minAltitudeFt: Double,
    val maxAltitudeFt: Double,
    val effectiveStartEpochMs: Long,
    val effectiveEndEpochMs: Long,
    val radiusNm: Double,
    val centerLat: Double? = null,
    val centerLon: Double? = null
) {
    fun isActiveAt(timeMs: Long): Boolean {
        return timeMs in effectiveStartEpochMs..effectiveEndEpochMs
    }
}

@Serializable
data class NoticeToAirmen(
    val id: String,
    val text: String,
    val issuedEpochMs: Long,
    val expiresEpochMs: Long? = null,
    val isCritical: Boolean = false
)

@Serializable
data class AirspaceInfo(
    val primaryClass: AirspaceClass = AirspaceClass.CLASS_G,
    val controlledAirspaceAuthorizationRequired: Boolean = false,
    val uasFacilityMapMaxAltitudeFt: Double? = null, // Max auto-approved LAANC ceiling (0 - 400 ft)
    val activeTfrs: List<TemporaryFlightRestriction> = emptyList(),
    /**
     * True only when [activeTfrs] came from an authoritative TFR source. When false, an empty
     * [activeTfrs] means "not checked", NOT "no TFRs". Defaults to false so no producer can
     * claim TFR coverage by accident.
     */
    val tfrDataSourced: Boolean = false,
    /** TFR areas containing the launch point whose daily schedule does not cover the flight window. */
    val scheduledTfrsOutsideWindow: List<TemporaryFlightRestriction> = emptyList(),
    /** TFRs with no published boundary that may apply here (same state, or launch state unknown). */
    val unmappedRegionalTfrs: List<TemporaryFlightRestriction> = emptyList(),
    /** Nationwide TFR notices with no boundary (e.g. 14 CFR 99.7 national-security UAS sites). */
    val unmappedNationwideTfrCount: Int = 0,
    val zones: List<AirspaceZone> = emptyList(),
    val notams: List<NoticeToAirmen> = emptyList(),
    val specialUseAirspaceActive: Boolean = false,
    val specialUseName: String? = null,
    /** Prohibited/Restricted area containing the launch point, if any. */
    val specialUseType: SpecialUseType? = null,
    /** Highest controlled class (B/C/D/E-surface) containing the launch point, independent of special use. */
    val controlledClass: AirspaceClass? = null,
    /** Ground elevation used to evaluate MSL floors; null = unknown (MSL floors treated as possibly low). */
    val groundElevationFt: Double? = null,
    val nearestAirportCode: String? = null,
    val nearestAirportDistanceNm: Double? = null,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val sourceName: String = "FAA Live Airspace",
    val isStale: Boolean = false
) {
    fun hasActiveTfr(timeMs: Long = System.currentTimeMillis()): Boolean {
        return activeTfrs.any { it.isActiveAt(timeMs) }
    }
}

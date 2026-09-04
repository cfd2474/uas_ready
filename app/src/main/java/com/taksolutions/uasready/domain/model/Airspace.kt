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

@Serializable
data class AirspaceZone(
    val id: String,
    val name: String,
    val type: AirspaceZoneType,
    val centerLat: Double,
    val centerLon: Double,
    val radiusMeters: Double,
    val floorFt: Double = 0.0,
    val ceilingFt: Double? = null,
    val description: String = "",
    val polygonCoordinates: List<Pair<Double, Double>> = emptyList()
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
    val zones: List<AirspaceZone> = emptyList(),
    val notams: List<NoticeToAirmen> = emptyList(),
    val specialUseAirspaceActive: Boolean = false,
    val specialUseName: String? = null,
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

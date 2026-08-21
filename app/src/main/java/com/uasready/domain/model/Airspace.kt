package com.uasready.domain.model

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
data class TemporaryFlightRestriction(
    val id: String,
    val description: String,
    val type: String, // VIP, Hazard, Security, Stadium
    val minAltitudeFt: Double,
    val maxAltitudeFt: Double,
    val effectiveStartEpochMs: Long,
    val effectiveEndEpochMs: Long,
    val radiusNm: Double
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

package com.taksolutions.uasready.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SunData(
    val civilDawnEpochMs: Long,
    val sunriseEpochMs: Long,
    val sunsetEpochMs: Long,
    val civilDuskEpochMs: Long,
    val isDaylight: Boolean,
    val daylightRemainingMinutes: Long,
    val isCivilTwilight: Boolean = false,
    val sourceName: String = "NOAA Solar Calculator"
) {
    /**
     * Checks if a given time is in full darkness (after civil dusk or before civil dawn).
     */
    fun isDarknessAt(timeMs: Long): Boolean {
        return timeMs < civilDawnEpochMs || timeMs > civilDuskEpochMs
    }

    /**
     * Checks if a given time is in civil twilight (between dawn & sunrise, or sunset & dusk).
     */
    fun isCivilTwilightAt(timeMs: Long): Boolean {
        return (timeMs in civilDawnEpochMs until sunriseEpochMs) ||
                (timeMs in sunsetEpochMs..civilDuskEpochMs)
    }

    /**
     * Checks if a given time is in daylight (between sunrise and sunset).
     */
    fun isDaylightAt(timeMs: Long): Boolean {
        return timeMs in sunriseEpochMs..sunsetEpochMs
    }
}

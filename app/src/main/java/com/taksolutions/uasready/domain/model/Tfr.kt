package com.taksolutions.uasready.domain.model

import kotlinx.serialization.Serializable

/** A UTC time window [startEpochMs, endEpochMs]. A null bound is open (permanent / until further notice). */
@Serializable
data class TimeWindow(val startEpochMs: Long?, val endEpochMs: Long?) {
    fun overlaps(start: Long, end: Long): Boolean =
        (startEpochMs == null || startEpochMs <= end) && (endEpochMs == null || endEpochMs >= start)
}

/**
 * One area of a TFR NOTAM (FAA XNOTAM `aseTFRArea` + its `abdMergedArea` polygon).
 * Semantics: see docs/PLATFORM_REFERENCE.md "FAA TFR feed".
 */
@Serializable
data class TfrArea(
    val name: String,
    val floorFt: Double,
    val floorReference: AltitudeReference,
    val ceilingFt: Double?,
    val ceilingReference: AltitudeReference?,
    /** Closed ring of (latitude, longitude); empty when the FAA published no geometry. */
    val polygon: List<Pair<Double, Double>>,
    /** Date envelopes when the area is in effect; empty = use the NOTAM's own effective period. */
    val windows: List<TimeWindow> = emptyList(),
    /**
     * Daily UTC time-of-day schedule inside [windows] (`isScheduledTfrArea=TRUE`), minutes after
     * midnight UTC. Null = active for the whole window.
     */
    val dailyStartMinuteUtc: Int? = null,
    val dailyEndMinuteUtc: Int? = null
)

@Serializable
data class TfrNotice(
    val notamId: String,
    /** FAA list type: SECURITY, HAZARDS, VIP, AIR SHOWS/SPORTS, UAS PUBLIC GATHERING, SPECIAL… */
    val type: String,
    val description: String,
    /** FAA list state: a two-letter code, or "USA" for nationwide notices. */
    val state: String,
    val cfrSection: String? = null,
    val effective: TimeWindow = TimeWindow(null, null),
    val areas: List<TfrArea> = emptyList(),
    /** FAA list `mod_abs_time`; a change means the NOTAM was amended. */
    val modifiedStamp: String = "",
    /** Opening of the NOTAM text; identifies notices evaluated through other FAA layers. */
    val textHeader: String = "",
    /** States named in the NOTAM text (the list `state` holds only one). */
    val textStates: List<String> = emptyList(),
    /** TfrXmlParser.VERSION that produced this entry; older cached entries are re-downloaded. */
    val parserVersion: Int = 0
) {
    val hasGeometry: Boolean get() = areas.any { it.polygon.size >= 3 }
    val isNationwide: Boolean get() = state.equals("USA", ignoreCase = true)
}

/** Every current TFR from the FAA feed. Only produced when the whole feed was read successfully. */
@Serializable
data class TfrSnapshot(
    val notices: List<TfrNotice>,
    val fetchedEpochMs: Long,
    val sourceName: String = "FAA TFR (tfr.faa.gov)"
)

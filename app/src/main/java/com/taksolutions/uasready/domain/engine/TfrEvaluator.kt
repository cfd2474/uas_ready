package com.taksolutions.uasready.domain.engine

import com.taksolutions.uasready.domain.geo.GeoMath
import com.taksolutions.uasready.domain.model.*

/** What the TFR feed means for one launch point, altitude and flight window. */
data class TfrEvaluation(
    /** Mapped TFR areas containing the launch point, in effect during the window, floor below planned altitude. */
    val active: List<TemporaryFlightRestriction>,
    /** As [active], but the area's daily schedule does not overlap the window. Still worth a caution. */
    val scheduledOutsideWindow: List<TemporaryFlightRestriction>,
    /**
     * Regional TFRs with no published boundary that may cover the launch state (NOTAM text states, not
     * just the list's single state) — or every such TFR when the launch state is unknown. Notices
     * evaluated through another FAA layer are left out while that layer answers.
     */
    val unmappedRegional: List<TemporaryFlightRestriction>,
    /** Nationwide notices (state "USA") with no boundary, e.g. the 14 CFR 99.7 national-security sites notice. */
    val unmappedNationwideCount: Int,
    /** Mapped TFR areas near the launch point that matter during the window, for the map. */
    val mapZones: List<AirspaceZone>
)

/**
 * Pure: decides which TFRs apply. Conservative where the feed is ambiguous —
 * unknown ground elevation treats MSL floors as low; non-daily day codes are treated as active for the
 * whole window; open-ended effective dates mean "in effect".
 */
class TfrEvaluator(private val mapRadiusLatDeg: Double = 0.55) {

    fun evaluate(
        snapshot: TfrSnapshot,
        latitude: Double,
        longitude: Double,
        groundElevationFt: Double?,
        plannedAltitudeAglFt: Double,
        window: FlightWindow,
        launchStateCode: String?,
        /** True when the FAA national-security and DC SFRA layers answered; see [UnmappedTfrCoverage]. */
        securityLayersSourced: Boolean = false
    ): TfrEvaluation {
        val active = mutableListOf<TemporaryFlightRestriction>()
        val scheduled = mutableListOf<TemporaryFlightRestriction>()
        val mapZones = mutableListOf<AirspaceZone>()
        val lonHalf = GeoMath.longitudeHalfWidthDeg(mapRadiusLatDeg, latitude)

        for (notice in snapshot.notices) {
            notice.areas.forEachIndexed { index, area ->
                if (area.polygon.size < 3) return@forEachIndexed
                if (floorAglFt(area, groundElevationFt) >= plannedAltitudeAglFt) return@forEachIndexed
                val timing = timing(notice, area, window.startEpochMs, window.endEpochMs)
                if (timing == Timing.NOT_IN_EFFECT) return@forEachIndexed

                val near = area.polygon.any { (lat, lon) ->
                    kotlin.math.abs(lat - latitude) <= mapRadiusLatDeg && kotlin.math.abs(lon - longitude) <= lonHalf
                }
                if (near) mapZones.add(toZone(notice, area, index))

                if (GeoMath.isPointInPolygon(latitude, longitude, area.polygon)) {
                    val tfr = toTfr(notice, area)
                    if (timing == Timing.ACTIVE) active.add(tfr) else scheduled.add(tfr)
                }
            }
        }

        val unmapped = snapshot.notices.filter { !it.hasGeometry }
        val regional = unmapped.filter { !it.isNationwide }
            .filter { !(securityLayersSourced && UnmappedTfrCoverage.of(it) != TfrCoverage.NONE) }
            .filter { launchStateCode == null || launchStateCode.uppercase() in UnmappedTfrCoverage.statesOf(it) }
            .filter { notice -> windowsOf(notice, null).any { it.overlaps(window.startEpochMs, window.endEpochMs) } }
            .map { toTfr(it, null) }

        return TfrEvaluation(
            active = active.distinctBy { it.id },
            scheduledOutsideWindow = scheduled.distinctBy { it.id }.filter { s -> active.none { it.id == s.id } },
            unmappedRegional = regional,
            unmappedNationwideCount = unmapped.count { it.isNationwide },
            mapZones = mapZones
        )
    }

    fun floorAglFt(area: TfrArea, groundElevationFt: Double?): Double = when (area.floorReference) {
        AltitudeReference.AGL -> area.floorFt
        AltitudeReference.MSL -> if (groundElevationFt == null) 0.0 else area.floorFt - groundElevationFt
    }

    private enum class Timing { ACTIVE, SCHEDULED_INACTIVE, NOT_IN_EFFECT }

    private fun windowsOf(notice: TfrNotice, area: TfrArea?): List<TimeWindow> =
        area?.windows?.takeIf { it.isNotEmpty() } ?: listOf(notice.effective)

    private fun timing(notice: TfrNotice, area: TfrArea, start: Long, end: Long): Timing {
        val envelopes = windowsOf(notice, area).filter { it.overlaps(start, end) }
        if (envelopes.isEmpty()) return Timing.NOT_IN_EFFECT
        val dailyStart = area.dailyStartMinuteUtc ?: return Timing.ACTIVE
        val dailyEnd = area.dailyEndMinuteUtc ?: return Timing.ACTIVE
        val active = envelopes.any { env ->
            dailyWindowOverlaps(env, dailyStart, dailyEnd, start, end)
        }
        return if (active) Timing.ACTIVE else Timing.SCHEDULED_INACTIVE
    }

    /** Daily UTC window [dailyStart, dailyEnd] (crossing midnight when end ≤ start), clipped to [env]. */
    private fun dailyWindowOverlaps(env: TimeWindow, dailyStart: Int, dailyEnd: Int, start: Long, end: Long): Boolean {
        val firstDay = Math.floorDiv(start, DAY_MS) - 1 // previous day: its window may cross midnight
        val lastDay = Math.floorDiv(end, DAY_MS)
        for (day in firstDay..lastDay) {
            val wStart = day * DAY_MS + dailyStart * MINUTE_MS
            var wEnd = day * DAY_MS + dailyEnd * MINUTE_MS
            if (dailyEnd <= dailyStart) wEnd += DAY_MS
            val clippedStart = maxOf(wStart, env.startEpochMs ?: Long.MIN_VALUE)
            val clippedEnd = minOf(wEnd, env.endEpochMs ?: Long.MAX_VALUE)
            if (clippedStart <= clippedEnd && clippedStart <= end && clippedEnd >= start) return true
        }
        return false
    }

    private fun toTfr(notice: TfrNotice, area: TfrArea?): TemporaryFlightRestriction {
        val envelope = area?.windows?.takeIf { it.isNotEmpty() } ?: listOf(notice.effective)
        val schedule = if (area?.dailyStartMinuteUtc != null && area.dailyEndMinuteUtc != null)
            " (daily ${hhmm(area.dailyStartMinuteUtc)}–${hhmm(area.dailyEndMinuteUtc)} UTC)" else ""
        val floor = area?.let { " ${altitudeText(it.floorFt, it.floorReference)} to " +
            (it.ceilingFt?.let { c -> altitudeText(c, it.ceilingReference ?: AltitudeReference.MSL) } ?: "unspecified") } ?: ""
        return TemporaryFlightRestriction(
            id = notice.notamId,
            description = notice.description.take(160) + floor + schedule +
                (notice.cfrSection?.let { " — 14 CFR § $it" } ?: ""),
            type = notice.type,
            minAltitudeFt = area?.floorFt ?: 0.0,
            maxAltitudeFt = area?.ceilingFt ?: 0.0,
            effectiveStartEpochMs = envelope.minOf { it.startEpochMs ?: Long.MIN_VALUE },
            effectiveEndEpochMs = envelope.maxOf { it.endEpochMs ?: Long.MAX_VALUE },
            radiusNm = 0.0
        )
    }

    private fun toZone(notice: TfrNotice, area: TfrArea, index: Int): AirspaceZone {
        val lat = area.polygon.map { it.first }.average()
        val lon = area.polygon.map { it.second }.average()
        return AirspaceZone(
            id = "TFR-${notice.notamId}-$index",
            name = "TFR ${notice.notamId} ${area.name}",
            type = AirspaceZoneType.RESTRICTED_ZONE,
            centerLat = lat,
            centerLon = lon,
            radiusMeters = 0.0,
            floorFt = area.floorFt,
            ceilingFt = area.ceilingFt,
            description = "${notice.type}: ${notice.description.take(120)}",
            polygonCoordinates = area.polygon,
            floorReference = area.floorReference
        )
    }

    private fun altitudeText(ft: Double, ref: AltitudeReference): String = when {
        ref == AltitudeReference.AGL && ft <= 0.0 -> "surface"
        ref == AltitudeReference.AGL -> "${ft.toInt()} ft AGL"
        else -> "${ft.toInt()} ft MSL"
    }

    private fun hhmm(minutes: Int) = String.format(java.util.Locale.US, "%02d%02d", minutes / 60, minutes % 60)

    private companion object {
        const val MINUTE_MS = 60_000L
        const val DAY_MS = 24 * 60 * MINUTE_MS
    }
}

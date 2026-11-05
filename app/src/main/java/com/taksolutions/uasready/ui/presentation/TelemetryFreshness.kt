package com.taksolutions.uasready.ui.presentation

/** How much of the last refresh came from live sources. */
enum class TelemetryFreshness {
    /** Every live source answered. */
    LIVE,
    /** Some live sources answered; others failed or fell back to cached data. */
    PARTIAL,
    /** No live source answered. */
    OFFLINE;

    companion object {
        /** @param sourcesLive one entry per live source: true when it returned live (not cached) data. */
        fun of(sourcesLive: Collection<Boolean>): TelemetryFreshness = when {
            sourcesLive.isNotEmpty() && sourcesLive.all { it } -> LIVE
            sourcesLive.any { it } -> PARTIAL
            else -> OFFLINE
        }
    }
}

/** Visual severity of the header status chip. */
enum class ChipSeverity { OK, CAUTION, ALERT }

data class FreshnessChip(val text: String, val severity: ChipSeverity)

/**
 * Header chip text. The elapsed time is measured from the last refresh that brought live data,
 * so going offline never shows a reassuring green "0m".
 */
object FreshnessChipText {
    private const val STALE_AFTER_MINUTES = 5

    fun of(freshness: TelemetryFreshness?, minutesSinceLiveData: Int?): FreshnessChip {
        val age = minutesSinceLiveData?.let(::formatAge)
        return when (freshness) {
            null -> FreshnessChip("Awaiting data", ChipSeverity.CAUTION)
            TelemetryFreshness.OFFLINE ->
                FreshnessChip(if (age != null) "Offline • live data $age ago" else "Offline • no live data", ChipSeverity.ALERT)
            TelemetryFreshness.PARTIAL -> FreshnessChip("Partial data • ${age ?: "0m"}", ChipSeverity.CAUTION)
            TelemetryFreshness.LIVE -> {
                val minutes = minutesSinceLiveData ?: 0
                FreshnessChip(
                    "Last update • ${formatAge(minutes)}",
                    if (minutes >= STALE_AFTER_MINUTES) ChipSeverity.CAUTION else ChipSeverity.OK
                )
            }
        }
    }

    private fun formatAge(minutes: Int): String = if (minutes < 60) "${minutes}m" else "${minutes / 60}h"
}

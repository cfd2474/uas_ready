package com.uasready.data.nasr

import java.text.SimpleDateFormat
import java.util.*

object AiracCycleCalculator {

    // Known 28-day AIRAC anchor epoch: 2026-01-22 00:00:00 UTC (Cycle 2601)
    private const val ANCHOR_EPOCH_MS = 1769040000000L // 2026-01-22 00:00:00 UTC
    private const val CYCLE_DURATION_MS = 28L * 24L * 60L * 60L * 1000L // 28 days

    private val DATE_FORMAT = SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Calculates current AIRAC cycle for a given epoch timestamp.
     */
    fun calculateCycleInfo(nowEpochMs: Long = System.currentTimeMillis()): AiracCycleInfo {
        val deltaMs = nowEpochMs - ANCHOR_EPOCH_MS
        val cycleIndex = (deltaMs / CYCLE_DURATION_MS).toInt()
        val effectiveMs = ANCHOR_EPOCH_MS + (cycleIndex * CYCLE_DURATION_MS)
        val expireMs = effectiveMs + CYCLE_DURATION_MS

        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = effectiveMs
        }
        val year = cal.get(Calendar.YEAR) % 100
        val cycleNum = (cycleIndex % 13) + 1
        val cycleName = String.format(Locale.US, "%02d%02d", year, cycleNum)

        val daysUntilExpiry = ((expireMs - nowEpochMs) / (24L * 60L * 60L * 1000L)).coerceAtLeast(1).toInt()

        return AiracCycleInfo(
            cycleName = cycleName,
            effectiveEpochMs = effectiveMs,
            expireEpochMs = expireMs,
            lastCheckedEpochMs = nowEpochMs,
            lastUpdatedEpochMs = effectiveMs,
            isExpired = false,
            daysUntilExpiry = daysUntilExpiry
        )
    }

    fun formatDate(epochMs: Long): String {
        return DATE_FORMAT.format(Date(epochMs))
    }

    /**
     * Returns the download URL for the FAA 28-day NASR subscription package.
     */
    fun getNasrSubscriptionUrl(cycleName: String): String {
        return "https://nfdc.faa.gov/webContent/28Day-Data/28Day_Subscription_$cycleName.zip"
    }
}

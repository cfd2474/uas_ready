package com.taksolutions.uasready.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FlightWindow(
    val startEpochMs: Long,
    val endEpochMs: Long
) {
    val durationMinutes: Long
        get() = (endEpochMs - startEpochMs).coerceAtLeast(0) / (60 * 1000)

    /**
     * Generates sampling timestamps throughout the flight window to evaluate forecast conditions.
     */
    fun getSamplingIntervals(intervalMinutes: Int = 30): List<Long> {
        if (endEpochMs <= startEpochMs) return listOf(startEpochMs)
        val intervals = mutableListOf<Long>()
        val stepMs = intervalMinutes * 60 * 1000L
        var current = startEpochMs
        while (current < endEpochMs) {
            intervals.add(current)
            current += stepMs
        }
        if (!intervals.contains(endEpochMs)) {
            intervals.add(endEpochMs)
        }
        return intervals
    }

    companion object {
        fun defaultTwoHours(fromMs: Long = System.currentTimeMillis()): FlightWindow {
            return FlightWindow(
                startEpochMs = fromMs,
                endEpochMs = fromMs + 2 * 60 * 60 * 1000L
            )
        }
    }
}

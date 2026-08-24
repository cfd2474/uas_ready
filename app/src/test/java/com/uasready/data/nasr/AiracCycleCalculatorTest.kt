package com.uasready.data.nasr

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class AiracCycleCalculatorTest {

    @Test
    fun testCalculateCycleInfo_AnchorCycle2601() {
        // Anchor epoch: 2026-01-22 00:00:00 UTC
        val anchorMs = 1769040000000L
        val info = AiracCycleCalculator.calculateCycleInfo(anchorMs)

        assertEquals("2601", info.cycleName)
        assertEquals(anchorMs, info.effectiveEpochMs)
        assertEquals(anchorMs + 28L * 86400000L, info.expireEpochMs)
        assertFalse(info.isExpired)
        assertEquals(28, info.daysUntilExpiry)
    }

    @Test
    fun testCalculateCycleInfo_MidYearCycle() {
        // Test August 2026 (~210 days after anchor)
        val testEpochMs = 1769040000000L + (7 * 28L * 86400000L) + (5 * 86400000L) // Cycle 2608 + 5 days
        val info = AiracCycleCalculator.calculateCycleInfo(testEpochMs)

        assertEquals("2608", info.cycleName)
        assertFalse(info.isExpired)
        assertEquals(23, info.daysUntilExpiry)
    }

    @Test
    fun testFormatDate() {
        val anchorMs = 1769040000000L // 2026-01-22
        val formatted = AiracCycleCalculator.formatDate(anchorMs)
        assertTrue(formatted.contains("2026"))
        assertTrue(formatted.contains("Jan") || formatted.contains("22"))
    }

    @Test
    fun testGetNasrSubscriptionUrl() {
        val url = AiracCycleCalculator.getNasrSubscriptionUrl("2608")
        assertTrue(url.startsWith("https://nfdc.faa.gov/"))
        assertTrue(url.endsWith("28Day_Subscription_2608.zip"))
    }
}

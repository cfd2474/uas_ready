package com.uasready.domain.model

import org.junit.Assert.*
import org.junit.Test

class DomainModelsTest {

    @Test
    fun testAircraftPresetsLoaded() {
        val presets = Aircraft.PRESETS
        assertTrue(presets.isNotEmpty())
        val m3t = presets.firstOrNull { it.id == "dji_m3t" }
        assertNotNull(m3t)
        assertEquals(34.0, m3t!!.limitations.maxGustSpeedMph, 0.01)
        assertEquals(14.0, m3t.limitations.minOperatingTempF, 0.01)
        assertEquals(104.0, m3t.limitations.maxOperatingTempF, 0.01)
    }

    @Test
    fun testPilotAuthorityDistinction() {
        val pilot107 = Pilot(activeAuthority = PilotAuthorityType.PART_107)
        assertEquals(PilotAuthorityType.PART_107, pilot107.activeAuthority)
        assertEquals("107 License", pilot107.activeAuthority.displayName)

        val pilotCoa = Pilot(activeAuthority = PilotAuthorityType.PUBLIC_COA)
        assertEquals(PilotAuthorityType.PUBLIC_COA, pilotCoa.activeAuthority)
        assertEquals("Public COA", pilotCoa.activeAuthority.displayName)
    }

    @Test
    fun testSpaceWeatherCalculation() {
        assertEquals(GeomagneticStormScale.NONE, SpaceWeather.calculateStormScale(3.0))
        assertEquals(GeomagneticStormScale.G1_MINOR, SpaceWeather.calculateStormScale(5.0))
        assertEquals(GeomagneticStormScale.G3_STRONG, SpaceWeather.calculateStormScale(7.0))
        assertEquals(GnssRiskLevel.LOW, SpaceWeather.calculateGnssRisk(2.5))
        assertEquals(GnssRiskLevel.MODERATE, SpaceWeather.calculateGnssRisk(4.5))
        assertEquals(GnssRiskLevel.SEVERE, SpaceWeather.calculateGnssRisk(7.5))
    }

    @Test
    fun testFlightWindowSampling() {
        val now = 1700000000000L
        val window = FlightWindow(
            startEpochMs = now,
            endEpochMs = now + 90 * 60 * 1000L // 90 minutes
        )
        assertEquals(90L, window.durationMinutes)
        val intervals = window.getSamplingIntervals(intervalMinutes = 30)
        assertEquals(4, intervals.size) // 0 min, 30 min, 60 min, 90 min
    }

    @Test
    fun testCsvChecklistParsing() {
        val csv = """
            Item Title, Description, IsCritical
            Check Propellers, Inspect leading edges for fractures, true
            Calibrate Compass, Away from ferrous metals, false
            Confirm Home Point, Verify RTH coordinates on map, true
        """.trimIndent()

        val checklist = ChecklistGroup.parseFromCsv("Pre-Launch Quick Check", csv)
        assertEquals("Pre-Launch Quick Check", checklist.title)
        assertEquals(3, checklist.items.size)
        assertEquals("Check Propellers", checklist.items[0].title)
        assertTrue(checklist.items[0].isCritical)
        assertEquals("Calibrate Compass", checklist.items[1].title)
        assertFalse(checklist.items[1].isCritical)
    }
}

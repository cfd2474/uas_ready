package com.uasready.domain.model

import org.junit.Assert.*
import org.junit.Test

class DomainModelsTest {

    @Test
    fun testAircraftPresetsLoaded() {
        val presets = Aircraft.PRESETS
        assertTrue(presets.isNotEmpty())
        val m350 = presets.firstOrNull { it.id == "dji_m350_rtk" }
        assertNotNull(m350)
        assertEquals(26.8, m350!!.limitations.maxSustainedWindSpeedMph, 0.01)
        assertEquals(-4.0, m350.limitations.minOperatingTempF, 0.01)
        assertEquals(122.0, m350.limitations.maxOperatingTempF, 0.01)
        assertEquals("IP55", m350.limitations.ipRating)

        val m3e3t = presets.firstOrNull { it.id == "dji_m3e_3t" }
        assertNotNull(m3e3t)
        assertEquals(26.8, m3e3t!!.limitations.maxSustainedWindSpeedMph, 0.01)
        assertEquals(14.0, m3e3t.limitations.minOperatingTempF, 0.01)
        assertEquals(104.0, m3e3t.limitations.maxOperatingTempF, 0.01)
    }

    @Test
    fun testPilotAuthorityDistinction() {
        val pilot107 = Pilot(activeAuthority = PilotAuthorityType.PART_107)
        assertEquals(PilotAuthorityType.PART_107, pilot107.activeAuthority)
        assertEquals("Licensed Pilot", pilot107.activeAuthority.displayName)

        val pilotCoa = Pilot(activeAuthority = PilotAuthorityType.PUBLIC_COA)
        assertEquals(PilotAuthorityType.PUBLIC_COA, pilotCoa.activeAuthority)
        assertEquals("Non-licensed Pilot", pilotCoa.activeAuthority.displayName)
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
    fun testEmergencyProceduresLoaded() {
        val procs = EmergencyProcedure.DEFAULT_PROCEDURES
        assertEquals(10, procs.size)
        assertEquals("Return to Home (RTH)", procs[0].title)
        assertEquals(1, procs[0].stepNumber)
        assertEquals("Emergency Landing", procs[1].title)
        assertEquals("Post-Incident Inspection", procs[9].title)
        assertEquals(10, procs[9].stepNumber)
    }

    @Test
    fun testChecklistGroupItemAddition() {
        val preflight = ChecklistGroup.DEFAULT_CHECKLISTS.first { it.category == ChecklistCategory.PREFLIGHT }
        val initialSize = preflight.items.size
        val customItem = ChecklistItem(
            id = "custom_thermal_1",
            title = "FLIR Thermal Calibration",
            description = "Perform flat-field calibration before flight",
            isCritical = true
        )
        val updated = preflight.copy(items = preflight.items + customItem)
        assertEquals(initialSize + 1, updated.items.size)
        assertTrue(updated.items.last().isCritical)
        assertEquals("FLIR Thermal Calibration", updated.items.last().title)
    }
}

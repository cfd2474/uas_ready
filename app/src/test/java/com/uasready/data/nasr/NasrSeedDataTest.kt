package com.uasready.data.nasr

import com.uasready.domain.model.AirspaceClass
import org.junit.Assert.*
import org.junit.Test

class NasrSeedDataTest {

    @Test
    fun testSeedAirportsContainRequiredFacilities() {
        val airports = NasrSeedData.getSeedAirports()
        assertTrue(airports.isNotEmpty())

        val kajo = airports.find { it.icaoId == "KAJO" }
        assertNotNull("KAJO should exist in seed data", kajo)
        assertEquals("122.700", kajo?.ctafFreq)

        val f70 = airports.find { it.icaoId == "F70" }
        assertNotNull("F70 should exist in seed data", f70)
        assertEquals("122.800", f70?.ctafFreq)

        val kont = airports.find { it.icaoId == "KONT" }
        assertNotNull("KONT should exist in seed data", kont)
        assertEquals("120.600", kont?.towerFreq)

        val klax = airports.find { it.icaoId == "KLAX" }
        assertNotNull("KLAX should exist in seed data", klax)
        assertEquals("120.950", klax?.towerFreq)
    }

    @Test
    fun testSeedAirspaceContainment() {
        // Point in Ontario Airport (34.0560, -117.6012)
        val kajoLat = 33.8977
        val kajoLon = -117.6030
        val kontLat = 34.0560
        val kontLon = -117.6012

        val ontClassCPoly = GeometryUtils.generateCirclePolygon(kontLat, kontLon, 9260.0)
        assertTrue("KONT airport center should be inside KONT Class C polygon", GeometryUtils.isPointInsidePolygon(kontLat, kontLon, ontClassCPoly))
        assertFalse("KAJO airport should be outside KONT Class C polygon", GeometryUtils.isPointInsidePolygon(kajoLat, kajoLon, ontClassCPoly))
    }
}

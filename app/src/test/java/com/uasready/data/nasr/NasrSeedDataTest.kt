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

        val kord = airports.find { it.icaoId == "KORD" }
        assertNotNull("KORD Chicago O'Hare should exist in CONUS seed data", kord)
        assertEquals("120.750", kord?.towerFreq)

        val katl = airports.find { it.icaoId == "KATL" }
        assertNotNull("KATL Atlanta should exist in CONUS seed data", katl)
        assertEquals("119.100", katl?.towerFreq)

        val ksea = airports.find { it.icaoId == "KSEA" }
        assertNotNull("KSEA Seattle should exist in CONUS seed data", ksea)
        assertEquals("119.900", ksea?.towerFreq)

        val kdca = airports.find { it.icaoId == "KDCA" }
        assertNotNull("KDCA Washington National should exist in CONUS seed data", kdca)
        assertEquals("119.100", kdca?.towerFreq)

        val kjfk = airports.find { it.icaoId == "KJFK" }
        assertNotNull("KJFK New York should exist in CONUS seed data", kjfk)
        assertEquals("119.100", kjfk?.towerFreq)
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

    @Test
    fun testConusAirspacesAndSuaCoverage() {
        val airspaces = NasrSeedData.getSeedAirspaces()
        assertTrue("Should have nationwide CONUS airspaces", airspaces.size >= 30)
        assertNotNull(airspaces.find { it.id == "NASR-KORD-SFC" })
        assertNotNull(airspaces.find { it.id == "NASR-KATL-SFC" })
        assertNotNull(airspaces.find { it.id == "NASR-KJFK-SFC" })
        assertNotNull(airspaces.find { it.id == "NASR-KSEA-SFC" })

        val sua = NasrSeedData.getSeedSua()
        assertNotNull("P-56A Washington DC prohibited area should exist", sua.find { it.id == "P-56A" })
        assertNotNull("P-40 Camp David prohibited area should exist", sua.find { it.id == "P-40" })
        assertNotNull("R-2508 Edwards AFB restricted area should exist", sua.find { it.id == "R-2508" })
    }
}

package com.taksolutions.uasready.data.repository

import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests that call the live FAA ArcGIS airspace services.
 * Results depend on network access and current FAA data, so they are skipped by default.
 * Run with: ./gradlew testDebugUnitTest -PliveTests
 */
class LiveFaaAirspaceIntegrationTest {

    @Before
    fun requireLiveTestsFlag() {
        assumeTrue(
            "Live FAA tests skipped (enable with -PliveTests)",
            System.getProperty("uasready.liveTests") == "true"
        )
    }

    @Test
    fun testLiveAirspaceRepositorySanFrancisco() = kotlinx.coroutines.runBlocking {
        val repo = LiveAirspaceRepository()
        val sfLat = 37.7749
        val sfLon = -122.4194
        val result = repo.getAirspaceInfo(sfLat, sfLon)

        assertTrue(result.isSuccess)
        val airspace = result.getOrNull()
        assertNotNull(airspace)
        assertTrue("San Francisco should have controlled airspace polygons in 30 NM radius", airspace!!.zones.isNotEmpty())
        assertTrue("San Francisco airspace should include Class B, C, D or E zones", airspace.zones.any { it.name.contains("CLASS", ignoreCase = true) || it.name.contains("SAN FRANCISCO", ignoreCase = true) })
        // Verify non-surface Class E5/E6 transition areas are completely excluded
        assertFalse("Class E5 (700ft/1200ft) transition areas should be excluded", airspace.zones.any { it.name.contains("CLASS E5", ignoreCase = true) })
        assertFalse("Class E6 enroute areas should be excluded", airspace.zones.any { it.name.contains("CLASS E6", ignoreCase = true) })
        // Verify 500' floor filter: all zones must have starting altitude < 500' (surface to 500')
        assertTrue("All SF airspace zones must have floor < 500 ft", airspace.zones.all { it.floorFt < 500.0 })
        assertFalse("No SF airspace zones should have floor >= 500 ft", airspace.zones.any { it.floorFt >= 500.0 })
    }

    @Test
    fun testLiveAirspaceRepositoryOntarioCorona() = kotlinx.coroutines.runBlocking {
        val repo = LiveAirspaceRepository()
        val ontLat = 34.0560
        val ontLon = -117.6012
        val result = repo.getAirspaceInfo(ontLat, ontLon)

        assertTrue(result.isSuccess)
        val airspace = result.getOrNull()
        assertNotNull(airspace)
        assertTrue("Ontario/Corona should have controlled airspace polygons", airspace!!.zones.isNotEmpty())
        assertTrue("Launch at KONT should require controlled airspace authorization", airspace.controlledAirspaceAuthorizationRequired)
        assertFalse("Class E5 transition areas should be excluded in Ontario/Corona", airspace.zones.any { it.name.contains("CLASS E5", ignoreCase = true) })
        // Verify 500' floor filter: all zones must have starting altitude < 500' (surface to 500')
        assertTrue("All Ontario airspace zones must have floor < 500 ft", airspace.zones.all { it.floorFt < 500.0 })
        assertFalse("No Ontario airspace zones should have floor >= 500 ft", airspace.zones.any { it.floorFt >= 500.0 })
    }
}

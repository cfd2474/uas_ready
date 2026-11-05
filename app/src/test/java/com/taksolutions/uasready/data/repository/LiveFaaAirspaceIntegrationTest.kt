package com.taksolutions.uasready.data.repository

import com.taksolutions.uasready.domain.engine.AirspaceClassifier
import com.taksolutions.uasready.domain.model.AirspaceClass
import com.taksolutions.uasready.domain.model.AltitudeReference
import kotlinx.coroutines.runBlocking
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

    private val classifier = AirspaceClassifier()

    @Before
    fun requireLiveTestsFlag() {
        assumeTrue(
            "Live FAA tests skipped (enable with -PliveTests)",
            System.getProperty("uasready.liveTests") == "true"
        )
    }

    @Test
    fun testLiveAirspaceRepositorySanFrancisco() = runBlocking {
        val snapshot = LiveAirspaceRepository().getAirspaceSnapshot(37.7749, -122.4194).getOrThrow()

        assertTrue("San Francisco should have controlled airspace candidates in 30 NM", snapshot.zones.isNotEmpty())
        assertFalse("Class E5 transition areas are never candidates", snapshot.zones.any { it.name.contains("CLASS E5", true) })
        assertFalse("Class E6 enroute areas are never candidates", snapshot.zones.any { it.name.contains("CLASS E6", true) })
        assertTrue("AGL candidates all have floors below 400 ft",
            snapshot.zones.filter { it.floorReference == AltitudeReference.AGL }.all { it.floorFt < 400.0 })

        // Downtown SF (ground ~50 ft) sits under the SFO Class B
        val info = classifier.classify(snapshot, 37.7749, -122.4194, groundElevationFt = 50.0, plannedAltitudeAglFt = 400.0)
        assertTrue("Map zones are those reachable from a 400 ft flight at this ground elevation",
            info.zones.all { classifier.floorAglFt(it, 50.0) < 400.0 })
    }

    @Test
    fun testLiveAirspaceRepositoryOntarioCorona() = runBlocking {
        val snapshot = LiveAirspaceRepository().getAirspaceSnapshot(34.0560, -117.6012).getOrThrow()
        val info = classifier.classify(snapshot, 34.0560, -117.6012, groundElevationFt = 944.0, plannedAltitudeAglFt = 400.0)

        assertEquals("KONT surface area is Class C", AirspaceClass.CLASS_C, info.primaryClass)
        assertTrue("Launch at KONT requires authorization", info.controlledAirspaceAuthorizationRequired)
    }
}

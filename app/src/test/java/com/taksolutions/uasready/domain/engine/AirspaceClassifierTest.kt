package com.taksolutions.uasready.domain.engine

import com.taksolutions.uasready.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class AirspaceClassifierTest {

    private val classifier = AirspaceClassifier()
    private val lat = 39.66
    private val lon = -104.60

    private fun square(d: Double = 0.05) = listOf(
        lat - d to lon - d, lat - d to lon + d, lat + d to lon + d, lat + d to lon - d
    )

    private fun zone(
        id: String, floorFt: Double, ref: AltitudeReference,
        cls: AirspaceClass? = null, sua: SpecialUseType? = null, poly: List<Pair<Double, Double>> = square()
    ) = AirspaceZone(
        id = id, name = id, type = AirspaceZoneType.AUTHORIZATION_ZONE, centerLat = lat, centerLon = lon,
        radiusMeters = 0.0, floorFt = floorFt, polygonCoordinates = poly, floorReference = ref,
        airspaceClass = cls, specialUseType = sua
    )

    private fun classify(vararg zones: AirspaceZone, ground: Double? = 5900.0) = classifier.classify(
        AirspaceSnapshot(lat, lon, zones.toList(), fetchedEpochMs = 1L, sourceName = "test"),
        lat, lon, ground, plannedAltitudeAglFt = 400.0
    )

    @Test
    fun noZones_isClassG_noAuth() {
        val info = classify()
        assertEquals(AirspaceClass.CLASS_G, info.primaryClass)
        assertFalse(info.controlledAirspaceAuthorizationRequired)
    }

    @Test
    fun surfaceClassD_atLaunch_isClassD_withAuth() {
        val info = classify(zone("D", 0.0, AltitudeReference.AGL, cls = AirspaceClass.CLASS_D))
        assertEquals(AirspaceClass.CLASS_D, info.primaryClass)
        assertTrue(info.controlledAirspaceAuthorizationRequired)
    }

    @Test
    fun flightLevelRestrictedArea_isIgnored() {
        // R-5002G: floor FL200 = 20,000 ft MSL over ground at 56 ft
        val info = classify(zone("R-5002G", 20_000.0, AltitudeReference.MSL, sua = SpecialUseType.RESTRICTED), ground = 56.0)
        assertEquals(AirspaceClass.CLASS_G, info.primaryClass)
        assertNull(info.specialUseType)
        assertTrue("Not shown on the map either", info.zones.isEmpty())
    }

    @Test
    fun mslShelf_comparedAgainstGround() {
        val shelf = zone("DEN-B", 6500.0, AltitudeReference.MSL, cls = AirspaceClass.CLASS_B)
        assertEquals("598 ft AGL shelf is above a 400 ft flight", AirspaceClass.CLASS_G, classify(shelf, ground = 5902.0).primaryClass)
        assertEquals("300 ft AGL shelf is reached by a 400 ft flight", AirspaceClass.CLASS_B, classify(shelf, ground = 6200.0).primaryClass)
    }

    @Test
    fun unknownGround_treatsMslFloorsAsPossiblyLow() {
        val info = classify(zone("DEN-B", 6500.0, AltitudeReference.MSL, cls = AirspaceClass.CLASS_B), ground = null)
        assertEquals(AirspaceClass.CLASS_B, info.primaryClass)
        assertNull(info.groundElevationFt)
    }

    @Test
    fun aglFloorAt400OrAbove_isIgnored() {
        assertEquals(AirspaceClass.CLASS_G, classify(zone("E5", 700.0, AltitudeReference.AGL, cls = AirspaceClass.CLASS_E_SURFACE)).primaryClass)
    }

    @Test
    fun prohibitedAreaOverClassB_reportsBoth() {
        val info = classify(
            zone("P-56", 0.0, AltitudeReference.AGL, sua = SpecialUseType.PROHIBITED),
            zone("DCA-B", 0.0, AltitudeReference.AGL, cls = AirspaceClass.CLASS_B)
        )
        assertEquals(AirspaceClass.SPECIAL_USE, info.primaryClass)
        assertEquals(SpecialUseType.PROHIBITED, info.specialUseType)
        assertEquals(AirspaceClass.CLASS_B, info.controlledClass)
        assertEquals("P-56", info.specialUseName)
    }

    @Test
    fun zoneNotContainingLaunch_isShownButDoesNotClassify() {
        val far = listOf(lat + 0.2 to lon + 0.2, lat + 0.2 to lon + 0.3, lat + 0.3 to lon + 0.3, lat + 0.3 to lon + 0.2)
        val info = classify(zone("C", 0.0, AltitudeReference.AGL, cls = AirspaceClass.CLASS_C, poly = far))
        assertEquals(AirspaceClass.CLASS_G, info.primaryClass)
        assertEquals(1, info.zones.size)
    }

    @Test
    fun cachedSnapshot_isMarkedStale() {
        val info = classifier.classify(
            AirspaceSnapshot(lat, lon, emptyList(), fetchedEpochMs = 1L, sourceName = "test", isCached = true),
            lat, lon, 5900.0, 400.0
        )
        assertTrue(info.isStale)
    }
}

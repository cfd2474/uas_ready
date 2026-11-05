package com.taksolutions.uasready.ui.presentation

import com.taksolutions.uasready.domain.geo.GeoMath
import com.taksolutions.uasready.domain.model.AirportWarningZone
import com.taksolutions.uasready.domain.model.AirspaceZone
import com.taksolutions.uasready.domain.model.AirspaceZoneType
import org.junit.Assert.*
import org.junit.Test

class AirspaceInspectorTest {

    private val square = listOf(32.0 to -117.0, 32.0 to -116.9, 31.9 to -116.9, 31.9 to -117.0)

    private fun zone(id: String, type: AirspaceZoneType, ring: List<Pair<Double, Double>> = square, radius: Double = 0.0) =
        AirspaceZone(id, id, type, 31.95, -116.95, radius, polygonCoordinates = ring)

    private val warning = AirportWarningZone("KSAN", "San Diego", 1, "RUNWAY_BUFFER_3KM", "Runway Buffer (3km)", 3000,
        "#FFCC00", 31.95, -116.95, square)

    @Test
    fun returnsOnlyEnabledLayersContainingThePoint() {
        val zones = listOf(zone("B", AirspaceZoneType.AUTHORIZATION_ZONE), zone("TFR", AirspaceZoneType.RESTRICTED_ZONE),
            zone("FAR", AirspaceZoneType.RESTRICTED_ZONE, listOf(40.0 to -100.0, 40.0 to -99.9, 39.9 to -99.9)))
        val hits = AirspaceInspector.inspect(31.95, -116.95, zones, setOf(AirspaceZoneType.RESTRICTED_ZONE), listOf(warning), true)
        assertEquals(listOf("TFR"), hits.zones.map { it.id })
        assertEquals(listOf("KSAN"), hits.warningZones.map { it.ident })
        assertTrue(AirspaceInspector.inspect(31.95, -116.95, zones, emptySet(), listOf(warning), false).let { it.zones.isEmpty() && it.warningZones.isEmpty() })
    }

    @Test
    fun zonesWithoutARingAreCircles() {
        val circle = zone("C", AirspaceZoneType.WARNING_ZONE, emptyList(), radius = 1000.0)
        assertTrue(AirspaceInspector.contains(circle, 31.95, -116.95))
        assertTrue(AirspaceInspector.contains(circle, 31.95 + 0.008, -116.95))  // ~890 m north
        assertFalse(AirspaceInspector.contains(circle, 31.95 + 0.010, -116.95)) // ~1110 m north
    }

    @Test
    fun haversineDistance() {
        assertEquals(111_319.0, GeoMath.distanceMeters(0.0, 0.0, 0.0, 1.0), 1.0) // one degree of longitude at the equator
        assertEquals(0.0, GeoMath.distanceMeters(32.0, -117.0, 32.0, -117.0), 0.0)
    }
}

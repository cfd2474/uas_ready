package com.uasready.data.nasr

import org.junit.Assert.*
import org.junit.Test

class GeometryUtilsTest {

    @Test
    fun testPointInsidePolygon_Square() {
        val square = listOf(
            Pair(33.0, -117.0),
            Pair(33.0, -116.0),
            Pair(34.0, -116.0),
            Pair(34.0, -117.0),
            Pair(33.0, -117.0)
        )

        // Point inside
        assertTrue(GeometryUtils.isPointInsidePolygon(33.5, -116.5, square))

        // Points outside
        assertFalse(GeometryUtils.isPointInsidePolygon(32.5, -116.5, square))
        assertFalse(GeometryUtils.isPointInsidePolygon(34.5, -116.5, square))
        assertFalse(GeometryUtils.isPointInsidePolygon(33.5, -115.5, square))
        assertFalse(GeometryUtils.isPointInsidePolygon(33.5, -117.5, square))
    }

    @Test
    fun testBoundingBox_CalculationAndIntersects() {
        val polygon = listOf(
            Pair(33.80, -117.65),
            Pair(33.90, -117.50),
            Pair(33.75, -117.40),
            Pair(33.80, -117.65)
        )

        val bbox = GeometryUtils.calculateBoundingBox(polygon)
        assertEquals(33.75, bbox.minLat, 0.001)
        assertEquals(33.90, bbox.maxLat, 0.001)
        assertEquals(-117.65, bbox.minLon, 0.001)
        assertEquals(-117.40, bbox.maxLon, 0.001)

        assertTrue(bbox.contains(33.85, -117.55))
        assertFalse(bbox.contains(34.0, -117.55))

        val overlappingBbox = GeometryUtils.BoundingBox(33.70, 33.85, -117.60, -117.30)
        assertTrue(bbox.intersects(overlappingBbox))

        val distantBbox = GeometryUtils.BoundingBox(35.0, 36.0, -118.0, -117.0)
        assertFalse(bbox.intersects(distantBbox))
    }

    @Test
    fun testWkbEncodingAndDecoding() {
        val originalPoints = listOf(
            Pair(33.8977, -117.6030),
            Pair(33.9519, -117.4451),
            Pair(33.9747, -117.6366),
            Pair(33.8977, -117.6030)
        )

        val wkb = GeometryUtils.encodePolygonToWkb(originalPoints)
        assertNotNull(wkb)
        assertTrue(wkb.isNotEmpty())

        val decodedPoints = GeometryUtils.decodeWkbToPolygon(wkb)
        assertEquals(originalPoints.size, decodedPoints.size)
        for (i in originalPoints.indices) {
            assertEquals(originalPoints[i].first, decodedPoints[i].first, 0.00001)
            assertEquals(originalPoints[i].second, decodedPoints[i].second, 0.00001)
        }
    }

    @Test
    fun testHaversineDistance_KAJO_to_KONT() {
        // KAJO (Corona): 33.8977, -117.6030
        // KONT (Ontario): 34.0560, -117.6012
        val distNm = GeometryUtils.calculateDistanceNm(33.8977, -117.6030, 34.0560, -117.6012)
        // Distance is ~9.5 NM
        assertTrue("Distance should be around 9.5 NM, got $distNm", distNm in 9.0..10.5)
    }

    @Test
    fun testGenerateCirclePolygon() {
        val poly = GeometryUtils.generateCirclePolygon(34.0560, -117.6012, 9260.0, 16)
        assertTrue(poly.size >= 16)
        // Polygon is closed
        assertEquals(poly.first().first, poly.last().first, 0.0001)
        assertEquals(poly.first().second, poly.last().second, 0.0001)
    }
}

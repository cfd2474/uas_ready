package com.uasready.data.nasr

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * Geometric utilities for spatial indexing, Point-in-Polygon raycasting,
 * Well-Known Binary (WKB) polygon serialization, and geodesic math.
 */
object GeometryUtils {

    data class BoundingBox(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    ) {
        fun intersects(other: BoundingBox): Boolean {
            return minLat <= other.maxLat && maxLat >= other.minLat &&
                   minLon <= other.maxLon && maxLon >= other.minLon
        }

        fun contains(lat: Double, lon: Double): Boolean {
            return lat in minLat..maxLat && lon in minLon..maxLon
        }
    }

    /**
     * Calculates the minimum bounding box for a polygon coordinates list (lat, lon pairs).
     */
    fun calculateBoundingBox(points: List<Pair<Double, Double>>): BoundingBox {
        if (points.isEmpty()) return BoundingBox(0.0, 0.0, 0.0, 0.0)
        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE

        for ((lat, lon) in points) {
            if (lat < minLat) minLat = lat
            if (lat > maxLat) maxLat = lat
            if (lon < minLon) minLon = lon
            if (lon > maxLon) maxLon = lon
        }
        return BoundingBox(minLat, maxLat, minLon, maxLon)
    }

    /**
     * Exact Point-in-Polygon test using the even-odd ray casting algorithm.
     * @param lat Point latitude
     * @param lon Point longitude
     * @param polygon List of (lat, lon) vertices forming the polygon perimeter
     */
    fun isPointInsidePolygon(lat: Double, lon: Double, polygon: List<Pair<Double, Double>>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val (latI, lonI) = polygon[i]
            val (latJ, lonJ) = polygon[j]
            if (((latI > lat) != (latJ > lat)) &&
                (lon < (lonJ - lonI) * (lat - latI) / (latJ - latI) + lonI)
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    /**
     * Encodes a polygon (list of lat/lon vertices) into standard WKB (Well-Known Binary, Little Endian).
     * WKB Polygon structure:
     * 1 byte: byte order (1 = Little Endian)
     * 4 bytes: geometry type (3 = Polygon)
     * 4 bytes: number of rings (1 outer ring)
     * 4 bytes: number of points
     * N * 16 bytes: (lon Double, lat Double)
     */
    fun encodePolygonToWkb(points: List<Pair<Double, Double>>): ByteArray {
        val numPoints = points.size
        // Total bytes: 1 (order) + 4 (type) + 4 (numRings) + 4 (numPoints) + numPoints * 16
        val buffer = ByteBuffer.allocate(13 + numPoints * 16).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(1.toByte()) // Little Endian
        buffer.putInt(3)        // WKBPolygon
        buffer.putInt(1)        // 1 ring
        buffer.putInt(numPoints)
        for ((lat, lon) in points) {
            buffer.putDouble(lon) // WKB stores X (lon) first
            buffer.putDouble(lat) // Y (lat) second
        }
        return buffer.array()
    }

    /**
     * Decodes a WKB byte array into a list of (lat, lon) pairs.
     */
    fun decodeWkbToPolygon(wkb: ByteArray): List<Pair<Double, Double>> {
        if (wkb.size < 13) return emptyList()
        val buffer = ByteBuffer.wrap(wkb)
        val byteOrder = if (buffer.get() == 1.toByte()) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
        buffer.order(byteOrder)

        val geomType = buffer.int
        if (geomType != 3 && geomType != 1003) { // 3 = Polygon, 1003 = Polygon Z
            return emptyList()
        }

        val numRings = buffer.int
        if (numRings < 1) return emptyList()

        val numPoints = buffer.int
        if (numPoints <= 0 || numPoints > 100000) return emptyList()

        val points = ArrayList<Pair<Double, Double>>(numPoints)
        for (i in 0 until numPoints) {
            val lon = buffer.double
            val lat = buffer.double
            points.add(Pair(lat, lon))
        }
        return points
    }

    /**
     * Calculates great-circle distance in Nautical Miles using the Haversine formula.
     */
    fun calculateDistanceNm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distanceKm = 6371.0 * c
        return distanceKm * 0.539957
    }

    /**
     * Generates densified polygon vertices for circular airspace volumes.
     */
    fun generateCirclePolygon(centerLat: Double, centerLon: Double, radiusMeters: Double, numPoints: Int = 24): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        val earthRadius = 6378137.0
        val latRad = Math.toRadians(centerLat)
        val lonRad = Math.toRadians(centerLon)
        val dOverR = radiusMeters / earthRadius

        for (i in 0 until numPoints) {
            val bearing = 2 * Math.PI * i / numPoints
            val pointLatRad = asin(sin(latRad) * cos(dOverR) + cos(latRad) * sin(dOverR) * cos(bearing))
            val pointLonRad = lonRad + atan2(sin(bearing) * sin(dOverR) * cos(latRad), cos(dOverR) - sin(latRad) * sin(pointLatRad))
            points.add(Pair(Math.toDegrees(pointLatRad), Math.toDegrees(pointLonRad)))
        }
        // Close polygon
        if (points.isNotEmpty()) {
            points.add(points.first())
        }
        return points
    }
}

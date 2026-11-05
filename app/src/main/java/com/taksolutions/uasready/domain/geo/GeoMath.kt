package com.taksolutions.uasready.domain.geo

import kotlin.math.cos
import kotlin.math.hypot

object GeoMath {

    /** Ray-casting point-in-polygon. [polygon] is a ring of (lat, lon) pairs. */
    fun isPointInPolygon(lat: Double, lon: Double, polygon: List<Pair<Double, Double>>): Boolean {
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
     * Longitude half-width (degrees) that spans the same ground distance as [latHalfWidthDeg]
     * of latitude at [latitude]. Degrees of longitude shrink by cos(latitude).
     */
    fun longitudeHalfWidthDeg(latHalfWidthDeg: Double, latitude: Double): Double {
        val c = cos(Math.toRadians(latitude)).coerceAtLeast(0.05) // guard near the poles
        return latHalfWidthDeg / c
    }

    /**
     * Approximate distance in nautical miles from a point to a polygon ring; 0 when inside.
     * Local equirectangular projection: accurate to well under 1 % at the few-NM scale it is used for.
     */
    fun distanceToPolygonNm(lat: Double, lon: Double, polygon: List<Pair<Double, Double>>): Double {
        if (polygon.size < 3) return Double.POSITIVE_INFINITY
        if (isPointInPolygon(lat, lon, polygon)) return 0.0
        val kx = 60.0 * cos(Math.toRadians(lat)) // NM per degree of longitude here
        val ky = 60.0                            // NM per degree of latitude
        var best = Double.POSITIVE_INFINITY
        for (i in polygon.indices) {
            val (aLat, aLon) = polygon[i]
            val (bLat, bLon) = polygon[(i + 1) % polygon.size]
            val ax = (aLon - lon) * kx; val ay = (aLat - lat) * ky
            val bx = (bLon - lon) * kx; val by = (bLat - lat) * ky
            val dx = bx - ax; val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else (-(ax * dx + ay * dy) / len2).coerceIn(0.0, 1.0)
            best = minOf(best, hypot(ax + t * dx, ay + t * dy))
        }
        return best
    }
}

package com.taksolutions.uasready.domain.geo

import kotlin.math.cos

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
}

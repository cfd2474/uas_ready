package com.taksolutions.uasready.domain.model

/**
 * Represents a DJI GEO 2.0-style airport geofence warning zone.
 * Follows ICAO Annex 14 approach-surface bow-tie divergence unioned with
 * 4,000m (Level 3 Enhanced Warning) or 6,000m (Level 0 Warning) runway centreline buffers.
 */
data class AirportWarningZone(
    val ident: String,
    val name: String,
    val level: Int, // 0 = Warning, 3 = Enhanced Warning
    val zoneName: String, // "warning", "enhanced_warning"
    val ringRadiusMeters: Int, // 6000 or 4000
    val colorHex: String, // "#FFCC00" or "#EE8815"
    val centerLat: Double,
    val centerLon: Double,
    val polygonCoordinates: List<Pair<Double, Double>> // (lat, lon) pairs for outer ring
)

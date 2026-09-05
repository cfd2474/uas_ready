package com.taksolutions.uasready.domain.model

/**
 * Represents an airport proximity warning zone:
 * - High Risk Zone: Approach bow-tie corridor (1,200m corridor + 15% flare to 5.25km).
 * - Runway Buffer: 3km buffer around runway centrelines.
 */
data class AirportWarningZone(
    val ident: String,
    val name: String,
    val level: Int, // 3 = High Risk Zone (Bowtie), 1 = Runway Buffer (3km)
    val zoneType: String, // "HIGH_RISK_BOWTIE", "RUNWAY_BUFFER_3KM"
    val zoneName: String, // "High Risk Zone", "Runway Buffer (3km)"
    val ringRadiusMeters: Int, // 0 for pure bowtie, 3000 for runway buffer
    val colorHex: String, // "#EE8815" or "#FFCC00"
    val centerLat: Double,
    val centerLon: Double,
    val polygonCoordinates: List<Pair<Double, Double>> // (lat, lon) pairs for outer ring
)

package com.taksolutions.uasready.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class GeomagneticStormScale {
    NONE,
    G1_MINOR,     // Kp = 5
    G2_MODERATE,  // Kp = 6
    G3_STRONG,    // Kp = 7
    G4_SEVERE,    // Kp = 8
    G5_EXTREME    // Kp = 9
}

@Serializable
enum class GnssRiskLevel {
    LOW,
    MODERATE,
    HIGH,
    SEVERE
}

@Serializable
data class SpaceWeather(
    val currentKpIndex: Double,
    val forecastMaxKpIndex: Double,
    val geomagneticStormScale: GeomagneticStormScale,
    val gnssRiskLevel: GnssRiskLevel,
    val activeAlerts: List<String> = emptyList(),
    val timestampEpochMs: Long,
    val sourceName: String = "NOAA SWPC Live",
    val isStale: Boolean = false
) {
    companion object {
        fun calculateStormScale(kp: Double): GeomagneticStormScale {
            return when {
                kp >= 9.0 -> GeomagneticStormScale.G5_EXTREME
                kp >= 8.0 -> GeomagneticStormScale.G4_SEVERE
                kp >= 7.0 -> GeomagneticStormScale.G3_STRONG
                kp >= 6.0 -> GeomagneticStormScale.G2_MODERATE
                kp >= 5.0 -> GeomagneticStormScale.G1_MINOR
                else -> GeomagneticStormScale.NONE
            }
        }

        fun calculateGnssRisk(kp: Double): GnssRiskLevel {
            return when {
                kp >= 7.0 -> GnssRiskLevel.SEVERE
                kp >= 5.5 -> GnssRiskLevel.HIGH
                kp >= 4.0 -> GnssRiskLevel.MODERATE
                else -> GnssRiskLevel.LOW
            }
        }
    }
}

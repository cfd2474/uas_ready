package com.uasready.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Serializable
data class GnssEstimation(
    val visibleSatellitesCount: Int,
    val lockedSatellitesCount: Int,
    val estimatedHdop: Double,
    val signalIntegrityPercent: Int,
    val has3dFix: Boolean = true,
    val isHomePointStable: Boolean = true,
    val constellationsTracked: List<String> = listOf("GPS", "GLONASS", "Galileo", "BeiDou")
) {
    companion object {
        fun estimate(
            latitude: Double,
            elevationFt: Double,
            kpIndex: Double
        ): GnssEstimation {
            // 1. Base multi-constellation satellite density (GPS 31, GLONASS 24, Galileo 24, BeiDou 30)
            // Mid-latitudes (20°-50°) see higher constellation overlap (~30-34 satellites in open sky).
            // Polar or equatorial regions have slightly different geometric distributions (~26-30).
            val latRad = Math.toRadians(abs(latitude))
            val baseSatellites = 28.0 + 4.0 * kotlin.math.sin(latRad * 2)

            // 2. Geometric horizon advantage by elevation (higher MSL elevation expands line of sight)
            val elevationBonus = min(3.0, max(-3.0, (elevationFt - 500.0) / 2500.0))
            val rawVisible = (baseSatellites + elevationBonus).roundToInt().coerceIn(16, 38)

            // 3. Ionospheric Scintillation attenuation based on NOAA SWPC Planetary Kp index
            // Kp < 3: 0% lock loss
            // Kp 3-4: 5-10% lock loss
            // Kp 5 (G1): 20% lock loss
            // Kp 6 (G2): 35% lock loss
            // Kp 7 (G3): 55% lock loss
            // Kp 8 (G4): 70% lock loss
            // Kp 9 (G5): 85% lock loss
            val scintillationLossRate = when {
                kpIndex < 3.0 -> 0.0
                kpIndex < 4.0 -> (kpIndex - 2.0) * 0.05
                kpIndex < 5.0 -> 0.10 + (kpIndex - 4.0) * 0.10
                kpIndex < 6.0 -> 0.20 + (kpIndex - 5.0) * 0.15
                kpIndex < 7.0 -> 0.35 + (kpIndex - 6.0) * 0.20
                kpIndex < 8.0 -> 0.55 + (kpIndex - 7.0) * 0.15
                else -> 0.70 + min(0.20, (kpIndex - 8.0) * 0.15)
            }

            val lockedCount = (rawVisible * (1.0 - scintillationLossRate)).roundToInt().coerceAtLeast(0)

            // 4. Calculate Horizontal Dilution of Precision (HDOP)
            // Ideal open sky HDOP is 0.6 - 0.9.
            // As satellite count drops or scintillation rises, HDOP degrades exponentially.
            val estimatedHdop = when {
                lockedCount >= 22 -> 0.7 + (kpIndex * 0.05)
                lockedCount >= 16 -> 0.9 + (kpIndex * 0.08)
                lockedCount >= 12 -> 1.2 + (kpIndex * 0.10)
                lockedCount >= 8 -> 1.7 + (kpIndex * 0.15)
                lockedCount >= 5 -> 2.6 + (kpIndex * 0.20)
                else -> 4.5 + (kpIndex * 0.30)
            }.coerceIn(0.6, 9.9)

            val roundedHdop = (estimatedHdop * 10.0).roundToInt() / 10.0

            val integrity = ((1.0 - scintillationLossRate) * 100.0).roundToInt().coerceIn(10, 100)
            val has3dFix = lockedCount >= 4
            val homePointStable = lockedCount >= 10 && roundedHdop <= 2.0

            return GnssEstimation(
                visibleSatellitesCount = rawVisible,
                lockedSatellitesCount = lockedCount,
                estimatedHdop = roundedHdop,
                signalIntegrityPercent = integrity,
                has3dFix = has3dFix,
                isHomePointStable = homePointStable
            )
        }
    }
}

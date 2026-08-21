package com.uasready.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Serializable
data class TerrainObstructionProfile(
    val launchElevationMeters: Double,
    val sectorMaskAngles: Map<Int, Double>, // Azimuth (0, 45, 90, 135, 180, 225, 270, 315) -> horizon angle in degrees
    val averageHorizonMaskDeg: Double,
    val maxObstructionDeg: Double,
    val worstObstructionAzimuth: Int,
    val terrainOcclusionPercent: Int,
    val terrainClassification: String
) {
    val launchElevationFt: Double get() = launchElevationMeters * 3.28084

    companion object {
        fun fromSectorAngles(
            launchElevationMeters: Double,
            sectorMaskAngles: Map<Int, Double>
        ): TerrainObstructionProfile {
            val angles = sectorMaskAngles.values
            val avg = if (angles.isNotEmpty()) angles.average() else 0.0
            val maxEntry = sectorMaskAngles.maxByOrNull { it.value }
            val maxDeg = maxEntry?.value ?: 0.0
            val worstAz = maxEntry?.key ?: 0

            // Standard receiver elevation mask is 10°. Any obstruction > 10° occludes sky fraction.
            var totalOcclusionFraction = 0.0
            for (angle in angles) {
                val excess = max(0.0, angle - 10.0)
                totalOcclusionFraction += (excess / 80.0) // 80° is maximum effective sky span (10° to 90°)
            }
            val avgOcclusionFraction = if (angles.isNotEmpty()) totalOcclusionFraction / angles.size else 0.0
            val occlusionPercent = (avgOcclusionFraction * 100.0).roundToInt().coerceIn(0, 90)

            val classification = when {
                maxDeg <= 12.0 -> "Open Horizon"
                maxDeg <= 25.0 -> "Moderate Ridge / Hill"
                maxDeg <= 40.0 -> "Steep Terrain / Valley"
                else -> "Deep Canyon / Gorge"
            }

            return TerrainObstructionProfile(
                launchElevationMeters = launchElevationMeters,
                sectorMaskAngles = sectorMaskAngles,
                averageHorizonMaskDeg = (avg * 10.0).roundToInt() / 10.0,
                maxObstructionDeg = (maxDeg * 10.0).roundToInt() / 10.0,
                worstObstructionAzimuth = worstAz,
                terrainOcclusionPercent = occlusionPercent,
                terrainClassification = classification
            )
        }

        fun defaultOpenSky(elevationFt: Double = 500.0): TerrainObstructionProfile {
            val openSectors = mapOf(
                0 to 3.0, 45 to 3.0, 90 to 3.0, 135 to 3.0,
                180 to 3.0, 225 to 3.0, 270 to 3.0, 315 to 3.0
            )
            return fromSectorAngles(elevationFt / 3.28084, openSectors)
        }
    }
}

@Serializable
data class GnssEstimation(
    val visibleSatellitesCount: Int,
    val lockedSatellitesCount: Int,
    val terrainOccludedSatellitesCount: Int = 0,
    val estimatedHdop: Double,
    val signalIntegrityPercent: Int,
    val has3dFix: Boolean = true,
    val isHomePointStable: Boolean = true,
    val terrainProfile: TerrainObstructionProfile? = null,
    val constellationsTracked: List<String> = listOf("GPS", "GLONASS", "Galileo", "BeiDou")
) {
    companion object {
        fun estimate(
            latitude: Double,
            elevationFt: Double,
            kpIndex: Double,
            terrainProfile: TerrainObstructionProfile? = null
        ): GnssEstimation {
            // 1. Base multi-constellation satellite density (GPS 31, GLONASS 24, Galileo 24, BeiDou 30)
            val latRad = Math.toRadians(abs(latitude))
            val baseSatellites = 28.0 + 4.0 * kotlin.math.sin(latRad * 2)

            // 2. Geometric horizon advantage by elevation (higher MSL elevation expands line of sight)
            val elevationBonus = min(3.0, max(-3.0, (elevationFt - 500.0) / 2500.0))
            val rawVisible = (baseSatellites + elevationBonus).roundToInt().coerceIn(16, 38)

            // 3. Terrain Shading / Viewshed Horizon Occlusion
            val terrainOcclusionRate = (terrainProfile?.terrainOcclusionPercent ?: 0) / 100.0
            val terrainOccludedCount = (rawVisible * terrainOcclusionRate).roundToInt().coerceAtLeast(0)
            val unoccludedVisible = (rawVisible - terrainOccludedCount).coerceAtLeast(4)

            // 4. Ionospheric Scintillation attenuation based on NOAA SWPC Planetary Kp index
            val scintillationLossRate = when {
                kpIndex < 3.0 -> 0.0
                kpIndex < 4.0 -> (kpIndex - 2.0) * 0.05
                kpIndex < 5.0 -> 0.10 + (kpIndex - 4.0) * 0.10
                kpIndex < 6.0 -> 0.20 + (kpIndex - 5.0) * 0.15
                kpIndex < 7.0 -> 0.35 + (kpIndex - 6.0) * 0.20
                kpIndex < 8.0 -> 0.55 + (kpIndex - 7.0) * 0.15
                else -> 0.70 + min(0.20, (kpIndex - 8.0) * 0.15)
            }

            val lockedCount = (unoccludedVisible * (1.0 - scintillationLossRate)).roundToInt().coerceAtLeast(0)

            // 5. Calculate Horizontal Dilution of Precision (HDOP)
            // As terrain blocks sky sectors, azimuth geometry is asymmetrically constrained.
            val terrainHdopInflation = if (terrainProfile != null && terrainProfile.maxObstructionDeg > 20.0) {
                ((terrainProfile.maxObstructionDeg - 20.0) / 30.0) * 0.6
            } else {
                0.0
            }

            val baseHdop = when {
                lockedCount >= 22 -> 0.7 + (kpIndex * 0.05)
                lockedCount >= 16 -> 0.9 + (kpIndex * 0.08)
                lockedCount >= 12 -> 1.2 + (kpIndex * 0.10)
                lockedCount >= 8 -> 1.7 + (kpIndex * 0.15)
                lockedCount >= 5 -> 2.6 + (kpIndex * 0.20)
                else -> 4.5 + (kpIndex * 0.30)
            }

            val estimatedHdop = (baseHdop + terrainHdopInflation).coerceIn(0.6, 9.9)
            val roundedHdop = (estimatedHdop * 10.0).roundToInt() / 10.0

            val integrity = (((1.0 - scintillationLossRate) * (1.0 - terrainOcclusionRate * 0.5)) * 100.0).roundToInt().coerceIn(10, 100)
            val has3dFix = lockedCount >= 4
            val homePointStable = lockedCount >= 10 && roundedHdop <= 2.0

            return GnssEstimation(
                visibleSatellitesCount = rawVisible,
                lockedSatellitesCount = lockedCount,
                terrainOccludedSatellitesCount = terrainOccludedCount,
                estimatedHdop = roundedHdop,
                signalIntegrityPercent = integrity,
                has3dFix = has3dFix,
                isHomePointStable = homePointStable,
                terrainProfile = terrainProfile
            )
        }
    }
}

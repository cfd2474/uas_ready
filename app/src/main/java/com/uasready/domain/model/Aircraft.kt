package com.uasready.domain.model

import kotlinx.serialization.Serializable

/**
 * Environmental and operational limitations for a specific sUAS aircraft model.
 */
@Serializable
data class AircraftLimitations(
    val maxSustainedWindSpeedMph: Double,
    val maxGustSpeedMph: Double,
    val minOperatingTempF: Double,
    val maxOperatingTempF: Double,
    val maxOperatingAltitudeAglFt: Double = 400.0,
    val maxTakeoffAltitudeMslFt: Double = 10000.0,
    val precipitationAllowed: Boolean = false,
    val ipRating: String = "IP43",
    val nightOperationCapable: Boolean = true, // Anti-collision strobe / lighting
    val minSatellitesRequired: Int = 10,
    val maxKpIndexTolerance: Int = 5,
    val notes: String = ""
)

/**
 * Represents an unmanned aircraft profile (preset or user-customized).
 */
@Serializable
data class Aircraft(
    val id: String,
    val manufacturer: String,
    val model: String,
    val displayName: String,
    val isCustom: Boolean = false,
    val basePresetId: String? = null,
    val organization: String = "Standard Fleet",
    val limitations: AircraftLimitations
) {
    companion object {
        /**
         * Predefined commercial sUAS profiles with manufacturer-specified operational envelopes.
         */
        val PRESETS: List<Aircraft> = listOf(
            Aircraft(
                id = "dji_m3t",
                manufacturer = "DJI",
                model = "Mavic 3 Thermal",
                displayName = "DJI Mavic 3 Thermal",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 27.0, // 12 m/s
                    maxGustSpeedMph = 34.0, // 15 m/s
                    minOperatingTempF = 14.0, // -10°C
                    maxOperatingTempF = 104.0, // 40°C
                    maxTakeoffAltitudeMslFt = 19685.0, // 6000 m
                    precipitationAllowed = false,
                    ipRating = "None",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "Top-tier compact thermal imaging sUAS for public safety"
                )
            ),
            Aircraft(
                id = "dji_m3e",
                manufacturer = "DJI",
                model = "Mavic 3 Enterprise",
                displayName = "DJI Mavic 3 Enterprise",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 27.0,
                    maxGustSpeedMph = 34.0,
                    minOperatingTempF = 14.0,
                    maxOperatingTempF = 104.0,
                    maxTakeoffAltitudeMslFt = 19685.0,
                    precipitationAllowed = false,
                    ipRating = "None",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "High-precision mapping and visual inspection sUAS"
                )
            ),
            Aircraft(
                id = "dji_m30t",
                manufacturer = "DJI",
                model = "Matrice 30T",
                displayName = "DJI Matrice 30T",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 27.0,
                    maxGustSpeedMph = 33.5, // 15 m/s
                    minOperatingTempF = -4.0, // -20°C
                    maxOperatingTempF = 122.0, // 50°C
                    maxTakeoffAltitudeMslFt = 23000.0, // 7000 m
                    precipitationAllowed = true, // IP55 weather resistance
                    ipRating = "IP55",
                    nightOperationCapable = true,
                    minSatellitesRequired = 12,
                    maxKpIndexTolerance = 6,
                    notes = "Public safety enterprise platform with IP55 rating"
                )
            ),
            Aircraft(
                id = "dji_m350_rtk",
                manufacturer = "DJI",
                model = "Matrice 350 RTK",
                displayName = "DJI Matrice 350 RTK",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 27.0,
                    maxGustSpeedMph = 34.0,
                    minOperatingTempF = -4.0,
                    maxOperatingTempF = 122.0,
                    maxTakeoffAltitudeMslFt = 23000.0,
                    precipitationAllowed = true,
                    ipRating = "IP55",
                    nightOperationCapable = true,
                    minSatellitesRequired = 12,
                    maxKpIndexTolerance = 6,
                    notes = "Heavy enterprise platform for multi-payload emergency operations"
                )
            ),
            Aircraft(
                id = "dji_m300_rtk",
                manufacturer = "DJI",
                model = "Matrice 300 RTK",
                displayName = "DJI Matrice 300 RTK",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 27.0,
                    maxGustSpeedMph = 34.0,
                    minOperatingTempF = -4.0,
                    maxOperatingTempF = 122.0,
                    maxTakeoffAltitudeMslFt = 23000.0,
                    precipitationAllowed = true,
                    ipRating = "IP45",
                    nightOperationCapable = true,
                    minSatellitesRequired = 12,
                    maxKpIndexTolerance = 6,
                    notes = "Proven industrial heavy-lift inspection UAS"
                )
            ),
            Aircraft(
                id = "autel_evo_max_4t",
                manufacturer = "Autel Robotics",
                model = "EVO Max 4T",
                displayName = "Autel EVO Max 4T",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 27.0,
                    maxGustSpeedMph = 33.5,
                    minOperatingTempF = -4.0,
                    maxOperatingTempF = 122.0,
                    maxTakeoffAltitudeMslFt = 23000.0,
                    precipitationAllowed = true,
                    ipRating = "IP43",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 6,
                    notes = "Semi-autonomous public safety drone with A-Mesh"
                )
            ),
            Aircraft(
                id = "skydio_x10",
                manufacturer = "Skydio",
                model = "X10",
                displayName = "Skydio X10",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 28.0,
                    maxGustSpeedMph = 34.0,
                    minOperatingTempF = -4.0,
                    maxOperatingTempF = 113.0, // 45°C
                    maxTakeoffAltitudeMslFt = 16000.0,
                    precipitationAllowed = true,
                    ipRating = "IP55",
                    nightOperationCapable = true,
                    minSatellitesRequired = 8,
                    maxKpIndexTolerance = 6,
                    notes = "Autonomous enterprise drone with AI obstacle avoidance"
                )
            ),
            Aircraft(
                id = "skydio_x2d",
                manufacturer = "Skydio",
                model = "X2D",
                displayName = "Skydio X2D",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 24.0,
                    maxGustSpeedMph = 30.0,
                    minOperatingTempF = 14.0,
                    maxOperatingTempF = 109.0,
                    maxTakeoffAltitudeMslFt = 15000.0,
                    precipitationAllowed = false,
                    ipRating = "None",
                    nightOperationCapable = true,
                    minSatellitesRequired = 8,
                    maxKpIndexTolerance = 5,
                    notes = "Defense / tactical public safety autonomous platform"
                )
            ),
            Aircraft(
                id = "parrot_anafi_usa",
                manufacturer = "Parrot",
                model = "ANAFI USA",
                displayName = "Parrot ANAFI USA",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 28.0,
                    maxGustSpeedMph = 33.0,
                    minOperatingTempF = -25.0,
                    maxOperatingTempF = 120.0,
                    maxTakeoffAltitudeMslFt = 16400.0,
                    precipitationAllowed = true,
                    ipRating = "IP53",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "Blue UAS certified compact tactical platform"
                )
            )
        )

        fun getDefault(): Aircraft = PRESETS.first()
    }
}

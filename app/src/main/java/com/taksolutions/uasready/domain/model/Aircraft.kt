package com.taksolutions.uasready.domain.model

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
         * Comprehensive predefined commercial & enterprise sUAS profiles.
         */
        val PRESETS: List<Aircraft> = listOf(
            // ==================== DJI ENTERPRISE ====================
            Aircraft(
                id = "dji_m350_rtk",
                manufacturer = "DJI",
                model = "Matrice 350 RTK",
                displayName = "DJI Matrice 350 RTK",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 26.8, // 12 m/s
                    maxGustSpeedMph = 26.8, // Not published separately by DJI
                    minOperatingTempF = -4.0, // -20°C
                    maxOperatingTempF = 122.0, // 50°C
                    maxTakeoffAltitudeMslFt = 22966.0, // 5000 m (2110 props) / 7000 m (2112 high-alt props)
                    precipitationAllowed = true, // IP55
                    ipRating = "IP55",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "Takeoff Alt MSL: 16,404 ft (2110 props) / 22,966 ft (2112 high-alt props). Source: enterprise.dji.com/matrice-350-rtk/specs"
                )
            ),
            Aircraft(
                id = "dji_m300_rtk",
                manufacturer = "DJI",
                model = "Matrice 300 RTK",
                displayName = "DJI Matrice 300 RTK",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 33.6, // 15 m/s
                    maxGustSpeedMph = 33.6, // Not published separately by DJI
                    minOperatingTempF = -4.0, // -20°C
                    maxOperatingTempF = 122.0, // 50°C
                    maxTakeoffAltitudeMslFt = 22966.0, // 5000 m (2110 props) / 7000 m (2195 high-alt props)
                    precipitationAllowed = true, // IP45
                    ipRating = "IP45",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "12 m/s (26.8 mph) limit during takeoff/landing. Takeoff Alt MSL: 16,404 ft (2110 props) / 22,966 ft (2195 props). Source: dji.com/matrice-300/specs"
                )
            ),
            Aircraft(
                id = "dji_m400",
                manufacturer = "DJI",
                model = "Matrice 400",
                displayName = "DJI Matrice 400",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 26.8, // 12 m/s
                    maxGustSpeedMph = 26.8,
                    minOperatingTempF = -4.0, // -20°C
                    maxOperatingTempF = 122.0, // 50°C
                    maxTakeoffAltitudeMslFt = 22966.0, // 7000 m (standard 2510F props)
                    precipitationAllowed = true, // IP55
                    ipRating = "IP55",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "Standard 2510F props. Rain limit 100 mm/24 h. Source: enterprise.dji.com/matrice-400/specs"
                )
            ),
            Aircraft(
                id = "dji_m30_30t",
                manufacturer = "DJI",
                model = "Matrice 30 / 30T",
                displayName = "DJI Matrice 30 / 30T",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 33.6, // 15 m/s
                    maxGustSpeedMph = 33.6,
                    minOperatingTempF = -4.0, // -20°C
                    maxOperatingTempF = 122.0, // 50°C
                    maxTakeoffAltitudeMslFt = 22966.0, // 7000 m
                    precipitationAllowed = true, // IP55
                    ipRating = "IP55",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "12 m/s (26.8 mph) limit during takeoff/landing. Source: enterprise.dji.com/matrice-30/specs"
                )
            ),
            Aircraft(
                id = "dji_m4e_4t",
                manufacturer = "DJI",
                model = "Matrice 4E / 4T",
                displayName = "DJI Matrice 4E / 4T",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 26.8, // 12 m/s
                    maxGustSpeedMph = 26.8,
                    minOperatingTempF = 14.0, // -10°C
                    maxOperatingTempF = 104.0, // 40°C
                    maxTakeoffAltitudeMslFt = 19685.0, // 6000 m (4000 m / 13,123 ft with payload/accessories)
                    precipitationAllowed = false,
                    ipRating = "None",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "Gimbal/camera: no IP rating. Takeoff Alt MSL: 19,685 ft (bare) / 13,123 ft (with payload). Source: enterprise.dji.com/matrice-4-series/specs"
                )
            ),
            Aircraft(
                id = "dji_m4d_4td",
                manufacturer = "DJI",
                model = "Matrice 4D / 4TD",
                displayName = "DJI Matrice 4D / 4TD (Dock 3)",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 26.8, // 12 m/s
                    maxGustSpeedMph = 26.8,
                    minOperatingTempF = -4.0, // -20°C
                    maxOperatingTempF = 122.0, // 50°C
                    maxTakeoffAltitudeMslFt = 13123.0, // 4000 m
                    precipitationAllowed = true, // IP55
                    ipRating = "IP55",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "12 m/s in-flight and takeoff/landing; anti-ice props standard. Source: enterprise.dji.com/dock-3"
                )
            ),
            Aircraft(
                id = "dji_m3e_3t",
                manufacturer = "DJI",
                model = "Mavic 3E / 3T",
                displayName = "DJI Mavic 3E / 3T (Enterprise Series)",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 26.8, // 12 m/s
                    maxGustSpeedMph = 26.8,
                    minOperatingTempF = 14.0, // -10°C
                    maxOperatingTempF = 104.0, // 40°C
                    maxTakeoffAltitudeMslFt = 19685.0, // 6000 m
                    precipitationAllowed = false,
                    ipRating = "None",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "Compact enterprise survey & thermal platform. Source: enterprise.dji.com/mavic-3-enterprise/specs"
                )
            ),
            Aircraft(
                id = "dji_m3ta",
                manufacturer = "DJI",
                model = "Mavic 3TA",
                displayName = "DJI Mavic 3TA",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 26.8, // 12 m/s
                    maxGustSpeedMph = 26.8,
                    minOperatingTempF = 14.0, // -10°C
                    maxOperatingTempF = 104.0, // 40°C
                    maxTakeoffAltitudeMslFt = 19685.0, // 6000 m
                    precipitationAllowed = false,
                    ipRating = "None",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "Same airframe as Mavic 3E/3T. Source: enterprise.dji.com/mavic-3-enterprise/specs"
                )
            ),
            Aircraft(
                id = "dji_m3m",
                manufacturer = "DJI",
                model = "Mavic 3M",
                displayName = "DJI Mavic 3M (Multispectral)",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 26.8, // 12 m/s
                    maxGustSpeedMph = 26.8,
                    minOperatingTempF = 14.0, // -10°C
                    maxOperatingTempF = 104.0, // 40°C
                    maxTakeoffAltitudeMslFt = 19685.0, // 6000 m
                    precipitationAllowed = false,
                    ipRating = "None",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "Multispectral crop & environmental assessment. Source: enterprise.dji.com/mavic-3-m/specs"
                )
            ),
            Aircraft(
                id = "dji_mini_4_pro",
                manufacturer = "DJI",
                model = "Mini 4 Pro",
                displayName = "DJI Mini 4 Pro",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 23.9, // 10.7 m/s (Beaufort Level 5)
                    maxGustSpeedMph = 23.9,
                    minOperatingTempF = 14.0, // -10°C
                    maxOperatingTempF = 104.0, // 40°C
                    maxTakeoffAltitudeMslFt = 13123.0, // 4000 m
                    precipitationAllowed = false,
                    ipRating = "None",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "10.7 m/s = Beaufort Level 5. Source: dji.com/mini-4-pro/specs"
                )
            ),
            Aircraft(
                id = "dji_mini_3_pro",
                manufacturer = "DJI",
                model = "Mini 3 Pro",
                displayName = "DJI Mini 3 Pro",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 23.9, // 10.7 m/s (Beaufort Level 5)
                    maxGustSpeedMph = 23.9,
                    minOperatingTempF = 14.0, // -10°C
                    maxOperatingTempF = 104.0, // 40°C
                    maxTakeoffAltitudeMslFt = 13123.0, // 4000 m
                    precipitationAllowed = false,
                    ipRating = "None",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "10.7 m/s = Beaufort Level 5. Source: dji.com/mini-3-pro/specs"
                )
            ),
            Aircraft(
                id = "dji_mini_3",
                manufacturer = "DJI",
                model = "Mini 3",
                displayName = "DJI Mini 3",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 23.9, // 10.7 m/s (Beaufort Level 5)
                    maxGustSpeedMph = 23.9,
                    minOperatingTempF = 14.0, // -10°C
                    maxOperatingTempF = 104.0, // 40°C
                    maxTakeoffAltitudeMslFt = 13123.0, // 4000 m
                    precipitationAllowed = false,
                    ipRating = "None",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "10.7 m/s = Beaufort Level 5. Source: dji.com/mini-3/specs"
                )
            ),

            // ==================== AUTEL ROBOTICS ====================
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
                    notes = "Semi-autonomous public safety drone with A-Mesh and thermal zoom"
                )
            ),
            Aircraft(
                id = "autel_evo_max_4n",
                manufacturer = "Autel Robotics",
                model = "EVO Max 4N",
                displayName = "Autel EVO Max 4N (Starlight Night Vision)",
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
                    notes = "Ultra-low-light 0.0001 Lux Starlight camera with radiometric thermal"
                )
            ),
            Aircraft(
                id = "autel_alpha",
                manufacturer = "Autel Robotics",
                model = "Alpha",
                displayName = "Autel Alpha Enterprise",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 27.0,
                    maxGustSpeedMph = 33.5,
                    minOperatingTempF = -4.0,
                    maxOperatingTempF = 122.0,
                    maxTakeoffAltitudeMslFt = 23000.0,
                    precipitationAllowed = true,
                    ipRating = "IP55",
                    nightOperationCapable = true,
                    minSatellitesRequired = 12,
                    maxKpIndexTolerance = 6,
                    notes = "Industrial IP55 multi-sensor flagship drone with mmWave radar"
                )
            ),
            Aircraft(
                id = "autel_titan",
                manufacturer = "Autel Robotics",
                model = "Titan",
                displayName = "Autel Titan Heavy Lift",
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
                    notes = "Heavy multi-payload delivery & public safety platform"
                )
            ),
            Aircraft(
                id = "autel_evo_2_dual_640t",
                manufacturer = "Autel Robotics",
                model = "EVO II Dual 640T V3",
                displayName = "Autel EVO II Dual 640T V3 Enterprise",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 27.0,
                    maxGustSpeedMph = 33.0,
                    minOperatingTempF = 14.0,
                    maxOperatingTempF = 104.0,
                    maxTakeoffAltitudeMslFt = 23000.0,
                    precipitationAllowed = false,
                    ipRating = "None",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "Thermal + 50MP visual inspection UAS"
                )
            ),
            Aircraft(
                id = "autel_evo_2_pro_enterprise",
                manufacturer = "Autel Robotics",
                model = "EVO II Pro Enterprise V3",
                displayName = "Autel EVO II Pro Enterprise V3 (6K)",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 27.0,
                    maxGustSpeedMph = 33.0,
                    minOperatingTempF = 14.0,
                    maxOperatingTempF = 104.0,
                    maxTakeoffAltitudeMslFt = 23000.0,
                    precipitationAllowed = false,
                    ipRating = "None",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "6K 1-inch sensor aerial inspection & mapping UAS"
                )
            ),
            Aircraft(
                id = "autel_dragonfish_standard",
                manufacturer = "Autel Robotics",
                model = "Dragonfish Standard",
                displayName = "Autel Dragonfish Standard VTOL",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 31.0,
                    maxGustSpeedMph = 38.0,
                    minOperatingTempF = -4.0,
                    maxOperatingTempF = 122.0,
                    maxTakeoffAltitudeMslFt = 19685.0,
                    precipitationAllowed = true,
                    ipRating = "IP43",
                    nightOperationCapable = true,
                    minSatellitesRequired = 12,
                    maxKpIndexTolerance = 6,
                    notes = "Fixed-wing tilt-rotor VTOL with 120-minute flight endurance"
                )
            ),

            // ==================== SKYDIO ====================
            Aircraft(
                id = "skydio_x10",
                manufacturer = "Skydio",
                model = "X10",
                displayName = "Skydio X10 Enterprise",
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
                    notes = "Autonomous AI navigation with NightSense zero-light navigation"
                )
            ),
            Aircraft(
                id = "skydio_x10d",
                manufacturer = "Skydio",
                model = "X10D",
                displayName = "Skydio X10D (Blue UAS Certified)",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 28.0,
                    maxGustSpeedMph = 34.0,
                    minOperatingTempF = -4.0,
                    maxOperatingTempF = 113.0,
                    maxTakeoffAltitudeMslFt = 16000.0,
                    precipitationAllowed = true,
                    ipRating = "IP55",
                    nightOperationCapable = true,
                    minSatellitesRequired = 8,
                    maxKpIndexTolerance = 6,
                    notes = "DoD / DIU Blue UAS compliant autonomous tactical system"
                )
            ),
            Aircraft(
                id = "skydio_x2d",
                manufacturer = "Skydio",
                model = "X2D",
                displayName = "Skydio X2D (Blue UAS Thermal)",
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
                    notes = "Defense & tactical reconnaissance autonomous platform"
                )
            ),
            Aircraft(
                id = "skydio_x2e",
                manufacturer = "Skydio",
                model = "X2E",
                displayName = "Skydio X2E Enterprise",
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
                    notes = "Enterprise structural inspection with 360 obstacle avoidance"
                )
            ),
            Aircraft(
                id = "skydio_2_plus_enterprise",
                manufacturer = "Skydio",
                model = "2+ Enterprise",
                displayName = "Skydio 2+ Enterprise",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 24.0,
                    maxGustSpeedMph = 28.0,
                    minOperatingTempF = 23.0,
                    maxOperatingTempF = 104.0,
                    maxTakeoffAltitudeMslFt = 15000.0,
                    precipitationAllowed = false,
                    ipRating = "None",
                    nightOperationCapable = false,
                    minSatellitesRequired = 8,
                    maxKpIndexTolerance = 5,
                    notes = "Compact autonomous close-proximity inspection UAS"
                )
            ),

            // ==================== PARROT ====================
            Aircraft(
                id = "parrot_anafi_usa",
                manufacturer = "Parrot",
                model = "ANAFI USA",
                displayName = "Parrot ANAFI USA (Blue UAS)",
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
                    notes = "Blue UAS certified 32x optical zoom & FLIR Boson thermal"
                )
            ),
            Aircraft(
                id = "parrot_anafi_usa_gov",
                manufacturer = "Parrot",
                model = "ANAFI USA GOV/MIL",
                displayName = "Parrot ANAFI USA GOV / MIL",
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
                    notes = "NDAA and TAA compliant tactical platform with WPA2 security"
                )
            ),
            Aircraft(
                id = "parrot_anafi_ai",
                manufacturer = "Parrot",
                model = "ANAFI Ai",
                displayName = "Parrot ANAFI Ai (4G LTE Connected)",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 28.0,
                    maxGustSpeedMph = 31.0,
                    minOperatingTempF = 14.0,
                    maxOperatingTempF = 104.0,
                    maxTakeoffAltitudeMslFt = 14763.0,
                    precipitationAllowed = true,
                    ipRating = "IP53",
                    nightOperationCapable = true,
                    minSatellitesRequired = 10,
                    maxKpIndexTolerance = 5,
                    notes = "4G LTE connected 48MP photogrammetry inspection drone"
                )
            ),
            Aircraft(
                id = "parrot_anafi_thermal",
                manufacturer = "Parrot",
                model = "ANAFI Thermal",
                displayName = "Parrot ANAFI Thermal",
                limitations = AircraftLimitations(
                    maxSustainedWindSpeedMph = 24.0,
                    maxGustSpeedMph = 31.0,
                    minOperatingTempF = 14.0,
                    maxOperatingTempF = 104.0,
                    maxTakeoffAltitudeMslFt = 14763.0,
                    precipitationAllowed = false,
                    ipRating = "None",
                    nightOperationCapable = true,
                    minSatellitesRequired = 8,
                    maxKpIndexTolerance = 5,
                    notes = "Ultra-compact dual sensor thermal inspection drone"
                )
            )
        )

        fun getDefault(): Aircraft = PRESETS.first()
    }
}

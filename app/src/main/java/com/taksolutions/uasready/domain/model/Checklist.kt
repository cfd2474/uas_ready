package com.taksolutions.uasready.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChecklistCategory(val title: String) {
    PREFLIGHT("Aircraft Preflight"),
    LAUNCH("Launch Readiness"),
    POSTFLIGHT("Postflight Inspection"),
    CUSTOM("Custom Reference")
}

@Serializable
data class ChecklistItem(
    val id: String,
    val title: String,
    val description: String = "",
    val isCritical: Boolean = false
)

@Serializable
data class ChecklistGroup(
    val id: String,
    val category: ChecklistCategory,
    val title: String,
    val subtitle: String = "Read-only operational checklist reference",
    val items: List<ChecklistItem>
) {
    companion object {
        /**
         * Default aviation and public-safety read-only checklists.
         */
        val DEFAULT_CHECKLISTS: List<ChecklistGroup> = listOf(
            ChecklistGroup(
                id = "preflight_default",
                category = ChecklistCategory.PREFLIGHT,
                title = "Aircraft Preflight Inspection",
                items = listOf(
                    ChecklistItem("pf_1", "Airframe & Hull", "Inspect fuselage, arms, landing gear, and fasteners for micro-fractures", true),
                    ChecklistItem("pf_2", "Propellers & Hubs", "Verify secure attachment, no nicks/chips, free spinning", true),
                    ChecklistItem("pf_3", "Battery & Terminals", "Check cell balance, charge >= 90%, secure latching, clean contacts", true),
                    ChecklistItem("pf_4", "Gimbal & Payload", "Remove gimbal clamp, inspect lens clarity, verify pan/tilt travel", true),
                    ChecklistItem("pf_5", "Sensors & Obstacle Avoidance", "Clean optical and infrared sensors, inspect vision sensors", false),
                    ChecklistItem("pf_6", "MicroSD & Storage", "Confirm high-speed V30 card inserted and adequate free capacity", false),
                    ChecklistItem("pf_7", "Ground Controller & Antennas", "Check controller battery >= 80%, orient antennas at 90 degrees", true),
                    ChecklistItem("pf_8", "Firmware & Calibration", "Verify firmware matches team standards; compass/IMU healthy", true)
                )
            ),
            ChecklistGroup(
                id = "launch_default",
                category = ChecklistCategory.LAUNCH,
                title = "Launch & Takeoff Protocol",
                items = listOf(
                    ChecklistItem("l_1", "Takeoff / Landing Zone", "Clear 20ft radius of unauthorized personnel and overhead obstacles", true),
                    ChecklistItem("l_2", "GNSS Lock & Satellites", "Confirm >= 10 GNSS satellites locked with strong signal quality", true),
                    ChecklistItem("l_3", "Home Point & RTH Altitude", "Verify home point matches launch pad; RTH altitude set above obstacles", true),
                    ChecklistItem("l_4", "Visual Observer & Crew Brief", "Brief VO on scan sectors, communication protocols, and abort criteria", false),
                    ChecklistItem("l_5", "Airspace & Frequency Scan", "Scan visual airspace for manned aircraft; monitor local CTAF/air traffic", true),
                    ChecklistItem("l_6", "Motor Arm & Hover Test", "Arm motors, execute 10-second hover at 10ft AGL, verify telemetry response", true)
                )
            ),
            ChecklistGroup(
                id = "postflight_default",
                category = ChecklistCategory.POSTFLIGHT,
                title = "Postflight Inspection & Secure",
                items = listOf(
                    ChecklistItem("post_1", "Motor Disarm & Power Down", "Confirm motors fully stopped before approaching; power off aircraft first", true),
                    ChecklistItem("post_2", "Battery Temperature & Storage", "Inspect battery for swelling or excessive heat; store in LiPo safe container", true),
                    ChecklistItem("post_3", "Propeller & Motor Inspection", "Check motor heat and spin resistance; inspect leading edges for debris", false),
                    ChecklistItem("post_4", "Sensor & Gimbal Lock", "Reinstall gimbal protector clamp and lens covers", false),
                    ChecklistItem("post_5", "Mission Data & Logs Extraction", "Transfer media and telemetry logs to department repository", false),
                    ChecklistItem("post_6", "Log Discrepancies", "Record flight hours and log any hardware or flight anomalies", true)
                )
            )
        )
    }
}

@Serializable
data class EmergencyProcedure(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val isCriticalWarning: Boolean = false
) {
    companion object {
        val DEFAULT_PROCEDURES: List<EmergencyProcedure> = listOf(
            EmergencyProcedure(
                1,
                "Return to Home (RTH)",
                "In case of signal loss, low battery, or other emergencies, use the Return to Home function. The drone will automatically return to its takeoff point. This function should be pre-set before flight."
            ),
            EmergencyProcedure(
                2,
                "Emergency Landing",
                "If you encounter an issue that requires immediate landing, slowly descend to a safe location. Be mindful of people, animals, and obstacles in the landing area."
            ),
            EmergencyProcedure(
                3,
                "Battery Issues",
                "If the battery level becomes critically low during flight, land as soon as safely possible. Avoid depleting the battery completely, as it may lead to a loss of control.",
                isCriticalWarning = true
            ),
            EmergencyProcedure(
                4,
                "Signal Loss",
                "If you lose the control signal, stay calm. The drone should automatically initiate the Return to Home process if it can't re-establish a connection within a set time."
            ),
            EmergencyProcedure(
                5,
                "Obstacle Collision",
                "If your drone collides with an obstacle, assess the situation. If the drone is still operational, carefully navigate it back and land. If control is lost, use the RTH feature if possible."
            ),
            EmergencyProcedure(
                6,
                "Weather Changes",
                "If you encounter unexpected bad weather, such as high winds or rain, return and land the drone immediately to avoid loss of control or damage."
            ),
            EmergencyProcedure(
                7,
                "Avoid Water",
                "If flying near water and facing an emergency, do everything possible to avoid landing in water, as this can severely damage the drone."
            ),
            EmergencyProcedure(
                8,
                "Firmware/Software Glitches",
                "If you experience technical issues related to firmware or software, try to safely land the drone. Avoid complex maneuvers until you can troubleshoot the issue on the ground."
            ),
            EmergencyProcedure(
                9,
                "Emergency Evasive Action",
                "Established flight regulations can be violated in an emergency situation if doing so will prevent an inflight emergency.",
                isCriticalWarning = true
            ),
            EmergencyProcedure(
                10,
                "Post-Incident Inspection",
                "After any emergency or hard landing, thoroughly inspect your drone for damage before the next flight. Any drone that has experienced a \"crash\" should be sent to the UAS coordinator for inspection."
            )
        )
    }
}

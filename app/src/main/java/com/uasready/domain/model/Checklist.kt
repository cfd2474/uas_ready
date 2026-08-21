package com.uasready.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChecklistCategory(val title: String) {
    PREFLIGHT("Aircraft Preflight"),
    LAUNCH("Launch Readiness"),
    POSTFLIGHT("Postflight Inspection"),
    CUSTOM("Custom Organization Reference")
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
         * Default aviation and public-safety read-only checklists as specified in guidance.md.
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

        /**
         * Parses a CSV string into a ChecklistGroup.
         * Expected CSV format:
         * Title, Description, IsCritical
         */
        fun parseFromCsv(title: String, csvContent: String): ChecklistGroup {
            val items = mutableListOf<ChecklistItem>()
            val lines = csvContent.lines().filter { it.isNotBlank() }
            
            lines.forEachIndexed { index, rawLine ->
                // Skip header if present
                val line = rawLine.trim()
                if (index == 0 && (line.startsWith("Title", ignoreCase = true) || line.startsWith("Item", ignoreCase = true))) {
                    return@forEachIndexed
                }
                val parts = parseCsvLine(line)
                if (parts.isNotEmpty()) {
                    val itemTitle = parts[0].trim()
                    val desc = parts.getOrNull(1)?.trim() ?: ""
                    val isCritical = parts.getOrNull(2)?.trim()?.equals("true", ignoreCase = true) ?: false
                    items.add(
                        ChecklistItem(
                            id = "csv_item_${index + 1}",
                            title = itemTitle,
                            description = desc,
                            isCritical = isCritical
                        )
                    )
                }
            }

            return ChecklistGroup(
                id = "custom_${System.currentTimeMillis()}",
                category = ChecklistCategory.CUSTOM,
                title = title,
                items = items
            )
        }

        private fun parseCsvLine(line: String): List<String> {
            val result = mutableListOf<String>()
            val sb = java.lang.StringBuilder()
            var inQuotes = false
            for (ch in line.toCharArray()) {
                when (ch) {
                    '"' -> inQuotes = !inQuotes
                    ',' -> {
                        if (inQuotes) {
                            sb.append(ch)
                        } else {
                            result.add(sb.toString())
                            sb.setLength(0)
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            result.add(sb.toString())
            return result
        }
    }
}

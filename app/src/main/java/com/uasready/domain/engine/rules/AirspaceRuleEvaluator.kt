package com.uasready.domain.engine.rules

import com.uasready.domain.engine.AssessmentContext
import com.uasready.domain.engine.CategoryRuleEvaluator
import com.uasready.domain.model.*

class AirspaceRuleEvaluator : CategoryRuleEvaluator {
    override val category: AssessmentCategory = AssessmentCategory.AIRSPACE

    override fun evaluate(context: AssessmentContext): CategoryAssessment {
        val airspace = context.airspace
        if (airspace == null) {
            return CategoryAssessment(
                category = category,
                status = AssessmentStatus.DATA_UNAVAILABLE,
                ruleResults = listOf(
                    RuleResult(
                        ruleId = "AIR-MISSING",
                        category = category,
                        status = AssessmentStatus.DATA_UNAVAILABLE,
                        title = "Airspace Data Unavailable",
                        inputValueFormatted = "No Telemetry",
                        thresholdFormatted = "FAA Live Telemetry Required",
                        explanation = "Unable to verify TFRs and controlled airspace. Mandatory live airspace check failed."
                    )
                ),
                summary = "Missing live airspace telemetry"
            )
        }

        val rules = mutableListOf<RuleResult>()
        val flightWindow = context.flightWindow

        // 1. Temporary Flight Restrictions (TFR)
        val activeTfrs = airspace.activeTfrs.filter {
            // Check overlap with flight window
            it.effectiveStartEpochMs <= flightWindow.endEpochMs && it.effectiveEndEpochMs >= flightWindow.startEpochMs
        }

        val hazardTfr = activeTfrs.find { it.type.contains("91.137", true) || it.description.contains("91.137", true) || it.description.contains("FIRE", true) || it.description.contains("HAZARD", true) }

        if (hazardTfr != null) {
            rules.add(
                RuleResult(
                    ruleId = "AIR-TFR-91137",
                    category = category,
                    status = AssessmentStatus.NO_GO,
                    title = "CRITICAL: 14 CFR § 91.137 Firefighting / Disaster TFR",
                    inputValueFormatted = hazardTfr.id,
                    thresholdFormatted = "Zero Hazard TFRs in AOR",
                    explanation = String.format("Flight location directly intersects a 14 CFR § 91.137 emergency firefighting / disaster relief TFR (%s). Unauthorized UAS operations risk mid-air collision with low-level aerial firefighting aircraft and face immediate federal criminal prosecution.", hazardTfr.description)
                )
            )
        } else if (activeTfrs.isNotEmpty()) {
            val tfr = activeTfrs.first()
            rules.add(
                RuleResult(
                    ruleId = "AIR-TFR-001",
                    category = category,
                    status = AssessmentStatus.NO_GO,
                    title = "Active Temporary Flight Restriction (TFR)",
                    inputValueFormatted = tfr.id,
                    thresholdFormatted = "Zero Active TFRs in flight window",
                    explanation = String.format("Flight location overlaps active %s TFR (%s). Flight is legally prohibited under 14 CFR § 91.137/141.", tfr.type, tfr.description)
                )
            )
        } else {
            rules.add(
                RuleResult(
                    ruleId = "AIR-TFR-002",
                    category = category,
                    status = AssessmentStatus.GO,
                    title = "Temporary Flight Restrictions",
                    inputValueFormatted = "None Active",
                    thresholdFormatted = "Clear",
                    explanation = "No active or scheduled TFRs intersect the planned flight location and time window."
                )
            )
        }

        // 2. Controlled Airspace Classification & Authorization (Warning for any Non-Class G)
        val isNonClassG = airspace.primaryClass != AirspaceClass.CLASS_G
        val formattedClassName = when (airspace.primaryClass) {
            AirspaceClass.CLASS_B -> "Class B"
            AirspaceClass.CLASS_C -> "Class C"
            AirspaceClass.CLASS_D -> "Class D"
            AirspaceClass.CLASS_E_SURFACE -> "Class E Surface"
            AirspaceClass.CLASS_E -> "Class E"
            AirspaceClass.CLASS_G -> "Class G"
            AirspaceClass.SPECIAL_USE -> "Special Use Airspace"
        }

        if (isNonClassG) {
            rules.add(
                RuleResult(
                    ruleId = "AIR-CTRL-001",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "Controlled Airspace Warning ($formattedClassName)",
                    inputValueFormatted = formattedClassName,
                    thresholdFormatted = "LAANC Approval Required",
                    explanation = String.format("Flight location is within %s controlled airspace. Non-Class G airspace requires FAA authorization. Please check official LAANC applications (e.g. Aloft/AirControl, AutoPylot, AirMap) for approval to fly in controlled airspace.", formattedClassName)
                )
            )
        } else {
            rules.add(
                RuleResult(
                    ruleId = "AIR-CTRL-002",
                    category = category,
                    status = AssessmentStatus.GO,
                    title = "Airspace Authorization",
                    inputValueFormatted = "Class G (Uncontrolled)",
                    thresholdFormatted = "Uncontrolled Class G",
                    explanation = "Flight location is inside uncontrolled Class G airspace. No prior FAA LAANC or ATC authorization required for operations up to 400 ft AGL."
                )
            )
        }

        // 3. UAS Facility Map (UASFM) Grid Altitude Limit
        val uasfmCeiling = airspace.uasFacilityMapMaxAltitudeFt
        if (uasfmCeiling != null) {
            val plannedAlt = context.plannedAltitudeAglFt
            when {
                plannedAlt > uasfmCeiling -> rules.add(
                    RuleResult(
                        ruleId = "AIR-UASFM-001",
                        category = category,
                        status = AssessmentStatus.NO_GO,
                        title = "UAS Facility Map Ceiling Exceeded",
                        inputValueFormatted = String.format("%.0f ft AGL Planned", plannedAlt),
                        thresholdFormatted = String.format("Max %.0f ft AGL Grid Ceiling", uasfmCeiling),
                        explanation = String.format("Planned altitude (%.0f ft) exceeds the maximum auto-approved UAS Facility Map grid ceiling (%.0f ft). Requires manual FAA safety review.", plannedAlt, uasfmCeiling)
                    )
                )
                uasfmCeiling == 0.0 -> rules.add(
                    RuleResult(
                        ruleId = "AIR-UASFM-002",
                        category = category,
                        status = AssessmentStatus.NO_GO,
                        title = "Zero-Altitude UASFM Grid Cell",
                        inputValueFormatted = "0 ft Ceiling",
                        thresholdFormatted = "> 0 ft",
                        explanation = "Flight location falls in a 0 ft UASFM grid cell (immediate runway approach or security perimeter). Automated LAANC not permitted."
                    )
                )
                else -> rules.add(
                    RuleResult(
                        ruleId = "AIR-UASFM-003",
                        category = category,
                        status = AssessmentStatus.GO,
                        title = "UAS Facility Map Ceiling",
                        inputValueFormatted = String.format("%.0f ft / %.0f ft Ceiling", plannedAlt, uasfmCeiling),
                        thresholdFormatted = String.format("<= %.0f ft", uasfmCeiling),
                        explanation = String.format("Planned altitude conforms with auto-approved UASFM grid ceiling (%.0f ft AGL).", uasfmCeiling)
                    )
                )
            }
        }

        // 4. Special Use Airspace
        if (airspace.specialUseAirspaceActive) {
            rules.add(
                RuleResult(
                    ruleId = "AIR-SUA-001",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "Special Use Airspace Active",
                    inputValueFormatted = airspace.specialUseName ?: "Active MOA / Alert Area",
                    thresholdFormatted = "Inactive / Clear",
                    explanation = String.format("Location intersects active Special Use Airspace (%s). Maintain extreme visual scan for high-speed military traffic.", airspace.specialUseName ?: "Active MOA")
                )
            )
        }

        // 5. AIRAC Cycle Staleness Check
        if (airspace.isStale) {
            rules.add(
                RuleResult(
                    ruleId = "AIR-CYCLE-STALE",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "AIRAC Aeronautical Cycle Expired",
                    inputValueFormatted = airspace.sourceName,
                    thresholdFormatted = "Current AIRAC Cycle Recommended",
                    explanation = "On-device FAA NASR aeronautical data cycle has expired. Advisories remain active, but database update is recommended."
                )
            )
        }

        // 6. Local CTAF Frequency Awareness (Listen Only)
        val ctafNotam = airspace.notams.firstOrNull { it.id.startsWith("CTAF-") }
        val ctafDesc = ctafNotam?.text ?: "Nearest Airport CTAF: 122.800 MHz"
        val ctafFreq = if (ctafDesc.contains("CTAF: ")) ctafDesc.substringAfter("CTAF: ") else "122.800 MHz"
        rules.add(
            RuleResult(
                ruleId = "AIR-CTAF-001",
                category = category,
                status = AssessmentStatus.GO,
                title = "Local CTAF (Listen Only)",
                inputValueFormatted = ctafFreq,
                thresholdFormatted = "Listen-Only Monitoring",
                explanation = "$ctafDesc. NOTE: LISTEN ONLY — UAS pilots are not authorized to talk or transmit on aviation air frequencies (14 CFR § 107.37 manned traffic awareness only)."
            )
        )

        val worstStatus = rules.maxByOrNull { it.status.priority }?.status ?: AssessmentStatus.GO
        val summary = when (worstStatus) {
            AssessmentStatus.NO_GO -> "Airspace restriction or authorization violation detected"
            AssessmentStatus.CAUTION -> "Aviation notices or Special Use Airspace in vicinity"
            AssessmentStatus.GO -> "No active airspace restrictions detected"
            AssessmentStatus.DATA_UNAVAILABLE -> "Airspace telemetry incomplete"
        }

        return CategoryAssessment(
            category = category,
            status = worstStatus,
            ruleResults = rules,
            summary = summary
        )
    }
}

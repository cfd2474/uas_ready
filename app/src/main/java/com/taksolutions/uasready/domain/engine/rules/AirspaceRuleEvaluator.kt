package com.taksolutions.uasready.domain.engine.rules

import com.taksolutions.uasready.domain.engine.AssessmentContext
import com.taksolutions.uasready.domain.engine.CategoryRuleEvaluator
import com.taksolutions.uasready.domain.model.*

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

        if (activeTfrs.isNotEmpty()) {
            val tfr = activeTfrs.first()
            val others = if (activeTfrs.size > 1) " (+${activeTfrs.size - 1} more)" else ""
            rules.add(
                RuleResult(
                    ruleId = "AIR-TFR-001",
                    category = category,
                    status = AssessmentStatus.NO_GO,
                    title = "Active Temporary Flight Restriction (TFR)",
                    inputValueFormatted = "TFR ${tfr.id}$others",
                    thresholdFormatted = "Zero Active TFRs in flight window",
                    explanation = "The launch point is inside ${tfr.type} TFR ${tfr.id}, in effect during the planned flight window " +
                        "and below your planned altitude: ${tfr.description}. Flight is prohibited unless the NOTAM's conditions are met " +
                        "(see the NOTAM text at tfr.faa.gov)."
                )
            )
        } else if (!airspace.tfrDataSourced) {
            // No authoritative TFR feed answered: an empty list means "not checked", never "clear".
            rules.add(
                RuleResult(
                    ruleId = "AIR-TFR-003",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "Temporary Flight Restrictions Not Verified",
                    inputValueFormatted = "Not Checked",
                    thresholdFormatted = "Pilot Verification Required",
                    explanation = "The FAA TFR feed could not be reached, so TFRs were not checked. Before flight, verify there are no active TFRs at this location and time using the FAA TFR list (tfr.faa.gov) or an FAA-approved B4UFLY / LAANC provider."
                )
            )
        } else {
            airspace.scheduledTfrsOutsideWindow.firstOrNull()?.let { tfr ->
                rules.add(
                    RuleResult(
                        ruleId = "AIR-TFR-004",
                        category = category,
                        status = AssessmentStatus.CAUTION,
                        title = "Scheduled TFR at Launch Point (Inactive in Window)",
                        inputValueFormatted = "TFR ${tfr.id}",
                        thresholdFormatted = "Outside scheduled hours",
                        explanation = "The launch point is inside ${tfr.type} TFR ${tfr.id}, which by its published schedule is not active during " +
                            "the planned window: ${tfr.description}. Confirm the hours in the NOTAM and do not fly into its active period."
                    )
                )
            }
            if (airspace.unmappedRegionalTfrs.isNotEmpty()) {
                val list = airspace.unmappedRegionalTfrs
                rules.add(
                    RuleResult(
                        ruleId = "AIR-TFR-005",
                        category = category,
                        status = AssessmentStatus.CAUTION,
                        title = "TFR Without Mapped Boundary in This Area",
                        inputValueFormatted = list.joinToString(", ") { "TFR ${it.id}" },
                        thresholdFormatted = "Read NOTAM text",
                        explanation = "The FAA publishes no boundary for these TFRs, so UASReady cannot tell whether your launch point is inside: " +
                            list.joinToString("; ") { "${it.id} ${it.description}" } +
                            ". Read the NOTAM text at tfr.faa.gov to confirm it does not cover your location."
                    )
                )
            }
            if (airspace.scheduledTfrsOutsideWindow.isEmpty() && airspace.unmappedRegionalTfrs.isEmpty()) {
                val nationwide = if (airspace.unmappedNationwideTfrCount > 0)
                    " ${airspace.unmappedNationwideTfrCount} nationwide notices have no mapped area (for example the 14 CFR § 99.7 national-security UAS sites); check B4UFLY near military or critical-infrastructure sites."
                else ""
                rules.add(
                    RuleResult(
                        ruleId = "AIR-TFR-002",
                        category = category,
                        status = AssessmentStatus.GO,
                        title = "Temporary Flight Restrictions",
                        inputValueFormatted = "None Active",
                        thresholdFormatted = "Clear",
                        explanation = "Checked against the live FAA TFR feed: no TFR area covers the launch point below your planned altitude during the flight window.$nationwide"
                    )
                )
            }
        }

        // 2. Controlled Airspace Classification & Authorization (LAANC applies to B/C/D/E-surface only)
        val controlledClass = airspace.controlledClass
            ?: airspace.primaryClass.takeIf { it != AirspaceClass.SPECIAL_USE }
            ?: AirspaceClass.CLASS_G
        val isNonClassG = controlledClass != AirspaceClass.CLASS_G
        val formattedClassName = when (controlledClass) {
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

        // 4. Prohibited / Restricted areas at the launch point. LAANC cannot authorize these.
        val specialUseType = airspace.specialUseType
            ?: if (airspace.primaryClass == AirspaceClass.SPECIAL_USE) SpecialUseType.RESTRICTED else null
        val specialUseName = airspace.specialUseName ?: "Special Use Airspace"
        when (specialUseType) {
            SpecialUseType.PROHIBITED -> rules.add(
                RuleResult(
                    ruleId = "AIR-SUA-P01",
                    category = category,
                    status = AssessmentStatus.NO_GO,
                    title = "Prohibited Area",
                    inputValueFormatted = specialUseName,
                    thresholdFormatted = "Outside Prohibited Areas",
                    explanation = String.format("Launch point is inside %s. Flight is prohibited (14 CFR § 73.83) unless authorized by the using agency. LAANC cannot authorize flight in prohibited areas.", specialUseName)
                )
            )
            SpecialUseType.RESTRICTED -> rules.add(
                RuleResult(
                    ruleId = "AIR-SUA-R01",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "Restricted Area",
                    inputValueFormatted = specialUseName,
                    thresholdFormatted = "Inactive or Agency Authorization",
                    explanation = String.format("Launch point is inside %s. When the area is active, flight requires permission of the controlling/using agency (14 CFR § 73.13). Check activation times (NOTAMs / FAA SUA schedule) and contact the controlling agency before flight. LAANC cannot authorize flight in restricted areas.", specialUseName)
                )
            )
            null -> Unit
        }

        // 5. Cached airspace (live FAA service unavailable)
        if (airspace.isStale) {
            val ageHours = ((System.currentTimeMillis() - airspace.timestampEpochMs) / 3_600_000L).coerceAtLeast(0)
            rules.add(
                RuleResult(
                    ruleId = "AIR-CACHE-001",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "Cached Airspace Data",
                    inputValueFormatted = String.format("%d h old", ageHours),
                    thresholdFormatted = "Live FAA data",
                    explanation = String.format("The live FAA airspace service was unavailable, so airspace from a lookup %d hours ago near this location is shown. Published airspace changes on a 28-day cycle; verify with an FAA-approved source if in doubt.", ageHours)
                )
            )
        }

        // 6. Ground elevation unknown → MSL-based floors assumed low (conservative)
        if (airspace.groundElevationFt == null && airspace.zones.any { it.floorReference == AltitudeReference.MSL }) {
            rules.add(
                RuleResult(
                    ruleId = "AIR-ELEV-001",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "Ground Elevation Unavailable",
                    inputValueFormatted = "Unknown",
                    thresholdFormatted = "Terrain elevation required",
                    explanation = "Ground elevation could not be determined, so airspace with floors defined above sea level is treated as reaching your altitude. Some airspace shown may actually be above you."
                )
            )
        }

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

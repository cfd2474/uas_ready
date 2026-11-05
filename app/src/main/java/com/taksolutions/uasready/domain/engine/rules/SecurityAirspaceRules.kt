package com.taksolutions.uasready.domain.engine.rules

import com.taksolutions.uasready.domain.model.*

/**
 * Rules for national-security UAS restrictions. Contract and wording sources:
 * docs/PLATFORM_REFERENCE.md "FAA National Security UAS Flight Restrictions" and "DC SFRA / FRZ".
 */
object SecurityAirspaceRules {

    private val category = AssessmentCategory.AIRSPACE

    fun evaluate(airspace: AirspaceInfo): List<RuleResult> {
        if (!airspace.securityAirspaceSourced) {
            return listOf(
                RuleResult(
                    ruleId = "AIR-NSUAS-002",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "National-Security UAS Restrictions Not Verified",
                    inputValueFormatted = "Not Checked",
                    thresholdFormatted = "Pilot Verification Required",
                    explanation = "The FAA national-security UAS layer could not be reached, so 14 CFR § 99.7 no-drone areas were not checked. " +
                        "These include the 1 NM strip along the US–Mexico border, military bases, ports of entry and the DC Flight Restricted Zone. " +
                        "Check B4UFLY or the FAA UAS Data Delivery System before flight."
                )
            )
        }

        val rules = mutableListOf<RuleResult>()
        val inside = airspace.nationalSecuritySitesAtLaunch
        if (inside.isNotEmpty()) {
            val site = inside.first()
            val others = if (inside.size > 1) " (+${inside.size - 1} more)" else ""
            rules += RuleResult(
                ruleId = "AIR-NSUAS-001",
                category = category,
                status = AssessmentStatus.NO_GO,
                title = "National-Security UAS Restriction",
                inputValueFormatted = site.name + others,
                thresholdFormatted = "Outside 14 CFR § 99.7 UAS restrictions",
                explanation = "The launch point is inside ${site.name}${agencyText(site)}, a national-security UAS flight restriction " +
                    "(14 CFR § 99.7 special security instructions), ${site.floorText} to ${site.ceilingText}. " +
                    "Drone flight is prohibited without a waiver from the responsible agency" +
                    (site.contact.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: "") + ". Violations can lead to civil penalties, criminal charges, or the aircraft being disabled."
            )
        }
        val nearby = airspace.nationalSecuritySitesNearby
        if (nearby.isNotEmpty()) {
            rules += RuleResult(
                ruleId = "AIR-NSUAS-004",
                category = category,
                status = AssessmentStatus.CAUTION,
                title = "National-Security UAS Restriction Within 1 NM",
                inputValueFormatted = nearby.first().name + if (nearby.size > 1) " (+${nearby.size - 1} more)" else "",
                thresholdFormatted = "Keep the aircraft out",
                explanation = "A national-security no-drone area lies within 1 NM of the launch point: " +
                    nearby.joinToString("; ") { it.name + agencyText(it) } +
                    ". Flying into it is prohibited without a waiver. Check its boundary on the map and keep the aircraft clear."
            )
        }

        when (airspace.dcSfraZone) {
            DcSfraZone.FRZ -> rules += RuleResult(
                ruleId = "AIR-DC-001",
                category = category,
                status = AssessmentStatus.NO_GO,
                title = "Washington DC Flight Restricted Zone",
                inputValueFormatted = "DC FRZ",
                thresholdFormatted = "TSA/FAA airspace waiver",
                explanation = "The launch point is inside the Washington DC Flight Restricted Zone. Under FDC NOTAM 5/7224, UAS operations " +
                    "are prohibited in the DC FRZ unless specifically authorized through the TSA/FAA airspace waiver process."
            )
            DcSfraZone.SFRA -> rules += RuleResult(
                ruleId = "AIR-DC-002",
                category = category,
                status = AssessmentStatus.CAUTION,
                title = "Washington DC Special Flight Rules Area",
                inputValueFormatted = "DC SFRA",
                thresholdFormatted = "FDC NOTAM 5/7224 conditions",
                explanation = "The launch point is inside the Washington DC SFRA (outside the FRZ). FDC NOTAM 5/7224 allows UAS here only under its conditions. " +
                    "Recreational flyers: drone registered and marked, under 55 lb, at or below 400 ft AGL, within visual line of sight, in VMC, not at night. " +
                    "Part 107 and public operators: comply with Part 107 or your exemption / COA. Never enter the FRZ, and stay clear of restricted and prohibited areas and TFRs."
            )
            null -> Unit
        }

        if (rules.isEmpty()) {
            rules += RuleResult(
                ruleId = "AIR-NSUAS-003",
                category = category,
                status = AssessmentStatus.GO,
                title = "National-Security UAS Restrictions",
                inputValueFormatted = "None at Launch Point",
                thresholdFormatted = "Clear",
                explanation = "Checked against the live FAA national-security UAS layer, which includes the US–Mexico border strip, " +
                    "and the DC SFRA/FRZ boundaries: none covers the launch point or lies within 1 NM."
            )
        }
        return rules
    }

    private fun agencyText(site: NationalSecurityUasSite) = if (site.agency.isNotBlank()) " (${site.agency})" else ""
}

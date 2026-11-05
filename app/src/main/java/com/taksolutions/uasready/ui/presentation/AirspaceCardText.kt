package com.taksolutions.uasready.ui.presentation

import com.taksolutions.uasready.domain.model.*

/** Home "Airspace" card text and status. Pure, so the safety wording is unit-testable. */
data class AirspaceCard(val text: CardText, val status: AssessmentStatus?)

object AirspaceCardText {

    fun of(airspace: AirspaceInfo?, assessment: AssessmentResult?): AirspaceCard {
        val rules = assessment?.allRuleResults.orEmpty()
        val category = assessment?.categoryAssessments?.firstOrNull { it.category == AssessmentCategory.AIRSPACE }
        fun rule(id: String) = rules.firstOrNull { it.ruleId == id }
        val ctrlRule = rules.firstOrNull { it.ruleId.startsWith("AIR-CTRL") }
        val tfrRules = rules.filter { it.ruleId.startsWith("AIR-TFR") }
        val tfrWorst = tfrRules.maxByOrNull { it.status.priority }?.status
        val securityRules = rules.filter { it.ruleId.startsWith("AIR-NSUAS") || it.ruleId.startsWith("AIR-DC") }
        val nsInside = rule("AIR-NSUAS-001")
        val dcFrz = rule("AIR-DC-001")
        val dcSfra = rule("AIR-DC-002")
        val nsNearby = rule("AIR-NSUAS-004")

        val classText = when (airspace?.primaryClass) {
            AirspaceClass.CLASS_B -> "Class B"
            AirspaceClass.CLASS_C -> "Class C"
            AirspaceClass.CLASS_D -> "Class D"
            AirspaceClass.CLASS_E_SURFACE -> "Class E Surface"
            AirspaceClass.CLASS_E -> "Class E"
            AirspaceClass.SPECIAL_USE -> if (airspace.specialUseType == SpecialUseType.PROHIBITED) "Prohibited Area" else "Restricted Area"
            AirspaceClass.CLASS_G -> "Class G"
            null -> "Unavailable"
        }

        // Never claim "No TFRs" unless the authoritative TFR feed was actually checked.
        val restrictionSummary = when {
            nsInside != null -> "NO-DRONE ZONE"
            dcFrz != null -> "DC FRZ"
            tfrWorst == AssessmentStatus.NO_GO -> "ACTIVE TFR"
            dcSfra != null -> "DC SFRA"
            airspace?.tfrDataSourced == true && tfrWorst == AssessmentStatus.CAUTION -> "TFR nearby"
            airspace?.tfrDataSourced == true -> "No TFRs"
            else -> "TFRs unverified"
        }

        val primary = when {
            airspace == null -> "Airspace Unavailable"
            airspace.isStale -> "Cached: $classText • $restrictionSummary"
            else -> "$classText • $restrictionSummary"
        }

        val secondary = when {
            airspace == null -> "Live FAA data required"
            nsInside != null -> "National security: waiver required"
            dcFrz != null -> "TSA/FAA waiver required"
            tfrWorst == AssessmentStatus.NO_GO -> "Flight Restricted / Prohibited"
            airspace.specialUseType == SpecialUseType.PROHIBITED -> "No flight without agency authorization"
            airspace.specialUseType == SpecialUseType.RESTRICTED -> "Contact controlling agency"
            nsNearby != null -> "No-drone zone within 1 NM"
            dcSfra != null -> "Follow NOTAM 5/7224 rules"
            airspace.tfrDataSourced && tfrWorst == AssessmentStatus.CAUTION -> "Read TFR NOTAM before flight"
            ctrlRule?.status == AssessmentStatus.CAUTION -> "LAANC Approval Required"
            !airspace.securityAirspaceSourced -> "No-drone zones unverified"
            airspace.tfrDataSourced -> "No flight restrictions"
            else -> "Check TFRs before flight"
        }

        val status = (listOfNotNull(category?.status, ctrlRule?.status, tfrWorst) + securityRules.map { it.status })
            .maxByOrNull { it.priority } ?: category?.status
        return AirspaceCard(CardText(primary, secondary), status)
    }
}

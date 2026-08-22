package com.uasready.domain.engine.rules

import com.uasready.domain.engine.AssessmentContext
import com.uasready.domain.engine.CategoryRuleEvaluator
import com.uasready.domain.model.*

class PilotAuthorityRuleEvaluator : CategoryRuleEvaluator {
    override val category: AssessmentCategory = AssessmentCategory.PILOT_QUALIFICATIONS

    override fun evaluate(context: AssessmentContext): CategoryAssessment {
        val pilot = context.pilot
        val sunData = context.sunData
        val flightWindow = context.flightWindow
        val aircraft = context.aircraft
        val rules = mutableListOf<RuleResult>()

        when (pilot.activeAuthority) {
            PilotAuthorityType.PART_107 -> {
                // Base 107 Authority
                rules.add(
                    RuleResult(
                        ruleId = "PLT-107-AUTH-001",
                        category = category,
                        status = AssessmentStatus.GO,
                        title = "107 License Authority",
                        inputValueFormatted = "107 License",
                        thresholdFormatted = "14 CFR Part 107",
                        explanation = "Operating under standard FAA 107 License. Cleared for daylight and nighttime operations.",
                        applicableAuthority = "107 License"
                    )
                )

                // Night Flight Evaluation under Part 107
                if (sunData != null) {
                    val isNight = sunData.isDarknessAt(flightWindow.startEpochMs) || sunData.isDarknessAt(flightWindow.endEpochMs)
                    if (isNight) {
                        if (!aircraft.limitations.nightOperationCapable) {
                            rules.add(
                                RuleResult(
                                    ruleId = "PLT-107-NGT-001",
                                    category = category,
                                    status = AssessmentStatus.NO_GO,
                                    title = "Anti-Collision Lighting Required",
                                    inputValueFormatted = "No Anti-Collision Strobe",
                                    thresholdFormatted = "3 SM Strobe Required",
                                    explanation = "107 License night operations require aircraft anti-collision lighting visible for at least 3 statute miles.",
                                    applicableAircraft = aircraft.displayName,
                                    applicableAuthority = "107 License"
                                )
                            )
                        } else {
                            rules.add(
                                RuleResult(
                                    ruleId = "PLT-107-NGT-002",
                                    category = category,
                                    status = AssessmentStatus.GO,
                                    title = "107 Night Flight Cleared",
                                    inputValueFormatted = "107 Night Operations",
                                    thresholdFormatted = "Aircraft Strobe Equipped",
                                    explanation = "107 Pilot is cleared for night flight. Aircraft is equipped with anti-collision lighting.",
                                    applicableAuthority = "107 License"
                                )
                            )
                        }
                    }
                }
            }

            PilotAuthorityType.PUBLIC_COA -> {
                rules.add(
                    RuleResult(
                        ruleId = "PLT-COA-AUTH-001",
                        category = category,
                        status = AssessmentStatus.GO,
                        title = "Public COA Authority",
                        inputValueFormatted = "Public COA",
                        thresholdFormatted = "Daylight Window Only",
                        explanation = "Operating under Public Agency COA. Flight permitted from 30 minutes before sunrise to 30 minutes after sunset.",
                        applicableAuthority = "Public COA"
                    )
                )

                // Daylight/Civil Twilight Window Enforcement for Public COA (Hard NO-GO for night)
                if (sunData != null) {
                    val allowedStartMs = sunData.civilDawnEpochMs  // 30 min before sunrise
                    val allowedEndMs = sunData.civilDuskEpochMs    // 30 min after sunset

                    val outsideDaylightWindow = flightWindow.startEpochMs < allowedStartMs || flightWindow.endEpochMs > allowedEndMs

                    if (outsideDaylightWindow) {
                        rules.add(
                            RuleResult(
                                ruleId = "PLT-COA-NGT-001",
                                category = category,
                                status = AssessmentStatus.NO_GO,
                                title = "Night Flight Prohibited Under COA",
                                inputValueFormatted = "Night / Outside Daylight Window",
                                thresholdFormatted = "30m Pre-Sunrise to 30m Post-Sunset Only",
                                explanation = "Public COA flight operations are strictly prohibited at night. Operations must remain within 30 minutes before civil sunrise and 30 minutes after civil sunset.",
                                applicableAuthority = "Public COA"
                            )
                        )
                    } else {
                        rules.add(
                            RuleResult(
                                ruleId = "PLT-COA-NGT-002",
                                category = category,
                                status = AssessmentStatus.GO,
                                title = "COA Daylight Window Satisfied",
                                inputValueFormatted = "Daylight Window",
                                thresholdFormatted = "Civil Twilight to Civil Twilight",
                                explanation = "Flight window is fully within the permitted COA daylight window (30 min before sunrise to 30 min after sunset).",
                                applicableAuthority = "Public COA"
                            )
                        )
                    }
                }
            }
        }

        val worstStatus = rules.maxByOrNull { it.status.priority }?.status ?: AssessmentStatus.GO
        val summary = when (worstStatus) {
            AssessmentStatus.NO_GO -> "Pilot operating authority constraints violated"
            AssessmentStatus.CAUTION -> "Operating authority advisory"
            AssessmentStatus.GO -> "${pilot.activeAuthority.displayName} operating criteria satisfied"
            AssessmentStatus.DATA_UNAVAILABLE -> "Authority calculation incomplete"
        }

        return CategoryAssessment(
            category = category,
            status = worstStatus,
            ruleResults = rules,
            summary = summary
        )
    }
}

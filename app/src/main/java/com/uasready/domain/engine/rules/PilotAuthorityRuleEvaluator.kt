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
                        title = "107 Licensed Pilot Authority",
                        inputValueFormatted = "107 License",
                        thresholdFormatted = "14 CFR Part 107",
                        explanation = "Operating under standard FAA 107 License. Cleared for daylight and nighttime operations.",
                        applicableAuthority = "Licensed Pilot"
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
                                    explanation = "Licensed Pilot night operations require aircraft anti-collision lighting visible for at least 3 statute miles.",
                                    applicableAircraft = aircraft.displayName,
                                    applicableAuthority = "Licensed Pilot"
                                )
                            )
                        } else {
                            rules.add(
                                RuleResult(
                                    ruleId = "PLT-107-NGT-002",
                                    category = category,
                                    status = AssessmentStatus.GO,
                                    title = "107 Night Flight Cleared",
                                    inputValueFormatted = "Night Operations",
                                    thresholdFormatted = "Aircraft Strobe Equipped",
                                    explanation = "Licensed Pilot is cleared for night flight. Aircraft is equipped with anti-collision lighting.",
                                    applicableAuthority = "Licensed Pilot"
                                )
                            )
                        }
                    }
                }
            }

            PilotAuthorityType.PUBLIC_COA -> {
                // Non-Licensed Pilot Base Authority
                rules.add(
                    RuleResult(
                        ruleId = "PLT-NONLIC-AUTH-001",
                        category = category,
                        status = AssessmentStatus.GO,
                        title = "Non-Licensed Pilot Operating Criteria",
                        inputValueFormatted = "Non-Licensed Pilot",
                        thresholdFormatted = "Daylight Window Only",
                        explanation = "Operating as a Non-licensed Pilot. Flight operations are permitted from 30 minutes before sunrise to 30 minutes after sunset.",
                        applicableAuthority = "Non-Licensed Pilot"
                    )
                )

                // Daylight/Civil Twilight Window Enforcement for Non-Licensed Pilot
                if (sunData != null) {
                    val allowedStartMs = sunData.civilDawnEpochMs  // 30 min before sunrise
                    val allowedEndMs = sunData.civilDuskEpochMs    // 30 min after sunset

                    val startOutsideWindow = flightWindow.startEpochMs < allowedStartMs || flightWindow.startEpochMs > allowedEndMs

                    if (startOutsideWindow) {
                        val isPreDawn = flightWindow.startEpochMs < allowedStartMs
                        rules.add(
                            RuleResult(
                                ruleId = "PLT-NONLIC-NGT-001",
                                category = category,
                                status = AssessmentStatus.NO_GO,
                                title = if (isPreDawn) "Pre-Dawn Flight Prohibited (Non-Licensed Pilot)" else "Night Flight Prohibited (Non-Licensed Pilot)",
                                inputValueFormatted = "Outside Daylight Window",
                                thresholdFormatted = String.format("30m Pre-Sunrise to 30m Post-Sunset (%s - %s)", formatEpochTime(allowedStartMs), formatEpochTime(allowedEndMs)),
                                explanation = if (isPreDawn) {
                                    String.format("Operations cannot begin until 30 minutes before sunrise (%s). Non-licensed Pilots are prohibited from night and pre-dawn flights.", formatEpochTime(allowedStartMs))
                                } else {
                                    String.format("Daylight operating window concluded at %s (30 minutes after sunset). Night flight operations are strictly prohibited for Non-licensed Pilots.", formatEpochTime(allowedEndMs))
                                },
                                applicableAuthority = "Non-Licensed Pilot"
                            )
                        )
                    } else if (flightWindow.endEpochMs > allowedEndMs) {
                        // Started in daylight, but planned window exceeds sunset/dusk
                        val minRemaining = ((allowedEndMs - flightWindow.startEpochMs) / (60 * 1000)).coerceAtLeast(0)
                        rules.add(
                            RuleResult(
                                ruleId = "PLT-NONLIC-NGT-003",
                                category = category,
                                status = AssessmentStatus.CAUTION,
                                title = "Flight Window Exceeds Sunset",
                                inputValueFormatted = String.format("%d min daylight remaining", minRemaining),
                                thresholdFormatted = String.format("Conclude by %s", formatEpochTime(allowedEndMs)),
                                explanation = String.format(
                                    "Operations are permitted right now in daylight, but all flights must conclude before 30 minutes after sunset (%s). You have %d minutes of legal daylight remaining.",
                                    formatEpochTime(allowedEndMs),
                                    minRemaining
                                ),
                                applicableAuthority = "Non-Licensed Pilot"
                            )
                        )
                    } else {
                        rules.add(
                            RuleResult(
                                ruleId = "PLT-NONLIC-NGT-002",
                                category = category,
                                status = AssessmentStatus.GO,
                                title = "Daylight Operating Window Satisfied",
                                inputValueFormatted = String.format("Within Window (%s - %s)", formatEpochTime(allowedStartMs), formatEpochTime(allowedEndMs)),
                                thresholdFormatted = "30m Pre-Sunrise to 30m Post-Sunset",
                                explanation = String.format(
                                    "Flight window is fully within permitted daylight hours for Non-licensed Pilots (30 minutes before sunrise at %s to 30 minutes after sunset at %s).",
                                    formatEpochTime(allowedStartMs),
                                    formatEpochTime(allowedEndMs)
                                ),
                                applicableAuthority = "Non-Licensed Pilot"
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

    private fun formatEpochTime(epochMs: Long): String {
        val date = java.util.Date(epochMs)
        val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
        return format.format(date)
    }
}

package com.taksolutions.uasready.domain.engine.rules

import com.taksolutions.uasready.domain.engine.AssessmentContext
import com.taksolutions.uasready.domain.engine.CategoryRuleEvaluator
import com.taksolutions.uasready.domain.model.*

class DaylightRuleEvaluator : CategoryRuleEvaluator {
    override val category: AssessmentCategory = AssessmentCategory.DAYLIGHT

    override fun evaluate(context: AssessmentContext): CategoryAssessment {
        val sunData = context.sunData
        if (sunData == null) {
            return CategoryAssessment(
                category = category,
                status = AssessmentStatus.DATA_UNAVAILABLE,
                ruleResults = listOf(
                    RuleResult(
                        ruleId = "SUN-MISSING",
                        category = category,
                        status = AssessmentStatus.DATA_UNAVAILABLE,
                        title = "Solar Ephemeris Unavailable",
                        inputValueFormatted = "No Telemetry",
                        thresholdFormatted = "Solar Ephemeris Required",
                        explanation = "Unable to calculate solar twilight and sunset times for the target coordinates."
                    )
                ),
                summary = "Missing solar ephemeris calculation"
            )
        }

        val rules = mutableListOf<RuleResult>()
        val flightWindow = context.flightWindow
        val pilot = context.pilot

        val startsInDaylight = sunData.isDaylightAt(flightWindow.startEpochMs)
        val endsInDaylight = sunData.isDaylightAt(flightWindow.endEpochMs)
        val endsInTwilight = sunData.isCivilTwilightAt(flightWindow.endEpochMs)
        val endsInDarkness = sunData.isDarknessAt(flightWindow.endEpochMs)

        // 1. Reference Card for Non-Licensed Pilot: Display exact permitted daylight flight window
        if (pilot.activeAuthority == PilotAuthorityType.PUBLIC_COA) {
            rules.add(
                RuleResult(
                    ruleId = "SUN-NONLIC-REF-001",
                    category = category,
                    status = AssessmentStatus.GO,
                    title = "Permitted Daylight Window (Non-Licensed Pilot)",
                    inputValueFormatted = "${formatEpochTime(sunData.civilDawnEpochMs)} to ${formatEpochTime(sunData.civilDuskEpochMs)}",
                    thresholdFormatted = "30m pre-sunrise to 30m post-sunset",
                    explanation = String.format(
                        "For Non-licensed Pilots, legal flight operations are permitted from 30 minutes before sunrise (%s) to 30 minutes after sunset (%s). Sunrise: %s, Sunset: %s.",
                        formatEpochTime(sunData.civilDawnEpochMs),
                        formatEpochTime(sunData.civilDuskEpochMs),
                        formatEpochTime(sunData.sunriseEpochMs),
                        formatEpochTime(sunData.sunsetEpochMs)
                    ),
                    applicableAuthority = "Non-Licensed Pilot"
                )
            )
        }

        // 2. Daylight & Twilight Timing Evaluation
        when {
            startsInDaylight && endsInDaylight -> {
                val minUntilSunset = (sunData.sunsetEpochMs - flightWindow.endEpochMs) / (60 * 1000)
                if (minUntilSunset in 0..30) {
                    rules.add(
                        RuleResult(
                            ruleId = "SUN-DUR-001",
                            category = category,
                            status = AssessmentStatus.CAUTION,
                            title = "Flight Window Closes Near Sunset",
                            inputValueFormatted = String.format("%d min before sunset", minUntilSunset),
                            thresholdFormatted = "> 30 min before sunset",
                            explanation = String.format("Planned flight window completes %d minutes prior to sunset (Sunset: %s). Monitor visual line-of-sight and fading ambient light.", minUntilSunset, formatEpochTime(sunData.sunsetEpochMs))
                        )
                    )
                } else {
                    rules.add(
                        RuleResult(
                            ruleId = "SUN-DUR-002",
                            category = category,
                            status = AssessmentStatus.GO,
                            title = "Daylight Flight Window",
                            inputValueFormatted = "Full Daylight",
                            thresholdFormatted = "Sunrise to Sunset",
                            explanation = String.format("Flight window is fully within daylight hours (Sunrise: %s • Sunset: %s).", formatEpochTime(sunData.sunriseEpochMs), formatEpochTime(sunData.sunsetEpochMs))
                        )
                    )
                }
            }

            endsInTwilight -> {
                rules.add(
                    RuleResult(
                        ruleId = "SUN-TWI-001",
                        category = category,
                        status = AssessmentStatus.CAUTION,
                        title = "Civil Twilight Flight Window",
                        inputValueFormatted = "Civil Twilight",
                        thresholdFormatted = "Anti-collision strobe required",
                        explanation = String.format("Planned flight operates during civil twilight (Civil Dusk: %s). High-intensity anti-collision strobe (3 SM visibility) required.", formatEpochTime(sunData.civilDuskEpochMs))
                    )
                )
            }

            endsInDarkness -> {
                rules.add(
                    RuleResult(
                        ruleId = "SUN-DRK-001",
                        category = category,
                        status = AssessmentStatus.CAUTION,
                        title = "Night Operation Window",
                        inputValueFormatted = "Night",
                        thresholdFormatted = "Night qualified pilot & aircraft strobe",
                        explanation = String.format("Flight window extends into full darkness (Past Civil Dusk: %s). Night operations require active anti-collision lighting and pilot night endorsement.", formatEpochTime(sunData.civilDuskEpochMs))
                    )
                )
            }
        }

        val worstStatus = rules.maxByOrNull { it.status.priority }?.status ?: AssessmentStatus.GO
        val summary = when (worstStatus) {
            AssessmentStatus.NO_GO -> "Solar / daylight criteria violated"
            AssessmentStatus.CAUTION -> "Flight operates near sunset, in civil twilight, or at night"
            AssessmentStatus.GO -> "Flight window remains within permitted daylight hours"
            AssessmentStatus.DATA_UNAVAILABLE -> "Solar calculation unavailable"
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

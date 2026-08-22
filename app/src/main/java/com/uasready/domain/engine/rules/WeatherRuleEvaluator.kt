package com.uasready.domain.engine.rules

import com.uasready.domain.engine.AssessmentContext
import com.uasready.domain.engine.CategoryRuleEvaluator
import com.uasready.domain.model.*

class WeatherRuleEvaluator : CategoryRuleEvaluator {
    override val category: AssessmentCategory = AssessmentCategory.WEATHER

    override fun evaluate(context: AssessmentContext): CategoryAssessment {
        val weather = context.weather
        if (weather == null) {
            return CategoryAssessment(
                category = category,
                status = AssessmentStatus.DATA_UNAVAILABLE,
                ruleResults = listOf(
                    RuleResult(
                        ruleId = "WX-MISSING",
                        category = category,
                        status = AssessmentStatus.DATA_UNAVAILABLE,
                        title = "Live Weather Unavailable",
                        inputValueFormatted = "No Data",
                        thresholdFormatted = "Live Connection Required",
                        explanation = "Live weather telemetry could not be retrieved. Flight assessment cannot be certified safe."
                    )
                ),
                summary = "Missing live meteorological telemetry"
            )
        }

        val rules = mutableListOf<RuleResult>()

        // 1. Visibility Rule (Part 107 requires >= 3 Statute Miles)
        val vis = weather.visibilityStatuteMiles
        when {
            vis < 3.0 -> rules.add(
                RuleResult(
                    ruleId = "WX-VIS-001",
                    category = category,
                    status = AssessmentStatus.NO_GO,
                    title = "Flight Visibility (Below Minimum)",
                    inputValueFormatted = String.format("%.1f SM", vis),
                    thresholdFormatted = ">= 3.0 SM (14 CFR § 107.51(c))",
                    explanation = String.format("Current flight visibility of %.1f SM is below the 3.0 statute mile statutory minimum for sUAS operations.", vis)
                )
            )
            vis < 4.0 -> rules.add(
                RuleResult(
                    ruleId = "WX-VIS-002",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "Flight Visibility (Marginal)",
                    inputValueFormatted = String.format("%.1f SM", vis),
                    thresholdFormatted = ">= 4.0 SM recommended",
                    explanation = String.format("Marginal visibility of %.1f SM. Visual Observer and heightened manned-aircraft scan required.", vis)
                )
            )
            else -> rules.add(
                RuleResult(
                    ruleId = "WX-VIS-003",
                    category = category,
                    status = AssessmentStatus.GO,
                    title = "Flight Visibility",
                    inputValueFormatted = String.format("%.1f SM", vis),
                    thresholdFormatted = ">= 3.0 SM",
                    explanation = String.format("Visibility (%.1f SM) satisfies operational minimums.", vis)
                )
            )
        }

        // 2. Cloud Ceiling Clearance Rule (500 ft below clouds)
        val ceiling = weather.cloudCeilingFt
        val plannedAlt = context.plannedAltitudeAglFt
        val minRequiredCeiling = plannedAlt + 500.0

        if (ceiling != null) {
            when {
                ceiling < minRequiredCeiling -> rules.add(
                    RuleResult(
                        ruleId = "WX-CEIL-001",
                        category = category,
                        status = AssessmentStatus.NO_GO,
                        title = "Cloud Ceiling Clearance",
                        inputValueFormatted = String.format("%.0f ft AGL", ceiling),
                        thresholdFormatted = String.format(">= %.0f ft AGL (500 ft above planned %.0f ft flight)", minRequiredCeiling, plannedAlt),
                        explanation = String.format("Cloud ceiling of %.0f ft AGL violates the required 500 ft vertical cloud clearance for a %.0f ft AGL flight.", ceiling, plannedAlt)
                    )
                )
                ceiling < minRequiredCeiling + 400.0 -> rules.add(
                    RuleResult(
                        ruleId = "WX-CEIL-002",
                        category = category,
                        status = AssessmentStatus.CAUTION,
                        title = "Low Cloud Ceiling",
                        inputValueFormatted = String.format("%.0f ft AGL", ceiling),
                        thresholdFormatted = String.format("> %.0f ft AGL", minRequiredCeiling + 400.0),
                        explanation = String.format("Cloud ceiling (%.0f ft AGL) is near the regulatory 500 ft buffer above planned operating altitude.", ceiling)
                    )
                )
                else -> rules.add(
                    RuleResult(
                        ruleId = "WX-CEIL-003",
                        category = category,
                        status = AssessmentStatus.GO,
                        title = "Cloud Ceiling",
                        inputValueFormatted = String.format("%.0f ft AGL", ceiling),
                        thresholdFormatted = String.format(">= %.0f ft AGL", minRequiredCeiling),
                        explanation = String.format("Cloud ceiling (%.0f ft AGL) provides ample clearance above operating altitude.", ceiling)
                    )
                )
            }
        } else {
            rules.add(
                RuleResult(
                    ruleId = "WX-CEIL-004",
                    category = category,
                    status = AssessmentStatus.GO,
                    title = "Cloud Ceiling",
                    inputValueFormatted = "Clear / Unlimited",
                    thresholdFormatted = ">= 500 ft above flight",
                    explanation = "No restrictive cloud ceiling detected."
                )
            )
        }

        // 3. Thunderstorm & Severe Convective Weather Rule
        val tstormProb = weather.thunderstormProbabilityPercent
        when {
            weather.precipitationType == PrecipitationType.THUNDERSTORM || tstormProb >= 50 -> rules.add(
                RuleResult(
                    ruleId = "WX-STM-001",
                    category = category,
                    status = AssessmentStatus.NO_GO,
                    title = "Thunderstorm / Severe Convective Activity",
                    inputValueFormatted = if (weather.precipitationType == PrecipitationType.THUNDERSTORM) "Active Thunderstorm" else "$tstormProb% Probability",
                    thresholdFormatted = "< 25% Probability",
                    explanation = "Active or imminent thunderstorm activity in operational zone. Extreme risk of severe downdrafts, microbursts, and lightning."
                )
            )
            tstormProb >= 25 -> rules.add(
                RuleResult(
                    ruleId = "WX-STM-002",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "Convective Activity Risk",
                    inputValueFormatted = "$tstormProb% Probability",
                    thresholdFormatted = "< 25% Probability",
                    explanation = "Elevated probability of thunderstorm formation during flight operations."
                )
            )
            else -> rules.add(
                RuleResult(
                    ruleId = "WX-STM-003",
                    category = category,
                    status = AssessmentStatus.GO,
                    title = "Convective Activity",
                    inputValueFormatted = "Low / None ($tstormProb%)",
                    thresholdFormatted = "< 25%",
                    explanation = "No significant thunderstorm or convective activity detected."
                )
            )
        }

        // 4. Forecast degradation evaluation across 120-minute flight window
        val forecast = context.forecast
        if (forecast != null && forecast.intervals.isNotEmpty()) {
            val windowSampling = context.flightWindow.getSamplingIntervals()
            for (sampleTime in windowSampling) {
                // Find matching forecast interval
                val interval = forecast.intervals.minByOrNull { Math.abs(it.timestampEpochMs - sampleTime) }
                if (interval != null) {
                    val offsetMin = (sampleTime - context.flightWindow.startEpochMs) / (60 * 1000)
                    if (interval.visibilityStatuteMiles < 3.0) {
                        val status = if (offsetMin < 60) AssessmentStatus.NO_GO else AssessmentStatus.CAUTION
                        rules.add(
                            RuleResult(
                                ruleId = if (offsetMin < 60) "WX-FCST-VIS-001" else "WX-FCST-VIS-002",
                                category = category,
                                status = status,
                                title = if (offsetMin < 60) "Forecast Visibility Below Min (< 60m)" else "Forecast Visibility Degraded in 60-120m Window",
                                inputValueFormatted = String.format("%.1f SM at +%dm", interval.visibilityStatuteMiles, offsetMin),
                                thresholdFormatted = ">= 3.0 SM",
                                explanation = if (offsetMin < 60) {
                                    String.format("Forecast visibility drops below 3.0 SM (%.1f SM) at +%d min into the 0–60 min flight window. Flight prohibited.", interval.visibilityStatuteMiles, offsetMin)
                                } else {
                                    String.format("Forecast visibility drops to %.1f SM at +%d min (60–120 min window). Short flight under 60 min permitted; plan for deteriorating visibility.", interval.visibilityStatuteMiles, offsetMin)
                                },
                                isForecastDerived = true,
                                forecastTimeOffsetMinutes = offsetMin
                            )
                        )
                    }
                    if (interval.thunderstormProbabilityPercent >= 50) {
                        val status = if (offsetMin < 60) AssessmentStatus.NO_GO else AssessmentStatus.CAUTION
                        rules.add(
                            RuleResult(
                                ruleId = if (offsetMin < 60) "WX-FCST-STM-001" else "WX-FCST-STM-002",
                                category = category,
                                status = status,
                                title = if (offsetMin < 60) "Forecast Thunderstorm Hazard (< 60m)" else "Forecast Thunderstorm in 60-120m Window",
                                inputValueFormatted = String.format("%d%% at +%dm", interval.thunderstormProbabilityPercent, offsetMin),
                                thresholdFormatted = "< 50%",
                                explanation = if (offsetMin < 60) {
                                    String.format("Forecast thunderstorm probability increases to %d%% at +%d min into the 0–60 min flight window. Immediate flight prohibited.", interval.thunderstormProbabilityPercent, offsetMin)
                                } else {
                                    String.format("Forecast convective activity increases to %d%% at +%d min (60–120 min window). Caution: Complete operations before cell development.", interval.thunderstormProbabilityPercent, offsetMin)
                                },
                                isForecastDerived = true,
                                forecastTimeOffsetMinutes = offsetMin
                            )
                        )
                    }
                }
            }
        }

        val worstStatus = rules.maxByOrNull { it.status.priority }?.status ?: AssessmentStatus.GO
        val summary = when (worstStatus) {
            AssessmentStatus.NO_GO -> "Meteorological criteria violated (visibility/ceiling/convective storm)"
            AssessmentStatus.CAUTION -> "Marginal meteorological conditions detected"
            AssessmentStatus.GO -> "All general weather conditions satisfy operational criteria"
            AssessmentStatus.DATA_UNAVAILABLE -> "Weather data incomplete"
        }

        return CategoryAssessment(
            category = category,
            status = worstStatus,
            ruleResults = rules,
            summary = summary
        )
    }
}

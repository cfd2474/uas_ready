package com.uasready.domain.engine.rules

import com.uasready.domain.engine.AssessmentContext
import com.uasready.domain.engine.CategoryRuleEvaluator
import com.uasready.domain.model.*

class SpaceWeatherRuleEvaluator : CategoryRuleEvaluator {
    override val category: AssessmentCategory = AssessmentCategory.SPACE_WEATHER

    override fun evaluate(context: AssessmentContext): CategoryAssessment {
        val spaceWeather = context.spaceWeather
        if (spaceWeather == null) {
            return CategoryAssessment(
                category = category,
                status = AssessmentStatus.DATA_UNAVAILABLE,
                ruleResults = listOf(
                    RuleResult(
                        ruleId = "SP-MISSING",
                        category = category,
                        status = AssessmentStatus.DATA_UNAVAILABLE,
                        title = "Space Weather Telemetry Unavailable",
                        inputValueFormatted = "No Telemetry",
                        thresholdFormatted = "NOAA SWPC Required",
                        explanation = "Unable to retrieve NOAA SWPC planetary K-index data. GNSS positioning safety margin cannot be verified."
                    )
                ),
                summary = "Missing NOAA space weather telemetry"
            )
        }

        val rules = mutableListOf<RuleResult>()
        val aircraft = context.aircraft
        val currentKp = spaceWeather.currentKpIndex
        val forecastKp = spaceWeather.forecastMaxKpIndex
        val maxKp = Math.max(currentKp, forecastKp)

        // 1. Planetary Kp Index & GNSS Reliability
        when {
            maxKp >= 6.0 -> rules.add(
                RuleResult(
                    ruleId = "SP-KP-001",
                    category = category,
                    status = AssessmentStatus.NO_GO,
                    title = "Severe Geomagnetic Activity (Kp >= 6)",
                    inputValueFormatted = String.format("Kp %.1f (%s)", maxKp, spaceWeather.geomagneticStormScale.name),
                    thresholdFormatted = "< Kp 5.0",
                    explanation = String.format("Planetary K-index of %.1f indicates major geomagnetic storm activity (%s). Severe risk of ionospheric scintillation, GNSS signal degradation, and magnetic compass disruption.", maxKp, spaceWeather.geomagneticStormScale.name)
                )
            )
            maxKp >= 4.0 -> rules.add(
                RuleResult(
                    ruleId = "SP-KP-002",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "Elevated Geomagnetic Activity (Kp 4-5)",
                    inputValueFormatted = String.format("Kp %.1f (%s)", maxKp, spaceWeather.geomagneticStormScale.name),
                    thresholdFormatted = "< Kp 4.0 Nominal",
                    explanation = String.format("Elevated geomagnetic activity (Kp %.1f). Expect possible satellite geometry (HDOP/VDOP) fluctuations and minor compass drift.", maxKp)
                )
            )
            else -> rules.add(
                RuleResult(
                    ruleId = "SP-KP-003",
                    category = category,
                    status = AssessmentStatus.GO,
                    title = "Space Weather & GNSS Positioning",
                    inputValueFormatted = String.format("Kp %.1f (Nominal)", maxKp),
                    thresholdFormatted = "< Kp 4.0",
                    explanation = "Quiet geomagnetic conditions. Optimal GNSS constellation geometry and compass stability."
                )
            )
        }

        // 2. Aircraft Specific Kp Tolerance
        val aircraftLimit = aircraft.limitations.maxKpIndexTolerance
        if (maxKp > aircraftLimit) {
            rules.add(
                RuleResult(
                    ruleId = "SP-AC-001",
                    category = category,
                    status = AssessmentStatus.NO_GO,
                    title = "Kp Exceeds Aircraft GNSS Envelope",
                    inputValueFormatted = String.format("Kp %.1f", maxKp),
                    thresholdFormatted = String.format("Max Kp %d (%s)", aircraftLimit, aircraft.displayName),
                    explanation = String.format("Current/forecast Kp (%.1f) exceeds %s's maximum operating threshold (Kp %d).", maxKp, aircraft.displayName, aircraftLimit),
                    applicableAircraft = aircraft.displayName
                )
            )
        }

        val worstStatus = rules.maxByOrNull { it.status.priority }?.status ?: AssessmentStatus.GO
        val summary = when (worstStatus) {
            AssessmentStatus.NO_GO -> "Severe space weather / geomagnetic storm hazard to GNSS"
            AssessmentStatus.CAUTION -> "Elevated Kp index may degrade GNSS accuracy"
            AssessmentStatus.GO -> "Normal GNSS and space weather conditions"
            AssessmentStatus.DATA_UNAVAILABLE -> "Space weather telemetry incomplete"
        }

        return CategoryAssessment(
            category = category,
            status = worstStatus,
            ruleResults = rules,
            summary = summary
        )
    }
}

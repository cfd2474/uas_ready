package com.uasready.domain.engine.rules

import com.uasready.domain.engine.AssessmentContext
import com.uasready.domain.engine.CategoryRuleEvaluator
import com.uasready.domain.model.*

class DataFreshnessRuleEvaluator : CategoryRuleEvaluator {
    override val category: AssessmentCategory = AssessmentCategory.DATA_FRESHNESS

    override fun evaluate(context: AssessmentContext): CategoryAssessment {
        val rules = mutableListOf<RuleResult>()

        // 1. Internet Connectivity Gate
        if (!context.hasInternetConnection) {
            rules.add(
                RuleResult(
                    ruleId = "DAT-NET-001",
                    category = category,
                    status = AssessmentStatus.DATA_UNAVAILABLE,
                    title = "No Internet Connection",
                    inputValueFormatted = "Offline",
                    thresholdFormatted = "Active Connection Mandatory",
                    explanation = "UASReady requires an active data connection to certify flight safety. Offline cached evaluations are strictly disabled for aviation safety."
                )
            )
            return CategoryAssessment(
                category = category,
                status = AssessmentStatus.DATA_UNAVAILABLE,
                ruleResults = rules,
                summary = "Active internet connection required"
            )
        }

        // 2. Weather Freshness
        val weather = context.weather
        if (weather != null) {
            val ageMin = (System.currentTimeMillis() - weather.timestampEpochMs) / (60 * 1000)
            if (weather.isStale || ageMin > 60) {
                rules.add(
                    RuleResult(
                        ruleId = "DAT-WX-001",
                        category = category,
                        status = AssessmentStatus.CAUTION,
                        title = "Stale Weather Telemetry",
                        inputValueFormatted = String.format("%d min old", ageMin),
                        thresholdFormatted = "< 60 min",
                        explanation = String.format("Weather observation data is %d minutes old. Local conditions may have changed.", ageMin)
                    )
                )
            } else {
                rules.add(
                    RuleResult(
                        ruleId = "DAT-WX-002",
                        category = category,
                        status = AssessmentStatus.GO,
                        title = "Weather Data Freshness",
                        inputValueFormatted = String.format("%d min ago (%s)", ageMin, weather.sourceName),
                        thresholdFormatted = "< 60 min",
                        explanation = "Weather observation telemetry is fresh and authoritative."
                    )
                )
            }
        } else {
            rules.add(
                RuleResult(
                    ruleId = "DAT-WX-003",
                    category = category,
                    status = AssessmentStatus.DATA_UNAVAILABLE,
                    title = "Missing Weather Telemetry",
                    inputValueFormatted = "Unavailable",
                    thresholdFormatted = "Required",
                    explanation = "Live weather telemetry is missing."
                )
            )
        }

        // 3. Space Weather Freshness
        val spaceWeather = context.spaceWeather
        if (spaceWeather != null) {
            val ageMin = (System.currentTimeMillis() - spaceWeather.timestampEpochMs) / (60 * 1000)
            if (spaceWeather.isStale || ageMin > 180) {
                rules.add(
                    RuleResult(
                        ruleId = "DAT-SP-001",
                        category = category,
                        status = AssessmentStatus.CAUTION,
                        title = "Stale Space Weather Data",
                        inputValueFormatted = String.format("%d min old", ageMin),
                        thresholdFormatted = "< 180 min",
                        explanation = "Planetary K-index observation has not updated recently."
                    )
                )
            } else {
                rules.add(
                    RuleResult(
                        ruleId = "DAT-SP-002",
                        category = category,
                        status = AssessmentStatus.GO,
                        title = "Space Weather Freshness",
                        inputValueFormatted = String.format("%d min ago", ageMin),
                        thresholdFormatted = "< 180 min",
                        explanation = "NOAA SWPC planetary Kp observation telemetry is fresh."
                    )
                )
            }
        } else {
            rules.add(
                RuleResult(
                    ruleId = "DAT-SP-003",
                    category = category,
                    status = AssessmentStatus.DATA_UNAVAILABLE,
                    title = "Missing Space Weather Telemetry",
                    inputValueFormatted = "Unavailable",
                    thresholdFormatted = "Required",
                    explanation = "Live space weather telemetry is missing."
                )
            )
        }

        val worstStatus = rules.maxByOrNull { it.status.priority }?.status ?: AssessmentStatus.GO
        val summary = when (worstStatus) {
            AssessmentStatus.NO_GO -> "Critical telemetry sources failed or stale"
            AssessmentStatus.CAUTION -> "One or more telemetry sources are older than recommended"
            AssessmentStatus.GO -> "All telemetry feeds are live and fresh"
            AssessmentStatus.DATA_UNAVAILABLE -> "Critical telemetry missing or offline"
        }

        return CategoryAssessment(
            category = category,
            status = worstStatus,
            ruleResults = rules,
            summary = summary
        )
    }
}

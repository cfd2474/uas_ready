package com.taksolutions.uasready.domain.engine.rules

import com.taksolutions.uasready.domain.engine.AssessmentContext
import com.taksolutions.uasready.domain.engine.CategoryRuleEvaluator
import com.taksolutions.uasready.domain.model.*

class AircraftRuleEvaluator : CategoryRuleEvaluator {
    override val category: AssessmentCategory = AssessmentCategory.AIRCRAFT_LIMITS

    override fun evaluate(context: AssessmentContext): CategoryAssessment {
        val weather = context.weather
        val aircraft = context.aircraft
        val limits = aircraft.limitations

        if (weather == null) {
            return CategoryAssessment(
                category = category,
                status = AssessmentStatus.DATA_UNAVAILABLE,
                ruleResults = emptyList(),
                summary = "Cannot evaluate aircraft limitations without environmental telemetry"
            )
        }

        val rules = mutableListOf<RuleResult>()

        // 1. Sustained Wind vs Aircraft Limit
        val windSpeed = weather.windSpeedMph
        val maxSustained = limits.maxSustainedWindSpeedMph
        when {
            windSpeed > maxSustained -> rules.add(
                RuleResult(
                    ruleId = "AC-WIND-001",
                    category = category,
                    status = AssessmentStatus.NO_GO,
                    title = "Sustained Wind Speed Exceeded",
                    inputValueFormatted = String.format("%.1f MPH", windSpeed),
                    thresholdFormatted = String.format("Max %.1f MPH (%s)", maxSustained, aircraft.displayName),
                    explanation = String.format("Current sustained wind speed (%.1f MPH) exceeds %s's maximum operating limit of %.1f MPH.", windSpeed, aircraft.displayName, maxSustained),
                    applicableAircraft = aircraft.displayName
                )
            )
            windSpeed >= maxSustained - 4.0 -> rules.add(
                RuleResult(
                    ruleId = "AC-WIND-002",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "Sustained Wind Speed Near Limit",
                    inputValueFormatted = String.format("%.1f MPH", windSpeed),
                    thresholdFormatted = String.format("Max %.1f MPH (%s)", maxSustained, aircraft.displayName),
                    explanation = String.format("Current sustained wind (%.1f MPH) is approaching %s's operational maximum (%.1f MPH). Expect rapid battery consumption and control margin reduction.", windSpeed, aircraft.displayName, maxSustained),
                    applicableAircraft = aircraft.displayName
                )
            )
            else -> rules.add(
                RuleResult(
                    ruleId = "AC-WIND-003",
                    category = category,
                    status = AssessmentStatus.GO,
                    title = "Sustained Wind Speed",
                    inputValueFormatted = String.format("%.1f MPH", windSpeed),
                    thresholdFormatted = String.format("Max %.1f MPH", maxSustained),
                    explanation = String.format("Sustained wind speed (%.1f MPH) is within aircraft operating envelope.", windSpeed),
                    applicableAircraft = aircraft.displayName
                )
            )
        }

        // 2. Wind Gusts vs Aircraft Limit
        val windGust = weather.windGustMph
        val maxGust = limits.maxGustSpeedMph
        when {
            windGust > maxGust -> rules.add(
                RuleResult(
                    ruleId = "AC-GUST-001",
                    category = category,
                    status = AssessmentStatus.NO_GO,
                    title = "Wind Gusts Exceeded",
                    inputValueFormatted = String.format("%.1f MPH", windGust),
                    thresholdFormatted = String.format("Max %.1f MPH (%s)", maxGust, aircraft.displayName),
                    explanation = String.format("Observed wind gusts of %.1f MPH exceed %s's maximum gust limit of %.1f MPH. Severe risk of loss of control or flyaway.", windGust, aircraft.displayName, maxGust),
                    applicableAircraft = aircraft.displayName
                )
            )
            windGust >= maxGust - 5.0 -> rules.add(
                RuleResult(
                    ruleId = "AC-GUST-002",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "Wind Gusts Approaching Limit",
                    inputValueFormatted = String.format("%.1f MPH", windGust),
                    thresholdFormatted = String.format("Max %.1f MPH (%s)", maxGust, aircraft.displayName),
                    explanation = String.format("Wind gusts (%.1f MPH) are approaching %s's maximum operating wind speed (%.1f MPH).", windGust, aircraft.displayName, maxGust),
                    applicableAircraft = aircraft.displayName
                )
            )
            else -> rules.add(
                RuleResult(
                    ruleId = "AC-GUST-003",
                    category = category,
                    status = AssessmentStatus.GO,
                    title = "Wind Gusts",
                    inputValueFormatted = String.format("%.1f MPH", windGust),
                    thresholdFormatted = String.format("Max %.1f MPH", maxGust),
                    explanation = String.format("Wind gusts (%.1f MPH) are well within aircraft limits.", windGust),
                    applicableAircraft = aircraft.displayName
                )
            )
        }

        // 3. Operating Temperature Range vs Aircraft Limits
        val temp = weather.temperatureF
        val minTemp = limits.minOperatingTempF
        val maxTemp = limits.maxOperatingTempF
        when {
            temp < minTemp -> rules.add(
                RuleResult(
                    ruleId = "AC-TEMP-001",
                    category = category,
                    status = AssessmentStatus.NO_GO,
                    title = "Temperature Below Minimum Operating Limit",
                    inputValueFormatted = String.format("%.1f°F", temp),
                    thresholdFormatted = String.format("Min %.1f°F (%s)", minTemp, aircraft.displayName),
                    explanation = String.format("Current temperature (%.1f°F) is below %s's certified minimum operating temperature (%.1f°F). High risk of LiPo battery voltage collapse and structural embrittlement.", temp, aircraft.displayName, minTemp),
                    applicableAircraft = aircraft.displayName
                )
            )
            temp > maxTemp -> rules.add(
                RuleResult(
                    ruleId = "AC-TEMP-002",
                    category = category,
                    status = AssessmentStatus.NO_GO,
                    title = "Temperature Above Maximum Operating Limit",
                    inputValueFormatted = String.format("%.1f°F", temp),
                    thresholdFormatted = String.format("Max %.1f°F (%s)", maxTemp, aircraft.displayName),
                    explanation = String.format("Current temperature (%.1f°F) exceeds %s's maximum operating temperature (%.1f°F). Severe risk of ESC thermal shutdown and battery overheating.", temp, aircraft.displayName, maxTemp),
                    applicableAircraft = aircraft.displayName
                )
            )
            temp <= minTemp + 5.0 || temp >= maxTemp - 5.0 -> rules.add(
                RuleResult(
                    ruleId = "AC-TEMP-003",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "Temperature Near Operational Boundary",
                    inputValueFormatted = String.format("%.1f°F", temp),
                    thresholdFormatted = String.format("%.1f°F to %.1f°F", minTemp, maxTemp),
                    explanation = String.format("Current temperature (%.1f°F) is near %s's operating limits (%.1f°F to %.1f°F). Monitor battery temperatures continuously.", temp, aircraft.displayName, minTemp, maxTemp),
                    applicableAircraft = aircraft.displayName
                )
            )
            else -> rules.add(
                RuleResult(
                    ruleId = "AC-TEMP-004",
                    category = category,
                    status = AssessmentStatus.GO,
                    title = "Operating Temperature",
                    inputValueFormatted = String.format("%.1f°F", temp),
                    thresholdFormatted = String.format("%.1f°F to %.1f°F", minTemp, maxTemp),
                    explanation = String.format("Ambient temperature (%.1f°F) is within nominal operating range.", temp),
                    applicableAircraft = aircraft.displayName
                )
            )
        }

        // 4. Precipitation Limitations
        val isPrecip = weather.precipitationProbabilityPercent > 30 || weather.precipitationRateInchesPerHour > 0.01 || weather.precipitationType != PrecipitationType.NONE
        if (isPrecip) {
            if (!limits.precipitationAllowed) {
                rules.add(
                    RuleResult(
                        ruleId = "AC-PRECIP-001",
                        category = category,
                        status = AssessmentStatus.NO_GO,
                        title = "Precipitation Prohibited for Aircraft",
                        inputValueFormatted = String.format("%s (%.2f in/hr)", weather.precipitationType.name, weather.precipitationRateInchesPerHour),
                        thresholdFormatted = "Zero Precipitation (IP rating: None)",
                        explanation = String.format("%s has no IP water-ingress protection. Flight in %s is strictly prohibited by manufacturer.", aircraft.displayName, weather.precipitationType.name.lowercase()),
                        applicableAircraft = aircraft.displayName
                    )
                )
            } else {
                rules.add(
                    RuleResult(
                        ruleId = "AC-PRECIP-002",
                        category = category,
                        status = AssessmentStatus.CAUTION,
                        title = "Precipitation Operations Allowed",
                        inputValueFormatted = String.format("%s (%.2f in/hr)", weather.precipitationType.name, weather.precipitationRateInchesPerHour),
                        thresholdFormatted = String.format("IP Rating: %s", limits.ipRating),
                        explanation = String.format("%s is rated %s for wet weather operations. Ensure payload optical glass is kept clear.", aircraft.displayName, limits.ipRating),
                        applicableAircraft = aircraft.displayName
                    )
                )
            }
        } else {
            rules.add(
                RuleResult(
                    ruleId = "AC-PRECIP-003",
                    category = category,
                    status = AssessmentStatus.GO,
                    title = "Precipitation Protection",
                    inputValueFormatted = "Dry / No Precipitation",
                    thresholdFormatted = "Nominal",
                    explanation = "No precipitation detected.",
                    applicableAircraft = aircraft.displayName
                )
            )
        }

        // 5. Forecast Wind degradation across 120-minute flight window
        val forecast = context.forecast
        if (forecast != null && forecast.intervals.isNotEmpty()) {
            val windowSampling = context.flightWindow.getSamplingIntervals()
            for (sampleTime in windowSampling) {
                val interval = forecast.intervals.minByOrNull { Math.abs(it.timestampEpochMs - sampleTime) }
                if (interval != null) {
                    val offsetMin = (sampleTime - context.flightWindow.startEpochMs) / (60 * 1000)
                    if (interval.windGustMph > maxGust) {
                        if (offsetMin < 60) {
                            rules.add(
                                RuleResult(
                                    ruleId = "AC-FCST-GUST-001",
                                    category = category,
                                    status = AssessmentStatus.NO_GO,
                                    title = "Forecast Gusts Exceed Limit (< 60m)",
                                    inputValueFormatted = String.format("%.1f MPH at +%dm", interval.windGustMph, offsetMin),
                                    thresholdFormatted = String.format("Max %.1f MPH (%s)", maxGust, aircraft.displayName),
                                    explanation = String.format("Forecast wind gusts of %.1f MPH exceed %s's limit (%.1f MPH) at +%d min into the 0–60 min flight window. Immediate flight prohibited.", interval.windGustMph, aircraft.displayName, maxGust, offsetMin),
                                    applicableAircraft = aircraft.displayName,
                                    isForecastDerived = true,
                                    forecastTimeOffsetMinutes = offsetMin
                                )
                            )
                        } else {
                            rules.add(
                                RuleResult(
                                    ruleId = "AC-FCST-GUST-002",
                                    category = category,
                                    status = AssessmentStatus.CAUTION,
                                    title = "Forecast Gusts Exceed Limit in 60-120m Window",
                                    inputValueFormatted = String.format("%.1f MPH at +%dm", interval.windGustMph, offsetMin),
                                    thresholdFormatted = String.format("Max %.1f MPH (%s)", maxGust, aircraft.displayName),
                                    explanation = String.format("Forecast gusts increase to %.1f MPH at +%d min (60–120 min window). Short flight under 60 minutes permitted; ensure return to base before deterioration.", interval.windGustMph, offsetMin, aircraft.displayName),
                                    applicableAircraft = aircraft.displayName,
                                    isForecastDerived = true,
                                    forecastTimeOffsetMinutes = offsetMin
                                )
                            )
                        }
                    } else if (interval.windGustMph >= maxGust - 5.0) {
                        rules.add(
                            RuleResult(
                                ruleId = "AC-FCST-GUST-003",
                                category = category,
                                status = AssessmentStatus.CAUTION,
                                title = "Forecast Wind Gusts Approaching Limit",
                                inputValueFormatted = String.format("%.1f MPH at +%dm", interval.windGustMph, offsetMin),
                                thresholdFormatted = String.format("Max %.1f MPH (%s)", maxGust, aircraft.displayName),
                                explanation = String.format("Forecast gusts reach %.1f MPH at +%d min, approaching %s's limit.", interval.windGustMph, offsetMin, aircraft.displayName),
                                applicableAircraft = aircraft.displayName,
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
            AssessmentStatus.NO_GO -> "Environmental conditions exceed aircraft operational envelope"
            AssessmentStatus.CAUTION -> "Operating near aircraft environmental limitations"
            AssessmentStatus.GO -> "All parameters are within aircraft operational limitations"
            AssessmentStatus.DATA_UNAVAILABLE -> "Missing aircraft limit comparison data"
        }

        return CategoryAssessment(
            category = category,
            status = worstStatus,
            ruleResults = rules,
            summary = summary
        )
    }
}

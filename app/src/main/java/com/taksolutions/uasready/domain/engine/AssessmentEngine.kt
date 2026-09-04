package com.taksolutions.uasready.domain.engine

import com.taksolutions.uasready.domain.engine.rules.*
import com.taksolutions.uasready.domain.model.*

/**
 * Deterministic, transparent rules engine orchestrator for UASReady.
 * Adheres to SOLID Single Responsibility and Open/Closed principles.
 */
class AssessmentEngine(
    private val evaluators: List<CategoryRuleEvaluator> = listOf(
        WeatherRuleEvaluator(),
        AircraftRuleEvaluator(),
        AirspaceRuleEvaluator(),
        PilotAuthorityRuleEvaluator(),
        SpaceWeatherRuleEvaluator(),
        DaylightRuleEvaluator(),
        DataFreshnessRuleEvaluator()
    )
) {

    /**
     * Executes the transparent rules engine on the given flight assessment context.
     */
    fun assess(context: AssessmentContext): AssessmentResult {
        val categoryAssessments = evaluators.map { evaluator ->
            evaluator.evaluate(context)
        }

        // Deterministic Priority Aggregation:
        // DATA_UNAVAILABLE > NO_GO > CAUTION > GO
        val overallStatus = categoryAssessments
            .map { it.status }
            .maxByOrNull { it.priority } ?: AssessmentStatus.GO

        val primaryHeadline = when (overallStatus) {
            AssessmentStatus.DATA_UNAVAILABLE -> "DATA UNAVAILABLE — TELEMETRY INCOMPLETE"
            AssessmentStatus.NO_GO -> "NO-GO — SAFETY CRITERIA VIOLATED"
            AssessmentStatus.CAUTION -> "CAUTION — ADVISORY CONDITIONS DETECTED"
            AssessmentStatus.GO -> "GO — ALL CRITERIA SATISFIED"
        }

        // Collect explainable reasons for any non-GO conditions, or confirm all criteria met
        val primaryReasons = mutableListOf<String>()
        when (overallStatus) {
            AssessmentStatus.DATA_UNAVAILABLE -> {
                categoryAssessments
                    .filter { it.status == AssessmentStatus.DATA_UNAVAILABLE }
                    .forEach { cat ->
                        cat.ruleResults.filter { it.status == AssessmentStatus.DATA_UNAVAILABLE }
                            .forEach { primaryReasons.add(it.explanation) }
                    }
            }
            AssessmentStatus.NO_GO -> {
                categoryAssessments
                    .filter { it.status == AssessmentStatus.NO_GO }
                    .forEach { cat ->
                        cat.ruleResults.filter { it.status == AssessmentStatus.NO_GO }
                            .forEach { primaryReasons.add(it.explanation) }
                    }
            }
            AssessmentStatus.CAUTION -> {
                categoryAssessments
                    .filter { it.status == AssessmentStatus.CAUTION }
                    .forEach { cat ->
                        cat.ruleResults.filter { it.status == AssessmentStatus.CAUTION }
                            .forEach { primaryReasons.add(it.explanation) }
                    }
            }
            AssessmentStatus.GO -> {
                primaryReasons.add("Weather, airspace, space weather, aircraft limits, and pilot qualifications satisfy all operational safety standards.")
            }
        }

        return AssessmentResult(
            overallStatus = overallStatus,
            primaryHeadline = primaryHeadline,
            primaryReasons = primaryReasons,
            categoryAssessments = categoryAssessments,
            evaluatedAtEpochMs = System.currentTimeMillis(),
            flightWindow = context.flightWindow,
            aircraft = context.aircraft,
            pilot = context.pilot,
            location = context.location
        )
    }
}

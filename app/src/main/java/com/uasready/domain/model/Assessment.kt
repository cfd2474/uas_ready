package com.uasready.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AssessmentStatus {
    GO,
    CAUTION,
    NO_GO,
    DATA_UNAVAILABLE;

    val priority: Int
        get() = when (this) {
            DATA_UNAVAILABLE -> 4 // Critical: missing connectivity/data must block false GO
            NO_GO -> 3
            CAUTION -> 2
            GO -> 1
        }
}

@Serializable
enum class AssessmentCategory(val displayName: String) {
    WEATHER("Weather Conditions"),
    AIRCRAFT_LIMITS("Aircraft Operational Envelope"),
    SPACE_WEATHER("Space Weather & GNSS"),
    AIRSPACE("Airspace & FAA Restrictions"),
    PILOT_QUALIFICATIONS("Pilot Operating Authority"),
    DAYLIGHT("Solar & Daylight Timing"),
    DATA_FRESHNESS("Data Freshness & Telemetry")
}

@Serializable
data class RuleResult(
    val ruleId: String,
    val category: AssessmentCategory,
    val status: AssessmentStatus,
    val title: String,
    val inputValueFormatted: String,
    val thresholdFormatted: String,
    val explanation: String,
    val applicableAircraft: String? = null,
    val applicableAuthority: String? = null,
    val isForecastDerived: Boolean = false,
    val forecastTimeOffsetMinutes: Long? = null
)

@Serializable
data class CategoryAssessment(
    val category: AssessmentCategory,
    val status: AssessmentStatus,
    val ruleResults: List<RuleResult>,
    val summary: String
)

@Serializable
data class AssessmentResult(
    val overallStatus: AssessmentStatus,
    val primaryHeadline: String,
    val primaryReasons: List<String>,
    val categoryAssessments: List<CategoryAssessment>,
    val evaluatedAtEpochMs: Long,
    val flightWindow: FlightWindow,
    val aircraft: Aircraft,
    val pilot: Pilot,
    val location: LocationInfo
) {
    val allRuleResults: List<RuleResult>
        get() = categoryAssessments.flatMap { it.ruleResults }

    val noGoRules: List<RuleResult>
        get() = allRuleResults.filter { it.status == AssessmentStatus.NO_GO }

    val cautionRules: List<RuleResult>
        get() = allRuleResults.filter { it.status == AssessmentStatus.CAUTION }

    val goRules: List<RuleResult>
        get() = allRuleResults.filter { it.status == AssessmentStatus.GO }
}

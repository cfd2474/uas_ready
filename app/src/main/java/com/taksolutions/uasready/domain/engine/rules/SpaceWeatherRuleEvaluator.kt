package com.taksolutions.uasready.domain.engine.rules

import com.taksolutions.uasready.domain.engine.AssessmentContext
import com.taksolutions.uasready.domain.engine.CategoryRuleEvaluator
import com.taksolutions.uasready.domain.model.*

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

        // 3. GNSS Satellite Navigation Solution & HDOP Safety Assessment
        val gnss = context.gnss ?: GnssEstimation.estimate(
            latitude = context.location.latitude,
            elevationFt = context.location.elevationFt,
            kpIndex = maxKp,
            terrainProfile = context.terrainProfile
        )

        val terrainNote = if (gnss.terrainOccludedSatellitesCount > 0 && gnss.terrainProfile != null) {
            " (${gnss.terrainOccludedSatellitesCount} sats occluded by ${gnss.terrainProfile.terrainClassification}, ${gnss.terrainProfile.maxObstructionDeg}° max ridge mask)"
        } else {
            ""
        }

        // Rule: Satellites in navigation solution
        when {
            gnss.lockedSatellitesCount >= 12 -> rules.add(
                RuleResult(
                    ruleId = "SP-GNSS-SATS",
                    category = category,
                    status = AssessmentStatus.GO,
                    title = "GNSS Satellites Visible",
                    inputValueFormatted = "${gnss.lockedSatellitesCount} Sats Visible",
                    thresholdFormatted = ">= 12 Sats (3D Fix)",
                    explanation = "${gnss.lockedSatellitesCount} multi-GNSS satellites visible in navigation solution$terrainNote. 3D fix verified; stable home point confirmed."
                )
            )
            gnss.lockedSatellitesCount in 8..11 -> rules.add(
                RuleResult(
                    ruleId = "SP-GNSS-SATS",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "Marginal GNSS Satellites Visible (8-11 Sats)",
                    inputValueFormatted = "${gnss.lockedSatellitesCount} Sats Visible",
                    thresholdFormatted = "12+ Sats for Full Nominal",
                    explanation = "${gnss.lockedSatellitesCount} satellites visible. Marginal constellation geometry; verify home point manually and avoid GNSS-dependent precision automated mapping."
                )
            )
            else -> rules.add(
                RuleResult(
                    ruleId = "SP-GNSS-SATS",
                    category = category,
                    status = AssessmentStatus.NO_GO,
                    title = "Insufficient GNSS Satellites Visible (<= 7 Sats)",
                    inputValueFormatted = "${gnss.lockedSatellitesCount} Sats Visible",
                    thresholdFormatted = "Min 8 Sats Required",
                    explanation = "Only ${gnss.lockedSatellitesCount} satellites visible in navigation solution (<= 7)$terrainNote. Severe risk of GPS loss-of-lock, ATTI mode fallback, or uncommanded fly-away."
                )
            )
        }

        // Rule: Horizontal Dilution of Precision (HDOP)
        when {
            gnss.estimatedHdop <= 1.5 -> rules.add(
                RuleResult(
                    ruleId = "SP-GNSS-HDOP",
                    category = category,
                    status = AssessmentStatus.GO,
                    title = "Horizontal Dilution of Precision (HDOP)",
                    inputValueFormatted = "HDOP ${gnss.estimatedHdop}",
                    thresholdFormatted = "<= 1.5",
                    explanation = "Optimal satellite geometry with HDOP ${gnss.estimatedHdop} (<= 1.5). Centimeter-to-sub-meter horizontal positioning precision."
                )
            )
            gnss.estimatedHdop <= 2.5 -> rules.add(
                RuleResult(
                    ruleId = "SP-GNSS-HDOP",
                    category = category,
                    status = AssessmentStatus.CAUTION,
                    title = "Degraded HDOP Precision (1.5 - 2.5)",
                    inputValueFormatted = "HDOP ${gnss.estimatedHdop}",
                    thresholdFormatted = "<= 1.5 Nominal",
                    explanation = "HDOP is elevated at ${gnss.estimatedHdop}. Positional accuracy reduced; maintain safe separation from structures and trees."
                )
            )
            else -> rules.add(
                RuleResult(
                    ruleId = "SP-GNSS-HDOP",
                    category = category,
                    status = AssessmentStatus.NO_GO,
                    title = "Excessive HDOP Error (> 2.5)",
                    inputValueFormatted = "HDOP ${gnss.estimatedHdop}",
                    thresholdFormatted = "<= 2.5 Max Safe",
                    explanation = "HDOP of ${gnss.estimatedHdop} exceeds safe navigation criteria (> 2.5). Satellite triangulation geometry is severely compromised."
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

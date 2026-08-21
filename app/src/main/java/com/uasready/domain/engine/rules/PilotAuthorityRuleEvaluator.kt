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

        val now = System.currentTimeMillis()

        when (pilot.activeAuthority) {
            PilotAuthorityType.PART_107 -> {
                val profile = pilot.part107Profile

                // 1. Part 107 Currency & Recurrent Training (14 CFR § 107.65)
                if (profile.isRecurrentTrainingExpired(now) || !profile.isPart107Current) {
                    rules.add(
                        RuleResult(
                            ruleId = "PLT-107-CUR-001",
                            category = category,
                            status = AssessmentStatus.NO_GO,
                            title = "Part 107 Recurrent Training Expired",
                            inputValueFormatted = "Expired",
                            thresholdFormatted = "Valid (Within 24 calendar months)",
                            explanation = "Pilot recurrent aeronautical training is expired under 14 CFR § 107.65. Pilot is legally prohibited from acting as remote PIC.",
                            applicableAuthority = "Part 107"
                        )
                    )
                } else if (profile.recurrentTrainingValidUntilEpochMs - now < 30L * 24 * 60 * 60 * 1000) {
                    rules.add(
                        RuleResult(
                            ruleId = "PLT-107-CUR-002",
                            category = category,
                            status = AssessmentStatus.CAUTION,
                            title = "Part 107 Currency Approaching Expiration",
                            inputValueFormatted = "Expiring soon",
                            thresholdFormatted = "> 30 Days Remaining",
                            explanation = "Part 107 recurrent training currency expires in less than 30 days. Complete FAA online recurrent module.",
                            applicableAuthority = "Part 107"
                        )
                    )
                } else {
                    rules.add(
                        RuleResult(
                            ruleId = "PLT-107-CUR-003",
                            category = category,
                            status = AssessmentStatus.GO,
                            title = "Part 107 Pilot Currency",
                            inputValueFormatted = "Current",
                            thresholdFormatted = "Valid 14 CFR § 107.65",
                            explanation = "Remote Pilot Certificate and recurrent aeronautical training are fully current.",
                            applicableAuthority = "Part 107"
                        )
                    )
                }

                // 2. Part 107 Night Operations (14 CFR § 107.29)
                val isNightFlight = sunData != null && (
                        flightWindow.startEpochMs < sunData.sunriseEpochMs ||
                                flightWindow.endEpochMs > sunData.sunsetEpochMs
                        )

                if (isNightFlight) {
                    if (!profile.nightTrainingCompleted) {
                        rules.add(
                            RuleResult(
                                ruleId = "PLT-107-NGT-001",
                                category = category,
                                status = AssessmentStatus.NO_GO,
                                title = "Part 107 Night Training Incomplete",
                                inputValueFormatted = "No Night Training",
                                thresholdFormatted = "Updated 107.29 Night Training Required",
                                explanation = "Flight window extends into night / civil twilight, but pilot has not completed updated initial/recurrent night training curriculum under 14 CFR § 107.29.",
                                applicableAuthority = "Part 107"
                            )
                        )
                    } else if (!aircraft.limitations.nightOperationCapable) {
                        rules.add(
                            RuleResult(
                                ruleId = "PLT-107-NGT-002",
                                category = category,
                                status = AssessmentStatus.NO_GO,
                                title = "Anti-Collision Lighting Required for Night",
                                inputValueFormatted = "No Strobe / Lighting",
                                thresholdFormatted = "Anti-collision strobe visible for 3 SM",
                                explanation = "Night flight operations under § 107.29 require anti-collision lighting with flash rate sufficient to avoid collision, visible for at least 3 statute miles.",
                                applicableAircraft = aircraft.displayName,
                                applicableAuthority = "Part 107"
                            )
                        )
                    } else {
                        rules.add(
                            RuleResult(
                                ruleId = "PLT-107-NGT-003",
                                category = category,
                                status = AssessmentStatus.GO,
                                title = "Part 107 Night Flight Requirements Met",
                                inputValueFormatted = "Night Endorsed + Strobe Equipped",
                                thresholdFormatted = "14 CFR § 107.29 Compliant",
                                explanation = "Pilot has completed night training and aircraft is equipped with approved anti-collision lighting.",
                                applicableAuthority = "Part 107"
                            )
                        )
                    }
                }
            }

            PilotAuthorityType.COA_COW -> {
                val coa = pilot.coaCowProfile

                // 1. COA Authorization Currency
                if (coa.isCoaExpired(now) || !coa.agencyQualificationCurrent) {
                    rules.add(
                        RuleResult(
                            ruleId = "PLT-COA-CUR-001",
                            category = category,
                            status = AssessmentStatus.NO_GO,
                            title = "Public Agency COA Expired",
                            inputValueFormatted = "Expired / Inactive",
                            thresholdFormatted = "Active Agency COA",
                            explanation = String.format("Public Agency COA authorization (%s) is expired or pilot agency currency has lapsed.", coa.coaNumber),
                            applicableAuthority = "COA/COW"
                        )
                    )
                } else {
                    rules.add(
                        RuleResult(
                            ruleId = "PLT-COA-CUR-002",
                            category = category,
                            status = AssessmentStatus.GO,
                            title = "Agency COA Authorization",
                            inputValueFormatted = coa.coaNumber,
                            thresholdFormatted = "Active & Qualified",
                            explanation = String.format("Pilot is authorized under active agency Certificate of Authorization (%s).", coa.coaNumber),
                            applicableAuthority = "COA/COW"
                        )
                    )
                }

                // 2. COA Altitude Limit
                if (context.plannedAltitudeAglFt > coa.maxAltitudeAuthorizedFt) {
                    rules.add(
                        RuleResult(
                            ruleId = "PLT-COA-ALT-001",
                            category = category,
                            status = AssessmentStatus.NO_GO,
                            title = "Planned Altitude Exceeds COA Authorization",
                            inputValueFormatted = String.format("%.0f ft AGL", context.plannedAltitudeAglFt),
                            thresholdFormatted = String.format("Max %.0f ft AGL (%s)", coa.maxAltitudeAuthorizedFt, coa.coaNumber),
                            explanation = String.format("Planned altitude of %.0f ft AGL exceeds the maximum altitude authorized under agency COA (%.0f ft AGL).", context.plannedAltitudeAglFt, coa.maxAltitudeAuthorizedFt),
                            applicableAuthority = "COA/COW"
                        )
                    )
                }

                // 3. COA Night Operations
                val isNightFlight = sunData != null && (
                        flightWindow.startEpochMs < sunData.sunriseEpochMs ||
                                flightWindow.endEpochMs > sunData.sunsetEpochMs
                        )

                if (isNightFlight) {
                    if (!coa.nightFlightAuthorized) {
                        rules.add(
                            RuleResult(
                                ruleId = "PLT-COA-NGT-001",
                                category = category,
                                status = AssessmentStatus.NO_GO,
                                title = "Night Operations Not Authorized Under COA",
                                inputValueFormatted = "Night Not Permitted",
                                thresholdFormatted = "COA Night Authorization Required",
                                explanation = String.format("Agency COA (%s) does not authorize nighttime sUAS operations.", coa.coaNumber),
                                applicableAuthority = "COA/COW"
                            )
                        )
                    } else {
                        rules.add(
                            RuleResult(
                                ruleId = "PLT-COA-NGT-002",
                                category = category,
                                status = AssessmentStatus.GO,
                                title = "COA Night Authorization",
                                inputValueFormatted = "Authorized",
                                thresholdFormatted = "COA Night Approved",
                                explanation = "Nighttime operations authorized under public aircraft agency COA terms.",
                                applicableAuthority = "COA/COW"
                            )
                        )
                    }
                }
            }
        }

        val worstStatus = rules.maxByOrNull { it.status.priority }?.status ?: AssessmentStatus.GO
        val summary = when (worstStatus) {
            AssessmentStatus.NO_GO -> "Pilot regulatory qualification or operating authority criteria not satisfied"
            AssessmentStatus.CAUTION -> "Pilot currency approaching expiration"
            AssessmentStatus.GO -> String.format("Pilot satisfies all %s operating authority criteria", pilot.activeAuthority.name)
            AssessmentStatus.DATA_UNAVAILABLE -> "Pilot qualification data incomplete"
        }

        return CategoryAssessment(
            category = category,
            status = worstStatus,
            ruleResults = rules,
            summary = summary
        )
    }
}

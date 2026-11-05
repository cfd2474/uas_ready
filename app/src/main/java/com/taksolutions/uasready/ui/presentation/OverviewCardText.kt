package com.taksolutions.uasready.ui.presentation

import com.taksolutions.uasready.domain.model.AssessmentResult
import com.taksolutions.uasready.domain.model.GnssEstimation
import com.taksolutions.uasready.domain.model.RuleResult
import com.taksolutions.uasready.domain.model.SpaceWeather
import com.taksolutions.uasready.domain.model.WeatherObservation

/** The two text lines of a Home overview card. */
data class CardText(val primary: String, val secondary: String)

/**
 * Builds the Home overview card text from what was actually received.
 *
 * Rule: a card never shows a plausible-looking value that did not come from a live source.
 * When the source is missing the card says so. (Earlier code fell back to invented values such as
 * "75°F" or "Vis 10 SM", which looked like real data while offline.)
 */
object OverviewCardText {

    const val NO_LIVE_DATA = "No live data"
    private const val PLACEHOLDER = "—"

    private fun AssessmentResult?.rule(predicate: (RuleResult) -> Boolean): RuleResult? =
        this?.allRuleResults?.firstOrNull(predicate)

    fun weather(assessment: AssessmentResult?, observation: WeatherObservation?): CardText {
        if (observation == null) return CardText(NO_LIVE_DATA, "Weather service unreachable")
        val temp = assessment.rule { it.ruleId.startsWith("WX-TEMP") || it.ruleId.startsWith("AC-TEMP") }
        val gust = assessment.rule { it.ruleId.startsWith("WX-GUST") || it.ruleId.startsWith("AC-GUST") }
        val vis = assessment.rule { it.ruleId.startsWith("WX-VIS") }
        return CardText(
            primary = "${temp?.inputValueFormatted ?: PLACEHOLDER} • Wind",
            secondary = "${gust?.inputValueFormatted ?: PLACEHOLDER} • ${vis?.inputValueFormatted?.let { "Vis $it" } ?: PLACEHOLDER}"
        )
    }

    fun daylight(assessment: AssessmentResult?): CardText {
        val sun = assessment.rule { it.ruleId.startsWith("SUN-") }
            ?: return CardText(PLACEHOLDER, "Not yet evaluated")
        return CardText(sun.inputValueFormatted, sun.explanation)
    }

    fun gnss(assessment: AssessmentResult?, gnss: GnssEstimation?): CardText {
        if (gnss != null) {
            return CardText("~${gnss.lockedSatellitesCount} Sats Visible", "HDOP ${gnss.estimatedHdop} • 3D Fix")
        }
        // The estimate needs live space weather (Kp); without it there is nothing honest to show.
        return CardText(NO_LIVE_DATA, "Needs NOAA space weather")
    }

    fun spaceWeather(assessment: AssessmentResult?, spaceWeather: SpaceWeather?): CardText {
        val kp = assessment.rule { it.ruleId.startsWith("SP-KP") }
        if (spaceWeather == null || kp == null) return CardText(NO_LIVE_DATA, "NOAA SWPC unreachable")
        return CardText(kp.inputValueFormatted, kp.thresholdFormatted)
    }
}

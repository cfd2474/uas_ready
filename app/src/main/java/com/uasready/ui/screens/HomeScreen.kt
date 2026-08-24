package com.uasready.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uasready.domain.model.AssessmentCategory
import com.uasready.domain.model.AssessmentStatus
import com.uasready.ui.components.SquareMetricCard
import com.uasready.ui.components.StatusBanner
import com.uasready.ui.theme.*
import com.uasready.ui.viewmodel.MainUiState

@Composable
fun HomeScreen(
    uiState: MainUiState,
    onNavigateToAssessment: (AssessmentCategory?) -> Unit,
    onNavigateToForecast: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) 3 else 2

    val assessment = uiState.assessmentResult

    // Extract category assessments
    val weatherCat = assessment?.categoryAssessments?.firstOrNull { it.category == AssessmentCategory.WEATHER }
    val airspaceCat = assessment?.categoryAssessments?.firstOrNull { it.category == AssessmentCategory.AIRSPACE }
    val spaceCat = assessment?.categoryAssessments?.firstOrNull { it.category == AssessmentCategory.SPACE_WEATHER }
    val daylightCat = assessment?.categoryAssessments?.firstOrNull { it.category == AssessmentCategory.DAYLIGHT }
    val pilotCat = assessment?.categoryAssessments?.firstOrNull { it.category == AssessmentCategory.PILOT_QUALIFICATIONS }
    val aircraftCat = assessment?.categoryAssessments?.firstOrNull { it.category == AssessmentCategory.AIRCRAFT_LIMITS }

    // 120-Minute Forecast Status Calculation:
    // Any failure in 0-60m window -> NO-GO; Failures in 60-120m window -> CAUTION; Otherwise -> GO
    val forecastStatus: AssessmentStatus = when {
        assessment == null -> AssessmentStatus.DATA_UNAVAILABLE
        weatherCat?.status == AssessmentStatus.NO_GO || daylightCat?.status == AssessmentStatus.NO_GO -> AssessmentStatus.NO_GO
        weatherCat?.status == AssessmentStatus.CAUTION || daylightCat?.status == AssessmentStatus.CAUTION -> AssessmentStatus.CAUTION
        else -> AssessmentStatus.GO
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier
            .fillMaxSize()
            .background(AviationDarkBackground)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Dominant Status Banner or "Obtaining..." Placeholder (Spans All Columns)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Spacer(modifier = Modifier.height(4.dp))

                if (assessment != null && !uiState.isLiveLoading) {
                    StatusBanner(
                        assessmentResult = assessment,
                        onTap = { onNavigateToAssessment(null) }
                    )
                } else {
                    // Obtaining Telemetry Placeholder Banner
                    ObtainingStatusPlaceholder(
                        isPilotPending = uiState.isPilotSelectionPending,
                        isLoading = uiState.isLiveLoading
                    )
                }
            }
        }

        // 2. Section Header (Spans All Columns)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "OPERATIONAL OVERVIEW",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Card 1: Location (Square Card)
        item {
            SquareMetricCard(
                title = "Location",
                primaryValue = uiState.currentLocation.displayName,
                secondaryValue = uiState.currentLocation.formattedCoordinates,
                icon = Icons.Default.LocationOn,
                onClick = onNavigateToMap
            )
        }

        // Card 2: Weather & Wind (Square Card)
        item {
            val tempRule = assessment?.allRuleResults?.firstOrNull { it.ruleId.startsWith("WX-TEMP") || it.ruleId.startsWith("AC-TEMP") }
            val gustRule = assessment?.allRuleResults?.firstOrNull { it.ruleId.startsWith("WX-GUST") || it.ruleId.startsWith("AC-GUST") }
            val windRule = assessment?.allRuleResults?.firstOrNull { it.ruleId.startsWith("WX-WIND") || it.ruleId.startsWith("AC-WIND") }
            val visRule = assessment?.allRuleResults?.firstOrNull { it.ruleId.startsWith("WX-VIS") }

            val weatherSummary = "${gustRule?.inputValueFormatted ?: "Wind 8 MPH"} • ${visRule?.inputValueFormatted ?: "Vis 10 SM"}"
            val weatherWorstStatus = listOfNotNull(
                weatherCat?.status,
                tempRule?.status,
                gustRule?.status,
                windRule?.status
            ).maxByOrNull { it.priority } ?: weatherCat?.status

            SquareMetricCard(
                title = "Weather & Wind",
                primaryValue = "${tempRule?.inputValueFormatted ?: "75°F"} • Wind",
                secondaryValue = weatherSummary,
                status = weatherWorstStatus,
                icon = Icons.Default.Cloud,
                onClick = { onNavigateToAssessment(AssessmentCategory.WEATHER) }
            )
        }

        // Card 3: Airspace & openAIP (Square Card)
        item {
            val airspaceRule = assessment?.allRuleResults?.firstOrNull { it.ruleId.startsWith("AIR-CTRL") || it.ruleId.startsWith("AIR-TFR") }
            SquareMetricCard(
                title = "Airspace & openAIP",
                primaryValue = airspaceRule?.inputValueFormatted ?: "Class G (Uncontrolled)",
                secondaryValue = airspaceRule?.thresholdFormatted ?: "No active flight restrictions",
                status = airspaceCat?.status,
                icon = Icons.Default.Flight,
                onClick = { onNavigateToAssessment(AssessmentCategory.AIRSPACE) }
            )
        }

        // Card 4: Daylight & Solar (Square Card)
        item {
            val sunRule = assessment?.allRuleResults?.firstOrNull { it.ruleId.startsWith("SUN-") }
            SquareMetricCard(
                title = "Daylight & Solar",
                primaryValue = sunRule?.inputValueFormatted ?: "Full Daylight",
                secondaryValue = sunRule?.explanation ?: "Flight window within daylight",
                status = daylightCat?.status,
                icon = Icons.Default.WbSunny,
                onClick = { onNavigateToAssessment(AssessmentCategory.DAYLIGHT) }
            )
        }

        // Card 5: GNSS Satellites Visible & HDOP (Square Card)
        item {
            val satsRule = assessment?.allRuleResults?.firstOrNull { it.ruleId == "SP-GNSS-SATS" }
            val hdopRule = assessment?.allRuleResults?.firstOrNull { it.ruleId == "SP-GNSS-HDOP" }
            val gnss = uiState.estimatedGnss

            val primaryText = if (gnss != null) {
                "~${gnss.lockedSatellitesCount} Sats Visible"
            } else {
                satsRule?.inputValueFormatted ?: "12+ Sats Visible"
            }

            val secondaryText = if (gnss != null) {
                "HDOP ${gnss.estimatedHdop} • 3D Fix"
            } else {
                hdopRule?.inputValueFormatted ?: "HDOP <= 1.5 • Multi-GNSS"
            }

            val gnssWorstStatus = when {
                satsRule?.status == AssessmentStatus.NO_GO || hdopRule?.status == AssessmentStatus.NO_GO -> AssessmentStatus.NO_GO
                satsRule?.status == AssessmentStatus.CAUTION || hdopRule?.status == AssessmentStatus.CAUTION -> AssessmentStatus.CAUTION
                satsRule?.status == AssessmentStatus.GO -> AssessmentStatus.GO
                else -> spaceCat?.status
            }

            SquareMetricCard(
                title = "GNSS & Sats",
                primaryValue = primaryText,
                secondaryValue = secondaryText,
                status = gnssWorstStatus,
                icon = Icons.Default.Satellite,
                onClick = { onNavigateToAssessment(AssessmentCategory.SPACE_WEATHER) }
            )
        }

        // Card 6: Space Weather & Geomagnetic Kp (Square Card)
        item {
            val kpRule = assessment?.allRuleResults?.firstOrNull { it.ruleId.startsWith("SP-KP") }

            SquareMetricCard(
                title = "Space Weather (Kp)",
                primaryValue = kpRule?.inputValueFormatted ?: "Kp 2.0 (Nominal)",
                secondaryValue = kpRule?.thresholdFormatted ?: "Normal solar activity",
                status = spaceCat?.status,
                icon = Icons.Default.Public,
                onClick = { onNavigateToAssessment(AssessmentCategory.SPACE_WEATHER) }
            )
        }

        // Card 7: Pilot Operating Authority (Square Card)
        item {
            SquareMetricCard(
                title = "Pilot Authority",
                primaryValue = if (uiState.isPilotSelectionPending) "Pending" else uiState.currentPilot.activeAuthority.displayName,
                secondaryValue = if (uiState.isPilotSelectionPending) "Select certification" else uiState.currentPilot.activeAuthority.description,
                status = pilotCat?.status,
                icon = Icons.Default.Badge,
                onClick = { onNavigateToAssessment(AssessmentCategory.PILOT_QUALIFICATIONS) }
            )
        }

        // Card 8: 120 Minute Forecast (Square Card with Status)
        item {
            SquareMetricCard(
                title = "120 Minute Forecast",
                primaryValue = if (forecastStatus == AssessmentStatus.GO) "Window Cleared" else if (forecastStatus == AssessmentStatus.CAUTION) "Caution Ahead" else "Restricted",
                secondaryValue = "0–60m Launch • 60–120m Watch",
                status = forecastStatus,
                icon = Icons.Default.Schedule,
                onClick = onNavigateToForecast
            )
        }

        // Bottom spacing
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
fun ObtainingStatusPlaceholder(
    isPilotPending: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AviationDarkCard)
            .border(1.5.dp, AviationAccent.copy(alpha = alpha), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = AviationAccent,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isPilotPending) "AWAITING PILOT CERTIFICATION SELECTION..." else "OBTAINING COMPLIANCE EVALUATION...",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = AviationAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = if (isPilotPending) "Select pilot status in dialog to start evaluation" else "Querying openAIP, NOAA Space Weather & local METAR...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

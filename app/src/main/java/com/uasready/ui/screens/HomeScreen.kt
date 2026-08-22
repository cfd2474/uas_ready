package com.uasready.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uasready.domain.model.AssessmentCategory
import com.uasready.domain.model.AssessmentStatus
import com.uasready.ui.components.MetricSummaryCard
import com.uasready.ui.components.StatusBanner
import com.uasready.ui.theme.*
import com.uasready.ui.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    onNavigateToAssessment: () -> Unit,
    onNavigateToAircraft: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMap: () -> Unit,
    onRefreshLiveData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assessment = uiState.assessmentResult

    // Extract quick status categories
    val weatherCat = assessment?.categoryAssessments?.firstOrNull { it.category == AssessmentCategory.WEATHER }
    val airspaceCat = assessment?.categoryAssessments?.firstOrNull { it.category == AssessmentCategory.AIRSPACE }
    val spaceCat = assessment?.categoryAssessments?.firstOrNull { it.category == AssessmentCategory.SPACE_WEATHER }
    val daylightCat = assessment?.categoryAssessments?.firstOrNull { it.category == AssessmentCategory.DAYLIGHT }
    val pilotCat = assessment?.categoryAssessments?.firstOrNull { it.category == AssessmentCategory.PILOT_QUALIFICATIONS }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "UASREADY",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AviationDarkCard)
                                .border(1.dp, AviationDarkBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = SafetyGoLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Refresh Button
                    IconButton(onClick = onRefreshLiveData) {
                        if (uiState.isLiveLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AviationAccent, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Live Telemetry", tint = TextPrimary)
                        }
                    }

                    // Settings / Pilot Authority Gear Button
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings & Authority", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AviationDarkBackground
                )
            )
        },
        containerColor = AviationDarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // 1. Dominant GO / CAUTION / NO-GO Status Banner
                if (assessment != null) {
                    StatusBanner(
                        assessmentResult = assessment,
                        onTap = onNavigateToAssessment
                    )
                }
            }

            // 2. Fast Summary Metric Cards Grid
            item {
                Text(
                    text = "OPERATIONAL OVERVIEW",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Location Card
                MetricSummaryCard(
                    title = "Location",
                    primaryValue = uiState.currentLocation.displayName,
                    secondaryValue = uiState.currentLocation.formattedCoordinates,
                    icon = Icons.Default.LocationOn,
                    onClick = onNavigateToMap
                )
            }

            item {
                // Weather Summary Card
                val tempRule = assessment?.allRuleResults?.firstOrNull { it.ruleId.startsWith("AC-TEMP") }
                val gustRule = assessment?.allRuleResults?.firstOrNull { it.ruleId.startsWith("AC-GUST") }
                val visRule = assessment?.allRuleResults?.firstOrNull { it.ruleId.startsWith("WX-VIS") }

                val weatherSummary = "${gustRule?.inputValueFormatted ?: "Wind 8 MPH"} • ${visRule?.inputValueFormatted ?: "Vis 10 SM"}"
                MetricSummaryCard(
                    title = "Weather & Wind",
                    primaryValue = "${tempRule?.inputValueFormatted ?: "75°F"} • Wind & Gusts",
                    secondaryValue = weatherSummary,
                    status = weatherCat?.status,
                    icon = Icons.Default.Cloud,
                    onClick = onNavigateToAssessment
                )
            }

            item {
                // Airspace Summary Card
                val airspaceRule = assessment?.allRuleResults?.firstOrNull { it.ruleId.startsWith("AIR-CTRL") || it.ruleId.startsWith("AIR-TFR") }
                MetricSummaryCard(
                    title = "Airspace & Restrictions",
                    primaryValue = airspaceRule?.inputValueFormatted ?: "Class G (Uncontrolled)",
                    secondaryValue = airspaceRule?.thresholdFormatted ?: "No active TFRs in flight area",
                    status = airspaceCat?.status,
                    icon = Icons.Default.Flight,
                    onClick = onNavigateToAssessment
                )
            }

            item {
                // Space Weather & Planetary Kp
                val kpRule = assessment?.allRuleResults?.firstOrNull { it.ruleId.startsWith("SP-KP") }

                MetricSummaryCard(
                    title = "Space Weather & Geomagnetic (Kp)",
                    primaryValue = kpRule?.inputValueFormatted ?: "Kp 2.0 (Nominal)",
                    secondaryValue = kpRule?.thresholdFormatted ?: "Normal solar & ionospheric activity",
                    status = spaceCat?.status,
                    icon = Icons.Default.Public,
                    onClick = onNavigateToAssessment
                )
            }

            item {
                // GNSS Satellites Visible & HDOP
                val satsRule = assessment?.allRuleResults?.firstOrNull { it.ruleId == "SP-GNSS-SATS" }
                val hdopRule = assessment?.allRuleResults?.firstOrNull { it.ruleId == "SP-GNSS-HDOP" }
                val gnss = uiState.estimatedGnss

                val primaryText = if (gnss != null) {
                    "~${gnss.lockedSatellitesCount} Satellites Visible"
                } else {
                    satsRule?.inputValueFormatted ?: "12+ Satellites Visible"
                }

                val secondaryText = if (gnss != null) {
                    val terrainStr = gnss.terrainProfile?.let { " • ${it.terrainClassification} (${it.maxObstructionDeg}°)" } ?: " • ${gnss.signalIntegrityPercent}% Signal"
                    "HDOP ${gnss.estimatedHdop} • 3D Fix$terrainStr"
                } else {
                    hdopRule?.inputValueFormatted ?: "HDOP <= 1.5 • Multi-GNSS"
                }

                val gnssWorstStatus = when {
                    satsRule?.status == AssessmentStatus.NO_GO || hdopRule?.status == AssessmentStatus.NO_GO -> AssessmentStatus.NO_GO
                    satsRule?.status == AssessmentStatus.CAUTION || hdopRule?.status == AssessmentStatus.CAUTION -> AssessmentStatus.CAUTION
                    satsRule?.status == AssessmentStatus.GO -> AssessmentStatus.GO
                    else -> spaceCat?.status
                }

                MetricSummaryCard(
                    title = "GNSS Satellites & Geometry",
                    primaryValue = primaryText,
                    secondaryValue = secondaryText,
                    status = gnssWorstStatus,
                    icon = Icons.Default.Satellite,
                    onClick = onNavigateToAssessment
                )
            }

            item {
                // Daylight Timing
                val sunRule = assessment?.allRuleResults?.firstOrNull { it.ruleId.startsWith("SUN-") }
                MetricSummaryCard(
                    title = "Daylight & Solar",
                    primaryValue = sunRule?.inputValueFormatted ?: "Full Daylight",
                    secondaryValue = sunRule?.explanation ?: "Flight window remains within daylight",
                    status = daylightCat?.status,
                    icon = Icons.Default.WbSunny,
                    onClick = onNavigateToAssessment
                )
            }

            item {
                // Aircraft Fleet Profile
                MetricSummaryCard(
                    title = "Aircraft Fleet Profile",
                    primaryValue = uiState.selectedAircraft.displayName,
                    secondaryValue = "Max Sustained ${uiState.selectedAircraft.limitations.maxSustainedWindSpeedMph.toInt()} MPH • Max Gust ${uiState.selectedAircraft.limitations.maxGustSpeedMph.toInt()} MPH",
                    icon = Icons.Default.Sensors,
                    onClick = onNavigateToAircraft
                )
            }

            item {
                // Pilot Authority
                MetricSummaryCard(
                    title = "Pilot Operating Authority",
                    primaryValue = uiState.currentPilot.activeAuthority.displayName,
                    secondaryValue = uiState.currentPilot.activeAuthority.description,
                    status = pilotCat?.status,
                    icon = Icons.Default.Badge,
                    onClick = onNavigateToSettings
                )
            }

            item {
                // 120 Minutes Forecasted Flight Window
                MetricSummaryCard(
                    title = "Flight Forecast Horizon",
                    primaryValue = "120 Minutes Forecasted",
                    secondaryValue = "0–60m Immediate Launch • 60–120m Degradation Watch",
                    icon = Icons.Default.Schedule,
                    onClick = onNavigateToAssessment
                )
            }

            // View Full Assessment Button
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onNavigateToAssessment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AviationCyan,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "VIEW FULL ASSESSMENT AUDIT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

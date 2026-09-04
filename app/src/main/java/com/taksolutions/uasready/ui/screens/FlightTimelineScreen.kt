package com.taksolutions.uasready.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taksolutions.uasready.domain.model.AssessmentStatus
import com.taksolutions.uasready.ui.theme.*
import com.taksolutions.uasready.ui.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightTimelineScreen(
    uiState: MainUiState,
    onUpdateFlightWindow: (Long, Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assessment = uiState.assessmentResult
    val flightWindow = uiState.flightWindow
    val samplingTimes = flightWindow.getSamplingIntervals(30)

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FLIGHT WINDOW TIMELINE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AviationDarkBackground)
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
            // Flight Window Header Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AviationDarkCard)
                        .border(1.dp, AviationDarkBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PLANNED WINDOW", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                            }
                            Text(
                                text = "${flightWindow.durationMinutes} min",
                                style = MaterialTheme.typography.titleMedium.copy(color = AviationAccent, fontWeight = FontWeight.Bold)
                            )
                        }

                        Text(
                            text = "${timeFormat.format(Date(flightWindow.startEpochMs))} — ${timeFormat.format(Date(flightWindow.endEpochMs))}",
                            style = MaterialTheme.typography.headlineMedium.copy(color = TextPrimary, fontWeight = FontWeight.Black)
                        )

                        Text(
                            text = "Condition evaluations are performed across continuous 30-minute intervals throughout this planned duration.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }
            }

            // Timeline Forecast Breakdown
            item {
                Text(
                    text = "INTERVAL SAFETY PROJECTIONS",
                    style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(samplingTimes) { sampleTime ->
                val offsetMin = (sampleTime - flightWindow.startEpochMs) / (60 * 1000)
                val timeStr = timeFormat.format(Date(sampleTime))

                // Find if any forecast rule triggered for this offset
                val forecastRules = assessment?.allRuleResults?.filter {
                    it.isForecastDerived && it.forecastTimeOffsetMinutes != null && Math.abs(it.forecastTimeOffsetMinutes - offsetMin) <= 30
                } ?: emptyList()

                val worstIntervalStatus = forecastRules.maxByOrNull { it.status.priority }?.status ?: assessment?.overallStatus ?: AssessmentStatus.GO
                val statusColor = worstIntervalStatus.toColor()
                val statusBg = worstIntervalStatus.toBgColor()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AviationDarkCard)
                        .border(1.dp, AviationDarkBorder, RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )
                                Text(
                                    text = if (offsetMin == 0L) "Launch (T+0)" else "T+${offsetMin}m",
                                    style = MaterialTheme.typography.labelMedium.copy(color = AviationAccent, fontSize = 11.sp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (forecastRules.isNotEmpty()) forecastRules.first().title else "Nominal Atmospheric Stability",
                                    style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = if (forecastRules.isNotEmpty()) forecastRules.first().inputValueFormatted else "Wind & Gusts Within Aircraft Limits",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 12.sp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = worstIntervalStatus.name,
                                    style = MaterialTheme.typography.labelMedium.copy(color = statusColor, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

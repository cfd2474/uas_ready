package com.uasready.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uasready.domain.model.*
import com.uasready.ui.components.RuleAuditCard
import com.uasready.ui.theme.*
import com.uasready.ui.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class ForecastTimeBlock(
    val offsetMinutes: Int,
    val timeFormatted: String,
    val offsetLabel: String,
    val tempF: Double,
    val windSpeedMph: Double,
    val windGustMph: Double,
    val windDirectionDegrees: Int,
    val windDirectionCardinal: String,
    val cloudCoverPercent: Int,
    val cloudCeilingFt: Double?,
    val precipitationProbabilityPercent: Int,
    val precipitationRateInchesPerHour: Double,
    val precipitationType: PrecipitationType,
    val conditionsDescription: String,
    val status: AssessmentStatus
)

fun degreesToCardinal(degrees: Int): String {
    val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val index = (((degrees % 360) + 360) % 360 / 22.5 + 0.5).toInt() % 16
    return directions[index]
}

fun calculateForecastBlocks(uiState: MainUiState): List<ForecastTimeBlock> {
    val obs = uiState.weatherObservation
    val forecast = uiState.weatherForecast
    val startMs = uiState.flightWindow.startEpochMs
    val aircraft = uiState.selectedAircraft
    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

    val offsets = listOf(0, 30, 60, 90, 120)

    return offsets.map { offsetMin ->
        val targetMs = startMs + offsetMin * 60 * 1000L
        val timeStr = timeFormat.format(Date(targetMs))
        val offsetLabel = if (offsetMin == 0) "T+0m (Launch)" else "T+${offsetMin}m"

        // Find nearest interval from hourly forecast, or fallback to current observation
        val matchedHourly = forecast?.intervals?.minByOrNull { Math.abs(it.timestampEpochMs - targetMs) }

        val tempF = if (offsetMin == 0 && obs != null) obs.temperatureF else matchedHourly?.temperatureF ?: obs?.temperatureF ?: 72.0
        val windSpeed = if (offsetMin == 0 && obs != null) obs.windSpeedMph else matchedHourly?.windSpeedMph ?: obs?.windSpeedMph ?: 8.0
        val windGust = if (offsetMin == 0 && obs != null) obs.windGustMph else matchedHourly?.windGustMph ?: obs?.windGustMph ?: 14.0
        val windDir = if (offsetMin == 0 && obs != null) obs.windDirectionDegrees else matchedHourly?.windDirectionDegrees ?: obs?.windDirectionDegrees ?: 240
        val cloudCover = if (offsetMin == 0 && obs != null) obs.cloudCoverPercent else if (matchedHourly?.cloudCeilingFt != null) 75 else 15
        val cloudCeiling = if (offsetMin == 0 && obs != null) obs.cloudCeilingFt else matchedHourly?.cloudCeilingFt
        val precipProb = if (offsetMin == 0 && obs != null) obs.precipitationProbabilityPercent else matchedHourly?.precipitationProbabilityPercent ?: 0
        val precipRate = if (offsetMin == 0 && obs != null) obs.precipitationRateInchesPerHour else matchedHourly?.precipitationRateInchesPerHour ?: 0.0
        val precipType = if (offsetMin == 0 && obs != null) obs.precipitationType else matchedHourly?.precipitationType ?: PrecipitationType.NONE
        val desc = if (offsetMin == 0 && obs != null) obs.conditionsDescription else matchedHourly?.conditionsDescription ?: obs?.conditionsDescription ?: "Clear Sky"

        // Determine status against aircraft limitations
        val maxGust = aircraft.limitations.maxGustSpeedMph
        val maxSustained = aircraft.limitations.maxSustainedWindSpeedMph
        val precipAllowed = aircraft.limitations.precipitationAllowed

        val status = when {
            windGust > maxGust || windSpeed > maxSustained -> AssessmentStatus.NO_GO
            precipType != PrecipitationType.NONE && !precipAllowed && precipRate > 0.0 -> AssessmentStatus.NO_GO
            windGust >= maxGust - 5.0 || windSpeed >= maxSustained - 4.0 -> AssessmentStatus.CAUTION
            precipProb >= 30 || (cloudCeiling != null && cloudCeiling < 1000.0) -> AssessmentStatus.CAUTION
            else -> AssessmentStatus.GO
        }

        ForecastTimeBlock(
            offsetMinutes = offsetMin,
            timeFormatted = timeStr,
            offsetLabel = offsetLabel,
            tempF = tempF,
            windSpeedMph = windSpeed,
            windGustMph = windGust,
            windDirectionDegrees = windDir,
            windDirectionCardinal = degreesToCardinal(windDir),
            cloudCoverPercent = cloudCover,
            cloudCeilingFt = cloudCeiling,
            precipitationProbabilityPercent = precipProb,
            precipitationRateInchesPerHour = precipRate,
            precipitationType = precipType,
            conditionsDescription = desc,
            status = status
        )
    }
}

@Composable
fun AssessmentDetailScreen(
    uiState: MainUiState,
    onCategoryFilterSelected: (AssessmentCategory?) -> Unit,
    onClearScrollToForecast: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assessment = uiState.assessmentResult
    val listState = rememberLazyListState()
    val forecastBlocks = remember(uiState.weatherObservation, uiState.weatherForecast, uiState.selectedAircraft, uiState.flightWindow) {
        calculateForecastBlocks(uiState)
    }

    // Auto-scroll to Forecast section when opened via main forecast card
    LaunchedEffect(uiState.scrollToForecastOnDetail) {
        if (uiState.scrollToForecastOnDetail) {
            // Index 2 corresponds to the start of the Forecast Breakdown section
            listState.animateScrollToItem(2)
            onClearScrollToForecast()
        }
    }

    Scaffold(
        topBar = {
            // Ultra-compact top bar header designed for 360dp landscape
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(AviationDarkBackground)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DETAILED REPORT",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                )
            }
        },
        containerColor = AviationDarkBackground
    ) { paddingValues ->
        if (assessment == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AviationAccent)
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Compact Overall Status Summary Card
            item {
                val overallColor = assessment.overallStatus.toColor()
                val overallBg = assessment.overallStatus.toBgColor()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(overallBg)
                        .border(1.dp, overallColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(overallColor)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = assessment.overallStatus.name,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = assessment.primaryHeadline,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                )
                            }
                        }

                        if (assessment.primaryReasons.isNotEmpty()) {
                            assessment.primaryReasons.forEach { reason ->
                                Text(
                                    text = "• $reason",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategoryFilter == null,
                            onClick = { onCategoryFilterSelected(null) },
                            label = { Text("ALL (${assessment.allRuleResults.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AviationCyan,
                                selectedLabelColor = Color.White,
                                containerColor = AviationDarkCard,
                                labelColor = TextSecondary
                            )
                        )
                    }

                    items(AssessmentCategory.values()) { category ->
                        val count = assessment.categoryAssessments.firstOrNull { it.category == category }?.ruleResults?.size ?: 0
                        FilterChip(
                            selected = uiState.selectedCategoryFilter == category,
                            onClick = { onCategoryFilterSelected(category) },
                            label = { Text("${category.displayName.substringBefore(" ")} ($count)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AviationCyan,
                                selectedLabelColor = Color.White,
                                containerColor = AviationDarkCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // 3. Dedicated 120-Minute Forecast Breakdown Section (30-Minute Time Blocks)
            if (uiState.selectedCategoryFilter == null || uiState.selectedCategoryFilter == AssessmentCategory.WEATHER) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "120-MINUTE FORECAST (30-MIN BLOCKS)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = AviationAccent,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Text(
                            text = "${uiState.selectedAircraft.displayName}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp)
                        )
                    }
                }

                items(forecastBlocks) { block ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = SolidColor(
                                if (block.status == AssessmentStatus.NO_GO) SafetyNoGoLight
                                else if (block.status == AssessmentStatus.CAUTION) SafetyCautionLight
                                else AviationDarkBorder
                            )
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Top Row: Time, Offset, Condition, and Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = block.timeFormatted,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = TextPrimary,
                                            fontSize = 13.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AviationDarkSurface,
                                        border = BorderStroke(1.dp, AviationDarkBorder)
                                    ) {
                                        Text(
                                            text = block.offsetLabel,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = AviationAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${block.conditionsDescription} • ${block.tempF.roundToInt()}°F",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(block.status.toBgColor())
                                        .border(1.dp, block.status.toColor(), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(block.status.toColor())
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = block.status.name,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = block.status.toColor(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = AviationDarkBorder)

                            // 3 Breakout Elements in a Row: Wind, Clouds, Precipitation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 1. WIND ELEMENT
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AviationDarkSurface,
                                    border = BorderStroke(1.dp, AviationDarkBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Air, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "WIND",
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                                            )
                                        }
                                        Text(
                                            text = "${block.windSpeedMph.roundToInt()} mph ${block.windDirectionCardinal}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp)
                                        )
                                        Text(
                                            text = "Gust ${block.windGustMph.roundToInt()} mph",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (block.windGustMph > uiState.selectedAircraft.limitations.maxGustSpeedMph) SafetyNoGoLight else TextMuted,
                                                fontWeight = if (block.windGustMph > uiState.selectedAircraft.limitations.maxGustSpeedMph) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }

                                // 2. CLOUDS ELEMENT
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AviationDarkSurface,
                                    border = BorderStroke(1.dp, AviationDarkBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Cloud, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "CLOUDS",
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                                            )
                                        }
                                        Text(
                                            text = if (block.cloudCeilingFt != null) "${block.cloudCeilingFt.roundToInt()} ft" else "Unlimited",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp)
                                        )
                                        Text(
                                            text = "${block.cloudCoverPercent}% Cover",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
                                        )
                                    }
                                }

                                // 3. PRECIPITATION ELEMENT
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AviationDarkSurface,
                                    border = BorderStroke(1.dp, AviationDarkBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "PRECIP",
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                                            )
                                        }
                                        Text(
                                            text = if (block.precipitationType == PrecipitationType.NONE) "0% • None" else "${block.precipitationProbabilityPercent}% ${block.precipitationType.name}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp)
                                        )
                                        Text(
                                            text = if (block.precipitationRateInchesPerHour > 0) "${block.precipitationRateInchesPerHour} in/hr" else "0.00 in/hr",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // 4. List of Evaluated Categories and Rules
            val categoriesToDisplay = if (uiState.selectedCategoryFilter != null) {
                assessment.categoryAssessments.filter { it.category == uiState.selectedCategoryFilter }
            } else {
                assessment.categoryAssessments
            }

            items(categoriesToDisplay) { catAssessment ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = catAssessment.category.displayName.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = AviationAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 11.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(catAssessment.status.toBgColor())
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = catAssessment.status.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = catAssessment.status.toColor(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    catAssessment.ruleResults.forEach { rule ->
                        RuleAuditCard(rule = rule, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

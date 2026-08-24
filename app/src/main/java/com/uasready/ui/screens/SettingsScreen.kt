package com.uasready.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uasready.data.nasr.AiracCycleCalculator
import com.uasready.data.nasr.AiracUpdateStatus
import com.uasready.domain.model.PilotAuthorityType
import com.uasready.ui.theme.*
import com.uasready.ui.viewmodel.MainUiState

enum class SettingsCategory(val title: String, val subtitle: String, val icon: ImageVector) {
    PILOT_AUTHORITY("Pilot Operating Authority", "Configure Licensed vs Non-licensed status", Icons.Default.Badge),
    AIRCRAFT_FLEET("Aircraft Fleet Management", "Search models, filter manufacturers & build custom", Icons.Default.FlightTakeoff),
    FAA_NASR_AIRSPACE("FAA NASR Database", "28-day AIRAC cycles, CTAF & TFR management", Icons.Default.CloudSync),
    THEME_APPEARANCE("Theme & Appearance", "Select Light, Dark, or System Auto mode", Icons.Default.Palette),
    UNIT_SYSTEM("Unit System & Telemetry", "Toggle US Aviation vs Metric units", Icons.Default.Straighten),
    DATA_SOURCES("Authoritative Telemetry Sources", "Review FAA NASR, NOAA & weather feeds", Icons.Default.Sensors)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onSetAuthority: (PilotAuthorityType) -> Unit,
    onSelectAircraft: (String) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onNavigateToAircraft: () -> Unit,
    onCheckAiracUpdate: () -> Unit = {},
    onPerformAiracUpdate: () -> Unit = {},
    onRebuildNasrDatabase: () -> Unit = {},
    onResetAiracUpdateStatus: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    var isMetric by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (activeCategory != null) activeCategory!!.title.uppercase() else "SETTINGS & PREFERENCES",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeCategory != null) {
                                activeCategory = null
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // If no category is selected: Show category selection buttons
            if (activeCategory == null) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SELECT CONFIGURATION CATEGORY",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                SettingsCategory.values().forEach { category ->
                    item {
                        val currentBadge = when (category) {
                            SettingsCategory.PILOT_AUTHORITY -> uiState.currentPilot.activeAuthority.displayName
                            SettingsCategory.AIRCRAFT_FLEET -> uiState.selectedAircraft.displayName
                            SettingsCategory.FAA_NASR_AIRSPACE -> uiState.airacCycleInfo?.let { "Cycle ${it.cycleName}" } ?: "SQLite R*Tree"
                            SettingsCategory.THEME_APPEARANCE -> uiState.themeMode.displayName
                            SettingsCategory.UNIT_SYSTEM -> if (isMetric) "Metric" else "US Aviation"
                            SettingsCategory.DATA_SOURCES -> "FAA NASR"
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationDarkBorder)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (category == SettingsCategory.AIRCRAFT_FLEET) {
                                        onNavigateToAircraft()
                                    } else {
                                        activeCategory = category
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AviationDarkSurface)
                                            .border(1.dp, AviationDarkBorder, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(category.icon, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = category.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                        )
                                        Text(
                                            text = category.subtitle,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, color = TextSecondary)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AviationDarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder)
                                ) {
                                    Text(
                                        text = currentBadge,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AviationAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Category-specific interactive data entry card
                item {
                    when (activeCategory!!) {
                        SettingsCategory.PILOT_AUTHORITY -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationAccent)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Select pilot certification status for session evaluation:",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                    )

                                    PilotAuthorityType.values().forEach { authority ->
                                        val isSelected = uiState.currentPilot.activeAuthority == authority
                                        val borderColor = if (isSelected) AviationAccent else AviationDarkBorder
                                        val bgColor = if (isSelected) AviationDarkSurface else Color.Transparent

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                                .background(bgColor)
                                                .clickable { onSetAuthority(authority) }
                                                .padding(12.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = authority.displayName,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) AviationAccent else TextPrimary
                                                        )
                                                    )
                                                    if (isSelected) {
                                                        Surface(shape = RoundedCornerShape(4.dp), color = AviationAccent.copy(alpha = 0.15f)) {
                                                            Text(
                                                                text = "ACTIVE",
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                style = MaterialTheme.typography.labelSmall.copy(color = AviationAccent, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = authority.description,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, color = TextSecondary)
                                                )
                                            }
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { onSetAuthority(authority) },
                                                colors = RadioButtonDefaults.colors(selectedColor = AviationAccent, unselectedColor = TextSecondary)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SettingsCategory.AIRCRAFT_FLEET -> {
                            // Direct navigation to AircraftScreen handles this
                            LaunchedEffect(Unit) {
                                onNavigateToAircraft()
                            }
                        }

                        SettingsCategory.THEME_APPEARANCE -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationAccent)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Select application display theme:",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                    )

                                    AppThemeMode.values().forEach { mode ->
                                        val isSelected = uiState.themeMode == mode
                                        val borderColor = if (isSelected) AviationAccent else AviationDarkBorder
                                        val bgColor = if (isSelected) AviationDarkSurface else Color.Transparent

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                                .background(bgColor)
                                                .clickable { onSetThemeMode(mode) }
                                                .padding(12.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = mode.displayName,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) AviationAccent else TextPrimary
                                                        )
                                                    )
                                                    if (isSelected) {
                                                        Surface(shape = RoundedCornerShape(4.dp), color = AviationAccent.copy(alpha = 0.15f)) {
                                                            Text(
                                                                text = "ACTIVE",
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                style = MaterialTheme.typography.labelSmall.copy(color = AviationAccent, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = mode.description,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, color = TextSecondary)
                                                )
                                            }
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { onSetThemeMode(mode) },
                                                colors = RadioButtonDefaults.colors(selectedColor = AviationAccent, unselectedColor = TextSecondary)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SettingsCategory.UNIT_SYSTEM -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationAccent)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Select preferred unit system for flight telemetry:",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(AviationDarkSurface)
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        // Left: US Standard
                                        Column(
                                            horizontalAlignment = Alignment.Start,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { isMetric = false }
                                        ) {
                                            Text(
                                                text = "Standard (US)",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (!isMetric) AviationAccent else TextSecondary
                                                )
                                            )
                                            Text(
                                                text = "MPH • °F • ft • SM",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = if (!isMetric) TextPrimary else TextMuted,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (!isMetric) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            )
                                        }

                                        // Center: Toggle Switch
                                        Switch(
                                            checked = isMetric,
                                            onCheckedChange = { isMetric = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = AviationAccent,
                                                uncheckedThumbColor = Color.White,
                                                uncheckedTrackColor = AviationDarkBorder
                                            )
                                        )

                                        // Right: Metric System
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { isMetric = true }
                                        ) {
                                            Text(
                                                text = "Metric (SI)",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isMetric) AviationAccent else TextSecondary
                                                )
                                            )
                                            Text(
                                                text = "m/s • °C • m • km",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = if (isMetric) TextPrimary else TextMuted,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isMetric) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SettingsCategory.FAA_NASR_AIRSPACE -> {
                            val airac = uiState.airacCycleInfo
                            val updateStatus = uiState.airacUpdateStatus

                            Card(
                                colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationAccent)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "FAA 28-Day NASR Database",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                            )
                                            Text(
                                                text = if (airac != null) "Cycle ${airac.cycleName} • On-Device SQLite R*Tree" else "Awaiting database check",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                                            )
                                        }

                                        if (airac != null) {
                                            val badgeColor = when {
                                                airac.isExpired -> SafetyNoGo
                                                airac.daysUntilExpiry <= 3 -> SafetyCautionLight
                                                else -> SafetyGoLight
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = badgeColor.copy(alpha = 0.2f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
                                            ) {
                                                Text(
                                                    text = if (airac.isExpired) "EXPIRED" else "${airac.daysUntilExpiry}d REMAINING",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = AviationDarkBorder)

                                    if (airac != null) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Effective: ${AiracCycleCalculator.formatDate(airac.effectiveEpochMs)} — Expires: ${AiracCycleCalculator.formatDate(airac.expireEpochMs)}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 10.5.sp)
                                            )
                                            Text(
                                                text = "Last checked: ${AiracCycleCalculator.formatDate(airac.lastCheckedEpochMs)}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 9.5.sp)
                                            )
                                        }
                                    }

                                    // Status Banners
                                    when (updateStatus) {
                                        is AiracUpdateStatus.Checking -> {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AviationAccent)
                                                Text("Checking FAA 28-day update availability...", style = MaterialTheme.typography.bodySmall.copy(color = AviationAccent))
                                            }
                                        }
                                        is AiracUpdateStatus.UpdateAvailable -> {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = SafetyCautionLight.copy(alpha = 0.15f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, SafetyCautionLight.copy(alpha = 0.5f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(
                                                        text = "New FAA Cycle Available: ${updateStatus.newCycle}",
                                                        style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                                                    )
                                                    Button(
                                                        onClick = onPerformAiracUpdate,
                                                        colors = ButtonDefaults.buttonColors(containerColor = AviationAccent),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("DOWNLOAD & ATOMICALLY SWAP DATABASE", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                        is AiracUpdateStatus.Downloading -> {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AviationCyan)
                                                Text("Building temporary database & verifying integrity...", style = MaterialTheme.typography.bodySmall.copy(color = AviationCyan))
                                            }
                                        }
                                        is AiracUpdateStatus.Rebuilding -> {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SafetyCautionLight)
                                                Text("Rebuilding on-device R*Tree database from seed...", style = MaterialTheme.typography.bodySmall.copy(color = SafetyCautionLight))
                                            }
                                        }
                                        is AiracUpdateStatus.Success -> {
                                            Text(
                                                text = "✓ Database up-to-date (Cycle ${updateStatus.cycleName})",
                                                style = MaterialTheme.typography.bodySmall.copy(color = SafetyGoLight, fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        is AiracUpdateStatus.UpToDate -> {
                                            Text(
                                                text = "✓ Current cycle is up to date.",
                                                style = MaterialTheme.typography.bodySmall.copy(color = SafetyGoLight, fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        is AiracUpdateStatus.Error -> {
                                            Text(
                                                text = "⚠ ${updateStatus.message}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = SafetyNoGo, fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        AiracUpdateStatus.Idle -> {}
                                    }

                                    // Action Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = onCheckAiracUpdate,
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = AviationAccent)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("CHECK UPDATES", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp))
                                        }

                                        OutlinedButton(
                                            onClick = onRebuildNasrDatabase,
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                        ) {
                                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("REBUILD DB", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp))
                                        }
                                    }
                                }
                            }
                        }

                        SettingsCategory.DATA_SOURCES -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationAccent)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("• Airspace & Airports: FAA 28-Day NASR Subscription (APT, FRQ, TWR)", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                                    Text("• Boundaries & SUA: FAA ADDS Open Data ArcGIS FeatureServer", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                                    Text("• UAS Facility Grids: FAA UAS Facility Map V5 FeatureServer", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                                    Text("• TFR Feeds: FAA tfr.faa.gov 14 CFR § 91.137 Active Feeds", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                                    Text("• Weather & Forecast: Open-Meteo & NOAA National Weather Service", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                                    Text("• Space Weather: NOAA SWPC Planetary K-Index & Geomagnetic Scale", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                                    Text("• Terrain Elevation DEM: Open-Meteo 90m SRTM / Copernicus Digital Elevation", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                                    Text("• Solar Ephemeris: NOAA Astronomical Solar Geometry Algorithm", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { activeCategory = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AviationDarkSurface, contentColor = AviationAccent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BACK TO CATEGORIES MENU", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

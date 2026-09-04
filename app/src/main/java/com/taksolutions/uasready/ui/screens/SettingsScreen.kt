package com.taksolutions.uasready.ui.screens

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
import com.taksolutions.uasready.domain.model.PilotAuthorityType
import com.taksolutions.uasready.ui.theme.*
import com.taksolutions.uasready.ui.viewmodel.MainUiState

enum class SettingsCategory(val title: String, val subtitle: String, val icon: ImageVector) {
    PILOT_AUTHORITY("Pilot Operating Authority", "Configure Licensed vs Non-licensed status", Icons.Default.Badge),
    AIRCRAFT_FLEET("Aircraft Fleet Management", "Search models, filter manufacturers & build custom", Icons.Default.FlightTakeoff),
    THEME_APPEARANCE("Theme & Appearance", "Select Light, Dark, or System Auto mode", Icons.Default.Palette),
    UNIT_SYSTEM("Unit System & Telemetry", "Toggle US Aviation vs Metric units", Icons.Default.Straighten),
    DATA_SOURCES("Authoritative Telemetry Sources", "Review openAIP, NOAA & weather feeds", Icons.Default.Sensors)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onSetAuthority: (PilotAuthorityType) -> Unit,
    onSelectAircraft: (String) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onNavigateToAircraft: () -> Unit,
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
                            SettingsCategory.THEME_APPEARANCE -> uiState.themeMode.displayName
                            SettingsCategory.UNIT_SYSTEM -> if (isMetric) "Metric" else "US Aviation"
                            SettingsCategory.DATA_SOURCES -> "openAIP API"
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

                        SettingsCategory.DATA_SOURCES -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationAccent)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("• Weather & Forecast: Open-Meteo & NOAA National Weather Service", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                                    Text("• Space Weather & GNSS: NOAA SWPC Planetary K-Index Feed", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                                    Text("• Terrain Elevation DEM: Open-Meteo 90m SRTM / Copernicus Digital Elevation", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                                    Text("• Solar Ephemeris: NOAA Astronomical Solar Geometry Algorithm", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                                    Text("• Airspace: openAIP Worldwide Aeronautical Database & API", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
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

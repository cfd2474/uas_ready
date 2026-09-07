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
import androidx.compose.ui.platform.LocalUriHandler
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
    DATA_SOURCES("Government Sources & Legal Notice", "Review official .gov data sources & non-affiliation disclaimer", Icons.Default.Gavel)
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
                            SettingsCategory.DATA_SOURCES -> "Official Sources & Notice"
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
                            val uriHandler = LocalUriHandler.current

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // 1. Legal Disclaimer of Non-Official Status
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SafetyCaution)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = SafetyCaution,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "DISCLAIMER OF NON-OFFICIAL STATUS",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp,
                                                    color = SafetyCaution,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                        Text(
                                            text = "UASReady is developed and maintained independently by Taktical Application and Knowledge Solutions, LLC. UASReady does NOT represent, and is NOT affiliated with, endorsed by, authorized by, or sponsored by the Federal Aviation Administration (FAA), the National Oceanic and Atmospheric Administration (NOAA), or any other United States government agency. This application is an independent commercial flight-planning aid and does NOT provide or constitute an official government service.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextPrimary,
                                                fontSize = 11.sp,
                                                lineHeight = 16.sp
                                            )
                                        )
                                    }
                                }

                                // 2. Official Government Data Sources (.gov)
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationAccent)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "OFFICIAL GOVERNMENT DATA SOURCES",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = AviationAccent,
                                                fontSize = 12.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                        Text(
                                            text = "UASReady references publicly accessible open data feeds from official United States government agencies:",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                                        )

                                        HorizontalDivider(color = AviationDarkBorder)

                                        // Source 1: FAA Aeronautical Data
                                        GovernmentSourceItem(
                                            title = "FAA Aeronautical Information Services",
                                            subtitle = "Official FAA 28-day aeronautical chart & airspace data",
                                            url = "https://www.faa.gov/air_traffic/flight_info/aeronav/aero_data/",
                                            onOpen = { uriHandler.openUri("https://www.faa.gov/air_traffic/flight_info/aeronav/aero_data/") }
                                        )

                                        // Source 2: FAA Open GIS Feeds
                                        GovernmentSourceItem(
                                            title = "FAA Open Data GIS (Class Airspace & SUA)",
                                            subtitle = "Live ArcGIS feeds for Class B/C/D/E airspace and SUA geometries",
                                            url = "https://ais-faa.opendata.arcgis.com/",
                                            onOpen = { uriHandler.openUri("https://ais-faa.opendata.arcgis.com/") }
                                        )

                                        // Source 3: FAA Part 107 Regulations
                                        GovernmentSourceItem(
                                            title = "FAA 14 CFR Part 107 Small UAS Regulations",
                                            subtitle = "Commercial drone operating requirements and rules",
                                            url = "https://www.faa.gov/uas/commercial_operators",
                                            onOpen = { uriHandler.openUri("https://www.faa.gov/uas/commercial_operators") }
                                        )

                                        // Source 4: NOAA SWPC
                                        GovernmentSourceItem(
                                            title = "NOAA Space Weather Prediction Center (SWPC)",
                                            subtitle = "Planetary Kp-index and geomagnetic storm monitoring",
                                            url = "https://www.swpc.noaa.gov/",
                                            onOpen = { uriHandler.openUri("https://www.swpc.noaa.gov/") }
                                        )

                                        // Source 5: NWS
                                        GovernmentSourceItem(
                                            title = "National Weather Service (NWS / NOAA)",
                                            subtitle = "Surface observation data and aviation weather forecasts",
                                            url = "https://www.weather.gov/",
                                            onOpen = { uriHandler.openUri("https://www.weather.gov/") }
                                        )
                                    }
                                }

                                // 3. Third-Party / Open Data Providers
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationDarkBorder)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "ADDITIONAL OPEN DATA PROVIDERS",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        )
                                        Text(
                                            text = "• Weather & Elevation DEM: Open-Meteo SRTM 90m & Copernicus DEM (https://open-meteo.com/)",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                                        )
                                        Text(
                                            text = "• Airfield Navigation & Runway Geometries: FAA 5010 Public Data via OurAirports open database",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                                        )
                                    }
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

@Composable
private fun GovernmentSourceItem(
    title: String,
    subtitle: String,
    url: String,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AviationDarkSurface)
            .clickable(onClick = onOpen)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 11.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 9.5.sp
                )
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = AviationCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            Icons.Default.OpenInNew,
            contentDescription = "Open $title",
            tint = AviationAccent,
            modifier = Modifier.size(18.dp)
        )
    }
}

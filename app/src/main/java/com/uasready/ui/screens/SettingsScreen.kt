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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uasready.domain.model.PilotAuthorityType
import com.uasready.ui.theme.*
import com.uasready.ui.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onSetAuthority: (PilotAuthorityType) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMetric by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS & PREFERENCES",
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
            // 1. Pilot Operating Authority (Part 107 vs Public COA)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PILOT OPERATING AUTHORITY",
                    style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationDarkBorder)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Select operating authority profile to configure regulatory flight permissions:",
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
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = AviationAccent.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(color = AviationAccent, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = authority.description,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = TextSecondary)
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSetAuthority(authority) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = AviationAccent,
                                        unselectedColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Unit System
            item {
                Text(
                    text = "UNIT SYSTEM",
                    style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationDarkBorder)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isMetric) "Metric System (m/s, °C, m, km)" else "US Aviation Standard (MPH, °F, ft, SM)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                )
                                Text(
                                    text = if (isMetric) "International standard metric units" else "Standard FAA aeronautical telemetry",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                )
                            }
                            Switch(
                                checked = isMetric,
                                onCheckedChange = { isMetric = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = AviationAccent,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = AviationDarkSurface
                                )
                            )
                        }
                    }
                }
            }

            // 3. Authoritative Data Sources
            item {
                Text(
                    text = "AUTHORITATIVE TELEMETRY SOURCES",
                    style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationDarkBorder)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• Weather & Forecast: Open-Meteo & NOAA National Weather Service", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                        Text("• Space Weather & GNSS: NOAA SWPC Planetary K-Index Feed", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                        Text("• Terrain Elevation DEM: Open-Meteo 90m SRTM / Copernicus Digital Elevation", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                        Text("• Solar Ephemeris: NOAA Astronomical Solar Geometry Algorithm", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                        Text("• Airspace: FAA Aeronautical Information Services (AIS)", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                    }
                }
            }

            // 4. App Info Footer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("UASReady Preflight Decision Support", style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                        Text("Deterministic Safety Assessment Engine", style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, fontSize = 11.sp))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

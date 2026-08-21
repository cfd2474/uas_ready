package com.uasready.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.uasready.domain.model.Aircraft
import com.uasready.domain.model.AircraftLimitations
import com.uasready.ui.theme.*
import com.uasready.ui.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AircraftScreen(
    uiState: MainUiState,
    onSelectAircraft: (String) -> Unit,
    onSaveCustomAircraft: (Aircraft) -> Unit,
    onDeleteCustomAircraft: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customMaxWind by remember { mutableStateOf("27.0") }
    var customMaxGust by remember { mutableStateOf("34.0") }
    var customMinTemp by remember { mutableStateOf("14.0") }
    var customMaxTemp by remember { mutableStateOf("104.0") }
    var customPrecipAllowed by remember { mutableStateOf(false) }

    val selected = uiState.selectedAircraft

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FLEET & AIRCRAFT",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            customName = "${selected.displayName} (Custom)"
                            customMaxWind = selected.limitations.maxSustainedWindSpeedMph.toString()
                            customMaxGust = selected.limitations.maxGustSpeedMph.toString()
                            customMinTemp = selected.limitations.minOperatingTempF.toString()
                            customMaxTemp = selected.limitations.maxOperatingTempF.toString()
                            customPrecipAllowed = selected.limitations.precipitationAllowed
                            showCustomDialog = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AviationCyan,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CLONE CUSTOM", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
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
            // Selected Active Aircraft Highlight
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AviationDarkCard)
                        .border(2.dp, AviationCyan, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = AviationCyan, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ACTIVE FLIGHT AIRCRAFT",
                                    style = MaterialTheme.typography.labelMedium.copy(color = AviationAccent, fontWeight = FontWeight.Bold)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SafetyGoBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("SELECTED", style = MaterialTheme.typography.labelMedium.copy(color = SafetyGoLight, fontWeight = FontWeight.Bold))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selected.displayName,
                            style = MaterialTheme.typography.headlineMedium.copy(color = TextPrimary, fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = "Manufacturer: ${selected.manufacturer} • Fleet: ${selected.organization}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = AviationDarkBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Operating Envelope Grid
                        Text(
                            text = "OPERATING ENVELOPE & CERTIFIED LIMITS",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("MAX SUSTAINED WIND", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 10.sp))
                                Text("${selected.limitations.maxSustainedWindSpeedMph.toInt()} MPH", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("MAX WIND GUST", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 10.sp))
                                Text("${selected.limitations.maxGustSpeedMph.toInt()} MPH", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TEMP ENVELOPE", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 10.sp))
                                Text("${selected.limitations.minOperatingTempF.toInt()}°F to ${selected.limitations.maxOperatingTempF.toInt()}°F", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("WATER / IP RATING", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 10.sp))
                                Text(if (selected.limitations.precipitationAllowed) selected.limitations.ipRating else "None (No Rain)", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("MAX TAKEOFF MSL", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 10.sp))
                                Text("${selected.limitations.maxTakeoffAltitudeMslFt.toInt()} ft", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("KP TOLERANCE", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 10.sp))
                                Text("Kp <= ${selected.limitations.maxKpIndexTolerance}", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // Fleet Preset Selector
            item {
                Text(
                    text = "COMMERCIAL FLEET PRESETS",
                    style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(uiState.allAircraft) { drone ->
                val isSelected = drone.id == selected.id
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AviationDarkCard else AviationDarkSurface)
                        .border(1.dp, if (isSelected) AviationCyan else AviationDarkBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelectAircraft(drone.id) }
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = drone.displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = if (isSelected) AviationAccent else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (drone.isCustom) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AviationDarkCard)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("CUSTOM", style = MaterialTheme.typography.labelMedium.copy(color = AviationAccent, fontSize = 9.sp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Max Gust: ${drone.limitations.maxGustSpeedMph.toInt()} MPH • Temp: ${drone.limitations.minOperatingTempF.toInt()}°F to ${drone.limitations.maxOperatingTempF.toInt()}°F • ${drone.limitations.ipRating}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }

                        if (drone.isCustom) {
                            IconButton(onClick = { onDeleteCustomAircraft(drone.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SafetyNoGoLight)
                            }
                        } else if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = AviationCyan)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Custom Aircraft Dialog
    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Create Custom Aircraft Profile", style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Profile / Unit Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customMaxWind,
                        onValueChange = { customMaxWind = it },
                        label = { Text("Max Sustained Wind (MPH)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customMaxGust,
                        onValueChange = { customMaxGust = it },
                        label = { Text("Max Wind Gust (MPH)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customMinTemp,
                        onValueChange = { customMinTemp = it },
                        label = { Text("Min Operating Temp (°F)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customMaxTemp,
                        onValueChange = { customMaxTemp = it },
                        label = { Text("Max Operating Temp (°F)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newCustom = Aircraft(
                            id = "custom_${System.currentTimeMillis()}",
                            manufacturer = selected.manufacturer,
                            model = customName,
                            displayName = customName,
                            isCustom = true,
                            basePresetId = selected.id,
                            limitations = selected.limitations.copy(
                                maxSustainedWindSpeedMph = customMaxWind.toDoubleOrNull() ?: 27.0,
                                maxGustSpeedMph = customMaxGust.toDoubleOrNull() ?: 34.0,
                                minOperatingTempF = customMinTemp.toDoubleOrNull() ?: 14.0,
                                maxOperatingTempF = customMaxTemp.toDoubleOrNull() ?: 104.0,
                                precipitationAllowed = customPrecipAllowed
                            )
                        )
                        onSaveCustomAircraft(newCustom)
                        showCustomDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AviationCyan)
                ) {
                    Text("SAVE TO FLEET")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = AviationDarkCard
        )
    }
}

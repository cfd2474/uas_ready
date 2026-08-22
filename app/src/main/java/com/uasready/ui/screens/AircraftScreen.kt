package com.uasready.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    // Filter state: List remains blank until a manufacturer is explicitly chosen
    var selectedManufacturer by remember { mutableStateOf<String?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val manufacturerList = listOf(
        "DJI",
        "Autel Robotics",
        "Skydio",
        "Parrot",
        "Custom Profiles"
    )

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            customName = "${selected.displayName} (Custom)"
                            customMaxWind = selected.limitations.maxSustainedWindSpeedMph.toString()
                            customMaxGust = selected.limitations.maxGustSpeedMph.toString()
                            customMinTemp = selected.limitations.minOperatingTempF.toString()
                            customMaxTemp = selected.limitations.maxOperatingTempF.toString()
                            customPrecipAllowed = selected.limitations.precipitationAllowed
                            showCustomDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AviationCyan,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BUILD CUSTOM", color = Color.White, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
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
            // Selected Active Aircraft Highlight Card
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
                                Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = AviationCyan, modifier = Modifier.size(22.dp))
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
                                Text("SELECTED", style = MaterialTheme.typography.labelMedium.copy(color = SafetyGoLight, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = selected.displayName,
                            style = MaterialTheme.typography.headlineSmall.copy(color = TextPrimary, fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = "Manufacturer: ${selected.manufacturer} • Fleet: ${selected.organization}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = AviationDarkBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Operating Envelope Grid
                        Text(
                            text = "OPERATING ENVELOPE & CERTIFIED LIMITS",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("MAX SUSTAINED WIND", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 9.sp))
                                Text("${selected.limitations.maxSustainedWindSpeedMph.toInt()} MPH", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("MAX WIND GUST", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 9.sp))
                                Text("${selected.limitations.maxGustSpeedMph.toInt()} MPH", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TEMP ENVELOPE", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 9.sp))
                                Text("${selected.limitations.minOperatingTempF.toInt()}°F to ${selected.limitations.maxOperatingTempF.toInt()}°F", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("WATER / IP RATING", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 9.sp))
                                Text(if (selected.limitations.precipitationAllowed) selected.limitations.ipRating else "None (No Rain)", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("MAX TAKEOFF MSL", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 9.sp))
                                Text("${selected.limitations.maxTakeoffAltitudeMslFt.toInt()} ft", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("KP TOLERANCE", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 9.sp))
                                Text("Kp <= ${selected.limitations.maxKpIndexTolerance}", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // Manufacturer Filter Dropdown Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SELECT MANUFACTURER TO VIEW MODELS",
                        style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedManufacturer ?: "— Select a Manufacturer —",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AviationDarkCard,
                                unfocusedContainerColor = AviationDarkCard,
                                focusedBorderColor = AviationCyan,
                                unfocusedBorderColor = AviationDarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = if (selectedManufacturer == null) TextSecondary else TextPrimary
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(AviationDarkCard)
                        ) {
                            manufacturerList.forEach { mfg ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (mfg == "DJI") "DJI Enterprise" else mfg,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = if (selectedManufacturer == mfg) AviationAccent else TextPrimary,
                                                fontWeight = if (selectedManufacturer == mfg) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    },
                                    onClick = {
                                        selectedManufacturer = mfg
                                        dropdownExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }
            }

            // Filtered Models List
            if (selectedManufacturer == null) {
                // Blank state until manufacturer is selected
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AviationDarkCard.copy(alpha = 0.5f))
                            .border(1.dp, AviationDarkBorder, RoundedCornerShape(10.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "NO MANUFACTURER SELECTED",
                                style = MaterialTheme.typography.titleMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Select a manufacturer from the dropdown above to view certified enterprise models.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }
                    }
                }
            } else {
                val filteredModels = uiState.allAircraft.filter { drone ->
                    when (selectedManufacturer) {
                        "Custom Profiles" -> drone.isCustom
                        "DJI" -> drone.manufacturer.contains("DJI", ignoreCase = true) && !drone.isCustom
                        "Autel Robotics" -> drone.manufacturer.contains("Autel", ignoreCase = true) && !drone.isCustom
                        "Skydio" -> drone.manufacturer.contains("Skydio", ignoreCase = true) && !drone.isCustom
                        "Parrot" -> drone.manufacturer.contains("Parrot", ignoreCase = true) && !drone.isCustom
                        else -> false
                    }
                }

                if (filteredModels.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AviationDarkCard)
                                .border(1.dp, AviationDarkBorder, RoundedCornerShape(10.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No aircraft profiles found for $selectedManufacturer.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    }
                } else {
                    items(filteredModels) { drone ->
                        val isSelected = drone.id == selected.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AviationDarkCard else AviationDarkSurface)
                                .border(1.5.dp, if (isSelected) AviationCyan else AviationDarkBorder, RoundedCornerShape(10.dp))
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
                                        text = "Max Gust: ${drone.limitations.maxGustSpeedMph.toInt()} MPH • Temp: ${drone.limitations.minOperatingTempF.toInt()}°F to ${drone.limitations.maxOperatingTempF.toInt()}°F • ${drone.limitations.ipRating} • Max Alt: ${drone.limitations.maxTakeoffAltitudeMslFt.toInt()} ft MSL",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                    if (drone.limitations.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = drone.limitations.notes,
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
                                        )
                                    }
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
                    colors = ButtonDefaults.buttonColors(containerColor = AviationCyan, contentColor = Color.White)
                ) {
                    Text("SAVE TO FLEET", color = Color.White, fontWeight = FontWeight.Bold)
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

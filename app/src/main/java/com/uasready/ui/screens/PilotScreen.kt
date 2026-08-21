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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uasready.domain.model.PilotAuthorityType
import com.uasready.ui.theme.*
import com.uasready.ui.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilotScreen(
    uiState: MainUiState,
    onSetAuthority: (PilotAuthorityType) -> Unit,
    onSetNightEndorsement: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pilot = uiState.currentPilot

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PILOT & CREDENTIALS",
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
            // Pilot Header Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(AviationDarkSurface)
                                .border(1.dp, AviationAccent, RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = pilot.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = TextPrimary)
                            )
                            Text(
                                text = "Operating Authority: ${pilot.activeAuthority.name.replace("_", " ")}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = AviationAccent)
                            )
                        }
                    }
                }
            }

            // Operating Authority Switcher
            item {
                Text(
                    text = "OPERATING AUTHORITY FRAMEWORK",
                    style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Part 107 Button
                    val isPart107 = pilot.activeAuthority == PilotAuthorityType.PART_107
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isPart107) AviationDarkCard else AviationDarkSurface)
                            .border(2.dp, if (isPart107) AviationCyan else AviationDarkBorder, RoundedCornerShape(10.dp))
                            .clickable { onSetAuthority(PilotAuthorityType.PART_107) }
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("FAA PART 107", style = MaterialTheme.typography.titleMedium.copy(color = if (isPart107) AviationAccent else TextPrimary, fontWeight = FontWeight.Bold))
                                if (isPart107) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = AviationCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Standard commercial & public safety framework (14 CFR Part 107)", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, color = TextSecondary))
                        }
                    }

                    // COA / COW Button
                    val isCoa = pilot.activeAuthority == PilotAuthorityType.COA_COW
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCoa) AviationDarkCard else AviationDarkSurface)
                            .border(2.dp, if (isCoa) AviationCyan else AviationDarkBorder, RoundedCornerShape(10.dp))
                            .clickable { onSetAuthority(PilotAuthorityType.COA_COW) }
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("COA / COW", style = MaterialTheme.typography.titleMedium.copy(color = if (isCoa) AviationAccent else TextPrimary, fontWeight = FontWeight.Bold))
                                if (isCoa) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = AviationCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Public Aircraft Operations (PAO) agency certificate authorization", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, color = TextSecondary))
                        }
                    }
                }
            }

            // Framework Details
            if (pilot.activeAuthority == PilotAuthorityType.PART_107) {
                item {
                    Text(
                        text = "PART 107 CREDENTIALS & CURRENCY",
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
                                Column {
                                    Text("Remote Pilot Certificate", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                                    Text(pilot.part107Profile.certificateNumber, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SafetyGoBg)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("CURRENT", style = MaterialTheme.typography.labelMedium.copy(color = SafetyGoLight, fontWeight = FontWeight.Bold))
                                }
                            }

                            HorizontalDivider(color = AviationDarkBorder)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("14 CFR § 107.29 Night Endorsement", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                                    Text("Completed recurrent night flight knowledge training", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                                }
                                Switch(
                                    checked = pilot.part107Profile.nightTrainingCompleted,
                                    onCheckedChange = { onSetNightEndorsement(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TextPrimary,
                                        checkedTrackColor = SafetyGoLight,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = AviationDarkSurface
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "PUBLIC AGENCY COA DETAILS",
                        style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationDarkBorder)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Jurisdiction: ${pilot.coaCowProfile.agencyName}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                            Text("COA Number: ${pilot.coaCowProfile.coaNumber}", style = MaterialTheme.typography.bodyMedium.copy(color = AviationAccent))
                            Text("Authorized Altitude: Up to ${pilot.coaCowProfile.maxAltitudeAuthorizedFt.toInt()} ft AGL", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                            Text("Night Flight Authorization: ${if (pilot.coaCowProfile.nightFlightAuthorized) "APPROVED" else "RESTRICTED"}", style = MaterialTheme.typography.bodyMedium.copy(color = if (pilot.coaCowProfile.nightFlightAuthorized) SafetyGoLight else SafetyNoGoLight, fontWeight = FontWeight.Bold))
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

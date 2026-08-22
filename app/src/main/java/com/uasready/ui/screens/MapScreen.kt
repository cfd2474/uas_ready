package com.uasready.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.uasready.domain.model.LocationInfo
import com.uasready.ui.theme.*
import com.uasready.ui.viewmodel.MainUiState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    uiState: MainUiState,
    onLocationChanged: (LocationInfo) -> Unit,
    onRefreshGpsLocation: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLayerMenu by remember { mutableStateOf(false) }
    var showAirspaceLayer by remember { mutableStateOf(true) }
    var showAirportBuffers by remember { mutableStateOf(true) }

    val loc = uiState.currentLocation

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AVIATION MAP",
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
                    IconButton(onClick = { showLayerMenu = !showLayerMenu }) {
                        Icon(Icons.Default.Layers, contentDescription = "Map Layers", tint = AviationAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AviationDarkBackground)
            )
        },
        containerColor = AviationDarkBackground
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Interactive OpenStreetMap View
            AndroidView(
                factory = { context ->
                    Configuration.getInstance().userAgentValue = "UASReady-Android-App/1.0"
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(13.5)
                        val startPoint = GeoPoint(loc.latitude, loc.longitude)
                        controller.setCenter(startPoint)

                        val marker = Marker(this)
                        marker.position = startPoint
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = loc.displayName
                        marker.snippet = loc.formattedCoordinates
                        overlays.add(marker)
                    }
                },
                update = { mapView ->
                    val point = GeoPoint(loc.latitude, loc.longitude)
                    mapView.controller.setCenter(point)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Layer Controls Overlay Card
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                if (showLayerMenu) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AviationDarkCard.copy(alpha = 0.95f))
                            .border(1.dp, AviationDarkBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("ACTIVE MAP OVERLAYS", style = MaterialTheme.typography.labelLarge.copy(color = AviationAccent, fontWeight = FontWeight.Bold))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("FAA Airspace & TFR Rings", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                                Switch(checked = showAirspaceLayer, onCheckedChange = { showAirspaceLayer = it })
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Airport Approach Buffers (5 NM)", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                                Switch(checked = showAirportBuffers, onCheckedChange = { showAirportBuffers = it })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Bottom Location & Coordinates Info Sheet
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AviationDarkCard.copy(alpha = 0.95f))
                    .border(1.5.dp, AviationDarkBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = loc.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AviationDarkSurface)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("GPS ACCURACY: ±${loc.accuracyMeters.toInt()}m", style = MaterialTheme.typography.labelMedium.copy(color = AviationAccent, fontSize = 10.sp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("COORDINATES", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 10.sp))
                            Text(loc.formattedCoordinates, style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
                        }
                        Column {
                            Text("ELEVATION", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 10.sp))
                            Text("${loc.elevationFt.toInt()} ft MSL", style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
                        }
                        Column {
                            Text("GNSS SATS", style = MaterialTheme.typography.labelMedium.copy(color = TextMuted, fontSize = 10.sp))
                            val satsText = if (uiState.estimatedGnss != null) "~${uiState.estimatedGnss.lockedSatellitesCount} (HDOP ${uiState.estimatedGnss.estimatedHdop})" else "12+ Visible"
                            Text(satsText, style = MaterialTheme.typography.bodyLarge.copy(color = AviationAccent, fontWeight = FontWeight.SemiBold))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRefreshGpsLocation,
                            colors = ButtonDefaults.buttonColors(containerColor = AviationCyan),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CURRENT GPS", color = AviationDarkBackground, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onLocationChanged(LocationInfo.defaultLocation()) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RESET HQ", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

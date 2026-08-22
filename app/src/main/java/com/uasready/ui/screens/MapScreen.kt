package com.uasready.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.viewinterop.AndroidView
import com.uasready.domain.model.AirspaceZone
import com.uasready.domain.model.AirspaceZoneType
import com.uasready.domain.model.LocationInfo
import com.uasready.ui.theme.*
import com.uasready.ui.viewmodel.MainUiState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Composable
fun MapScreen(
    uiState: MainUiState,
    onRefreshGpsLocation: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLegend by remember { mutableStateOf(true) }

    val loc = uiState.currentLocation
    val gnss = uiState.estimatedGnss
    val liveAirspaceZones: List<AirspaceZone> = uiState.airspaceInfo?.zones ?: emptyList()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AviationDarkBackground)
    ) {
        // Interactive OpenStreetMap View with Live openAIP Aeronautical Overlays
        AndroidView(
            factory = { context ->
                Configuration.getInstance().userAgentValue = "UASReady-Android-App/1.0"
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(12.8)
                    val startPoint = GeoPoint(loc.latitude, loc.longitude)
                    controller.setCenter(startPoint)

                    // User Launch Point Marker
                    val userMarker = Marker(this).apply {
                        id = "USER_MARKER"
                        position = startPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Launch Point: ${loc.displayName}"
                        snippet = "Coordinates: ${loc.formattedCoordinates}"
                    }
                    overlays.add(userMarker)
                }
            },
            update = { mapView ->
                val point = GeoPoint(loc.latitude, loc.longitude)
                mapView.controller.setCenter(point)

                // Remove previous airspace polygons to avoid any persisting old elements
                mapView.overlays.removeAll { it is Polygon }

                // Render strictly live openAIP airspace zones
                liveAirspaceZones.forEach { zone ->
                    val circlePoints = Polygon.pointsAsCircle(GeoPoint(zone.centerLat, zone.centerLon), zone.radiusMeters)
                    val polygon = Polygon(mapView).apply {
                        points = circlePoints
                        title = zone.name
                        snippet = zone.description
                        when (zone.type) {
                            AirspaceZoneType.RESTRICTED_ZONE -> {
                                fillPaint.color = android.graphics.Color.argb(70, 218, 54, 51) // Red fill
                                outlinePaint.color = android.graphics.Color.argb(255, 218, 54, 51) // Red stroke
                                outlinePaint.strokeWidth = 3f
                            }
                            AirspaceZoneType.AUTHORIZATION_ZONE -> {
                                fillPaint.color = android.graphics.Color.argb(55, 56, 139, 253) // Blue fill
                                outlinePaint.color = android.graphics.Color.argb(255, 56, 139, 253) // Blue stroke
                                outlinePaint.strokeWidth = 2.5f
                            }
                            AirspaceZoneType.WARNING_ZONE -> {
                                fillPaint.color = android.graphics.Color.argb(55, 227, 179, 65) // Amber fill
                                outlinePaint.color = android.graphics.Color.argb(255, 227, 179, 65) // Amber stroke
                                outlinePaint.strokeWidth = 2.5f
                            }
                            AirspaceZoneType.ALTITUDE_ZONE -> {
                                fillPaint.color = android.graphics.Color.argb(35, 0, 210, 255) // Cyan fill
                                outlinePaint.color = android.graphics.Color.argb(230, 0, 210, 255) // Cyan stroke
                                outlinePaint.strokeWidth = 2f
                            }
                            AirspaceZoneType.SPECIAL_USE -> {
                                fillPaint.color = android.graphics.Color.argb(50, 255, 140, 0)
                                outlinePaint.color = android.graphics.Color.argb(255, 255, 140, 0)
                                outlinePaint.strokeWidth = 2f
                            }
                        }
                    }
                    mapView.overlays.add(polygon)
                }

                // Refresh map invalidation
                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Floating Map Sub-Bar (Legend toggle + Source label)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = AviationDarkCard.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder)
            ) {
                Text(
                    text = "OPENAIP AIRSPACE DATA",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AviationAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }

            IconButton(
                onClick = { showLegend = !showLegend },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AviationDarkCard.copy(alpha = 0.92f))
                    .border(1.dp, AviationDarkBorder, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "Toggle Legend",
                    tint = if (showLegend) AviationAccent else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Floating openAIP Airspace Legend (Top-Right)
        if (showLegend) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 50.dp, end = 10.dp)
                    .widthIn(max = 230.dp),
                colors = CardDefaults.cardColors(containerColor = AviationDarkCard.copy(alpha = 0.95f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationDarkBorder)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "AIRSPACE CLASSIFICATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 9.sp
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SafetyNoGo))
                        Text("Restricted / Danger / Prohibited (Red)", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 10.sp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AviationCyan))
                        Text("Controlled / CTR / TMA (Blue)", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 10.sp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SafetyCautionLight))
                        Text("Warning / Class E / TMZ (Amber)", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 10.sp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF00D2FF)))
                        Text("Altitude / Facility Zone (Cyan)", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 10.sp))
                    }
                }
            }
        }

        // Bottom Telemetry Bar (Optimized for RC Pro Enterprise 640x360 landscape)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AviationDarkCard.copy(alpha = 0.95f))
                .border(1.dp, AviationDarkBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = loc.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Text(
                        text = "${loc.formattedCoordinates} • ${loc.elevationFt.toInt()} ft MSL",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp)
                    )
                }

                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (gnss != null) "~${gnss.lockedSatellitesCount} Sats Visible" else "12+ Sats Visible",
                        style = MaterialTheme.typography.bodySmall.copy(color = AviationAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    )
                    Text(
                        text = if (gnss != null) "HDOP ${gnss.estimatedHdop} • 3D Fix" else "HDOP <= 1.5",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onRefreshGpsLocation,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AviationDarkSurface)
                        .border(1.dp, AviationDarkBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Center GPS", tint = AviationAccent, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

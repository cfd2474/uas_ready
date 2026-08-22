package com.uasready.ui.screens

import android.graphics.Paint
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    uiState: MainUiState,
    onLocationChanged: (LocationInfo) -> Unit,
    onRefreshGpsLocation: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLegend by remember { mutableStateOf(true) }
    var selectedZoneInfo by remember { mutableStateOf<String?>(null) }

    val loc = uiState.currentLocation
    val gnss = uiState.estimatedGnss

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AIRSPACE & FLYSAFE MAP",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AviationDarkCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder)
                        ) {
                            Text(
                                text = "FAA // FLYSAFE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AviationAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showLegend = !showLegend }) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = "Toggle Legend",
                            tint = if (showLegend) AviationAccent else TextSecondary
                        )
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
            // Interactive OpenStreetMap View with DJI FlySafe Overlays
            AndroidView(
                factory = { context ->
                    Configuration.getInstance().userAgentValue = "UASReady-Android-App/1.0"
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(12.8)
                        val startPoint = GeoPoint(loc.latitude, loc.longitude)
                        controller.setCenter(startPoint)

                        // 1. Render DJI FlySafe Airspace Zones
                        val sampleZones = listOf(
                            AirspaceZone(
                                id = "KAJO-CORE",
                                name = "KAJO Class D Core (0 ft Auto-Approval)",
                                type = AirspaceZoneType.AUTHORIZATION_ZONE,
                                centerLat = 33.8977,
                                centerLon = -117.6033,
                                radiusMeters = 2200.0,
                                floorFt = 0.0,
                                ceilingFt = 0.0,
                                description = "Class D Surface Area. Mandatory LAANC Authorization Required."
                            ),
                            AirspaceZone(
                                id = "KAJO-RING",
                                name = "KAJO 200 ft LAANC Zone",
                                type = AirspaceZoneType.ALTITUDE_ZONE,
                                centerLat = 33.8977,
                                centerLon = -117.6033,
                                radiusMeters = 5500.0,
                                floorFt = 0.0,
                                ceilingFt = 200.0,
                                description = "UAS Facility Map grid. Max auto-approved altitude 200 ft AGL."
                            ),
                            AirspaceZone(
                                id = "KONT-CORE",
                                name = "KONT Class C Surface Area",
                                type = AirspaceZoneType.AUTHORIZATION_ZONE,
                                centerLat = 34.0560,
                                centerLon = -117.6012,
                                radiusMeters = 4800.0,
                                floorFt = 0.0,
                                ceilingFt = 0.0,
                                description = "Major Commercial Airport. Class C Surface. LAANC Mandatory."
                            ),
                            AirspaceZone(
                                id = "KONT-ALT",
                                name = "KONT 100 ft Altitude Zone",
                                type = AirspaceZoneType.ALTITUDE_ZONE,
                                centerLat = 34.0560,
                                centerLon = -117.6012,
                                radiusMeters = 9260.0,
                                floorFt = 0.0,
                                ceilingFt = 100.0,
                                description = "Class C Outer Ring. Max auto-approved altitude 100 ft AGL."
                            ),
                            AirspaceZone(
                                id = "KCNO-CORE",
                                name = "KCNO Chino Class D",
                                type = AirspaceZoneType.AUTHORIZATION_ZONE,
                                centerLat = 33.9748,
                                centerLon = -117.6366,
                                radiusMeters = 4200.0,
                                floorFt = 0.0,
                                ceilingFt = 200.0,
                                description = "Chino Airport Class D airspace. LAANC Required."
                            ),
                            AirspaceZone(
                                id = "KRAL-CORE",
                                name = "KRAL Riverside Class D",
                                type = AirspaceZoneType.AUTHORIZATION_ZONE,
                                centerLat = 33.9519,
                                centerLon = -117.4451,
                                radiusMeters = 4200.0,
                                floorFt = 0.0,
                                ceilingFt = 200.0,
                                description = "Riverside Municipal Class D. LAANC Required."
                            ),
                            AirspaceZone(
                                id = "PRADO-WARN",
                                name = "Prado Basin Warning Area",
                                type = AirspaceZoneType.WARNING_ZONE,
                                centerLat = 33.9050,
                                centerLon = -117.6250,
                                radiusMeters = 3200.0,
                                floorFt = 0.0,
                                ceilingFt = 2000.0,
                                description = "Enhanced Warning Zone: Heightened bird activity and low-flying aircraft."
                            )
                        )

                        sampleZones.forEach { zone ->
                            val circlePoints = Polygon.pointsAsCircle(GeoPoint(zone.centerLat, zone.centerLon), zone.radiusMeters)
                            val polygon = Polygon(this).apply {
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
                            overlays.add(polygon)
                        }

                        // 2. User Launch Point Marker
                        val userMarker = Marker(this).apply {
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
                },
                modifier = Modifier.fillMaxSize()
            )

            // Floating DJI FlySafe Legend (Top-Right / Thumb accessible)
            if (showLegend) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .widthIn(max = 240.dp),
                    colors = CardDefaults.cardColors(containerColor = AviationDarkCard.copy(alpha = 0.94f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationDarkBorder)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "DJI FLYSAFE AIRSPACE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 10.sp
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(SafetyNoGo))
                            Text("Restricted Zone / TFR (Red)", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 11.sp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(AviationCyan))
                            Text("Authorization Zone (Blue)", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 11.sp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(SafetyCautionLight))
                            Text("Warning / Airport 5NM (Amber)", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 11.sp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF00D2FF)))
                            Text("Altitude Zone (Cyan)", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 11.sp))
                        }
                    }
                }
            }

            // Bottom Flight Safety Info Bar (Optimized for RC Pro Enterprise 640x360 landscape)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AviationDarkCard.copy(alpha = 0.95f))
                    .border(1.dp, AviationDarkBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(
                            text = loc.displayName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            text = "${loc.formattedCoordinates} • ${loc.elevationFt.toInt()} ft MSL",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (gnss != null) "~${gnss.lockedSatellitesCount} Sats Visible" else "12+ Sats Visible",
                            style = MaterialTheme.typography.bodyMedium.copy(color = AviationAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        )
                        Text(
                            text = if (gnss != null) "HDOP ${gnss.estimatedHdop} • 3D Fix" else "HDOP <= 1.5",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onRefreshGpsLocation,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AviationDarkSurface)
                            .border(1.dp, AviationDarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Center GPS", tint = AviationAccent)
                    }
                }
            }
        }
    }
}

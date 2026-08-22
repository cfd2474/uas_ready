package com.uasready.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

enum class BasemapType(val displayName: String) {
    STREET("Street"),
    TOPO("Topo"),
    HYBRID("Hybrid")
}

// Google Street NO-POIs
private val STREET_TILE_SOURCE = object : OnlineTileSourceBase(
    "GoogleStreetNoPOI",
    0, 21, 256, ".png",
    arrayOf(
        "https://mt0.google.com/vt/lyrs=m&",
        "https://mt1.google.com/vt/lyrs=m&",
        "https://mt2.google.com/vt/lyrs=m&",
        "https://mt3.google.com/vt/lyrs=m&"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "${baseUrl}x=$x&y=$y&z=$zoom&s=Gal&apistyle=s.t%3A2%7Cs.e%3Al%7Cp.v%3Aoff"
    }
}

// Google Terrain NO-POIs
private val TOPO_TILE_SOURCE = object : OnlineTileSourceBase(
    "GoogleTerrainNoPOI",
    0, 21, 256, ".png",
    arrayOf(
        "https://mt0.google.com/vt/lyrs=p&",
        "https://mt1.google.com/vt/lyrs=p&",
        "https://mt2.google.com/vt/lyrs=p&",
        "https://mt3.google.com/vt/lyrs=p&"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "${baseUrl}x=$x&y=$y&z=$zoom&s=Gal&apistyle=s.t%3A2%7Cs.e%3Al%7Cp.v%3Aoff"
    }
}

// Google Hybrid NO-POIs
private val HYBRID_TILE_SOURCE = object : OnlineTileSourceBase(
    "GoogleHybridNoPOI",
    0, 21, 256, ".png",
    arrayOf(
        "https://mt0.google.com/vt/lyrs=y&",
        "https://mt1.google.com/vt/lyrs=y&",
        "https://mt2.google.com/vt/lyrs=y&",
        "https://mt3.google.com/vt/lyrs=y&"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "${baseUrl}x=$x&y=$y&z=$zoom&s=Gal&apistyle=s.t%3A2%7Cs.e%3Al%7Cp.v%3Aoff"
    }
}

@Composable
fun MapScreen(
    uiState: MainUiState,
    onRefreshGpsLocation: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLegend by remember { mutableStateOf(false) }
    var selectedBasemap by remember { mutableStateOf(BasemapType.STREET) }

    val loc = uiState.currentLocation
    val gnss = uiState.estimatedGnss
    val liveAirspaceZones: List<AirspaceZone> = uiState.airspaceInfo?.zones ?: emptyList()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AviationDarkBackground)
    ) {
        // Interactive Map View with Live Aeronautical Overlays & Selectable NO-POI Basemaps
        AndroidView(
            factory = { context ->
                Configuration.getInstance().userAgentValue = "UASReady-Android-App/1.0"
                MapView(context).apply {
                    setTileSource(STREET_TILE_SOURCE)
                    setMultiTouchControls(true)
                    controller.setZoom(12.8)
                    val startPoint = GeoPoint(loc.latitude, loc.longitude)
                    controller.setCenter(startPoint)

                    // User Launch Point Marker (50% smaller Red Pin)
                    val userMarker = Marker(this).apply {
                        id = "USER_MARKER"
                        position = startPoint
                        icon = createSmallRedPinDrawable(context)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Launch Point: ${loc.displayName}"
                        snippet = "Coordinates: ${loc.formattedCoordinates}"
                    }
                    overlays.add(userMarker)
                }
            },
            update = { mapView ->
                // Apply Selected NO-POI Basemap Tile Source
                val targetTileSource = when (selectedBasemap) {
                    BasemapType.STREET -> STREET_TILE_SOURCE
                    BasemapType.TOPO -> TOPO_TILE_SOURCE
                    BasemapType.HYBRID -> HYBRID_TILE_SOURCE
                }
                if (mapView.tileProvider.tileSource.name() != targetTileSource.name()) {
                    mapView.setTileSource(targetTileSource)
                }

                val point = GeoPoint(loc.latitude, loc.longitude)
                mapView.controller.setCenter(point)

                // Update or reposition User Marker
                val userMarker = mapView.overlays.filterIsInstance<Marker>().firstOrNull { it.id == "USER_MARKER" }
                if (userMarker != null) {
                    userMarker.position = point
                    userMarker.title = "Launch Point: ${loc.displayName}"
                    userMarker.snippet = "Coordinates: ${loc.formattedCoordinates}"
                }

                // Remove previous airspace polygons to avoid duplicate stacking
                mapView.overlays.removeAll { it is Polygon }

                // Render live airspace zones in map view extent
                liveAirspaceZones.forEach { zone ->
                    val pointsList: List<GeoPoint> = if (zone.polygonCoordinates.size >= 3) {
                        zone.polygonCoordinates.map { GeoPoint(it.first, it.second) }
                    } else {
                        Polygon.pointsAsCircle(GeoPoint(zone.centerLat, zone.centerLon), zone.radiusMeters)
                    }

                    val polygon = Polygon(mapView).apply {
                        points = pointsList
                        title = zone.name
                        snippet = zone.description
                        when (zone.type) {
                            AirspaceZoneType.RESTRICTED_ZONE -> {
                                fillPaint.color = AndroidColor.argb(70, 218, 54, 51)
                                outlinePaint.color = AndroidColor.argb(255, 218, 54, 51)
                                outlinePaint.strokeWidth = 3.5f
                            }
                            AirspaceZoneType.AUTHORIZATION_ZONE -> {
                                fillPaint.color = AndroidColor.argb(55, 56, 139, 253)
                                outlinePaint.color = AndroidColor.argb(255, 56, 139, 253)
                                outlinePaint.strokeWidth = 3f
                            }
                            AirspaceZoneType.WARNING_ZONE -> {
                                fillPaint.color = AndroidColor.argb(55, 227, 179, 65)
                                outlinePaint.color = AndroidColor.argb(255, 227, 179, 65)
                                outlinePaint.strokeWidth = 3f
                            }
                            AirspaceZoneType.ALTITUDE_ZONE -> {
                                fillPaint.color = AndroidColor.argb(35, 0, 210, 255)
                                outlinePaint.color = AndroidColor.argb(230, 0, 210, 255)
                                outlinePaint.strokeWidth = 2.5f
                            }
                            AirspaceZoneType.SPECIAL_USE -> {
                                fillPaint.color = AndroidColor.argb(50, 255, 140, 0)
                                outlinePaint.color = AndroidColor.argb(255, 255, 140, 0)
                                outlinePaint.strokeWidth = 2.5f
                            }
                        }
                    }
                    mapView.overlays.add(polygon)
                }

                mapView.postInvalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Floating Control Bar (Basemap Selector + Legend Toggle)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Basemap Segmented Switcher (Street, Topo, Hybrid)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AviationDarkCard.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder)
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasemapType.values().forEach { mode ->
                        val isSelected = selectedBasemap == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) AviationCyan else Color.Transparent)
                                .clickable { selectedBasemap = mode }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = mode.displayName.uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }

            // Legend Toggle Button
            IconButton(
                onClick = { showLegend = !showLegend },
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AviationDarkCard.copy(alpha = 0.95f))
                    .border(1.dp, AviationDarkBorder, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "Toggle Legend",
                    tint = if (showLegend) AviationAccent else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Floating Airspace Classification Legend (Top-Right)
        if (showLegend) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 46.dp, end = 8.dp)
                    .widthIn(max = 220.dp),
                colors = CardDefaults.cardColors(containerColor = AviationDarkCard.copy(alpha = 0.95f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationDarkBorder)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SafetyNoGo))
                        Text("Restricted / Prohibited (Red)", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 10.sp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AviationCyan))
                        Text("Controlled / Class B, C, D (Blue)", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 10.sp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SafetyCautionLight))
                        Text("Warning / Class E Surface (Amber)", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 10.sp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00D2FF)))
                        Text("Facility / Altitude Grid (Cyan)", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 10.sp))
                    }
                }
            }
        }

        // Bottom Telemetry Bar (640x360 Landscape Optimized)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AviationDarkCard.copy(alpha = 0.95f))
                .border(1.dp, AviationDarkBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = loc.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                    )
                    Text(
                        text = "${loc.formattedCoordinates} • ${loc.elevationFt.toInt()} ft MSL",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 9.sp)
                    )
                }

                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (gnss != null) "~${gnss.lockedSatellitesCount} Sats Visible" else "12+ Sats Visible",
                        style = MaterialTheme.typography.bodySmall.copy(color = AviationAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    )
                    Text(
                        text = if (gnss != null) "HDOP ${gnss.estimatedHdop} • 3D Fix" else "HDOP <= 1.5",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 8.sp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onRefreshGpsLocation,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AviationDarkSurface)
                        .border(1.dp, AviationDarkBorder, RoundedCornerShape(6.dp))
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Center GPS", tint = AviationAccent, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// 50% Decreased Size Red Location Pin
private fun createSmallRedPinDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val width = (14 * density).toInt() // Reduced 50% from 28dp
    val height = (20 * density).toInt() // Reduced 50% from 40dp
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val radius = width / 2f
    val path = Path().apply {
        arcTo(0f, 0f, width.toFloat(), width.toFloat(), 180f, 180f, true)
        quadTo(width.toFloat(), width * 0.85f, width / 2f, height.toFloat())
        quadTo(0f, width * 0.85f, 0f, radius)
        close()
    }

    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(220, 38, 38) // Vivid Red
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(153, 27, 27)
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }

    canvas.drawPath(path, pinPaint)
    canvas.drawPath(path, borderPaint)
    canvas.drawCircle(width / 2f, radius, radius * 0.4f, dotPaint)

    return BitmapDrawable(context.resources, bitmap)
}

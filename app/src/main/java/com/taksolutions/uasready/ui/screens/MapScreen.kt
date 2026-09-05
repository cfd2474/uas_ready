package com.taksolutions.uasready.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.taksolutions.uasready.data.repository.AirportCtafResult
import com.taksolutions.uasready.domain.model.AirportWarningZone
import com.taksolutions.uasready.domain.model.AirspaceZone
import com.taksolutions.uasready.domain.model.AirspaceZoneType
import com.taksolutions.uasready.ui.theme.*
import com.taksolutions.uasready.ui.viewmodel.MainUiState
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import kotlin.math.*

enum class BasemapType(val displayName: String) {
    STREET("Street"),
    TOPO("Topo"),
    HYBRID("Hybrid")
}

data class AirspaceInspection(
    val point: GeoPoint,
    val zones: List<AirspaceZone>,
    val warningZones: List<AirportWarningZone> = emptyList()
)

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
    var inspectionResult by remember { mutableStateOf<AirspaceInspection?>(null) }
    var selectedAirport by remember { mutableStateOf<AirportCtafResult?>(null) }
    var showAirportsLayer by remember { mutableStateOf(true) }
    var showAirportWarningZones by remember { mutableStateOf(true) }
    var shouldRecenterMap by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    var enabledZoneTypes by remember {
        mutableStateOf(
            setOf(
                AirspaceZoneType.RESTRICTED_ZONE,
                AirspaceZoneType.AUTHORIZATION_ZONE,
                AirspaceZoneType.WARNING_ZONE,
                AirspaceZoneType.SPECIAL_USE
            )
        )
    }

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
                    setBuiltInZoomControls(false)
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
                    mapViewRef = this
                }
            },
            update = { mapView ->
                mapViewRef = mapView

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
                if (shouldRecenterMap) {
                    mapView.controller.animateTo(point)
                    shouldRecenterMap = false
                }

                // Update or reposition User Marker
                val userMarker = mapView.overlays.filterIsInstance<Marker>().firstOrNull { it.id == "USER_MARKER" }
                if (userMarker != null) {
                    userMarker.position = point
                    userMarker.title = "Launch Point: ${loc.displayName}"
                    userMarker.snippet = "Coordinates: ${loc.formattedCoordinates}"
                }

                // Clear previous airspace polygons, events, and airport markers
                mapView.overlays.removeAll { it is Polygon || it is MapEventsOverlay || (it is Marker && it.id != "USER_MARKER") }

                // Map Touch Receiver: Handles clicks across all overlapping polygons
                val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        selectedAirport = null
                        val activeZones = liveAirspaceZones.filter { it.type in enabledZoneTypes }
                        val overlapping = activeZones.filter { isPointInZone(p.latitude, p.longitude, it) }
                        val overlappingWarnings = if (showAirportWarningZones) {
                            uiState.airportWarningZones.filter { isPointInPolygon(p.latitude, p.longitude, it.polygonCoordinates) }
                        } else emptyList()
                        inspectionResult = AirspaceInspection(p, overlapping, overlappingWarnings)
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint): Boolean {
                        return false
                    }
                })
                mapView.overlays.add(0, mapEventsOverlay)

                // 1. Render 30 NM Airport Custom Warning Zones (DJI GEO 2.0 Bow-Tie & Buffer)
                if (showAirportWarningZones) {
                    uiState.airportWarningZones.forEach { zone ->
                        if (zone.polygonCoordinates.size >= 3) {
                            val pts = zone.polygonCoordinates.map { GeoPoint(it.first, it.second) }
                            val warningPolygon = Polygon(mapView).apply {
                                points = pts
                                title = "${zone.ident} - ${zone.name}"
                                snippet = if (zone.zoneType == "HIGH_RISK_BOWTIE" || zone.level == 3) {
                                    "High Risk Zone (Runway approach/departure corridor)"
                                } else {
                                    "Runway Buffer (3km proximity radius)"
                                }

                                if (zone.zoneType == "HIGH_RISK_BOWTIE" || zone.level == 3) {
                                    // High Risk Zone (Orange #EE8815)
                                    fillPaint.color = AndroidColor.argb(40, 238, 136, 21)
                                    outlinePaint.color = AndroidColor.argb(230, 238, 136, 21)
                                    outlinePaint.strokeWidth = 2.0f
                                } else {
                                    // Runway Buffer 3km (Yellow #FFCC00)
                                    fillPaint.color = AndroidColor.argb(25, 255, 204, 0)
                                    outlinePaint.color = AndroidColor.argb(200, 255, 204, 0)
                                    outlinePaint.strokeWidth = 1.5f
                                }

                                setOnClickListener { _, _, clickPoint ->
                                    selectedAirport = null
                                    val activeZones = liveAirspaceZones.filter { it.type in enabledZoneTypes }
                                    val overlapping = activeZones.filter { isPointInZone(clickPoint.latitude, clickPoint.longitude, it) }
                                    val overlappingWarnings = if (showAirportWarningZones) {
                                        uiState.airportWarningZones.filter { isPointInPolygon(clickPoint.latitude, clickPoint.longitude, it.polygonCoordinates) }
                                    } else emptyList()
                                    inspectionResult = AirspaceInspection(clickPoint, overlapping, overlappingWarnings)
                                    true
                                }
                            }
                            mapView.overlays.add(warningPolygon)
                        }
                    }
                }

                // 2. Filter and render live airspace zones based on active category toggles
                val filteredZones = liveAirspaceZones.filter { it.type in enabledZoneTypes }

                filteredZones.forEach { zone ->
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
                                fillPaint.color = AndroidColor.argb(45, 56, 139, 253)
                                outlinePaint.color = AndroidColor.argb(255, 56, 139, 253)
                                outlinePaint.strokeWidth = 3f
                            }
                            AirspaceZoneType.WARNING_ZONE -> {
                                fillPaint.color = AndroidColor.argb(45, 227, 179, 65)
                                outlinePaint.color = AndroidColor.argb(255, 227, 179, 65)
                                outlinePaint.strokeWidth = 2.5f
                            }
                            AirspaceZoneType.ALTITUDE_ZONE -> {
                                fillPaint.color = AndroidColor.argb(45, 56, 139, 253)
                                outlinePaint.color = AndroidColor.argb(255, 56, 139, 253)
                                outlinePaint.strokeWidth = 2.5f
                            }
                            AirspaceZoneType.SPECIAL_USE -> {
                                fillPaint.color = AndroidColor.argb(45, 255, 140, 0)
                                outlinePaint.color = AndroidColor.argb(255, 255, 140, 0)
                                outlinePaint.strokeWidth = 2.5f
                            }
                        }

                        // Forward polygon clicks to multi-layer inspection
                        setOnClickListener { _, _, clickPoint ->
                            selectedAirport = null
                            val activeZones = liveAirspaceZones.filter { it.type in enabledZoneTypes }
                            val overlapping = activeZones.filter { isPointInZone(clickPoint.latitude, clickPoint.longitude, it) }
                            val overlappingWarnings = if (showAirportWarningZones) {
                                uiState.airportWarningZones.filter { isPointInPolygon(clickPoint.latitude, clickPoint.longitude, it.polygonCoordinates) }
                            } else emptyList()
                            inspectionResult = AirspaceInspection(clickPoint, overlapping, overlappingWarnings)
                            true
                        }
                    }
                    mapView.overlays.add(polygon)
                }

                // Render 30 NM Airport & CTAF Frequency Markers (On top of polygons)
                if (showAirportsLayer) {
                    uiState.nearbyAirports.forEach { airport ->
                        val airportMarker = Marker(mapView).apply {
                            id = "AIRPORT_${airport.ident}"
                            position = GeoPoint(airport.lat, airport.lon)
                            title = "${airport.ident} - ${airport.name}"
                            snippet = "CTAF/FREQ: ${airport.frequencyMhz} MHz (${airport.type}) • ${String.format(java.util.Locale.US, "%.1f NM", airport.distanceNm)}"
                            icon = createAirportMarkerDrawable(mapView.context, airport.ident, airport.frequencyMhz)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            setOnMarkerClickListener { _, _ ->
                                inspectionResult = null
                                selectedAirport = airport
                                true
                            }
                        }
                        mapView.overlays.add(airportMarker)
                    }
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

        // Top-Center Airspace 30 NM Radius Disclaimer Banner
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp),
            shape = RoundedCornerShape(6.dp),
            color = AviationDarkCard.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = AviationAccent,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "Surface to 500' Airspace within 30 NM radius",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Left-Center Vertical Zoom Controls (DJI RC Pro Enterprise Glove-Friendly Touch Targets)
        Surface(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp),
            shape = RoundedCornerShape(8.dp),
            color = AviationDarkCard.copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { mapViewRef?.controller?.zoomIn() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = AviationCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                HorizontalDivider(color = AviationDarkBorder, thickness = 0.8.dp, modifier = Modifier.width(32.dp))
                IconButton(
                    onClick = { mapViewRef?.controller?.zoomOut() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = AviationCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Floating Airspace Classification Legend & Layer Toggles (Top-Right)
        if (showLegend) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 46.dp, end = 8.dp)
                    .widthIn(max = 260.dp)
                    .verticalScroll(rememberScrollState()),
                colors = CardDefaults.cardColors(containerColor = AviationDarkCard.copy(alpha = 0.98f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationDarkBorder)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AIRSPACE LAYERS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 9.sp
                            )
                        )
                        Text(
                            text = "TOGGLE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AviationAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        )
                    }

                    HorizontalDivider(color = AviationDarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))

                    AirspaceLayerToggleRow(
                        name = "Restricted / Prohibited",
                        color = SafetyNoGo,
                        enabled = enabledZoneTypes.contains(AirspaceZoneType.RESTRICTED_ZONE),
                        onToggle = { enabled ->
                            enabledZoneTypes = if (enabled) enabledZoneTypes + AirspaceZoneType.RESTRICTED_ZONE else enabledZoneTypes - AirspaceZoneType.RESTRICTED_ZONE
                        }
                    )

                    AirspaceLayerToggleRow(
                        name = "Controlled (Class B, C, D)",
                        color = AviationCyan,
                        enabled = enabledZoneTypes.contains(AirspaceZoneType.AUTHORIZATION_ZONE),
                        onToggle = { enabled ->
                            enabledZoneTypes = if (enabled) enabledZoneTypes + AirspaceZoneType.AUTHORIZATION_ZONE else enabledZoneTypes - AirspaceZoneType.AUTHORIZATION_ZONE
                        }
                    )

                    AirspaceLayerToggleRow(
                        name = "Warning / Surface E",
                        color = SafetyCautionLight,
                        enabled = enabledZoneTypes.contains(AirspaceZoneType.WARNING_ZONE),
                        onToggle = { enabled ->
                            enabledZoneTypes = if (enabled) enabledZoneTypes + AirspaceZoneType.WARNING_ZONE else enabledZoneTypes - AirspaceZoneType.WARNING_ZONE
                        }
                    )

                    AirspaceLayerToggleRow(
                        name = "Special Use / MOA",
                        color = Color(0xFFFF8C00),
                        enabled = enabledZoneTypes.contains(AirspaceZoneType.SPECIAL_USE),
                        onToggle = { enabled ->
                            enabledZoneTypes = if (enabled) enabledZoneTypes + AirspaceZoneType.SPECIAL_USE else enabledZoneTypes - AirspaceZoneType.SPECIAL_USE
                        }
                    )

                    HorizontalDivider(color = AviationDarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))

                    AirspaceLayerToggleRow(
                        name = "Airports & Comms (30 NM)",
                        color = AviationCyan,
                        enabled = showAirportsLayer,
                        onToggle = { showAirportsLayer = it }
                    )

                    AirspaceLayerToggleRow(
                        name = "Airport Warning / High Risk",
                        color = Color(0xFFEE8815),
                        enabled = showAirportWarningZones,
                        onToggle = { showAirportWarningZones = it }
                    )
                }
            }
        }

        // Airport Comms Callout Card (Shows CTAF frequency, service type, distance, coordinates)
        selectedAirport?.let { airport ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 54.dp, start = 8.dp, end = 8.dp)
                    .fillMaxWidth()
                    .heightIn(max = 160.dp),
                colors = CardDefaults.cardColors(containerColor = AviationDarkCard.copy(alpha = 0.98f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationCyan)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Header with airport ident, name, and dismiss button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Flight, contentDescription = null, tint = AviationCyan, modifier = Modifier.size(15.dp))
                            Text(
                                text = "${airport.ident} — ${airport.name}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp),
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = { selectedAirport = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }

                    HorizontalDivider(color = AviationDarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // Body: Frequency details & distance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "COMMUNICATION FREQUENCY",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${airport.frequencyMhz} MHz",
                                    style = MaterialTheme.typography.titleMedium.copy(color = AviationCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AviationDarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, AviationCyan.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = airport.type,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "DISTANCE",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f NM", airport.distanceNm),
                                style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Coordinates & Advisory
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "Coord: %.4f°N, %.4f°W", airport.lat, abs(airport.lon)),
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 8.5.sp)
                        )
                        Text(
                            text = "Monitor frequency for manned traffic",
                            style = MaterialTheme.typography.bodySmall.copy(color = SafetyCautionLight, fontSize = 8.5.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }

        // Multi-Layer Airspace Inspector Popup (Shows all overlapping polygons at tapped location)
        if (selectedAirport == null) {
            inspectionResult?.let { inspect ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 54.dp, start = 8.dp, end = 8.dp)
                    .fillMaxWidth()
                    .heightIn(max = 160.dp),
                colors = CardDefaults.cardColors(containerColor = AviationDarkCard.copy(alpha = 0.98f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationDarkBorder)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Header with coordinate & dismiss button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(13.dp))
                            val totalLayers = inspect.zones.size + inspect.warningZones.size
                            Text(
                                text = if (totalLayers > 0) "AIRSPACE & WARNING ZONES ($totalLayers LAYERS)" else "UNCONTROLLED AIRSPACE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 10.sp)
                            )
                            Text(
                                text = String.format("%.4f°N, %.4f°W", inspect.point.latitude, abs(inspect.point.longitude)),
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp)
                            )
                        }
                        IconButton(
                            onClick = { inspectionResult = null },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(13.dp))
                        }
                    }

                    HorizontalDivider(color = AviationDarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 3.dp))

                    if (inspect.zones.isEmpty() && inspect.warningZones.isEmpty()) {
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SafetyGoLight))
                            Column {
                                Text("Class G Airspace (Uncontrolled)", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                                Text("No prior FAA LAANC or ATC authorization required for flight up to 400 ft AGL.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 8.5.sp))
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(inspect.zones) { zone ->
                                val (badgeColor, badgeText) = when (zone.type) {
                                    AirspaceZoneType.RESTRICTED_ZONE -> Pair(SafetyNoGo, "RESTRICTED / TFR")
                                    AirspaceZoneType.AUTHORIZATION_ZONE -> Pair(AviationCyan, "CONTROLLED AIRSPACE")
                                    AirspaceZoneType.WARNING_ZONE -> Pair(SafetyCautionLight, "WARNING / SURFACE E")
                                    AirspaceZoneType.ALTITUDE_ZONE -> Pair(AviationCyan, "CONTROLLED AIRSPACE")
                                    AirspaceZoneType.SPECIAL_USE -> Pair(Color(0xFFFF8C00), "SPECIAL USE / MOA")
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AviationDarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = zone.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(2.dp),
                                                    color = badgeColor.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = badgeText,
                                                        style = MaterialTheme.typography.labelSmall.copy(color = badgeColor, fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = zone.description,
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 8.5.sp)
                                            )
                                        }
                                    }
                                }
                            }

                            items(inspect.warningZones) { wZone ->
                                val isHighRisk = wZone.zoneType == "HIGH_RISK_BOWTIE" || wZone.level == 3
                                val badgeColor = if (isHighRisk) Color(0xFFEE8815) else Color(0xFFFFCC00)
                                val badgeText = if (isHighRisk) "HIGH RISK ZONE" else "RUNWAY BUFFER (3KM)"

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AviationDarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "${wZone.ident} - ${wZone.name}",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(2.dp),
                                                    color = badgeColor.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = badgeText,
                                                        style = MaterialTheme.typography.labelSmall.copy(color = badgeColor, fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (isHighRisk) {
                                                    "Runway approach/departure corridor • 5.25km extent"
                                                } else {
                                                    "Airport runway centerline buffer • 3km radius"
                                                },
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 8.5.sp)
                                            )
                                            Text(
                                                text = "Advisory: Airport proximity warning zone, monitor local traffic.",
                                                style = MaterialTheme.typography.bodySmall.copy(color = badgeColor, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // Bottom Telemetry Bar (640x360 Landscape Optimized with CTAF)
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
                Column(modifier = Modifier.weight(1.6f)) {
                    Text(
                        text = loc.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                    )
                    Text(
                        text = "${loc.formattedCoordinates} • ${loc.ctafDisplay}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 9.sp)
                    )
                }

                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(0.9f)) {
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
                    onClick = {
                        onRefreshGpsLocation()
                        shouldRecenterMap = true
                    },
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

@Composable
private fun AirspaceLayerToggleRow(
    name: String,
    color: Color,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.5f),
                    fontSize = 9.5.sp,
                    fontWeight = if (enabled) FontWeight.Medium else FontWeight.Normal
                )
            )
        }
        Checkbox(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = color,
                uncheckedColor = TextSecondary.copy(alpha = 0.4f),
                checkmarkColor = Color.Black
            ),
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun isPointInZone(lat: Double, lon: Double, zone: AirspaceZone): Boolean {
    if (zone.polygonCoordinates.size >= 3) {
        return isPointInPolygon(lat, lon, zone.polygonCoordinates)
    } else {
        val distMeters = calculateDistanceMeters(lat, lon, zone.centerLat, zone.centerLon)
        return distMeters <= zone.radiusMeters
    }
}

private fun isPointInPolygon(lat: Double, lon: Double, poly: List<Pair<Double, Double>>): Boolean {
    if (poly.size < 3) return false
    var inside = false
    var j = poly.size - 1
    for (i in poly.indices) {
        val (latI, lonI) = poly[i]
        val (latJ, lonJ) = poly[j]
        if (((latI > lat) != (latJ > lat)) &&
            (lon < (lonJ - lonI) * (lat - latI) / (latJ - latI) + lonI)
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6378137.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
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

// High-Contrast Outdoor-Readable Airport & CTAF Frequency Marker Pill
private fun createAirportMarkerDrawable(context: Context, ident: String, freqMhz: String): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val labelText = "$ident $freqMhz"

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 9.5f * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(56, 189, 248) // Aviation Sky Cyan
        textSize = 9.5f * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val iconStr = "✈ "
    val iconWidth = iconPaint.measureText(iconStr)
    val textWidth = textPaint.measureText(labelText)
    val totalContentWidth = iconWidth + textWidth

    val fontMetrics = textPaint.fontMetrics
    val textHeight = fontMetrics.descent - fontMetrics.ascent

    val padH = 6f * density
    val padV = 3f * density
    val width = (totalContentWidth + padH * 2).toInt().coerceAtLeast((36 * density).toInt())
    val height = (textHeight + padV * 2).toInt().coerceAtLeast((18 * density).toInt())

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val rect = android.graphics.RectF(0.6f * density, 0.6f * density, width - 0.6f * density, height - 0.6f * density)
    val cornerRadius = 4f * density

    // Background fill (Deep Slate / Aviation Dark)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(15, 23, 42) // Slate 900
        style = Paint.Style.FILL
    }
    // Border stroke (Vivid Sky Cyan)
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(56, 189, 248) // Sky 400
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }

    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

    // Draw Icon and Text
    val textY = padV - fontMetrics.ascent
    canvas.drawText(iconStr, padH, textY, iconPaint)
    canvas.drawText(labelText, padH + iconWidth, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}

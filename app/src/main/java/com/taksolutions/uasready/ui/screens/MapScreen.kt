package com.taksolutions.uasready.ui.screens

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
import com.taksolutions.uasready.ui.map.BitmapMarkerIcons
import com.taksolutions.uasready.ui.map.MapOverlayController
import com.taksolutions.uasready.ui.presentation.AirspaceInspector
import com.taksolutions.uasready.ui.presentation.OverviewCardText
import com.taksolutions.uasready.ui.theme.*
import com.taksolutions.uasready.ui.viewmodel.MainUiState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
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

    var overlayController by remember { mutableStateOf<MapOverlayController?>(null) }
    // The map keeps these callbacks for its lifetime; rememberUpdatedState makes them read current data.
    val currentOnMapTap by rememberUpdatedState { p: GeoPoint ->
        selectedAirport = null
        val hits = AirspaceInspector.inspect(
            p.latitude, p.longitude, liveAirspaceZones, enabledZoneTypes, uiState.airportWarningZones, showAirportWarningZones
        )
        inspectionResult = AirspaceInspection(p, hits.zones, hits.warningZones)
    }
    val currentOnAirportTap by rememberUpdatedState { airport: AirportCtafResult ->
        inspectionResult = null
        selectedAirport = airport
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AviationDarkBackground)
    ) {
        // Interactive map. Overlays are owned by MapOverlayController, which rebuilds a layer only
        // when its inputs change; this block runs on every recomposition and must stay cheap.
        AndroidView(
            factory = { context ->
                Configuration.getInstance().userAgentValue = "UASReady-Android-App/1.0"
                MapView(context).apply {
                    setTileSource(STREET_TILE_SOURCE)
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(false)
                    controller.setZoom(12.8)
                    controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
                    overlayController = MapOverlayController(
                        mapView = this,
                        markerIcons = BitmapMarkerIcons(context),
                        onMapTap = { p -> currentOnMapTap(p) },
                        onAirportTap = { airport -> currentOnAirportTap(airport) }
                    )
                    mapViewRef = this
                }
            },
            update = { _ ->
                val c = overlayController ?: return@AndroidView
                c.setTileSource(
                    when (selectedBasemap) {
                        BasemapType.STREET -> STREET_TILE_SOURCE
                        BasemapType.TOPO -> TOPO_TILE_SOURCE
                        BasemapType.HYBRID -> HYBRID_TILE_SOURCE
                    }
                )
                c.setUserLocation(loc.latitude, loc.longitude, "Launch Point: ${loc.displayName}", "Coordinates: ${loc.formattedCoordinates}")
                if (shouldRecenterMap) {
                    c.recenter(loc.latitude, loc.longitude)
                    shouldRecenterMap = false
                }
                c.setWarningZones(uiState.airportWarningZones, showAirportWarningZones)
                c.setAirspaceZones(liveAirspaceZones, enabledZoneTypes)
                c.setAirports(uiState.nearbyAirports, showAirportsLayer)
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
                    .size(MAP_RIGHT_CONTROL_SIZE)
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
                    .padding(bottom = 54.dp, start = 8.dp, end = MAP_RIGHT_CONTROLS_GUTTER)
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
                    .padding(bottom = 54.dp, start = 8.dp, end = MAP_RIGHT_CONTROLS_GUTTER)
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
        // The location button sits in the right-hand control column (under the layers button),
        // not inside the bar, so the bar and the cards above it never cover a control.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MAP_CONTROL_GAP)
        ) {
        Box(
            modifier = Modifier
                .weight(1f)
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
                        // Estimate from live space weather + terrain; never an invented value (see OverviewCardText).
                        text = if (gnss != null) "~${gnss.lockedSatellitesCount} Sats Visible" else "GNSS: ${OverviewCardText.NO_LIVE_DATA}",
                        style = MaterialTheme.typography.bodySmall.copy(color = AviationAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    )
                    Text(
                        text = if (gnss != null) "HDOP ${gnss.estimatedHdop} • 3D Fix" else "Needs NOAA space weather",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 8.sp)
                    )
                }

            }
        }

        IconButton(
            onClick = {
                onRefreshGpsLocation()
                shouldRecenterMap = true
            },
            modifier = Modifier
                .size(MAP_RIGHT_CONTROL_SIZE)
                .clip(RoundedCornerShape(8.dp))
                .background(AviationDarkCard.copy(alpha = 0.95f))
                .border(1.dp, AviationDarkBorder, RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Center GPS", tint = AviationAccent, modifier = Modifier.size(16.dp))
        }
        }
    }
}

/**
 * Right-hand control column (layers + location buttons), 8 dp from the edge. 48 dp = Material's
 * minimum touch target: a smaller IconButton is enlarged to 48 dp anyway and overflowed its slot,
 * pushing the button against the screen edge. Also glove-friendly on the RC Pro.
 */
private val MAP_RIGHT_CONTROL_SIZE = 48.dp
private val MAP_CONTROL_GAP = 8.dp
/** End padding for anything that must not cover the right-hand controls. */
private val MAP_RIGHT_CONTROLS_GUTTER = 8.dp + MAP_RIGHT_CONTROL_SIZE + MAP_CONTROL_GAP

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

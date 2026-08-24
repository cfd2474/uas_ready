package com.uasready.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.uasready.data.nasr.GeometryUtils
import com.uasready.data.nasr.NasrAirport
import com.uasready.data.nasr.NasrDatabaseHelper
import com.uasready.data.nasr.NasrSeedData
import com.uasready.data.nasr.ParsedTfr
import com.uasready.domain.model.AirspaceZone
import com.uasready.domain.model.AirspaceZoneType
import com.uasready.ui.theme.*
import com.uasready.ui.viewmodel.MainUiState
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
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
    val associatedAirport: NasrAirport? = null
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
    onDismissAiracWarning: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLegend by remember { mutableStateOf(false) }
    var selectedBasemap by remember { mutableStateOf(BasemapType.STREET) }
    var inspectionResult by remember { mutableStateOf<AirspaceInspection?>(null) }
    var shouldRecenterMap by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var renderJob by remember { mutableStateOf<Job?>(null) }
    var isRenderingAirspace by remember { mutableStateOf(false) }

    var enabledZoneTypes by remember {
        mutableStateOf(
            setOf(
                AirspaceZoneType.RESTRICTED_ZONE,
                AirspaceZoneType.AUTHORIZATION_ZONE,
                AirspaceZoneType.WARNING_ZONE,
                AirspaceZoneType.ALTITUDE_ZONE,
                AirspaceZoneType.SPECIAL_USE
            )
        )
    }

    val loc = uiState.currentLocation
    val gnss = uiState.estimatedGnss
    val airac = uiState.airacCycleInfo

    // Helper: Match or look up associated airport for a clicked location/polygon
    fun findAssociatedAirport(point: GeoPoint, zone: AirspaceZone?, airports: List<NasrAirport>, helper: NasrDatabaseHelper): NasrAirport? {
        if (zone != null) {
            val icaoCandidate = zone.id.substringAfter("NASR-").substringBefore("-").takeIf { it.length in 3..4 }
                ?: zone.name.split(" ", "(", ")", "-").firstOrNull { it.length == 4 && (it.startsWith("K") || it.startsWith("P")) }
            if (icaoCandidate != null) {
                val matched = airports.find { it.icaoId.equals(icaoCandidate, ignoreCase = true) }
                if (matched != null) return matched
            }
        }
        val nearby = airports.minByOrNull { GeometryUtils.calculateDistanceNm(point.latitude, point.longitude, it.latitude, it.longitude) }
        if (nearby != null && GeometryUtils.calculateDistanceNm(point.latitude, point.longitude, nearby.latitude, nearby.longitude) <= 8.0) {
            return nearby
        }
        val nearest = helper.findNearestAirport(point.latitude, point.longitude)
        return if (nearest != null && GeometryUtils.calculateDistanceNm(point.latitude, point.longitude, nearest.latitude, nearest.longitude) <= 10.0) {
            nearest
        } else {
            null
        }
    }

    // Master function: Queries visible extent asynchronously on IO dispatcher and updates overlays with loading indicator
    fun renderExtentOverlays(
        mapView: MapView,
        helper: NasrDatabaseHelper,
        currentEnabledTypes: Set<AirspaceZoneType> = enabledZoneTypes,
        debounceMs: Long = 60L
    ) {
        renderJob?.cancel()
        renderJob = coroutineScope.launch {
            if (debounceMs > 0) {
                delay(debounceMs)
            }
            isRenderingAirspace = true
            try {
                val bbox = mapView.boundingBox ?: return@launch
                val zoom = mapView.zoomLevelDouble

                // Expand bounds by 15% to guarantee smooth panning without visible popping
                val latMargin = maxOf(0.04, abs(bbox.latNorth - bbox.latSouth) * 0.15)
                val lonMargin = maxOf(0.04, abs(bbox.lonEast - bbox.lonWest) * 0.15)
                val minLat = bbox.latSouth - latMargin
                val maxLat = bbox.latNorth + latMargin
                val minLon = bbox.lonWest - lonMargin
                val maxLon = bbox.lonEast + lonMargin
                val nowMs = System.currentTimeMillis()

                // Execute SQLite queries on background IO dispatcher
                val allAptsInExtent = withContext(Dispatchers.IO) {
                    helper.queryAirportsInBoundingBox(minLat, maxLat, minLon, maxLon, limit = 500)
                }
                val airspacesInExtent = withContext(Dispatchers.IO) {
                    helper.queryAirspaceInBoundingBox(minLat, maxLat, minLon, maxLon, limit = 300)
                }
                val suaInExtent = withContext(Dispatchers.IO) {
                    helper.querySuaInBoundingBox(minLat, maxLat, minLon, maxLon, limit = 150)
                }
                val nsRestrictionsInExtent = withContext(Dispatchers.IO) {
                    helper.queryNationalSecurityRestrictionsInBoundingBox(minLat, maxLat, minLon, maxLon, limit = 200)
                }
                val activeTfrs = withContext(Dispatchers.IO) {
                    helper.queryActiveTfrsInBoundingBox(minLat, maxLat, minLon, maxLon, nowMs = nowMs, limit = 50)
                }
                val uasfmInExtent = withContext(Dispatchers.IO) {
                    if (zoom >= 10.0) {
                        helper.queryUasfmInBoundingBox(minLat, maxLat, minLon, maxLon, limit = 3000)
                    } else {
                        emptyList()
                    }
                }

                // Dynamic LOD / Stacking for Airports on wide zoom
                val visibleAirports = when {
                    zoom >= 8.5 -> allAptsInExtent
                    zoom >= 6.0 -> allAptsInExtent.filter { !it.towerFreq.isNullOrBlank() || it.useType == "PU" }
                    else -> allAptsInExtent.filter { !it.towerFreq.isNullOrBlank() }
                }

                val allExtentZones = mutableListOf<AirspaceZone>()
                allExtentZones.addAll(airspacesInExtent)
                allExtentZones.addAll(uasfmInExtent)
                allExtentZones.addAll(suaInExtent)
                allExtentZones.addAll(nsRestrictionsInExtent)
                for (tfr in activeTfrs) {
                    if (tfr.polygonCoordinates.isNotEmpty()) {
                        allExtentZones.add(
                            AirspaceZone(
                                id = "TFR-${tfr.notamId}",
                                name = if (tfr.isHazard91137) "14 CFR § 91.137 TFR (${tfr.notamId})" else "TFR ${tfr.notamId} (${tfr.type})",
                                type = AirspaceZoneType.RESTRICTED_ZONE,
                                centerLat = tfr.centerLat ?: ((minLat + maxLat) / 2.0),
                                centerLon = tfr.centerLon ?: ((minLon + maxLon) / 2.0),
                                radiusMeters = tfr.radiusNm * 1852.0,
                                floorFt = tfr.floorFt,
                                ceilingFt = tfr.ceilingFt,
                                description = "FAA TFR: ${tfr.description}",
                                polygonCoordinates = tfr.polygonCoordinates
                            )
                        )
                    }
                }

                // Clear all previous overlays
                mapView.overlays.clear()

                // LAYER 0: MapEventsReceiver (Underneath all polygons and markers for background taps)
                val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        val activeZones = allExtentZones.filter { it.type in currentEnabledTypes }
                        val overlapping = activeZones.filter { isPointInZone(p.latitude, p.longitude, it) }
                        val matchedApt = findAssociatedAirport(p, overlapping.firstOrNull(), allAptsInExtent, helper)
                        inspectionResult = AirspaceInspection(p, overlapping, matchedApt)
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint): Boolean = false
                })
                mapView.overlays.add(mapEventsOverlay)

                // LAYER 1: Polygon Overlays (Airspace, 1-arcminute UASFM grids, SUA, TFRs)
                val filteredZones = allExtentZones.filter { it.type in currentEnabledTypes }
                val sortedZones = filteredZones.sortedBy { if (it.type == AirspaceZoneType.ALTITUDE_ZONE) 1 else 0 }

                sortedZones.forEach { zone ->
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
                                // 1 Arc-Minute UAS Facility Map Grids with altitude-tiered coloring
                                val ceiling = zone.ceilingFt ?: 400.0
                                val fillA = when {
                                    ceiling <= 0.0 -> AndroidColor.argb(80, 235, 65, 65) // 0 ft Red
                                    ceiling <= 100.0 -> AndroidColor.argb(70, 255, 140, 0) // 50-100 ft Orange
                                    ceiling <= 200.0 -> AndroidColor.argb(60, 240, 195, 35) // 200 ft Yellow
                                    ceiling <= 300.0 -> AndroidColor.argb(55, 145, 215, 60) // 300 ft Chartreuse
                                    else -> AndroidColor.argb(50, 0, 210, 255) // 400 ft Cyan
                                }
                                fillPaint.color = fillA
                                // Crisp high-contrast 1 arc-minute grid boundary line so every single 1'x1' square is individually distinct
                                outlinePaint.color = AndroidColor.argb(210, 15, 20, 30)
                                outlinePaint.strokeWidth = 1.6f
                            }
                            AirspaceZoneType.SPECIAL_USE -> {
                                fillPaint.color = AndroidColor.argb(45, 255, 140, 0)
                                outlinePaint.color = AndroidColor.argb(255, 255, 140, 0)
                                outlinePaint.strokeWidth = 2.5f
                            }
                        }

                        // Forward polygon clicks to multi-layer inspection with associated airport
                        setOnClickListener { _, _, clickPoint ->
                            val active = allExtentZones.filter { it.type in currentEnabledTypes }
                            val overlapping = active.filter { isPointInZone(clickPoint.latitude, clickPoint.longitude, it) }
                            val matchedApt = findAssociatedAirport(clickPoint, zone, allAptsInExtent, helper)
                            inspectionResult = AirspaceInspection(clickPoint, overlapping, matchedApt)
                            true
                        }
                    }
                    mapView.overlays.add(polygon)
                }

                // LAYER 2: User Launch Point Marker
                val userPoint = GeoPoint(loc.latitude, loc.longitude)
                val userMarker = Marker(mapView).apply {
                    id = "USER_MARKER"
                    position = userPoint
                    icon = createSmallRedPinDrawable(mapView.context)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Launch Point: ${loc.displayName}"
                    snippet = "Coordinates: ${loc.formattedCoordinates}"
                }
                mapView.overlays.add(userMarker)

                // LAYER 3: Airport Markers (On top of polygons for highest visual visibility and touch priority)
                visibleAirports.forEach { apt ->
                    val aptPos = GeoPoint(apt.latitude, apt.longitude)
                    val aptMarker = Marker(mapView).apply {
                        id = "APT_${apt.icaoId}"
                        position = aptPos
                        icon = createAirportMarkerDrawable(mapView.context, apt.icaoId, apt.effectiveCtaf ?: "")
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "${apt.name} (${apt.icaoId})"
                        snippet = "CTAF: ${apt.effectiveCtaf ?: "N/A"} • Elev: ${apt.elevationFt.toInt()} ft MSL"
                        setOnMarkerClickListener { _, _ ->
                            val active = allExtentZones.filter { it.type in currentEnabledTypes }
                            val overlapping = active.filter { isPointInZone(apt.latitude, apt.longitude, it) }
                            inspectionResult = AirspaceInspection(aptPos, overlapping, apt)
                            true
                        }
                    }
                    mapView.overlays.add(aptMarker)
                }

                mapView.invalidate()
                mapView.postInvalidate()
            } finally {
                isRenderingAirspace = false
            }
        }
    }

    // Reactive listener to immediately re-render overlays when layer toggles change
    LaunchedEffect(enabledZoneTypes) {
        mapViewRef?.let { map ->
            val helper = NasrDatabaseHelper(map.context)
            renderExtentOverlays(map, helper, enabledZoneTypes)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AviationDarkBackground)
    ) {
        // Interactive Map View with Live Aeronautical Overlays & Selectable NO-POI Basemaps
        AndroidView(
            factory = { context ->
                val helper = NasrDatabaseHelper(context)
                NasrSeedData.populateDatabaseIfEmpty(helper)
                Configuration.getInstance().userAgentValue = "UASReady-Android-App/1.0"
                MapView(context).apply {
                    mapViewRef = this
                    setTileSource(STREET_TILE_SOURCE)
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(false)
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(12.8)
                    val startPoint = GeoPoint(loc.latitude, loc.longitude)
                    controller.setCenter(startPoint)

                    // Dynamic Extent Listener: Re-queries SQLite whenever pan/scroll/zoom extent changes
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            renderExtentOverlays(this@apply, helper, enabledZoneTypes)
                            return true
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            renderExtentOverlays(this@apply, helper, enabledZoneTypes)
                            return true
                        }
                    })

                    post {
                        renderExtentOverlays(this, helper, enabledZoneTypes)
                    }
                }
            },
            update = { mapView ->
                mapViewRef = mapView
                val helper = NasrDatabaseHelper(mapView.context)
                NasrSeedData.populateDatabaseIfEmpty(helper)

                // Apply Selected NO-POI Basemap Tile Source
                val targetTileSource = when (selectedBasemap) {
                    BasemapType.STREET -> STREET_TILE_SOURCE
                    BasemapType.TOPO -> TOPO_TILE_SOURCE
                    BasemapType.HYBRID -> HYBRID_TILE_SOURCE
                }
                if (mapView.tileProvider.tileSource.name() != targetTileSource.name()) {
                    mapView.setTileSource(targetTileSource)
                }

                if (shouldRecenterMap) {
                    val point = GeoPoint(loc.latitude, loc.longitude)
                    mapView.controller.animateTo(point)
                    shouldRecenterMap = false
                }

                renderExtentOverlays(mapView, helper, enabledZoneTypes)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center-Left Vertical Zoom Controls (Optimized for thumb reach on DJI RC Pro Enterprise landscape canvas)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AviationDarkCard.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder),
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(
                    onClick = { mapViewRef?.controller?.zoomIn() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = AviationAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AviationDarkCard.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder),
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(
                    onClick = { mapViewRef?.controller?.zoomOut() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = AviationAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Top Floating Control Bar (Basemap Selector + AIRAC Badge + Legend Toggle)
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

            // Center Group: Loading Indicator & AIRAC Cycle Currency Indicator Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dynamic Map Extent Rendering Loading Overlay
                AnimatedVisibility(
                    visible = isRenderingAirspace,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AviationDarkCard.copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AviationCyan.copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.8.dp,
                                color = AviationCyan
                            )
                            Text(
                                text = "RENDERING AIRSPACE...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AviationCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }

                if (airac != null) {
                    val badgeColor = when {
                        airac.isExpired -> SafetyNoGo
                        airac.daysUntilExpiry <= 3 -> SafetyCautionLight
                        else -> SafetyGoLight
                    }
                    val badgeStatusText = when {
                        airac.isExpired -> "EXPIRED"
                        airac.daysUntilExpiry <= 3 -> "${airac.daysUntilExpiry}d LEFT"
                        else -> "CURRENT"
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AviationDarkCard.copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(badgeColor))
                            Text(
                                text = "FAA NASR ${airac.cycleName} • $badgeStatusText",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
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
                        name = "UAS Facility Grid (UASFM)",
                        color = Color(0xFF00D2FF),
                        enabled = enabledZoneTypes.contains(AirspaceZoneType.ALTITUDE_ZONE),
                        onToggle = { enabled ->
                            enabledZoneTypes = if (enabled) enabledZoneTypes + AirspaceZoneType.ALTITUDE_ZONE else enabledZoneTypes - AirspaceZoneType.ALTITUDE_ZONE
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
                }
            }
        }

        // Multi-Layer Airspace Inspector Popup (Shows all overlapping polygons at tapped location)
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
                            Text(
                                text = if (inspect.zones.isNotEmpty()) "AIRSPACE INTERSECTIONS (${inspect.zones.size} LAYERS)" else "UNCONTROLLED AIRSPACE",
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

                    if (inspect.zones.isEmpty() && inspect.associatedAirport == null) {
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
                            // Associated Airport Facility Card (CTAF Listen Only, Tower, ATIS, Elevation)
                            inspect.associatedAirport?.let { apt ->
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AviationDarkSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AviationAccent.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(14.dp))
                                                    Text(
                                                        text = "${apt.name} (${apt.icaoId})",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(3.dp),
                                                    color = AviationAccent.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "AIRPORT FACILITY",
                                                        style = MaterialTheme.typography.labelSmall.copy(color = AviationAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(3.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                apt.effectiveCtaf?.let { ctaf ->
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = AviationCyan.copy(alpha = 0.2f),
                                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, AviationCyan)
                                                    ) {
                                                        Text(
                                                            text = "CTAF: $ctaf MHz",
                                                            style = MaterialTheme.typography.labelSmall.copy(color = AviationCyan, fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                apt.towerFreq?.let { twr ->
                                                    Text(
                                                        text = "Tower: $twr MHz",
                                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 8.5.sp)
                                                    )
                                                }
                                                apt.atisFreq?.let { atis ->
                                                    Text(
                                                        text = "ATIS: $atis MHz",
                                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 8.5.sp)
                                                    )
                                                }
                                                Text(
                                                    text = "Elev: ${apt.elevationFt.toInt()} ft MSL",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 8.5.sp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "• Note: CTAF is Listen-Only. UAS pilots are not authorized to talk on air frequencies.",
                                                style = MaterialTheme.typography.bodySmall.copy(color = SafetyCautionLight, fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold)
                                            )
                                        }
                                    }
                                }
                            }

                            items(inspect.zones) { zone ->
                                val (badgeColor, badgeText) = when (zone.type) {
                                    AirspaceZoneType.RESTRICTED_ZONE -> Pair(SafetyNoGo, "RESTRICTED / TFR")
                                    AirspaceZoneType.AUTHORIZATION_ZONE -> Pair(AviationCyan, "CONTROLLED AIRSPACE")
                                    AirspaceZoneType.WARNING_ZONE -> Pair(SafetyCautionLight, "WARNING / SURFACE E")
                                    AirspaceZoneType.ALTITUDE_ZONE -> {
                                        val ceil = (zone.ceilingFt ?: 400.0).toInt()
                                        if (ceil == 0) Pair(SafetyNoGo, "UASFM 0 FT (NO LAANC)")
                                        else Pair(Color(0xFF00D2FF), "UASFM $ceil FT CEILING")
                                    }
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
                        }
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
        // AIRAC Cycle Expiry Warning Dialog (Advisory, never locks flight)
        if (uiState.showAiracExpiryPrompt && airac != null) {
            AlertDialog(
                onDismissRequest = onDismissAiracWarning,
                icon = {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = SafetyCautionLight, modifier = Modifier.size(28.dp))
                },
                title = {
                    Text(
                        text = "AIRAC CYCLE EXPIRED (${airac.cycleName})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "The on-device FAA NASR aeronautical database cycle (${airac.cycleName}) has expired.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                        )
                        Text(
                            text = "Aviation awareness remains active. You may proceed with caution or update to the latest cycle via Settings.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onDismissAiracWarning,
                        colors = ButtonDefaults.buttonColors(containerColor = AviationCyan)
                    ) {
                        Text("Proceed with Caution", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = AviationDarkCard,
                textContentColor = TextPrimary
            )
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
    val width = (14 * density).toInt()
    val height = (20 * density).toInt()
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
        color = AndroidColor.rgb(220, 38, 38)
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

// Airport CTAF Badge Marker (High-Contrast Sectional Style)
private fun createAirportMarkerDrawable(context: Context, icao: String, ctaf: String): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val width = (64 * density).toInt()
    val height = (26 * density).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(220, 13, 17, 23)
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(255, 0, 210, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }
    val textPaintIcao = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 9f * density
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val textPaintCtaf = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(0, 210, 255)
        textSize = 8f * density
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val rect = android.graphics.RectF(1.5f * density, 1.5f * density, width - 1.5f * density, height - 1.5f * density)
    canvas.drawRoundRect(rect, 4f * density, 4f * density, bgPaint)
    canvas.drawRoundRect(rect, 4f * density, 4f * density, borderPaint)

    canvas.drawText(icao, 5f * density, 11f * density, textPaintIcao)
    val ctafLabel = if (ctaf.isNotBlank()) ctaf else "CTAF"
    canvas.drawText(ctafLabel, 5f * density, 21f * density, textPaintCtaf)

    return BitmapDrawable(context.resources, bitmap)
}

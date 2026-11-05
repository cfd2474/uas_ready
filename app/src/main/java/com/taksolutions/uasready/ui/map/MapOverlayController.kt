package com.taksolutions.uasready.ui.map

import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.Log
import com.taksolutions.uasready.data.repository.AirportCtafResult
import com.taksolutions.uasready.domain.model.AirportWarningZone
import com.taksolutions.uasready.domain.model.AirspaceZone
import com.taksolutions.uasready.domain.model.AirspaceZoneType
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon

/**
 * Owns the osmdroid overlays of the airspace map.
 *
 * Compose may call the setters on every recomposition; each layer is rebuilt only when its own
 * inputs change (previously every tap, legend toggle or loading flag rebuilt every polygon and
 * marker bitmap). All taps — on empty map or on a polygon — go to [onMapTap] with the tapped
 * point, so the caller always inspects the current data (no listener captures stale state).
 */
class MapOverlayController(
    private val mapView: MapView,
    private val markerIcons: MarkerIconFactory,
    private val onMapTap: (GeoPoint) -> Unit,
    private val onAirportTap: (AirportCtafResult) -> Unit
) {
    private val polygonTap = Polygon.OnClickListener { _, _, point -> onMapTap(point); true }

    private val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean { onMapTap(p); return true }
        override fun longPressHelper(p: GeoPoint): Boolean = false
    })

    private val userMarker = Marker(mapView).apply {
        id = USER_MARKER_ID
        icon = markerIcons.launchPin()
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    }

    private var warningLayer: List<Overlay> = emptyList()
    private var zoneLayer: List<Overlay> = emptyList()
    private var airportLayer: List<Overlay> = emptyList()

    // Inputs of the last build of each layer. Lists are compared by identity: the view model
    // publishes a new list instance only when a refresh brings new data.
    private var warningKey: Any? = UNSET
    private var zoneKey: Any? = UNSET
    private var airportKey: Any? = UNSET

    init {
        assemble()
    }

    fun setTileSource(source: ITileSource) {
        if (mapView.tileProvider.tileSource.name() != source.name()) mapView.setTileSource(source)
    }

    fun setUserLocation(latitude: Double, longitude: Double, title: String, snippet: String) {
        val p = userMarker.position
        if (p == null || p.latitude != latitude || p.longitude != longitude || userMarker.title != title) {
            userMarker.position = GeoPoint(latitude, longitude)
            userMarker.title = title
            userMarker.snippet = snippet
            mapView.postInvalidate()
        }
    }

    fun recenter(latitude: Double, longitude: Double) = mapView.controller.animateTo(GeoPoint(latitude, longitude))

    fun setWarningZones(zones: List<AirportWarningZone>, visible: Boolean) {
        val key = Key(zones, visible)
        if (key == warningKey) return
        warningKey = key
        warningLayer = timed("warning zones") {
            if (!visible) emptyList() else zones.filter { it.polygonCoordinates.size >= 3 }.map { zone ->
                Polygon(mapView).apply {
                    points = zone.polygonCoordinates.map { GeoPoint(it.first, it.second) }
                    title = "${zone.ident} - ${zone.name}"
                    val highRisk = zone.zoneType == "HIGH_RISK_BOWTIE" || zone.level == 3
                    snippet = if (highRisk) "High Risk Zone (Runway approach/departure corridor)" else "Runway Buffer (3km proximity radius)"
                    if (highRisk) style(AndroidColor.argb(40, 238, 136, 21), AndroidColor.argb(230, 238, 136, 21), 2.0f)
                    else style(AndroidColor.argb(25, 255, 204, 0), AndroidColor.argb(200, 255, 204, 0), 1.5f)
                    setOnClickListener(polygonTap)
                }
            }
        }
        assemble()
    }

    fun setAirspaceZones(zones: List<AirspaceZone>, enabledTypes: Set<AirspaceZoneType>) {
        val key = Key(zones, enabledTypes)
        if (key == zoneKey) return
        zoneKey = key
        zoneLayer = timed("airspace zones") {
            zones.filter { it.type in enabledTypes }.map { zone ->
                Polygon(mapView).apply {
                    points = if (zone.polygonCoordinates.size >= 3) zone.polygonCoordinates.map { GeoPoint(it.first, it.second) }
                    else Polygon.pointsAsCircle(GeoPoint(zone.centerLat, zone.centerLon), zone.radiusMeters)
                    title = zone.name
                    snippet = zone.description
                    when (zone.type) {
                        AirspaceZoneType.RESTRICTED_ZONE -> style(AndroidColor.argb(70, 218, 54, 51), AndroidColor.argb(255, 218, 54, 51), 3.5f)
                        AirspaceZoneType.AUTHORIZATION_ZONE -> style(AndroidColor.argb(45, 56, 139, 253), AndroidColor.argb(255, 56, 139, 253), 3f)
                        AirspaceZoneType.WARNING_ZONE -> style(AndroidColor.argb(45, 227, 179, 65), AndroidColor.argb(255, 227, 179, 65), 2.5f)
                        AirspaceZoneType.ALTITUDE_ZONE -> style(AndroidColor.argb(45, 56, 139, 253), AndroidColor.argb(255, 56, 139, 253), 2.5f)
                        AirspaceZoneType.SPECIAL_USE -> style(AndroidColor.argb(45, 255, 140, 0), AndroidColor.argb(255, 255, 140, 0), 2.5f)
                    }
                    setOnClickListener(polygonTap)
                }
            }
        }
        assemble()
    }

    fun setAirports(airports: List<AirportCtafResult>, visible: Boolean) {
        val key = Key(airports, visible)
        if (key == airportKey) return
        airportKey = key
        airportLayer = timed("airports") {
            if (!visible) emptyList() else airports.map { airport ->
                Marker(mapView).apply {
                    id = "AIRPORT_${airport.ident}"
                    position = GeoPoint(airport.lat, airport.lon)
                    title = "${airport.ident} - ${airport.name}"
                    snippet = "CTAF/FREQ: ${airport.frequencyMhz} MHz (${airport.type}) • ${String.format(java.util.Locale.US, "%.1f NM", airport.distanceNm)}"
                    icon = markerIcons.airportLabel(airport.ident, airport.frequencyMhz)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { _, _ -> onAirportTap(airport); true }
                }
            }
        }
        assemble()
    }

    /** Fixed order, bottom to top: tap receiver, warning zones, airspace, airports, launch pin. */
    private fun assemble() {
        val overlays = mapView.overlays
        overlays.clear()
        overlays.add(eventsOverlay)
        overlays.addAll(warningLayer)
        overlays.addAll(zoneLayer)
        overlays.addAll(airportLayer)
        overlays.add(userMarker)
        mapView.postInvalidate()
    }

    private fun Polygon.style(fill: Int, outline: Int, width: Float) {
        fillPaint.color = fill
        outlinePaint.color = outline
        outlinePaint.strokeWidth = width
    }

    private inline fun <T : Collection<*>> timed(layer: String, build: () -> T): T {
        val start = SystemClock.elapsedRealtimeNanos()
        val result = build()
        Log.i(TAG, "rebuilt $layer: ${result.size} overlays in ${(SystemClock.elapsedRealtimeNanos() - start) / 1_000_000} ms")
        return result
    }

    /** Identity on the data list, equality on the visibility/filter part. */
    private class Key(val data: List<*>, val filter: Any) {
        override fun equals(other: Any?) = other is Key && other.data === data && other.filter == filter
        override fun hashCode() = System.identityHashCode(data) * 31 + filter.hashCode()
    }

    private companion object {
        const val TAG = "MapOverlays"
        const val USER_MARKER_ID = "USER_MARKER"
        val UNSET = Any()
    }
}

/** Builds marker icons. Implementations cache, since each icon is a bitmap. */
interface MarkerIconFactory {
    fun launchPin(): Drawable
    fun airportLabel(ident: String, frequencyMhz: String): Drawable
}

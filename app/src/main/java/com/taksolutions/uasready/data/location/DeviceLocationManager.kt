package com.taksolutions.uasready.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.taksolutions.uasready.data.repository.CtafLookupHelper
import com.taksolutions.uasready.domain.model.LocationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class DeviceLocationManager(private val context: Context) {

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    companion object {
        private const val TAG = "DeviceLocationManager"
    }

    fun hasLocationPermission(): Boolean {
        val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationInfo? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted.")
            return@withContext null
        }

        val lm = locationManager ?: return@withContext null

        // 1. Try to get last known location first for immediate response
        var bestLocation: Location? = null
        val providers = lm.getProviders(true)
        for (provider in providers) {
            val l = lm.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }

        // If last known location is fresh (< 5 min), use it or continue to request single update
        if (bestLocation != null && (System.currentTimeMillis() - bestLocation.time) < 300_000) {
            Log.i(TAG, "Using fresh last-known location: ${bestLocation.latitude}, ${bestLocation.longitude}")
            return@withContext locationToLocationInfo(bestLocation)
        }

        // 2. Request a fresh single location update
        try {
            val freshLocation = requestSingleUpdate()
            if (freshLocation != null) {
                return@withContext locationToLocationInfo(freshLocation)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting single location update: ${e.message}", e)
        }

        // Fallback to best last known if available
        bestLocation?.let { locationToLocationInfo(it) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleUpdate(): Location? = suspendCancellableCoroutine { cont ->
        val lm = locationManager
        if (lm == null || !hasLocationPermission()) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> LocationManager.PASSIVE_PROVIDER
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.i(TAG, "Fresh GPS fix acquired: ${location.latitude}, ${location.longitude}")
                lm.removeUpdates(this)
                if (cont.isActive) {
                    cont.resume(location)
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {
                if (cont.isActive) {
                    cont.resume(null)
                }
            }
        }

        try {
            lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            cont.invokeOnCancellation {
                lm.removeUpdates(listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request single location update: ${e.message}", e)
            if (cont.isActive) cont.resume(null)
        }
    }

    private suspend fun locationToLocationInfo(location: Location): LocationInfo {
        val lat = location.latitude
        val lon = location.longitude
        val altMeters = if (location.hasAltitude()) location.altitude else 206.0
        val elevationFt = altMeters * 3.28084
        val accuracy = if (location.hasAccuracy()) location.accuracy else 5.0f

        val displayName = reverseGeocode(lat, lon)
        CtafLookupHelper.initialize(context)
        val ctafResult = CtafLookupHelper.findNearestCtaf(lat, lon)

        return LocationInfo(
            latitude = lat,
            longitude = lon,
            elevationFt = elevationFt,
            displayName = displayName,
            accuracyMeters = accuracy,
            isGpsDerived = true,
            ctafFrequency = ctafResult?.frequencyMhz,
            ctafType = ctafResult?.type,
            nearestAirportIdent = ctafResult?.ident,
            nearestAirportName = ctafResult?.name,
            nearestAirportDistanceNm = ctafResult?.distanceNm
        )
    }

    private fun toStateCode(stateName: String?): String? {
        if (stateName.isNullOrBlank()) return null
        if (stateName.length == 2) return stateName.uppercase()
        val usStates = mapOf(
            "alabama" to "AL", "alaska" to "AK", "arizona" to "AZ", "arkansas" to "AR", "california" to "CA",
            "colorado" to "CO", "connecticut" to "CT", "delaware" to "DE", "florida" to "FL", "georgia" to "GA",
            "hawaii" to "HI", "idaho" to "ID", "illinois" to "IL", "indiana" to "IN", "iowa" to "IA",
            "kansas" to "KS", "kentucky" to "KY", "louisiana" to "LA", "maine" to "ME", "maryland" to "MD",
            "massachusetts" to "MA", "michigan" to "MI", "minnesota" to "MN", "mississippi" to "MS", "missouri" to "MO",
            "montana" to "MT", "nebraska" to "NE", "nevada" to "NV", "new hampshire" to "NH", "new jersey" to "NJ",
            "new mexico" to "NM", "new york" to "NY", "north carolina" to "NC", "north dakota" to "ND", "ohio" to "OH",
            "oklahoma" to "OK", "oregon" to "OR", "pennsylvania" to "PA", "rhode island" to "RI", "south carolina" to "SC",
            "south dakota" to "SD", "tennessee" to "TN", "texas" to "TX", "utah" to "UT", "vermont" to "VT",
            "virginia" to "VA", "washington" to "WA", "west virginia" to "WV", "wisconsin" to "WI", "wyoming" to "WY",
            "district of columbia" to "DC"
        )
        return usStates[stateName.trim().lowercase()] ?: stateName
    }

    private fun reverseGeocode(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: address.subAdminArea
                val stateCode = toStateCode(address.adminArea)
                when {
                    city != null && stateCode != null -> "$city, $stateCode"
                    city != null -> city
                    stateCode != null -> stateCode
                    else -> String.format(Locale.US, "%.4f° N, %.4f° W", lat, Math.abs(lon))
                }
            } else {
                String.format(Locale.US, "%.4f° N, %.4f° W", lat, Math.abs(lon))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder error: ${e.message}")
            String.format(Locale.US, "%.4f° N, %.4f° W", lat, Math.abs(lon))
        }
    }
}

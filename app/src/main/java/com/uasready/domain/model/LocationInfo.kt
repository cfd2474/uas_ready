package com.uasready.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val elevationFt: Double = 0.0,
    val displayName: String = "Acquiring GPS...",
    val accuracyMeters: Float = 0.0f,
    val isGpsDerived: Boolean = false,
    val ctafFrequency: String? = null,
    val ctafType: String? = null,
    val nearestAirportIdent: String? = null,
    val nearestAirportName: String? = null,
    val nearestAirportDistanceNm: Double? = null
) {
    val formattedCoordinates: String
        get() = if (latitude == 0.0 && longitude == 0.0) {
            "Searching for satellites..."
        } else {
            String.format(java.util.Locale.US, "%.4f° N, %.4f° W", latitude, Math.abs(longitude))
        }

    val ctafDisplay: String
        get() = when {
            ctafFrequency != null && nearestAirportIdent != null && nearestAirportDistanceNm != null ->
                "CTAF $ctafFrequency MHz • $nearestAirportIdent (${String.format(java.util.Locale.US, "%.1f", nearestAirportDistanceNm)} NM)"
            ctafFrequency != null && nearestAirportIdent != null ->
                "CTAF $ctafFrequency MHz • $nearestAirportIdent"
            ctafFrequency != null ->
                "CTAF $ctafFrequency MHz"
            else ->
                "CTAF 122.800 MHz (Multicom/Unicom)"
        }

    companion object {
        fun defaultLocation(): LocationInfo = LocationInfo(
            latitude = 33.8753,
            longitude = -117.5664,
            elevationFt = 0.0,
            displayName = "Corona, CA",
            accuracyMeters = 0.0f,
            isGpsDerived = false,
            ctafFrequency = "120.6",
            ctafType = "TOWER",
            nearestAirportIdent = "KONT",
            nearestAirportName = "Ontario International Airport",
            nearestAirportDistanceNm = 0.3
        )
    }
}

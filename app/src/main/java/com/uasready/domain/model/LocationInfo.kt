package com.uasready.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val elevationFt: Double = 0.0,
    val displayName: String = "Acquiring GPS...",
    val accuracyMeters: Float = 0.0f,
    val isGpsDerived: Boolean = false
) {
    val formattedCoordinates: String
        get() = if (latitude == 0.0 && longitude == 0.0) {
            "Searching for satellites..."
        } else {
            String.format("%.4f° N, %.4f° W", latitude, Math.abs(longitude))
        }

    companion object {
        fun defaultLocation(): LocationInfo = LocationInfo(
            latitude = 33.8753,
            longitude = -117.5664,
            elevationFt = 0.0,
            displayName = "Acquiring GPS Fix...",
            accuracyMeters = 0.0f,
            isGpsDerived = false
        )
    }
}

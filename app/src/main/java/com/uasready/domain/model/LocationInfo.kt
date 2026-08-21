package com.uasready.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val elevationFt: Double = 678.0,
    val displayName: String = "Corona, CA",
    val accuracyMeters: Float = 5.0f,
    val isGpsDerived: Boolean = true
) {
    val formattedCoordinates: String
        get() = String.format("%.4f° N, %.4f° W", latitude, Math.abs(longitude))

    companion object {
        fun defaultLocation(): LocationInfo = LocationInfo(
            latitude = 33.8753,
            longitude = -117.5664,
            elevationFt = 678.0,
            displayName = "Corona, CA (HQ)"
        )
    }
}

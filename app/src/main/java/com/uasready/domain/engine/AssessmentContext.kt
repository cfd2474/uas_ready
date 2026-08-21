package com.uasready.domain.engine

import com.uasready.domain.model.*

/**
 * Encapsulates the complete flight parameters and environmental data fed into the Rules Engine.
 */
data class AssessmentContext(
    val aircraft: Aircraft,
    val pilot: Pilot,
    val weather: WeatherObservation?,
    val forecast: WeatherForecast?,
    val spaceWeather: SpaceWeather?,
    val airspace: AirspaceInfo?,
    val sunData: SunData?,
    val flightWindow: FlightWindow,
    val location: LocationInfo,
    val plannedAltitudeAglFt: Double = 400.0,
    val hasInternetConnection: Boolean = true
)

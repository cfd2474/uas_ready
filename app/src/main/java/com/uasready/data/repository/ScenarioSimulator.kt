package com.uasready.data.repository

import com.uasready.domain.engine.AssessmentContext
import com.uasready.domain.model.*

enum class SimulationScenario(val title: String, val description: String) {
    LIVE_DATA("Live Telemetry Mode", "Live data from NOAA, Open-Meteo, and FAA"),
    NOMINAL_GO("Nominal GO Scenario", "Calm 8 MPH winds, clear skies, Kp 2.0, Class G, qualified Part 107 pilot"),
    HIGH_WIND_CAUTION("High Gusts CAUTION", "30 MPH gusts approaching DJI Mavic 3T 34 MPH operating limit"),
    WIND_EXCEEDED_NOGO("Wind Limit Exceeded NO-GO", "38 MPH gusts exceeding DJI Mavic 3T 34 MPH maximum operating limit"),
    SEVERE_THUNDERSTORM_NOGO("Thunderstorm Alert NO-GO", "Severe convective thunderstorm with active lightning in operational zone"),
    CONTROLLED_AIRSPACE_NOGO("Controlled Airspace NO-GO", "Class B surface area with mandatory LAANC authorization required"),
    ACTIVE_TFR_NOGO("Active TFR NO-GO", "Presidential VIP Temporary Flight Restriction intersecting flight coordinates"),
    SEVERE_SPACE_WEATHER_NOGO("Geomagnetic Storm Kp=7.5 NO-GO", "Severe G3 space weather storm with degraded GNSS constellation risk"),
    NIGHT_UNQUALIFIED_NOGO("Night Flight Unqualified NO-GO", "Flight window in darkness with unendorsed Part 107 pilot"),
    DETERIORATING_FORECAST_NOGO("Deteriorating Forecast NO-GO", "Calm at launch, but forecast gusts spike to 38 MPH at +45 minutes"),
    STALE_DATA_WARNING("Stale Telemetry CAUTION", "Weather observation data is 90 minutes old")
}

object ScenarioSimulator {

    fun generateContext(
        scenario: SimulationScenario,
        aircraft: Aircraft = Aircraft.getDefault(),
        pilot: Pilot = Pilot.getDefault(),
        location: LocationInfo = LocationInfo.defaultLocation()
    ): AssessmentContext {
        val now = System.currentTimeMillis()
        val flightWindow = FlightWindow.defaultTwoHours(now)

        val defaultSunData = SunData(
            civilDawnEpochMs = now - 4 * 3600 * 1000L,
            sunriseEpochMs = now - 3 * 3600 * 1000L,
            sunsetEpochMs = now + 5 * 3600 * 1000L,
            civilDuskEpochMs = now + 6 * 3600 * 1000L,
            isDaylight = true,
            daylightRemainingMinutes = 300
        )

        val defaultWeather = WeatherObservation(
            temperatureF = 75.0,
            apparentTemperatureF = 75.0,
            windSpeedMph = 8.0,
            windGustMph = 14.0,
            windDirectionDegrees = 240,
            visibilityStatuteMiles = 10.0,
            cloudCoverPercent = 10,
            cloudCeilingFt = 6000.0,
            precipitationProbabilityPercent = 0,
            precipitationRateInchesPerHour = 0.0,
            precipitationType = PrecipitationType.NONE,
            relativeHumidityPercent = 40,
            pressureInHg = 29.92,
            thunderstormProbabilityPercent = 0,
            conditionsDescription = "Clear & Optimal",
            timestampEpochMs = now,
            sourceName = "Simulated NOAA Telemetry"
        )

        val defaultSpaceWeather = SpaceWeather(
            currentKpIndex = 2.0,
            forecastMaxKpIndex = 2.3,
            geomagneticStormScale = GeomagneticStormScale.NONE,
            gnssRiskLevel = GnssRiskLevel.LOW,
            timestampEpochMs = now,
            sourceName = "Simulated NOAA SWPC"
        )

        val defaultAirspace = AirspaceInfo(
            primaryClass = AirspaceClass.CLASS_G,
            controlledAirspaceAuthorizationRequired = false,
            uasFacilityMapMaxAltitudeFt = 400.0,
            activeTfrs = emptyList(),
            timestampEpochMs = now,
            sourceName = "Simulated FAA Airspace"
        )

        val defaultGnss = GnssEstimation.estimate(
            latitude = location.latitude,
            elevationFt = location.elevationFt,
            kpIndex = defaultSpaceWeather.currentKpIndex
        )

        return when (scenario) {
            SimulationScenario.LIVE_DATA, SimulationScenario.NOMINAL_GO -> {
                AssessmentContext(
                    aircraft = aircraft,
                    pilot = pilot,
                    weather = defaultWeather,
                    forecast = null,
                    spaceWeather = defaultSpaceWeather,
                    airspace = defaultAirspace,
                    sunData = defaultSunData,
                    flightWindow = flightWindow,
                    location = location,
                    gnss = defaultGnss
                )
            }

            SimulationScenario.HIGH_WIND_CAUTION -> {
                val weather = defaultWeather.copy(
                    windSpeedMph = 18.0,
                    windGustMph = 23.5, // Approaching 26.8 MPH operating limit
                    conditionsDescription = "High Gusty Winds"
                )
                AssessmentContext(
                    aircraft = aircraft,
                    pilot = pilot,
                    weather = weather,
                    forecast = null,
                    spaceWeather = defaultSpaceWeather,
                    airspace = defaultAirspace,
                    sunData = defaultSunData,
                    flightWindow = flightWindow,
                    location = location,
                    gnss = defaultGnss
                )
            }

            SimulationScenario.WIND_EXCEEDED_NOGO -> {
                val weather = defaultWeather.copy(
                    windSpeedMph = 28.0,
                    windGustMph = 32.0, // Exceeds 26.8 MPH maximum operating limit
                    conditionsDescription = "Dangerous High Wind Gusts"
                )
                AssessmentContext(
                    aircraft = aircraft,
                    pilot = pilot,
                    weather = weather,
                    forecast = null,
                    spaceWeather = defaultSpaceWeather,
                    airspace = defaultAirspace,
                    sunData = defaultSunData,
                    flightWindow = flightWindow,
                    location = location,
                    gnss = defaultGnss
                )
            }

            SimulationScenario.SEVERE_THUNDERSTORM_NOGO -> {
                val weather = defaultWeather.copy(
                    precipitationType = PrecipitationType.THUNDERSTORM,
                    thunderstormProbabilityPercent = 90,
                    precipitationRateInchesPerHour = 0.8,
                    cloudCeilingFt = 800.0,
                    visibilityStatuteMiles = 2.0,
                    conditionsDescription = "Severe Thunderstorm & Heavy Rain"
                )
                AssessmentContext(
                    aircraft = aircraft,
                    pilot = pilot,
                    weather = weather,
                    forecast = null,
                    spaceWeather = defaultSpaceWeather,
                    airspace = defaultAirspace,
                    sunData = defaultSunData,
                    flightWindow = flightWindow,
                    location = location,
                    gnss = defaultGnss
                )
            }

            SimulationScenario.CONTROLLED_AIRSPACE_NOGO -> {
                val airspace = defaultAirspace.copy(
                    primaryClass = AirspaceClass.CLASS_B,
                    controlledAirspaceAuthorizationRequired = true,
                    uasFacilityMapMaxAltitudeFt = 0.0
                )
                AssessmentContext(
                    aircraft = aircraft,
                    pilot = pilot,
                    weather = defaultWeather,
                    forecast = null,
                    spaceWeather = defaultSpaceWeather,
                    airspace = airspace,
                    sunData = defaultSunData,
                    flightWindow = flightWindow,
                    location = location,
                    gnss = defaultGnss
                )
            }

            SimulationScenario.ACTIVE_TFR_NOGO -> {
                val tfr = TemporaryFlightRestriction(
                    id = "TFR-08-4921",
                    description = "VIP Security Movement Area",
                    type = "VIP Security",
                    minAltitudeFt = 0.0,
                    maxAltitudeFt = 18000.0,
                    effectiveStartEpochMs = flightWindow.startEpochMs - 100000,
                    effectiveEndEpochMs = flightWindow.endEpochMs + 100000,
                    radiusNm = 5.0
                )
                val airspace = defaultAirspace.copy(activeTfrs = listOf(tfr))
                AssessmentContext(
                    aircraft = aircraft,
                    pilot = pilot,
                    weather = defaultWeather,
                    forecast = null,
                    spaceWeather = defaultSpaceWeather,
                    airspace = airspace,
                    sunData = defaultSunData,
                    flightWindow = flightWindow,
                    location = location,
                    gnss = defaultGnss
                )
            }

            SimulationScenario.SEVERE_SPACE_WEATHER_NOGO -> {
                val spaceWeather = defaultSpaceWeather.copy(
                    currentKpIndex = 7.5,
                    forecastMaxKpIndex = 8.0,
                    geomagneticStormScale = GeomagneticStormScale.G3_STRONG,
                    gnssRiskLevel = GnssRiskLevel.SEVERE,
                    activeAlerts = listOf("NOAA SWPC Strong Geomagnetic Storm G3 Warning")
                )
                val stormGnss = GnssEstimation.estimate(
                    latitude = location.latitude,
                    elevationFt = location.elevationFt,
                    kpIndex = 7.5
                )
                AssessmentContext(
                    aircraft = aircraft,
                    pilot = pilot,
                    weather = defaultWeather,
                    forecast = null,
                    spaceWeather = spaceWeather,
                    airspace = defaultAirspace,
                    sunData = defaultSunData,
                    flightWindow = flightWindow,
                    location = location,
                    gnss = stormGnss
                )
            }

            SimulationScenario.NIGHT_UNQUALIFIED_NOGO -> {
                val nightPilot = pilot.copy(
                    activeAuthority = PilotAuthorityType.PUBLIC_COA
                )
                val nightSun = defaultSunData.copy(
                    sunriseEpochMs = flightWindow.startEpochMs - 14 * 3600 * 1000L,
                    sunsetEpochMs = flightWindow.startEpochMs - 2 * 3600 * 1000L,
                    civilDuskEpochMs = flightWindow.startEpochMs - 90 * 60 * 1000L,
                    isDaylight = false
                )
                AssessmentContext(
                    aircraft = aircraft,
                    pilot = nightPilot,
                    weather = defaultWeather,
                    forecast = null,
                    spaceWeather = defaultSpaceWeather,
                    airspace = defaultAirspace,
                    sunData = nightSun,
                    flightWindow = flightWindow,
                    location = location,
                    gnss = defaultGnss
                )
            }

            SimulationScenario.DETERIORATING_FORECAST_NOGO -> {
                val hourly = listOf(
                    HourlyForecastInterval(
                        timestampEpochMs = flightWindow.startEpochMs,
                        temperatureF = 72.0,
                        windSpeedMph = 14.0,
                        windGustMph = 20.0,
                        windDirectionDegrees = 270,
                        visibilityStatuteMiles = 10.0,
                        cloudCeilingFt = 5000.0,
                        precipitationProbabilityPercent = 10,
                        precipitationRateInchesPerHour = 0.0,
                        conditionsDescription = "Clear"
                    ),
                    HourlyForecastInterval(
                        timestampEpochMs = flightWindow.startEpochMs + 30 * 60 * 1000L,
                        temperatureF = 65.0,
                        windSpeedMph = 28.0,
                        windGustMph = 39.0, // Exceeds limit at +30m into flight window (< 60m NO-GO)
                        windDirectionDegrees = 290,
                        visibilityStatuteMiles = 3.0,
                        cloudCeilingFt = 1200.0,
                        precipitationProbabilityPercent = 85,
                        precipitationRateInchesPerHour = 0.4,
                        precipitationType = PrecipitationType.RAIN,
                        conditionsDescription = "High Winds & Rain Squall"
                    )
                )
                val forecast = WeatherForecast(
                    intervals = hourly,
                    generatedAtEpochMs = now,
                    sourceName = "Simulated NOAA GFS Forecast"
                )
                AssessmentContext(
                    aircraft = aircraft,
                    pilot = pilot,
                    weather = defaultWeather,
                    forecast = forecast,
                    spaceWeather = defaultSpaceWeather,
                    airspace = defaultAirspace,
                    sunData = defaultSunData,
                    flightWindow = flightWindow,
                    location = location,
                    gnss = defaultGnss
                )
            }

            SimulationScenario.STALE_DATA_WARNING -> {
                val staleWeather = defaultWeather.copy(
                    timestampEpochMs = now - (3 * 3600 * 1000L), // 3 hours old
                    isStale = true
                )
                AssessmentContext(
                    aircraft = aircraft,
                    pilot = pilot,
                    weather = staleWeather,
                    forecast = null,
                    spaceWeather = defaultSpaceWeather,
                    airspace = defaultAirspace,
                    sunData = defaultSunData,
                    flightWindow = flightWindow,
                    location = location,
                    gnss = defaultGnss
                )
            }
        }
    }
}

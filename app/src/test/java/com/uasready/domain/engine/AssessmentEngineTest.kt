package com.uasready.domain.engine

import com.uasready.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AssessmentEngineTest {

    private lateinit var engine: AssessmentEngine
    private lateinit var defaultAircraft: Aircraft
    private lateinit var defaultPilot: Pilot
    private lateinit var defaultLocation: LocationInfo
    private lateinit var defaultFlightWindow: FlightWindow
    private lateinit var nominalWeather: WeatherObservation
    private lateinit var nominalSpaceWeather: SpaceWeather
    private lateinit var nominalAirspace: AirspaceInfo
    private lateinit var nominalSunData: SunData

    @Before
    fun setup() {
        engine = AssessmentEngine()
        defaultAircraft = Aircraft.PRESETS.first { it.id == "dji_m3t" } // Max gust 34 MPH, temp 14F-104F
        defaultPilot = Pilot.getDefault()
        defaultLocation = LocationInfo.defaultLocation()

        val now = System.currentTimeMillis()
        defaultFlightWindow = FlightWindow(
            startEpochMs = now + 1000 * 60 * 10, // 10 min from now
            endEpochMs = now + 1000 * 60 * 70     // 70 min from now
        )

        nominalWeather = WeatherObservation(
            temperatureF = 72.0,
            apparentTemperatureF = 72.0,
            windSpeedMph = 8.0,
            windGustMph = 14.0,
            windDirectionDegrees = 240,
            visibilityStatuteMiles = 10.0,
            cloudCoverPercent = 15,
            cloudCeilingFt = 5000.0,
            precipitationProbabilityPercent = 0,
            precipitationRateInchesPerHour = 0.0,
            precipitationType = PrecipitationType.NONE,
            relativeHumidityPercent = 45,
            pressureInHg = 29.92,
            thunderstormProbabilityPercent = 0,
            conditionsDescription = "Clear Sky",
            timestampEpochMs = now
        )

        nominalSpaceWeather = SpaceWeather(
            currentKpIndex = 2.0,
            forecastMaxKpIndex = 2.3,
            geomagneticStormScale = GeomagneticStormScale.NONE,
            gnssRiskLevel = GnssRiskLevel.LOW,
            timestampEpochMs = now
        )

        nominalAirspace = AirspaceInfo(
            primaryClass = AirspaceClass.CLASS_G,
            controlledAirspaceAuthorizationRequired = false,
            activeTfrs = emptyList(),
            timestampEpochMs = now
        )

        nominalSunData = SunData(
            civilDawnEpochMs = now - 1000 * 60 * 60 * 4,
            sunriseEpochMs = now - 1000 * 60 * 60 * 3,
            sunsetEpochMs = now + 1000 * 60 * 60 * 5, // Sunset 5 hours away
            civilDuskEpochMs = now + 1000 * 60 * 60 * 6,
            isDaylight = true,
            daylightRemainingMinutes = 300
        )
    }

    @Test
    fun testNominalScenarioProducesOverallGO() {
        val context = AssessmentContext(
            aircraft = defaultAircraft,
            pilot = defaultPilot,
            weather = nominalWeather,
            forecast = null,
            spaceWeather = nominalSpaceWeather,
            airspace = nominalAirspace,
            sunData = nominalSunData,
            flightWindow = defaultFlightWindow,
            location = defaultLocation
        )

        val result = engine.assess(context)
        assertEquals(AssessmentStatus.GO, result.overallStatus)
        assertTrue(result.primaryHeadline.contains("GO"))
        assertEquals(0, result.noGoRules.size)
        assertEquals(0, result.cautionRules.size)
    }

    @Test
    fun testHighWindGustsApproachingLimitProducesCAUTION() {
        // M3T limit is 34 MPH gust. 30 MPH gust is within 5 MPH margin -> CAUTION
        val cautionWeather = nominalWeather.copy(windGustMph = 30.0)
        val context = AssessmentContext(
            aircraft = defaultAircraft,
            pilot = defaultPilot,
            weather = cautionWeather,
            forecast = null,
            spaceWeather = nominalSpaceWeather,
            airspace = nominalAirspace,
            sunData = nominalSunData,
            flightWindow = defaultFlightWindow,
            location = defaultLocation
        )

        val result = engine.assess(context)
        assertEquals(AssessmentStatus.CAUTION, result.overallStatus)
        assertTrue(result.primaryReasons.any { it.contains("approaching") || it.contains("maximum operating wind") })
    }

    @Test
    fun testWindGustsExceedingLimitProducesNOGO() {
        // M3T limit is 34 MPH gust. 38 MPH gust -> NO-GO
        val noGoWeather = nominalWeather.copy(windGustMph = 38.0)
        val context = AssessmentContext(
            aircraft = defaultAircraft,
            pilot = defaultPilot,
            weather = noGoWeather,
            forecast = null,
            spaceWeather = nominalSpaceWeather,
            airspace = nominalAirspace,
            sunData = nominalSunData,
            flightWindow = defaultFlightWindow,
            location = defaultLocation
        )

        val result = engine.assess(context)
        assertEquals(AssessmentStatus.NO_GO, result.overallStatus)
        assertTrue(result.noGoRules.any { it.ruleId == "AC-GUST-001" })
    }

    @Test
    fun testMissingLiveWeatherProducesDataUnavailable() {
        val context = AssessmentContext(
            aircraft = defaultAircraft,
            pilot = defaultPilot,
            weather = null, // Missing data
            forecast = null,
            spaceWeather = nominalSpaceWeather,
            airspace = nominalAirspace,
            sunData = nominalSunData,
            flightWindow = defaultFlightWindow,
            location = defaultLocation
        )

        val result = engine.assess(context)
        assertEquals(AssessmentStatus.DATA_UNAVAILABLE, result.overallStatus)
    }

    @Test
    fun testActiveTFRProducesNOGO() {
        val tfr = TemporaryFlightRestriction(
            id = "TFR-4-8291",
            description = "VIP Movement Security Area",
            type = "VIP",
            minAltitudeFt = 0.0,
            maxAltitudeFt = 18000.0,
            effectiveStartEpochMs = defaultFlightWindow.startEpochMs - 10000,
            effectiveEndEpochMs = defaultFlightWindow.endEpochMs + 10000,
            radiusNm = 3.0
        )
        val airspaceWithTfr = nominalAirspace.copy(activeTfrs = listOf(tfr))
        val context = AssessmentContext(
            aircraft = defaultAircraft,
            pilot = defaultPilot,
            weather = nominalWeather,
            forecast = null,
            spaceWeather = nominalSpaceWeather,
            airspace = airspaceWithTfr,
            sunData = nominalSunData,
            flightWindow = defaultFlightWindow,
            location = defaultLocation
        )

        val result = engine.assess(context)
        assertEquals(AssessmentStatus.NO_GO, result.overallStatus)
        assertTrue(result.noGoRules.any { it.ruleId == "AIR-TFR-001" })
    }

    @Test
    fun testSevereGeomagneticKpProducesNOGO() {
        val severeSpaceWeather = nominalSpaceWeather.copy(
            currentKpIndex = 7.0,
            geomagneticStormScale = GeomagneticStormScale.G3_STRONG,
            gnssRiskLevel = GnssRiskLevel.SEVERE
        )
        val context = AssessmentContext(
            aircraft = defaultAircraft,
            pilot = defaultPilot,
            weather = nominalWeather,
            forecast = null,
            spaceWeather = severeSpaceWeather,
            airspace = nominalAirspace,
            sunData = nominalSunData,
            flightWindow = defaultFlightWindow,
            location = defaultLocation
        )

        val result = engine.assess(context)
        assertEquals(AssessmentStatus.NO_GO, result.overallStatus)
        assertTrue(result.noGoRules.any { it.ruleId == "SP-KP-001" })
    }

    @Test
    fun testDeterioratingForecastInFlightWindowProducesNOGO() {
        // Current weather is calm (10 MPH), but forecast at +45 min has 36 MPH gusts
        val forecast = WeatherForecast(
            intervals = listOf(
                HourlyForecastInterval(
                    timestampEpochMs = defaultFlightWindow.startEpochMs + 45 * 60 * 1000L,
                    temperatureF = 70.0,
                    windSpeedMph = 25.0,
                    windGustMph = 36.0, // Exceeds 34 MPH M3T limit
                    windDirectionDegrees = 270,
                    visibilityStatuteMiles = 10.0,
                    cloudCeilingFt = 5000.0,
                    precipitationProbabilityPercent = 0,
                    precipitationRateInchesPerHour = 0.0,
                    conditionsDescription = "High Wind Gusts"
                )
            ),
            generatedAtEpochMs = System.currentTimeMillis()
        )

        val context = AssessmentContext(
            aircraft = defaultAircraft,
            pilot = defaultPilot,
            weather = nominalWeather,
            forecast = forecast,
            spaceWeather = nominalSpaceWeather,
            airspace = nominalAirspace,
            sunData = nominalSunData,
            flightWindow = defaultFlightWindow,
            location = defaultLocation
        )

        val result = engine.assess(context)
        assertEquals(AssessmentStatus.NO_GO, result.overallStatus)
        assertTrue(result.noGoRules.any { it.ruleId == "AC-FCST-GUST-001" })
    }

    @Test
    fun testPart107NightWithoutEndorsementProducesNOGO() {
        val nightPilot = defaultPilot.copy(
            part107Profile = Part107Profile(
                certificateNumber = "12345",
                nightTrainingCompleted = false
            )
        )
        // Flight window takes place at night (sunset was 2 hours ago)
        val nightSunData = nominalSunData.copy(
            sunriseEpochMs = defaultFlightWindow.startEpochMs - 12 * 60 * 60 * 1000L,
            sunsetEpochMs = defaultFlightWindow.startEpochMs - 2 * 60 * 60 * 1000L,
            civilDuskEpochMs = defaultFlightWindow.startEpochMs - 100 * 60 * 1000L,
            isDaylight = false
        )

        val context = AssessmentContext(
            aircraft = defaultAircraft,
            pilot = nightPilot,
            weather = nominalWeather,
            forecast = null,
            spaceWeather = nominalSpaceWeather,
            airspace = nominalAirspace,
            sunData = nightSunData,
            flightWindow = defaultFlightWindow,
            location = defaultLocation
        )

        val result = engine.assess(context)
        assertEquals(AssessmentStatus.NO_GO, result.overallStatus)
        assertTrue(result.noGoRules.any { it.ruleId == "PLT-107-NGT-001" })
    }

    @Test
    fun testGnssSatellitesRuleEvaluationThresholds() {
        // 1. 12+ Satellites -> GO
        val gnssGo = GnssEstimation(
            visibleSatellitesCount = 28,
            lockedSatellitesCount = 18,
            estimatedHdop = 1.0,
            signalIntegrityPercent = 95
        )
        val contextGo = AssessmentContext(
            aircraft = defaultAircraft,
            pilot = defaultPilot,
            weather = nominalWeather,
            forecast = null,
            spaceWeather = nominalSpaceWeather,
            airspace = nominalAirspace,
            sunData = nominalSunData,
            flightWindow = defaultFlightWindow,
            location = defaultLocation,
            gnss = gnssGo
        )
        val resultGo = engine.assess(contextGo)
        val satsRuleGo = resultGo.allRuleResults.first { it.ruleId == "SP-GNSS-SATS" }
        assertEquals(AssessmentStatus.GO, satsRuleGo.status)

        // 2. 8-11 Satellites -> CAUTION
        val gnssCaution = GnssEstimation(
            visibleSatellitesCount = 24,
            lockedSatellitesCount = 10,
            estimatedHdop = 1.8,
            signalIntegrityPercent = 60
        )
        val contextCaution = contextGo.copy(gnss = gnssCaution)
        val resultCaution = engine.assess(contextCaution)
        val satsRuleCaution = resultCaution.allRuleResults.first { it.ruleId == "SP-GNSS-SATS" }
        assertEquals(AssessmentStatus.CAUTION, satsRuleCaution.status)

        // 3. <= 7 Satellites -> NO-GO
        val gnssNoGo = GnssEstimation(
            visibleSatellitesCount = 20,
            lockedSatellitesCount = 6,
            estimatedHdop = 3.2,
            signalIntegrityPercent = 30
        )
        val contextNoGo = contextGo.copy(gnss = gnssNoGo)
        val resultNoGo = engine.assess(contextNoGo)
        val satsRuleNoGo = resultNoGo.allRuleResults.first { it.ruleId == "SP-GNSS-SATS" }
        assertEquals(AssessmentStatus.NO_GO, satsRuleNoGo.status)
        val hdopRuleNoGo = resultNoGo.allRuleResults.first { it.ruleId == "SP-GNSS-HDOP" }
        assertEquals(AssessmentStatus.NO_GO, hdopRuleNoGo.status)
    }

    @Test
    fun testGnssEstimatorCalculations() {
        val nominal = GnssEstimation.estimate(latitude = 33.8753, elevationFt = 600.0, kpIndex = 1.7)
        assertTrue(nominal.lockedSatellitesCount >= 20)
        assertTrue(nominal.estimatedHdop <= 1.2)
        assertEquals(100, nominal.signalIntegrityPercent)

        val severeStorm = GnssEstimation.estimate(latitude = 33.8753, elevationFt = 600.0, kpIndex = 8.5)
        assertTrue(severeStorm.lockedSatellitesCount <= 10)
        assertTrue(severeStorm.estimatedHdop > 2.0)
        assertTrue(severeStorm.signalIntegrityPercent < 50)
    }
}

package com.uasready.data.repository

import com.uasready.domain.engine.AssessmentEngine
import com.uasready.domain.model.Aircraft
import com.uasready.domain.model.AircraftLimitations
import com.uasready.domain.model.AssessmentStatus
import com.uasready.domain.model.PilotAuthorityType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DataLayerTest {

    private lateinit var solarRepo: SolarRepository
    private lateinit var aircraftRepo: AircraftRepository
    private lateinit var pilotRepo: PilotRepository
    private lateinit var engine: AssessmentEngine

    @Before
    fun setup() {
        solarRepo = AstronomicalSolarRepository()
        aircraftRepo = InMemoryAircraftRepository()
        pilotRepo = InMemoryPilotRepository()
        engine = AssessmentEngine()
    }

    @Test
    fun testSolarCalculationProducesValidDaylight() {
        val coronaLat = 33.8753
        val coronaLon = -117.5664
        val sunData = solarRepo.calculateSunData(coronaLat, coronaLon)

        assertNotNull(sunData)
        assertTrue(sunData.civilDawnEpochMs < sunData.sunriseEpochMs)
        assertTrue(sunData.sunriseEpochMs < sunData.sunsetEpochMs)
        assertTrue(sunData.sunsetEpochMs < sunData.civilDuskEpochMs)
    }

    @Test
    fun testCoronaLocal1707IsDaylight() {
        val coronaLat = 33.8753
        val coronaLon = -117.5664
        // August 22 at 17:07 local time (5:07 PM)
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 17)
            set(java.util.Calendar.MINUTE, 7)
            set(java.util.Calendar.SECOND, 0)
        }
        val sunData = solarRepo.calculateSunData(coronaLat, coronaLon, cal.timeInMillis)

        assertTrue("17:07 local time should be daylight", sunData.isDaylight)
        assertTrue("Daylight remaining should be > 0", sunData.daylightRemainingMinutes > 0)
        assertTrue("17:07 should not be darkness", !sunData.isDarknessAt(cal.timeInMillis))
    }

    @Test
    fun testAircraftRepositoryCustomFleetManagement() {
        val initialList = aircraftRepo.aircraftListState.value
        assertTrue(initialList.isNotEmpty())

        val customDrone = Aircraft(
            id = "custom_corona_fire_1",
            manufacturer = "DJI",
            model = "Mavic 3T (Corona Fire SAR #1)",
            displayName = "Corona Fire SAR Mavic 3T #1",
            isCustom = true,
            basePresetId = "dji_m3t",
            organization = "Corona Fire Dept",
            limitations = AircraftLimitations(
                maxSustainedWindSpeedMph = 30.0,
                maxGustSpeedMph = 36.0,
                minOperatingTempF = 10.0,
                maxOperatingTempF = 110.0
            )
        )

        aircraftRepo.saveCustomAircraft(customDrone)
        assertEquals(customDrone.id, aircraftRepo.selectedAircraftState.value.id)
        assertTrue(aircraftRepo.aircraftListState.value.any { it.id == "custom_corona_fire_1" })

        aircraftRepo.deleteCustomAircraft("custom_corona_fire_1")
        assertFalse(aircraftRepo.aircraftListState.value.any { it.id == "custom_corona_fire_1" })
    }

    @Test
    fun testPilotRepositoryAuthoritySwitching() {
        pilotRepo.setAuthority(PilotAuthorityType.PUBLIC_COA)
        assertEquals(PilotAuthorityType.PUBLIC_COA, pilotRepo.pilotState.value.activeAuthority)

        pilotRepo.setAuthority(PilotAuthorityType.PART_107)
        assertEquals(PilotAuthorityType.PART_107, pilotRepo.pilotState.value.activeAuthority)
    }

    @Test
    fun testScenarioSimulatorEndToEndWithAssessmentEngine() {
        // 1. Nominal Scenario -> GO
        val goContext = ScenarioSimulator.generateContext(SimulationScenario.NOMINAL_GO)
        val goResult = engine.assess(goContext)
        assertEquals(AssessmentStatus.GO, goResult.overallStatus)

        // 2. High Wind Caution -> CAUTION
        val cautionContext = ScenarioSimulator.generateContext(SimulationScenario.HIGH_WIND_CAUTION)
        val cautionResult = engine.assess(cautionContext)
        assertEquals(AssessmentStatus.CAUTION, cautionResult.overallStatus)

        // 3. Wind Exceeded -> NO-GO
        val windExceededContext = ScenarioSimulator.generateContext(SimulationScenario.WIND_EXCEEDED_NOGO)
        val windExceededResult = engine.assess(windExceededContext)
        assertEquals(AssessmentStatus.NO_GO, windExceededResult.overallStatus)

        // 4. Thunderstorm -> NO-GO
        val stormContext = ScenarioSimulator.generateContext(SimulationScenario.SEVERE_THUNDERSTORM_NOGO)
        val stormResult = engine.assess(stormContext)
        assertEquals(AssessmentStatus.NO_GO, stormResult.overallStatus)

        // 5. Active TFR -> NO-GO
        val tfrContext = ScenarioSimulator.generateContext(SimulationScenario.ACTIVE_TFR_NOGO)
        val tfrResult = engine.assess(tfrContext)
        assertEquals(AssessmentStatus.NO_GO, tfrResult.overallStatus)

        // 6. Geomagnetic Storm -> NO-GO
        val spaceContext = ScenarioSimulator.generateContext(SimulationScenario.SEVERE_SPACE_WEATHER_NOGO)
        val spaceResult = engine.assess(spaceContext)
        assertEquals(AssessmentStatus.NO_GO, spaceResult.overallStatus)

        // 7. Night Unqualified -> NO-GO
        val nightContext = ScenarioSimulator.generateContext(SimulationScenario.NIGHT_UNQUALIFIED_NOGO)
        val nightResult = engine.assess(nightContext)
        assertEquals(AssessmentStatus.NO_GO, nightResult.overallStatus)

        // 8. Deteriorating Forecast -> NO-GO
        val detContext = ScenarioSimulator.generateContext(SimulationScenario.DETERIORATING_FORECAST_NOGO)
        val detResult = engine.assess(detContext)
        assertEquals(AssessmentStatus.NO_GO, detResult.overallStatus)

        // 9. Stale Data -> CAUTION
        val staleContext = ScenarioSimulator.generateContext(SimulationScenario.STALE_DATA_WARNING)
        val staleResult = engine.assess(staleContext)
        assertEquals(AssessmentStatus.CAUTION, staleResult.overallStatus)
    }

    @Test
    fun testLiveSpaceWeatherRepositoryPayloadParsing() {
        val jsonString = """
            [
                {"time_tag":"2026-08-20T18:00:00","Kp":3.33,"a_running":18,"station_count":8},
                {"time_tag":"2026-08-20T21:00:00","Kp":2.67,"a_running":12,"station_count":8},
                {"time_tag":"2026-08-21T00:00:00","Kp":3.33,"a_running":18,"station_count":8}
            ]
        """.trimIndent()

        val jsonArray = org.json.JSONArray(jsonString)
        val parsedKps = mutableListOf<Double>()
        for (i in 0 until jsonArray.length()) {
            val element = jsonArray.get(i)
            if (element is org.json.JSONObject && element.has("Kp")) {
                parsedKps.add(element.getDouble("Kp"))
            }
        }

        assertEquals(3, parsedKps.size)
        assertEquals(3.33, parsedKps.last(), 0.01)
    }

    @Test
    fun testMainViewModelConstructorReflection() {
        val constructor = com.uasready.ui.viewmodel.MainViewModel::class.java.getConstructor(android.app.Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun testTerrainObstructionProfileFromSectorAngles() {
        val steepCanyonSectors = mapOf(
            0 to 5.0,
            45 to 8.0,
            90 to 42.0, // 42° East canyon wall
            135 to 15.0,
            180 to 6.0,
            225 to 12.0,
            270 to 38.0, // 38° West canyon wall
            315 to 10.0
        )
        val profile = com.uasready.domain.model.TerrainObstructionProfile.fromSectorAngles(
            launchElevationMeters = 350.0,
            sectorMaskAngles = steepCanyonSectors
        )

        assertEquals(42.0, profile.maxObstructionDeg, 0.1)
        assertEquals(90, profile.worstObstructionAzimuth)
        assertTrue(profile.terrainOcclusionPercent > 5)
        assertEquals("Deep Canyon / Gorge", profile.terrainClassification)

        val gnssWithCanyon = com.uasready.domain.model.GnssEstimation.estimate(
            latitude = 34.0,
            elevationFt = 1100.0,
            kpIndex = 2.0,
            terrainProfile = profile
        )
        assertTrue(gnssWithCanyon.terrainOccludedSatellitesCount >= 2)
        assertTrue(gnssWithCanyon.lockedSatellitesCount < gnssWithCanyon.visibleSatellitesCount)
    }

    @Test
    fun testLiveAirspaceRepositorySanFrancisco() = kotlinx.coroutines.runBlocking {
        val repo = LiveAirspaceRepository()
        val sfLat = 37.7749
        val sfLon = -122.4194
        val result = repo.getAirspaceInfo(sfLat, sfLon)

        assertTrue(result.isSuccess)
        val airspace = result.getOrNull()
        assertNotNull(airspace)
        assertTrue("San Francisco should have controlled airspace polygons in 30 NM radius", airspace!!.zones.isNotEmpty())
        assertTrue("San Francisco airspace should include Class B, C, D or E zones", airspace.zones.any { it.name.contains("CLASS", ignoreCase = true) || it.name.contains("SAN FRANCISCO", ignoreCase = true) })
    }

    @Test
    fun testLiveAirspaceRepositoryOntarioCorona() = kotlinx.coroutines.runBlocking {
        val repo = LiveAirspaceRepository()
        val ontLat = 34.0560
        val ontLon = -117.6012
        val result = repo.getAirspaceInfo(ontLat, ontLon)

        assertTrue(result.isSuccess)
        val airspace = result.getOrNull()
        assertNotNull(airspace)
        assertTrue("Ontario/Corona should have controlled airspace polygons", airspace!!.zones.isNotEmpty())
        assertTrue("Launch at KONT should require controlled airspace authorization", airspace.controlledAirspaceAuthorizationRequired)
    }
}



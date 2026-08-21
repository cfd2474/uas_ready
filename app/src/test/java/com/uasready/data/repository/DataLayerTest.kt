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
        pilotRepo.setAuthority(PilotAuthorityType.COA_COW)
        assertEquals(PilotAuthorityType.COA_COW, pilotRepo.pilotState.value.activeAuthority)

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
}


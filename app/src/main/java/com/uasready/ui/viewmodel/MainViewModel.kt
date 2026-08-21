package com.uasready.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uasready.data.repository.*
import com.uasready.domain.engine.AssessmentContext
import com.uasready.domain.engine.AssessmentEngine
import com.uasready.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val selectedAircraft: Aircraft = Aircraft.getDefault(),
    val allAircraft: List<Aircraft> = Aircraft.PRESETS,
    val currentPilot: Pilot = Pilot.getDefault(),
    val currentLocation: LocationInfo = LocationInfo.defaultLocation(),
    val flightWindow: FlightWindow = FlightWindow.defaultTwoHours(),
    val plannedAltitudeAglFt: Double = 400.0,
    val isLiveLoading: Boolean = false,
    val liveErrorMessage: String? = null,
    val currentScenario: SimulationScenario = SimulationScenario.NOMINAL_GO,
    val assessmentResult: AssessmentResult? = null,
    val selectedCategoryFilter: AssessmentCategory? = null
)

class MainViewModel(
    private val weatherRepo: WeatherRepository = LiveWeatherRepository(),
    private val spaceWeatherRepo: SpaceWeatherRepository = LiveSpaceWeatherRepository(),
    private val solarRepo: SolarRepository = AstronomicalSolarRepository(),
    private val airspaceRepo: AirspaceRepository = LiveAirspaceRepository(),
    private val aircraftRepo: AircraftRepository = InMemoryAircraftRepository(),
    private val pilotRepo: PilotRepository = InMemoryPilotRepository(),
    private val assessmentEngine: AssessmentEngine = AssessmentEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Collect aircraft and pilot state
        viewModelScope.launch {
            aircraftRepo.aircraftListState.collect { list ->
                _uiState.update { it.copy(allAircraft = list) }
            }
        }
        viewModelScope.launch {
            aircraftRepo.selectedAircraftState.collect { selected ->
                _uiState.update { it.copy(selectedAircraft = selected) }
                reevaluateAssessment()
            }
        }
        viewModelScope.launch {
            pilotRepo.pilotState.collect { pilot ->
                _uiState.update { it.copy(currentPilot = pilot) }
                reevaluateAssessment()
            }
        }

        // Initial evaluation
        reevaluateAssessment()
    }

    fun selectScenario(scenario: SimulationScenario) {
        _uiState.update { it.copy(currentScenario = scenario) }
        if (scenario == SimulationScenario.LIVE_DATA) {
            fetchLiveData()
        } else {
            val context = ScenarioSimulator.generateContext(
                scenario = scenario,
                aircraft = _uiState.value.selectedAircraft,
                pilot = _uiState.value.currentPilot,
                location = _uiState.value.currentLocation
            )
            val result = assessmentEngine.assess(context)
            _uiState.update { it.copy(assessmentResult = result, liveErrorMessage = null) }
        }
    }

    fun fetchLiveData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLiveLoading = true, liveErrorMessage = null) }
            val state = _uiState.value
            val lat = state.currentLocation.latitude
            val lon = state.currentLocation.longitude

            val weatherResult = weatherRepo.getWeatherData(lat, lon)
            val spaceResult = spaceWeatherRepo.getSpaceWeather()
            val airspaceResult = airspaceRepo.getAirspaceInfo(lat, lon)
            val sunData = solarRepo.calculateSunData(lat, lon, System.currentTimeMillis())

            val weatherPair = weatherResult.getOrNull()
            val spaceWeather = spaceResult.getOrNull()
            val airspace = airspaceResult.getOrNull()

            val context = AssessmentContext(
                aircraft = state.selectedAircraft,
                pilot = state.currentPilot,
                weather = weatherPair?.first,
                forecast = weatherPair?.second,
                spaceWeather = spaceWeather,
                airspace = airspace,
                sunData = sunData,
                flightWindow = state.flightWindow,
                location = state.currentLocation,
                plannedAltitudeAglFt = state.plannedAltitudeAglFt,
                hasInternetConnection = weatherResult.isSuccess || spaceResult.isSuccess
            )

            val assessment = assessmentEngine.assess(context)
            _uiState.update {
                it.copy(
                    isLiveLoading = false,
                    assessmentResult = assessment,
                    liveErrorMessage = if (weatherResult.isFailure) "Live Weather Fetch Failed" else null
                )
            }
        }
    }

    fun reevaluateAssessment() {
        selectScenario(_uiState.value.currentScenario)
    }

    fun selectAircraft(aircraftId: String) {
        aircraftRepo.selectAircraft(aircraftId)
    }

    fun saveCustomAircraft(aircraft: Aircraft) {
        aircraftRepo.saveCustomAircraft(aircraft)
    }

    fun deleteCustomAircraft(aircraftId: String) {
        aircraftRepo.deleteCustomAircraft(aircraftId)
    }

    fun setPilotAuthority(type: PilotAuthorityType) {
        pilotRepo.setAuthority(type)
    }

    fun setNightEndorsement(enabled: Boolean) {
        pilotRepo.setNightEndorsement(enabled)
    }

    fun updateLocation(location: LocationInfo) {
        _uiState.update { it.copy(currentLocation = location) }
        reevaluateAssessment()
    }

    fun updateFlightWindow(startMs: Long, endMs: Long) {
        _uiState.update { it.copy(flightWindow = FlightWindow(startMs, endMs)) }
        reevaluateAssessment()
    }

    fun setCategoryFilter(category: AssessmentCategory?) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }
}

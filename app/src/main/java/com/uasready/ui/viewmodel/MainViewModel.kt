package com.uasready.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uasready.data.location.DeviceLocationManager
import com.uasready.data.repository.*
import com.uasready.domain.engine.AssessmentContext
import com.uasready.domain.engine.AssessmentEngine
import com.uasready.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.uasready.ui.theme.AppThemeMode

data class MainUiState(
    val selectedAircraft: Aircraft = Aircraft.getDefault(),
    val allAircraft: List<Aircraft> = Aircraft.PRESETS,
    val currentPilot: Pilot = Pilot.getDefault(),
    val isPilotSelectionPending: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.AUTO,
    val currentLocation: LocationInfo = LocationInfo.defaultLocation(),
    val flightWindow: FlightWindow = FlightWindow.defaultTwoHours(),
    val plannedAltitudeAglFt: Double = 400.0,
    val isLiveLoading: Boolean = false,
    val liveErrorMessage: String? = null,
    val assessmentResult: AssessmentResult? = null,
    val airspaceInfo: AirspaceInfo? = null,
    val estimatedGnss: GnssEstimation? = null,
    val selectedCategoryFilter: AssessmentCategory? = null
)

class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val weatherRepo: WeatherRepository = LiveWeatherRepository(),
    private val spaceWeatherRepo: SpaceWeatherRepository = LiveSpaceWeatherRepository(),
    private val solarRepo: SolarRepository = AstronomicalSolarRepository(),
    private val airspaceRepo: AirspaceRepository = LiveAirspaceRepository(),
    private val terrainRepo: TerrainRepository = LiveTerrainRepository(),
    private val aircraftRepo: AircraftRepository = InMemoryAircraftRepository(),
    private val pilotRepo: PilotRepository = InMemoryPilotRepository(),
    private val assessmentEngine: AssessmentEngine = AssessmentEngine(),
    private val locationManager: DeviceLocationManager = DeviceLocationManager(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "MainViewModel"
    }

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
                if (!_uiState.value.isPilotSelectionPending) {
                    reevaluateAssessment()
                }
            }
        }
        viewModelScope.launch {
            pilotRepo.pilotState.collect { pilot ->
                _uiState.update { it.copy(currentPilot = pilot) }
                if (!_uiState.value.isPilotSelectionPending) {
                    reevaluateAssessment()
                }
            }
        }

        // Try to obtain initial GPS location silently if permission is already granted
        refreshGpsLocation(silent = true)
    }

    fun refreshGpsLocation(silent: Boolean = false) {
        viewModelScope.launch {
            try {
                val gpsLoc = locationManager.getCurrentLocation()
                if (gpsLoc != null) {
                    Log.i(TAG, "GPS location acquired: ${gpsLoc.formattedCoordinates} (${gpsLoc.displayName})")
                    _uiState.update { it.copy(currentLocation = gpsLoc) }
                    if (!_uiState.value.isPilotSelectionPending) {
                        fetchLiveData()
                    }
                } else if (!silent) {
                    Log.w(TAG, "Could not acquire GPS fix (permission not granted or no GPS signal)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error acquiring GPS location: ${e.message}", e)
            }
        }
    }

    fun setPilotAuthority(type: PilotAuthorityType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPilotSelectionPending = false) }
            pilotRepo.setAuthority(type)
            fetchLiveData()
        }
    }

    fun fetchLiveData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLiveLoading = true, liveErrorMessage = null) }

            // Check if we can get fresh GPS location first
            val gpsLoc = locationManager.getCurrentLocation()
            if (gpsLoc != null) {
                _uiState.update { it.copy(currentLocation = gpsLoc) }
            }

            val state = _uiState.value
            val lat = state.currentLocation.latitude
            val lon = state.currentLocation.longitude

            val weatherResult = weatherRepo.getWeatherData(lat, lon)
            val spaceResult = spaceWeatherRepo.getSpaceWeather()
            val airspaceResult = airspaceRepo.getAirspaceInfo(lat, lon)
            val terrainResult = terrainRepo.getTerrainProfile(lat, lon)
            val sunData = solarRepo.calculateSunData(lat, lon, System.currentTimeMillis())

            val weatherPair = weatherResult.getOrNull()
            val spaceWeather = spaceResult.getOrNull()
            val airspace = airspaceResult.getOrNull()
            val terrainProfile = terrainResult.getOrNull()

            val gnss = spaceWeather?.let {
                GnssEstimation.estimate(
                    latitude = lat,
                    elevationFt = state.currentLocation.elevationFt,
                    kpIndex = it.currentKpIndex,
                    terrainProfile = terrainProfile
                )
            }

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
                gnss = gnss,
                terrainProfile = terrainProfile,
                plannedAltitudeAglFt = state.plannedAltitudeAglFt,
                hasInternetConnection = weatherResult.isSuccess || spaceResult.isSuccess
            )

            val assessment = assessmentEngine.assess(context)
            _uiState.update {
                it.copy(
                    isLiveLoading = false,
                    assessmentResult = assessment,
                    airspaceInfo = airspace,
                    estimatedGnss = gnss,
                    liveErrorMessage = if (weatherResult.isFailure) "Live Weather Fetch Failed" else null
                )
            }
        }
    }

    fun reevaluateAssessment() {
        fetchLiveData()
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

    fun updateLocation(location: LocationInfo) {
        _uiState.update { it.copy(currentLocation = location) }
        if (!_uiState.value.isPilotSelectionPending) {
            fetchLiveData()
        }
    }

    fun updateFlightWindow(startMs: Long, endMs: Long) {
        _uiState.update { it.copy(flightWindow = FlightWindow(startMs, endMs)) }
        if (!_uiState.value.isPilotSelectionPending) {
            fetchLiveData()
        }
    }

    fun setCategoryFilter(category: AssessmentCategory?) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }
}

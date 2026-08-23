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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.uasready.data.repository.PersistentAircraftRepository
import com.uasready.domain.model.PilotAuthorityType
import com.uasready.ui.theme.AppThemeMode

data class MainUiState(
    val selectedAircraft: Aircraft = Aircraft.getDefault(),
    val allAircraft: List<Aircraft> = Aircraft.PRESETS,
    val currentPilot: Pilot = Pilot.getDefault(),
    val isPilotSelectionPending: Boolean = true,
    val showFirstTimeFleetSetup: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.AUTO,
    val currentLocation: LocationInfo = LocationInfo.defaultLocation(),
    val flightWindow: FlightWindow = FlightWindow.defaultTwoHours(),
    val plannedAltitudeAglFt: Double = 400.0,
    val isLiveLoading: Boolean = false,
    val liveErrorMessage: String? = null,
    val assessmentResult: AssessmentResult? = null,
    val weatherObservation: WeatherObservation? = null,
    val weatherForecast: WeatherForecast? = null,
    val airspaceInfo: AirspaceInfo? = null,
    val estimatedGnss: GnssEstimation? = null,
    val selectedCategoryFilter: AssessmentCategory? = null,
    val scrollToForecastOnDetail: Boolean = false,
    val lastTelemetryUpdateEpochMs: Long = System.currentTimeMillis()
)

class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val weatherRepo: WeatherRepository = LiveWeatherRepository(),
    private val spaceWeatherRepo: SpaceWeatherRepository = LiveSpaceWeatherRepository(),
    private val solarRepo: SolarRepository = AstronomicalSolarRepository(),
    private val airspaceRepo: AirspaceRepository = LiveAirspaceRepository(),
    private val terrainRepo: TerrainRepository = LiveTerrainRepository(),
    private val aircraftRepo: AircraftRepository = PersistentAircraftRepository(application),
    private val pilotRepo: PilotRepository = InMemoryPilotRepository(),
    private val assessmentEngine: AssessmentEngine = AssessmentEngine(),
    private val locationManager: DeviceLocationManager = DeviceLocationManager(application)
) : AndroidViewModel(application) {

    private val setupPrefs = application.getSharedPreferences("uas_ready_setup_prefs", android.content.Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        MainUiState(
            showFirstTimeFleetSetup = !setupPrefs.getBoolean("has_completed_initial_setup", false)
        )
    )
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
                if (!_uiState.value.isPilotSelectionPending && !_uiState.value.showFirstTimeFleetSetup) {
                    reevaluateAssessment()
                }
            }
        }
        viewModelScope.launch {
            pilotRepo.pilotState.collect { pilot ->
                _uiState.update { it.copy(currentPilot = pilot) }
                if (!_uiState.value.isPilotSelectionPending && !_uiState.value.showFirstTimeFleetSetup) {
                    reevaluateAssessment()
                }
            }
        }

        // Auto-refresh telemetry when age reaches >= 10 minutes
        viewModelScope.launch {
            while (isActive) {
                delay(10_000L)
                val state = _uiState.value
                if (!state.isPilotSelectionPending && !state.showFirstTimeFleetSetup && !state.isLiveLoading) {
                    val elapsed = System.currentTimeMillis() - state.lastTelemetryUpdateEpochMs
                    if (elapsed >= 10 * 60 * 1000L) {
                        Log.i(TAG, "Telemetry age exceeded 10m ($elapsed ms). Performing auto-refresh.")
                        fetchLiveData()
                    }
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

            val now = System.currentTimeMillis()
            val activeFlightWindow = if (state.flightWindow.startEpochMs < now - 60_000L) {
                FlightWindow.defaultTwoHours(now)
            } else {
                state.flightWindow
            }

            val context = AssessmentContext(
                aircraft = state.selectedAircraft,
                pilot = state.currentPilot,
                weather = weatherPair?.first,
                forecast = weatherPair?.second,
                spaceWeather = spaceWeather,
                airspace = airspace,
                sunData = sunData,
                flightWindow = activeFlightWindow,
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
                    weatherObservation = weatherPair?.first,
                    weatherForecast = weatherPair?.second,
                    airspaceInfo = airspace,
                    estimatedGnss = gnss,
                    lastTelemetryUpdateEpochMs = System.currentTimeMillis(),
                    liveErrorMessage = if (weatherResult.isFailure) "Live Weather Fetch Failed" else null
                )
            }
        }
    }

    fun reevaluateAssessment() {
        fetchLiveData()
    }

    fun navigateToForecastDetail() {
        _uiState.update {
            it.copy(
                selectedCategoryFilter = null,
                scrollToForecastOnDetail = true
            )
        }
    }

    fun clearScrollToForecast() {
        _uiState.update {
            it.copy(scrollToForecastOnDetail = false)
        }
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

    fun completeFirstTimeFleetSetup() {
        _uiState.update { it.copy(showFirstTimeFleetSetup = false) }
    }

    fun setPilotAuthority(authority: PilotAuthorityType) {
        pilotRepo.setAuthority(authority)
        setupPrefs.edit().putBoolean("has_completed_initial_setup", true).apply()
        _uiState.update {
            it.copy(
                currentPilot = Pilot(activeAuthority = authority),
                isPilotSelectionPending = false,
                showFirstTimeFleetSetup = false
            )
        }
        fetchLiveData()
    }

    fun setCategoryFilter(category: AssessmentCategory?) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }
}

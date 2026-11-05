package com.taksolutions.uasready.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taksolutions.uasready.data.location.DeviceLocationManager
import com.taksolutions.uasready.data.repository.*
import com.taksolutions.uasready.domain.engine.AssessmentContext
import com.taksolutions.uasready.domain.engine.AssessmentEngine
import com.taksolutions.uasready.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.taksolutions.uasready.data.repository.PersistentAircraftRepository
import com.taksolutions.uasready.domain.model.PilotAuthorityType
import com.taksolutions.uasready.ui.theme.AppThemeMode

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
    val lastTelemetryUpdateEpochMs: Long = System.currentTimeMillis(),
    val nearbyAirports: List<AirportCtafResult> = emptyList(),
    val airportWarningZones: List<AirportWarningZone> = emptyList()
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
    private val locationManager: DeviceLocationManager = DeviceLocationManager(application),
    private val airportWarningRepo: AirportWarningZoneRepository = LiveAirportWarningZoneRepository(application)
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
        private const val NEARBY_RADIUS_NM = 30.0
        private const val WARNING_ZONE_RADIUS_DEG = 0.55
    }

    /** The in-flight telemetry refresh. A newer refresh cancels it so stale results never overwrite fresh ones. */
    private var refreshJob: Job? = null

    /** The in-flight nearby-airport lookup (used when no full refresh follows a location change). */
    private var nearbyJob: Job? = null

    private data class NearbyAviation(
        val airports: List<AirportCtafResult>,
        val warningZones: List<AirportWarningZone>
    )

    private data class FetchedTelemetry(
        val weather: Result<Pair<WeatherObservation, WeatherForecast>>,
        val space: Result<SpaceWeather>,
        val airspace: Result<AirspaceInfo>,
        val terrain: Result<TerrainObstructionProfile>
    )

    /** CTAF spatial scan + SQLite warning-zone query. Disk/CPU bound, so never on the main thread. */
    private suspend fun lookupNearbyAviation(lat: Double, lon: Double): NearbyAviation = withContext(Dispatchers.IO) {
        NearbyAviation(
            airports = CtafLookupHelper.findAirportsWithinRadius(lat, lon, NEARBY_RADIUS_NM),
            warningZones = airportWarningRepo.getWarningZones(lat, lon, WARNING_ZONE_RADIUS_DEG)
        )
    }

    private fun refreshNearbyAviation(lat: Double, lon: Double) {
        nearbyJob?.cancel()
        nearbyJob = viewModelScope.launch {
            val nearby = lookupNearbyAviation(lat, lon)
            _uiState.update { it.copy(nearbyAirports = nearby.airports, airportWarningZones = nearby.warningZones) }
        }
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
        val defaultLoc = _uiState.value.currentLocation
        refreshNearbyAviation(defaultLoc.latitude, defaultLoc.longitude)
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
                        fetchLiveData() // also refreshes nearby airports / warning zones
                    } else {
                        refreshNearbyAviation(gpsLoc.latitude, gpsLoc.longitude)
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
        refreshJob?.cancel()
        nearbyJob?.cancel() // the refresh below recomputes nearby data for the latest location
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLiveLoading = true, liveErrorMessage = null) }

            // Check if we can get fresh GPS location first
            val gpsLoc = locationManager.getCurrentLocation()
            if (gpsLoc != null) {
                _uiState.update { it.copy(currentLocation = gpsLoc) }
            }

            val state = _uiState.value
            val lat = state.currentLocation.latitude
            val lon = state.currentLocation.longitude

            // Independent sources: fetch concurrently so latency is the slowest source, not the sum.
            val fetched = coroutineScope {
                // Local lookups publish immediately; they must not wait on the slowest network source.
                launch {
                    val nearby = lookupNearbyAviation(lat, lon)
                    _uiState.update { it.copy(nearbyAirports = nearby.airports, airportWarningZones = nearby.warningZones) }
                }
                val weather = async { weatherRepo.getWeatherData(lat, lon) }
                val space = async { spaceWeatherRepo.getSpaceWeather() }
                val airspaceAsync = async { airspaceRepo.getAirspaceInfo(lat, lon) }
                val terrain = async { terrainRepo.getTerrainProfile(lat, lon) }
                FetchedTelemetry(weather.await(), space.await(), airspaceAsync.await(), terrain.await())
            }
            val weatherResult = fetched.weather
            val spaceResult = fetched.space
            val airspaceResult = fetched.airspace
            val terrainResult = fetched.terrain
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
        viewModelScope.launch {
            val updatedLoc = if (location.ctafFrequency == null) {
                val ctafResult = withContext(Dispatchers.IO) {
                    CtafLookupHelper.findNearestCtaf(location.latitude, location.longitude)
                }
                if (ctafResult != null) {
                    location.copy(
                        ctafFrequency = ctafResult.frequencyMhz,
                        ctafType = ctafResult.type,
                        nearestAirportIdent = ctafResult.ident,
                        nearestAirportName = ctafResult.name,
                        nearestAirportDistanceNm = ctafResult.distanceNm
                    )
                } else {
                    location
                }
            } else {
                location
            }
            _uiState.update { it.copy(currentLocation = updatedLoc) }
            if (!_uiState.value.isPilotSelectionPending) {
                fetchLiveData() // also refreshes nearby airports / warning zones
            } else {
                refreshNearbyAviation(updatedLoc.latitude, updatedLoc.longitude)
            }
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

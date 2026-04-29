package `is`.rosaparks.viewmodel

import android.app.Application
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `is`.rosaparks.data.ParkingGarage
import `is`.rosaparks.data.ParkingLocation
import `is`.rosaparks.data.ParkingSession
import `is`.rosaparks.data.ParkingZone
import `is`.rosaparks.data.api.fetchGarages
import `is`.rosaparks.data.api.fetchZones
import `is`.rosaparks.data.api.startParkingSession
import `is`.rosaparks.data.api.stopParkingSession
import `is`.rosaparks.data.api.toLocalGarages
import `is`.rosaparks.data.api.toLocalZones
import `is`.rosaparks.data.defaultZonePolygons
import `is`.rosaparks.data.reykjavikZones
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val SAVED_PLATE_KEY = stringPreferencesKey("saved_plate")
private val OPEN_MAP_FIRST_KEY = booleanPreferencesKey("open_map_first")

class ParkingViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val dataStore = application.dataStore

    private val _zones = MutableStateFlow(reykjavikZones)
    val zones: StateFlow<List<ParkingZone>> = _zones.asStateFlow()

    private val _zonePolygons = MutableStateFlow(defaultZonePolygons)
    val zonePolygons: StateFlow<Map<String, List<List<Pair<Double, Double>>>>> = _zonePolygons.asStateFlow()

    private val _garages = MutableStateFlow<List<ParkingGarage>>(emptyList())
    val garages: StateFlow<List<ParkingGarage>> = _garages.asStateFlow()

    private val _garagePolygons = MutableStateFlow<Map<String, List<List<Pair<Double, Double>>>>>(emptyMap())
    val garagePolygons: StateFlow<Map<String, List<List<Pair<Double, Double>>>>> = _garagePolygons.asStateFlow()

    private val _selectedLocation = MutableStateFlow<ParkingLocation?>(null)
    val selectedLocation: StateFlow<ParkingLocation?> = _selectedLocation.asStateFlow()

    /** Back-compat accessor so existing zone-specific UI can filter by type. */
    val selectedZone: StateFlow<ParkingZone?> =
        MutableStateFlow(null as ParkingZone?)
            .also { derived ->
                viewModelScope.launch {
                    _selectedLocation.collect { loc ->
                        derived.value = loc as? ParkingZone
                    }
                }
            }.asStateFlow()

    private val _plate = MutableStateFlow("")
    val plate: StateFlow<String> = _plate.asStateFlow()

    private val _activeSession = MutableStateFlow<ParkingSession?>(null)
    val activeSession: StateFlow<ParkingSession?> = _activeSession.asStateFlow()

    private val _completedSession = MutableStateFlow<ParkingSession?>(null)
    val completedSession: StateFlow<ParkingSession?> = _completedSession.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _runningCost = MutableStateFlow(0)
    val runningCost: StateFlow<Int> = _runningCost.asStateFlow()

    private val _openMapFirst = MutableStateFlow(true)
    val openMapFirst: StateFlow<Boolean> = _openMapFirst.asStateFlow()

    private val _preferencesLoaded = MutableStateFlow(false)
    val preferencesLoaded: StateFlow<Boolean> = _preferencesLoaded.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            _plate.value = prefs[SAVED_PLATE_KEY] ?: ""
            _openMapFirst.value = prefs[OPEN_MAP_FIRST_KEY] ?: true
            _preferencesLoaded.value = true
        }
        viewModelScope.launch {
            try {
                val response = fetchZones()
                val result = response.toLocalZones()
                if (result.zones.isNotEmpty()) {
                    _zones.value = result.zones
                    _zonePolygons.value = defaultZonePolygons + result.polygons
                    Log.d("ParkingVM", "Sótti ${result.zones.size} svæði frá Worker")
                }
            } catch (e: Exception) {
                Log.w("ParkingVM", "Worker ekki tiltækt, nota hardcoded gögn", e)
            }
        }
        viewModelScope.launch {
            try {
                val response = fetchGarages()
                val result = response.toLocalGarages()
                _garages.value = result.garages
                _garagePolygons.value = result.polygons
                Log.d("ParkingVM", "Sótti ${result.garages.size} bílastæðahús frá Worker")
            } catch (e: Exception) {
                Log.w("ParkingVM", "Bílastæðahús ekki tiltæk", e)
            }
        }
    }

    fun selectZone(zone: ParkingZone) {
        _selectedLocation.value = zone
    }

    fun selectGarage(garage: ParkingGarage) {
        _selectedLocation.value = garage
    }

    fun garageById(id: String): ParkingGarage? = _garages.value.firstOrNull { it.id == id }

    fun updatePlate(newPlate: String) {
        val cleaned = newPlate.uppercase().take(6)
        _plate.value = cleaned
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[SAVED_PLATE_KEY] = cleaned
            }
        }
    }

    fun setOpenMapFirst(enabled: Boolean) {
        _openMapFirst.value = enabled
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[OPEN_MAP_FIRST_KEY] = enabled
            }
        }
    }

    fun startParking() {
        val location = _selectedLocation.value ?: return
        val plate = _plate.value
        val session =
            ParkingSession(
                location = location,
                plate = plate,
                startTimeMillis = System.currentTimeMillis(),
            )
        _activeSession.value = session
        startTimer()

        viewModelScope.launch {
            try {
                val response = startParkingSession(plate, location.id)
                _activeSession.value =
                    _activeSession.value?.copy(
                        serverSessionId = response.sessionId,
                    )
                Log.d("ParkingVM", "Lota skráð á server: ${response.sessionId}")
            } catch (e: Exception) {
                Log.w("ParkingVM", "Gat ekki skráð lotu á server", e)
            }
        }
    }

    fun stopParking() {
        timerJob?.cancel()
        val session = _activeSession.value ?: return
        val completed = session.copy(endTimeMillis = System.currentTimeMillis())
        _completedSession.value = completed
        _activeSession.value = null
        _elapsedSeconds.value = 0
        _runningCost.value = 0

        val serverId = session.serverSessionId
        if (serverId != null) {
            viewModelScope.launch {
                try {
                    val response = stopParkingSession(serverId)
                    Log.d("ParkingVM", "Lotu lokið á server: ${response.durationMinutes} mín")
                } catch (e: Exception) {
                    Log.w("ParkingVM", "Gat ekki lokið lotu á server", e)
                }
            }
        }
    }

    fun cancelParking() {
        timerJob?.cancel()
        val session = _activeSession.value
        _activeSession.value = null
        _elapsedSeconds.value = 0
        _runningCost.value = 0

        val serverId = session?.serverSessionId
        if (serverId != null) {
            viewModelScope.launch {
                try {
                    stopParkingSession(serverId)
                    Log.d("ParkingVM", "Lotu hætt á server")
                } catch (e: Exception) {
                    Log.w("ParkingVM", "Gat ekki hætt lotu á server", e)
                }
            }
        }
    }

    fun reset() {
        _selectedLocation.value = null
        _completedSession.value = null
        _elapsedSeconds.value = 0
        _runningCost.value = 0
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob =
            viewModelScope.launch {
                while (true) {
                    val session = _activeSession.value ?: break
                    val elapsedMs = System.currentTimeMillis() - session.startTimeMillis
                    _elapsedSeconds.value = elapsedMs / 1_000
                    // Integer division: cost stays 0 for first 59s (intentional — no charge for partial minute)
                    _runningCost.value = session.costForMinutes(elapsedMs / 60_000)
                    delay(1_000)
                }
            }
    }
}

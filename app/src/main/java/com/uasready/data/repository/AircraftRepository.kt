package com.uasready.data.repository

import android.content.Context
import com.uasready.domain.model.Aircraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

interface AircraftRepository {
    val aircraftListState: StateFlow<List<Aircraft>>
    val selectedAircraftState: StateFlow<Aircraft>

    fun selectAircraft(id: String)
    fun saveCustomAircraft(aircraft: Aircraft)
    fun deleteCustomAircraft(id: String)
}

class PersistentAircraftRepository(
    private val context: Context? = null
) : AircraftRepository {

    private val prefs = context?.getSharedPreferences("uas_ready_aircraft_prefs", Context.MODE_PRIVATE)

    private val allAircraft = mutableListOf<Aircraft>().apply {
        addAll(Aircraft.PRESETS)
        loadCustomAircraft()
    }

    private val _aircraftListState = MutableStateFlow<List<Aircraft>>(allAircraft.toList())
    override val aircraftListState: StateFlow<List<Aircraft>> = _aircraftListState.asStateFlow()

    private val _selectedAircraftState = MutableStateFlow<Aircraft>(loadInitialSelectedAircraft())
    override val selectedAircraftState: StateFlow<Aircraft> = _selectedAircraftState.asStateFlow()

    private fun loadInitialSelectedAircraft(): Aircraft {
        val savedId = prefs?.getString("selected_aircraft_id", null)
        return allAircraft.firstOrNull { it.id == savedId } ?: Aircraft.getDefault()
    }

    override fun selectAircraft(id: String) {
        val found = allAircraft.firstOrNull { it.id == id }
        if (found != null) {
            _selectedAircraftState.value = found
            prefs?.edit()?.putString("selected_aircraft_id", found.id)?.apply()
        }
    }

    override fun saveCustomAircraft(aircraft: Aircraft) {
        val existingIndex = allAircraft.indexOfFirst { it.id == aircraft.id }
        if (existingIndex >= 0) {
            allAircraft[existingIndex] = aircraft
        } else {
            allAircraft.add(aircraft)
        }
        _aircraftListState.value = allAircraft.toList()
        _selectedAircraftState.value = aircraft
        persistCustomAircraft()
        prefs?.edit()?.putString("selected_aircraft_id", aircraft.id)?.apply()
    }

    override fun deleteCustomAircraft(id: String) {
        if (!allAircraft.any { it.id == id && it.isCustom }) return
        allAircraft.removeAll { it.id == id }
        _aircraftListState.value = allAircraft.toList()
        persistCustomAircraft()
        if (_selectedAircraftState.value.id == id) {
            val defaultCraft = Aircraft.getDefault()
            _selectedAircraftState.value = defaultCraft
            prefs?.edit()?.putString("selected_aircraft_id", defaultCraft.id)?.apply()
        }
    }

    private fun persistCustomAircraft() {
        if (prefs == null) return
        val customList = allAircraft.filter { it.isCustom }
        try {
            val json = Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(Aircraft.serializer()),
                customList
            )
            prefs.edit().putString("custom_aircraft_list", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadCustomAircraft() {
        if (prefs == null) return
        val json = prefs.getString("custom_aircraft_list", null) ?: return
        try {
            val customList = Json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(Aircraft.serializer()),
                json
            )
            allAircraft.addAll(customList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

typealias InMemoryAircraftRepository = PersistentAircraftRepository


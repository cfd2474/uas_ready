package com.uasready.data.repository

import com.uasready.domain.model.Aircraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AircraftRepository {
    val aircraftListState: StateFlow<List<Aircraft>>
    val selectedAircraftState: StateFlow<Aircraft>

    fun selectAircraft(id: String)
    fun saveCustomAircraft(aircraft: Aircraft)
    fun deleteCustomAircraft(id: String)
}

class InMemoryAircraftRepository : AircraftRepository {

    private val allAircraft = mutableListOf<Aircraft>().apply {
        addAll(Aircraft.PRESETS)
    }

    private val _aircraftListState = MutableStateFlow<List<Aircraft>>(allAircraft.toList())
    override val aircraftListState: StateFlow<List<Aircraft>> = _aircraftListState.asStateFlow()

    private val _selectedAircraftState = MutableStateFlow<Aircraft>(Aircraft.getDefault())
    override val selectedAircraftState: StateFlow<Aircraft> = _selectedAircraftState.asStateFlow()

    override fun selectAircraft(id: String) {
        val found = allAircraft.firstOrNull { it.id == id }
        if (found != null) {
            _selectedAircraftState.value = found
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
    }

    override fun deleteCustomAircraft(id: String) {
        if (!allAircraft.any { it.id == id && it.isCustom }) return
        allAircraft.removeAll { it.id == id }
        _aircraftListState.value = allAircraft.toList()
        if (_selectedAircraftState.value.id == id) {
            _selectedAircraftState.value = Aircraft.getDefault()
        }
    }
}

package com.uasready.data.repository

import com.uasready.domain.model.Pilot
import com.uasready.domain.model.PilotAuthorityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PilotRepository {
    val pilotState: StateFlow<Pilot>

    fun updatePilot(pilot: Pilot)
    fun setAuthority(type: PilotAuthorityType)
    fun setNightEndorsement(completed: Boolean)
}

class InMemoryPilotRepository : PilotRepository {

    private val _pilotState = MutableStateFlow<Pilot>(Pilot.getDefault())
    override val pilotState: StateFlow<Pilot> = _pilotState.asStateFlow()

    override fun updatePilot(pilot: Pilot) {
        _pilotState.value = pilot
    }

    override fun setAuthority(type: PilotAuthorityType) {
        _pilotState.value = _pilotState.value.copy(activeAuthority = type)
    }

    override fun setNightEndorsement(completed: Boolean) {
        val current = _pilotState.value
        val updatedPart107 = current.part107Profile.copy(nightTrainingCompleted = completed)
        _pilotState.value = current.copy(part107Profile = updatedPart107)
    }
}

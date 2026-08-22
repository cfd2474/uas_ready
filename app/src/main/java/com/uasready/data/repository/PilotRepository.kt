package com.uasready.data.repository

import com.uasready.domain.model.Pilot
import com.uasready.domain.model.PilotAuthorityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PilotRepository {
    val pilotState: StateFlow<Pilot>
    fun setAuthority(type: PilotAuthorityType)
}

class InMemoryPilotRepository : PilotRepository {

    private val _pilotState = MutableStateFlow<Pilot>(Pilot.getDefault())
    override val pilotState: StateFlow<Pilot> = _pilotState.asStateFlow()

    override fun setAuthority(type: PilotAuthorityType) {
        _pilotState.value = _pilotState.value.copy(activeAuthority = type)
    }
}

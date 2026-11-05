package com.taksolutions.uasready.data.repository

import android.content.Context
import com.taksolutions.uasready.domain.model.Pilot
import com.taksolutions.uasready.domain.model.PilotAuthorityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PilotRepository {
    val pilotState: StateFlow<Pilot>

    /** True once the pilot has explicitly chosen an authority (now or on an earlier launch). */
    val hasConfirmedSelection: Boolean

    fun setAuthority(type: PilotAuthorityType)
}

class InMemoryPilotRepository : PilotRepository {

    private val _pilotState = MutableStateFlow<Pilot>(Pilot.getDefault())
    override val pilotState: StateFlow<Pilot> = _pilotState.asStateFlow()

    override var hasConfirmedSelection: Boolean = false
        private set

    override fun setAuthority(type: PilotAuthorityType) {
        _pilotState.value = _pilotState.value.copy(activeAuthority = type)
        hasConfirmedSelection = true
    }
}

/** Where the pilot's chosen authority is kept between launches. */
interface PilotAuthorityStore {
    fun load(): PilotAuthorityType?
    fun save(type: PilotAuthorityType)
}

class SharedPreferencesPilotAuthorityStore(context: Context) : PilotAuthorityStore {
    private val prefs = context.getSharedPreferences("uas_ready_pilot_prefs", Context.MODE_PRIVATE)

    override fun load(): PilotAuthorityType? =
        prefs.getString(KEY_AUTHORITY, null)?.let { saved ->
            // An unknown value (e.g. a removed enum constant) means "not chosen", never a guess.
            PilotAuthorityType.values().firstOrNull { it.name == saved }
        }

    override fun save(type: PilotAuthorityType) {
        prefs.edit().putString(KEY_AUTHORITY, type.name).apply()
    }

    private companion object {
        const val KEY_AUTHORITY = "active_authority"
    }
}

/** Remembers the pilot's authority across launches so the start-up prompt is asked only once. */
class PersistentPilotRepository(private val store: PilotAuthorityStore) : PilotRepository {

    private val saved = store.load()
    private val _pilotState = MutableStateFlow(saved?.let { Pilot(activeAuthority = it) } ?: Pilot.getDefault())
    override val pilotState: StateFlow<Pilot> = _pilotState.asStateFlow()

    override var hasConfirmedSelection: Boolean = saved != null
        private set

    override fun setAuthority(type: PilotAuthorityType) {
        store.save(type)
        _pilotState.value = _pilotState.value.copy(activeAuthority = type)
        hasConfirmedSelection = true
    }
}

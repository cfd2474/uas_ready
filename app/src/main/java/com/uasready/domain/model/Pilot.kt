package com.uasready.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PilotAuthorityType(val displayName: String, val description: String) {
    PART_107(
        displayName = "107 License",
        description = "Cleared for night operations with anti-collision lighting."
    ),
    PUBLIC_COA(
        displayName = "Public COA",
        description = "Flight strictly restricted to daylight (30 min before sunrise to 30 min after sunset)."
    )
}

@Serializable
data class Pilot(
    val activeAuthority: PilotAuthorityType = PilotAuthorityType.PART_107
) {
    companion object {
        fun getDefault(): Pilot = Pilot()
    }
}

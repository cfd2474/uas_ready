package com.uasready.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PilotAuthorityType(val displayName: String, val description: String) {
    PART_107(
        displayName = "Licensed Pilot",
        description = "FAA Part 107 Remote Pilot. Cleared for day and night operations (with anti-collision strobe)."
    ),
    PUBLIC_COA(
        displayName = "Non-licensed Pilot",
        description = "Operating without Part 107 license. Daylight window only (30 min before sunrise to 30 min after sunset)."
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

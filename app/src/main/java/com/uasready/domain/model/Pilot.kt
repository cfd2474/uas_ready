package com.uasready.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PilotAuthorityType {
    PART_107,
    COA_COW
}

@Serializable
data class Part107Profile(
    val certificateNumber: String = "4382910",
    val recurrentTrainingValidUntilEpochMs: Long = System.currentTimeMillis() + 180L * 24 * 60 * 60 * 1000, // Valid 6 months ahead by default
    val nightTrainingCompleted: Boolean = true,
    val isPart107Current: Boolean = true
) {
    fun isRecurrentTrainingExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
        return nowMs > recurrentTrainingValidUntilEpochMs
    }
}

@Serializable
data class CoaCowProfile(
    val agencyName: String = "Riverside County Fire UAS",
    val coaNumber: String = "2024-WSA-192-COA",
    val authorizationValidUntilEpochMs: Long = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000,
    val nightFlightAuthorized: Boolean = true,
    val maxAltitudeAuthorizedFt: Double = 400.0,
    val agencyQualificationCurrent: Boolean = true
) {
    fun isCoaExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
        return nowMs > authorizationValidUntilEpochMs
    }
}

@Serializable
data class Pilot(
    val id: String = "pilot_default",
    val name: String = "Mike (Pilot in Command)",
    val activeAuthority: PilotAuthorityType = PilotAuthorityType.PART_107,
    val part107Profile: Part107Profile = Part107Profile(),
    val coaCowProfile: CoaCowProfile = CoaCowProfile()
) {
    companion object {
        fun getDefault(): Pilot = Pilot()
    }
}

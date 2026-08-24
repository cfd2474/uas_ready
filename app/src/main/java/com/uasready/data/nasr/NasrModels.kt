package com.uasready.data.nasr

import com.uasready.domain.model.AirspaceClass
import com.uasready.domain.model.AirspaceZoneType
import kotlinx.serialization.Serializable

@Serializable
data class NasrAirport(
    val facilityId: String,
    val icaoId: String,
    val name: String,
    val city: String = "",
    val state: String = "",
    val latitude: Double,
    val longitude: Double,
    val elevationFt: Double = 0.0,
    val useType: String = "PU", // PU = Public, PR = Private
    val ctafFreq: String? = null,
    val unicomFreq: String? = null,
    val towerFreq: String? = null,
    val atisFreq: String? = null
) {
    val effectiveCtaf: String?
        get() = ctafFreq ?: towerFreq ?: unicomFreq
}

@Serializable
data class NasrRunway(
    val id: String,
    val facilityId: String,
    val baseEndId: String,
    val recipEndId: String,
    val lengthFt: Double,
    val widthFt: Double,
    val surface: String = "ASPH",
    val trueBearing: Double = 0.0
)

@Serializable
data class NasrFrequency(
    val id: String,
    val facilityId: String,
    val type: String, // CTAF, TWR, GND, ATIS, UNICOM, etc.
    val freqMhz: String,
    val name: String
)

@Serializable
data class NasrAirspaceFeature(
    val id: String,
    val name: String,
    val airspaceClass: AirspaceClass,
    val zoneType: AirspaceZoneType,
    val floorFt: Double = 0.0,
    val floorDatum: String = "MSL",
    val ceilingFt: Double? = 400.0,
    val ceilingDatum: String = "MSL",
    val polygonCoordinates: List<Pair<Double, Double>> = emptyList()
)

@Serializable
data class NasrUasfmGrid(
    val id: String,
    val icaoId: String,
    val ceilingFt: Double,
    val polygonCoordinates: List<Pair<Double, Double>> = emptyList()
)

@Serializable
data class NasrSua(
    val id: String,
    val name: String,
    val type: String, // MOA, RESTRICTED, PROHIBITED, ALERT, WARNING
    val floorFt: Double = 0.0,
    val ceilingFt: Double = 50000.0,
    val scheduleDesc: String = "",
    val polygonCoordinates: List<Pair<Double, Double>> = emptyList()
)

@Serializable
data class AiracCycleInfo(
    val cycleName: String,
    val effectiveEpochMs: Long,
    val expireEpochMs: Long,
    val lastCheckedEpochMs: Long = System.currentTimeMillis(),
    val lastUpdatedEpochMs: Long = System.currentTimeMillis(),
    val isExpired: Boolean = false,
    val daysUntilExpiry: Int = 28
)

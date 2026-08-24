package com.uasready.data.repository

import com.uasready.domain.model.AirspaceInfo

interface AirspaceRepository {
    suspend fun getAirspaceInfo(latitude: Double, longitude: Double): Result<AirspaceInfo>
}

package com.taksolutions.uasready.data.repository

import com.taksolutions.uasready.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class CachingAirspaceRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val day = 24 * 60 * 60 * 1000L
    private var now = 100 * day
    private var liveFails = false

    private val live = object : AirspaceRepository {
        override suspend fun getAirspaceSnapshot(latitude: Double, longitude: Double): Result<AirspaceSnapshot> =
            if (liveFails) Result.failure(IOException("quota")) else Result.success(
                AirspaceSnapshot(
                    latitude, longitude,
                    listOf(AirspaceZone("Z", "Zone", AirspaceZoneType.AUTHORIZATION_ZONE, latitude, longitude, 0.0,
                        polygonCoordinates = listOf(0.0 to 0.0, 0.0 to 1.0, 1.0 to 1.0), airspaceClass = AirspaceClass.CLASS_D)),
                    fetchedEpochMs = now, sourceName = "live"
                )
            )
    }

    private fun repo() = CachingAirspaceRepository(
        delegate = live,
        store = FileAirspaceSnapshotStore(tmp.root.resolve("cache.json")),
        clock = { now }
    )

    @Test
    fun liveSuccess_isReturnedUncached() = runBlocking {
        val r = repo().getAirspaceSnapshot(34.0, -117.0).getOrThrow()
        assertFalse(r.isCached)
    }

    @Test
    fun liveFailure_nearRecentLookup_servesCacheMarked() = runBlocking {
        repo().getAirspaceSnapshot(34.0, -117.0)
        liveFails = true
        now += 2 * day

        val r = repo().getAirspaceSnapshot(34.02, -117.02) // ~1.5 NM away, new repo instance = persisted
        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow().isCached)
        assertEquals(1, r.getOrThrow().zones.size)
    }

    @Test
    fun liveFailure_cacheOlderThanNasrCycle_isFailure() = runBlocking {
        repo().getAirspaceSnapshot(34.0, -117.0)
        liveFails = true
        now += 29 * day
        assertTrue(repo().getAirspaceSnapshot(34.0, -117.0).isFailure)
    }

    @Test
    fun liveFailure_farFromCachedLookup_isFailure() = runBlocking {
        repo().getAirspaceSnapshot(34.0, -117.0)
        liveFails = true
        assertTrue(repo().getAirspaceSnapshot(34.5, -117.0).isFailure) // ~30 NM away
    }

    @Test
    fun corruptCacheFile_isIgnored() = runBlocking {
        tmp.root.resolve("cache.json").writeText("{not json")
        liveFails = true
        assertTrue(repo().getAirspaceSnapshot(34.0, -117.0).isFailure)
    }
}

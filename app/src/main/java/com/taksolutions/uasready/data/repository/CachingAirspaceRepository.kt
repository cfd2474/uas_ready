package com.taksolutions.uasready.data.repository

import android.util.Log
import com.taksolutions.uasready.domain.model.AirspaceSnapshot
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.*

/** Persistence for recently fetched airspace snapshots. */
interface AirspaceSnapshotStore {
    fun load(): List<AirspaceSnapshot>
    fun save(snapshots: List<AirspaceSnapshot>)
}

/** Stores snapshots as JSON in a single app-private file. Corrupt/missing file → empty cache. */
class FileAirspaceSnapshotStore(private val file: File) : AirspaceSnapshotStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(AirspaceSnapshot.serializer())

    override fun load(): List<AirspaceSnapshot> = try {
        if (file.exists()) json.decodeFromString(serializer, file.readText()) else emptyList()
    } catch (e: Exception) {
        Log.w("AirspaceCache", "Ignoring unreadable airspace cache: ${e.message}")
        emptyList()
    }

    override fun save(snapshots: List<AirspaceSnapshot>) {
        try {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(serializer, snapshots))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        } catch (e: Exception) {
            Log.w("AirspaceCache", "Could not write airspace cache: ${e.message}")
        }
    }
}

/**
 * Decorator: serves live airspace, and when the live source fails (network loss, FAA org-wide
 * quota throttling), falls back to the most recent snapshot fetched near the same point.
 *
 * FAA airspace changes on the 28-day NASR cycle, so a snapshot up to [maxAge] old is still the
 * published airspace in almost all cases. Cached results are marked [AirspaceSnapshot.isCached]
 * so the engine and UI can say the data is not live.
 */
class CachingAirspaceRepository(
    private val delegate: AirspaceRepository,
    private val store: AirspaceSnapshotStore,
    private val maxAgeMs: Long = 28L * 24 * 60 * 60 * 1000,
    private val maxDistanceNm: Double = 5.0,
    private val maxEntries: Int = 20,
    private val clock: () -> Long = System::currentTimeMillis
) : AirspaceRepository {

    private val lock = Any()

    override suspend fun getAirspaceSnapshot(latitude: Double, longitude: Double): Result<AirspaceSnapshot> {
        val live = delegate.getAirspaceSnapshot(latitude, longitude)
        live.getOrNull()?.let { remember(it); return live }

        val cached = synchronized(lock) {
            store.load()
                .filter { clock() - it.fetchedEpochMs in 0..maxAgeMs }
                .filter { distanceNm(latitude, longitude, it.centerLat, it.centerLon) <= maxDistanceNm }
                .maxByOrNull { it.fetchedEpochMs }
        }
        return if (cached != null) {
            Log.w("AirspaceCache", "Live airspace failed (${live.exceptionOrNull()?.message}); using cached snapshot from ${cached.fetchedEpochMs}")
            Result.success(cached.copy(isCached = true))
        } else {
            live
        }
    }

    private fun remember(snapshot: AirspaceSnapshot) = synchronized(lock) {
        val kept = store.load().filter {
            clock() - it.fetchedEpochMs <= maxAgeMs &&
                distanceNm(snapshot.centerLat, snapshot.centerLon, it.centerLat, it.centerLon) > maxDistanceNm
        }
        store.save((listOf(snapshot.copy(isCached = false)) + kept).take(maxEntries))
    }

    private fun distanceNm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 3440.065 // Earth radius in NM
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}

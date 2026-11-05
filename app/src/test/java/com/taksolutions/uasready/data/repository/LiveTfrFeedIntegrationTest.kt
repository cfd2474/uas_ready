package com.taksolutions.uasready.data.repository

import com.taksolutions.uasready.domain.engine.TfrEvaluator
import com.taksolutions.uasready.domain.model.FlightWindow
import com.taksolutions.uasready.domain.model.TfrNotice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Calls the live FAA TFR feed (tfr.faa.gov). Opt-in: ./gradlew testDebugUnitTest -PliveTests
 * Prints a summary used to verify docs/PLATFORM_REFERENCE.md.
 */
class LiveTfrFeedIntegrationTest {

    @Before
    fun requireLiveTestsFlag() {
        assumeTrue("Live TFR tests skipped (enable with -PliveTests)", System.getProperty("uasready.liveTests") == "true")
    }

    private class MemoryStore : TfrDetailStore {
        var saved: List<TfrNotice> = emptyList()
        override fun load() = saved
        override fun save(notices: List<TfrNotice>) { saved = notices }
    }

    @Test
    fun liveFeedParsesAndAMappedTfrIsDetectedAtItsOwnCentre() = runBlocking {
        val snapshot = LiveTfrRepository(MemoryStore()).getTfrSnapshot().getOrThrow()
        val mapped = snapshot.notices.filter { it.hasGeometry }
        println("Live TFR feed: ${snapshot.notices.size} notices, ${mapped.size} mapped, " +
            "${snapshot.notices.count { !it.hasGeometry && it.isNationwide }} nationwide unmapped, " +
            "${snapshot.notices.count { !it.hasGeometry && !it.isNationwide }} regional unmapped")
        assertTrue("FAA always lists some TFRs", snapshot.notices.isNotEmpty())
        assertTrue("Most TFRs have geometry", mapped.isNotEmpty())

        // Every vertex parsed into the US / territories range.
        mapped.flatMap { n -> n.areas.flatMap { it.polygon } }.forEach { (lat, lon) ->
            assertTrue("lat $lat", lat in -15.0..72.0)
            assertTrue("lon $lon", lon in -180.0..180.0)
        }

        // Pick a surface-floor area in effect now; its own centroid must be reported as inside it.
        val now = System.currentTimeMillis()
        val window = FlightWindow(now, now + 60_000L)
        val candidate = mapped.firstNotNullOfOrNull { n ->
            n.areas.firstOrNull { a ->
                a.polygon.size >= 3 && a.floorFt == 0.0 && a.dailyStartMinuteUtc == null &&
                    (a.windows.ifEmpty { listOf(n.effective) }).any { it.overlaps(window.startEpochMs, window.endEpochMs) }
            }?.let { n to it }
        }
        assumeTrue("No surface TFR active right now to probe", candidate != null)
        val (notice, area) = candidate!!
        val lat = area.polygon.map { it.first }.average()
        val lon = area.polygon.map { it.second }.average()
        val result = TfrEvaluator().evaluate(snapshot, lat, lon, null, 400.0, window, notice.state)
        println("Probe ${notice.notamId} ${area.name} at $lat,$lon → active=${result.active.map { it.id }}")
        assertTrue("Centroid of ${notice.notamId} must be inside an active TFR", result.active.any { it.id == notice.notamId })
    }
}

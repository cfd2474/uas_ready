package com.taksolutions.uasready.data.repository

import com.taksolutions.uasready.domain.engine.SecurityAirspaceEvaluator
import com.taksolutions.uasready.domain.engine.TfrEvaluator
import com.taksolutions.uasready.domain.model.DcSfraZone
import com.taksolutions.uasready.domain.model.FlightWindow
import com.taksolutions.uasready.domain.model.TfrNotice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Calls the live FAA national-security, Boundary_Airspace and TFR services.
 * Opt-in: ./gradlew testDebugUnitTest -PliveTests --tests '*LiveSecurityAirspace*'
 * Probe points: docs/PLATFORM_REFERENCE.md (verified 2026-11-05).
 */
class LiveSecurityAirspaceIntegrationTest {

    @Before
    fun requireLiveTestsFlag() {
        assumeTrue("Live tests skipped (enable with -PliveTests)", System.getProperty("uasready.liveTests") == "true")
    }

    // The FAA ArcGIS quota is org-wide; wait out a throttle instead of failing the run.
    private val repo = LiveSecurityAirspaceRepository(client = ArcGisQueryClient(quotaRetryDelayMs = 65_000L))

    private fun borderStripAt(lat: Double, lon: Double): List<String> = runBlocking {
        val s = repo.getSecurityAirspace(lat, lon).getOrThrow()
        println("Probe $lat,$lon → inside=${s.sitesAtLaunchPoint.map { it.faaId }} nearby=${s.nearbySites.size} dc=${s.dcSfraZone}")
        s.sitesAtLaunchPoint.map { it.faaId }.filter { it.startsWith("20261016-DHS-Z") }
    }

    // Strip edges along each meridian, measured 2026-11-05: San Ysidro 32.54242–32.55701,
    // Nogales 31.33301–31.34753, El Paso 31.76338–31.77893, Brownsville 25.89705–25.91350.
    @Test
    fun borderStripCoversTheWholeBorder() {
        assertEquals(listOf("20261016-DHS-ZLA-1"), borderStripAt(32.550, -117.03))  // San Ysidro, CA
        assertEquals(listOf("20261016-DHS-ZAB-2"), borderStripAt(31.340, -110.94))  // Nogales, AZ
        assertEquals(listOf("20261016-DHS-ZAB-2"), borderStripAt(31.771, -106.45))  // El Paso, TX
        assertEquals(listOf("20261016-DHS-ZHU-3"), borderStripAt(25.905, -97.5))    // Brownsville, TX
    }

    @Test
    fun widenedStripAndInlandPoints() = runBlocking {
        val evaluator = SecurityAirspaceEvaluator()
        suspend fun eval(lat: Double, lon: Double) = evaluator.evaluate(repo.getSecurityAirspace(lat, lon).getOrThrow(), lat, lon)
        // 0.93 NM from the border, ~0.06 NM past the FAA polygon: inside per the NOTAM's 1 NM.
        assertTrue(eval(32.5580, -117.03).sitesAtLaunchPoint.any { it.faaId == "20261016-DHS-ZLA-1" })
        // ~1.3 NM from the border: outside, but within 1 NM of the strip → nearby caution.
        val near = eval(32.5640, -117.03)
        assertTrue(near.sitesAtLaunchPoint.none { it.isMexicoBorderStrip })
        assertTrue(near.sitesNearby.any { it.faaId == "20261016-DHS-ZLA-1" })
        // ~5 NM north of Nogales: no border restriction at all.
        val inland = eval(31.42, -110.94)
        assertTrue((inland.sitesAtLaunchPoint + inland.sitesNearby).none { it.isMexicoBorderStrip })
    }

    @Test
    fun dcSfraAndFrz() = runBlocking {
        assertEquals(DcSfraZone.FRZ, repo.getSecurityAirspace(38.8521, -77.0377).getOrThrow().dcSfraZone) // DCA
        assertEquals(DcSfraZone.SFRA, repo.getSecurityAirspace(39.14, -77.20).getOrThrow().dcSfraZone)    // Gaithersburg, MD
        assertNull(repo.getSecurityAirspace(39.25, -77.45).getOrThrow().dcSfraZone)                        // outside the SFRA
    }

    private class MemoryStore : TfrDetailStore {
        var saved: List<TfrNotice> = emptyList()
        override fun load() = saved
        override fun save(notices: List<TfrNotice>) { saved = notices }
    }

    @Test
    fun liveBorderNoticesNameTheirStatesInTheText() = runBlocking {
        val snapshot = LiveTfrRepository(MemoryStore()).getTfrSnapshot().getOrThrow()
        val border = snapshot.notices.filter { it.notamId in setOf("6/9954", "6/9978", "6/9956") }
        border.forEach { println("${it.notamId} list=${it.state} text=${it.textStates}") }
        assumeTrue("Border NOTAMs no longer listed", border.isNotEmpty())
        val now = System.currentTimeMillis()
        val window = FlightWindow(now, now + 60_000L)
        // Without the security layers, a California border launch must still be warned (6/9954 is listed "AZ").
        val ca = TfrEvaluator().evaluate(snapshot, 32.535, -117.03, 100.0, 400.0, window, "CA", securityLayersSourced = false)
        assertTrue(ca.unmappedRegional.any { it.id == "6/9954" })
        val nm = TfrEvaluator().evaluate(snapshot, 31.78, -107.63, 4000.0, 400.0, window, "NM", securityLayersSourced = false)
        assertTrue(nm.unmappedRegional.any { it.id == "6/9978" })
        // With them, the border notices are evaluated through the strip instead.
        val covered = TfrEvaluator().evaluate(snapshot, 32.535, -117.03, 100.0, 400.0, window, "CA", securityLayersSourced = true)
        assertTrue(covered.unmappedRegional.none { it.id in setOf("6/9954", "6/9978", "6/9956") })
    }
}

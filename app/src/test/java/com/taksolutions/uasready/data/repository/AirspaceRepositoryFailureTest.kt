package com.taksolutions.uasready.data.repository

import com.taksolutions.uasready.domain.model.AirspaceClass
import com.taksolutions.uasready.domain.model.AltitudeReference
import com.taksolutions.uasready.domain.model.SpecialUseType
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Contract tests for [LiveAirspaceRepository] against a local in-process HTTP server
 * (deterministic, no network):
 *  - any failed FAA lookup is a failure (engine → DATA_UNAVAILABLE), never "no airspace";
 *  - FAA floor fields are interpreted per docs/PLATFORM_REFERENCE.md.
 */
class AirspaceRepositoryFailureTest {

    private lateinit var server: MockWebServer
    private var serverRunning = false
    /** Per-path queue of responses; the last one repeats. */
    private val responses = mutableMapOf<String, MutableList<Pair<Int, String>>>()

    private val emptyFeatures = """{"type":"FeatureCollection","features":[]}"""
    private val quotaError = """{"error":{"code":429,"message":"Unable to perform query. Too many requests."}}"""

    private fun respond(path: String, vararg r: Pair<Int, String>) { responses[path] = r.toMutableList() }

    @Before
    fun startServer() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val queue = responses[request.requestUrl?.encodedPath] ?: return MockResponse().setResponseCode(404)
                val (code, body) = if (queue.size > 1) queue.removeAt(0) else queue[0]
                return MockResponse().setResponseCode(code).setBody(body)
            }
        }
        server.start()
        serverRunning = true
    }

    @After
    fun stopServer() {
        if (serverRunning) server.shutdown()
        serverRunning = false
    }

    private fun repo() = LiveAirspaceRepository(
        classAirspaceUrl = server.url("/class").toString(),
        specialUseAirspaceUrl = server.url("/sua").toString(),
        quotaRetryDelayMs = 10
    )

    private fun square(lat: Double, lon: Double, d: Double = 0.05) =
        "[[[${lon - d},${lat - d}],[${lon + d},${lat - d}],[${lon + d},${lat + d}],[${lon - d},${lat + d}],[${lon - d},${lat - d}]]]"

    private fun feature(props: String, lat: Double = 34.0, lon: Double = -117.0) =
        """{"type":"Feature","properties":{$props},"geometry":{"type":"Polygon","coordinates":${square(lat, lon)}}}"""

    private fun collection(vararg features: String) = """{"type":"FeatureCollection","features":[${features.joinToString(",")}]}"""

    // ---- Failure contract ------------------------------------------------------------------

    @Test
    fun bothQueriesSucceedWithNoFeatures_isEmptySnapshot() = runBlocking {
        respond("/class", 200 to emptyFeatures)
        respond("/sua", 200 to emptyFeatures)

        val snapshot = repo().getAirspaceSnapshot(34.0, -117.0).getOrThrow()

        assertTrue(snapshot.zones.isEmpty())
        assertFalse(snapshot.isCached)
    }

    @Test
    fun classAirspaceHttp500_isFailure() = runBlocking {
        respond("/class", 500 to "server error")
        respond("/sua", 200 to emptyFeatures)
        assertTrue(repo().getAirspaceSnapshot(34.0, -117.0).isFailure)
    }

    @Test
    fun specialUseAirspaceHttp503_isFailure() = runBlocking {
        respond("/class", 200 to emptyFeatures)
        respond("/sua", 503 to "unavailable")
        assertTrue(repo().getAirspaceSnapshot(34.0, -117.0).isFailure)
    }

    @Test
    fun arcGisErrorObjectWithHttp200_isFailure() = runBlocking {
        respond("/class", 200 to """{"error":{"code":400,"message":"Invalid query"}}""")
        respond("/sua", 200 to emptyFeatures)
        assertTrue(repo().getAirspaceSnapshot(34.0, -117.0).isFailure)
    }

    @Test
    fun malformedBody_isFailure() = runBlocking {
        respond("/class", 200 to "<html>captive portal</html>")
        respond("/sua", 200 to emptyFeatures)
        assertTrue(repo().getAirspaceSnapshot(34.0, -117.0).isFailure)
    }

    @Test
    fun truncatedResult_isFailure() = runBlocking {
        respond("/class", 200 to """{"type":"FeatureCollection","features":[],"exceededTransferLimit":true}""")
        respond("/sua", 200 to emptyFeatures)
        assertTrue(repo().getAirspaceSnapshot(34.0, -117.0).isFailure)
    }

    @Test
    fun unreachableHost_isFailure() = runBlocking {
        val repo = repo() // capture the port before shutting down
        stopServer()
        assertTrue(repo.getAirspaceSnapshot(34.0, -117.0).isFailure)
    }

    @Test
    fun quotaExceededOnce_isRetriedAndSucceeds() = runBlocking {
        respond("/class", 200 to quotaError, 200 to emptyFeatures)
        respond("/sua", 200 to emptyFeatures)
        assertTrue(repo().getAirspaceSnapshot(34.0, -117.0).isSuccess)
    }

    @Test
    fun quotaExceededTwice_isFailure() = runBlocking {
        respond("/class", 200 to quotaError)
        respond("/sua", 200 to emptyFeatures)
        assertTrue(repo().getAirspaceSnapshot(34.0, -117.0).isFailure)
    }

    // ---- Floor semantics (docs/PLATFORM_REFERENCE.md) ---------------------------------------

    @Test
    fun flightLevelFloor_isConvertedToMslFeet() = runBlocking {
        respond("/class", 200 to emptyFeatures)
        respond("/sua", 200 to collection(
            feature(""""OBJECTID":1,"NAME":"R-5002G","TYPE_CODE":"R","LOWER_VAL":"200","LOWER_UOM":"FL","LOWER_CODE":"STD"""")
        ))

        val zone = repo().getAirspaceSnapshot(34.0, -117.0).getOrThrow().zones.single()

        assertEquals(AltitudeReference.MSL, zone.floorReference)
        assertEquals(20_000.0, zone.floorFt, 0.1)
        assertEquals(SpecialUseType.RESTRICTED, zone.specialUseType)
    }

    @Test
    fun sfcCodedFloor_isAboveGroundLevel_andHighOnesDropped() = runBlocking {
        respond("/class", 200 to collection(
            feature(""""OBJECTID":1,"NAME":"BAKER CITY","CLASS":"E","LOCAL_TYPE":"CLASS_E2","LOWER_VAL":"0","LOWER_UOM":"FT","LOWER_CODE":"SFC""""),
            feature(""""OBJECTID":2,"NAME":"BAKER CITY","CLASS":"E","LOCAL_TYPE":"CLASS_E5","LOWER_VAL":"700","LOWER_UOM":"FT","LOWER_CODE":"SFC""""),
            feature(""""OBJECTID":3,"NAME":"DENVER","CLASS":"B","LOCAL_TYPE":"CLASS_B","LOWER_VAL":"6500","LOWER_UOM":"FT","LOWER_CODE":"MSL"""")
        ))
        respond("/sua", 200 to emptyFeatures)

        val zones = repo().getAirspaceSnapshot(34.0, -117.0).getOrThrow().zones

        val e2 = zones.single { it.airspaceClass == AirspaceClass.CLASS_E_SURFACE }
        assertEquals(AltitudeReference.AGL, e2.floorReference)
        assertEquals(0.0, e2.floorFt, 0.1)
        assertFalse("E5 (700 ft AGL) can never matter below 400 ft", zones.any { it.id.startsWith("FAA-CLASS-2-") })
        val shelf = zones.single { it.airspaceClass == AirspaceClass.CLASS_B }
        assertEquals("MSL shelves are kept for the classifier to judge against terrain", AltitudeReference.MSL, shelf.floorReference)
        assertEquals(6500.0, shelf.floorFt, 0.1)
    }
}

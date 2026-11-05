package com.taksolutions.uasready.data.repository

import com.taksolutions.uasready.domain.model.AirspaceClass
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
 * Safety contract for [LiveAirspaceRepository]: a failed FAA lookup must surface as a failure
 * (engine → DATA_UNAVAILABLE), never as uncontrolled Class G airspace.
 * Uses a local in-process HTTP server, so it is deterministic and needs no network.
 */
class AirspaceRepositoryFailureTest {

    private lateinit var server: MockWebServer
    private var serverRunning = false
    private val responses = mutableMapOf<String, Pair<Int, String>>()

    private val emptyFeatures = """{"type":"FeatureCollection","features":[]}"""

    @Before
    fun startServer() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val (code, body) = responses[request.requestUrl?.encodedPath] ?: (404 to "not found")
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
        specialUseAirspaceUrl = server.url("/sua").toString()
    )

    @Test
    fun bothQueriesSucceedWithNoFeatures_isClassG_andTfrsUnverified() = runBlocking {
        responses["/class"] = 200 to emptyFeatures
        responses["/sua"] = 200 to emptyFeatures

        val airspace = repo().getAirspaceInfo(34.0, -117.0).getOrThrow()

        assertEquals(AirspaceClass.CLASS_G, airspace.primaryClass)
        assertFalse("Live repo has no TFR feed", airspace.tfrDataSourced)
        assertNull("UASFM grids are not queried, so no ceiling may be claimed", airspace.uasFacilityMapMaxAltitudeFt)
    }

    @Test
    fun classAirspaceHttp500_isFailure() = runBlocking {
        responses["/class"] = 500 to "server error"
        responses["/sua"] = 200 to emptyFeatures

        assertTrue(repo().getAirspaceInfo(34.0, -117.0).isFailure)
    }

    @Test
    fun specialUseAirspaceHttp503_isFailure() = runBlocking {
        responses["/class"] = 200 to emptyFeatures
        responses["/sua"] = 503 to "unavailable"

        assertTrue(repo().getAirspaceInfo(34.0, -117.0).isFailure)
    }

    @Test
    fun arcGisErrorObjectWithHttp200_isFailure() = runBlocking {
        responses["/class"] = 200 to """{"error":{"code":400,"message":"Invalid query"}}"""
        responses["/sua"] = 200 to emptyFeatures

        assertTrue(repo().getAirspaceInfo(34.0, -117.0).isFailure)
    }

    @Test
    fun malformedBody_isFailure() = runBlocking {
        responses["/class"] = 200 to "<html>captive portal</html>"
        responses["/sua"] = 200 to emptyFeatures

        assertTrue(repo().getAirspaceInfo(34.0, -117.0).isFailure)
    }

    @Test
    fun unreachableHost_isFailure() = runBlocking {
        val repo = repo() // capture the port before shutting down
        stopServer() // nothing listening on the port any more
        assertTrue(repo.getAirspaceInfo(34.0, -117.0).isFailure)
    }
}

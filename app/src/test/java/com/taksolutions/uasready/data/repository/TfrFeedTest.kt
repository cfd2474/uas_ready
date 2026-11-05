package com.taksolutions.uasready.data.repository

import com.taksolutions.uasready.domain.engine.AssessmentContext
import com.taksolutions.uasready.domain.engine.TfrEvaluator
import com.taksolutions.uasready.domain.engine.rules.AirspaceRuleEvaluator
import com.taksolutions.uasready.domain.model.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** Synthetic fixtures shaped like tfr.faa.gov responses (structure verified 2026-11-05, see PLATFORM_REFERENCE). */
class TfrFeedTest {

    private fun utc(s: String) = Instant.parse(s).toEpochMilli()

    /** Square ~1.2 NM around (34.0, -117.0). */
    private val square = listOf(
        "34.02N" to "117.02W", "34.02N" to "116.98W", "33.98N" to "116.98W", "33.98N" to "117.02W", "34.02N" to "117.02W"
    )

    private fun detailXml(
        id: String,
        lowerCode: String = "HEI", lowerVal: String = "0", lowerUom: String = "FT",
        notEffective: String? = "2026-11-05T10:00:00", notExpire: String? = "2026-11-05T22:00:00",
        schedule: String = "",
        withPolygon: Boolean = true
    ): String {
        val avx = if (withPolygon) square.joinToString("") { (la, lo) ->
            "<Avx><codeDatum>WGE</codeDatum><codeType>GRC</codeType><geoLat>$la</geoLat><geoLong>0$lo</geoLong></Avx>"
        } else ""
        val merged = if (withPolygon) "<abdMergedArea><AbdUid><AseUid><codeType>RAS</codeType><codeId>100</codeId></AseUid></AbdUid><txtRmk>100</txtRmk>$avx</abdMergedArea>" else ""
        return "﻿<XNOTAM-Update><Group><Add><Not>" +
            "<NotUid><txtNameAcctFac>FDC</txtNameAcctFac><txtLocalName>$id</txtLocalName></NotUid>" +
            (notEffective?.let { "<dateEffective>$it</dateEffective>" } ?: "") +
            (notExpire?.let { "<dateExpire>$it</dateExpire>" } ?: "") +
            "<codeTimeZone>EST</codeTimeZone>" +
            "<TfrNot><codeType>91.137(a)(2)</codeType><TFRAreaGroup><aseTFRArea>" +
            "<AseUid><codeType>RAS</codeType><codeId>100</codeId></AseUid><txtName>Area A</txtName>" +
            "<codeDistVerUpper>ALT</codeDistVerUpper><valDistVerUpper>180</valDistVerUpper><uomDistVerUpper>FL</uomDistVerUpper>" +
            "<codeDistVerLower>$lowerCode</codeDistVerLower><valDistVerLower>$lowerVal</valDistVerLower><uomDistVerLower>$lowerUom</uomDistVerLower>" +
            schedule +
            "</aseTFRArea>$merged</TFRAreaGroup></TfrNot></Not></Add></Group></XNOTAM-Update>"
    }

    // ---------- Parser ----------

    @Test
    fun parsesAreaAltitudesTimesAndPolygon() {
        val d = TfrXmlParser().parse(detailXml("6/1234", lowerCode = "ALT", lowerVal = "4100"))
        assertEquals("6/1234", d.notamId)
        assertEquals("91.137(a)(2)", d.cfrSection)
        // Times are UTC even though codeTimeZone says EST.
        assertEquals(utc("2026-11-05T10:00:00Z"), d.effective.startEpochMs)
        val area = d.areas.single()
        assertEquals(4100.0, area.floorFt, 0.0)
        assertEquals(AltitudeReference.MSL, area.floorReference)
        assertEquals(18000.0, area.ceilingFt!!, 0.0) // FL180
        assertEquals(5, area.polygon.size)
        assertEquals(34.02, area.polygon[0].first, 1e-9)
        assertEquals(-117.02, area.polygon[0].second, 1e-9)
    }

    @Test
    fun parsesDailyScheduleAndOpenEndedDates() {
        val schedule = "<isScheduledTfrArea>TRUE</isScheduledTfrArea><dayCode>Daily</dayCode><ScheduleGroup>" +
            "<isTimeSeparate>TRUE</isTimeSeparate><dateEffective>2026-11-04T00:00:00</dateEffective><dateExpire>2026-11-10T23:59:00</dateExpire>" +
            "<startTime>2026-11-04T14:00:00</startTime><endTime>2026-11-04T23:59:00</endTime></ScheduleGroup>"
        val d = TfrXmlParser().parse(detailXml("6/2000", notEffective = null, notExpire = null, schedule = schedule))
        assertNull(d.effective.startEpochMs)
        assertNull(d.effective.endEpochMs)
        assertEquals(14 * 60, d.areas.single().dailyStartMinuteUtc)
        assertEquals(23 * 60 + 59, d.areas.single().dailyEndMinuteUtc)
    }

    @Test
    fun nonDailyDayCodeIsNotInterpretedAsTimeOfDay() {
        val schedule = "<isScheduledTfrArea>TRUE</isScheduledTfrArea><dayCode>Saturday</dayCode><ScheduleGroup>" +
            "<isTimeSeparate>TRUE</isTimeSeparate><dateEffective>2026-11-04T00:00:00</dateEffective><dateExpire>2026-11-10T23:59:00</dateExpire>" +
            "<startTime>2026-11-04T14:00:00</startTime><endTime>2026-11-04T16:00:00</endTime></ScheduleGroup>"
        assertNull(TfrXmlParser().parse(detailXml("6/2001", schedule = schedule)).areas.single().dailyStartMinuteUtc)
    }

    // ---------- Repository ----------

    private lateinit var server: MockWebServer
    private val requested = mutableListOf<String>()
    private var detailCode = 200

    private val listJson = """[
        {"notam_id":"6/1234","type":"HAZARDS","state":"CA","description":"TEST FIRE, CA","mod_abs_time":"202611050100"},
        {"notam_id":"6/9954","type":"SECURITY","state":"AZ","description":"U.S. MEXICO BORDER, AZ","mod_abs_time":"202610200000"}
    ]"""

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                synchronized(requested) { requested.add(path) }
                return when {
                    path.startsWith("/tfrapi/getTfrList") -> MockResponse().setBody(listJson)
                    path == "/download/detail_6_1234.xml" -> MockResponse().setResponseCode(detailCode).setBody(detailXml("6/1234"))
                    path == "/download/detail_6_9954.xml" -> MockResponse().setResponseCode(detailCode).setBody(detailXml("6/9954", withPolygon = false))
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
    }

    @After
    fun stop() = server.shutdown()

    private class MemoryStore : TfrDetailStore {
        var saved: List<TfrNotice> = emptyList()
        override fun load() = saved
        override fun save(notices: List<TfrNotice>) { saved = notices }
    }

    private fun repo(store: TfrDetailStore) = LiveTfrRepository(store, baseUrl = server.url("").toString().trimEnd('/'))

    @Test
    fun fetchesListAndDetails() = runBlocking {
        val snapshot = repo(MemoryStore()).getTfrSnapshot().getOrThrow()
        assertEquals(2, snapshot.notices.size)
        val fire = snapshot.notices.first { it.notamId == "6/1234" }
        assertTrue(fire.hasGeometry)
        assertEquals("CA", fire.state)
        assertFalse(snapshot.notices.first { it.notamId == "6/9954" }.hasGeometry)
    }

    @Test
    fun unchangedNoticesAreNotDownloadedAgain() = runBlocking {
        val store = MemoryStore()
        repo(store).getTfrSnapshot().getOrThrow()
        requested.clear()
        repo(store).getTfrSnapshot().getOrThrow()
        assertEquals(listOf("/tfrapi/getTfrList"), requested.toList())
    }

    @Test
    fun anyDetailFailureFailsTheWholeSnapshot() = runBlocking {
        detailCode = 500
        assertTrue(repo(MemoryStore()).getTfrSnapshot().isFailure)
    }

    @Test
    fun cachedDetailsFromAnOlderParserAreDownloadedAgain() = runBlocking {
        val store = MemoryStore()
        repo(store).getTfrSnapshot().getOrThrow()
        store.saved = store.saved.map { it.copy(parserVersion = TfrXmlParser.VERSION - 1) }
        requested.clear()
        repo(store).getTfrSnapshot().getOrThrow()
        assertEquals(3, requested.size) // list + both details
        assertTrue(store.saved.all { it.parserVersion == TfrXmlParser.VERSION })
    }

    @Test
    fun unreachableFeedFails() = runBlocking {
        val r = LiveTfrRepository(MemoryStore(), baseUrl = "http://127.0.0.1:1").getTfrSnapshot()
        assertTrue(r.isFailure)
    }

    // ---------- Evaluator ----------

    private val evaluator = TfrEvaluator()
    private fun notice(area: TfrArea, effective: TimeWindow = TimeWindow(utc("2026-11-05T10:00:00Z"), utc("2026-11-05T22:00:00Z")), state: String = "CA") =
        TfrNotice("6/1234", "HAZARDS", "TEST FIRE, CA", state, "91.137(a)(2)", effective, listOf(area), "x")

    private fun area(floor: Double = 0.0, ref: AltitudeReference = AltitudeReference.AGL, start: Int? = null, end: Int? = null) = TfrArea(
        name = "Area A", floorFt = floor, floorReference = ref, ceilingFt = 18000.0, ceilingReference = AltitudeReference.MSL,
        polygon = listOf(34.02 to -117.02, 34.02 to -116.98, 33.98 to -116.98, 33.98 to -117.02, 34.02 to -117.02),
        dailyStartMinuteUtc = start, dailyEndMinuteUtc = end
    )

    private fun window(from: String, to: String) = FlightWindow(utc(from), utc(to))
    private fun eval(n: TfrNotice, lat: Double = 34.0, lon: Double = -117.0, ground: Double? = 1000.0,
                     w: FlightWindow = window("2026-11-05T12:00:00Z", "2026-11-05T14:00:00Z"), state: String? = "CA") =
        evaluator.evaluate(TfrSnapshot(listOf(n), 0L), lat, lon, ground, 400.0, w, state)

    @Test
    fun insideActiveSurfaceTfrIsActive() {
        val r = eval(notice(area()))
        assertEquals(listOf("6/1234"), r.active.map { it.id })
        assertEquals(1, r.mapZones.size)
    }

    @Test
    fun outsidePolygonIsNotActiveButStillMapped() {
        val r = eval(notice(area()), lat = 34.1)
        assertTrue(r.active.isEmpty())
        assertEquals(1, r.mapZones.size)
    }

    @Test
    fun windowOutsideEffectivePeriodIsIgnored() {
        val r = eval(notice(area()), w = window("2026-11-06T12:00:00Z", "2026-11-06T14:00:00Z"))
        assertTrue(r.active.isEmpty())
        assertTrue(r.mapZones.isEmpty())
    }

    @Test
    fun mslFloorAboveFlightIsIgnoredButUnknownGroundIsConservative() {
        val high = notice(area(floor = 4100.0, ref = AltitudeReference.MSL))
        assertTrue(eval(high, ground = 1000.0).active.isEmpty())   // floor 3100 ft AGL
        assertEquals(1, eval(high, ground = null).active.size)       // ground unknown → assume low
        assertEquals(1, eval(high, ground = 3900.0).active.size)     // floor 200 ft AGL
    }

    @Test
    fun aglFloorAt400IsIgnored() {
        assertTrue(eval(notice(area(floor = 400.0))).active.isEmpty())
    }

    @Test
    fun dailyScheduleSeparatesActiveFromInactive() {
        val n = notice(area(start = 14 * 60, end = 23 * 60 + 59))
        val morning = eval(n, w = window("2026-11-05T10:30:00Z", "2026-11-05T12:30:00Z"))
        assertTrue(morning.active.isEmpty())
        assertEquals(1, morning.scheduledOutsideWindow.size)
        val afternoon = eval(n, w = window("2026-11-05T13:30:00Z", "2026-11-05T15:30:00Z"))
        assertEquals(1, afternoon.active.size)
    }

    @Test
    fun dailyScheduleCrossingMidnight() {
        val n = notice(area(start = 22 * 60, end = 2 * 60), effective = TimeWindow(null, null))
        assertEquals(1, eval(n, w = window("2026-11-06T01:00:00Z", "2026-11-06T01:30:00Z")).active.size)
        assertTrue(eval(n, w = window("2026-11-06T03:00:00Z", "2026-11-06T04:00:00Z")).active.isEmpty())
    }

    @Test
    fun permanentTfrIsAlwaysInEffect() {
        val r = eval(notice(area(), effective = TimeWindow(null, null)), w = window("2030-01-01T00:00:00Z", "2030-01-01T02:00:00Z"))
        assertEquals(1, r.active.size)
    }

    @Test
    fun unmappedRegionalTfrAppliesInItsStateOrWhenStateUnknown() {
        // A single-state notice without a boundary (no text states, no other FAA layer covers it).
        val border = TfrNotice("6/7001", "SECURITY", "PHOENIX, AZ", "AZ", areas = emptyList())
        val nationwide = TfrNotice("6/3366", "SECURITY", "USA", "USA", areas = emptyList())
        val snap = TfrSnapshot(listOf(border, nationwide), 0L)
        val w = window("2026-11-05T12:00:00Z", "2026-11-05T14:00:00Z")
        assertEquals(1, evaluator.evaluate(snap, 32.0, -111.0, 2000.0, 400.0, w, "AZ").unmappedRegional.size)
        assertTrue(evaluator.evaluate(snap, 34.0, -117.0, 1000.0, 400.0, w, "CA").unmappedRegional.isEmpty())
        assertEquals(1, evaluator.evaluate(snap, 34.0, -117.0, 1000.0, 400.0, w, null).unmappedRegional.size)
        assertEquals(1, evaluator.evaluate(snap, 34.0, -117.0, 1000.0, 400.0, w, "CA").unmappedNationwideCount)
    }

    // ---------- Rules ----------

    private fun ruleFor(info: AirspaceInfo): RuleResult {
        val now = utc("2026-11-05T12:00:00Z")
        val ctx = AssessmentContext(
            aircraft = Aircraft.getDefault(), pilot = Pilot.getDefault(), weather = null, forecast = null,
            spaceWeather = null, airspace = info, sunData = null, flightWindow = FlightWindow(now, now + 7_200_000L),
            location = LocationInfo(34.0, -117.0), gnss = null, terrainProfile = null, plannedAltitudeAglFt = 400.0,
            hasInternetConnection = true
        )
        return AirspaceRuleEvaluator().evaluate(ctx).ruleResults.first { it.ruleId.startsWith("AIR-TFR") }
    }

    @Test
    fun rulesReflectFeedResult() {
        val active = eval(notice(area())).active
        assertEquals(AssessmentStatus.NO_GO, ruleFor(AirspaceInfo(tfrDataSourced = true, activeTfrs = active)).status)
        assertEquals("AIR-TFR-002", ruleFor(AirspaceInfo(tfrDataSourced = true)).ruleId)
        assertEquals("AIR-TFR-003", ruleFor(AirspaceInfo(tfrDataSourced = false)).ruleId)
        val border = TemporaryFlightRestriction("6/9954", "BORDER", "SECURITY", 0.0, 0.0, 0L, Long.MAX_VALUE, 0.0)
        val unmapped = ruleFor(AirspaceInfo(tfrDataSourced = true, unmappedRegionalTfrs = listOf(border)))
        assertEquals("AIR-TFR-005", unmapped.ruleId)
        assertEquals(AssessmentStatus.CAUTION, unmapped.status)
    }
}

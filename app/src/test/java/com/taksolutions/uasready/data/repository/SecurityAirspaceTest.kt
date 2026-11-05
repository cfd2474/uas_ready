package com.taksolutions.uasready.data.repository

import com.taksolutions.uasready.domain.engine.AssessmentContext
import com.taksolutions.uasready.domain.engine.SecurityAirspaceEvaluator
import com.taksolutions.uasready.domain.engine.TfrCoverage
import com.taksolutions.uasready.domain.engine.TfrEvaluator
import com.taksolutions.uasready.domain.engine.UnmappedTfrCoverage
import com.taksolutions.uasready.domain.engine.rules.AirspaceRuleEvaluator
import com.taksolutions.uasready.domain.geo.GeoMath
import com.taksolutions.uasready.domain.model.*
import com.taksolutions.uasready.ui.presentation.AirspaceCardText
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

/** Chunk 6: US–Mexico border strip, 14 CFR 99.7 sites, DC SFRA/FRZ. Facts: docs/PLATFORM_REFERENCE.md. */
class SecurityAirspaceTest {

    private fun utc(s: String) = Instant.parse(s).toEpochMilli()

    // Real headers (txtDescrUSNS openings) of the live NOTAMs, 2026-11-05.
    private val header9954 = "!FDC 6/9954 ZLA SECURITY CA, AZ..AIRSPACE U.S. MEXICO BORDER..TEMPORARY FLIGHT RESTRICTIONS. OCTOBER 19, 2026-OCTOBER 19, 2027 LOCAL."
    private val header9978 = "!FDC 6/9978 ZAB SECURITY AZ, NM, TX..AIRSPACE U.S. MEXICO BORDER..TEMPORARY FLIGHT RESTRICTIONS."
    private val header7224 = "!FDC 5/7224 ZDC PART 1 OF 9 SECURITY SPECIAL SECURITY INSTRUCTIONS, WASHINGTON, DC. THIS NOTAM CANCELS FDC 4/1783 TO PROVIDE " +
        "SPECIAL SECURITY INSTRUCTIONS FOR UNMANNED AIRCRAFT OPERATIONS (UAS) IN THE WASHINGTON DC METROPOLITAN AREA SPECIAL FLIGHT RULES AREA (SFRA)"

    private val borderEffective = TimeWindow(utc("2026-10-20T00:00:00Z"), utc("2027-10-19T23:59:00Z"))
    private val border9954 = TfrNotice("6/9954", "SECURITY", "U.S. MEXICO BORDER, AZ, Tuesday, October 20, 2026", "AZ", "99.7",
        borderEffective, textHeader = header9954, textStates = listOf("CA", "AZ"))
    private val border9978 = TfrNotice("6/9978", "SECURITY", "U.S. MEXICO BORDER, TX", "TX", "99.7",
        borderEffective, textHeader = header9978, textStates = listOf("AZ", "NM", "TX"))
    private val dc7224 = TfrNotice("5/7224", "SECURITY", "WASHINGTON, DC, Wednesday, September 3, 2025 UTC", "DC", "99.7",
        TimeWindow(utc("2025-09-03T15:10:00Z"), null), textHeader = header7224)
    private val otherUnmapped = TfrNotice("6/7001", "SECURITY", "SOMEWHERE, TX", "TX", "99.7", borderEffective)

    private val window = FlightWindow(utc("2026-11-05T12:00:00Z"), utc("2026-11-05T14:00:00Z"))

    // ---------- NOTAM text header ----------

    @Test
    fun statesAreReadFromTheNotamHeader() {
        assertEquals(listOf("CA", "AZ"), TfrXmlParser.statesInHeader(header9954))
        assertEquals(listOf("AZ", "NM", "TX"), TfrXmlParser.statesInHeader(header9978))
        assertEquals(listOf("NH"), TfrXmlParser.statesInHeader("!FDC 6/5156 ZBW NH..AIRSPACE BERLIN, NH..TEMPORARY FLIGHT RESTRICTIONS."))
        assertTrue(TfrXmlParser.statesInHeader(header7224).isEmpty())
        assertTrue(TfrXmlParser.statesInHeader("").isEmpty())
    }

    @Test
    fun parserKeepsTheHeader() {
        val xml = "﻿<XNOTAM-Update><Group><Add><Not><NotUid><txtLocalName>6/9954</txtLocalName></NotUid>" +
            "<TfrNot><codeType>99.7</codeType></TfrNot><txtDescrUSNS>$header9954   MORE   TEXT</txtDescrUSNS></Not></Add></Group></XNOTAM-Update>"
        val parsed = TfrXmlParser().parse(xml)
        assertTrue(parsed.textHeader.startsWith("!FDC 6/9954 ZLA SECURITY CA, AZ"))
        assertEquals(listOf("CA", "AZ"), parsed.textStates)
    }

    // ---------- Which unmapped TFRs another FAA layer covers ----------

    @Test
    fun borderAndDcNoticesAreRecognised() {
        assertEquals(TfrCoverage.MEXICO_BORDER, UnmappedTfrCoverage.of(border9954))
        assertEquals(TfrCoverage.MEXICO_BORDER, UnmappedTfrCoverage.of(border9978))
        assertEquals(TfrCoverage.DC_SFRA, UnmappedTfrCoverage.of(dc7224))
        assertEquals(TfrCoverage.NONE, UnmappedTfrCoverage.of(otherUnmapped))
        // A DC notice that is not the UAS SFRA instructions is not swallowed.
        assertEquals(TfrCoverage.NONE, UnmappedTfrCoverage.of(dc7224.copy(textHeader = "!FDC 6/1111 ZDC DC..AIRSPACE WASHINGTON, DC..VIP")))
    }

    @Test
    fun californiaAndNewMexicoBorderPilotsAreNotMissedWhenLayersFail() {
        val snap = TfrSnapshot(listOf(border9954, border9978, dc7224), 0L)
        val e = TfrEvaluator()
        fun regional(state: String?) = e.evaluate(snap, 32.5, -117.0, 100.0, 400.0, window, state, securityLayersSourced = false)
            .unmappedRegional.map { it.id }.toSet()
        assertEquals(setOf("6/9954"), regional("CA")) // listed "AZ" by the FAA list
        assertEquals(setOf("6/9978"), regional("NM")) // listed "TX"
        assertEquals(setOf("5/7224"), regional("MD")) // listed "DC"
        assertEquals(setOf("5/7224"), regional("VA"))
        assertTrue(regional("KS").isEmpty())
    }

    @Test
    fun coveredNoticesLeaveTheUnmappedListWhenLayersAnswer() {
        val snap = TfrSnapshot(listOf(border9954, border9978, dc7224, otherUnmapped), 0L)
        val r = TfrEvaluator().evaluate(snap, 31.3, -110.9, 3800.0, 400.0, window, "TX", securityLayersSourced = true)
        assertEquals(listOf("6/7001"), r.unmappedRegional.map { it.id })
    }

    // ---------- Geometry ----------

    private val square = listOf(32.0 to -111.0, 32.0 to -110.9, 31.9 to -110.9, 31.9 to -111.0) // ~6 NM tall

    @Test
    fun distanceToPolygon() {
        assertEquals(0.0, GeoMath.distanceToPolygonNm(31.95, -110.95, square), 0.0)
        // 0.01° latitude north of the top edge ≈ 0.6 NM
        assertEquals(0.6, GeoMath.distanceToPolygonNm(32.01, -110.95, square), 0.01)
        assertTrue(GeoMath.distanceToPolygonNm(32.1, -110.95, square) > 5.0)
    }

    private fun site(id: String, polygons: List<List<Pair<Double, Double>>> = listOf(square)) =
        NationalSecurityUasSite(id, "Site $id", "DHS CBP", "Surface", "400' AGL", "WAIVERS@EXAMPLE.GOV", polygons = polygons)

    @Test
    fun evaluatorSeparatesInsideNearbyAndFar() {
        val far = listOf(40.0 to -100.0, 40.0 to -99.9, 39.9 to -99.9, 39.9 to -100.0)
        val snapshot = SecurityAirspaceSnapshot(
            sitesAtLaunchPoint = listOf(site("A", emptyList())),
            nearbySites = listOf(site("A"), site("B"), site("C", listOf(far))),
            dcSfraZone = null, fetchedEpochMs = 0L
        )
        // Launch 0.6 NM north of square: B is nearby; A is "inside" per the server; C is far.
        val r = SecurityAirspaceEvaluator().evaluate(snapshot, 32.01, -110.95)
        assertEquals(listOf("A"), r.sitesAtLaunchPoint.map { it.faaId })
        assertEquals(listOf("B"), r.sitesNearby.map { it.faaId })
        assertEquals(3, r.mapZones.size)
        assertTrue(r.mapZones.all { it.type == AirspaceZoneType.RESTRICTED_ZONE })
        assertTrue(r.sitesNearby.all { it.polygons.isEmpty() })
    }

    @Test
    fun borderStripIsWidenedToTheNotamsOneMile() {
        val strip = site("20261016-DHS-ZLA-1").copy(isMexicoBorderStrip = true)
        val base = site("BASE") // an ordinary site is not widened
        val snapshot = SecurityAirspaceSnapshot(emptyList(), listOf(strip, base.copy(faaId = "BASE")), null, 0L)
        // 0.1 NM north of the polygon: inside per the NOTAM's 1 NM, outside the FAA polygon.
        val r = SecurityAirspaceEvaluator().evaluate(snapshot, 32.0 + 0.1 / 60.0, -110.95)
        assertEquals(listOf("20261016-DHS-ZLA-1"), r.sitesAtLaunchPoint.map { it.faaId })
        assertEquals(listOf("BASE"), r.sitesNearby.map { it.faaId })
        // 0.3 NM north: past the margin, so nearby (CAUTION) only.
        val far = SecurityAirspaceEvaluator().evaluate(snapshot, 32.0 + 0.3 / 60.0, -110.95)
        assertTrue(far.sitesAtLaunchPoint.isEmpty())
        assertEquals(setOf("20261016-DHS-ZLA-1", "BASE"), far.sitesNearby.map { it.faaId }.toSet())
    }

    // ---------- Rules + Home card ----------

    private fun rules(info: AirspaceInfo): List<RuleResult> {
        val now = utc("2026-11-05T12:00:00Z")
        val ctx = AssessmentContext(
            aircraft = Aircraft.getDefault(), pilot = Pilot.getDefault(), weather = null, forecast = null,
            spaceWeather = null, airspace = info, sunData = null, flightWindow = FlightWindow(now, now + 7_200_000L),
            location = LocationInfo(32.5, -117.0), gnss = null, terrainProfile = null, plannedAltitudeAglFt = 400.0,
            hasInternetConnection = true
        )
        return AirspaceRuleEvaluator().evaluate(ctx).ruleResults
    }
    private fun rule(info: AirspaceInfo, prefix: String) = rules(info).filter { it.ruleId.startsWith(prefix) }

    private val checked = AirspaceInfo(tfrDataSourced = true, securityAirspaceSourced = true)

    @Test
    fun securityRules() {
        assertEquals(listOf("AIR-NSUAS-002" to AssessmentStatus.CAUTION),
            rule(AirspaceInfo(tfrDataSourced = true), "AIR-NSUAS").map { it.ruleId to it.status })
        assertEquals(listOf("AIR-NSUAS-003" to AssessmentStatus.GO), rule(checked, "AIR-NSUAS").map { it.ruleId to it.status })

        val inside = rule(checked.copy(nationalSecuritySitesAtLaunch = listOf(site("20261016-DHS-ZLA-1"))), "AIR-NSUAS").single()
        assertEquals("AIR-NSUAS-001", inside.ruleId)
        assertEquals(AssessmentStatus.NO_GO, inside.status)
        assertTrue(inside.explanation.contains("WAIVERS@EXAMPLE.GOV"))

        val near = rule(checked.copy(nationalSecuritySitesNearby = listOf(site("B"))), "AIR-NSUAS").single()
        assertEquals("AIR-NSUAS-004" to AssessmentStatus.CAUTION, near.ruleId to near.status)

        assertEquals(listOf("AIR-DC-001" to AssessmentStatus.NO_GO),
            rule(checked.copy(dcSfraZone = DcSfraZone.FRZ), "AIR-DC").map { it.ruleId to it.status })
        assertEquals(listOf("AIR-DC-002" to AssessmentStatus.CAUTION),
            rule(checked.copy(dcSfraZone = DcSfraZone.SFRA), "AIR-DC").map { it.ruleId to it.status })
        assertTrue(rule(checked.copy(dcSfraZone = DcSfraZone.SFRA), "AIR-NSUAS").isEmpty())
    }

    @Test
    fun cleanCheckedLocationIsGo() {
        assertEquals(AssessmentStatus.GO, rules(checked).maxByOrNull { it.status.priority }!!.status)
    }

    @Test
    fun homeCardNamesTheRestriction() {
        fun card(info: AirspaceInfo): Pair<String, String> {
            val rr = rules(info)
            val worst = rr.maxByOrNull { it.status.priority }!!.status
            val result = AssessmentResult(
                overallStatus = worst, primaryHeadline = "", primaryReasons = emptyList(),
                categoryAssessments = listOf(CategoryAssessment(AssessmentCategory.AIRSPACE, worst, rr, "")),
                evaluatedAtEpochMs = 0L, flightWindow = window, aircraft = Aircraft.getDefault(),
                pilot = Pilot.getDefault(), location = LocationInfo(32.5, -117.0)
            )
            val c = AirspaceCardText.of(info, result)
            return c.text.primary to c.text.secondary
        }
        assertEquals("Class G • NO-DRONE ZONE" to "National security: waiver required",
            card(checked.copy(nationalSecuritySitesAtLaunch = listOf(site("X")))))
        assertEquals("Class G • DC FRZ" to "TSA/FAA waiver required", card(checked.copy(dcSfraZone = DcSfraZone.FRZ)))
        assertEquals("Class G • No TFRs" to "No-drone zones unverified", card(AirspaceInfo(tfrDataSourced = true)))
        assertEquals("Class G • No TFRs" to "No flight restrictions", card(checked))
    }

    // ---------- Repository (MockWebServer) ----------

    private lateinit var server: MockWebServer
    private var pointBody = """{"features":[{"attributes":{"OBJECTID":2291,"Proponent":"DHS","Branch":"CBP","Base":"ZLA","Facility":"1","FAA_ID":"20261016-DHS-ZLA-1","POC":"SWBDRONEZONEWAIVERS@CBP.DHS.GOV CBP AT 951-570-4185","Floor":"Surface","Ceiling":"400' AGL"}}]}"""
    private var nearbyBody = """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{"OBJECTID":2291,"Base":"ZLA","Facility":"1","FAA_ID":"20261016-DHS-ZLA-1","POC":"SWBDRONEZONEWAIVERS@CBP.DHS.GOV","Floor":"Surface","Ceiling":"400' AGL"},
        "geometry":{"type":"Polygon","coordinates":[[[-117.1,32.53],[-117.0,32.53],[-117.0,32.55],[-117.1,32.55],[-117.1,32.53]]]}}]}"""
    private var dcBody = """{"features":[]}"""

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.startsWith("/ns") && path.contains("returnGeometry=false") -> MockResponse().setBody(pointBody)
                    path.startsWith("/ns") -> MockResponse().setBody(nearbyBody)
                    path.startsWith("/boundary") -> MockResponse().setBody(dcBody)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
    }

    @After
    fun stop() = server.shutdown()

    private fun repo() = LiveSecurityAirspaceRepository(
        nationalSecurityUrl = server.url("/ns/query").toString(),
        boundaryAirspaceUrl = server.url("/boundary/query").toString(),
        client = ArcGisQueryClient(quotaRetryDelayMs = 1L)
    )

    @Test
    fun repositoryReadsAllThreeQueries() = runBlocking {
        dcBody = """{"features":[{"attributes":{"SECTOR":"DC SFRA"}},{"attributes":{"SECTOR":"DC FRZ"}}]}"""
        val s = repo().getSecurityAirspace(32.54, -117.05).getOrThrow()
        val inside = s.sitesAtLaunchPoint.single()
        assertEquals("20261016-DHS-ZLA-1", inside.faaId)
        assertEquals("US–Mexico border 1 NM UAS zone (ZLA)", inside.name)
        assertEquals("DHS CBP", inside.agency)
        assertEquals(1, s.nearbySites.single().polygons.size)
        assertTrue(s.nearbySites.single().isMexicoBorderStrip)
        assertEquals(DcSfraZone.FRZ, s.dcSfraZone) // FRZ wins over SFRA
    }

    @Test
    fun anyArcGisErrorFailsTheLookup() = runBlocking {
        dcBody = """{"error":{"code":429,"message":"Unable to perform query. Too many requests."}}"""
        assertTrue(repo().getSecurityAirspace(32.54, -117.05).isFailure)
    }

    @Test
    fun truncatedResultFailsTheLookup() = runBlocking {
        pointBody = """{"exceededTransferLimit":true,"features":[]}"""
        assertTrue(repo().getSecurityAirspace(32.54, -117.05).isFailure)
    }
}

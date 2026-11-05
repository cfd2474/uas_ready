package com.taksolutions.uasready.data.repository

import com.taksolutions.uasready.domain.model.AirspaceClass
import com.taksolutions.uasready.domain.model.AirspaceInfo
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Nationwide airspace accuracy survey (live network, opt-in: ./gradlew testDebugUnitTest -PliveTests).
 *
 * For each survey point, compares what the APP reports ([LiveAirspaceRepository]) against an
 * independent TRUTH built from:
 *  - FAA server-side point intersection on full-resolution geometry (no client simplification,
 *    no client point-in-polygon), and
 *  - correct floor semantics: LOWER_CODE=SFC → value is AGL; MSL → subtract real ground
 *    elevation; LOWER_UOM=FL → flight level (×100 ft MSL). Airspace counts only if its floor is
 *    below the 400 ft AGL Part 107 ceiling at that point.
 *
 * Writes a markdown report to app/build/reports/airspace-survey.md.
 */
class AirspaceAccuracySurveyTest {

    private data class SurveyPoint(
        val name: String,
        val region: String,
        val lat: Double,
        val lon: Double,
        /** Published expectation (FAA charts / JO 7400.11). Null = defer to FAA data truth. */
        val expected: AirspaceClass?
    )

    private data class TruthHit(val label: String, val cls: AirspaceClass, val floorAglFt: Double)

    private val points = listOf(
        // Class B primary airports, coast to coast
        SurveyPoint("KLAX Los Angeles", "West", 33.9425, -118.4081, AirspaceClass.CLASS_B),
        SurveyPoint("KSFO San Francisco", "West", 37.6213, -122.3790, AirspaceClass.CLASS_B),
        SurveyPoint("KSEA Seattle", "Northwest", 47.4502, -122.3088, AirspaceClass.CLASS_B),
        SurveyPoint("KDEN Denver", "Mountain", 39.8561, -104.6737, AirspaceClass.CLASS_B),
        SurveyPoint("KORD Chicago", "Midwest", 41.9742, -87.9073, AirspaceClass.CLASS_B),
        SurveyPoint("KATL Atlanta", "Southeast", 33.6407, -84.4277, AirspaceClass.CLASS_B),
        SurveyPoint("KMIA Miami", "Southeast", 25.7959, -80.2870, AirspaceClass.CLASS_B),
        SurveyPoint("KJFK New York", "Northeast", 40.6413, -73.7781, AirspaceClass.CLASS_B),
        SurveyPoint("KBOS Boston", "Northeast", 42.3656, -71.0096, AirspaceClass.CLASS_B),
        SurveyPoint("PHNL Honolulu", "Hawaii", 21.3187, -157.9225, AirspaceClass.CLASS_B),
        // Class C
        SurveyPoint("KONT Ontario", "West", 34.0560, -117.6012, AirspaceClass.CLASS_C),
        SurveyPoint("KTUS Tucson", "Southwest", 32.1161, -110.9410, AirspaceClass.CLASS_C),
        SurveyPoint("KABQ Albuquerque", "Southwest", 35.0402, -106.6090, AirspaceClass.CLASS_C),
        SurveyPoint("KBOI Boise", "Mountain", 43.5644, -116.2228, AirspaceClass.CLASS_C),
        SurveyPoint("KAUS Austin", "South", 30.1975, -97.6664, AirspaceClass.CLASS_C),
        SurveyPoint("PANC Anchorage", "Alaska", 61.1743, -149.9983, AirspaceClass.CLASS_C),
        // Class D (incl. one beneath a Class B shelf)
        SurveyPoint("KCNO Chino", "West", 33.9747, -117.6366, AirspaceClass.CLASS_D),
        SurveyPoint("KBJC Rocky Mtn Metro", "Mountain", 39.9088, -105.1172, AirspaceClass.CLASS_D),
        SurveyPoint("KFTW Fort Worth Meacham", "South", 32.8198, -97.3624, AirspaceClass.CLASS_D),
        SurveyPoint("KHHR Hawthorne (under LAX B shelf)", "West", 33.9228, -118.3352, AirspaceClass.CLASS_D),
        // Class E surface area (non-towered)
        SurveyPoint("KBKE Baker City E2", "Northwest", 44.8452, -117.8314, AirspaceClass.CLASS_E_SURFACE),
        // Class G
        SurveyPoint("Rural Kansas", "Plains", 38.50, -99.00, null),
        SurveyPoint("Rural Montana", "Plains", 47.00, -109.00, null),
        SurveyPoint("Under DEN B shelf (6500 MSL, ~600 ft AGL)", "Mountain", 39.6588, -104.6044, AirspaceClass.CLASS_G),
        // Special use
        SurveyPoint("P-56 White House", "Northeast", 38.8977, -77.0365, AirspaceClass.SPECIAL_USE),
        SurveyPoint("R-2505 China Lake", "West", 35.70, -117.65, AirspaceClass.SPECIAL_USE),
        SurveyPoint("W-147B Gulf (floor FL230 only)", "Gulf", 28.1644, -92.9940, AirspaceClass.CLASS_G),
        // Restricted areas whose floor is a flight level, with no low-level P/R beneath (found from FAA data)
        SurveyPoint("R-5002G Warren Grove NJ (floor FL200 only)", "Northeast", 39.6741, -74.3048, AirspaceClass.CLASS_G),
        SurveyPoint("R-4008 Chesapeake MD (floor FL250 only)", "Northeast", 37.9400, -76.3564, AirspaceClass.CLASS_G),
        SurveyPoint("R-5107F White Sands NM (floor FL240 only)", "Southwest", 32.8683, -105.7970, AirspaceClass.CLASS_G),
    )

    @Before
    fun requireLiveTestsFlag() {
        assumeTrue("Live survey skipped (enable with -PliveTests)", System.getProperty("uasready.liveTests") == "true")
    }

    @Test
    fun nationwideAirspaceSurvey() = runBlocking {
        val repo = LiveAirspaceRepository()
        val rows = mutableListOf<String>()
        val mismatches = mutableListOf<String>()
        var appThrottled = 0

        for (p in points) {
            // The FAA ArcGIS quota is shared org-wide; when throttled, wait and retry (counted in the report).
            var appResult: Result<AirspaceInfo> = Result.failure(IllegalStateException("not run"))
            var appMs = 0L
            for (attempt in 1..4) {
                val t0 = System.currentTimeMillis()
                appResult = repo.getAirspaceInfo(p.lat, p.lon)
                appMs = System.currentTimeMillis() - t0
                if (appResult.isSuccess || appResult.exceptionOrNull()?.message?.contains("Too many requests", true) != true) break
                appThrottled++
                println("  throttled (app) at ${p.name}, retry $attempt in 30s")
                Thread.sleep(30_000)
            }
            val app = appResult.getOrNull()

            val ground = groundElevationFt(p.lat, p.lon)
            val hits = truthHits(p.lat, p.lon, ground)
            val truth = hits.filter { it.floorAglFt < 400.0 }.maxByOrNull { priority(it.cls) }?.cls ?: AirspaceClass.CLASS_G

            val appCls = app?.primaryClass
            val agree = appCls == truth
            val expectOk = p.expected == null || p.expected == truth
            if (!agree) mismatches += "${p.name}: app=${appCls ?: "FAILURE(${appResult.exceptionOrNull()?.message})"} truth=$truth"

            val ignored = hits.filter { it.floorAglFt >= 400.0 }.joinToString("; ") { "${it.label} @${it.floorAglFt.toInt()} AGL" }
            val counted = hits.filter { it.floorAglFt < 400.0 }.joinToString("; ") { "${it.label} @${it.floorAglFt.toInt()} AGL" }
            rows += String.format(
                Locale.US, "| %s | %s | %s | %s | %s | %s | %s | %.0f | %d | %d | %s | %s |",
                if (agree) "✅" else "❌", p.name, p.region, p.expected ?: "—", truth, appCls ?: "FAILURE",
                if (expectOk) "yes" else "NO", ground, app?.zones?.size ?: 0, appMs, counted.ifBlank { "—" }, ignored.ifBlank { "—" }
            )
            println("${if (agree) "OK  " else "FAIL"} ${p.name}: app=$appCls truth=$truth expected=${p.expected} ground=${ground.toInt()}ft ${appMs}ms")
            Thread.sleep(4000) // stay well inside the FAA ArcGIS 6000 request-units/minute quota
        }

        val report = buildString {
            appendLine("# Airspace accuracy survey")
            appendLine()
            appendLine("App = `LiveAirspaceRepository`. Truth = FAA server-side point intersection (full resolution) + floor-vs-terrain logic, counting airspace whose floor is < 400 ft AGL.")
            appendLine()
            appendLine("| | Point | Region | Published | Truth | App | Published=Truth | Ground ft | App zones | App ms | Counted (floor < 400 AGL) | Ignored (floor ≥ 400 AGL) |")
            appendLine("|---|---|---|---|---|---|---|---|---|---|---|---|")
            rows.forEach { appendLine(it) }
            appendLine()
            appendLine("**${points.size - mismatches.size}/${points.size} agree.** App lookups throttled by FAA org quota during run: $appThrottled.")
            mismatches.forEach { appendLine("- $it") }
        }
        File("build/reports").mkdirs()
        File("build/reports/airspace-survey.md").writeText(report)
        println(report)

        assertTrue("Airspace mismatches:\n" + mismatches.joinToString("\n"), mismatches.isEmpty())
    }

    // ---- Truth -------------------------------------------------------------------------------

    private fun truthHits(lat: Double, lon: Double, groundFt: Double): List<TruthHit> {
        val hits = mutableListOf<TruthHit>()
        val cls = pointQuery("Class_Airspace", lat, lon, "NAME,CLASS,LOCAL_TYPE,LOWER_VAL,LOWER_UOM,LOWER_CODE")
        for (a in cls) {
            val local = a.optString("LOCAL_TYPE")
            val c = when (a.optString("CLASS")) {
                "B" -> AirspaceClass.CLASS_B
                "C" -> AirspaceClass.CLASS_C
                "D" -> AirspaceClass.CLASS_D
                "E" -> if (local in setOf("CLASS_E2", "CLASS_E3", "CLASS_E4")) AirspaceClass.CLASS_E_SURFACE else AirspaceClass.CLASS_E
                else -> continue // A, G, Other
            }
            hits += TruthHit("${a.optString("NAME")} [$local]", c, floorAgl(a, groundFt))
        }
        val sua = pointQuery("Special_Use_Airspace", lat, lon, "NAME,TYPE_CODE,LOWER_VAL,LOWER_UOM,LOWER_CODE")
        for (a in sua) {
            // Only Prohibited/Restricted change the app's primary class; MOA/Alert/Warning are advisory.
            if (a.optString("TYPE_CODE") !in setOf("P", "R")) {
                hits += TruthHit("${a.optString("NAME")} [${a.optString("TYPE_CODE")}, advisory]", AirspaceClass.CLASS_G, floorAgl(a, groundFt))
                continue
            }
            hits += TruthHit("${a.optString("NAME")} [${a.optString("TYPE_CODE")}]", AirspaceClass.SPECIAL_USE, floorAgl(a, groundFt))
        }
        return hits
    }

    private fun floorAgl(a: JSONObject, groundFt: Double): Double {
        val v = a.optString("LOWER_VAL").toDoubleOrNull() ?: 0.0
        return when {
            a.optString("LOWER_UOM") == "FL" -> v * 100.0 - groundFt
            a.optString("LOWER_CODE") == "SFC" -> v
            a.optString("LOWER_CODE") == "MSL" -> v - groundFt
            else -> v * 100.0 - groundFt // STD
        }
    }

    private fun pointQuery(service: String, lat: Double, lon: Double, fields: String): List<JSONObject> {
        repeat(4) { attempt ->
            try {
                return pointQueryOnce(service, lat, lon, fields)
            } catch (e: IllegalStateException) {
                if (e.message?.contains("429") != true) throw e
                println("  throttled (truth) $service, retry ${attempt + 1} in 30s")
                Thread.sleep(30_000)
            }
        }
        return pointQueryOnce(service, lat, lon, fields)
    }

    private fun pointQueryOnce(service: String, lat: Double, lon: Double, fields: String): List<JSONObject> {
        val url = "https://services6.arcgis.com/ssFJjBXIUyZDrSYZ/arcgis/rest/services/$service/FeatureServer/0/query" +
            "?where=1%3D1&geometry=$lon,$lat&geometryType=esriGeometryPoint&inSR=4326" +
            "&spatialRel=esriSpatialRelIntersects&outFields=${enc(fields)}&returnGeometry=false&f=json"
        val json = getJson(url)
        json.optJSONObject("error")?.let { error("FAA $service truth query error: $it") }
        val features = json.getJSONArray("features")
        return (0 until features.length()).map { features.getJSONObject(it).getJSONObject("attributes") }
    }

    private fun groundElevationFt(lat: Double, lon: Double): Double {
        repeat(5) { attempt ->
            try {
                val json = getJson("https://api.open-meteo.com/v1/elevation?latitude=$lat&longitude=$lon")
                return json.getJSONArray("elevation").getDouble(0) * 3.28084
            } catch (e: Exception) {
                Thread.sleep(5000L * (attempt + 1))
            }
        }
        error("Elevation lookup failed for $lat,$lon")
    }

    private fun getJson(url: String): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000; readTimeout = 30_000
            setRequestProperty("User-Agent", "UASReady-airspace-survey/1.0")
        }
        try {
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode} for $url")
            return JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun priority(c: AirspaceClass) = when (c) {
        AirspaceClass.SPECIAL_USE -> 6
        AirspaceClass.CLASS_B -> 5
        AirspaceClass.CLASS_C -> 4
        AirspaceClass.CLASS_D -> 3
        AirspaceClass.CLASS_E_SURFACE -> 2
        AirspaceClass.CLASS_E -> 1
        AirspaceClass.CLASS_G -> 0
    }
}

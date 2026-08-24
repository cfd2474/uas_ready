package com.uasready.data.nasr

import com.uasready.domain.model.TemporaryFlightRestriction
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.InputStream
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory

data class ParsedTfr(
    val notamId: String,
    val issueDate: String,
    val type: String,
    val description: String,
    val floorFt: Double = 0.0,
    val ceilingFt: Double = 18000.0,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val centerLat: Double? = null,
    val centerLon: Double? = null,
    val radiusNm: Double = 5.0,
    val polygonCoordinates: List<Pair<Double, Double>> = emptyList(),
    val isHazard91137: Boolean = false
) {
    fun toDomainModel(): TemporaryFlightRestriction {
        return TemporaryFlightRestriction(
            id = notamId,
            description = description,
            type = if (isHazard91137) "91.137 Hazard / Firefighting" else type,
            minAltitudeFt = floorFt,
            maxAltitudeFt = ceilingFt,
            effectiveStartEpochMs = startEpochMs,
            effectiveEndEpochMs = endEpochMs,
            radiusNm = radiusNm,
            centerLat = centerLat,
            centerLon = centerLon
        )
    }
}

object TfrXmlParser {

    private val DATE_FORMAT = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Parses an FAA TFR detail XML stream into a [ParsedTfr] object.
     * Guaranteed uniqueness based on NOTAM number + Issue Date.
     */
    fun parseDetailXml(inputStream: InputStream): ParsedTfr? {
        val xmlContent = inputStream.bufferedReader().use { it.readText() }
        return parseDetailXml(xmlContent)
    }

    fun parseDetailXml(xmlContent: String): ParsedTfr? {
        if (xmlContent.isBlank()) return null
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(xmlContent)))
            doc.documentElement.normalize()

            var notamId = getTagValue(doc.documentElement, "NOTAM_ID", "NOTAMNUMBER", "NOTAM_NUM")
            var issueDate = getTagValue(doc.documentElement, "ISSUE_DATE", "ISSUEDATE", "DATEISSUED")
            var type = getTagValue(doc.documentElement, "TYPE", "TFR_TYPE", "REASON").ifBlank { "TFR" }
            var description = getTagValue(doc.documentElement, "DESCRIPTION", "TXT_DESCR", "NOTAM_TEXT")
            val floorStr = getTagValue(doc.documentElement, "ALT_LOWER", "FLOOR")
            val ceilingStr = getTagValue(doc.documentElement, "ALT_UPPER", "CEILING")
            val startStr = getTagValue(doc.documentElement, "EFFECTIVE_START", "START_DATE", "VAL_START")
            val endStr = getTagValue(doc.documentElement, "EFFECTIVE_END", "END_DATE", "VAL_END")
            val latStr = getTagValue(doc.documentElement, "LATITUDE", "CENTER_LAT")
            val lonStr = getTagValue(doc.documentElement, "LONGITUDE", "CENTER_LON")
            val radStr = getTagValue(doc.documentElement, "RADIUS", "RADIUS_NM")
            val coordStr = getTagValue(doc.documentElement, "COORDINATES", "POSLIST")

            val now = System.currentTimeMillis()
            val startEpochMs = parseDate(startStr) ?: now
            val endEpochMs = parseDate(endStr) ?: (now + 86400000L * 7)
            val floorFt = parseAltitude(floorStr)
            val ceilingFt = if (ceilingStr.isNotBlank()) parseAltitude(ceilingStr) else 18000.0
            val centerLat = latStr.toDoubleOrNull()
            val centerLon = lonStr.toDoubleOrNull()
            val radiusNm = radStr.toDoubleOrNull() ?: 5.0

            val polygonPoints = if (coordStr.isNotBlank()) parseCoordinates(coordStr) else emptyList()

            val isHazard91137 = type.contains("91.137", true) ||
                    description.contains("91.137", true) ||
                    type.contains("HAZARD", true) ||
                    description.contains("HAZARD", true) ||
                    type.contains("FIRE", true) ||
                    description.contains("FIRE", true)

            if (notamId.isBlank()) {
                val notamMatcher = Pattern.compile("(\\d/\\d{4})").matcher(xmlContent)
                if (notamMatcher.find()) {
                    notamId = notamMatcher.group(1) ?: "TFR-LIVE"
                } else {
                    notamId = "TFR-${now % 10000}"
                }
            }

            if (issueDate.isBlank()) {
                issueDate = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            }

            val finalPolygon = if (polygonPoints.size >= 3) {
                polygonPoints
            } else if (centerLat != null && centerLon != null) {
                GeometryUtils.generateCirclePolygon(centerLat, centerLon, radiusNm * 1852.0, 24)
            } else {
                emptyList()
            }

            return ParsedTfr(
                notamId = notamId,
                issueDate = issueDate,
                type = type,
                description = description.ifBlank { "Temporary Flight Restriction $notamId" },
                floorFt = floorFt,
                ceilingFt = ceilingFt,
                startEpochMs = startEpochMs,
                endEpochMs = endEpochMs,
                centerLat = centerLat,
                centerLon = centerLon,
                radiusNm = radiusNm,
                polygonCoordinates = finalPolygon,
                isHazard91137 = isHazard91137
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun getTagValue(root: Element, vararg tagNames: String): String {
        for (tag in tagNames) {
            val list = root.getElementsByTagName(tag)
            if (list.length > 0) {
                val node = list.item(0)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    val text = node.textContent?.trim() ?: ""
                    if (text.isNotBlank()) return text
                }
            }
        }
        return ""
    }

    private fun parseAltitude(text: String): Double {
        val clean = text.replace(",", "").replace("FT", "").replace("MSL", "").replace("AGL", "").trim()
        val num = clean.toDoubleOrNull()
        if (num != null) return num
        if (clean.startsWith("FL", true)) {
            val fl = clean.substring(2).toDoubleOrNull()
            if (fl != null) return fl * 100.0
        }
        return 0.0
    }

    private fun parseDate(text: String): Long? {
        if (text.isBlank()) return null
        try {
            return DATE_FORMAT.parse(text)?.time
        } catch (_: Exception) {}
        try {
            return ISO_DATE_FORMAT.parse(text)?.time
        } catch (_: Exception) {}
        return null
    }

    private fun parseCoordinates(text: String): List<Pair<Double, Double>> {
        val list = mutableListOf<Pair<Double, Double>>()
        val tokens = text.trim().split(Regex("[\\s,]+"))
        var i = 0
        while (i + 1 < tokens.size) {
            val v1 = tokens[i].toDoubleOrNull()
            val v2 = tokens[i + 1].toDoubleOrNull()
            if (v1 != null && v2 != null) {
                val (lat, lon) = if (abs(v1) <= 90.0 && abs(v2) > 90.0) Pair(v1, v2) else Pair(v2, v1)
                list.add(Pair(lat, lon))
            }
            i += 2
        }
        return list
    }

    private fun abs(d: Double): Double = if (d < 0) -d else d
}

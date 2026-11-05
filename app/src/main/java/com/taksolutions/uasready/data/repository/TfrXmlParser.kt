package com.taksolutions.uasready.data.repository

import com.taksolutions.uasready.domain.model.AltitudeReference
import com.taksolutions.uasready.domain.model.TfrArea
import com.taksolutions.uasready.domain.model.TimeWindow
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.xml.parsers.DocumentBuilderFactory

/** The parts of one FAA TFR detail XML (XNOTAM-Update) that the app uses. */
data class ParsedTfrDetail(
    val notamId: String,
    val cfrSection: String?,
    val effective: TimeWindow,
    val areas: List<TfrArea>
)

/**
 * Parses `https://tfr.faa.gov/download/detail_<yr>_<seq>.xml`.
 *
 * Contract (docs/PLATFORM_REFERENCE.md, verified 2026-11-05):
 * - File starts with a UTF-8 BOM.
 * - `dateEffective` / `dateExpire` are UTC even when `codeTimeZone` names a local zone; either may be absent
 *   (permanent / until further notice).
 * - Altitudes: `codeDistVer*` HEI = AGL, ALT = MSL; `uomDistVer*` FT or FL (FL = value × 100 ft MSL).
 * - Area polygon: `abdMergedArea` whose `AbdUid/AseUid/codeId` matches the area's `AseUid/codeId`;
 *   vertices `geoLat` "26.7214855N", `geoLong` "080.10472222W" (WGS84, already polygonized).
 */
class TfrXmlParser {

    fun parse(xml: String): ParsedTfrDetail {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            // No external entities or DTDs: the document is untrusted network input.
            trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            trySetFeature("http://xml.org/sax/features/external-general-entities", false)
            trySetFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isExpandEntityReferences = false
        }
        val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml.trimStart('﻿'))))
        val not = doc.getElementsByTagName("Not").item(0) as? Element
            ?: throw IllegalArgumentException("TFR detail has no <Not> element")

        val notUid = not.child("NotUid")
        val notamId = notUid?.text("txtLocalName")
            ?: throw IllegalArgumentException("TFR detail has no NOTAM id")
        val effective = TimeWindow(parseUtc(not.text("dateEffective")), parseUtc(not.text("dateExpire")))
        val tfrNot = not.child("TfrNot")
        val cfr = tfrNot?.text("codeType")

        val polygons: Map<String, List<Pair<Double, Double>>> = not.descendants("abdMergedArea").mapNotNull { merged ->
            val id = merged.child("AbdUid")?.child("AseUid")?.text("codeId") ?: merged.text("txtRmk") ?: return@mapNotNull null
            val ring = merged.children("Avx").mapNotNull { avx ->
                val lat = parseCoordinate(avx.text("geoLat"))
                val lon = parseCoordinate(avx.text("geoLong"))
                if (lat != null && lon != null) lat to lon else null
            }
            id to ring
        }.toMap()

        val areas = not.descendants("aseTFRArea").map { area ->
            val id = area.child("AseUid")?.text("codeId")
            val floor = parseAltitude(area.text("codeDistVerLower"), area.text("valDistVerLower"), area.text("uomDistVerLower"))
                ?: Altitude(0.0, AltitudeReference.AGL) // missing floor → surface (conservative)
            val ceiling = parseAltitude(area.text("codeDistVerUpper"), area.text("valDistVerUpper"), area.text("uomDistVerUpper"))
            val scheduleGroups = area.children("ScheduleGroup")
            val windows = scheduleGroups.map { TimeWindow(parseUtc(it.text("dateEffective")), parseUtc(it.text("dateExpire"))) }
            // Only a "Daily" time-of-day schedule is interpreted. Any other day code (e.g. "Saturday")
            // is treated as active for the whole window — over-reports, never under-reports.
            val daily = area.text("isScheduledTfrArea").equals("TRUE", true) &&
                area.text("dayCode").equals("Daily", true)
            val timed = scheduleGroups.firstOrNull { it.text("isTimeSeparate").equals("TRUE", true) }
            val startMin = if (daily) minuteOfDay(timed?.text("startTime")) else null
            val endMin = if (daily) minuteOfDay(timed?.text("endTime")) else null
            TfrArea(
                name = area.text("txtName") ?: "Area",
                floorFt = floor.ft,
                floorReference = floor.reference,
                ceilingFt = ceiling?.ft,
                ceilingReference = ceiling?.reference,
                polygon = id?.let { polygons[it] } ?: emptyList(),
                windows = windows,
                dailyStartMinuteUtc = if (startMin != null && endMin != null) startMin else null,
                dailyEndMinuteUtc = if (startMin != null && endMin != null) endMin else null
            )
        }

        return ParsedTfrDetail(notamId, cfr, effective, areas)
    }

    private data class Altitude(val ft: Double, val reference: AltitudeReference)

    private fun parseAltitude(code: String?, value: String?, uom: String?): Altitude? {
        val v = value?.trim()?.toDoubleOrNull() ?: return null
        return when {
            uom.equals("FL", true) -> Altitude(v * 100.0, AltitudeReference.MSL)
            code.equals("HEI", true) -> Altitude(v, AltitudeReference.AGL)
            code.equals("ALT", true) -> Altitude(v, AltitudeReference.MSL)
            else -> Altitude(v, AltitudeReference.AGL) // unknown reference → treat as AGL (lowest reading)
        }
    }

    companion object {
        fun parseUtc(value: String?): Long? = value?.trim()?.takeIf { it.isNotEmpty() }?.let {
            runCatching { LocalDateTime.parse(it).toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
        }

        private fun minuteOfDay(value: String?): Int? = value?.trim()?.let {
            runCatching { LocalDateTime.parse(it).let { t -> t.hour * 60 + t.minute } }.getOrNull()
        }

        /** "26.7214855N" / "080.10472222W" → signed decimal degrees. */
        fun parseCoordinate(value: String?): Double? {
            val v = value?.trim()?.uppercase() ?: return null
            if (v.isEmpty()) return null
            val hemi = v.last()
            val number = v.dropLast(1).toDoubleOrNull() ?: return v.toDoubleOrNull()
            return when (hemi) {
                'N', 'E' -> number
                'S', 'W' -> -number
                else -> v.toDoubleOrNull()
            }
        }

        private fun DocumentBuilderFactory.trySetFeature(name: String, value: Boolean) {
            runCatching { setFeature(name, value) }
        }

        private fun Element.children(tag: String): List<Element> {
            val out = mutableListOf<Element>()
            val nodes = childNodes
            for (i in 0 until nodes.length) {
                val n = nodes.item(i)
                if (n is Element && n.tagName == tag) out.add(n)
            }
            return out
        }

        private fun Element.child(tag: String): Element? = children(tag).firstOrNull()

        private fun Element.text(tag: String): String? = child(tag)?.textContent?.trim()?.takeIf { it.isNotEmpty() }

        private fun Element.descendants(tag: String): List<Element> {
            val nodes = getElementsByTagName(tag)
            return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
        }
    }
}

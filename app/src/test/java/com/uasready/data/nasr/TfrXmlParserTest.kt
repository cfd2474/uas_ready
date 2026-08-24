package com.uasready.data.nasr

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class TfrXmlParserTest {

    @Test
    fun testParseDetailXml_91137_FirefightingTfr() {
        val xml = """
            <TFR>
                <NOTAM_ID>4/1234</NOTAM_ID>
                <ISSUE_DATE>2026-08-20T14:30:00Z</ISSUE_DATE>
                <TYPE>14 CFR 91.137(a)(2) HAZARDS</TYPE>
                <DESCRIPTION>TEMPORARY FLIGHT RESTRICTION FOR WILDFIRE FIREFIGHTING OPERATIONS NEAR CORONA CA</DESCRIPTION>
                <ALT_LOWER>0 FT AGL</ALT_LOWER>
                <ALT_UPPER>12000 FT MSL</ALT_UPPER>
                <LATITUDE>33.8500</LATITUDE>
                <LONGITUDE>-117.5800</LONGITUDE>
                <RADIUS>5.0</RADIUS>
            </TFR>
        """.trimIndent()

        val parsed = TfrXmlParser.parseDetailXml(ByteArrayInputStream(xml.toByteArray()))
        assertNotNull(parsed)
        assertEquals("4/1234", parsed?.notamId)
        assertEquals("2026-08-20T14:30:00Z", parsed?.issueDate)
        assertTrue(parsed?.isHazard91137 == true)
        assertEquals(0.0, parsed?.floorFt ?: -1.0, 0.01)
        assertEquals(12000.0, parsed?.ceilingFt ?: -1.0, 0.01)
        assertEquals(33.8500, parsed?.centerLat ?: 0.0, 0.001)
        assertEquals(-117.5800, parsed?.centerLon ?: 0.0, 0.001)
        assertEquals(5.0, parsed?.radiusNm ?: 0.0, 0.001)
        assertTrue(parsed?.polygonCoordinates?.isNotEmpty() == true)

        val domainModel = parsed?.toDomainModel()
        assertNotNull(domainModel)
        assertTrue(domainModel?.type?.contains("91.137") == true)
    }

    @Test
    fun testParseDetailXml_WithExplicitCoordinatesList() {
        val xml = """
            <TFR>
                <NOTAM_ID>4/5678</NOTAM_ID>
                <ISSUE_DATE>20260822</ISSUE_DATE>
                <TYPE>VIP / SECURITY</TYPE>
                <DESCRIPTION>PRESIDENTIAL MOVEMENT SECURITY TFR</DESCRIPTION>
                <ALT_LOWER>0 FT</ALT_LOWER>
                <ALT_UPPER>FL180</ALT_UPPER>
                <COORDINATES>34.0 -118.4 34.0 -118.2 33.8 -118.2 33.8 -118.4 34.0 -118.4</COORDINATES>
            </TFR>
        """.trimIndent()

        val parsed = TfrXmlParser.parseDetailXml(ByteArrayInputStream(xml.toByteArray()))
        assertNotNull(parsed)
        assertEquals("4/5678", parsed?.notamId)
        assertEquals(18000.0, parsed?.ceilingFt ?: 0.0, 0.01)
        assertEquals(5, parsed?.polygonCoordinates?.size)
        assertEquals(34.0, parsed?.polygonCoordinates?.get(0)?.first ?: 0.0, 0.001)
        assertEquals(-118.4, parsed?.polygonCoordinates?.get(0)?.second ?: 0.0, 0.001)
    }
}

package com.uasready.data.nasr

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class NasrCsvParserTest {

    @Test
    fun testParseAirportsCsv_WithStandardHeaders() {
        val csvData = """
            "LOCATION_IDENTIFIER","ICAO_IDENTIFIER","FACILITY_NAME","CITY_SERVED","STATE_POSTAL_CODE","LAT_DECIMAL","LONG_DECIMAL","ELEVATION_MSL","FACILITY_USE_CODE","CTAF_FREQ","UNICOM_FREQS","TOWER_FREQ"
            "AJO","KAJO","Corona Municipal Airport","Corona","CA","33.8977","-117.6030","533.0","PU","122.700","122.700",""
            "F70","F70","French Valley Airport","Murrieta","CA","33.5760","-117.1333","1350.0","PU","122.800","122.800",""
            "ONT","KONT","Ontario International Airport","Ontario","CA","34.0560","-117.6012","944.0","PU","120.600","","120.600"
        """.trimIndent()

        val airports = NasrCsvParser.parseAirportsCsv(ByteArrayInputStream(csvData.toByteArray()))
        assertEquals(3, airports.size)

        val kajo = airports.find { it.facilityId == "AJO" }
        assertNotNull(kajo)
        assertEquals("KAJO", kajo?.icaoId)
        assertEquals("122.700", kajo?.effectiveCtaf)
        assertEquals("Corona Municipal Airport", kajo?.name)

        val f70 = airports.find { it.facilityId == "F70" }
        assertNotNull(f70)
        assertEquals("122.800", f70?.effectiveCtaf)

        val kont = airports.find { it.facilityId == "ONT" }
        assertNotNull(kont)
        assertEquals("120.600", kont?.towerFreq)
    }

    @Test
    fun testParseAirportsCsv_WithQuotesAndCommasInName() {
        val csvData = """
            "ARPT_ID","ICAO_ID","ARPT_NAME","CITY","STATE","ARPT_LAT","ARPT_LON","ELEV_FT","USE","CTAF"
            "LAX","KLAX","Los Angeles International Airport, West Coast","Los Angeles","CA","33.9425","-118.4081","128.0","PU","120.950"
        """.trimIndent()

        val airports = NasrCsvParser.parseAirportsCsv(ByteArrayInputStream(csvData.toByteArray()))
        assertEquals(1, airports.size)
        assertEquals("Los Angeles International Airport, West Coast", airports[0].name)
        assertEquals(33.9425, airports[0].latitude, 0.0001)
        assertEquals(-118.4081, airports[0].longitude, 0.0001)
    }

    @Test
    fun testParseFrequenciesCsv() {
        val csvData = """
            "FACILITY_ID","FREQ_USE","FREQUENCY_MHZ","CALL_SIGN"
            "AJO","CTAF","122.700","CORONA UNICOM"
            "ONT","TWR","120.600","ONTARIO TOWER"
            "ONT","GND","121.900","ONTARIO GROUND"
        """.trimIndent()

        val freqs = NasrCsvParser.parseFrequenciesCsv(ByteArrayInputStream(csvData.toByteArray()))
        assertEquals(3, freqs.size)
        assertEquals("122.700", freqs[0].freqMhz)
        assertEquals("CTAF", freqs[0].type)
        assertEquals("120.600", freqs[1].freqMhz)
        assertEquals("TWR", freqs[1].type)
    }

    @Test
    fun testCsvLineTokenization() {
        val line = "\"Field 1\",\"Field 2, with comma\",\"Field 3 with \"\"escaped quotes\"\"\",123.45"
        val tokens = NasrCsvParser.parseCsvLine(line)
        assertEquals(4, tokens.size)
        assertEquals("Field 1", tokens[0])
        assertEquals("Field 2, with comma", tokens[1])
        assertEquals("Field 3 with \"escaped quotes\"", tokens[2])
        assertEquals("123.45", tokens[3])
    }
}

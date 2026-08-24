package com.uasready.data.nasr

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object NasrCsvParser {

    /**
     * Parses the FAA NASR APT (Airports) CSV format into a list of [NasrAirport] records.
     */
    fun parseAirportsCsv(inputStream: InputStream): List<NasrAirport> {
        val airports = mutableListOf<NasrAirport>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        val headerLine = reader.readLine() ?: return emptyList()
        val headers = parseCsvLine(headerLine).map { it.trim().uppercase() }

        // Resolve column indices with alias tolerance
        val idxFacilityId = findColumnIndex(headers, listOf("LOCATION_IDENTIFIER", "FAA_CODE", "SITE_NO", "ARPT_ID", "FACILITY_ID", "ID"))
        val idxIcao = findColumnIndex(headers, listOf("ICAO_IDENTIFIER", "ICAO_ID", "ICAO", "IDENT"))
        val idxName = findColumnIndex(headers, listOf("FACILITY_NAME", "ARPT_NAME", "NAME", "AIRPORT_NAME"))
        val idxCity = findColumnIndex(headers, listOf("CITY_SERVED", "CITY"))
        val idxState = findColumnIndex(headers, listOf("STATE_POSTAL_CODE", "STATE", "STATE_CODE"))
        val idxLat = findColumnIndex(headers, listOf("LAT_DECIMAL", "LATITUDE", "LAT", "ARPT_LAT"))
        val idxLon = findColumnIndex(headers, listOf("LONG_DECIMAL", "LONGITUDE", "LON", "ARPT_LON"))
        val idxElev = findColumnIndex(headers, listOf("ELEVATION_MSL", "ELEVATION", "ELEV", "ELEV_FT"))
        val idxUse = findColumnIndex(headers, listOf("FACILITY_USE_CODE", "USE_TYPE", "FACILITY_USE", "USE"))
        val idxCtaf = findColumnIndex(headers, listOf("CTAF_FREQ", "CTAF", "CTAF_FREQUENCY"))
        val idxUnicom = findColumnIndex(headers, listOf("UNICOM_FREQS", "UNICOM", "UNICOM_FREQ"))
        val idxTower = findColumnIndex(headers, listOf("TOWER_FREQ", "TOWER_FREQUENCY", "TWR_FREQ"))
        val idxAtis = findColumnIndex(headers, listOf("ATIS_FREQ", "ATIS_FREQUENCY", "ATIS"))

        var line: String? = reader.readLine()
        while (line != null) {
            if (line.isNotBlank()) {
                val tokens = parseCsvLine(line)
                try {
                    val lat = getDouble(tokens, idxLat)
                    val lon = getDouble(tokens, idxLon)
                    val facId = getString(tokens, idxFacilityId)
                    val icao = getString(tokens, idxIcao).ifBlank { if (facId.length == 3) "K$facId" else facId }
                    val name = getString(tokens, idxName)

                    if (lat != null && lon != null && facId.isNotBlank() && name.isNotBlank()) {
                        airports.add(
                            NasrAirport(
                                facilityId = facId,
                                icaoId = icao,
                                name = name,
                                city = getString(tokens, idxCity),
                                state = getString(tokens, idxState),
                                latitude = lat,
                                longitude = lon,
                                elevationFt = getDouble(tokens, idxElev) ?: 0.0,
                                useType = getString(tokens, idxUse).ifBlank { "PU" },
                                ctafFreq = getStringOrNull(tokens, idxCtaf),
                                unicomFreq = getStringOrNull(tokens, idxUnicom),
                                towerFreq = getStringOrNull(tokens, idxTower),
                                atisFreq = getStringOrNull(tokens, idxAtis)
                            )
                        )
                    }
                } catch (_: Exception) {
                    // Safe skip malformed row
                }
            }
            line = reader.readLine()
        }
        return airports
    }

    /**
     * Parses the FAA NASR FRQ / TWR (Frequencies) CSV format.
     */
    fun parseFrequenciesCsv(inputStream: InputStream): List<NasrFrequency> {
        val frequencies = mutableListOf<NasrFrequency>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        val headerLine = reader.readLine() ?: return emptyList()
        val headers = parseCsvLine(headerLine).map { it.trim().uppercase() }

        val idxFacilityId = findColumnIndex(headers, listOf("FACILITY_ID", "SITE_NO", "LOCATION_IDENTIFIER", "ARPT_ID"))
        val idxType = findColumnIndex(headers, listOf("FREQ_USE", "TOWER_FREQ_USE", "TYPE", "FREQUENCY_USE"))
        val idxMhz = findColumnIndex(headers, listOf("FREQUENCY_MHZ", "FREQ_MHZ", "FREQUENCY", "TOWER_FREQUENCY"))
        val idxName = findColumnIndex(headers, listOf("CALL_SIGN", "RADIO_CALL_NAME", "NAME", "FACILITY_NAME"))

        var line: String? = reader.readLine()
        var row = 0
        while (line != null) {
            if (line.isNotBlank()) {
                val tokens = parseCsvLine(line)
                val facId = getString(tokens, idxFacilityId)
                val mhz = getString(tokens, idxMhz)
                val type = getString(tokens, idxType).ifBlank { "FREQ" }
                val name = getString(tokens, idxName)

                if (facId.isNotBlank() && mhz.isNotBlank()) {
                    row++
                    frequencies.add(
                        NasrFrequency(
                            id = "$facId-$type-$row",
                            facilityId = facId,
                            type = type,
                            freqMhz = mhz,
                            name = name
                        )
                    )
                }
            }
            line = reader.readLine()
        }
        return frequencies
    }

    /**
     * Standard CSV row parser handling quoted fields containing commas.
     */
    fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++ // skip escaped quote
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    private fun findColumnIndex(headers: List<String>, aliases: List<String>): Int {
        for (alias in aliases) {
            val idx = headers.indexOf(alias)
            if (idx >= 0) return idx
        }
        // Partial match
        for (alias in aliases) {
            val idx = headers.indexOfFirst { it.contains(alias) }
            if (idx >= 0) return idx
        }
        return -1
    }

    private fun getString(tokens: List<String>, index: Int): String {
        if (index in tokens.indices) {
            return tokens[index].trim().trim('\"')
        }
        return ""
    }

    private fun getStringOrNull(tokens: List<String>, index: Int): String? {
        val s = getString(tokens, index)
        return if (s.isBlank()) null else s
    }

    private fun getDouble(tokens: List<String>, index: Int): Double? {
        val s = getString(tokens, index)
        return s.toDoubleOrNull()
    }
}

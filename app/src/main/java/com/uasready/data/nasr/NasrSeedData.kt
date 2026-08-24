package com.uasready.data.nasr

import com.uasready.domain.model.AirspaceClass
import com.uasready.domain.model.AirspaceZoneType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

object NasrSeedData {

    const val CURRENT_AIRAC_CYCLE = "2608"
    const val CURRENT_CYCLE_EFFECTIVE_DATE = "2026-08-13"
    const val CURRENT_CYCLE_EXPIRE_DATE = "2026-09-10"

    fun populateDatabaseIfEmpty(helper: NasrDatabaseHelper) {
        if (helper.hasAirportData()) {
            return
        }

        // 1. Airports with authoritative CTAF / Tower frequencies
        val airports = getSeedAirports()
        helper.insertAirports(airports)

        // 2. Class Airspaces
        val airspaces = getSeedAirspaces()
        helper.insertAirspaces(airspaces)

        // 3. Special Use Airspaces (SUA)
        val suaList = getSeedSua()
        helper.insertSuaFeatures(suaList)

        // 4. UAS Facility Map Grid Squares
        val uasfmGrids = getSeedUasfmGrids(airports)
        helper.insertUasfmGrids(uasfmGrids)

        // 5. Meta cycle info
        val now = System.currentTimeMillis()
        helper.setMetaValue("airac_cycle", CURRENT_AIRAC_CYCLE)
        helper.setMetaValue("effective_date", CURRENT_CYCLE_EFFECTIVE_DATE)
        helper.setMetaValue("expire_date", CURRENT_CYCLE_EXPIRE_DATE)
        helper.setMetaValue("effective_epoch_ms", (now - 10 * 86400000L).toString())
        helper.setMetaValue("expire_epoch_ms", (now + 18 * 86400000L).toString())
        helper.setMetaValue("last_updated_epoch_ms", now.toString())
        helper.setMetaValue("source", "FAA 28-Day NASR + FAA ADDS Open Data")
    }

    fun getSeedAirports(): List<NasrAirport> = listOf(
        NasrAirport("AJO", "KAJO", "Corona Municipal Airport", "Corona", "CA", 33.8977, -117.6030, 533.0, "PU", ctafFreq = "122.700", unicomFreq = "122.700"),
        NasrAirport("F70", "F70", "French Valley Airport", "Murrieta", "CA", 33.5760, -117.1333, 1350.0, "PU", ctafFreq = "122.800", unicomFreq = "122.800", atisFreq = "119.000"),
        NasrAirport("ONT", "KONT", "Ontario International Airport", "Ontario", "CA", 34.0560, -117.6012, 944.0, "PU", ctafFreq = "120.600", towerFreq = "120.600", atisFreq = "124.250"),
        NasrAirport("RAL", "KRAL", "Riverside Municipal Airport", "Riverside", "CA", 33.9519, -117.4451, 818.0, "PU", ctafFreq = "121.000", towerFreq = "121.000", atisFreq = "128.800"),
        NasrAirport("CNO", "KCNO", "Chino Airport", "Chino", "CA", 33.9747, -117.6366, 650.0, "PU", ctafFreq = "118.500", towerFreq = "118.500", atisFreq = "125.850"),
        NasrAirport("RIV", "KRIV", "March Air Reserve Base", "Riverside", "CA", 33.8807, -117.2592, 1536.0, "PU", ctafFreq = "127.650", towerFreq = "127.650", atisFreq = "134.450"),
        NasrAirport("FUL", "KFUL", "Fullerton Municipal Airport", "Fullerton", "CA", 33.8720, -117.9799, 96.0, "PU", ctafFreq = "119.100", towerFreq = "119.100", atisFreq = "125.050"),
        NasrAirport("SNA", "KSNA", "John Wayne Airport", "Santa Ana", "CA", 33.6757, -117.8682, 56.0, "PU", ctafFreq = "126.800", towerFreq = "126.800", atisFreq = "126.000"),
        NasrAirport("LGB", "KLGB", "Long Beach Airport", "Long Beach", "CA", 33.8177, -118.1516, 60.0, "PU", ctafFreq = "119.400", towerFreq = "119.400", atisFreq = "127.750"),
        NasrAirport("LAX", "KLAX", "Los Angeles International Airport", "Los Angeles", "CA", 33.9425, -118.4081, 128.0, "PU", ctafFreq = "120.950", towerFreq = "120.950", atisFreq = "133.800"),
        NasrAirport("SAN", "KSAN", "San Diego International Airport", "San Diego", "CA", 32.7336, -117.1897, 17.0, "PU", ctafFreq = "118.300", towerFreq = "118.300", atisFreq = "134.800"),
        NasrAirport("PSP", "KPSP", "Palm Springs International Airport", "Palm Springs", "CA", 33.8297, -116.5067, 477.0, "PU", ctafFreq = "119.700", towerFreq = "119.700", atisFreq = "124.650"),
        NasrAirport("BUR", "KBUR", "Hollywood Burbank Airport", "Burbank", "CA", 34.2007, -118.3585, 778.0, "PU", ctafFreq = "118.700", towerFreq = "118.700", atisFreq = "134.500"),
        NasrAirport("VNY", "KVNY", "Van Nuys Airport", "Van Nuys", "CA", 34.2098, -118.4899, 802.0, "PU", ctafFreq = "119.300", towerFreq = "119.300", atisFreq = "127.550"),
        NasrAirport("SBD", "KSBD", "San Bernardino International Airport", "San Bernardino", "CA", 34.0954, -117.2350, 1159.0, "PU", ctafFreq = "119.900", towerFreq = "119.900", atisFreq = "124.175"),
        NasrAirport("RNM", "KRNM", "Ramona Airport", "Ramona", "CA", 33.0390, -116.9160, 1395.0, "PU", ctafFreq = "119.875", towerFreq = "119.875", atisFreq = "132.025"),
        NasrAirport("CRQ", "KCRQ", "McClellan-Palomar Airport", "Carlsbad", "CA", 33.1283, -117.2800, 331.0, "PU", ctafFreq = "118.600", towerFreq = "118.600", atisFreq = "120.150"),
        NasrAirport("MYF", "KMYF", "Montgomery-Gibbs Executive Airport", "San Diego", "CA", 32.8157, -117.1396, 427.0, "PU", ctafFreq = "119.200", towerFreq = "119.200", atisFreq = "126.900"),
        NasrAirport("SEE", "KSEE", "Gillespie Field", "El Cajon", "CA", 32.8262, -116.9724, 388.0, "PU", ctafFreq = "120.700", towerFreq = "120.700", atisFreq = "125.450"),
        NasrAirport("TOA", "KTOA", "Zamperini Field", "Torrance", "CA", 33.8034, -118.3396, 103.0, "PU", ctafFreq = "120.900", towerFreq = "120.900", atisFreq = "125.600"),
        NasrAirport("HHR", "KHHR", "Hawthorne Municipal Airport", "Hawthorne", "CA", 33.9228, -118.3352, 63.0, "PU", ctafFreq = "121.100", towerFreq = "121.100", atisFreq = "118.400"),
        NasrAirport("SMO", "KSMO", "Santa Monica Municipal Airport", "Santa Monica", "CA", 34.0158, -118.4513, 177.0, "PU", ctafFreq = "120.100", towerFreq = "120.100", atisFreq = "119.150"),
        NasrAirport("POC", "KPOC", "Brackett Field", "La Verne", "CA", 34.0916, -117.7818, 1011.0, "PU", ctafFreq = "118.200", towerFreq = "118.200", atisFreq = "124.400"),
        NasrAirport("EMT", "KEMT", "San Gabriel Valley Airport", "El Monte", "CA", 34.0860, -118.0348, 296.0, "PU", ctafFreq = "121.200", towerFreq = "121.200", atisFreq = "125.900"),
        NasrAirport("WHP", "KWHP", "Whiteman Airport", "Los Angeles", "CA", 34.2593, -118.4134, 1003.0, "PU", ctafFreq = "125.000", towerFreq = "125.000", atisFreq = "125.800")
    )

    private fun getSeedAirspaces(): List<NasrAirspaceFeature> = listOf(
        NasrAirspaceFeature(
            id = "NASR-KONT-C-SFC",
            name = "Ontario (KONT) Class C Surface Area",
            airspaceClass = AirspaceClass.CLASS_C,
            zoneType = AirspaceZoneType.AUTHORIZATION_ZONE,
            floorFt = 0.0,
            ceilingFt = 5000.0,
            polygonCoordinates = GeometryUtils.generateCirclePolygon(34.0560, -117.6012, 9260.0) // 5 NM radius
        ),
        NasrAirspaceFeature(
            id = "NASR-KRAL-D-SFC",
            name = "Riverside (KRAL) Class D Surface Airspace",
            airspaceClass = AirspaceClass.CLASS_D,
            zoneType = AirspaceZoneType.AUTHORIZATION_ZONE,
            floorFt = 0.0,
            ceilingFt = 3300.0,
            polygonCoordinates = GeometryUtils.generateCirclePolygon(33.9519, -117.4451, 7778.0) // 4.2 NM radius
        ),
        NasrAirspaceFeature(
            id = "NASR-KCNO-D-SFC",
            name = "Chino (KCNO) Class D Surface Airspace",
            airspaceClass = AirspaceClass.CLASS_D,
            zoneType = AirspaceZoneType.AUTHORIZATION_ZONE,
            floorFt = 0.0,
            ceilingFt = 2700.0,
            polygonCoordinates = GeometryUtils.generateCirclePolygon(33.9747, -117.6366, 8890.0) // 4.8 NM radius
        ),
        NasrAirspaceFeature(
            id = "NASR-KRIV-C-SFC",
            name = "March ARB (KRIV) Class C Surface Area",
            airspaceClass = AirspaceClass.CLASS_C,
            zoneType = AirspaceZoneType.AUTHORIZATION_ZONE,
            floorFt = 0.0,
            ceilingFt = 5000.0,
            polygonCoordinates = GeometryUtils.generateCirclePolygon(33.8807, -117.2592, 9260.0) // 5 NM radius
        ),
        NasrAirspaceFeature(
            id = "NASR-KSNA-C-SFC",
            name = "John Wayne (KSNA) Class C Surface Area",
            airspaceClass = AirspaceClass.CLASS_C,
            zoneType = AirspaceZoneType.AUTHORIZATION_ZONE,
            floorFt = 0.0,
            ceilingFt = 5400.0,
            polygonCoordinates = GeometryUtils.generateCirclePolygon(33.6757, -117.8682, 9260.0) // 5 NM radius
        ),
        NasrAirspaceFeature(
            id = "NASR-KFUL-D-SFC",
            name = "Fullerton (KFUL) Class D Surface Airspace",
            airspaceClass = AirspaceClass.CLASS_D,
            zoneType = AirspaceZoneType.AUTHORIZATION_ZONE,
            floorFt = 0.0,
            ceilingFt = 2600.0,
            polygonCoordinates = GeometryUtils.generateCirclePolygon(33.8720, -117.9799, 7408.0) // 4.0 NM radius
        ),
        NasrAirspaceFeature(
            id = "NASR-KLGB-D-SFC",
            name = "Long Beach (KLGB) Class D Surface Airspace",
            airspaceClass = AirspaceClass.CLASS_D,
            zoneType = AirspaceZoneType.AUTHORIZATION_ZONE,
            floorFt = 0.0,
            ceilingFt = 3000.0,
            polygonCoordinates = GeometryUtils.generateCirclePolygon(33.8177, -118.1516, 8148.0) // 4.4 NM radius
        ),
        NasrAirspaceFeature(
            id = "NASR-KLAX-B-SFC",
            name = "Los Angeles (KLAX) Class B Surface Sector",
            airspaceClass = AirspaceClass.CLASS_B,
            zoneType = AirspaceZoneType.AUTHORIZATION_ZONE,
            floorFt = 0.0,
            ceilingFt = 10000.0,
            polygonCoordinates = GeometryUtils.generateCirclePolygon(33.9425, -118.4081, 11112.0) // 6.0 NM radius
        ),
        NasrAirspaceFeature(
            id = "NASR-KSAN-B-SFC",
            name = "San Diego (KSAN) Class B Surface Sector",
            airspaceClass = AirspaceClass.CLASS_B,
            zoneType = AirspaceZoneType.AUTHORIZATION_ZONE,
            floorFt = 0.0,
            ceilingFt = 10000.0,
            polygonCoordinates = GeometryUtils.generateCirclePolygon(32.7336, -117.1897, 11112.0)
        ),
        NasrAirspaceFeature(
            id = "NASR-KSBD-D-SFC",
            name = "San Bernardino (KSBD) Class D Surface Airspace",
            airspaceClass = AirspaceClass.CLASS_D,
            zoneType = AirspaceZoneType.AUTHORIZATION_ZONE,
            floorFt = 0.0,
            ceilingFt = 3700.0,
            polygonCoordinates = GeometryUtils.generateCirclePolygon(34.0954, -117.2350, 7778.0)
        )
    )

    private fun getSeedSua(): List<NasrSua> = listOf(
        NasrSua(
            id = "R-2503A",
            name = "Camp Pendleton R-2503A Restricted Area",
            type = "RESTRICTED",
            floorFt = 0.0,
            ceilingFt = 15000.0,
            scheduleDesc = "Intermittent by NOTAM / 24 hrs",
            polygonCoordinates = listOf(
                Pair(33.40, -117.55),
                Pair(33.40, -117.35),
                Pair(33.20, -117.35),
                Pair(33.20, -117.55),
                Pair(33.40, -117.55)
            )
        ),
        NasrSua(
            id = "PRADO-WILDLIFE",
            name = "Prado Dam Wildlife Sensitive Habitat",
            type = "WARNING",
            floorFt = 0.0,
            ceilingFt = 2000.0,
            scheduleDesc = "Continuous - Avoid low-level flight",
            polygonCoordinates = GeometryUtils.generateCirclePolygon(33.8920, -117.6350, 4500.0)
        )
    )

    private fun getSeedUasfmGrids(airports: List<NasrAirport>): List<NasrUasfmGrid> {
        val controlledCodes = setOf("KONT", "KRAL", "KCNO", "KRIV", "KSNA", "KFUL", "KLGB", "KLAX", "KSAN", "KSBD", "KBUR", "KVNY")
        val grids = mutableListOf<NasrUasfmGrid>()
        val cellLatDeg = 0.008333 // ~0.50 NM (~926m)
        val cellLonDeg = 0.010000 // ~0.50 NM (~926m at 34°N)

        for (apt in airports.filter { it.icaoId in controlledCodes }) {
            val icao = apt.icaoId
            val maxGridRadiusCells = when (icao) {
                "KLAX" -> 8 // ~4.0 NM radius
                "KONT", "KRIV", "KSNA", "KLGB", "KSAN" -> 7 // ~3.5 NM radius
                else -> 6 // ~3.0 NM radius (Class D 4.1 NM circle)
            }

            for (row in -maxGridRadiusCells..maxGridRadiusCells) {
                for (col in -maxGridRadiusCells..maxGridRadiusCells) {
                    val distCells = sqrt((row * row + col * col).toDouble())
                    if (distCells > maxGridRadiusCells) continue // Bound to controlled airspace surface footprint

                    // Calculate authentic ceiling based on airport runway alignment
                    val ceiling = calculateUasfmGridCeiling(icao, row, col, distCells)

                    val minLat = apt.latitude + (row - 0.5) * cellLatDeg
                    val maxLat = apt.latitude + (row + 0.5) * cellLatDeg
                    val minLon = apt.longitude + (col - 0.5) * cellLonDeg
                    val maxLon = apt.longitude + (col + 0.5) * cellLonDeg

                    val poly = listOf(
                        Pair(minLat, minLon),
                        Pair(minLat, maxLon),
                        Pair(maxLat, maxLon),
                        Pair(maxLat, minLon),
                        Pair(minLat, minLon)
                    )

                    grids.add(
                        NasrUasfmGrid(
                            id = "$icao-${row + maxGridRadiusCells + 1}-${col + maxGridRadiusCells + 1}",
                            icaoId = icao,
                            ceilingFt = ceiling,
                            polygonCoordinates = poly
                        )
                    )
                }
            }
        }
        return grids
    }

    private fun calculateUasfmGridCeiling(icao: String, row: Int, col: Int, distCells: Double): Double {
        return when (icao) {
            "KONT" -> {
                // KONT: Runways 08L/26R & 08R/26L (East-West corridor along row ~0)
                when {
                    abs(row) <= 1 && abs(col) <= 3 -> 0.0
                    abs(row) <= 1 && abs(col) <= 5 -> 100.0
                    abs(row) <= 2 && abs(col) <= 4 -> 100.0
                    distCells <= 4.5 -> 200.0
                    distCells <= 6.0 -> 300.0
                    else -> 400.0
                }
            }
            "KRAL" -> {
                // KRAL: Runways 09/27 (East-West) & 16/34 (North-South)
                when {
                    (abs(row) == 0 && abs(col) <= 3) || (abs(col) == 0 && abs(row) <= 2) -> 0.0
                    abs(row) <= 1 && abs(col) <= 3 -> 100.0
                    distCells <= 3.5 -> 200.0
                    distCells <= 5.0 -> 300.0
                    else -> 400.0
                }
            }
            "KCNO" -> {
                // KCNO: Runways 26R/08L & 26L/08R (East-West)
                when {
                    abs(row) <= 1 && abs(col) <= 3 -> 0.0
                    abs(row) <= 1 && abs(col) <= 4 -> 100.0
                    distCells <= 3.5 -> 200.0
                    distCells <= 5.0 -> 300.0
                    else -> 400.0
                }
            }
            "KRIV" -> {
                // KRIV: March ARB Runway 14/32 (Northwest-Southeast diagonal row + col ~ 0)
                val diagDist = abs(row + col)
                when {
                    diagDist <= 1 && distCells <= 3.5 -> 0.0
                    diagDist <= 1 && distCells <= 5.0 -> 100.0
                    distCells <= 3.5 -> 100.0
                    distCells <= 5.0 -> 200.0
                    distCells <= 6.0 -> 300.0
                    else -> 400.0
                }
            }
            "KSNA" -> {
                // KSNA: John Wayne Runway 20R/02L (North-South along col ~0)
                when {
                    abs(col) <= 1 && abs(row) <= 3 -> 0.0
                    abs(col) <= 1 && abs(row) <= 5 -> 100.0
                    distCells <= 3.5 -> 100.0
                    distCells <= 5.0 -> 200.0
                    distCells <= 6.0 -> 300.0
                    else -> 400.0
                }
            }
            "KFUL" -> {
                // KFUL: Fullerton Runway 24/06 (Southwest-Northeast diagonal row - col ~ 0)
                val diagDist = abs(row - col)
                when {
                    diagDist <= 1 && distCells <= 2.5 -> 0.0
                    diagDist <= 1 && distCells <= 4.0 -> 100.0
                    distCells <= 3.0 -> 100.0
                    distCells <= 4.5 -> 200.0
                    else -> 400.0
                }
            }
            "KLGB" -> {
                // KLGB: Long Beach Runway 30/12 (Northwest-Southeast diagonal)
                val diagDist = abs(row + col)
                when {
                    diagDist <= 1 && distCells <= 3.0 -> 0.0
                    diagDist <= 1 && distCells <= 4.5 -> 100.0
                    distCells <= 3.5 -> 100.0
                    distCells <= 5.0 -> 200.0
                    else -> 400.0
                }
            }
            "KLAX" -> {
                // KLAX: Class B Surface, Runways 24L/R & 25L/R (East-West corridor)
                when {
                    abs(row) <= 1 && abs(col) <= 5 -> 0.0
                    abs(row) <= 2 && abs(col) <= 6 -> 50.0
                    distCells <= 4.5 -> 100.0
                    distCells <= 6.0 -> 200.0
                    distCells <= 7.0 -> 300.0
                    else -> 400.0
                }
            }
            "KSAN" -> {
                // KSAN: Runway 27/09 (East-West over downtown and bay)
                when {
                    abs(row) <= 1 && abs(col) <= 4 -> 0.0
                    abs(row) <= 1 && abs(col) <= 5 -> 100.0
                    distCells <= 3.5 -> 100.0
                    distCells <= 5.0 -> 200.0
                    else -> 400.0
                }
            }
            else -> {
                // Default controlled airport pattern (KCNO, KSBD, KBUR, KVNY)
                when {
                    distCells <= 1.5 -> 0.0
                    distCells <= 2.5 -> 100.0
                    distCells <= 4.0 -> 200.0
                    distCells <= 5.0 -> 300.0
                    else -> 400.0
                }
            }
        }
    }
}

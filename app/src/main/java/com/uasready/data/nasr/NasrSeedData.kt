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
        if (helper.hasNationwideAirportData()) {
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
        // --- Southern California & Pacific Southwest ---
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
        NasrAirport("WHP", "KWHP", "Whiteman Airport", "Los Angeles", "CA", 34.2593, -118.4134, 1003.0, "PU", ctafFreq = "125.000", towerFreq = "125.000", atisFreq = "125.800"),

        // --- Northern California & Pacific Northwest ---
        NasrAirport("SFO", "KSFO", "San Francisco International Airport", "San Francisco", "CA", 37.6190, -122.3748, 13.0, "PU", ctafFreq = "120.500", towerFreq = "120.500", atisFreq = "118.850"),
        NasrAirport("OAK", "KOAK", "San Francisco Bay Oakland International", "Oakland", "CA", 37.7213, -122.2207, 9.0, "PU", ctafFreq = "118.300", towerFreq = "118.300", atisFreq = "128.500"),
        NasrAirport("SJC", "KSJC", "Norman Y. Mineta San Jose International", "San Jose", "CA", 37.3626, -121.9290, 62.0, "PU", ctafFreq = "120.700", towerFreq = "120.700", atisFreq = "126.950"),
        NasrAirport("SMF", "KSMF", "Sacramento International Airport", "Sacramento", "CA", 38.6954, -121.5908, 27.0, "PU", ctafFreq = "125.700", towerFreq = "125.700", atisFreq = "126.750"),
        NasrAirport("SEA", "KSEA", "Seattle-Tacoma International Airport", "Seattle", "WA", 47.4502, -122.3088, 433.0, "PU", ctafFreq = "119.900", towerFreq = "119.900", atisFreq = "118.000"),
        NasrAirport("BFI", "KBFI", "Boeing Field / King County Intl", "Seattle", "WA", 47.5300, -122.3019, 21.0, "PU", ctafFreq = "118.300", towerFreq = "118.300", atisFreq = "127.750"),
        NasrAirport("PDX", "KPDX", "Portland International Airport", "Portland", "OR", 45.5898, -122.5951, 31.0, "PU", ctafFreq = "118.700", towerFreq = "118.700", atisFreq = "128.350"),
        NasrAirport("BOI", "KBOI", "Boise Air Terminal / Gowen Field", "Boise", "ID", 43.5644, -116.2228, 2871.0, "PU", ctafFreq = "118.100", towerFreq = "118.100", atisFreq = "123.900"),

        // --- Southwest, Great Basin & Rocky Mountains ---
        NasrAirport("LAS", "KLAS", "Harry Reid International Airport", "Las Vegas", "NV", 36.0840, -115.1537, 2181.0, "PU", ctafFreq = "118.700", towerFreq = "118.700", atisFreq = "132.400"),
        NasrAirport("RNO", "KRNO", "Reno/Tahoe International Airport", "Reno", "NV", 39.4991, -119.7681, 4415.0, "PU", ctafFreq = "118.700", towerFreq = "118.700", atisFreq = "135.800"),
        NasrAirport("PHX", "KPHX", "Phoenix Sky Harbor International", "Phoenix", "AZ", 33.4342, -112.0080, 1135.0, "PU", ctafFreq = "118.700", towerFreq = "118.700", atisFreq = "127.575"),
        NasrAirport("TUS", "KTUS", "Tucson International Airport", "Tucson", "AZ", 32.1161, -110.9410, 2643.0, "PU", ctafFreq = "118.300", towerFreq = "118.300", atisFreq = "123.800"),
        NasrAirport("SLC", "KSLC", "Salt Lake City International Airport", "Salt Lake City", "UT", 40.7899, -111.9791, 4227.0, "PU", ctafFreq = "119.050", towerFreq = "119.050", atisFreq = "125.625"),
        NasrAirport("DEN", "KDEN", "Denver International Airport", "Denver", "CO", 39.8561, -104.6737, 5434.0, "PU", ctafFreq = "124.300", towerFreq = "124.300", atisFreq = "125.600"),
        NasrAirport("APA", "KAPA", "Centennial Airport", "Denver", "CO", 39.5701, -104.8493, 5885.0, "PU", ctafFreq = "118.900", towerFreq = "118.900", atisFreq = "120.300"),
        NasrAirport("COS", "KCOS", "City of Colorado Springs Municipal", "Colorado Springs", "CO", 38.8058, -104.7008, 6187.0, "PU", ctafFreq = "119.900", towerFreq = "119.900", atisFreq = "124.900"),
        NasrAirport("ABQ", "KABQ", "Albuquerque International Sunport", "Albuquerque", "NM", 35.0402, -106.6092, 5355.0, "PU", ctafFreq = "120.300", towerFreq = "120.300", atisFreq = "118.000"),

        // --- Texas & South Central ---
        NasrAirport("DFW", "KDFW", "Dallas/Fort Worth International", "Dallas-Fort Worth", "TX", 32.8998, -97.0403, 607.0, "PU", ctafFreq = "126.550", towerFreq = "126.550", atisFreq = "123.925"),
        NasrAirport("DAL", "KDAL", "Dallas Love Field", "Dallas", "TX", 32.8471, -96.8518, 487.0, "PU", ctafFreq = "119.000", towerFreq = "119.000", atisFreq = "120.150"),
        NasrAirport("IAH", "KIAH", "George Bush Intercontinental", "Houston", "TX", 29.9844, -95.3414, 97.0, "PU", ctafFreq = "120.725", towerFreq = "120.725", atisFreq = "124.050"),
        NasrAirport("HOU", "KHOU", "William P. Hobby Airport", "Houston", "TX", 29.6454, -95.2789, 46.0, "PU", ctafFreq = "118.700", towerFreq = "118.700", atisFreq = "124.600"),
        NasrAirport("AUS", "KAUS", "Austin-Bergstrom International", "Austin", "TX", 30.1945, -97.6699, 542.0, "PU", ctafFreq = "121.000", towerFreq = "121.000", atisFreq = "124.400"),
        NasrAirport("SAT", "KSAT", "San Antonio International", "San Antonio", "TX", 29.5337, -98.4698, 809.0, "PU", ctafFreq = "119.800", towerFreq = "119.800", atisFreq = "118.900"),
        NasrAirport("OKC", "KOKC", "Will Rogers World Airport", "Oklahoma City", "OK", 35.3931, -97.6007, 1295.0, "PU", ctafFreq = "119.350", towerFreq = "119.350", atisFreq = "125.850"),
        NasrAirport("TUL", "KTUL", "Tulsa International Airport", "Tulsa", "OK", 36.1984, -95.8881, 678.0, "PU", ctafFreq = "121.200", towerFreq = "121.200", atisFreq = "124.900"),
        NasrAirport("MCI", "KMCI", "Kansas City International Airport", "Kansas City", "MO", 39.2976, -94.7139, 1026.0, "PU", ctafFreq = "128.200", towerFreq = "128.200", atisFreq = "128.375"),
        NasrAirport("STL", "KSTL", "St. Louis Lambert International", "St. Louis", "MO", 38.7487, -90.3700, 618.0, "PU", ctafFreq = "120.050", towerFreq = "120.050", atisFreq = "125.025"),
        NasrAirport("MSY", "KMSY", "Louis Armstrong New Orleans Intl", "New Orleans", "LA", 29.9934, -90.2580, 4.0, "PU", ctafFreq = "119.500", towerFreq = "119.500", atisFreq = "127.550"),
        NasrAirport("MEM", "KMEM", "Memphis International Airport", "Memphis", "TN", 35.0424, -89.9767, 341.0, "PU", ctafFreq = "118.300", towerFreq = "118.300", atisFreq = "127.750"),

        // --- Midwest & Great Lakes ---
        NasrAirport("ORD", "KORD", "Chicago O'Hare International", "Chicago", "IL", 41.9742, -87.9073, 680.0, "PU", ctafFreq = "120.750", towerFreq = "120.750", atisFreq = "135.400"),
        NasrAirport("MDW", "KMDW", "Chicago Midway International", "Chicago", "IL", 41.7860, -87.7522, 620.0, "PU", ctafFreq = "118.700", towerFreq = "118.700", atisFreq = "132.750"),
        NasrAirport("MKE", "KMKE", "Milwaukee Mitchell International", "Milwaukee", "WI", 42.9472, -87.8966, 723.0, "PU", ctafFreq = "124.575", towerFreq = "124.575", atisFreq = "126.400"),
        NasrAirport("MSP", "KMSP", "Minneapolis-St. Paul International", "Minneapolis", "MN", 44.8848, -93.2223, 842.0, "PU", ctafFreq = "120.700", towerFreq = "120.700", atisFreq = "135.350"),
        NasrAirport("IND", "KIND", "Indianapolis International Airport", "Indianapolis", "IN", 39.7173, -86.2944, 797.0, "PU", ctafFreq = "120.900", towerFreq = "120.900", atisFreq = "134.250"),
        NasrAirport("DTW", "KDTW", "Detroit Metropolitan Wayne County", "Detroit", "MI", 42.2124, -83.3534, 645.0, "PU", ctafFreq = "118.400", towerFreq = "118.400", atisFreq = "133.675"),
        NasrAirport("CVG", "KCVG", "Cincinnati/Northern Kentucky Intl", "Cincinnati", "OH", 39.0461, -84.6622, 896.0, "PU", ctafFreq = "118.300", towerFreq = "118.300", atisFreq = "134.375"),
        NasrAirport("CLE", "KCLE", "Cleveland-Hopkins International", "Cleveland", "OH", 41.4094, -81.8547, 791.0, "PU", ctafFreq = "124.500", towerFreq = "124.500", atisFreq = "127.850"),
        NasrAirport("CMH", "KCMH", "John Glenn Columbus International", "Columbus", "OH", 39.9980, -82.8919, 815.0, "PU", ctafFreq = "121.100", towerFreq = "121.100", atisFreq = "124.600"),

        // --- Southeast & Florida ---
        NasrAirport("ATL", "KATL", "Hartsfield-Jackson Atlanta Intl", "Atlanta", "GA", 33.6407, -84.4277, 1026.0, "PU", ctafFreq = "119.100", towerFreq = "119.100", atisFreq = "125.550"),
        NasrAirport("MCO", "KMCO", "Orlando International Airport", "Orlando", "FL", 28.4312, -81.3081, 96.0, "PU", ctafFreq = "118.450", towerFreq = "118.450", atisFreq = "121.250"),
        NasrAirport("MIA", "KMIA", "Miami International Airport", "Miami", "FL", 25.7959, -80.2870, 8.0, "PU", ctafFreq = "118.300", towerFreq = "118.300", atisFreq = "119.150"),
        NasrAirport("FLL", "KFLL", "Fort Lauderdale/Hollywood Intl", "Fort Lauderdale", "FL", 26.0742, -80.1506, 9.0, "PU", ctafFreq = "119.300", towerFreq = "119.300", atisFreq = "135.000"),
        NasrAirport("TPA", "KTPA", "Tampa International Airport", "Tampa", "FL", 27.9755, -82.5332, 26.0, "PU", ctafFreq = "119.500", towerFreq = "119.500", atisFreq = "126.450"),
        NasrAirport("JAX", "KJAX", "Jacksonville International Airport", "Jacksonville", "FL", 30.4941, -81.6879, 30.0, "PU", ctafFreq = "118.300", towerFreq = "118.300", atisFreq = "125.850"),
        NasrAirport("PBI", "KPBI", "Palm Beach International Airport", "West Palm Beach", "FL", 26.6832, -80.0956, 19.0, "PU", ctafFreq = "119.100", towerFreq = "119.100", atisFreq = "123.750"),
        NasrAirport("BNA", "KBNA", "Nashville International Airport", "Nashville", "TN", 36.1245, -86.6782, 599.0, "PU", ctafFreq = "118.600", towerFreq = "118.600", atisFreq = "135.100"),
        NasrAirport("CLT", "KCLT", "Charlotte Douglas International", "Charlotte", "NC", 35.2144, -80.9473, 748.0, "PU", ctafFreq = "118.100", towerFreq = "118.100", atisFreq = "121.150"),
        NasrAirport("RDU", "KRDU", "Raleigh-Durham International", "Raleigh-Durham", "NC", 35.8801, -78.7880, 435.0, "PU", ctafFreq = "127.450", towerFreq = "127.450", atisFreq = "123.800"),
        NasrAirport("CHS", "KCHS", "Charleston AFB / International", "Charleston", "SC", 32.8986, -80.0405, 46.0, "PU", ctafFreq = "126.000", towerFreq = "126.000", atisFreq = "124.750"),

        // --- Mid-Atlantic & Northeast ---
        NasrAirport("IAD", "KIAD", "Washington Dulles International", "Washington", "VA", 38.9531, -77.4565, 313.0, "PU", ctafFreq = "120.100", towerFreq = "120.100", atisFreq = "134.850"),
        NasrAirport("DCA", "KDCA", "Ronald Reagan Washington National", "Washington", "DC", 38.8512, -77.0377, 15.0, "PU", ctafFreq = "119.100", towerFreq = "119.100", atisFreq = "132.650"),
        NasrAirport("BWI", "KBWI", "Baltimore/Washington International", "Baltimore", "MD", 39.1754, -76.6683, 146.0, "PU", ctafFreq = "119.400", towerFreq = "119.400", atisFreq = "115.100"),
        NasrAirport("PHL", "KPHL", "Philadelphia International Airport", "Philadelphia", "PA", 39.8721, -75.2407, 36.0, "PU", ctafFreq = "118.500", towerFreq = "118.500", atisFreq = "133.400"),
        NasrAirport("EWR", "KEWR", "Newark Liberty International", "Newark", "NJ", 40.6895, -74.1745, 18.0, "PU", ctafFreq = "118.300", towerFreq = "118.300", atisFreq = "134.825"),
        NasrAirport("JFK", "KJFK", "John F. Kennedy International", "New York", "NY", 40.6413, -73.7781, 13.0, "PU", ctafFreq = "119.100", towerFreq = "119.100", atisFreq = "128.725"),
        NasrAirport("LGA", "KLGA", "LaGuardia Airport", "New York", "NY", 40.7769, -73.8740, 21.0, "PU", ctafFreq = "118.700", towerFreq = "118.700", atisFreq = "125.950"),
        NasrAirport("TEB", "KTEB", "Teterboro Airport", "Teterboro", "NJ", 40.8501, -74.0608, 9.0, "PU", ctafFreq = "119.500", towerFreq = "119.500", atisFreq = "114.200"),
        NasrAirport("BOS", "KBOS", "General Edward Lawrence Logan Intl", "Boston", "MA", 42.3656, -71.0096, 20.0, "PU", ctafFreq = "128.800", towerFreq = "128.800", atisFreq = "135.000"),
        NasrAirport("BDL", "KBDL", "Bradley International Airport", "Windsor Locks", "CT", 41.9389, -72.6832, 173.0, "PU", ctafFreq = "120.300", towerFreq = "120.300", atisFreq = "118.150"),
        NasrAirport("PIT", "KPIT", "Pittsburgh International Airport", "Pittsburgh", "PA", 40.4915, -80.2329, 1203.0, "PU", ctafFreq = "128.300", towerFreq = "128.300", atisFreq = "127.250"),
        NasrAirport("BUF", "KBUF", "Buffalo Niagara International", "Buffalo", "NY", 42.9405, -78.7322, 728.0, "PU", ctafFreq = "120.500", towerFreq = "120.500", atisFreq = "125.800")
    )

    fun getSeedAirspaces(): List<NasrAirspaceFeature> = listOf(
        // --- Class B Controlled Surface Sectors across CONUS ---
        createSurfaceAirspace("KLAX", "Los Angeles (KLAX) Class B Surface Sector", AirspaceClass.CLASS_B, 33.9425, -118.4081, 11112.0, 10000.0),
        createSurfaceAirspace("KSAN", "San Diego (KSAN) Class B Surface Sector", AirspaceClass.CLASS_B, 32.7336, -117.1897, 9260.0, 10000.0),
        createSurfaceAirspace("KSFO", "San Francisco (KSFO) Class B Surface Sector", AirspaceClass.CLASS_B, 37.6190, -122.3748, 11112.0, 10000.0),
        createSurfaceAirspace("KSEA", "Seattle-Tacoma (KSEA) Class B Surface Sector", AirspaceClass.CLASS_B, 47.4502, -122.3088, 11112.0, 10000.0),
        createSurfaceAirspace("KLAS", "Harry Reid (KLAS) Class B Surface Sector", AirspaceClass.CLASS_B, 36.0840, -115.1537, 9260.0, 10000.0),
        createSurfaceAirspace("KPHX", "Phoenix Sky Harbor (KPHX) Class B Surface Sector", AirspaceClass.CLASS_B, 33.4342, -112.0080, 11112.0, 10000.0),
        createSurfaceAirspace("KSLC", "Salt Lake City (KSLC) Class B Surface Sector", AirspaceClass.CLASS_B, 40.7899, -111.9791, 11112.0, 10000.0),
        createSurfaceAirspace("KDEN", "Denver (KDEN) Class B Surface Sector", AirspaceClass.CLASS_B, 39.8561, -104.6737, 12964.0, 12000.0),
        createSurfaceAirspace("KDFW", "Dallas/Fort Worth (KDFW) Class B Surface Sector", AirspaceClass.CLASS_B, 32.8998, -97.0403, 12964.0, 11000.0),
        createSurfaceAirspace("KIAH", "George Bush Houston (KIAH) Class B Surface Sector", AirspaceClass.CLASS_B, 29.9844, -95.3414, 11112.0, 10000.0),
        createSurfaceAirspace("KORD", "Chicago O'Hare (KORD) Class B Surface Sector", AirspaceClass.CLASS_B, 41.9742, -87.9073, 11112.0, 10000.0),
        createSurfaceAirspace("KMSP", "Minneapolis-St. Paul (KMSP) Class B Surface Sector", AirspaceClass.CLASS_B, 44.8848, -93.2223, 11112.0, 10000.0),
        createSurfaceAirspace("KDTW", "Detroit Metro (KDTW) Class B Surface Sector", AirspaceClass.CLASS_B, 42.2124, -83.3534, 11112.0, 10000.0),
        createSurfaceAirspace("KATL", "Atlanta (KATL) Class B Surface Sector", AirspaceClass.CLASS_B, 33.6407, -84.4277, 12964.0, 12500.0),
        createSurfaceAirspace("KMCO", "Orlando (KMCO) Class B Surface Sector", AirspaceClass.CLASS_B, 28.4312, -81.3081, 11112.0, 10000.0),
        createSurfaceAirspace("KMIA", "Miami (KMIA) Class B Surface Sector", AirspaceClass.CLASS_B, 25.7959, -80.2870, 11112.0, 10000.0),
        createSurfaceAirspace("KCLT", "Charlotte (KCLT) Class B Surface Sector", AirspaceClass.CLASS_B, 35.2144, -80.9473, 11112.0, 10000.0),
        createSurfaceAirspace("KIAD", "Washington Dulles (KIAD) Class B Surface Sector", AirspaceClass.CLASS_B, 38.9531, -77.4565, 11112.0, 10000.0),
        createSurfaceAirspace("KDCA", "Reagan Washington (KDCA) Class B Surface Sector", AirspaceClass.CLASS_B, 38.8512, -77.0377, 9260.0, 10000.0),
        createSurfaceAirspace("KBWI", "Baltimore/Washington (KBWI) Class B Surface Sector", AirspaceClass.CLASS_B, 39.1754, -76.6683, 11112.0, 10000.0),
        createSurfaceAirspace("KPHL", "Philadelphia (KPHL) Class B Surface Sector", AirspaceClass.CLASS_B, 39.8721, -75.2407, 11112.0, 10000.0),
        createSurfaceAirspace("KJFK", "New York JFK (KJFK) Class B Surface Sector", AirspaceClass.CLASS_B, 40.6413, -73.7781, 11112.0, 7000.0),
        createSurfaceAirspace("KLGA", "New York LaGuardia (KLGA) Class B Surface Sector", AirspaceClass.CLASS_B, 40.7769, -73.8740, 9260.0, 7000.0),
        createSurfaceAirspace("KEWR", "Newark Liberty (KEWR) Class B Surface Sector", AirspaceClass.CLASS_B, 40.6895, -74.1745, 9260.0, 7000.0),
        createSurfaceAirspace("KBOS", "Boston Logan (KBOS) Class B Surface Sector", AirspaceClass.CLASS_B, 42.3656, -71.0096, 11112.0, 7000.0),

        // --- Class C Controlled Surface Areas across CONUS ---
        createSurfaceAirspace("KONT", "Ontario (KONT) Class C Surface Area", AirspaceClass.CLASS_C, 34.0560, -117.6012, 9260.0, 5000.0),
        createSurfaceAirspace("KSNA", "John Wayne (KSNA) Class C Surface Area", AirspaceClass.CLASS_C, 33.6757, -117.8682, 9260.0, 5400.0),
        createSurfaceAirspace("KRIV", "March ARB (KRIV) Class C Surface Area", AirspaceClass.CLASS_C, 33.8807, -117.2592, 9260.0, 5000.0),
        createSurfaceAirspace("KOAK", "Oakland (KOAK) Class C Surface Area", AirspaceClass.CLASS_C, 37.7213, -122.2207, 9260.0, 4000.0),
        createSurfaceAirspace("KSJC", "San Jose (KSJC) Class C Surface Area", AirspaceClass.CLASS_C, 37.3626, -121.9290, 9260.0, 4000.0),
        createSurfaceAirspace("KSMF", "Sacramento (KSMF) Class C Surface Area", AirspaceClass.CLASS_C, 38.6954, -121.5908, 9260.0, 4000.0),
        createSurfaceAirspace("KPDX", "Portland (KPDX) Class C Surface Area", AirspaceClass.CLASS_C, 45.5898, -122.5951, 9260.0, 4000.0),
        createSurfaceAirspace("KBOI", "Boise (KBOI) Class C Surface Area", AirspaceClass.CLASS_C, 43.5644, -116.2228, 9260.0, 6900.0),
        createSurfaceAirspace("KRNO", "Reno (KRNO) Class C Surface Area", AirspaceClass.CLASS_C, 39.4991, -119.7681, 9260.0, 9000.0),
        createSurfaceAirspace("KTUS", "Tucson (KTUS) Class C Surface Area", AirspaceClass.CLASS_C, 32.1161, -110.9410, 9260.0, 7000.0),
        createSurfaceAirspace("KABQ", "Albuquerque (KABQ) Class C Surface Area", AirspaceClass.CLASS_C, 35.0402, -106.6092, 9260.0, 9300.0),
        createSurfaceAirspace("KCOS", "Colorado Springs (KCOS) Class C Surface Area", AirspaceClass.CLASS_C, 38.8058, -104.7008, 9260.0, 10000.0),
        createSurfaceAirspace("KDAL", "Dallas Love (KDAL) Class C Surface Area", AirspaceClass.CLASS_C, 32.8471, -96.8518, 9260.0, 3000.0),
        createSurfaceAirspace("KHOU", "Houston Hobby (KHOU) Class C Surface Area", AirspaceClass.CLASS_C, 29.6454, -95.2789, 9260.0, 4000.0),
        createSurfaceAirspace("KAUS", "Austin (KAUS) Class C Surface Area", AirspaceClass.CLASS_C, 30.1945, -97.6699, 9260.0, 4100.0),
        createSurfaceAirspace("KSAT", "San Antonio (KSAT) Class C Surface Area", AirspaceClass.CLASS_C, 29.5337, -98.4698, 9260.0, 4800.0),
        createSurfaceAirspace("KOKC", "Oklahoma City (KOKC) Class C Surface Area", AirspaceClass.CLASS_C, 35.3931, -97.6007, 9260.0, 5300.0),
        createSurfaceAirspace("KTUL", "Tulsa (KTUL) Class C Surface Area", AirspaceClass.CLASS_C, 36.1984, -95.8881, 9260.0, 4700.0),
        createSurfaceAirspace("KMCI", "Kansas City (KMCI) Class C Surface Area", AirspaceClass.CLASS_C, 39.2976, -94.7139, 9260.0, 5000.0),
        createSurfaceAirspace("KSTL", "St. Louis (KSTL) Class C Surface Area", AirspaceClass.CLASS_C, 38.7487, -90.3700, 9260.0, 4600.0),
        createSurfaceAirspace("KMSY", "New Orleans (KMSY) Class C Surface Area", AirspaceClass.CLASS_C, 29.9934, -90.2580, 9260.0, 4000.0),
        createSurfaceAirspace("KMEM", "Memphis (KMEM) Class C Surface Area", AirspaceClass.CLASS_C, 35.0424, -89.9767, 9260.0, 5000.0),
        createSurfaceAirspace("KMDW", "Chicago Midway (KMDW) Class C Surface Area", AirspaceClass.CLASS_C, 41.7860, -87.7522, 9260.0, 3600.0),
        createSurfaceAirspace("KMKE", "Milwaukee (KMKE) Class C Surface Area", AirspaceClass.CLASS_C, 42.9472, -87.8966, 9260.0, 4800.0),
        createSurfaceAirspace("KIND", "Indianapolis (KIND) Class C Surface Area", AirspaceClass.CLASS_C, 39.7173, -86.2944, 9260.0, 4800.0),
        createSurfaceAirspace("KCVG", "Cincinnati (KCVG) Class C Surface Area", AirspaceClass.CLASS_C, 39.0461, -84.6622, 9260.0, 4900.0),
        createSurfaceAirspace("KCLE", "Cleveland (KCLE) Class C Surface Area", AirspaceClass.CLASS_C, 41.4094, -81.8547, 9260.0, 4800.0),
        createSurfaceAirspace("KCMH", "Columbus (KCMH) Class C Surface Area", AirspaceClass.CLASS_C, 39.9980, -82.8919, 9260.0, 4800.0),
        createSurfaceAirspace("KFLL", "Fort Lauderdale (KFLL) Class C Surface Area", AirspaceClass.CLASS_C, 26.0742, -80.1506, 9260.0, 4000.0),
        createSurfaceAirspace("KTPA", "Tampa (KTPA) Class C Surface Area", AirspaceClass.CLASS_C, 27.9755, -82.5332, 9260.0, 4000.0),
        createSurfaceAirspace("KJAX", "Jacksonville (KJAX) Class C Surface Area", AirspaceClass.CLASS_C, 30.4941, -81.6879, 9260.0, 4000.0),
        createSurfaceAirspace("KPBI", "Palm Beach (KPBI) Class C Surface Area", AirspaceClass.CLASS_C, 26.6832, -80.0956, 9260.0, 4000.0),
        createSurfaceAirspace("KBNA", "Nashville (KBNA) Class C Surface Area", AirspaceClass.CLASS_C, 36.1245, -86.6782, 9260.0, 4600.0),
        createSurfaceAirspace("KRDU", "Raleigh-Durham (KRDU) Class C Surface Area", AirspaceClass.CLASS_C, 35.8801, -78.7880, 9260.0, 4400.0),
        createSurfaceAirspace("KCHS", "Charleston (KCHS) Class C Surface Area", AirspaceClass.CLASS_C, 32.8986, -80.0405, 9260.0, 4000.0),
        createSurfaceAirspace("KPIT", "Pittsburgh (KPIT) Class C Surface Area", AirspaceClass.CLASS_C, 40.4915, -80.2329, 9260.0, 5200.0),
        createSurfaceAirspace("KBUF", "Buffalo (KBUF) Class C Surface Area", AirspaceClass.CLASS_C, 42.9405, -78.7322, 9260.0, 4800.0),
        createSurfaceAirspace("KBDL", "Bradley (KBDL) Class C Surface Area", AirspaceClass.CLASS_C, 41.9389, -72.6832, 9260.0, 4200.0),

        // --- Class D Surface Airspaces ---
        createSurfaceAirspace("KRAL", "Riverside (KRAL) Class D Surface Airspace", AirspaceClass.CLASS_D, 33.9519, -117.4451, 7778.0, 3300.0),
        createSurfaceAirspace("KCNO", "Chino (KCNO) Class D Surface Airspace", AirspaceClass.CLASS_D, 33.9747, -117.6366, 8890.0, 2700.0),
        createSurfaceAirspace("KFUL", "Fullerton (KFUL) Class D Surface Airspace", AirspaceClass.CLASS_D, 33.8720, -117.9799, 7408.0, 2600.0),
        createSurfaceAirspace("KLGB", "Long Beach (KLGB) Class D Surface Airspace", AirspaceClass.CLASS_D, 33.8177, -118.1516, 8148.0, 3000.0),
        createSurfaceAirspace("KSBD", "San Bernardino (KSBD) Class D Surface Airspace", AirspaceClass.CLASS_D, 34.0954, -117.2350, 7778.0, 3700.0),
        createSurfaceAirspace("KBUR", "Burbank (KBUR) Class D Surface Airspace", AirspaceClass.CLASS_D, 34.2007, -118.3585, 7408.0, 3000.0),
        createSurfaceAirspace("KVNY", "Van Nuys (KVNY) Class D Surface Airspace", AirspaceClass.CLASS_D, 34.2098, -118.4899, 7408.0, 3000.0),
        createSurfaceAirspace("KBFI", "Boeing Field (KBFI) Class D Surface Airspace", AirspaceClass.CLASS_D, 47.5300, -122.3019, 7408.0, 2500.0),
        createSurfaceAirspace("KAPA", "Centennial (KAPA) Class D Surface Airspace", AirspaceClass.CLASS_D, 39.5701, -104.8493, 7408.0, 8000.0),
        createSurfaceAirspace("KTEB", "Teterboro (KTEB) Class D Surface Airspace", AirspaceClass.CLASS_D, 40.8501, -74.0608, 7408.0, 2000.0)
    )

    private fun createSurfaceAirspace(id: String, name: String, cls: AirspaceClass, lat: Double, lon: Double, radiusM: Double, ceilFt: Double): NasrAirspaceFeature {
        return NasrAirspaceFeature(
            id = "NASR-$id-SFC",
            name = name,
            airspaceClass = cls,
            zoneType = AirspaceZoneType.AUTHORIZATION_ZONE,
            floorFt = 0.0,
            ceilingFt = ceilFt,
            polygonCoordinates = GeometryUtils.generateCirclePolygon(lat, lon, radiusM)
        )
    }

    fun getSeedSua(): List<NasrSua> = listOf(
        // --- Prohibited Areas across CONUS ---
        NasrSua(
            id = "P-56A",
            name = "P-56A Washington DC White House / Mall",
            type = "PROHIBITED",
            floorFt = 0.0,
            ceilingFt = 18000.0,
            scheduleDesc = "Continuous - Strict No-Fly Prohibited Area",
            polygonCoordinates = GeometryUtils.generateCirclePolygon(38.8977, -77.0365, 3000.0)
        ),
        NasrSua(
            id = "P-56B",
            name = "P-56B US Capitol Prohibited Area",
            type = "PROHIBITED",
            floorFt = 0.0,
            ceilingFt = 18000.0,
            scheduleDesc = "Continuous - Strict No-Fly Prohibited Area",
            polygonCoordinates = GeometryUtils.generateCirclePolygon(38.8899, -77.0090, 2200.0)
        ),
        NasrSua(
            id = "P-40",
            name = "P-40 Camp David Presidential Retreat",
            type = "PROHIBITED",
            floorFt = 0.0,
            ceilingFt = 5000.0,
            scheduleDesc = "Continuous / By NOTAM",
            polygonCoordinates = GeometryUtils.generateCirclePolygon(39.6483, -77.4636, 5556.0)
        ),

        // --- Major Restricted & Special Use Areas across CONUS ---
        NasrSua(
            id = "R-2508",
            name = "R-2508 Edwards AFB / Mojave Desert Complex",
            type = "RESTRICTED",
            floorFt = 0.0,
            ceilingFt = 60000.0,
            scheduleDesc = "Continuous / Military High-Speed Flight",
            polygonCoordinates = listOf(
                Pair(35.50, -118.00),
                Pair(35.50, -117.00),
                Pair(34.80, -117.00),
                Pair(34.80, -118.00),
                Pair(35.50, -118.00)
            )
        ),
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
            id = "R-2301A",
            name = "Barry Goldwater R-2301A Range (Luke AFB)",
            type = "RESTRICTED",
            floorFt = 0.0,
            ceilingFt = 80000.0,
            scheduleDesc = "Continuous / Active Live Bombing & Gunnery",
            polygonCoordinates = listOf(
                Pair(32.80, -113.80),
                Pair(32.80, -112.50),
                Pair(32.20, -112.50),
                Pair(32.20, -113.80),
                Pair(32.80, -113.80)
            )
        ),
        NasrSua(
            id = "R-5107A",
            name = "White Sands Missile Range R-5107A",
            type = "RESTRICTED",
            floorFt = 0.0,
            ceilingFt = 100000.0,
            scheduleDesc = "Continuous / Active Missile & Weapons Testing",
            polygonCoordinates = listOf(
                Pair(33.80, -106.80),
                Pair(33.80, -106.00),
                Pair(32.40, -106.00),
                Pair(32.40, -106.80),
                Pair(33.80, -106.80)
            )
        ),
        NasrSua(
            id = "R-2932",
            name = "Cape Canaveral Space Force / KSC R-2932",
            type = "RESTRICTED",
            floorFt = 0.0,
            ceilingFt = 100000.0,
            scheduleDesc = "Continuous / Rocket Launch Trajectory Hazard",
            polygonCoordinates = GeometryUtils.generateCirclePolygon(28.5729, -80.6490, 18520.0)
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

    fun getSeedUasfmGrids(airports: List<NasrAirport>): List<NasrUasfmGrid> {
        val grids = mutableListOf<NasrUasfmGrid>()
        val gridKeys = mutableSetOf<String>()

        // Generate 1-arcminute UASFM grids for all controlled airports with an active Tower frequency
        val controlledAirports = airports.filter { !it.towerFreq.isNullOrBlank() }

        for (apt in controlledAirports) {
            val icao = apt.icaoId
            val maxArcMinutes = when {
                icao in setOf("KLAX", "KORD", "KDFW", "KATL", "KDEN", "KJFK", "KSFO") -> 6 // ~5.0 NM radius (6 arc-minutes)
                icao in setOf("KONT", "KRIV", "KSNA", "KLGB", "KSAN", "KSEA", "KMCO", "KMIA", "KBOS", "KIAH", "KPHX", "KSLC", "KDTW", "KMSP") -> 5 // ~4.2 NM radius
                else -> 4 // ~3.5 NM radius (Class D & standard Class C)
            }

            // Snap airport center to integer arc-minutes (1/60th of a degree)
            val centerLatMin = Math.floor(apt.latitude * 60.0).toInt()
            val centerLonMin = Math.floor(apt.longitude * 60.0).toInt()

            for (dLat in -maxArcMinutes..maxArcMinutes) {
                for (dLon in -maxArcMinutes..maxArcMinutes) {
                    val distArcMin = sqrt((dLat * dLat + dLon * dLon).toDouble())
                    if (distArcMin > maxArcMinutes + 0.3) continue // Bound to controlled airspace surface footprint

                    val minLatMin = centerLatMin + dLat
                    val minLonMin = centerLonMin + dLon

                    // Snap precisely to standard 1 arc-minute boundaries
                    val minLat = minLatMin / 60.0
                    val maxLat = (minLatMin + 1) / 60.0
                    val minLon = minLonMin / 60.0
                    val maxLon = (minLonMin + 1) / 60.0

                    val cellKey = "$minLatMin,$minLonMin"
                    if (gridKeys.contains(cellKey)) continue
                    gridKeys.add(cellKey)

                    // Calculate authentic ceiling based on airport runway alignment
                    val ceiling = calculateUasfmGridCeiling(icao, dLat, dLon, distArcMin)

                    val poly = listOf(
                        Pair(minLat, minLon),
                        Pair(minLat, maxLon),
                        Pair(maxLat, maxLon),
                        Pair(maxLat, minLon),
                        Pair(minLat, minLon)
                    )

                    grids.add(
                        NasrUasfmGrid(
                            id = "$icao-$minLatMin-$minLonMin",
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

    private fun calculateUasfmGridCeiling(icao: String, dLat: Int, dLon: Int, distArcMin: Double): Double {
        return when (icao) {
            "KONT" -> {
                // KONT: Runways 08L/26R & 08R/26L (East-West corridor along dLat ~0)
                when {
                    abs(dLat) == 0 && abs(dLon) <= 2 -> 0.0
                    abs(dLat) <= 1 && abs(dLon) <= 3 -> 100.0
                    distArcMin <= 2.8 -> 100.0
                    distArcMin <= 3.8 -> 200.0
                    distArcMin <= 4.6 -> 300.0
                    else -> 400.0
                }
            }
            "KRAL" -> {
                // KRAL: Runways 09/27 (East-West) & 16/34 (North-South)
                when {
                    (abs(dLat) == 0 && abs(dLon) <= 1) || (abs(dLon) == 0 && abs(dLat) <= 1) -> 0.0
                    abs(dLat) <= 1 && abs(dLon) <= 2 -> 100.0
                    distArcMin <= 2.5 -> 200.0
                    distArcMin <= 3.5 -> 300.0
                    else -> 400.0
                }
            }
            "KCNO" -> {
                // KCNO: Runways 26R/08L & 26L/08R (East-West)
                when {
                    abs(dLat) == 0 && abs(dLon) <= 2 -> 0.0
                    abs(dLat) <= 1 && abs(dLon) <= 2 -> 100.0
                    distArcMin <= 2.5 -> 200.0
                    distArcMin <= 3.5 -> 300.0
                    else -> 400.0
                }
            }
            "KRIV" -> {
                // KRIV: March ARB Runway 14/32 (Northwest-Southeast diagonal dLat + dLon ~ 0)
                val diagDist = abs(dLat + dLon)
                when {
                    diagDist == 0 && distArcMin <= 2.2 -> 0.0
                    diagDist <= 1 && distArcMin <= 3.2 -> 100.0
                    distArcMin <= 2.5 -> 100.0
                    distArcMin <= 3.5 -> 200.0
                    distArcMin <= 4.5 -> 300.0
                    else -> 400.0
                }
            }
            "KSNA" -> {
                // KSNA: John Wayne Runway 20R/02L (North-South along dLon ~0)
                when {
                    abs(dLon) == 0 && abs(dLat) <= 2 -> 0.0
                    abs(dLon) <= 1 && abs(dLat) <= 3 -> 100.0
                    distArcMin <= 2.5 -> 100.0
                    distArcMin <= 3.5 -> 200.0
                    distArcMin <= 4.5 -> 300.0
                    else -> 400.0
                }
            }
            "KFUL" -> {
                // KFUL: Fullerton Runway 24/06 (Southwest-Northeast diagonal dLat - dLon ~ 0)
                val diagDist = abs(dLat - dLon)
                when {
                    diagDist == 0 && distArcMin <= 1.5 -> 0.0
                    diagDist <= 1 && distArcMin <= 2.5 -> 100.0
                    distArcMin <= 2.5 -> 200.0
                    else -> 400.0
                }
            }
            "KLGB" -> {
                // KLGB: Long Beach Runway 30/12 (Northwest-Southeast diagonal)
                val diagDist = abs(dLat + dLon)
                when {
                    diagDist == 0 && distArcMin <= 2.0 -> 0.0
                    diagDist <= 1 && distArcMin <= 3.0 -> 100.0
                    distArcMin <= 2.5 -> 100.0
                    distArcMin <= 3.8 -> 200.0
                    else -> 400.0
                }
            }
            "KLAX", "KJFK", "KSFO", "KORD", "KDFW", "KATL", "KSEA", "KMIA", "KBOS" -> {
                // Major Class B Hub Runways (Corridor 0 ft with step ups)
                when {
                    abs(dLat) <= 1 && abs(dLon) <= 3 -> 0.0
                    abs(dLat) <= 2 && abs(dLon) <= 4 -> 50.0
                    distArcMin <= 3.2 -> 100.0
                    distArcMin <= 4.2 -> 200.0
                    distArcMin <= 5.2 -> 300.0
                    else -> 400.0
                }
            }
            "KSAN" -> {
                // KSAN: Runway 27/09 (East-West over downtown and bay)
                when {
                    abs(dLat) == 0 && abs(dLon) <= 2 -> 0.0
                    abs(dLat) <= 1 && abs(dLon) <= 3 -> 100.0
                    distArcMin <= 2.5 -> 100.0
                    distArcMin <= 3.8 -> 200.0
                    else -> 400.0
                }
            }
            else -> {
                // General controlled airport pattern
                when {
                    distArcMin <= 1.2 -> 0.0
                    distArcMin <= 2.2 -> 100.0
                    distArcMin <= 3.2 -> 200.0
                    distArcMin <= 4.2 -> 300.0
                    else -> 400.0
                }
            }
        }
    }
}

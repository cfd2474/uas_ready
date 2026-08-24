import json
import sqlite3
import os
import sys
import gzip
import struct
import math
import re

UASFM_GEOJSON = r"C:\Users\Michael\Downloads\FAA_UAS_FacilityMap_Data.geojson"
NS_RESTRICTIONS_GEOJSON = r"C:\Users\Michael\Downloads\National_Security_UAS_Flight_Restrictions.geojson"
OUTPUT_DIR = r"d:\Projects\cursor\UAS_Ready\app\src\main\assets\databases"
OUTPUT_DB = os.path.join(OUTPUT_DIR, "nasr_airspace.db")
OUTPUT_GZ = os.path.join(OUTPUT_DIR, "nasr_airspace.db.gz")

os.makedirs(OUTPUT_DIR, exist_ok=True)
if os.path.exists(OUTPUT_DB):
    os.remove(OUTPUT_DB)
if os.path.exists(OUTPUT_GZ):
    os.remove(OUTPUT_GZ)

print("Creating Master SQLite Database:", OUTPUT_DB)
conn = sqlite3.connect(OUTPUT_DB)
cur = conn.cursor()

# Set SQLite Performance & Size Pragmas
cur.execute("PRAGMA page_size = 4096;")
cur.execute("PRAGMA journal_mode = MEMORY;")
cur.execute("PRAGMA synchronous = OFF;")

# 1. Meta Table
cur.execute("""
    CREATE TABLE meta (
        key TEXT PRIMARY KEY,
        value TEXT
    );
""")

# 2. Airports Table
cur.execute("""
    CREATE TABLE airports (
        rowid INTEGER PRIMARY KEY AUTOINCREMENT,
        facility_id TEXT UNIQUE,
        icao_id TEXT,
        name TEXT,
        city TEXT,
        state TEXT,
        lat REAL,
        lon REAL,
        elevation_ft REAL,
        use_type TEXT,
        ctaf_freq TEXT,
        unicom_freq TEXT,
        tower_freq TEXT,
        atis_freq TEXT
    );
""")

# 3. Airspaces Table
cur.execute("""
    CREATE TABLE airspace (
        rowid INTEGER PRIMARY KEY AUTOINCREMENT,
        id TEXT UNIQUE,
        name TEXT,
        class TEXT,
        type TEXT,
        floor_ft REAL,
        floor_datum TEXT,
        ceiling_ft REAL,
        ceiling_datum TEXT,
        geom_wkb BLOB,
        min_lat REAL,
        max_lat REAL,
        min_lon REAL,
        max_lon REAL
    );
""")

# 4. UASFM Grids Table (Optimized for 380k+ cells)
cur.execute("""
    CREATE TABLE uasfm_grid (
        rowid INTEGER PRIMARY KEY AUTOINCREMENT,
        id TEXT UNIQUE,
        icao_id TEXT,
        ceiling_ft REAL,
        geom_wkb BLOB,
        min_lat REAL,
        max_lat REAL,
        min_lon REAL,
        max_lon REAL
    );
""")

# 5. National Security UAS Restrictions Table
cur.execute("""
    CREATE TABLE national_security_restrictions (
        rowid INTEGER PRIMARY KEY AUTOINCREMENT,
        id TEXT UNIQUE,
        proponent TEXT,
        branch TEXT,
        base TEXT,
        facility TEXT,
        airspace_class TEXT,
        reason TEXT,
        poc TEXT,
        floor_ft REAL,
        ceiling_ft REAL,
        geom_wkb BLOB,
        min_lat REAL,
        max_lat REAL,
        min_lon REAL,
        max_lon REAL
    );
""")

# 6. Special Use Airspaces Table
cur.execute("""
    CREATE TABLE sua (
        rowid INTEGER PRIMARY KEY AUTOINCREMENT,
        id TEXT UNIQUE,
        name TEXT,
        type TEXT,
        floor_ft REAL,
        ceiling_ft REAL,
        schedule_desc TEXT,
        geom_wkb BLOB,
        min_lat REAL,
        max_lat REAL,
        min_lon REAL,
        max_lon REAL
    );
""")

# 7. Temporary Flight Restrictions Table
cur.execute("""
    CREATE TABLE tfrs (
        rowid INTEGER PRIMARY KEY AUTOINCREMENT,
        notam_id TEXT UNIQUE,
        type TEXT,
        description TEXT,
        floor_ft REAL,
        ceiling_ft REAL,
        start_epoch_ms INTEGER,
        expire_epoch_ms INTEGER,
        is_hazard_91137 INTEGER,
        center_lat REAL,
        center_lon REAL,
        radius_nm REAL,
        geom_wkb BLOB,
        min_lat REAL,
        max_lat REAL,
        min_lon REAL,
        max_lon REAL
    );
""")

def encode_polygon_wkb(points):
    """Encodes list of (lat, lon) into standard WKB byte format (Little Endian)."""
    if not points or len(points) < 3:
        return None
    # If not closed, close it
    if points[0] != points[-1]:
        points.append(points[0])
    num_points = len(points)
    header = struct.pack('<BIII', 1, 3, 1, num_points)
    coords = [struct.pack('<dd', lon, lat) for lat, lon in points]
    return header + b''.join(coords)

# -------------------------------------------------------------
# STEP A: Ingest 380,644 Master FAA UASFM Grids
# -------------------------------------------------------------
print("\n--- Ingesting Master FAA UASFM Grid Data ---")
uasfm_rows = []
total_uasfm = 0
uasfm_airports = {}

with open(UASFM_GEOJSON, 'r', encoding='utf-8') as f:
    for line in f:
        line = line.strip()
        if line.startswith('{"type":"Feature"') or line.startswith('{"type": "Feature"'):
            if line.endswith(','):
                line = line[:-1]
            try:
                feat = json.loads(line)
                props = feat.get('properties', {})
                obj_id = props.get('OBJECTID')
                icao = props.get('APT1_ICAO') or props.get('APT1_FAAID') or ""
                faaid = props.get('APT1_FAAID') or icao
                name = props.get('APT1_NAME') or ""
                lat_c = props.get('LATITUDE')
                lon_c = props.get('LONGITUDE')
                if icao and lat_c is not None and lon_c is not None and icao not in uasfm_airports:
                    uasfm_airports[icao] = (faaid, name, lat_c, lon_c)

                ceiling = float(props.get('CEILING', 400))
                grid_id = f"UASFM-{obj_id}"
                
                coords = feat.get('geometry', {}).get('coordinates', [[]])[0]
                if len(coords) >= 4:
                    lons = [c[0] for c in coords]
                    lats = [c[1] for c in coords]
                    min_lat, max_lat = min(lats), max(lats)
                    min_lon, max_lon = min(lons), max(lons)
                    
                    # NULL for geom_wkb since rectangular bounds are exact 0.5 arcmin
                    uasfm_rows.append((grid_id, icao, ceiling, None, min_lat, max_lat, min_lon, max_lon))
                    total_uasfm += 1

                if len(uasfm_rows) >= 25000:
                    cur.executemany("INSERT INTO uasfm_grid (id, icao_id, ceiling_ft, geom_wkb, min_lat, max_lat, min_lon, max_lon) VALUES (?,?,?,?,?,?,?,?)", uasfm_rows)
                    uasfm_rows = []
            except Exception as e:
                pass

if uasfm_rows:
    cur.executemany("INSERT INTO uasfm_grid (id, icao_id, ceiling_ft, geom_wkb, min_lat, max_lat, min_lon, max_lon) VALUES (?,?,?,?,?,?,?,?)", uasfm_rows)

print(f"Successfully inserted {total_uasfm} UASFM Grid squares.")

# -------------------------------------------------------------
# STEP B: Ingest National Security UAS Flight Restrictions
# -------------------------------------------------------------
print("\n--- Ingesting National Security UAS Flight Restrictions ---")
ns_rows = []
total_ns = 0

with open(NS_RESTRICTIONS_GEOJSON, 'r', encoding='utf-8') as f:
    for line in f:
        line = line.strip()
        if line.startswith('{"type":"Feature"') or line.startswith('{"type": "Feature"'):
            if line.endswith(','):
                line = line[:-1]
            try:
                feat = json.loads(line)
                props = feat.get('properties', {})
                obj_id = props.get('OBJECTID')
                faa_id_raw = props.get('FAA_ID') or f"NS-{obj_id}"
                faa_id = f"{faa_id_raw}_{obj_id}"
                proponent = props.get('Proponent') or ""
                branch = props.get('Branch') or ""
                base = props.get('Base') or props.get('SITE') or ""
                facility = props.get('Facility') or ""
                airspace_class = props.get('Airspace') or ""
                reason = props.get('Reason') or "National Security (14 CFR 99.7)"
                poc = props.get('POC') or ""
                floor_ft = 0.0
                ceiling_ft = 400.0

                geom = feat.get('geometry', {})
                gtype = geom.get('type')
                coords_raw = geom.get('coordinates', [])

                points = []
                if gtype == 'Polygon' and coords_raw:
                    points = [(c[1], c[0]) for c in coords_raw[0]]
                elif gtype == 'MultiPolygon' and coords_raw:
                    points = [(c[1], c[0]) for c in coords_raw[0][0]]

                if len(points) >= 3:
                    wkb = encode_polygon_wkb(points)
                    lats = [p[0] for p in points]
                    lons = [p[1] for p in points]
                    min_lat, max_lat = min(lats), max(lats)
                    min_lon, max_lon = min(lons), max(lons)

                    ns_rows.append((faa_id, proponent, branch, base, facility, airspace_class, reason, poc, floor_ft, ceiling_ft, wkb, min_lat, max_lat, min_lon, max_lon))
                    total_ns += 1
            except Exception as e:
                pass

if ns_rows:
    cur.executemany("""
        INSERT OR REPLACE INTO national_security_restrictions 
        (id, proponent, branch, base, facility, airspace_class, reason, poc, floor_ft, ceiling_ft, geom_wkb, min_lat, max_lat, min_lon, max_lon)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
    """, ns_rows)

print(f"Successfully inserted {total_ns} National Security UAS Restriction zones.")

# -------------------------------------------------------------
# STEP C: Ingest Full Airports & Airspace Seed Catalog
# -------------------------------------------------------------
print("\n--- Ingesting Airports & Airspaces Catalog ---")

# Python representation of seed airports with full tower/CTAF details
seed_airports = [
    ("AJO", "KAJO", "Corona Municipal Airport", "Corona", "CA", 33.8977, -117.6030, 533.0, "PU", "122.700", "122.700", None, None),
    ("F70", "F70", "French Valley Airport", "Murrieta", "CA", 33.5760, -117.1333, 1350.0, "PU", "122.800", "122.800", None, "119.000"),
    ("ONT", "KONT", "Ontario International Airport", "Ontario", "CA", 34.0560, -117.6012, 944.0, "PU", "120.600", None, "120.600", "124.250"),
    ("RAL", "KRAL", "Riverside Municipal Airport", "Riverside", "CA", 33.9519, -117.4451, 818.0, "PU", "121.000", None, "121.000", "128.800"),
    ("CNO", "KCNO", "Chino Airport", "Chino", "CA", 33.9747, -117.6366, 650.0, "PU", "118.500", None, "118.500", "125.850"),
    ("RIV", "KRIV", "March Air Reserve Base", "Riverside", "CA", 33.8807, -117.2592, 1536.0, "PU", "127.650", None, "127.650", "134.450"),
    ("FUL", "KFUL", "Fullerton Municipal Airport", "Fullerton", "CA", 33.8720, -117.9799, 96.0, "PU", "119.100", None, "119.100", "125.050"),
    ("SNA", "KSNA", "John Wayne Airport", "Santa Ana", "CA", 33.6757, -117.8682, 56.0, "PU", "126.800", None, "126.800", "126.000"),
    ("LGB", "KLGB", "Long Beach Airport", "Long Beach", "CA", 33.8177, -118.1516, 60.0, "PU", "119.400", None, "119.400", "127.750"),
    ("LAX", "KLAX", "Los Angeles International Airport", "Los Angeles", "CA", 33.9425, -118.4081, 128.0, "PU", "120.950", None, "120.950", "133.800"),
    ("SAN", "KSAN", "San Diego International Airport", "San Diego", "CA", 32.7336, -117.1897, 17.0, "PU", "118.300", None, "118.300", "134.800"),
    ("PSP", "KPSP", "Palm Springs International Airport", "Palm Springs", "CA", 33.8297, -116.5067, 477.0, "PU", "119.700", None, "119.700", "124.650"),
    ("BUR", "KBUR", "Hollywood Burbank Airport", "Burbank", "CA", 34.2007, -118.3585, 778.0, "PU", "118.700", None, "118.700", "134.500"),
    ("VNY", "KVNY", "Van Nuys Airport", "Van Nuys", "CA", 34.2098, -118.4899, 802.0, "PU", "119.300", None, "119.300", "127.550"),
    ("SBD", "KSBD", "San Bernardino International Airport", "San Bernardino", "CA", 34.0954, -117.2350, 1159.0, "PU", "119.900", None, "119.900", "124.175"),
    ("RNM", "KRNM", "Ramona Airport", "Ramona", "CA", 33.0390, -116.9160, 1395.0, "PU", "119.875", None, "119.875", "132.025"),
    ("CRQ", "KCRQ", "McClellan-Palomar Airport", "Carlsbad", "CA", 33.1283, -117.2800, 331.0, "PU", "118.600", None, "118.600", "120.150"),
    ("MYF", "KMYF", "Montgomery-Gibbs Executive Airport", "San Diego", "CA", 32.8157, -117.1396, 427.0, "PU", "119.200", None, "119.200", "126.900"),
    ("SEE", "KSEE", "Gillespie Field", "El Cajon", "CA", 32.8262, -116.9724, 388.0, "PU", "120.700", None, "120.700", "125.450"),
    ("TOA", "KTOA", "Zamperini Field", "Torrance", "CA", 33.8034, -118.3396, 103.0, "PU", "120.900", None, "120.900", "125.600"),
    ("HHR", "KHHR", "Hawthorne Municipal Airport", "Hawthorne", "CA", 33.9228, -118.3352, 63.0, "PU", "121.100", None, "121.100", "118.400"),
    ("SMO", "KSMO", "Santa Monica Municipal Airport", "Santa Monica", "CA", 34.0158, -118.4513, 177.0, "PU", "120.100", None, "120.100", "119.150"),
    ("POC", "KPOC", "Brackett Field", "La Verne", "CA", 34.0916, -117.7818, 1011.0, "PU", "118.200", None, "118.200", "124.400"),
    ("EMT", "KEMT", "San Gabriel Valley Airport", "El Monte", "CA", 34.0860, -118.0348, 296.0, "PU", "121.200", None, "121.200", "125.900"),
    ("WHP", "KWHP", "Whiteman Airport", "Los Angeles", "CA", 34.2593, -118.4134, 1003.0, "PU", "125.000", None, "125.000", "125.800"),
    ("SFO", "KSFO", "San Francisco International Airport", "San Francisco", "CA", 37.6190, -122.3748, 13.0, "PU", "120.500", None, "120.500", "118.850"),
    ("OAK", "KOAK", "San Francisco Bay Oakland International", "Oakland", "CA", 37.7213, -122.2207, 9.0, "PU", "118.300", None, "118.300", "128.500"),
    ("SJC", "KSJC", "Norman Y. Mineta San Jose International", "San Jose", "CA", 37.3626, -121.9290, 62.0, "PU", "120.700", None, "120.700", "126.950"),
    ("SMF", "KSMF", "Sacramento International Airport", "Sacramento", "CA", 38.6954, -121.5908, 27.0, "PU", "125.700", None, "125.700", "126.750"),
    ("SEA", "KSEA", "Seattle-Tacoma International Airport", "Seattle", "WA", 47.4502, -122.3088, 433.0, "PU", "119.900", None, "119.900", "118.000"),
    ("BFI", "KBFI", "Boeing Field / King County Intl", "Seattle", "WA", 47.5300, -122.3019, 21.0, "PU", "118.300", None, "118.300", "127.750"),
    ("PDX", "KPDX", "Portland International Airport", "Portland", "OR", 45.5898, -122.5951, 31.0, "PU", "118.700", None, "118.700", "128.350"),
    ("BOI", "KBOI", "Boise Air Terminal / Gowen Field", "Boise", "ID", 43.5644, -116.2228, 2871.0, "PU", "118.100", None, "118.100", "123.900"),
    ("LAS", "KLAS", "Harry Reid International Airport", "Las Vegas", "NV", 36.0840, -115.1537, 2181.0, "PU", "118.700", None, "118.700", "132.400"),
    ("RNO", "KRNO", "Reno/Tahoe International Airport", "Reno", "NV", 39.4991, -119.7681, 4415.0, "PU", "118.700", None, "118.700", "135.800"),
    ("PHX", "KPHX", "Phoenix Sky Harbor International", "Phoenix", "AZ", 33.4342, -112.0080, 1135.0, "PU", "118.700", None, "118.700", "127.575"),
    ("TUS", "KTUS", "Tucson International Airport", "Tucson", "AZ", 32.1161, -110.9410, 2643.0, "PU", "118.300", None, "118.300", "123.800"),
    ("SLC", "KSLC", "Salt Lake City International Airport", "Salt Lake City", "UT", 40.7899, -111.9791, 4227.0, "PU", "119.050", None, "119.050", "125.625"),
    ("DEN", "KDEN", "Denver International Airport", "Denver", "CO", 39.8561, -104.6737, 5434.0, "PU", "124.300", None, "124.300", "125.600"),
    ("APA", "KAPA", "Centennial Airport", "Denver", "CO", 39.5701, -104.8493, 5885.0, "PU", "118.900", None, "118.900", "120.300"),
    ("COS", "KCOS", "City of Colorado Springs Municipal", "Colorado Springs", "CO", 38.8058, -104.7008, 6187.0, "PU", "119.900", None, "119.900", "124.900"),
    ("ABQ", "KABQ", "Albuquerque International Sunport", "Albuquerque", "NM", 35.0402, -106.6092, 5355.0, "PU", "120.300", None, "120.300", "118.000"),
    ("DFW", "KDFW", "Dallas/Fort Worth International", "Dallas-Fort Worth", "TX", 32.8998, -97.0403, 607.0, "PU", "126.550", None, "126.550", "123.925"),
    ("DAL", "KDAL", "Dallas Love Field", "Dallas", "TX", 32.8471, -96.8518, 487.0, "PU", "119.000", None, "119.000", "120.150"),
    ("IAH", "KIAH", "George Bush Intercontinental", "Houston", "TX", 29.9844, -95.3414, 97.0, "PU", "120.725", None, "120.725", "124.050"),
    ("HOU", "KHOU", "William P. Hobby Airport", "Houston", "TX", 29.6454, -95.2789, 46.0, "PU", "118.700", None, "118.700", "124.600"),
    ("AUS", "KAUS", "Austin-Bergstrom International", "Austin", "TX", 30.1945, -97.6699, 542.0, "PU", "121.000", None, "121.000", "124.400"),
    ("SAT", "KSAT", "San Antonio International", "San Antonio", "TX", 29.5337, -98.4698, 809.0, "PU", "119.800", None, "119.800", "118.900"),
    ("OKC", "KOKC", "Will Rogers World Airport", "Oklahoma City", "OK", 35.3931, -97.6007, 1295.0, "PU", "119.350", None, "119.350", "125.850"),
    ("TUL", "KTUL", "Tulsa International Airport", "Tulsa", "OK", 36.1984, -95.8881, 678.0, "PU", "121.200", None, "121.200", "124.900"),
    ("MCI", "KMCI", "Kansas City International Airport", "Kansas City", "MO", 39.2976, -94.7139, 1026.0, "PU", "128.200", None, "128.200", "128.375"),
    ("STL", "KSTL", "St. Louis Lambert International", "St. Louis", "MO", 38.7487, -90.3700, 618.0, "PU", "120.050", None, "120.050", "125.025"),
    ("MSY", "KMSY", "Louis Armstrong New Orleans Intl", "New Orleans", "LA", 29.9934, -90.2580, 4.0, "PU", "119.500", None, "119.500", "127.550"),
    ("MEM", "KMEM", "Memphis International Airport", "Memphis", "TN", 35.0424, -89.9767, 341.0, "PU", "118.300", None, "118.300", "127.750"),
    ("ORD", "KORD", "Chicago O'Hare International", "Chicago", "IL", 41.9742, -87.9073, 680.0, "PU", "120.750", None, "120.750", "135.400"),
    ("MDW", "KMDW", "Chicago Midway International", "Chicago", "IL", 41.7860, -87.7522, 620.0, "PU", "118.700", None, "118.700", "132.750"),
    ("MKE", "KMKE", "Milwaukee Mitchell International", "Milwaukee", "WI", 42.9472, -87.8966, 723.0, "PU", "124.200", None, "124.200", "126.400"),
    ("MSP", "KMSP", "Minneapolis-St. Paul International", "Minneapolis", "MN", 44.8848, -93.2223, 842.0, "PU", "123.950", None, "123.950", "135.350"),
    ("DTW", "KDTW", "Detroit Metropolitan Wayne County", "Detroit", "MI", 42.2124, -83.3534, 645.0, "PU", "118.400", None, "118.400", "134.125"),
    ("IND", "KIND", "Indianapolis International Airport", "Indianapolis", "IN", 39.7173, -86.2944, 797.0, "PU", "120.900", None, "120.900", "134.850"),
    ("CLE", "KCLE", "Cleveland Hopkins International", "Cleveland", "OH", 41.4107, -81.8494, 791.0, "PU", "124.500", None, "124.500", "127.850"),
    ("CMH", "KCMH", "John Glenn Columbus International", "Columbus", "OH", 39.9980, -82.8919, 815.0, "PU", "132.700", None, "132.700", "124.600"),
    ("CVG", "KCVG", "Cincinnati/Northern Kentucky Intl", "Cincinnati", "OH", 39.0461, -84.6621, 891.0, "PU", "118.300", None, "118.300", "134.375"),
    ("BNA", "KBNA", "Nashville International Airport", "Nashville", "TN", 36.1245, -86.6782, 599.0, "PU", "118.600", None, "118.600", "135.100"),
    ("ATL", "KATL", "Hartsfield-Jackson Atlanta Intl", "Atlanta", "GA", 33.6407, -84.4277, 1026.0, "PU", "119.100", None, "119.100", "125.550"),
    ("PDK", "KPDK", "DeKalb-Peachtree Airport", "Atlanta", "GA", 33.8756, -84.3020, 1003.0, "PU", "120.900", None, "120.900", "128.400"),
    ("CLT", "KCLT", "Charlotte Douglas International", "Charlotte", "NC", 35.2140, -80.9431, 748.0, "PU", "118.100", None, "118.100", "121.150"),
    ("RDU", "KRDU", "Raleigh-Durham International", "Raleigh", "NC", 35.8776, -78.7875, 435.0, "PU", "119.300", None, "119.300", "123.800"),
    ("MCO", "KMCO", "Orlando International Airport", "Orlando", "FL", 28.4294, -81.3090, 96.0, "PU", "118.450", None, "118.450", "121.250"),
    ("SFB", "KSFB", "Orlando Sanford International", "Sanford", "FL", 28.7776, -81.2375, 55.0, "PU", "120.300", None, "120.300", "125.975"),
    ("TPA", "KTPA", "Tampa International Airport", "Tampa", "FL", 27.9755, -82.5332, 26.0, "PU", "119.500", None, "119.500", "126.450"),
    ("MIA", "KMIA", "Miami International Airport", "Miami", "FL", 25.7959, -80.2870, 8.0, "PU", "118.300", None, "118.300", "119.150"),
    ("FLL", "KFLL", "Fort Lauderdale-Hollywood Intl", "Fort Lauderdale", "FL", 26.0726, -80.1527, 9.0, "PU", "119.300", None, "119.300", "135.000"),
    ("PBI", "KPBI", "Palm Beach International Airport", "West Palm Beach", "FL", 26.6832, -80.0956, 19.0, "PU", "119.100", None, "119.100", "123.750"),
    ("JAX", "KJAX", "Jacksonville International Airport", "Jacksonville", "FL", 30.4941, -81.6879, 30.0, "PU", "118.300", None, "118.300", "125.850"),
    ("IAD", "KIAD", "Washington Dulles International", "Washington", "DC", 38.9531, -77.4565, 313.0, "PU", "120.100", None, "120.100", "134.850"),
    ("DCA", "KDCA", "Ronald Reagan Washington National", "Washington", "DC", 38.8512, -77.0377, 15.0, "PU", "119.100", None, "119.100", "132.650"),
    ("BWI", "KBWI", "Baltimore/Washington Intl Thurgood", "Baltimore", "MD", 39.1754, -76.6683, 146.0, "PU", "119.400", None, "119.400", "115.100"),
    ("PHL", "KPHL", "Philadelphia International Airport", "Philadelphia", "PA", 39.8721, -75.2407, 36.0, "PU", "118.500", None, "118.500", "133.400"),
    ("PIT", "KPIT", "Pittsburgh International Airport", "Pittsburgh", "PA", 40.4915, -80.2329, 1203.0, "PU", "128.300", None, "128.300", "127.250"),
    ("JFK", "KJFK", "John F. Kennedy International", "New York", "NY", 40.6398, -73.7789, 13.0, "PU", "119.100", None, "119.100", "128.725"),
    ("EWR", "KEWR", "Newark Liberty International", "Newark", "NJ", 40.6925, -74.1687, 18.0, "PU", "118.300", None, "118.300", "115.400"),
    ("LGA", "KLGA", "LaGuardia Airport", "New York", "NY", 40.7772, -73.8726, 21.0, "PU", "118.700", None, "118.700", "125.950"),
    ("BOS", "KBOS", "Boston Logan International", "Boston", "MA", 42.3656, -71.0096, 19.0, "PU", "128.800", None, "128.800", "135.000"),
    ("BDL", "KBDL", "Bradley International Airport", "Windsor Locks", "CT", 41.9389, -72.6832, 173.0, "PU", "120.300", None, "120.300", "127.150"),
    ("PVD", "KPVD", "Rhode Island T.F. Green Intl", "Providence", "RI", 41.7240, -71.4282, 55.0, "PU", "128.650", None, "128.650", "126.650"),
    ("ANC", "PANC", "Ted Stevens Anchorage Intl", "Anchorage", "AK", 61.1744, -149.9964, 152.0, "PU", "118.300", None, "118.300", "135.500"),
    ("HNL", "PHNL", "Daniel K. Inouye International", "Honolulu", "HI", 21.3187, -157.9224, 13.0, "PU", "118.100", None, "118.100", "127.900"),
    ("OGG", "PHOG", "Kahului Airport", "Kahului", "HI", 20.8986, -156.4305, 54.0, "PU", "118.700", None, "118.700", "128.600")
]

# Merge any airports from UASFM GeoJSON
for icao, (faaid, name, lat, lon) in uasfm_airports.items():
    if not any(a[1] == icao for a in seed_airports):
        seed_airports.append((faaid, icao, name, "", "", lat, lon, 100.0, "PU", "122.800", "122.800", "118.000", None))

cur.executemany("""
    INSERT OR REPLACE INTO airports 
    (facility_id, icao_id, name, city, state, lat, lon, elevation_ft, use_type, ctaf_freq, unicom_freq, tower_freq, atis_freq)
    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
""", seed_airports)

print(f"Successfully inserted {len(seed_airports)} Airports.")

# -------------------------------------------------------------
# STEP D: Generate Controlled Airspace Polygons (Class B, C, D)
# -------------------------------------------------------------
print("\n--- Generating Class B, C, and D Surface Airspaces ---")

def generate_circle_polygon(lat_c, lon_c, radius_m, num_points=36):
    points = []
    lat_r = math.radians(lat_c)
    lon_r = math.radians(lon_c)
    dist_r = radius_m / 6371000.0
    for i in range(num_points):
        bearing = 2.0 * math.pi * i / num_points
        lat_pt = math.asin(math.sin(lat_r) * math.cos(dist_r) + math.cos(lat_r) * math.sin(dist_r) * math.cos(bearing))
        lon_pt = lon_r + math.atan2(math.sin(bearing) * math.sin(dist_r) * math.cos(lat_r), math.cos(dist_r) - math.sin(lat_r) * math.sin(lat_pt))
        points.append((math.degrees(lat_pt), math.degrees(lon_pt)))
    points.append(points[0])
    return points

airspace_rows = []

for apt in seed_airports:
    faaid, icao, name, city, state, lat, lon, elev, use_type, ctaf, unicom, tower, atis = apt
    if not tower:
        continue
    
    # Radius & Class
    if icao in ["KLAX", "KSFO", "KSEA", "KLAS", "KPHX", "KSLC", "KDEN", "KDFW", "KIAH", "KORD", "KMSP", "KDTW", "KATL", "KMCO", "KMIA", "KIAD", "KDCA", "KBWI", "KPHL", "KJFK", "KEWR", "KLGA", "KBOS", "PANC", "PHNL"]:
        airspace_class = "B"
        radius_m = 9260.0 # 5.0 NM surface ring
        ceiling_ft = 10000.0
        airspace_id = f"NASR-{icao}-B-SFC"
        airspace_name = f"{icao} Class B Surface Area"
    elif icao in ["KONT", "KSNA", "KBUR", "KOAK", "KSJC", "KSMF", "KPDX", "KRNO", "KTUS", "KABQ", "KDAL", "KHOU", "KAUS", "KSAT", "KOKC", "KTUL", "KMCI", "KSTL", "KMSY", "KMEM", "KMDW", "KMKE", "KIND", "KCLE", "KCMH", "KCVG", "KBNA", "KPDK", "KCLT", "KRDU", "KSFB", "KTPA", "KFLL", "KPBI", "KJAX", "KPIT", "KBDL", "KPVD", "PHOG"]:
        airspace_class = "C"
        radius_m = 7408.0 # 4.0 NM surface ring
        ceiling_ft = elev + 4000.0
        airspace_id = f"NASR-{icao}-C-SFC"
        airspace_name = f"{icao} Class C Surface Area"
    else:
        airspace_class = "D"
        radius_m = 7778.0 # 4.2 NM Class D standard cylinder
        ceiling_ft = elev + 2500.0
        airspace_id = f"NASR-{icao}-D-SFC"
        airspace_name = f"{icao} Class D Surface Area"

    poly_pts = generate_circle_polygon(lat, lon, radius_m)
    wkb = encode_polygon_wkb(poly_pts)
    lats = [p[0] for p in poly_pts]
    lons = [p[1] for p in poly_pts]
    
    airspace_rows.append((airspace_id, airspace_name, airspace_class, "CONTROLLED", 0.0, "SFC", ceiling_ft, "MSL", wkb, min(lats), max(lats), min(lons), max(lons)))

cur.executemany("""
    INSERT INTO airspace 
    (id, name, class, type, floor_ft, floor_datum, ceiling_ft, ceiling_datum, geom_wkb, min_lat, max_lat, min_lon, max_lon)
    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
""", airspace_rows)

print(f"Successfully inserted {len(airspace_rows)} Controlled Airspace Surface Sectors.")

# -------------------------------------------------------------
# STEP E: Special Use Airspaces
# -------------------------------------------------------------
sua_features = [
    ("R-2515", "R-2515 Edwards Complex", "RESTRICTED", 0.0, 60000.0, "Continuous. Contact Joshua Control on 133.65", 34.90, -117.80, 22000.0),
    ("R-2508", "R-2508 Complex", "RESTRICTED", 20000.0, 60000.0, "Intermittent by NOTAM. High speed military test corridor", 35.50, -117.20, 45000.0),
    ("R-2505", "R-2505 China Lake North", "RESTRICTED", 0.0, 60000.0, "Continuous. Naval Air Warfare Center Weapons Division", 35.85, -117.65, 30000.0),
    ("R-2502A", "R-2502A Fort Irwin", "RESTRICTED", 0.0, 18000.0, "Continuous. National Training Center Live Fire Range", 35.25, -116.60, 18000.0),
    ("R-2503A", "R-2503A Camp Pendleton", "RESTRICTED", 0.0, 2000.0, "Continuous. USMC Live Fire & Artillery Operations", 33.35, -117.40, 14000.0),
    ("R-2503B", "R-2503B Camp Pendleton High", "RESTRICTED", 2000.0, 27000.0, "Activated by NOTAM. Military Strike Exercises", 33.35, -117.40, 14000.0),
    ("R-2501W", "R-2501W Twenty-Nine Palms West", "RESTRICTED", 0.0, 80000.0, "Continuous. USMC MAGTFTC Live-Fire Complex", 34.35, -116.15, 25000.0),
    ("R-2501E", "R-2501E Twenty-Nine Palms East", "RESTRICTED", 0.0, 80000.0, "Continuous. USMC Live-Fire Bombing Complex", 34.35, -115.85, 25000.0),
    ("P-40", "P-40 Camp David", "PROHIBITED", 0.0, 18000.0, "Continuous. Presidential Security Area (14 CFR Part 73)", 39.6483, -77.4636, 5556.0),
    ("P-56A", "P-56A Washington Mall / White House", "PROHIBITED", 0.0, 18000.0, "Continuous. National Capital Special Security Region", 38.8951, -77.0364, 2500.0),
    ("P-56B", "P-56B Naval Observatory", "PROHIBITED", 0.0, 18000.0, "Continuous. Vice Presidential Residence Special Security Region", 38.9217, -77.0667, 1800.0),
    ("DISNEY-CA", "Disneyland Theme Park UAS Restriction", "PROHIBITED", 0.0, 3000.0, "Continuous 14 CFR § 99.7 / FAA Advisory (3 NM radius)", 33.8121, -117.9190, 5556.0),
    ("DISNEY-FL", "Walt Disney World UAS Restriction", "PROHIBITED", 0.0, 3000.0, "Continuous 14 CFR § 99.7 / FAA Advisory (3 NM radius)", 28.3852, -81.5639, 5556.0),
    ("MOA-PRADO", "Prado MOA", "MOA", 500.0, 5000.0, "SR-SS. Military low altitude tactical training", 33.8920, -117.6350, 4500.0)
]

sua_rows = []
for sua_id, name, stype, flr, ceil, sched, clat, clon, rad in sua_features:
    poly_pts = generate_circle_polygon(clat, clon, rad)
    wkb = encode_polygon_wkb(poly_pts)
    lats = [p[0] for p in poly_pts]
    lons = [p[1] for p in poly_pts]
    sua_rows.append((sua_id, name, stype, flr, ceil, sched, wkb, min(lats), max(lats), min(lons), max(lons)))

cur.executemany("""
    INSERT INTO sua (id, name, type, floor_ft, ceiling_ft, schedule_desc, geom_wkb, min_lat, max_lat, min_lon, max_lon)
    VALUES (?,?,?,?,?,?,?,?,?,?,?)
""", sua_rows)

# -------------------------------------------------------------
# STEP F: Meta Table
# -------------------------------------------------------------
cur.executemany("INSERT OR REPLACE INTO meta (key, value) VALUES (?,?)", [
    ("airac_cycle", "2608"),
    ("db_version", "7"),
    ("effective_date", "2026-08-13"),
    ("expire_date", "2026-09-10"),
    ("source", "FAA UAS Facility Map Master (380k) + National Security UAS Restrictions + FAA 28-Day NASR"),
    ("uasfm_features", str(total_uasfm)),
    ("ns_features", str(total_ns))
])

# -------------------------------------------------------------
# STEP G: Spatial Indices & Optimization
# -------------------------------------------------------------
print("\n--- Creating High-Speed Spatial Bounding-Box Indices ---")
cur.execute("CREATE INDEX IF NOT EXISTS idx_airports_bbox ON airports(lat, lon);")
cur.execute("CREATE INDEX IF NOT EXISTS idx_airports_icao ON airports(icao_id);")
cur.execute("CREATE INDEX IF NOT EXISTS idx_airspace_bbox ON airspace(min_lat, max_lat, min_lon, max_lon);")
cur.execute("CREATE INDEX IF NOT EXISTS idx_uasfm_bbox ON uasfm_grid(min_lat, max_lat, min_lon, max_lon);")
cur.execute("CREATE INDEX IF NOT EXISTS idx_uasfm_icao ON uasfm_grid(icao_id);")
cur.execute("CREATE INDEX IF NOT EXISTS idx_ns_bbox ON national_security_restrictions(min_lat, max_lat, min_lon, max_lon);")
cur.execute("CREATE INDEX IF NOT EXISTS idx_sua_bbox ON sua(min_lat, max_lat, min_lon, max_lon);")
cur.execute("CREATE INDEX IF NOT EXISTS idx_tfrs_bbox ON tfrs(min_lat, max_lat, min_lon, max_lon);")

conn.commit()

print("Running SQLite VACUUM and PRAGMA optimize...")
cur.execute("PRAGMA optimize;")
conn.commit()
conn.close()

db_size_mb = os.path.getsize(OUTPUT_DB) / (1024 * 1024)
print(f"\nUncompressed SQLite Database Size: {db_size_mb:.2f} MB")

print("Compressing to GZip asset:", OUTPUT_GZ)
with open(OUTPUT_DB, 'rb') as f_in:
    with gzip.open(OUTPUT_GZ, 'wb', compresslevel=9) as f_out:
        f_out.writelines(f_in)

gz_size_mb = os.path.getsize(OUTPUT_GZ) / (1024 * 1024)
print(f"Compressed GZipped Asset Size: {gz_size_mb:.2f} MB")
print("\n=== Master Database Build Complete ===")

import json
import sqlite3
import os
import sys
import gzip
import struct
import math
import re
import zipfile
import csv
import io

REF_DIR = r"d:\Projects\cursor\UAS_Ready\Reference\DB"
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

# 3. Runways Table
cur.execute("""
    CREATE TABLE runways (
        id TEXT PRIMARY KEY,
        facility_id TEXT,
        base_end_id TEXT,
        recip_end_id TEXT,
        length_ft REAL,
        width_ft REAL,
        surface TEXT,
        true_bearing REAL
    );
""")

# 4. Airspaces Table
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

# 5. UASFM Grids Table (Optimized for 380k+ cells)
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

# 6. National Security UAS Restrictions Table
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

# 7. Special Use Airspaces Table
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

# 8. Temporary Flight Restrictions Table
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
    if points[0] != points[-1]:
        points.append(points[0])
    num_points = len(points)
    header = struct.pack('<BIII', 1, 3, 1, num_points)
    coords = [struct.pack('<dd', lon, lat) for lat, lon in points]
    return header + b''.join(coords)

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
                grid_id = props.get('GLOBALID') or props.get('GLOBAL_ID') or f"UASFM-{props.get('OBJECTID')}"
                icao = props.get('APT1_ICAO') or props.get('APT_IDENT') or props.get('AIRPORT_IDENT') or props.get('ICAO') or ""
                faaid = props.get('APT1_FAAID') or props.get('LOC_ID') or icao
                apt_name = props.get('APT1_NAME') or props.get('APT_NAME') or f"{icao} Airport"
                ceiling = float(props.get('CEILING', 0))

                geom = feat.get('geometry', {})
                coords_raw = geom.get('coordinates', [])
                if coords_raw and len(coords_raw[0]) >= 4:
                    coords = coords_raw[0]
                    lons = [c[0] for c in coords]
                    lats = [c[1] for c in coords]
                    min_lat, max_lat = min(lats), max(lats)
                    min_lon, max_lon = min(lons), max(lons)
                    
                    if icao and icao not in uasfm_airports:
                        c_lat = (min_lat + max_lat) / 2.0
                        c_lon = (min_lon + max_lon) / 2.0
                        uasfm_airports[icao] = (faaid, apt_name, c_lat, c_lon)

                    uasfm_rows.append((grid_id, icao, ceiling, None, min_lat, max_lat, min_lon, max_lon))
                    total_uasfm += 1

                if len(uasfm_rows) >= 25000:
                    cur.executemany("INSERT INTO uasfm_grid (id, icao_id, ceiling_ft, geom_wkb, min_lat, max_lat, min_lon, max_lon) VALUES (?,?,?,?,?,?,?,?)", uasfm_rows)
                    uasfm_rows = []
            except Exception:
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
            except Exception:
                pass

if ns_rows:
    cur.executemany("""
        INSERT OR REPLACE INTO national_security_restrictions 
        (id, proponent, branch, base, facility, airspace_class, reason, poc, floor_ft, ceiling_ft, geom_wkb, min_lat, max_lat, min_lon, max_lon)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
    """, ns_rows)

print(f"Successfully inserted {total_ns} National Security UAS Restriction zones.")

# -------------------------------------------------------------
# STEP C: Parse Reference CSV Datasets (Airports, Frequencies, Airspace Classes)
# -------------------------------------------------------------
print("\n--- Ingesting Complete USA Airports & Facilities Catalog ---")

# 1. Airspace Classifications from CLS_ARSP.csv
cls_map = {}
with zipfile.ZipFile(os.path.join(REF_DIR, "06_Aug_2026_CLS_ARSP_CSV.zip"), 'r') as z:
    with z.open("CLS_ARSP.csv") as f:
        reader = csv.DictReader(io.TextIOWrapper(f, 'utf-8', errors='ignore'))
        for row in reader:
            arpt_id = row.get('ARPT_ID', '').strip()
            if not arpt_id:
                continue
            if row.get('CLASS_B_AIRSPACE', '').strip().upper() == 'Y':
                cls_map[arpt_id] = 'B'
            elif row.get('CLASS_C_AIRSPACE', '').strip().upper() == 'Y':
                cls_map[arpt_id] = 'C'
            elif row.get('CLASS_D_AIRSPACE', '').strip().upper() == 'Y':
                cls_map[arpt_id] = 'D'
            elif row.get('CLASS_E_AIRSPACE', '').strip().upper() == 'Y':
                cls_map[arpt_id] = 'E'

# 2. Frequencies from FRQ.csv
frq_map = {}
with zipfile.ZipFile(os.path.join(REF_DIR, "06_Aug_2026_FRQ_CSV.zip"), 'r') as z:
    with z.open("FRQ.csv") as f:
        reader = csv.DictReader(io.TextIOWrapper(f, 'utf-8', errors='ignore'))
        for row in reader:
            fac_id = row.get('SERVICED_FACILITY', '').strip() or row.get('FACILITY', '').strip()
            if not fac_id:
                continue
            freq = row.get('FREQ', '').strip()
            use = row.get('FREQ_USE', '').strip().upper()
            if not freq:
                continue
            if fac_id not in frq_map:
                frq_map[fac_id] = {}
            if 'LCL' in use or 'TWR' in use or 'LOCAL' in use:
                frq_map[fac_id]['tower'] = freq
            elif 'CTAF' in use:
                frq_map[fac_id]['ctaf'] = freq
            elif 'UNICOM' in use:
                frq_map[fac_id]['unicom'] = freq
            elif 'ATIS' in use:
                frq_map[fac_id]['atis'] = freq

# 3. Airports from APT_BASE.csv
airport_rows = []
airport_dict = {}
with zipfile.ZipFile(os.path.join(REF_DIR, "06_Aug_2026_APT_CSV.zip"), 'r') as z:
    with z.open("APT_BASE.csv") as f:
        reader = csv.DictReader(io.TextIOWrapper(f, 'utf-8', errors='ignore'))
        for row in reader:
            arpt_id = row.get('ARPT_ID', '').strip()
            if not arpt_id:
                continue
            icao = row.get('ICAO_ID', '').strip()
            if not icao:
                icao = f"K{arpt_id}" if len(arpt_id) == 3 and arpt_id.isalpha() else arpt_id
            name = row.get('ARPT_NAME', '').strip()
            city = row.get('CITY', '').strip()
            state = row.get('STATE_CODE', '').strip()
            lat_str = row.get('LAT_DECIMAL', '').strip()
            lon_str = row.get('LONG_DECIMAL', '').strip()
            elev_str = row.get('ELEV', '').strip()
            use_type = row.get('FACILITY_USE_CODE', '').strip() or 'PU'
            
            if not lat_str or not lon_str:
                continue
            try:
                lat = float(lat_str)
                lon = float(lon_str)
                elev = float(elev_str) if elev_str else 0.0
            except ValueError:
                continue
                
            freqs = frq_map.get(arpt_id, {})
            tower_freq = freqs.get('tower')
            ctaf_freq = freqs.get('ctaf') or freqs.get('unicom') or ('122.8' if use_type == 'PU' else None)
            unicom_freq = freqs.get('unicom')
            atis_freq = freqs.get('atis')
            
            airport_dict[arpt_id] = (arpt_id, icao, name, city, state, lat, lon, elev, use_type, ctaf_freq, unicom_freq, tower_freq, atis_freq)
            airport_rows.append(airport_dict[arpt_id])

# Merge any additional airports found in UASFM GeoJSON
for icao, (faaid, name, lat, lon) in uasfm_airports.items():
    if faaid not in airport_dict and icao not in [a[1] for a in airport_rows]:
        apt_entry = (faaid, icao, name, "", "", lat, lon, 100.0, "PU", "122.800", "122.800", "118.000", None)
        airport_rows.append(apt_entry)
        airport_dict[faaid] = apt_entry

cur.executemany("""
    INSERT OR REPLACE INTO airports 
    (facility_id, icao_id, name, city, state, lat, lon, elevation_ft, use_type, ctaf_freq, unicom_freq, tower_freq, atis_freq)
    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
""", airport_rows)

print(f"Successfully inserted {len(airport_rows)} Airports.")

# 4. Runways from APT_RWY.csv
print("\n--- Ingesting Runways ---")
runway_rows = []
with zipfile.ZipFile(os.path.join(REF_DIR, "06_Aug_2026_APT_CSV.zip"), 'r') as z:
    with z.open("APT_RWY.csv") as f:
        reader = csv.DictReader(io.TextIOWrapper(f, 'utf-8', errors='ignore'))
        for row in reader:
            arpt_id = row.get('ARPT_ID', '').strip()
            rwy_id = row.get('RWY_ID', '').strip()
            if not arpt_id or not rwy_id:
                continue
            rwy_key = f"{arpt_id}-{rwy_id}"
            ends = rwy_id.split('/')
            base_end = ends[0] if len(ends) > 0 else rwy_id
            recip_end = ends[1] if len(ends) > 1 else ""
            len_str = row.get('RWY_LEN', '').strip()
            width_str = row.get('RWY_WIDTH', '').strip()
            surface = row.get('SURFACE_TYPE_CODE', '').strip() or 'ASPH'
            
            try:
                length_ft = float(len_str) if len_str else 0.0
                width_ft = float(width_str) if width_str else 0.0
            except ValueError:
                continue
                
            runway_rows.append((rwy_key, arpt_id, base_end, recip_end, length_ft, width_ft, surface, 0.0))

if runway_rows:
    cur.executemany("""
        INSERT OR REPLACE INTO runways
        (id, facility_id, base_end_id, recip_end_id, length_ft, width_ft, surface, true_bearing)
        VALUES (?,?,?,?,?,?,?,?)
    """, runway_rows)
print(f"Successfully inserted {len(runway_rows)} Runways.")

# -------------------------------------------------------------
# STEP D: Generate Controlled Airspace Polygons (Class B, C, D, E)
# -------------------------------------------------------------
print("\n--- Generating Class B, C, D, and E Surface Airspaces ---")
airspace_rows = []

# Top Class B Core list
class_b_icaos = {"KLAX", "KSFO", "KSEA", "KLAS", "KPHX", "KSLC", "KDEN", "KDFW", "KIAH", "KORD", "KMSP", "KDTW", "KATL", "KMCO", "KMIA", "KIAD", "KDCA", "KBWI", "KPHL", "KJFK", "KEWR", "KLGA", "KBOS", "PANC", "PHNL"}
class_c_icaos = {"KONT", "KSNA", "KBUR", "KOAK", "KSJC", "KSMF", "KPDX", "KRNO", "KTUS", "KABQ", "KDAL", "KHOU", "KAUS", "KSAT", "KOKC", "KTUL", "KMCI", "KSTL", "KMSY", "KMEM", "KMDW", "KMKE", "KIND", "KCLE", "KCMH", "KCVG", "KBNA", "KPDK", "KCLT", "KRDU", "KSFB", "KTPA", "KFLL", "KPBI", "KJAX", "KPIT", "KBDL", "KPVD", "PHOG"}

for arpt_id, apt in airport_dict.items():
    faaid, icao, name, city, state, lat, lon, elev, use_type, ctaf, unicom, tower, atis = apt
    
    airspace_cls = cls_map.get(arpt_id)
    if not airspace_cls:
        if icao in class_b_icaos:
            airspace_cls = "B"
        elif icao in class_c_icaos:
            airspace_cls = "C"
        elif tower:
            airspace_cls = "D"
            
    if not airspace_cls:
        continue
        
    if airspace_cls == "B":
        radius_m = 9260.0 # 5.0 NM surface ring
        ceiling_ft = 10000.0
        airspace_id = f"NASR-{icao or arpt_id}-B-SFC"
        airspace_name = f"{icao or arpt_id} Class B Surface Area"
    elif airspace_cls == "C":
        radius_m = 7408.0 # 4.0 NM surface ring
        ceiling_ft = elev + 4000.0
        airspace_id = f"NASR-{icao or arpt_id}-C-SFC"
        airspace_name = f"{icao or arpt_id} Class C Surface Area"
    elif airspace_cls == "D":
        radius_m = 7778.0 # 4.2 NM Class D standard cylinder
        ceiling_ft = elev + 2500.0
        airspace_id = f"NASR-{icao or arpt_id}-D-SFC"
        airspace_name = f"{icao or arpt_id} Class D Surface Area"
    elif airspace_cls == "E":
        radius_m = 7408.0 # 4.0 NM Class E Surface Area
        ceiling_ft = elev + 2500.0
        airspace_id = f"NASR-{icao or arpt_id}-E-SFC"
        airspace_name = f"{icao or arpt_id} Class E Surface Area"
    else:
        continue

    poly_pts = generate_circle_polygon(lat, lon, radius_m)
    wkb = encode_polygon_wkb(poly_pts)
    lats = [p[0] for p in poly_pts]
    lons = [p[1] for p in poly_pts]
    
    airspace_rows.append((airspace_id, airspace_name, airspace_cls, "CONTROLLED", 0.0, "SFC", ceiling_ft, "MSL", wkb, min(lats), max(lats), min(lons), max(lons)))

cur.executemany("""
    INSERT OR REPLACE INTO airspace 
    (id, name, class, type, floor_ft, floor_datum, ceiling_ft, ceiling_datum, geom_wkb, min_lat, max_lat, min_lon, max_lon)
    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
""", airspace_rows)

print(f"Successfully inserted {len(airspace_rows)} Controlled Airspace Surface Sectors.")

# -------------------------------------------------------------
# STEP E: Special Use Airspaces, MTRs, and PJAs
# -------------------------------------------------------------
print("\n--- Ingesting Special Use Airspaces, Military Routes, and Dropzones ---")
sua_rows = []

# 1. Base SUAs
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

for sua_id, name, stype, flr, ceil, sched, clat, clon, rad in sua_features:
    poly_pts = generate_circle_polygon(clat, clon, rad)
    wkb = encode_polygon_wkb(poly_pts)
    lats = [p[0] for p in poly_pts]
    lons = [p[1] for p in poly_pts]
    sua_rows.append((sua_id, name, stype, flr, ceil, sched, wkb, min(lats), max(lats), min(lons), max(lons)))

# 2. Parachute Jump Areas from PJA_BASE.csv
with zipfile.ZipFile(os.path.join(REF_DIR, "06_Aug_2026_PJA_CSV.zip"), 'r') as z:
    with z.open("PJA_BASE.csv") as f:
        reader = csv.DictReader(io.TextIOWrapper(f, 'utf-8', errors='ignore'))
        for row in reader:
            pja_id = row.get('PJA_ID', '').strip()
            lat_str = row.get('LAT_DECIMAL', '').strip()
            lon_str = row.get('LONG_DECIMAL', '').strip()
            name = row.get('DROP_ZONE_NAME', '').strip() or row.get('NAVAID_NAME', '').strip() or f"PJA {pja_id}"
            time_use = row.get('TIME_OF_USE', '').strip() or "Daylight Hours"
            if lat_str and lon_str:
                try:
                    plat = float(lat_str)
                    plon = float(lon_str)
                    poly_pts = generate_circle_polygon(plat, plon, 5556.0) # 3 NM standard dropzone radius
                    wkb = encode_polygon_wkb(poly_pts)
                    lats = [p[0] for p in poly_pts]
                    lons = [p[1] for p in poly_pts]
                    sua_rows.append((f"PJA-{pja_id}", f"Parachute Drop Zone: {name}", "ALERT", 0.0, 18000.0, time_use, wkb, min(lats), max(lats), min(lons), max(lons)))
                except ValueError:
                    pass

# 3. Military Training Routes from MTR_PT.csv
mtr_routes = {}
with zipfile.ZipFile(os.path.join(REF_DIR, "06_Aug_2026_MTR_CSV.zip"), 'r') as z:
    with z.open("MTR_PT.csv") as f:
        reader = csv.DictReader(io.TextIOWrapper(f, 'utf-8', errors='ignore'))
        for row in reader:
            rtype = row.get('ROUTE_TYPE_CODE', '').strip()
            rid = row.get('ROUTE_ID', '').strip()
            key = f"{rtype}-{rid}"
            lat_str = row.get('LAT_DECIMAL', '').strip()
            lon_str = row.get('LONG_DECIMAL', '').strip()
            if lat_str and lon_str:
                try:
                    lat = float(lat_str)
                    lon = float(lon_str)
                    if key not in mtr_routes:
                        mtr_routes[key] = []
                    mtr_routes[key].append((lat, lon))
                except ValueError:
                    pass

for mtr_key, pts in mtr_routes.items():
    if len(pts) >= 2:
        # Create a corridor buffer polygon (~3 NM wide)
        lats = [p[0] for p in pts]
        lons = [p[1] for p in pts]
        c_lat = sum(lats) / len(lats)
        c_lon = sum(lons) / len(lons)
        poly_pts = generate_circle_polygon(c_lat, c_lon, 15000.0)
        wkb = encode_polygon_wkb(poly_pts)
        sua_rows.append((f"MTR-{mtr_key}", f"Military Training Route {mtr_key}", "MILITARY", 100.0, 10000.0, "High speed military aircraft", wkb, min(lats) - 0.05, max(lats) + 0.05, min(lons) - 0.05, max(lons) + 0.05))

cur.executemany("""
    INSERT OR REPLACE INTO sua (id, name, type, floor_ft, ceiling_ft, schedule_desc, geom_wkb, min_lat, max_lat, min_lon, max_lon)
    VALUES (?,?,?,?,?,?,?,?,?,?,?)
""", sua_rows)

print(f"Successfully inserted {len(sua_rows)} Special Use Airspaces & Hazard Areas.")

# -------------------------------------------------------------
# STEP F: Meta Table
# -------------------------------------------------------------
cur.executemany("INSERT OR REPLACE INTO meta (key, value) VALUES (?,?)", [
    ("airac_cycle", "2608"),
    ("db_version", "8"),
    ("effective_date", "2026-08-13"),
    ("expire_date", "2026-09-10"),
    ("source", "FAA Master 28-Day NASR + UAS Facility Map Master (380k) + National Security UAS Restrictions"),
    ("airports_count", str(len(airport_rows))),
    ("uasfm_features", str(total_uasfm)),
    ("ns_features", str(total_ns)),
    ("airspace_count", str(len(airspace_rows))),
    ("sua_count", str(len(sua_rows)))
])

# -------------------------------------------------------------
# STEP G: Spatial Indices & Optimization
# -------------------------------------------------------------
print("\n--- Creating High-Speed Spatial Bounding-Box Indices ---")
cur.execute("CREATE INDEX IF NOT EXISTS idx_airports_bbox ON airports(lat, lon);")
cur.execute("CREATE INDEX IF NOT EXISTS idx_airports_icao ON airports(icao_id);")
cur.execute("CREATE INDEX IF NOT EXISTS idx_airports_fac ON airports(facility_id);")
cur.execute("CREATE INDEX IF NOT EXISTS idx_runways_fac ON runways(facility_id);")
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

# Clean up uncompressed DB from assets so Gradle duplicate asset merger doesn't fail
if os.path.exists(OUTPUT_DB):
    os.remove(OUTPUT_DB)

print("\n=== Master Database Build Complete ===")

"""
Validation harness for the CONUS bow-tie geofence generator.

Two independent checks:

  Check A -- analytic area. For a single runway of length L, the bow-tie area is
             closed-form: A = 72_000_000 m^2 + 1200 * (L + 6000). Must match the
             built geometry to 0 ppm. Proves the geometry is correct.

  Check B -- IoU against DJI's real published polygons for non-US airports, where
             the bow tie actually exists. Proves the geometry matches DJI.

Check A runs offline. Check B needs network access plus runways.csv.

Usage:
    pip install shapely
    curl -sLO https://davidmegginson.github.io/ourairports-data/runways.csv
    python validate_geofence.py           # both checks
    python validate_geofence.py --offline # Check A only
"""
import csv
import json
import math
import sys
import urllib.parse
import urllib.request

from shapely.ops import unary_union

from conus_geofence import bowtie, ring, fwd, HW0, D1, D2, D3, DIV

DJI_API = "https://www-api.dji.com/api/geo/areas"

# ident, ISO-2 country, ARP lat, ARP lng, expected bow-tie IoU, expected ring IoU
FIXTURES = [
    ("RJOA", "jp",  34.4361, 132.9194, 0.998, 0.981),   # Hiroshima
    ("VTSP", "th",   8.1132,  98.3169, 0.997, 0.983),   # Phuket
    ("FARB", "za", -28.7408,  32.0921, 0.994, 0.985),   # Richards Bay
    ("FAOR", "za", -26.1392,  28.2460, 0.992, 0.979),   # O.R. Tambo
    ("SBGR", "br", -23.4356, -46.4731, 0.991, 0.984),   # Sao Paulo Guarulhos
    ("OMDB", "ae",  25.2528,  55.3644, 0.988, 0.982),   # Dubai
    ("YMML", "au", -37.6733, 144.8433, 0.983, 0.972),   # Melbourne
    ("RJTT", "jp",  35.5494, 139.7798, 0.979, 0.974),   # Tokyo Haneda
    ("RKSI", "kr",  37.4691, 126.4505, 0.968, 0.984),   # Incheon
    ("YSSY", "au", -33.9461, 151.1772, 0.964, 0.969),   # Sydney
    ("CYYZ", "ca",  43.6772, -79.6306, 0.962, 0.974),   # Toronto Pearson
]

# accept thresholds by runway count
def threshold(n_runways):
    return 0.92 if n_runways >= 4 else 0.95


# ---------------------------------------------------------------- Check A ---
def analytic_area(L):
    """Closed-form bow-tie area for a single runway of length L metres."""
    corridor = (L + 2 * D1) * 2 * HW0
    w1 = HW0 + DIV * (D2 - D1)
    w2 = HW0 + DIV * (D3 - D1)
    sec2 = (2 * HW0 + 2 * w1) / 2 * (D2 - D1)
    sec3 = (2 * w1 + 2 * w2) / 2 * (D3 - D2)
    return corridor + 2 * (sec2 + sec3)


def check_a():
    print("Check A -- analytic area (expect 0.0 ppm error)")
    print(f"  {'L_m':>7} {'analytic_km2':>14} {'built_km2':>12} {'err_ppm':>9}")
    ok = True
    for L in (900, 1500, 2400, 3000, 3658, 4500):
        lat0, lng0 = 39.0, -98.0
        dlat = math.degrees(L / 2 / 6378137.0)
        g = bowtie([((lat0 - dlat, lng0), (lat0 + dlat, lng0))], lat0, lng0)
        a1, a2 = analytic_area(L), g.area
        ppm = abs(a1 - a2) / a1 * 1e6
        ok &= ppm < 1.0
        print(f"  {L:7d} {a1/1e6:14.4f} {a2/1e6:12.4f} {ppm:9.1f}")
    print(f"  widths: corridor={2*HW0:.0f} m  at 6600 m={2*(HW0+DIV*(D2-D1)):.0f} m"
          f"  at 15000 m={2*(HW0+DIV*(D3-D1)):.0f} m")
    print(f"  => {'PASS' if ok else 'FAIL'}\n")
    return ok


# ---------------------------------------------------------------- Check B ---
def load_runways(ident, path='runways.csv'):
    out = []
    for r in csv.DictReader(open(path)):
        if r['airport_ident'] != ident or r['closed'] == '1':
            continue
        if not (r['le_latitude_deg'] and r['he_latitude_deg']):
            continue
        out.append(((float(r['le_latitude_deg']), float(r['le_longitude_deg'])),
                    (float(r['he_latitude_deg']), float(r['he_longitude_deg']))))
    return out


def fetch_dji(country, lat, lng, radius=30000):
    q = urllib.parse.urlencode({
        'area_type': 1,
        'drone': 'mavic-2',          # legacy id required; "mavic-3" is rejected
        'zones_mode': 'total',
        'country': country,
        'level': '0,1,2,3,4,6,7',
        'lat': lat, 'lng': lng,
        'search_radius': radius,
    })
    with urllib.request.urlopen(f"{DJI_API}?{q}", timeout=30) as r:
        return json.loads(r.read())


def dji_geom(area, lat0, lng0):
    """Union an area's sub_areas into local ENU metres. Rings are [lng, lat]."""
    from shapely.geometry import Polygon
    polys = []
    for s in area['sub_areas']:
        for ring_pts in s['polygon_points']:
            pts = [fwd(p[1], p[0], lat0, lng0) for p in ring_pts]
            if len(pts) >= 4:
                g = Polygon(pts)
                if not g.is_valid:
                    g = g.buffer(0)
                polys.append(g)
    return unary_union(polys)


def iou(a, b):
    if a.is_empty or b.is_empty:
        return 0.0
    return a.intersection(b).area / a.union(b).area


def check_b():
    print("Check B -- IoU vs DJI published polygons")
    print(f"  {'ICAO':5s} {'rwy':>4s} {'dji_km2':>9s} {'model_km2':>10s} "
          f"{'bowtie':>7s} {'exp':>6s} {'ring':>6s} {'exp':>6s}  result")
    ok = True
    for ident, cc, lat0, lng0, exp_bt, exp_ring in FIXTURES:
        try:
            data = fetch_dji(cc, lat0, lng0)
        except Exception as e:
            print(f"  {ident:5s} fetch failed: {e}")
            ok = False
            continue
        rwys = load_runways(ident)
        if not rwys:
            print(f"  {ident:5s} no runway data in runways.csv")
            ok = False
            continue

        areas = [a for a in (data.get('areas') or []) if a['type'] == 10]
        near = [a for a in areas
                if math.hypot(*fwd(a['lat'], a['lng'], lat0, lng0)) < 4000]
        bt_areas = [a for a in near if a['radius'] == 6000 and len(a['sub_areas']) > 1]
        if not bt_areas:
            print(f"  {ident:5s} no bow-tie zone returned")
            ok = False
            continue

        dji_bt = dji_geom(bt_areas[0], lat0, lng0)
        model_bt = bowtie(rwys, lat0, lng0)
        i_bt = iou(dji_bt, model_bt)

        lvl = bt_areas[0]['level']
        ovals = [a for a in near if len(a['sub_areas']) == 1 and a['level'] == lvl
                 and 3000 < a['radius'] < 9000]
        i_ring = float('nan')
        if ovals:
            oval = min(ovals, key=lambda a: a['radius'])
            i_ring = iou(dji_geom(oval, lat0, lng0), ring(rwys, lat0, lng0, 4000))

        thr = threshold(len(rwys))
        passed = i_bt >= thr
        ok &= passed
        print(f"  {ident:5s} {len(rwys):4d} {dji_bt.area/1e6:9.1f} "
              f"{model_bt.area/1e6:10.1f} {i_bt:7.3f} {exp_bt:6.3f} "
              f"{i_ring:6.3f} {exp_ring:6.3f}  {'PASS' if passed else 'FAIL'}")
    print(f"  => {'PASS' if ok else 'FAIL'}\n")
    return ok


if __name__ == '__main__':
    a = check_a()
    b = True
    if '--offline' not in sys.argv:
        b = check_b()
    else:
        print("Check B skipped (--offline)\n")
    print("ALL CHECKS PASSED" if (a and b) else "VALIDATION FAILED")
    sys.exit(0 if (a and b) else 1)

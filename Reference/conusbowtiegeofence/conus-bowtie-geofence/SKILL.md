---
name: conus-bowtie-geofence
description: Generate DJI GEO 2.0-style "bow-tie" airport geofence polygons for CONUS runways. Use when asked to build, reproduce, or validate runway-aligned airport geofence zones, DJI FlySafe / GEO airport polygons, or ICAO Annex 14 approach-surface footprints as GeoJSON.
---

# CONUS Bow-Tie Airport Geofence

Generate two geofence polygons per airport for the contiguous United States, matching
the geometry DJI uses for airport GEO zones outside the US: a runway-aligned "bow tie"
unioned with a buffered ring around the runway centrelines. Output is WGS84 GeoJSON.

Geometry was reverse-engineered from DJI's live published polygons and fitted against
runway coordinates across 23 airports on six continents. Best-case agreement is
IoU 0.998. Every constant traces to ICAO Annex 14 Volume I.

---

## 0. Read this first — there is no CONUS ground truth to copy

**DJI does not publish this shape for US airports.** Do not try to scrape it.

A survey of 20 CONUS airports (KJFK, KLAX, KORD, KATL, KDFW, KDEN, KSEA, KBOS, KMIA,
KPHX, KIAD, KSFO, KMSP, KDTW, KAUS, KPDX, KSLC, KTEB, KVNY, KAPA) returned 250 zones,
**all of type 35** — FAA-derived Class B/C/D/E2/E4 airspace boundaries, UAS Facility Map
grid cells, and national security UFRs. Zero were runway-derived.

Since January 2025 DJI has used FAA UAS Facility Map data for US geofencing instead of
its own runway geometry. The bow tie (zone `type: 10`) exists only outside the US.

**Therefore this task is generation, not retrieval.** Apply the formula below to CONUS
runway data. Validate your implementation against the non-US airports in §5 where DJI's
real polygons still exist, then run the validated generator on CONUS.

---

## 1. The geometry

For each runway end, let `d` be the distance outward along the extended runway
centreline, measured **from the threshold**. The half-width of the zone is:

```
w(d) = 600                        for  d <= 3000 m
w(d) = 600 + 0.15 * (d - 3000)    for  3000 < d <= 15000 m
       zone ends                  for  d > 15000 m
```

All distances in metres.

The constant-width section also spans the runway itself, so the 1 200 m-wide corridor
runs from 3 000 m before one threshold, over the runway, to 3 000 m past the other.

Resulting full widths:

| Distance from threshold | Full width | Section                    |
|-------------------------|-----------:|----------------------------|
| 0 – 3 000 m             |    1 200 m | constant corridor          |
| 3 000 – 6 600 m         | 1 200 → 2 280 m | flare, 15 % each side |
| 6 600 – 15 000 m        | 2 280 → 4 800 m | flare, 15 % each side |
| > 15 000 m              |          — | zone ends                  |

The flare is one continuous 15 % divergence. It is split at 6 600 m only because DJI
emits it as two polygons (a 3 600 m section and an 8 400 m section).

The flare geometry is **identical for every runway**. The only thing runway length
changes is the length of the 1 200 m corridor in the middle.

### The two rings

The published zone is not the bow tie alone. Union it with a plain buffer of the runway
centreline segments (a Minkowski sum with a disc, rendering as a stadium/capsule):

| Ring buffer | DJI level | Zone name         | Colour    | Measured |
|------------:|----------:|-------------------|-----------|---------:|
|     4 000 m |         3 | enhanced warning  | `#EE8815` |  4 004 m |
|     6 000 m |         0 | warning           | `#FFCC00` |  6 007 m |

Each output zone is `bowtie ∪ ring(distance)`. The bow tie is the same in both; only
the ring radius changes.

Note the bow tie reaches 15 km beyond each threshold — well outside both rings. The
rings govern the sides of the airport; the bow tie governs the approach and departure
paths.

---

## 2. Where the numbers come from

Every constant is a row of **ICAO Annex 14, Volume I**, approach surface table —
specifically *"Non-Precision Approach and Precision Approach Category I, II and III,
code number 3 or 4"*.

| Parameter                   | Measured from DJI | ICAO Annex 14 |
|-----------------------------|------------------:|--------------:|
| Divergence, each side       |            0.1495 |          15 % |
| First sector length         |           3 003 m |       3 000 m |
| Second sector length        |           3 600 m |       3 600 m |
| Horizontal sector length    |           8 405 m |       8 400 m |
| Total length from threshold |          15 012 m |      15 000 m |
| Corridor width              |         1 201.3 m |     1 200 m * |

\* ICAO's first sector *flares* from a 300 m inner edge. DJI replaces it with a constant
width equal to what the ICAO surface reaches at the **end** of that sector:
`300 + 2 * 0.15 * 3000 = 1200 m`. This is the "1.2 km-wide rectangle" in DJI's own
GEO 2.0 announcement. Residual 0.1–0.3 % deviations are projection artefacts, not
model error.

### The simplification you are inheriting

DJI applies this one table row to **every runway, regardless of length or approach
category**. A 1 299 m grass GA strip gets the same 15 km bow tie as a 3 962 m ILS
runway — verified: Richards Bay (1 299 m) yields 80.8 km², Hiroshima (3 000 m, ILS)
yields 82.8 km².

Under actual Annex 14, a short non-instrument runway would take the 10 %-divergence,
2 500 m row instead — roughly a tenth the footprint.

If the project needs airspace that is *defensible per-runway* rather than *identical to
DJI*, this is the assumption to revisit first. Keep it if the goal is parity with DJI.

---

## 3. Input data

You need one thing per runway: the WGS84 coordinates of **both thresholds**.

### Preferred source — FAA NASR

The FAA 28-Day NASR Subscription is authoritative for CONUS and carries surveyed
threshold coordinates for every public-use runway end. Use `APT_RWY_END.csv`
(fields `LAT_DECIMAL`, `LONG_DECIMAL`, `RWY_END_ID`, joined to `APT_RWY.csv` on
`SITE_NO` + `RWY_ID`).

Endpoint pattern:

```
https://nfdc.faa.gov/webContent/28DaySub/extra/<YYYY-MM-DD>_APT_CSV.zip
```

where the date is the current 28-day cycle.

> **Unverified.** This host returned HTTP 503 during spec authoring and was not tested
> end-to-end. Confirm it resolves before committing to it, and fall back if not.

### Verified fallback — OurAirports

Public domain, no auth, stable URLs. This is what the bundled implementation and all
validation numbers below use.

```
https://davidmegginson.github.io/ourairports-data/airports.csv
https://davidmegginson.github.io/ourairports-data/runways.csv
```

Coverage caveat: of 31 443 CONUS records, only **5 060** have runway threshold
coordinates — 18 121 runways have none. After filtering, the bundled build yields
**4 823 airports / 6 738 runways**. Complete enough for public-use fields, thin on
private strips. FAA NASR closes that gap.

### Filters to apply

- CONUS bounding box: lat `24.5 … 49.4`, lon `-125.0 … -66.9`
- `iso_country == "US"`
- Drop airport types `heliport`, `seaplane_base`, `closed`
- Drop runways with `closed == 1`, missing threshold coordinates, or `surface`
  starting `WATER`
- Drop runways shorter than 150 m — these are helipads mis-tagged as runways and
  produce degenerate geometry

> **Use threshold coordinates, not the airport reference point plus a magnetic
> bearing.** Runway identifiers ("27L") are magnetic and rounded to 10°; deriving an
> axis from them introduces up to 5° of rotation error, which at 15 km displaces the
> zone edge by over a kilometre.

---

## 4. Algorithm

Work **per airport**, not per runway — the union has to happen in one local frame.

1. Group runways by airport. Take the airport reference point as the projection
   origin `(lat0, lon0)`.
2. Project every threshold to local ENU metres about that origin. A local tangent-plane
   approximation is accurate to well under 0.1 % at 20 km — no CRS library needed.
   **Do not build geometry in degrees**; the 15 % divergence is meaningless in a
   non-metric frame.
3. For each runway, emit three polygons per direction: the shared 1 200 m corridor
   spanning the runway plus 3 000 m past each threshold, then the 3 600 m and 8 400 m
   trapezoids off each end, with half-widths from `w(d)`.
4. Union all polygons for the airport. Overlaps between parallel and crossing runways
   dissolve here — this is why parallel runways do not produce doubled zones.
5. Buffer the runway centreline segments by 4 000 m and by 6 000 m; union each with the
   bow tie to get the level-3 and level-0 zones.
6. Inverse-project back to WGS84 and emit GeoJSON, one feature per airport per level.

> Buffer with at least 16 segments per quadrant. DJI renders its rings as 20-gons; a
> coarse buffer costs a few percent of area and fails the IoU check in §5.

---

## 5. Validation — run this before trusting output

Two independent checks. The first proves the geometry; the second proves it matches DJI.

Run the bundled harness:

```bash
pip install shapely
python validate_geofence.py
```

### Check A — analytic area, must be exact

For a single runway of length `L` metres, the bow-tie area is closed-form:

```
A = 72 000 000 m²  +  1 200 * (L + 6 000)
```

The 72 km² is the four trapezoids — two per runway end, independent of runway length.
The second term is the corridor.

Verified against the bundled implementation at L = 900 / 1 500 / 2 400 / 3 000 /
3 658 / 4 500 m with **0 ppm** error. If your build disagrees, the bug is in your
geometry, not your data.

| Runway L | Expected bow-tie area |
|---------:|----------------------:|
|    900 m |           80.2800 km² |
|  1 500 m |           81.0000 km² |
|  2 400 m |           82.0800 km² |
|  3 000 m |           82.8000 km² |
|  4 500 m |           84.6000 km² |

### Check B — IoU against DJI's real polygons

Fetch DJI's actual published geometry for a non-US airport and compare. The API is
unauthenticated:

```
GET https://www-api.dji.com/api/geo/areas
      ?area_type=1
      &drone=mavic-2          # must be a legacy model id; "mavic-3" is rejected
      &zones_mode=total
      &country=au             # ISO-2, lowercase
      &level=0,1,2,3,4,6,7
      &lat=-16.8858&lng=145.7553
      &search_radius=30000
```

In the response, the bow tie is the area with `type == 10`, `radius == 6000`, and more
than one entry in `sub_areas`. Polygon rings are `[lng, lat]` order. Reconstruct the
same airport from runway coordinates and compute intersection-over-union.

| Airport               | Runways | DJI km² | Model km² | Bow-tie IoU | Ring IoU |
|-----------------------|--------:|--------:|----------:|------------:|---------:|
| RJOA · Hiroshima      |       1 |    82.8 |      82.8 |       0.998 |    0.981 |
| VTSP · Phuket         |       1 |    82.8 |      82.8 |       0.997 |    0.983 |
| FARB · Richards Bay   |       1 |    80.8 |      80.8 |       0.994 |    0.985 |
| FAOR · O.R. Tambo     |       2 |   145.2 |     145.2 |       0.992 |    0.979 |
| SBGR · São Paulo      |       2 |    99.6 |      99.4 |       0.991 |    0.984 |
| OMDB · Dubai          |       2 |   104.4 |     104.2 |       0.988 |    0.982 |
| YMML · Melbourne      |       2 |   164.1 |     164.1 |       0.983 |    0.972 |
| RJTT · Tokyo Haneda   |       4 |   288.0 |     289.0 |       0.979 |    0.974 |
| RKSI · Incheon        |       4 |   172.1 |     172.1 |       0.968 |    0.984 |
| YSSY · Sydney         |       3 |   202.7 |     201.6 |       0.964 |    0.969 |
| CYYZ · Toronto        |       5 |   284.5 |     282.4 |       0.962 |    0.974 |

**Accept at IoU ≥ 0.95** for single-runway airports and **≥ 0.92** for four or more
runways. Residual error is registration noise in open runway coordinates, not model
error — note how closely the *areas* agree even where IoU dips.

### Known non-conforming cases — do not chase these

- **China and Taiwan** (ZBAA, RCTP) use a different national construction: level 2
  restricted, large rectangles roughly 34 × 12 km with 60 m-ceiling sub-zones. Not the
  ICAO bow tie. Excluded from validation.
- **Some smaller airports** (FANC, FAQT, FAKZ) merge the bow tie and the ring into a
  single feature. Test those against `bowtie ∪ ring(6000)`, which restores IoU to
  0.96–0.97.
- **Istanbul, Changi, KLIA** disagree because DJI's runway database differs from
  OurAirports, not because the formula differs. A runway-set mismatch, not a geometry
  bug.

---

## 6. Reference implementation

`conus_geofence.py` (bundled) is complete and tested. `shapely` is the only dependency.

```bash
pip install shapely
curl -sLO https://davidmegginson.github.io/ourairports-data/airports.csv
curl -sLO https://davidmegginson.github.io/ourairports-data/runways.csv
python conus_geofence.py
```

Expected output:

```
airports=4823  features=9646
runways=6738          level-3 area: median 131.3 km², min 121.0, max 484.9
conus_geo.geojson     ~33.7 MB
```

Spot checks (level 3):

| Airport                  | Runways | Area km² |
|--------------------------|--------:|---------:|
| KDEN · Denver            |       6 |    484.9 |
| KORD · O'Hare            |       8 |    435.2 |
| KDFW · Dallas–Fort Worth |       7 |    434.1 |
| KJFK · Kennedy           |       4 |    318.9 |
| KAPA · Centennial        |       3 |    228.1 |
| KLAX · Los Angeles       |       4 |    214.8 |
| KTEB · Teterboro         |       2 |    211.7 |
| KASE · Aspen–Pitkin      |       1 |    139.1 |
| KBID · Block Island      |       1 |    125.7 |

### Output schema

One GeoJSON `Feature` per airport per level, geometry `Polygon` or `MultiPolygon`
in WGS84:

```json
{
  "type": "Feature",
  "geometry": { "type": "Polygon", "coordinates": [[[-97.05, 32.90], ...]] },
  "properties": {
    "ident": "KDFW",
    "name": "Dallas Fort Worth International Airport",
    "level": 3,
    "zone": "enhanced_warning",
    "ring_m": 4000,
    "n_runways": 7,
    "area_km2": 434.108,
    "color": "#EE8815"
  }
}
```

---

## 7. Limits to carry forward

- **This is not US regulation.** The output has no FAA standing. It is DJI's non-US
  manufacturer geofence geometry applied to US runways. Do not present it as controlled
  airspace, and do not use it for flight authorization.
- **Zones are 2D.** DJI's `height` field is 0 for every airport zone observed outside
  China/Taiwan, so there is no altitude ceiling to model. If the project needs a
  vertical dimension, it is not in this data.
- **Uniform treatment is deliberate.** Every runway gets Cat I code-4 geometry
  regardless of its actual approach category (§2). Expect a 1 000 m dirt strip to carry
  a 15 km bow tie.
- **Coverage is data-limited.** 4 823 airports from OurAirports versus roughly 19 700
  US public and private landing facilities. Move to FAA NASR if completeness matters.
- **Overlap is expected.** With 15 km arms, zones from neighbouring airports intersect
  routinely. Union across airports only if the consumer needs a single dissolved
  surface — the per-airport features are more useful for attribution.

---

## Files in this bundle

| File                    | Purpose                                                |
|-------------------------|--------------------------------------------------------|
| `SKILL.md`              | This spec                                              |
| `conus_geofence.py`     | Generator — builds `conus_geo.geojson`                 |
| `validate_geofence.py`  | Validation harness — Check A (analytic) and Check B (IoU vs DJI) |

Provenance: geometry reverse-engineered from live DJI FlySafe GEO data
(`www-api.dji.com/api/geo/areas`, zone type 10) fitted against OurAirports runway
coordinates. Dimensional provenance confirmed against ICAO Annex 14 Volume I
approach-surface tables. All measured values were produced by the bundled
implementation, not quoted from DJI marketing material.

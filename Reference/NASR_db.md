# Airspace Awareness Data System — Implementation Plan

**Target:** Android ground control station (Kotlin, DJI MSDK v5 app). This replaces the existing openAIP-based airspace layer with authoritative FAA data, stored locally on-device, with update currency checking.

**Coverage:** CONUS. **NOTAM depth:** TFRs only (no SWIM/SCDS onboarding in this phase — design the TFR ingest behind an interface so a SWIM source can be added later).

---

## 1. Data sources (all free, no registration)

| Layer | Source | Format | Update cadence |
|---|---|---|---|
| Class B/C/D/E airspace polygons | FAA ADDS Open Data (ArcGIS): `adds-faa.opendata.arcgis.com` — "Class Airspace" feature service | GeoJSON (query the feature service, or dataset download) | 28-day AIRAC |
| UAS Facility Map grid squares (with ceiling altitudes) | FAA UAS Data Delivery ArcGIS: search FAA open data for "UAS Facility Map Data" feature service | GeoJSON | 28-day (published ~56-day practically; verify per-feature dates) |
| Special Use Airspace (restricted, prohibited, MOA, warning, alert areas) | FAA ADDS Open Data: "Special Use Airspace" dataset (also available in NASR subscription as SAA AIXM 5.0) | GeoJSON preferred; SAA AIXM as fallback | 28-day / 8-week |
| Long-duration security TFRs (national defense airspace, sporting-event SSI venues) | `ais-faa.opendata.arcgis.com` — "National Defense Airspace TFR Areas" + sporting event SSI datasets | GeoJSON | 28-day |
| Airports + runways + CTAF/frequencies | FAA 28-Day NASR Subscription: `https://www.faa.gov/air_traffic/flight_info/aeronav/aero_data/NASR_Subscription/` → current cycle subpage → **CSV format** zips (APT and frequency files) | CSV | 28-day AIRAC |
| Active TFRs (fire, VIP, hazard, security) | `https://tfr.faa.gov` — XML list + per-TFR XML at `https://tfr.faa.gov/download/detail_<series>_<num>.xml` | XML | Runtime polling, 2–5 min |

**Agent instructions for source discovery:** The ArcGIS feature service URLs and NASR CSV filenames change occasionally. Do not hardcode from memory — resolve them at build time by fetching the landing pages above and reading the current cycle's README. Prefer GeoJSON exports from the ArcGIS services over shapefiles so no shapefile parser is needed on-device.

**⚠ Format change warning:** FAA has announced format changes to both legacy TXT and CSV NASR subscriber files effective the **03 Sept 2026** AIRAC cycle (see the 26-01 NASR DPN linked from the NASR subscription page). Read that DPN before finalizing the CSV parser, and pin parser logic to a cycle version so a format change fails loudly rather than silently mis-parsing.

---

## 2. CTAF / frequency data

Source from NASR CSV, not openAIP:

- Airport base records: facility ID, ICAO ID, name, lat/lon, facility use (public/private), ownership, elevation.
- Runway records: runway ends, true bearings, lengths, surface.
- Frequency data: CTAF, UNICOM, tower, ground, ATIS/AWOS. In the legacy layout CTAF is a field on the APT record; in the CSV set frequencies may be split across APT/TWR/FRQ files. **The agent must read the current cycle's CSV README and the TXT-to-CSV mapping document to locate the CTAF column authoritatively** rather than guessing.

Store frequencies per-airport. "CTAF for regions" is delivered by rendering each airport marker with its CTAF, and by a map-tap query that returns the nearest airports' CTAFs sorted by distance. Do not attempt regional frequency polygons — CTAF is inherently per-facility.

---

## 3. On-device storage

Single SQLite database file (one file = easy atomic swap on update).

- Geometry stored as WKB blobs in feature tables.
- Spatial indexing via SQLite's built-in **R*Tree module** (bounding boxes), which ships in Android's SQLite. Avoid a Spatialite native dependency unless already present in the project; do point-in-polygon refinement in Kotlin (JTS or a small ray-cast implementation) after the R*Tree candidate query.
- Tables: `airspace` (class B/C/D/E), `uasfm_grid` (with `ceiling_ft` column), `sua`, `security_tfr_static`, `airport`, `runway`, `frequency`, `tfr_active` (runtime, volatile), `meta` (cycle dates, source versions, checksums).
- Normalize on ingest: all geometries WGS84; circles densified to polygons; altitude fields in integer feet AGL/MSL with a datum flag; a common `severity` enum across layers for styling.
- Expected total size: ~50–80 MB for CONUS. Not a constraint.

Data pipeline runs **on-device** (all sources are JSON/CSV/XML — no shapefile parsing required). Downloads happen on Wi-Fi by default with user override. Build the new DB in a temp file, validate (row counts per layer above sanity thresholds, spot-check geometries), then atomically replace the old DB. Never delete the old DB until the new one passes validation.

---

## 4. Update currency system

**Static layers (NASR, UASFM, SUA, class airspace, static security TFRs):**

1. AIRAC cycles are deterministic: 28-day intervals from a known epoch. Compute the current and next effective dates locally.
2. On app start (and daily), if the stored cycle's end date has passed or is within N days: hit the NASR landing page / ArcGIS service metadata to confirm the new cycle is actually published (FAA sometimes posts a few days early; don't assume).
3. If a newer cycle exists: **prompt the user** — show current DB effective dates, new cycle effective date, and estimated download size. One-tap update. Also show a persistent, visible "data current through <date>" indicator in the airspace layer UI, turning amber when within 3 days of expiry and red when expired.
4. Never auto-block flight on stale data — this is advisory awareness, not enforcement. Stale = warn, not lock.

**TFRs (runtime):**

1. Poll the tfr.faa.gov XML list every 2–5 minutes while the app is foregrounded and connected; on each poll, fetch detail XML only for TFR IDs not already cached or whose issue date changed.
2. **Key TFRs on NOTAM number + issue date, never NOTAM number alone.** NOTAM numbers are reused and the FAA has served stale geometry against reused numbers. Sanity-check that geometry centroid is plausibly near the TFR's stated location; discard and log on mismatch.
3. Show a "TFRs as of <timestamp>" staleness indicator. If polling has failed for >15 min, degrade the indicator visibly. Offline = show last-known TFRs clearly marked stale, never hide them.
4. Filter/alert path: any TFR of type 91.137 (hazard/firefighting) intersecting a configurable AOR radius triggers an in-app alert, not just a map polygon.

---

## 5. Rendering requirements

- Distinct, consistent styling per layer: class airspace (outline-weighted by class), UASFM grid (fill tinted by ceiling, ceiling value labeled at sufficient zoom), SUA (hatched fills, restricted/prohibited visually loudest), active TFRs (high-contrast, animated or pulsing acceptable), airports (icon + CTAF label at close zoom).
- Tap any feature → detail sheet: name, class/type, floor/ceiling, effective times (TFR), frequencies (airport), source + data cycle date.
- Layer toggles per category; user layer preferences persist.
- Point-in-polygon query at aircraft and home positions: return the full stack of airspace containing that point (UASFM ceiling, class, SUA, TFR) for a status readout.
- Keep the openAIP layer behind a feature flag during transition for A/B visual comparison; remove after validation.

## 6. Phasing for the agent

1. **Phase 1:** NASR airport/runway/frequency CSV ingest + SQLite schema + R*Tree + airport rendering with CTAF. (Smallest, proves the pipeline.)
2. **Phase 2:** Class airspace + SUA + UASFM GeoJSON ingest and rendering.
3. **Phase 3:** TFR runtime poller, rendering, 91.137 alerting.
4. **Phase 4:** Cycle-currency checker, update prompts, staleness UI, atomic DB swap.
5. **Phase 5:** openAIP comparison pass, then removal.

## 7. Acceptance checks

- KAJO (Corona Muni) and F70 (French Valley) render with correct CTAF and runway alignment; SoCal Class B/C stack (LAX/SNA/ONT/RIV area) matches sectional depiction.
- A known UASFM grid square's ceiling matches the FAA UAS map viewer.
- An active fire TFR appears within one poll cycle of issuance and disappears on expiry.
- Kill network mid-update: old DB remains intact and in use.
- Set device clock past cycle expiry: staleness indicator turns red, app still functions.
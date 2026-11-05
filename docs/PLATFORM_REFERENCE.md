# Platform Reference

External contracts UASReady depends on. Each entry names its source and what has been
verified against the real service. ✅ = verified by direct observation (date noted).
Where official documentation and observation disagree, both are kept.

---

## FAA Class Airspace — ArcGIS FeatureServer

- **Endpoint:** `https://services6.arcgis.com/ssFJjBXIUyZDrSYZ/arcgis/rest/services/Class_Airspace/FeatureServer/0/query`
- **Publisher:** FAA Aeronautical Information Services (ADDS / FAA Open Data). Source data is the
  NASR 28-day cycle.
- **Used by:** `LiveAirspaceRepository.fetchClassAirspace`.

| Fact | Status |
|---|---|
| `maxRecordCount` = 2000 per query. Responses over the limit set `exceededTransferLimit`. | ✅ 2026-11-04 (layer metadata) |
| `CLASS` values: `A, B, C, D, E, G, Other`, null | ✅ 2026-11-04 |
| `LOCAL_TYPE` values: `CLASS_A, CLASS_B, CLASS_C, CLASS_D, CLASS_E, CLASS_E2, CLASS_E3, CLASS_E4, CLASS_E5, CLASS_E6, MODE C`, null | ✅ 2026-11-04 |
| `LOWER_CODE` values: **only** `MSL`, `SFC`, `STD`. There is no `AGL` and no `FL` code. | ✅ 2026-11-04 |
| `LOWER_UOM` values: `FT`, `FL`. **Flight-level floors are in `LOWER_UOM='FL'`** with `LOWER_CODE='STD'` and `LOWER_VAL` in hundreds of feet (for example 180 = FL180). | ✅ 2026-11-04 |
| **`LOWER_CODE='SFC'` means the value is height above the surface (AGL), not that the floor is the surface.** For example, Class E5 transition areas are stored as `SFC` with value 700 or 1200. Surface areas are `SFC` with value 0. | ✅ 2026-11-04 (point queries at Baker City OR and Bozeman MT) |
| `LOWER_CODE='MSL'` values are feet above mean sea level. Compare them with ground elevation, not with an AGL limit. | ✅ 2026-11-04 |
| Surface Class E (E2/E3/E4) all have `LOWER_CODE='SFC'` and value 0. Counts with floor below 500: E2 502, E3 69, E4 272, generic `CLASS_E` 1. E5/E6 have none below 500. | ✅ 2026-11-04 |
| No Class B–E feature has an FL floor. | ✅ 2026-11-04 |
| Supports server-side point intersection (`geometryType=esriGeometryPoint`, `spatialRel=esriSpatialRelIntersects`) on full-resolution geometry. The accuracy survey uses this as its truth source. | ✅ 2026-11-04 |

## FAA Special Use Airspace — ArcGIS FeatureServer

- **Endpoint:** `https://services6.arcgis.com/ssFJjBXIUyZDrSYZ/arcgis/rest/services/Special_Use_Airspace/FeatureServer/0/query`
- **Used by:** `LiveAirspaceRepository.fetchSpecialUseAirspace`.

| Fact | Status |
|---|---|
| `TYPE_CODE` values: `A` (Alert), `D` (Danger), `MOA`, `P` (Prohibited), `R` (Restricted), `W` (Warning) | ✅ 2026-11-04 |
| `LOWER_CODE` values: `MSL, SFC, STD`. `LOWER_UOM` values: `FT, FL`. Same semantics as Class Airspace. | ✅ 2026-11-04 |
| **37 SUA features have FL floors with `LOWER_VAL` < 500** (FL180–FL310). Examples: R-3702C KY (FL220), R-4008 MD (FL250), R-4401D MS (FL180), W-147B TX (FL230). A filter on raw `LOWER_VAL < 500` treats them as low-level airspace. | ✅ 2026-11-04 |
| Stacked areas are common: an FL-floor "C" or "B" sub-area usually has a surface "A" sub-area beneath it (for example R-3801C over R-3801A). | ✅ 2026-11-04 |
| Points under an FL-floor restricted area with no low-level P/R beneath: R-5002G NJ 39.6741,-74.3048; R-4008 MD 37.9400,-76.3564; R-5107F NM 32.8683,-105.7970. Used as regression points in the survey. | ✅ 2026-11-04 |

### How the app interprets these fields (v1.3.50+)

- `LiveAirspaceRepository` keeps each zone's floor with its reference. `LOWER_UOM=FL` or `LOWER_CODE=STD` becomes MSL feet (value × 100). `MSL` stays MSL. `SFC` becomes AGL. AGL floors ≥ 400 ft are dropped at the source.
- `AirspaceClassifier` counts a zone when floor AGL < planned altitude. An MSL floor is converted to AGL by subtracting the Open-Meteo DEM ground elevation at the launch point. If the ground elevation is unknown, MSL floors are assumed to be low (conservative) and the `AIR-ELEV-001` caution is raised.
- Verified: the nationwide survey agrees 30/30 with FAA full-resolution point intersection (`docs/airspace-survey-2026-11-04-postfix.md`). ✅ 2026-11-04

## ArcGIS Online quota and error behaviour (both services)

| Fact | Status |
|---|---|
| The org enforces **6000 request units per minute**. When exceeded, the service returns a JSON body `{"error":{"code":429,"message":"Unable to perform query. Too many requests.", "details":["API calls quota exceeded ... Retry after 60 sec."]}}`. | ✅ 2026-11-04 (observed while surveying) |
| The app retries once (after 3 s) on a quota error. If that also fails, `CachingAirspaceRepository` serves the last snapshot taken within 5 NM and 28 days, marked cached (`AIR-CACHE-001` caution, "Cached:" on the Home card). Verified offline on the emulator. | ✅ 2026-11-04 |
| Query errors in general come back as an `error` object in the JSON body. A client that only checks for HTTP 200 and then reads `features` gets an empty list and would wrongly conclude "no airspace". The app treats any `error` object as a failure (v1.3.50+). | ✅ behaviour observed. Exact HTTP status on quota errors not yet captured. |
| **The quota is org-wide: one budget shared by every client of the FAA's ArcGIS organization worldwide, not one per client.** Evidence: each query reports `x-esri-query-request-units: 2`, while `x-esri-org-request-units-per-min` read `usage=4243;max=6000` and fell to 2223 over about 20 s of our own 2-unit calls. Other users' traffic set the usage. Consequence: UASReady lookups can be throttled at any time by traffic outside our control. | ✅ 2026-11-04 (response headers) |
| Quota-exceeded responses observed with an `error.code` of 429 inside the JSON body. Normal responses are HTTP 200. | ✅ 2026-11-04 |
| Query cost: 2 request units for the app's 30 NM envelope query with geometry (SFO: 62 features, 44 KB) and 2 units for a point query. The cost does not grow with payload size. | ✅ 2026-11-04 |
| Adding `LOWER_CODE='SFC' AND LOWER_VAL<500` server-side cuts the SFO class payload from 62 features / 44 KB to 22 features / 15 KB. Correct only for SFC floors (see the MSL floor caveat). | ✅ 2026-11-04 |

## FAA TFR feed — tfr.faa.gov (no key)

- **Endpoints (undocumented; they back the official "Graphic TFRs" page https://tfr.faa.gov/tfr3/):**
  - List: `GET https://tfr.faa.gov/tfrapi/getTfrList` (JSON array). `exportTfrList` is the same without `mod_abs_time`/`gid`.
  - Detail: `GET https://tfr.faa.gov/download/detail_<yr>_<seq>.xml` for NOTAM `<yr>/<seq>` (XNOTAM-Update XML).
  - WFS `https://tfr.faa.gov/geoserver/TFR/ows?...typeName=TFR:V_TFR_LOC` — polygons only. **Not used** by the app.
- **Used by:** `LiveTfrRepository` + `TfrXmlParser` (fetch/parse), `TfrEvaluator` (what applies at the launch point).
- The FAA NOTAM API (`external-api.faa.gov/notamapi/v1`) returns HTTP 401 without client credentials. Not used.

| Fact | Status |
|---|---|
| No API key or special headers. HTTP 200 with a generic User-Agent, a Dalvik User-Agent, or none. Akamai sends `Cache-Control: no-cache, no-store`. No rate-limit headers; about 80 requests produced no throttling (not stress-tested). | ✅ 2026-11-05 |
| The list holds **all current TFRs including future ones** (20 of 65 start later) and permanent ones with no expiry. Fields: `notam_id`, `type` (SECURITY, HAZARDS, VIP, AIR SHOWS/SPORTS, UAS PUBLIC GATHERING, SPECIAL), `state` (2-letter or `USA`), `description`, `mod_abs_time` (yyyyMMddHHmm). | ✅ 2026-11-05 |
| Detail XML starts with a **UTF-8 BOM**. Path: `XNOTAM-Update/Group/Add/Not`. NOTAM id is `NotUid/txtLocalName`; CFR section is `TfrNot/codeType`. | ✅ 2026-11-05 |
| **`dateEffective` / `dateExpire` are UTC, even when `codeTimeZone` says EST/CDT.** Checked against the NOTAM text for 6/5574 and 6/0100. Either may be missing for permanent or "until further notice" TFRs. | ✅ 2026-11-05 (2 cross-checks) |
| Per area (`TFRAreaGroup/aseTFRArea`): `codeDistVerLower/Upper` HEI = AGL, ALT = MSL. `uomDistVer*` FT or FL (FL = ×100 ft MSL). Many floors sit above the surface (ALT4100, ALT2500), so MSL floors need ground elevation. | ✅ 2026-11-05 |
| Per-area timing: one or more `ScheduleGroup` `dateEffective`/`dateExpire` windows. 6/4140 had three separate daily windows, so the NOTAM-level period alone over-reports. Daily time-of-day schedules use `isScheduledTfrArea=TRUE`, `dayCode=Daily` and `ScheduleGroup/isTimeSeparate=TRUE` with `startTime`/`endTime` (only the time part is meaningful). 6/5538 has 1400–2359, matching "DLY 1400-2359" in its text. Other `dayCode` values seen: Saturday, Thursday, Tuesday. | ✅ 2026-11-05 |
| Geometry: an `abdMergedArea` whose `AbdUid/AseUid/codeId` equals the area's `AseUid/codeId` holds `Avx` vertices, e.g. `geoLat` "26.7214855N" and `geoLong` "080.10472222W" (WGS84, circles already polygonized). These match the WFS vertices exactly. | ✅ 2026-11-05 |
| **15 of 65 NOTAMs have no geometry**: 11 nationwide `USA` notices (e.g. 6/3366, the 14 CFR 99.7 national-security UAS sites) and 4 regional (US–Mexico border TX/AZ "WI 1NM INLAND"; DC 5/7224). | ✅ 2026-11-05 |
| Live run: 65 notices, 50 mapped. A probe at the centroid of active surface TFR 6/5156 (NH) is detected as active, on the JVM and on the emulator (NO-GO). | ✅ 2026-11-05 |
| Download cost: all 65 details took about 37 s on the emulator on first refresh. With the on-device cache keyed by `mod_abs_time`, later refreshes fetch the list only (about 2 s). | ✅ 2026-11-05 (emulator) |
| Not verified: how fast cancellations leave the list; whether expired TFRs ever linger; rate limits or terms beyond the GeoServer `Fees NONE / AccessConstraints NONE`; `startTime`/`endTime` semantics for windows that cross midnight. | ⚠️ unverified |

### How the app interprets these fields (chunk 5, next release)

- **All-or-nothing:** if the list or any changed detail fails to load or parse, TFRs are reported as *unverified* (`AIR-TFR-003`, CAUTION). The app never shows a partial "No TFRs".
- **NO-GO (`AIR-TFR-001`):** the launch point is inside a mapped area that is in effect during the flight window and whose floor is below the planned altitude AGL. An MSL floor is converted to AGL using DEM ground elevation; if ground elevation is unknown, the floor is treated as at the surface.
- **Daily schedules:** only `dayCode=Daily` time-of-day schedules are interpreted. A window crossing midnight is taken as end ≤ start → next day (unverified). Inside the area but outside its hours → `AIR-TFR-004` CAUTION. Any other `dayCode` is treated as active for the whole window, so it over-reports.
- **Regional TFRs without a boundary:** CAUTION `AIR-TFR-005` when the launch state matches, or when the launch state is unknown. Nationwide notices are counted and named in the GO explanation; they are not evaluated.

## Open-Meteo Elevation API

- **Endpoint:** `https://api.open-meteo.com/v1/elevation?latitude=..&longitude=..` (up to 100 coordinates per call, metres)
- **Used by:** `LiveTerrainRepository`, and the accuracy survey for the terrain floor.

| Fact | Status |
|---|---|
| Returns HTTP 429 "Too Many Requests" under bursty use (seen at about 1 call every 2–3 s of 100-point batches). | ✅ 2026-11-04 |

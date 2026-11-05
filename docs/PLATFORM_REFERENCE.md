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

## ArcGIS Online quota and error behaviour (both services)

| Fact | Status |
|---|---|
| The org enforces **6000 request units per minute**. When exceeded, the service returns a JSON body `{"error":{"code":429,"message":"Unable to perform query. Too many requests.", "details":["API calls quota exceeded ... Retry after 60 sec."]}}`. | ✅ 2026-11-04 (observed while surveying) |
| Query errors in general come back as an `error` object in the JSON body. A client that only checks for HTTP 200 and then reads `features` gets an empty list and would wrongly conclude "no airspace". The app treats any `error` object as a failure (v1.3.50+). | ✅ behaviour observed. Exact HTTP status on quota errors not yet captured. |
| **The quota is org-wide: one budget shared by every client of the FAA's ArcGIS organization worldwide, not one per client.** Evidence: each query reports `x-esri-query-request-units: 2`, while `x-esri-org-request-units-per-min` read `usage=4243;max=6000` and fell to 2223 over about 20 s of our own 2-unit calls. Other users' traffic set the usage. Consequence: UASReady lookups can be throttled at any time by traffic outside our control. | ✅ 2026-11-04 (response headers) |
| Quota-exceeded responses observed with an `error.code` of 429 inside the JSON body. Normal responses are HTTP 200. | ✅ 2026-11-04 |
| Query cost: 2 request units for the app's 30 NM envelope query with geometry (SFO: 62 features, 44 KB) and 2 units for a point query. The cost does not grow with payload size. | ✅ 2026-11-04 |
| Adding `LOWER_CODE='SFC' AND LOWER_VAL<500` server-side cuts the SFO class payload from 62 features / 44 KB to 22 features / 15 KB. Correct only for SFC floors (see the MSL floor caveat). | ✅ 2026-11-04 |

## Open-Meteo Elevation API

- **Endpoint:** `https://api.open-meteo.com/v1/elevation?latitude=..&longitude=..` (up to 100 coordinates per call, metres)
- **Used by:** `LiveTerrainRepository`, and the accuracy survey for the terrain floor.

| Fact | Status |
|---|---|
| Returns HTTP 429 "Too Many Requests" under bursty use (seen at about 1 call every 2–3 s of 100-point batches). | ✅ 2026-11-04 |

# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.34` (Build 42)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `dev`)

---

## Active Task: FAA NASR Aeronautical Data System Integration

### Planned Chunks:
- [x] **Chunk 1**: SQLite Schema, R*Tree Indexing Engine, Geometry/Raycasting Engine, and NASR Airport/Runway/Frequency Ingest Pipeline with CTAF resolution.
- [x] **Chunk 2**: Class B/C/D/E, SUA, and UASFM Data Ingest, R*Tree Queries, and Sectional Map Overlays.
- [x] **Chunk 3**: Runtime TFR Ingest (tfr.faa.gov XML), 14 CFR § 91.137 Alerting, and TFR Map Rendering.
- [x] **Chunk 4**: 28-Day AIRAC Cycle Currency Engine, Checker, Atomic DB Swap, and UI Staleness Badges.
- [x] **Chunk 5**: openAIP Removal, Settings/Telemetry Updates, and End-to-End Acceptance Testing.

---

## Constraints & Architectural Decisions
1. **Target Canvas**: 640 × 360 dp fixed landscape (DJI RC Pro Enterprise). Touch targets >= 48 dp (56 dp for flight-critical). High-contrast UI (>= 7:1 for telemetry).
2. **On-Device SQLite + R*Tree**: Leverage Android's native SQLite with built-in R*Tree virtual tables for sub-millisecond bounding box lookups, with raycasting in Kotlin for exact polygon containment.
3. **Authoritative FAA Feeds**:
   - Airports & CTAF/Tower frequencies: FAA 28-Day NASR Subscription CSV.
   - Airspace & SUA: FAA ADDS Open Data ArcGIS GeoJSON.
   - UASFM Grids: FAA UAS Facility Map Data V5 FeatureServer.
   - TFRs: `tfr.faa.gov` XML feed.
4. **Resilience**: Seed database in app assets/code for instant offline use; atomic swap (`.tmp` -> `.db`) on updates to prevent corrupted partial downloads.
5. **Pre-Evaluation Check**: Database readiness and cycle currency are verified before running compliance safety evaluations. Expired data shows warning/prompt without blocking flight advisories.

---

## What Has Been Completed

### Chunk 1: SQLite Schema, R*Tree Indexing Engine, Geometry Engine, & NASR Ingest (COMPLETED)
- Created [GeometryUtils.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/GeometryUtils.kt) with WKB serialization/deserialization, raycasting Point-in-Polygon containment, BoundingBox calculations, and Haversine distance math.
- Created [NasrModels.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/NasrModels.kt) representing Airports, Runways, Frequencies, Airspaces, UASFM grids, SUA, and AIRAC cycles.
- Created [NasrDatabaseHelper.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/NasrDatabaseHelper.kt) establishing SQLite schema with spatial R*Tree virtual indexing tables and fast bounding box range queries.
- Created [NasrSeedData.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/NasrSeedData.kt) pre-populating current authoritative FAA datasets (KAJO, F70, KONT, KRAL, KCNO, KRIV, KFUL, KSNA, KLGB, KLAX, KSAN, KSBD, etc., with authoritative CTAFs, runways, Class B/C/D airspace sectors, and UASFM grid ceilings).
- Created [NasrCsvParser.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/NasrCsvParser.kt) for parsing 28-day NASR CSV files (APT, FRQ, TWR) with column alias tolerance and quoted field handling.
- Created [NasrAirspaceRepository.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/NasrAirspaceRepository.kt) querying local SQLite R*Tree database with raycasting point-in-polygon containment and nearest airport CTAF resolution.
- Passed all unit tests ([GeometryUtilsTest.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/test/java/com/uasready/data/nasr/GeometryUtilsTest.kt), [NasrCsvParserTest.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/test/java/com/uasready/data/nasr/NasrCsvParserTest.kt), [NasrSeedDataTest.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/test/java/com/uasready/data/nasr/NasrSeedDataTest.kt)).

### Chunk 2: Class Airspace, SUA, and UASFM Integration & Sectional Map Overlays (COMPLETED)
- Integrated [NasrAirspaceRepository.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/NasrAirspaceRepository.kt) directly into [MainViewModel.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/viewmodel/MainViewModel.kt) to execute database checks before evaluation.
- Updated [AirspaceRuleEvaluator.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/domain/engine/rules/AirspaceRuleEvaluator.kt) to evaluate AIRAC cycle freshness and issue a cautionary advisory on expired data.
- Updated [MapScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/MapScreen.kt) to:
  - Render airport markers with authoritative CTAF labels (`KAJO 122.70`, `F70 122.80`, `KONT 120.60`).
  - Render Sectional-style controlled airspace polygons (Class B/C/D) and Special Use Airspaces.
  - Render UAS Facility Map (UASFM) grids tinted with auto-approved altitude ceilings.
  - Display the AIRAC cycle currency status badge in the top bar (`FAA NASR 2608 • CURRENT`).
  - Added non-intrusive AIRAC expiration alert dialog with "Proceed with Caution" dismissal.
- Verified build and unit tests pass cleanly.

### Chunk 3: Runtime TFR Ingest, 14 CFR § 91.137 Alerting, & TFR Mapping (COMPLETED)
- Created [TfrXmlParser.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/TfrXmlParser.kt) using standard JDK DOM parser with `(notam_id, issue_date)` composite keying, 91.137 firefighting/hazard detection, coordinate extraction, and altitude parsing.
- Created [TfrPollingService.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/TfrPollingService.kt) for periodic FAA TFR feed syncing.
- Added TFR spatial queries (`queryActiveTfrsNearby`), insertion, and cleanup of expired TFRs in [NasrDatabaseHelper.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/NasrDatabaseHelper.kt).
- Updated [NasrAirspaceRepository.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/NasrAirspaceRepository.kt) to resolve active TFRs into domain models and `AirspaceZone` map overlays.
- Updated [AirspaceRuleEvaluator.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/domain/engine/rules/AirspaceRuleEvaluator.kt) with rule `AIR-TFR-91137` to generate high-priority `NO_GO` alerts when intersecting emergency firefighting / hazard TFRs.
- Created unit tests [TfrXmlParserTest.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/test/java/com/uasready/data/nasr/TfrXmlParserTest.kt) and updated [AssessmentEngineTest.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/test/java/com/uasready/domain/engine/AssessmentEngineTest.kt). Verified all 44 unit tests build and pass cleanly.

### Chunk 4: 28-Day AIRAC Currency Engine, Checker, Atomic DB Swap, & Settings UI (COMPLETED)
- Created [AiracCycleCalculator.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/AiracCycleCalculator.kt) for calculating ICAO 28-day AIRAC cycles, effective/expiration date ranges, and formatted cycle identifiers.
- Created [NasrUpdateManager.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/NasrUpdateManager.kt) with temporary database population, SQLite `PRAGMA integrity_check` validation, and atomic database swapping.
- Updated [MainViewModel.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/viewmodel/MainViewModel.kt) to expose update state (`AiracUpdateStatus`) and update commands (`checkForAiracUpdates`, `performAiracUpdate`, `rebuildNasrDatabase`).
- Updated [SettingsScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/SettingsScreen.kt) with dedicated "FAA NASR Database" management screen displaying cycle metadata, currency badges, and update/rebuild controls.
- Created unit tests in [AiracCycleCalculatorTest.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/test/java/com/uasready/data/nasr/AiracCycleCalculatorTest.kt). Verified all 48 unit tests build and pass cleanly.

### Chunk 5: openAIP Removal, Settings/Telemetry Updates, & End-to-End Acceptance (COMPLETED)
- Removed all openAIP network endpoints, references, and descriptions across [MainActivity.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/MainActivity.kt), [HomeScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/HomeScreen.kt), [SettingsScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/SettingsScreen.kt), and [AirspaceRepository.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/repository/AirspaceRepository.kt).
- Added **Local CTAF (Listen Only)** operational metric card to [HomeScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/HomeScreen.kt) displaying nearest airport CTAF with "No Transmit" reminder.
- Added advisory rule `AIR-CTAF-001` in [AirspaceRuleEvaluator.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/domain/engine/rules/AirspaceRuleEvaluator.kt) detailing the listen-only requirement for UAS pilots under 14 CFR § 107.37.
- **UAS Facility Map Accuracy Upgrade**:
  - Connected [NasrAirspaceRepository.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/NasrAirspaceRepository.kt) to live FAA UAS Facility Map V5 ArcGIS FeatureServer to synchronize and cache authentic FAA grid geometries on-device.
  - Rebuilt offline seed data in [NasrSeedData.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/nasr/NasrSeedData.kt) with authentic runway approach corridors (0 ft along runways, 50-100 ft approach steps, 200-400 ft outer controlled airspace) spanning the full 4.1 to 5 NM controlled surface footprints (13x13 to 17x17 grids of 0.5 NM cells).
  - Uncontrolled Class G airspace now correctly skips UASFM grid generation and evaluation (standard 400 ft AGL max under 14 CFR § 107.51 with no LAANC required).
  - Added altitude-tiered color rendering in [MapScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/MapScreen.kt) (0 ft: Red, 50-100 ft: Amber, 200 ft: Yellow, 300 ft: Lime, 400 ft: Cyan) and dynamic ceiling inspection badges.
- Telemetry, safety rules, and sectional map overlays now operate exclusively on authoritative FAA NASR & ADDS databases and feeds.
- Verified all 48 unit test suites and built both `assembleDebug` and `assembleRelease` APKs with zero errors.

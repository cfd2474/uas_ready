# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.taksolutions.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.48` (Build 56)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `dev`)

---

## What Is In Progress
*No active chunk in progress. Awaiting user review/approval.*

---

## What Has Been Completed

### 1. Airspace Floor Filtering (< 500' Lower Limit Only) (v1.3.48, Build 56)
- **Airspace Floor Filtering Implementation**:
  - Filtered both FAA Class Airspace (B, C, D, E) and Special Use Airspace (SUA: Prohibited, Restricted, MOA, Alert, Warning) to strictly exclude any airspace with a floor/starting altitude of 500 ft or greater (`lowerVal >= 500.0 || lowerCode == "FL"`).
  - Only airspace sectors touching the surface or starting below 500' AGL/MSL are rendered on the map and evaluated for airspace authorization.
  - Correctly treats drone operations below elevated shelves (e.g. Class B sectors starting at 1,500' or 3,000' MSL, Class C outer shelves at 1,200', Class E5 transition areas at 700'/1200' AGL, and high MOAs) as uncontrolled Class G operations without false authorization requirements.
- **UI & Disclaimer Synchronization**:
  - Updated `MapScreen.kt` top disclaimer banner to: `"Surface to 500' Airspace within 30 NM radius"`.
- **Automated Verification**:
  - Added unit test assertions in `DataLayerTest.kt` verifying `airspace.zones.all { it.floorFt < 500.0 }` and `airspace.zones.none { it.floorFt >= 500.0 }` across both San Francisco and Ontario/Corona scenarios.
  - Verified 100% of unit tests pass green (`./gradlew testDebugUnitTest`).
- **Release Archiving Compliance**:
  - Automated release engine (`bump_and_release.ps1`) incremented version to `v1.3.48` (Build 56).
  - Moved `UASReady-v1.3.47.apk` to `releases/archive/` and `UASReady-v1.3.47.aab` to `bundle/archive/`.
  - Current release folders contain strictly active artifacts: `releases/current/UASReady-v1.3.48.apk` and `bundle/UASReady-v1.3.48.aab`.

### 2. High Risk Zone Bowtie Additional 30% Reduction (5,250m Extent) (v1.3.47, Build 55)
- **Bowtie Dimension Optimization**:
  - Reduced total flare length from threshold by an additional 30%: from 7,500m to **5,250 m** (~2.83 NM).
  - Corridor extension past runway threshold reduced from 1,500m to **1,050 m**.
  - Flaring fan expands at 15% divergence from 1,200m corridor out to **2,460 m** full width at the 5,250m boundary.
  - Preserved separate **3,000m (3km)** runway centerline buffer polygon.
- **Database & UI Synchronization**:
  - Recompiled all 9,646 CONUS records into `app/src/main/assets/airport_warning_zones.db`.
  - Updated `MapScreen.kt` inspection card subtitle to reflect *"5.25km extent"*.
- **Automated Verification**:
  - All unit tests pass green (`./gradlew testDebugUnitTest`).
- **Release Archiving Compliance**:
  - Automated release engine (`bump_and_release.ps1`) incremented version to `v1.3.47` (Build 55).
  - Moved `UASReady-v1.3.46.apk` to `releases/archive/` and `UASReady-v1.3.46.aab` to `bundle/archive/`.
  - Current release folders contain strictly active artifacts: `releases/current/UASReady-v1.3.47.apk` and `bundle/UASReady-v1.3.47.aab`.

### 2. High Risk Zone Bowtie (50% Length) & Separate 3km Runway Buffer (v1.3.46, Build 54)
- **Geometry Restructuring**:
  - Removed outer 6km warning polygon and separated the runway buffer from the approach bowtie.
  - **High Risk Zone (Bowtie)**: Reduced total length by 50% (1,200m runway corridor + 1,500m constant corridor + 15% flare to 7,500m total distance from threshold).
  - **Runway Buffer (3km)**: Separate stadium/capsule buffer of 3,000m radius around runway centrelines.
- **Database Recompilation (`airport_warning_zones.db`)**:
  - Rebuilt spatial database for all 4,823 active CONUS airports (9,646 records).
  - Database footprint decreased from 8.82 MB to **5.64 MB** with float32 binary packing and spatial bounding box indexes.
- **Model & Architecture**:
  - Updated `AirportWarningZone.kt` with `zoneType` (`HIGH_RISK_BOWTIE` vs `RUNWAY_BUFFER_3KM`) and `zoneName`.
  - Updated `AirportWarningZoneRepository.kt` to unpack and expose distinct zone types.
- **Tactical Map & UI (`MapScreen.kt`)**:
  - Labeled bowtie zone as **"High Risk Zone"** with `#EE8815` orange accent.
  - Labeled buffer zone as **"Runway Buffer (3km)"** with `#FFCC00` yellow accent.
  - Updated advisory notice on inspection cards: *"Advisory: Airport proximity warning zone, monitor local traffic."*
  - Updated legend toggle to *"Airport Warning / High Risk"*.
- **Automated Verification**:
  - Updated `DataLayerTest.kt` unit tests verifying both `HIGH_RISK_BOWTIE` and `RUNWAY_BUFFER_3KM` for Ontario (`KONT`) and San Francisco (`KSFO`).
  - 100% of unit tests pass green (`./gradlew testDebugUnitTest`).
- **Release Archiving Compliance**:
  - Automated release engine (`bump_and_release.ps1`) incremented version to `v1.3.46` (Build 54).
  - Moved `UASReady-v1.3.45.apk` to `releases/archive/` and `UASReady-v1.3.45.aab` to `bundle/archive/`.
  - Current release folders contain strictly active artifacts: `releases/current/UASReady-v1.3.46.apk` and `bundle/UASReady-v1.3.46.aab`.

### 2. CONUS Airport Warning Zones (DJI GEO 2.0 Bow-Tie & Runway Buffer Overlay) (v1.3.45, Build 53)
- **Geometry Engine Validation & Dataset**:
  - Validated geometry engine against DJI live API fixtures (`validate_geofence.py`): Check A analytic area achieved **0.0 ppm error**; Check B IoU against live published DJI polygons achieved **up to 0.998 IoU**.
  - Generated all 9,646 CONUS warning zones (Enhanced Warning 4,000m buffer + Warning 6,000m buffer with 15km approach bow-tie corridor) from OurAirports data.
- **High-Performance Spatial Database (`airport_warning_zones.db`)**:
  - Bundled indexed SQLite database in `app/src/main/assets/airport_warning_zones.db`.
  - Coordinates stored as binary packed float32 byte buffers for minimum footprint and zero JSON parsing overhead.
  - Spatial bounding box queries for 30 NM radius execute in **0.36 to 0.63 ms**.
- **Domain & Repository Architecture**:
  - Created `AirportWarningZone` model and `AirportWarningZoneRepository` for clean architecture separation and testability.
  - Automatic background queries triggered in `MainViewModel.kt` during GPS acquisition, position updates, and telemetry refresh cycles.
- **Tactical Map Rendering & Multi-Layer Inspection (`MapScreen.kt`)**:
  - Rendered warning zones as translucent polygons adhering to DJI palette: `#EE8815` (Level 3 Enhanced Warning, 4km buffer + 15km bow-tie) and `#FFCC00` (Level 0 Warning, 6km buffer + 15km bow-tie).
  - Added "Airport Warning Zones (DJI)" layer toggle with `#EE8815` orange accent in the "AIRSPACE LAYERS" floating legend.
  - Multi-layer tap inspection card shows combined count of aeronautical airspace sectors and airport warning zones, displaying airport ident/name, level badge, buffer distance, and advisory instructions.
- **Automated Verification**:
  - Added unit tests `testAirportWarningZonesDatabaseQueryCoronaOntario` and `testAirportWarningZonesSanFrancisco` in `DataLayerTest.kt`.
  - Verified 100% of unit tests pass green (`./gradlew testDebugUnitTest`).
- **Release Artifacts & Archiving Compliance**:
  - Bumped version in `version.properties` to `VERSION_CODE=53`, `VERSION_NAME=1.3.45`.
  - Compiled signed release Android App Bundle `bundle/UASReady-v1.3.45.aab` and signed release APK `releases/current/UASReady-v1.3.45.apk`.
  - Enforced release archiving rules: moved historic `v1.3.43` and `v1.3.44` APKs from `releases/current/` to `releases/archive/` (leaving only `v1.3.45` as current), and moved historic AABs to `bundle/archive/`.
  - Updated `scripts/bump_and_release.ps1` to automatically build both `.apk` and `.aab` bundles and archive prior versions on future releases.

### 2. 30 NM Airport Overlay & CTAF Comms Frequency Markers (v1.3.44, Build 52)
- **30 NM Radius Spatial Airport Query**:
  - Added `lat` and `lon` fields to `AirportCtafResult` in `CtafLookupHelper.kt`.
  - Implemented `findAirportsWithinRadius(lat, lon, radiusNm = 30.0)` returning all airports within 30 NM sorted ascending by distance from launch point.
  - Added modular `initializeFromJson(jsonText)` for instant loading and unit testing without Android context dependencies.
- **ViewModel & State Management**:
  - Added `nearbyAirports: List<AirportCtafResult>` to `MainUiState`.
  - Automatically populated `nearbyAirports` on GPS acquisition, location updates, and telemetry refresh cycles in `MainViewModel.kt`.
- **High-Contrast Tactical Map Markers (`MapScreen.kt`)**:
  - Implemented custom `createAirportMarkerDrawable(context, ident, freqMhz)` producing a high-contrast aviation pill marker (`✈ $ident $freqMhz`) with deep slate background (`#0F172A`), sky cyan border (`#38BDF8`), and crisp typography.
  - Displayed markers directly over all airports within the 30 NM radius circle.
  - Added "Airports & Comms (30 NM)" layer toggle with cyan accent in the "AIRSPACE LAYERS" floating legend.
- **Interactive Airport Comms Callout Card**:
  - Tapping any airport marker opens an airport detail card positioned above the bottom telemetry bar.
  - Displays Airport Identifier, Airport Name, Primary Comms Frequency (e.g. `120.6 MHz`), Frequency Type badge (`TOWER`, `CTAF`, `UNICOM`), Distance in NM from launch point, GPS coordinates, and traffic monitoring advisory.
  - Sized and padded for glove-friendly touch targets on the DJI RC Pro Enterprise canvas (640 × 360 dp).
- **Automated Verification**:
  - Added `testAirportsWithinRadiusCoronaOntario()` in `DataLayerTest.kt` verifying airports within 30 NM radius are returned with CTAF frequencies, coordinates, and sorted ascending by distance.
  - All 32 unit tests pass green (`./gradlew testDebugUnitTest`).
- **Release Artifacts**:
  - Bumped version in `version.properties` to `VERSION_CODE=52`, `VERSION_NAME=1.3.44`.
  - Generated signed release App Bundle `bundle/UASReady-v1.3.44.aab` and signed release APK `releases/current/UASReady-v1.3.44.apk`.

### 3. Google Play Target API 36 Compliance (v1.3.43, Build 51)
- **Target & Compile SDK 36**: Updated `compileSdk = 36` and `targetSdk = 36` in `app/build.gradle.kts` to satisfy Google Play's requirement for target API level 36.
- **Gradle Properties**: Added `android.suppressUnsupportedCompileSdk=36` to suppress legacy AGP compileSdk warning.
- **Version Bump**: Bumped to `VERSION_CODE=51`, `VERSION_NAME=1.3.43` so Google Play accepts the replacement bundle upload.
- **Artifacts**: Compiled signed release bundle `bundle/UASReady-v1.3.43.aab` and signed release APK `releases/current/UASReady-v1.3.43.apk`.

### 4. Application Bundle Compilation (`.aab`)
- **Bundle Generation**: Compiled signed release Android App Bundle using `./gradlew bundleRelease` with release signing keystore.
- **Dedicated Bundle Directory**: Created `bundle/` folder in repo root with versioned bundle `bundle/UASReady-v1.3.43.aab`.
- **Git Tracking**: Updated `.gitignore` to track release `.aab` bundles in the `bundle/` directory.

### 5. Full Package Refactor to `com.taksolutions.uasready` (v1.3.42, Build 50)
- **Official Application ID & Namespace**: Updated `applicationId = "com.taksolutions.uasready"` and `namespace = "com.taksolutions.uasready"` in `app/build.gradle.kts`.
- **Directory Hierarchy Migration**: Moved all source packages and classes from `app/src/main/java/com/uasready/` to `app/src/main/java/com/taksolutions/uasready/` and test suites to `app/src/test/java/com/taksolutions/uasready/`.
- **Full Codebase Refactor**: Migrated package declarations and import references across all 52 Kotlin and XML source/test files to `com.taksolutions.uasready`.
- **Automated Verification**: All 31 unit tests pass green under the new package namespace (`./gradlew testDebugUnitTest`).
- **Release Assembly**: Generated signed release APK `releases/current/UASReady-v1.3.42.apk`.

### 6. Non-Surface Class E Airspace Filtering & Acceleration (v1.3.41, Build 49)
- **Excluded Non-Surface Class E**: Filtered out non-surface Class E airspace (such as `CLASS_E5` 700 ft AGL / 1200 ft AGL transition areas and `CLASS_E6` enroute areas) that do not reach down to the surface (0 ft AGL/MSL), preventing non-surface transition layers from cluttering low-altitude UAS telemetry.
- **Retained Surface Class E**: Preserved all surface-based Class E airport designations (`CLASS_E2`, `CLASS_E3`, `CLASS_E4` surface extensions where `lowerVal <= 0.0` or `lowerCode == "SFC"`).
- **Concurrent Async Queries & Geometry Simplification**: Leveraged Kotlin coroutines `async` to query Class Airspace and Special Use Airspace concurrently with `maxAllowableOffset=0.001`, dropping network payload from ~4MB to ~45KB and speeding up queries by >4x.
- **Unit Test Coverage**: Validated against live FAA telemetry for San Francisco (46 controlled sectors) and Ontario/Corona (39 controlled sectors).

### 3. Pure Live FAA Airspace Engine (Purged All Hardcoded Sectors)
- **Zero Hardcoded Sectors**: Completely removed `regionalFallbackSectors`, artificial circle polygons, and hardcoded regional caches.
- **Pure Live Telemetry**: All airspace polygons, classifications, altitudes, and authorizations now derive 100% directly from live FAA OpenData servers (`Class_Airspace` and `Special_Use_Airspace`) without masking testing accuracy.
- **Uncontrolled Default**: When no controlled or special use airspace exists within the 30 NM radius (or offline), cleanly reports pure Class G uncontrolled airspace with empty polygon arrays.

### 4. Nationwide Live FAA Class & Special Use Airspace Integration
- **Official FAA Class Airspace Integration**: Updated `AirspaceRepository.kt` to query the official FAA OpenData `Class_Airspace` FeatureServer across the entire United States via 30 NM bounding box envelope.
- **Official FAA Special Use Airspace (SUA)**: Integrated live queries to FAA `Special_Use_Airspace` FeatureServer for Prohibited, Restricted, Warning, Alert, and Military Operations Area (MOA) boundaries nationwide.
- **MultiPolygon & Sector Boundary Extraction**: Built robust GeoJSON parser for `Polygon` and `MultiPolygon` geometries with altitude MSL floor/ceiling attributes and launch point containment detection.

### 5. Airspace-Only Map, Left-Center Zoom Controls, 30NM Disclaimer, & Local CTAF Detail
- **Removed UAS Facility Maps**: Completely purged UAS Facility Map API queries and grid cell overlays; map rendering and inspection is now 100% focused on pure aeronautical airspace sectors (Class B/C/D/E, Restricted/Prohibited, and MOA/SUA areas).
- **Map Disclaimer**: Added a top-center disclaimer banner stating *"Aeronautical Airspace within 30 NM radius"*.
- **Left-Center Map Zoom Controls**: Positioned dedicated glove-friendly Zoom In (`+`) and Zoom Out (`-`) touch targets at `Alignment.CenterStart` (left center vertically) adhering to DJI RC Pro Enterprise layout constraints. Disabled OS default zoom controls.
- **Local CTAF Detail Item**:
  - Bundled high-performance spatial database of 3,916 US airports with CTAF and Tower frequencies in [airports_ctaf.json](file:///d:/Projects/cursor/UAS_Ready/app/src/main/assets/airports_ctaf.json).
  - Created [CtafLookupHelper.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/taksolutions/uasready/data/repository/CtafLookupHelper.kt) for instant Haversine distance spatial lookups.
  - Displayed local CTAF frequency, nearest airport identifier, and distance on the Home Screen Location metric card and Map Screen bottom telemetry bar.
- **Location Card**: Displayed City, State directly alongside GPS coordinates and local CTAF frequency.

# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.41` (Build 49)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `dev`)

---

## What Has Been Completed

### 1. Non-Surface Class E Airspace Filtering & Acceleration (v1.3.41, Build 49)
- **Excluded Non-Surface Class E**: Filtered out non-surface Class E airspace (such as `CLASS_E5` 700 ft AGL / 1200 ft AGL transition areas and `CLASS_E6` enroute areas) that do not reach down to the surface (0 ft AGL/MSL), preventing non-surface transition layers from cluttering low-altitude UAS telemetry.
- **Retained Surface Class E**: Preserved all surface-based Class E airport designations (`CLASS_E2`, `CLASS_E3`, `CLASS_E4` surface extensions where `lowerVal <= 0.0` or `lowerCode == "SFC"`).
- **Concurrent Async Queries & Geometry Simplification**: Leveraged Kotlin coroutines `async` to query Class Airspace and Special Use Airspace concurrently with `maxAllowableOffset=0.001`, dropping network payload from ~4MB to ~45KB and speeding up queries by >4x.
- **Unit Test Coverage**: Validated against live FAA telemetry for San Francisco (46 controlled sectors) and Ontario/Corona (39 controlled sectors).

### 2. Pure Live FAA Airspace Engine (Purged All Hardcoded Sectors)
- **Zero Hardcoded Sectors**: Completely removed `regionalFallbackSectors`, artificial circle polygons, and hardcoded regional caches.
- **Pure Live Telemetry**: All airspace polygons, classifications, altitudes, and authorizations now derive 100% directly from live FAA OpenData servers (`Class_Airspace` and `Special_Use_Airspace`) without masking testing accuracy.
- **Uncontrolled Default**: When no controlled or special use airspace exists within the 30 NM radius (or offline), cleanly reports pure Class G uncontrolled airspace with empty polygon arrays.

### 3. Nationwide Live FAA Class & Special Use Airspace Integration
- **Official FAA Class Airspace Integration**: Updated `AirspaceRepository.kt` to query the official FAA OpenData `Class_Airspace` FeatureServer across the entire United States via 30 NM bounding box envelope.
- **Official FAA Special Use Airspace (SUA)**: Integrated live queries to FAA `Special_Use_Airspace` FeatureServer for Prohibited, Restricted, Warning, Alert, and Military Operations Area (MOA) boundaries nationwide.
- **MultiPolygon & Sector Boundary Extraction**: Built robust GeoJSON parser for `Polygon` and `MultiPolygon` geometries with altitude MSL floor/ceiling attributes and launch point containment detection.

### 4. Airspace-Only Map, Left-Center Zoom Controls, 30NM Disclaimer, & Local CTAF Detail
- **Removed UAS Facility Maps**: Completely purged UAS Facility Map API queries and grid cell overlays; map rendering and inspection is now 100% focused on pure aeronautical airspace sectors (Class B/C/D/E, Restricted/Prohibited, and MOA/SUA areas).
- **Map Disclaimer**: Added a top-center disclaimer banner stating *"Aeronautical Airspace within 30 NM radius"*.
- **Left-Center Map Zoom Controls**: Positioned dedicated glove-friendly Zoom In (`+`) and Zoom Out (`-`) touch targets at `Alignment.CenterStart` (left center vertically) adhering to DJI RC Pro Enterprise layout constraints. Disabled OS default zoom controls.
- **Local CTAF Detail Item**:
  - Bundled high-performance spatial database of 3,916 US airports with CTAF and Tower frequencies in [airports_ctaf.json](file:///d:/Projects/cursor/UAS_Ready/app/src/main/assets/airports_ctaf.json).
  - Created [CtafLookupHelper.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/repository/CtafLookupHelper.kt) for instant Haversine distance spatial lookups.
  - Displayed local CTAF frequency, nearest airport identifier, and distance on the Home Screen Location metric card and Map Screen bottom telemetry bar.
- **Location Card**: Displayed City, State directly alongside GPS coordinates and local CTAF frequency.

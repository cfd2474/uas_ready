# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.30` (Build 38)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Airspace Legend Category Layer Toggles (On / Off)
- In [MapScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/MapScreen.kt), upgraded the floating legend panel with individual category toggles (Switch / Checkboxes):
  - `Restricted / Prohibited (Red)`
  - `Controlled Airspace (Class B, C, D) (Blue)`
  - `Warning / Surface E (Amber)`
  - `UAS Facility Map (UASFM) Grids (Cyan)`
  - `Special Use / MOA (Orange)`
- Dynamically updates the active map overlay in real time.

### 2. Multi-Layer Polygon Intersection Touch Inspector
- Implemented `MapEventsOverlay` and point-in-polygon ray casting in [MapScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/MapScreen.kt).
- When a user taps anywhere on the map with multiple overlapping polygons (e.g. Class D Controlled Airspace + UAS Facility Map grid + Airport Warning), a structured floating popup displays **all overlapping layers at that touch point**, showing:
  - Color badge & Category
  - Zone name & Ceiling/Floor specifications (e.g. `Surface to 3,300 ft MSL` / `200 ft AGL grid ceiling`)
  - Operational guidance (LAANC authorization mandatory, auto-approval ceilings, or TFR prohibitions).

### 3. UAS Facility Map (UASFM) Grid Overlay
- In [AirspaceRepository.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/repository/AirspaceRepository.kt), added high-fidelity UAS Facility Map grid cell generation across controlled airports (KONT, KRAL, KCNO, KRIV, KFUL, KSNA, KLGB, KLAX, KSAN) and FAA live ArcGIS endpoints.
- Rendered on top of controlled airspace polygons with distinct semi-transparent cyan fill and crisp border lines.

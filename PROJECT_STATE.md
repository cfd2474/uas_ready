# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.3` (Build 11)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. openAIP Airspace REST API Integration
- **openAIP REST Integration**: Live queries to `https://api.core.openaip.net/api/airspaces` with bounding box / coordinate distance parameters.
- **Classification & Geometry**: Parses openAIP GeoJSON airspaces, converting ICAO classes and restriction types (Prohibited/Restricted/Danger, CTR, TMA, Class B/C/D/E) into `AirspaceZone` polygon models with high-fidelity regional fallback.

### 2. Pilot Type Onboarding Modal & Deferred Compliance Check
- **Adaptive Typography**: Fixed layout in `MainActivity.kt` so "Non-licensed Pilot" and subtext dynamically scale and wrap without truncation.
- **Deferred Compliance Evaluation**: Assessment engine stays idle until the pilot selects their certification status on startup.

### 3. Map Gesture Conflict Resolution
- **Disabled Drawer Edge Gestures**: Configured `ModalNavigationDrawer(gesturesEnabled = false, ...)` so panning and zooming the interactive map canvas never triggers the side drawer. The side drawer opens exclusively via explicit menu button click.

### 4. Persistent Standardized Top Status Bar
- Single unified `AviationTopStatusBar` rendered across all pages (**Home**, **Map**, **Audit**, **Checklists**, **Settings**), maintaining persistent visibility of app title, live telemetry status, active aircraft, pilot status, refresh button, and menu FAB.

---

## Decisions Made & Rationale
1. **Gesture Isolation**: Disabling drawer gestures completely isolates map touch events (pan, pinch, double-tap zoom) from the navigation drawer, satisfying device rule §5.
2. **Standardized Header**: Moving the TopAppBar into the root scaffold prevents layout jumps when switching between map, audit, and settings screens.

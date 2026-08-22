# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.5` (Build 13)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Light, Dark, and Auto Theme Mode
- Implemented `AppThemeMode` with `DARK` (high-contrast night ops), `LIGHT` (high-visibility outdoor sunlight), and `AUTO` (follows system theme).
- Added interactive **Theme & Appearance** selector card in Settings.

### 2. Obtaining Status Placeholder
- When compliance status is loading or awaiting pilot certification, an animated high-contrast banner is displayed: *"OBTAINING TELEMETRY & COMPLIANCE EVALUATION..."*.

### 3. Responsive Square Metric Cards Grid
- Converted category metric cards to `SquareMetricCard` rendered in a responsive `LazyVerticalGrid`:
  - **3 columns** in Landscape mode.
  - **2 columns** in Portrait mode.
- Top status banner, overview header, and full assessment button span all columns.

### 4. Pure openAIP Airspace Validation (Corona KAJO & Global)
- Completely purged all synthetic dummy zones (including fake Class D over KAJO).
- Map overlays render strictly live openAIP aeronautical zones (`https://api.core.openaip.net/api/airspaces`). KAJO properly evaluates to uncontrolled Class G airspace.

### 5. Flight Readiness Navigation Reset on Launch
- When the pilot selects their certification on startup, the navigation stack immediately resets to the **Flight Readiness (Home)** screen.

---

## Decisions Made & Rationale
1. **Responsive Grid Layout**: Squarish 3-column landscape grid presents all 8 flight readiness categories simultaneously without extensive vertical scrolling on the 360 dp canvas.
2. **Pure openAIP Integration**: Eliminating all mock elements prevents false-positive warnings over airfields like KAJO and ensures reliable real-world compliance.

# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.8` (Build 16)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Portrait Responsive Top Status Bar
- Automatically splits top bar in portrait mode so `UASREADY`, `LIVE`, aircraft chip, and pilot chip (`PILOT: ...`) are placed across two clean lines without crushing.

### 2. Category Card Direct Jump to Detailed Report
- Tapping any check category card on the Home Screen immediately opens the **Detailed Report** focused/filtered on that specific category.
- Removed bottom "View full assessment audit" button.

### 3. Detailed Report Renaming
- Standardized all navigation menu items, screens, and titles from "Assessment Audit" to **"Detailed Report"**.

### 4. High-Contrast Light Mode Buttons
- Fixed all dark blue / cyan buttons across the app (including CSV import) to strictly use bold white text.

### 5. openAIP Polygon Overlays & Red Map Pin
- Enhanced openAIP GeoJSON polygon coordinate parsing.
- Rendered live airspace boundaries onto the osmdroid `MapView`.
- Replaced default launch icon with a custom Red Map Pin teardrop marker.

### 6. Standard vs Metric Unit Switch UI
- Displays labeled options on both sides: `[ Standard (US) ] --- Toggle --- [ Metric (SI) ]` with active highlight styling.

### 7. 120 Minute Forecast Card Evaluation
- Renamed card to **"120 Minute Forecast"** with dynamic status evaluation (*0–60m failures $\rightarrow$ NO-GO, 60–120m failures $\rightarrow$ CAUTION, clear $\rightarrow$ GO*).

---

## Decisions Made & Rationale
1. **Map Extent Parsing**: Limiting openAIP polygon parsing to the active map view radius prevents latency while ensuring aeronautical overlays render cleanly.
2. **Category Direct Jumps**: Skipping the general audit list and immediately opening the relevant category provides a faster operational workflow for pilots.

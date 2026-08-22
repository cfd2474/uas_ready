# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.1` (Build 9)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. DJI FlySafe Airspace Mapping Engine
- **Spatial Zone Geometry**: Defined `AirspaceZone` and `AirspaceZoneType` covering Restricted (TFR/Prohibited), Authorization (Class B/C/D), Warning (5 NM Airport Buffers), and Altitude Zones (UASFM).
- **Map Overlays**: Rendered multi-colored DJI FlySafe polygons on `MapView`:
  - 🔴 **Restricted Zones (Red)**: `0xFFDA3633` outline, translucent fill (TFRs, strict no-fly).
  - 🔵 **Authorization Zones (Blue)**: `0xFF388BFD` outline, translucent fill (Class B/C/D core).
  - 🟡 **Warning Zones (Amber)**: `0xFFE3B341` outline, translucent fill (5 NM airport vicinity, wildlife).
  - 🔷 **Altitude Zones (Cyan)**: `0xFF00D2FF` outline (UASFM grids).
- **FlySafe Legend**: Floating top-right overlay with zone color codes.

### 2. Active Aircraft in Top Status Bar & Fleet in Settings
- **Top Status Bar Active Craft**: Compact chip (`✈ DJI Mavic 3T`) in the top app bar next to `LIVE` badge.
- **Removed Fleet Card**: Removed standalone fleet profile card from the Home overview.
- **Fleet in Settings**: Integrated aircraft fleet switcher and specifications into `SettingsScreen.kt`, linked to custom aircraft creation.
- **Bottom Navigation**: Streamlined 4-button landscape navigation (**Ready**, **Audit**, **Map**, **Checklists**) optimized for RC Pro Enterprise thumb zones.

### 3. Reordered Home Overview Cards
Cards are ordered sequentially:
1. Location
2. Weather & Wind
3. Airspace & Restrictions
4. **Daylight & Solar** (moved below Airspace)
5. **GNSS Satellites & Geometry** (Satellites Visible)
6. **Space Weather & Geomagnetic (Kp)** (moved below GNSS)
7. Pilot Operating Authority (107 License / Public COA)
8. Flight Forecast Horizon (120 Minutes Forecasted)

### 4. DJI RC Pro Enterprise 640x360 Landscape Optimization
- Adheres to `physical-parameters.md`: high-contrast dark theme, touch targets $\ge 48\text{ dp}$, edge-to-edge full canvas mapping with translucent floating overlays.

---

## Decisions Made & Rationale
1. **Airspace Map Visuals**: Styled exactly to DJI FlySafe visual language (Red for TFR/Restricted, Blue for Controlled Airspace, Amber for Airport Warning, Cyan for Altitude) to provide immediate familiarity to enterprise UAS pilots.
2. **Top Bar Aircraft Chip**: Kept the active aircraft visible on all main screens without consuming vertical screen space on the 360 dp canvas.

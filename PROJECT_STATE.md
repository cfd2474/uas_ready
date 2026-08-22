# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Current Version**: `v1.3.0` (Build 8)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. GNSS Constellation & Terrain Viewshed Engine
- **Multi-Constellation Modeling**: Modeled GPS, GLONASS, Galileo, and BeiDou density based on receiver latitude and elevation.
- **NOAA SWPC Scintillation**: Modeled ionospheric scintillation lock loss as a function of Planetary Kp.
- **Online DEM Terrain Shading Viewshed**: Integrated Open-Meteo 90m SRTM/Copernicus Digital Elevation API across 8 radial compass azimuths at 1.5 km and 3.0 km radii to compute horizon ridge mask angles $\theta(\alpha) = \arctan\left(\frac{\Delta E}{D}\right)$ and solid-angle sky occlusion.
- **Companion Safety Criteria**:
  - **GO**: $\ge 12$ Visible Satellites, $\text{HDOP} \le 1.5$.
  - **CAUTION**: $8\text{--}11$ Visible Satellites, $\text{HDOP } 1.5\text{--}2.5$.
  - **NO-GO**: $\le 7$ Visible Satellites or $\text{HDOP} > 2.5$.

### 2. Pilot-Agnostic Settings & Operating Authority
- Removed pilot names, license numbers, and COA agency registration fields.
- Implemented **Pilot Operating Authority** settings:
  - **107 License**: Cleared for day and night flight operations with aircraft 3 SM anti-collision lighting.
  - **Public COA**: Flight restricted to 30 minutes before civil sunrise to 30 minutes after civil sunset. Night flight is a **hard NO-GO** (no night waiver references).
- Replaced bottom bar pilot tab with top header **Settings gear icon**.

### 3. Flight Forecast Horizon & 120-Minute Tiering
- Verbiage updated to **"120 Minutes Forecasted"**.
- Implemented time-decay safety tiering across hourly sampling intervals:
  - Limit exceedances occurring within **$0 \le T < 60\text{ min}$** $\rightarrow$ 🔴 **NO-GO** (immediate launch prohibited).
  - Limit exceedances occurring within **$60 \le T \le 120\text{ min}$** $\rightarrow$ 🟡 **CAUTION** (short mission $<60\text{ min}$ permitted; return to base before deterioration).

### 4. UI Polish & Verbiage
- Replaced "Satellites Locked" / "Sats Locked" with **"Satellites Visible"** / **"Sats Visible"**.
- Removed testing scenarios from main application; default is live telemetry.
- Cleaned up navigation and deprecated icon imports.

---

## Active & Upcoming Chunks
- [x] **Chunk 1**: Pilot-Agnostic Domain Model & Rule Evaluator Updates.
- [x] **Chunk 2**: 120-Minute Forecast Tiering & UI Refactoring.
- [x] **Chunk 3**: Full Test Suite Validation & Signed Release `v1.3.0`.

---

## Decisions Made & Rationale
1. **Pilot Authority Model**: Replaced individual pilot data classes with `PilotAuthorityType.PART_107` and `PilotAuthorityType.PUBLIC_COA`.
2. **Forecast Window Degradation**: Tiered forecast failures at $T < 60\text{m}$ as NO-GO and $60\text{m} \le T \le 120\text{m}$ as CAUTION to allow immediate tactical flights while warning pilots of impending weather fronts.

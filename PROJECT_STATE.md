# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.27` (Build 35)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Taskbar "LIVE" Telemetry Age Ticker & Stale Threshold (>= 5 min)
- In [MainActivity.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/MainActivity.kt), converted the top taskbar "LIVE" badge into a dynamic elapsed time ticker:
  - Formats real-time elapsed age (e.g. `LIVE • 0m`, `LIVE • 3m`, `LIVE • 7m`).
  - **Green** (`#00E676`) for nominal freshness (`< 5 min`).
  - **Yellow / Caution** (`#FFD600`) when telemetry age reaches `≥ 5 min`.
  - Tappable button to trigger instant manual refresh.

### 2. Automatic Telemetry Refresh (>= 10 min)
- In [MainViewModel.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/viewmodel/MainViewModel.kt), implemented a background telemetry age monitor that continuously verifies data freshness:
  - When telemetry age reaches `≥ 10 minutes`, the safety engine automatically invokes `fetchLiveData()` to pull fresh weather, NOAA space weather, solar ephemeris, and airspace conditions.

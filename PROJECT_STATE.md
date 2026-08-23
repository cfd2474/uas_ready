# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.24` (Build 32)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Startup License Type Picker Label Update
- Updated startup certification selection in [MainActivity.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/MainActivity.kt) and [Pilot.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/domain/model/Pilot.kt) to:
  - **"Non-licensed/Not permitted for night flight"** (with subtitle *"Daylight Window Only"*).

### 2. Comprehensive Documentation & README Refresh
- Overhauled [README.md](file:///d:/Projects/cursor/UAS_Ready/README.md) to document:
  - DJI RC Pro Enterprise landscape canvas optimization (640 × 360 dp).
  - Complete 12-model DJI Enterprise fleet limits from `Reference/DJI_Fleet_Environmental_Limits.xlsx`.
  - 10-step dedicated Emergency Procedures SOP with safety principle banner.
  - Multi-source basemaps (Google Street, Google Topo, Google Hybrid, OSM, OpenTopoMap).
  - Dynamic `+ Add Checklist Item` modal dialog.
  - Pilot authority certification modes and daylight window advisory evaluation.

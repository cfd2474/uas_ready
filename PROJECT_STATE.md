# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.11` (Build 19)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Filtered Manufacturer Dropdown Fleet UI
- Redesigned [AircraftScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/AircraftScreen.kt) with an `ExposedDropdownMenuBox` filter selector.
- Model list remains completely blank until a manufacturer is selected (`DJI Enterprise`, `Autel Robotics`, `Skydio`, `Parrot`, or `Custom Profiles`), avoiding visual clutter on the 360dp landscape screen.

### 2. Comprehensive Enterprise Drone Catalog
- Expanded [Aircraft.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/domain/model/Aircraft.kt) to include complete enterprise models with certified operational envelopes:
  - **DJI Enterprise**: M3T, M3E, M3M, M30T, M30, M350 RTK, M300 RTK, M210 RTK V2, M3TD (Dock 2 Thermal), M3D (Dock 2), Inspire 3, FlyCart 30, Air 3, Mini 4 Pro.
  - **Autel Robotics**: EVO Max 4T, EVO Max 4N, Alpha Enterprise, Titan Heavy Lift, EVO II Dual 640T V3, EVO II Pro Enterprise V3, Dragonfish Standard VTOL.
  - **Skydio**: X10, X10D (Blue UAS), X2D (Blue UAS Thermal), X2E, 2+ Enterprise.
  - **Parrot**: ANAFI USA (Blue UAS), ANAFI USA GOV/MIL, ANAFI Ai (4G LTE), ANAFI Thermal.

---

## Decisions Made & Rationale
1. **Manufacturer Gatekeeper**: Making the model list blank until a manufacturer is selected keeps the screen fast and clean, preventing operators from having to scroll through long lists of irrelevant aircraft.

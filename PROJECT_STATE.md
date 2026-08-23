# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.19` (Build 27)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Production Cleanup of Hardcoded Test Data
- Cleaned [LocationInfo.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/domain/model/LocationInfo.kt) by removing hardcoded test location string (`"Corona, CA (HQ)"`) and replacing default states with dynamic acquiring status (`"Acquiring GPS Fix..."` / `"Searching for satellites..."`) until live device GPS fix and reverse geocoding complete.

### 2. Permitted Daylight Flight Window Reference for Non-Licensed Pilots
- Added exact operating window reference card in [DaylightRuleEvaluator.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/domain/engine/rules/DaylightRuleEvaluator.kt).

### 3. Non-Licensed Pilot Terminology Alignment
- Completely eliminated legacy COA wording in favor of direct Non-licensed Pilot criteria.

# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.20` (Build 28)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Default Day/Night Application Theme to System Auto
- In [Theme.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/theme/Theme.kt) and [MainViewModel.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/viewmodel/MainViewModel.kt), updated the default theme configuration to `AppThemeMode.AUTO` (`isSystemInDarkTheme()`), allowing the app to automatically track the controller's system light/dark mode out of the box while still supporting manual override in Settings.

### 2. Cleaned Test Data
- Removed hardcoded test location string in [LocationInfo.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/domain/model/LocationInfo.kt).

### 3. Non-Licensed Pilot Permitted Daylight Hours Reference
- Added reference card in [DaylightRuleEvaluator.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/domain/engine/rules/DaylightRuleEvaluator.kt) and replaced all COA terminology with Non-licensed Pilot criteria.

# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.16` (Build 24)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. 3-Second Responsive App Splash Page
- Added a dedicated full-screen Splash Page in [MainActivity.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/MainActivity.kt) displaying the official UASReady emblem on a solid dark aviation background (`AviationDarkBackground`).
- Automatically scales and centers with `ContentScale.Fit`, ensuring zero cutoff or distortion across any orientation (landscape or portrait).
- Displays for exactly 3 seconds (`delay(3000L)`) on cold boot before transitioning to the pilot certification flow and main app.

### 2. Direct Fleet Management Navigation
- Direct navigation from Settings to [AircraftScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/AircraftScreen.kt).

### 3. Real-Time Craft Search & Manufacturer Filter
- Manufacturer dropdown alongside live "contains"-style search field.

### 4. Clean NO-POIs Basemaps
- Google Street NO-POIs, Terrain NO-POIs, and Hybrid NO-POIs in [MapScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/MapScreen.kt).

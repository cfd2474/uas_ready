# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.14` (Build 22)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Real-Time Aircraft Search & Manufacturer Dropdown
- Upgraded [AircraftScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/AircraftScreen.kt) with side-by-side controls:
  - **Manufacturer Dropdown**: `All Manufacturers`, `DJI Enterprise`, `Autel Robotics`, `Skydio`, `Parrot`, `Custom Profiles`.
  - **Live Search Bar**: Real-time "contains"-style search field (`searchQuery`) with clear button that instantly filters model names, display names, and notes as the operator types.

### 2. Updated Official Full-Bleed App Icon
- Regenerated all Android launcher icon mipmaps and `drawable/app_logo.png` from the latest full-bleed official UASReady artwork.

### 3. Clean NO-POIs Basemaps
- High-resolution Google Street NO-POIs, Terrain NO-POIs, and Hybrid NO-POIs with instant switching in [MapScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/MapScreen.kt).

# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.13` (Build 21)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Clean NO-POIs Basemaps (Street, Topo, Hybrid)
- Configured clean, uncluttered Google styled basemaps with point-of-interest label filtering (`apistyle=s.t:2|s.e:l|p.v:off`) in [MapScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/MapScreen.kt):
  - **Street**: `Google Street NO-POIs` (`lyrs=m`)
  - **Topo**: `Google Terrain NO-POIs` (`lyrs=p`)
  - **Hybrid**: `Google Hybrid NO-POIs` (`lyrs=y`)
- Supports zoom levels 0 through 21 with multi-server load balancing.

### 2. Official App Launcher Icon Integration
- Full mipmap density icons generated and registered in manifest.

### 3. Filtered Dropdown Enterprise Fleet Catalog
- Filtered dropdown with complete catalogs for DJI Enterprise, Autel Robotics, Skydio, and Parrot.

# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.17` (Build 25)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Local Device Time Solar Ephemeris & Night Flight Timing
- Fixed the solar calculation engine in [SolarRepository.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/repository/SolarRepository.kt) to compute solar noon, sunrise, sunset, and civil twilight using the device's local calendar date and local timezone offset (`localCal.timeZone.getOffset(dateEpochMs)`).
- Resolved the timezone rollover issue where evaluation after 17:00 local time in US timezones (which maps to >00:00 UTC next day) previously computed tomorrow's sunrise/sunset epochs, incorrectly reporting a night flight violation during broad daylight.
- Verified with unit tests in [DataLayerTest.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/test/java/com/uasready/data/repository/DataLayerTest.kt) for Corona, CA (zip 92882) at 17:07 local time.

### 2. 3-Second Responsive Dark Splash Screen
- Integrated the official UASReady emblem on a solid dark canvas with `ContentScale.Fit` in [MainActivity.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/MainActivity.kt).

### 3. Direct Fleet Management Navigation & Search Filter
- Streamlined settings navigation to [AircraftScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/AircraftScreen.kt) with manufacturer dropdown and real-time craft name search filter.

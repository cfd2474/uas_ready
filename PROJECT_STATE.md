# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.18` (Build 26)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Permitted Daylight Flight Window Reference for Non-Licensed Pilots
- In [DaylightRuleEvaluator.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/domain/engine/rules/DaylightRuleEvaluator.kt), added a dedicated reference rule card when operating as a Non-licensed Pilot (`SUN-NONLIC-REF-001`) showing the exact permitted daylight operating window (from 30 minutes before sunrise to 30 minutes after sunset) alongside exact sunrise and sunset times.

### 2. Streamlined Non-Licensed Pilot Terminology (Removed COA Wording)
- In [PilotAuthorityRuleEvaluator.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/domain/engine/rules/PilotAuthorityRuleEvaluator.kt), replaced all instances of "COA", "Public COA Authority", and related acronyms with "Non-licensed Pilot".
- All evaluations and explanations directly explain the 30-minute pre-sunrise and 30-minute post-sunset daylight operating parameters.

### 3. Local Device Time Solar Ephemeris
- Full local timezone offset support in [SolarRepository.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/repository/SolarRepository.kt).

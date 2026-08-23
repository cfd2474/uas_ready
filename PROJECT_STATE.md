# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.29` (Build 37)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Controlled Airspace Detection & Warning (All Non-Class G)
- In [AirspaceRuleEvaluator.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/domain/engine/rules/AirspaceRuleEvaluator.kt), added classification detection for any Non-Class G airspace:
  - If flight location is in any non-Class G airspace (`Class B`, `Class C`, `Class D`, `Class E Surface`, `Class E`, `Special Use`), it generates a **Warning / CAUTION** advisory (`AIR-CTRL-001`).
  - Explanation explicitly advises the pilot: *"Flight location is within [Airspace Class] controlled airspace. Non-Class G airspace requires FAA authorization. Please check official LAANC applications (e.g. Aloft/AirControl, AutoPylot, AirMap) for approval to fly in controlled airspace."*
  - If flight location is inside uncontrolled `Class G` airspace, it evaluates as **GO** (`AIR-CTRL-002: Uncontrolled Class G`).
- Added unit test in [AssessmentEngineTest.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/test/java/com/uasready/domain/engine/AssessmentEngineTest.kt) verifying non-Class G airspace produces a CAUTION warning and official LAANC guidance.

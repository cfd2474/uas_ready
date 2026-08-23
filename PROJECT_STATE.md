# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.28` (Build 36)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Stale Telemetry Age Threshold Updated to >10 Minutes
- In [DataFreshnessRuleEvaluator.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/domain/engine/rules/DataFreshnessRuleEvaluator.kt), updated the stale weather telemetry rule (`DAT-WX-001` / `DAT-WX-002`) threshold from 60 minutes to **10 minutes**:
  - Weather telemetry age `> 10 min` triggers a **CAUTION** advisory (`DAT-WX-001`).
  - Weather telemetry age `≤ 10 min` passes as **GO** (`DAT-WX-002`).
- Updated system documentation in [README.md](file:///d:/Projects/cursor/UAS_Ready/README.md).

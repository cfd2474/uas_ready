# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.31` (Build 39)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Taskbar Label Updated from "LIVE" to "Last update"
- In [MainActivity.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/MainActivity.kt), updated the top taskbar status ticker badge:
  - Label format: `Last update • 0m`, `Last update • 3m`, `Last update • 7m`.
  - Turns **Yellow** at `≥ 5 minutes` and auto-refreshes at `≥ 10 minutes`.
  - Tapping the badge triggers an immediate live refresh.

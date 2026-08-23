# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.23` (Build 31)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. DJI Enterprise Fleet Environmental Limits Alignment (from `Reference\DJI_Fleet_Environmental_Limits.xlsx`)
- Replaced the DJI drone list and all specification limits in [Aircraft.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/domain/model/Aircraft.kt) with the 12 models and exact values from the workbook:
  1. **Matrice 350 RTK**: 12 m/s (26.8 mph) wind, -20°C to 50°C (-4°F to 122°F), IP55, 16,404 ft (2110 props) / 22,966 ft (2112 props).
  2. **Matrice 300 RTK**: 15 m/s (33.6 mph, 12 m/s takeoff/landing), -20°C to 50°C (-4°F to 122°F), IP45, 16,404 ft / 22,966 ft.
  3. **Matrice 400**: 12 m/s (26.8 mph), -20°C to 50°C (-4°F to 122°F), IP55 (100 mm/24h rain), 22,966 ft.
  4. **Matrice 30 / 30T**: 15 m/s (33.6 mph, 12 m/s takeoff/landing), -20°C to 50°C (-4°F to 122°F), IP55, 22,966 ft.
  5. **Matrice 4E / 4T**: 12 m/s (26.8 mph), -10°C to 40°C (14°F to 104°F), None (no IP rating on camera/gimbal), 19,685 ft bare / 13,123 ft with payload.
  6. **Matrice 4D / 4TD**: 12 m/s (26.8 mph), -20°C to 50°C (-4°F to 122°F), IP55, 13,123 ft.
  7. **Mavic 3E / 3T (Enterprise Series)**: 12 m/s (26.8 mph), -10°C to 40°C (14°F to 104°F), None, 19,685 ft.
  8. **Mavic 3TA**: 12 m/s (26.8 mph), -10°C to 40°C (14°F to 104°F), None, 19,685 ft.
  9. **Mavic 3M (Multispectral)**: 12 m/s (26.8 mph), -10°C to 40°C (14°F to 104°F), None, 19,685 ft.
  10. **DJI Mini 4 Pro**: 10.7 m/s (23.9 mph), -10°C to 40°C (14°F to 104°F), None, 13,123 ft.
  11. **DJI Mini 3 Pro**: 10.7 m/s (23.9 mph), -10°C to 40°C (14°F to 104°F), None, 13,123 ft.
  12. **DJI Mini 3**: 10.7 m/s (23.9 mph), -10°C to 40°C (14°F to 104°F), None, 13,123 ft.

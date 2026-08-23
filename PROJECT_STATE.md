# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.26` (Build 34)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Persistent Aircraft Selection Across App Sessions
- Upgraded [AircraftRepository.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/data/repository/AircraftRepository.kt) to `PersistentAircraftRepository` using `SharedPreferences`.
- Automatically persists the active `selected_aircraft_id` whenever an aircraft is selected in Fleet Management, Settings, or First-Time Setup, and restores it on next launch.

### 2. First-Time Setup Flow (Fleet Picker Before Pilot Selection)
- Implemented a two-step initial onboarding setup in [MainActivity.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/MainActivity.kt) and [MainViewModel.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/viewmodel/MainViewModel.kt):
  - **Step 1**: On first launch, the **Fleet Management Setup** popup opens directly, allowing the user to filter by manufacturer, search airframe models, and view certified environmental envelopes.
  - An **"ACCEPT SELECTION & PROCEED"** confirmation button saves the selected aircraft and advances to Step 2.
  - **Step 2**: The **Pilot Certification Selection** popup opens (`Licensed Pilot` vs `Non-licensed/Not permitted for night flight`).
  - Upon selecting certification, the app records initial setup as completed and launches the safety assessment dashboard.

### 3. Add Checklist Item Dialog Checkbox Layout & Alignment
- In [ReferenceScreen.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/uasready/ui/screens/ReferenceScreen.kt), overhauled the "Critical Checklist Item" toggle with a structured `Surface` card, explicit label (`CRITICAL CHECKLIST ITEM`), descriptive subtext (`Mandatory safety verification before launch`), and properly aligned `Checkbox`.
- Added vertical scroll constraints so dialog content is never clipped on 360dp landscape screens.

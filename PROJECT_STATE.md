# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.4` (Build 12)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Pure openAIP Airspace Architecture
- Completely removed all FlySafe references, schemas, and verbiage.
- **Sole Source of Airspace Telemetry**: `openAIP Worldwide Aeronautical Database & API` (`https://docs.openaip.net/`).
- Queries openAIP REST endpoints (`https://api.core.openaip.net/api/airspaces`) dynamically around flight coordinates.
- Map displays pure openAIP classifications (Controlled CTR/TMA, Restricted/Prohibited/Danger, Class E / TMZ / RMZ, Gliding/Special Activity).

### 2. Startup Pilot Onboarding Modal & Deferred Compliance Check
- Popup dynamically formats "Licensed Pilot" vs "Non-licensed Pilot (Daylight Window Only)" without text truncation.
- Safety engine remains paused on app launch until user selects their active profile.

### 3. Isolated Map Canvas Gestures
- Disabled drawer swipe/edge gestures (`gesturesEnabled = false`).
- Map panning and zooming are fully isolated from navigation drawer.

### 4. Unified Persistent Top Status Bar
- Standardized `AviationTopStatusBar` across all screens.

---

## Decisions Made & Rationale
1. **Exclusive openAIP Standardization**: Using openAIP as the sole airspace telemetry source provides open, standardized, worldwide airspace classifications and geometry without vendor-specific dependencies.

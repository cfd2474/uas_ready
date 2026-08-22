# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.6` (Build 14)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. Expanded Side Drawer Compact Layout & Smooth Scrolling
- Redesigned the navigation drawer in `MainActivity.kt` with a streamlined header and compact navigation buttons (`height = 42.dp`).
- Added vertical scroll container (`verticalScroll(rememberScrollState())`) to guarantee that all 5 menu options (**Flight Readiness**, **Assessment Audit**, **Aviation Map (openAIP)**, **Checklists & Emergency**, and **Settings & Fleet**) are always completely visible on the 360 dp landscape canvas.

### 2. Light, Dark, and Auto Theme Mode
- Full support for `DARK` (night ops), `LIGHT` (sunlight readability), and `AUTO` (system default) selectable in Settings.

### 3. Responsive Square Metric Cards Grid
- Home screen operational metrics formatted into square cards in a 3-column (landscape) / 2-column (portrait) responsive grid.

### 4. Pure openAIP Airspace Validation
- Purged all hardcoded legacy mock zones (such as fake KAJO Class D) to strictly display live openAIP aeronautical queries.

---

## Decisions Made & Rationale
1. **Scrollable Compact Drawer**: By making the navigation drawer scrollable and using 42 dp touch-target action surfaces, all items remain accessible regardless of screen orientation or display scaling on the RC Pro Enterprise controller.

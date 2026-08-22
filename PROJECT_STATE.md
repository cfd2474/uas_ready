# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.2` (Build 10)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `main`)

---

## What Has Been Completed

### 1. App Startup Pilot Onboarding Modal Popup
- Modal startup dialog prompts user to select operational status:
  - **Licensed Pilot** (`PART_107`): Cleared for daylight and night operations (with aircraft anti-collision strobe).
  - **Non-licensed Pilot** (`PUBLIC_COA`): Strictly restricted to daylight window (30 min before sunrise to 30 min after sunset). Night flight is a hard NO-GO.
- Side-by-side tap buttons; selection instantly applies to the session and dismisses.

### 2. Side Drawer Navigation & Bottom Bar Removal
- Removed bottom navigation bar to maximize 360 dp vertical screen space on the DJI RC Pro Enterprise.
- Added upper-right Menu FAB button that triggers a modal slide-out side bar:
  - **Flight Readiness (Home)**
  - **Assessment Audit**
  - **Aviation Map (FlySafe)**
  - **Checklists & Emergency**
  - **Settings & Fleet**

### 3. Button-Activated Data Entry Cards in Settings
- Settings converted into 4 interactive category buttons:
  1. **Pilot Operating Authority**
  2. **Aircraft Fleet Management**
  3. **Unit System & Telemetry**
  4. **Authoritative Telemetry Sources**
- Tapping any button opens an interactive data entry card.

### 4. Compact Status Banner
- Reduced vertical footprint of the top GO / NO-GO banner by >50% into a sleek horizontal row layout optimized for 640 × 360 dp landscape canvas.

---

## Decisions Made & Rationale
1. **Vertical Space Optimization**: Removing the bottom bar and compacting the status banner reclaims ~110 dp of vertical screen height, keeping critical telemetry and live views unobstructed.
2. **Onboarding UX**: Startup certification modal ensures every flight session immediately operates under the correct legal and safety constraints without digging through menus.

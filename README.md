# UASReady 🛰️✈️
**Deterministic Preflight Decision-Support & Operational Safety Engine for sUAS**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-brightgreen.svg)](https://developer.android.com)
[![Device Target](https://img.shields.io/badge/Target-DJI%20RC%20Pro%20Enterprise-blue.svg)]()
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Deterministic](https://img.shields.io/badge/Rules%20Engine-100%25%20Deterministic-orange.svg)]()

**UASReady** is a purpose-built tactical and commercial preflight flight-readiness assessment application for small Unmanned Aircraft Systems (sUAS), optimized specifically for the **DJI RC Pro Enterprise** controller (5.5" IPS 1920×1080 fixed landscape canvas). Engineered for public safety agencies, Part 107 remote pilots, and critical infrastructure inspectors, UASReady evaluates live weather, NOAA space weather telemetry, FAA airspace rules, exact solar ephemeris, and aircraft limitations to produce an immediate, explainable **GO / CAUTION / NO-GO** flight safety verdict.

---

## ⚡ Key Capabilities & Features

- **100% Deterministic Safety Engine**: Zero opaque AI hallucinations. Every flight determination is backed by transparent rule evaluations with human-readable rationales and explicit parameter thresholds.
- **Pilot Operating Authority Mode**:
  - **Licensed Pilot (14 CFR Part 107)**: Full day and nighttime operational clearance (with 3 SM anti-collision lighting).
  - **Non-licensed / Not Permitted for Night Flight**: Strict daylight operating window enforcement (30 minutes before sunrise to 30 minutes after sunset). Provides real-time daylight minutes remaining and sunset advisory notices.
- **Dedicated Emergency Procedures SOP**: Complete 10-step emergency protocol covering Return-to-Home (RTH), emergency descent, critical battery procedures, signal loss, obstacle collision, unexpected weather, water avoidance, firmware glitches, emergency evasive regulatory exemptions, and post-incident coordinator inspection.
- **Operational Checklists & Custom Item Modal**: Aviation-standard read-only checklists for Preflight Inspection, Launch Readiness, and Postflight Secure, plus an interactive "+ Add Checklist Item" modal for department-specific SOP additions.
- **Verified DJI Enterprise Fleet Limits**: Built-in manufacturer environmental specifications sourced directly from official technical limits (`Reference/DJI_Fleet_Environmental_Limits.xlsx`), covering:
  - Matrice 350 RTK, Matrice 300 RTK, Matrice 400
  - Matrice 30 / 30T, Matrice 4E / 4T, Matrice 4D / 4TD (Dock 3)
  - Mavic 3E / 3T Enterprise Series, Mavic 3TA, Mavic 3M Multispectral
  - DJI Mini 4 Pro, Mini 3 Pro, Mini 3
  - Plus Autel Robotics, Skydio, and Parrot airframes with custom aircraft configuration support.
- **Fleet Management Filtering**: Manufacturer dropdown filter and real-time search filtering across commercial fleet presets.
- **Multi-Source Basemap & Tactical Map Overlay**:
  - Google Street (No-POIs)
  - Google Terrain / Topographic (No-POIs)
  - Google Hybrid / Satellite (No-POIs)
  - Standard OpenStreetMap and OpenTopoMap
  - Interactive radius range rings (500ft, 1000ft, 0.5 SM, 1.0 SM, 3.0 SM), live coordinate readouts, and terrain elevation profiling.
- **Live Environmental Telemetry**:
  - **Live Weather**: Open-Meteo & NOAA NWS hourly forecasts, sustained wind & gust limits, precipitation, and cloud base ceiling calculations.
  - **Space Weather & Geomagnetic Storms**: NOAA Space Weather Prediction Center (SWPC) live planetary Kp index and G-scale alerts for GPS/GNSS multi-rotor stability.
  - **Astronomical Solar Ephemeris**: Local-device timezone aware ephemeris calculating exact civil dawn, sunrise, sunset, and civil dusk.
  - **Airspace Buffer Evaluation**: Class B/C/D/E surface areas and airport distance buffers.
- **DJI RC Pro Enterprise Landscape Optimization**: Hardcoded for 640 × 360 dp landscape canvas with high-contrast sunlight readability (≥ 7:1 ratio), 56 dp thumb-zone touch targets, and zero gesture conflicts with live mapping surfaces.
- **Day / Night Appearance**: Seamless Dark, Light, and System (Auto) theme support.

---

## 🛡️ Flight Decision Matrix

| Safety Status | Condition | Operator Action |
| :--- | :--- | :--- |
| 🟢 **GO** | All environmental, regulatory, and aircraft limits are fully met. | Proceed with standard preflight checklist & launch. |
| 🟡 **CAUTION** | Marginal conditions (gusts near limit, elevated Kp index, civil twilight, flight window ending near dusk). | Heightened vigilance, review advisory notes, verify abort criteria. |
| 🔴 **NO-GO** | Hard limitation breached (excessive winds, precipitation, airspace violation, night flight without Part 107 license). | **Flight prohibited.** Abort mission or relocate launch zone. |
| ⚪ **DATA UNAVAILABLE** | Network lost or stale telemetry (>10 min). | Acquire connectivity or switch to verified offline scenario drill. |

---

## 📦 Releases & Versioning

All signed production APKs are organized in the `releases/` directory:

```
releases/
├── current/
│   └── UASReady-v1.3.24.apk       # Current active signed release APK
└── archive/
    └── UASReady-v1.3.23.apk       # Historical release APKs labeled by version
```

### Automated Release Workflow
Use the automated PowerShell release engine in `scripts/`:

```powershell
# Bump patch version (e.g. 1.3.23 -> 1.3.24) and generate signed release APK
.\scripts\bump_and_release.ps1 -BumpType patch

# Bump minor version and push directly to GitHub
.\scripts\bump_and_release.ps1 -BumpType minor -GitPush -CommitMessage "Release summary"
```

---

## 🏗️ Architecture & Tech Stack

```
com.uasready/
├── domain/                      # Pure Kotlin Core
│   ├── model/                   # Aircraft, Pilot, Weather, SpaceWeather, Airspace, Assessment, Checklist
│   └── engine/                  # Deterministic Rules Engine & Category Evaluators
│       └── rules/               # Weather, Airspace, SpaceWeather, Daylight, Aircraft, Pilot
├── data/                        # Repository Layer & Telemetry Feed Handlers
│   ├── repository/              # LiveWeather, LiveSpaceWeather, Solar, Airspace, Fleet
│   └── sim/                     # Scenario Field Drills (Simulators)
└── ui/                          # Presentation Layer (Jetpack Compose & Material 3)
    ├── components/              # StatusBanners, MetricCards, AuditCards
    ├── screens/                 # Home, Assessment, Map, Timeline, Fleet, Pilot, Reference, Settings
    ├── navigation/              # Navigation Host & Route Destinations
    └── theme/                   # High-Contrast Public-Safety Color Palette & Typography
```

- **Architecture**: Clean Architecture following SOLID design principles.
- **Language**: Kotlin 1.9.24
- **UI Toolkit**: Jetpack Compose & Material 3 (100% Declarative)
- **Networking**: OkHttp 4.12 & Kotlinx Serialization
- **GIS / Mapping**: OSMDroid 6.1.18 (OpenStreetMap + Google Tile Overlays)
- **Ephemeris Algorithms**: NOAA Solar Calculator Ephemeris (Exact Sunrise, Sunset, Civil Twilight)
- **Unit Testing**: JUnit 4 & Coroutines Test Harness (100% deterministic test coverage)

---

## 🛠️ Building From Source

### Prerequisites
- JDK 17
- Android SDK 34 (Android 14)
- Gradle 8.2+ (or use included `./gradlew`)

### Build Debug APK
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Build Signed Release APK
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Run Unit Tests
```bash
./gradlew test
```

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

Developed with precision for unmanned aviation safety by **Michael Leckliter**.

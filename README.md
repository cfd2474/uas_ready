# UASReady 🛰️✈️
**Deterministic Preflight Decision-Support & Operational Safety Engine for sUAS**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Deterministic](https://img.shields.io/badge/Rules%20Engine-100%25%20Deterministic-orange.svg)]()

**UASReady** is a purpose-built tactical and commercial preflight flight-readiness assessment application for small Unmanned Aircraft Systems (sUAS). Built for public safety agencies, Part 107 remote pilots, and critical infrastructure inspectors, UASReady evaluates live weather, NOAA space weather telemetry, FAA/COA airspace rules, exact solar ephemeris, and aircraft limitations to produce an immediate, explainable **GO / CAUTION / NO-GO** flight safety verdict.

---

## ⚡ Key Highlights

- **100% Deterministic Safety Engine**: Zero opaque AI hallucinations. Every flight determination is backed by transparent rule evaluations with human-readable rationales and explicit parameter thresholds.
- **Connectivity Gating**: If critical live data cannot be retrieved or verified, the system automatically gates the assessment with a `DATA UNAVAILABLE` verdict, preventing false GO determinations in degraded operational environments.
- **FAA Part 107 vs Public-Safety COA/COW Switching**: Instant toggling between standard Part 107 limitations (400 ft AGL, 3 SM visibility, daylight/civil twilight) and Agency COA/COW authorizations (elevated altitude ceilings, waived cloud clearances, emergency night operations).
- **Public-Safety Dark-First Design**: Tactical, high-contrast Material 3 UI engineered for rapid outdoor scanning under intense sunlight or night-vision conditions.
- **Live Environmental Telemetry**:
  - **Live Weather**: Open-Meteo & NOAA NWS hourly forecasts, sustained wind & gust limits, precipitation, and cloud base ceiling calculations.
  - **Space Weather & Geomagnetic Storms**: NOAA Space Weather Prediction Center (SWPC) live planetary Kp index and G-scale alerts for GPS/GNSS multi-rotor stability.
  - **Astronomical Solar Ephemeris**: Exact NOAA solar noon, equation of time, civil twilight, and daylight tracking for night waiver enforcement.
  - **Live Airspace Buffer Evaluation**: Class B/C/D/E surface areas and airport distance buffers.
- **Fleet & Limitation Profiles**: Predefined commercial sUAS specifications (DJI Matrice 350 RTK, M30T, Mavic 3 Enterprise/Thermal, Autel EVO Max 4T, Skydio X10/X2D, Parrot ANAFI USA) plus custom aircraft profile builder.
- **Flight Window Timeline**: 30-minute interval predictive safety assessment across scheduled mission windows.
- **OpenStreetMap Tactical Tactical Viewer**: Launch site coordinate readouts, elevation calculation, radius range rings, and map overlay layers.
- **Tactical Checklists & CSV Importer**: Preflight, Launch, and Postflight safety checklists with custom CSV upload support.

---

## 🛡️ Flight Decision Matrix

| Safety Status | Condition | Operator Action |
| :--- | :--- | :--- |
| 🟢 **GO** | All environmental, regulatory, and aircraft limits are fully met. | Proceed with standard preflight checklist & launch. |
| 🟡 **CAUTION** | Marginal conditions (gusts near limit, elevated Kp index, civil twilight). | Heightened vigilance, review advisory notes, verify abort criteria. |
| 🔴 **NO-GO** | Hard limitation breached (excessive winds, precipitation, airspace violation, night without waiver). | **Flight prohibited.** Abort mission or relocate launch zone. |
| ⚪ **DATA UNAVAILABLE** | Network lost or stale telemetry (>60 min). | Acquire connectivity or switch to verified offline scenario drill. |

---

## 📱 App Footer & Dynamic Versioning

The application features a pinned status line in the footer across all navigation screens showing the build index and version:

```
UAS READY // FLIGHT READINESS                v1.0.0 (Build 1)
```

Each new release automatically increments the build index and version number, keeping field operators informed of their exact client build.

---

## 📦 Releases

All signed production APKs are organized in the `releases/` directory:

```
releases/
├── current/
│   └── UASReady-v1.0.0.apk       # Current active signed release APK
└── archive/
    └── UASReady-v0.9.0.apk       # Historical release APKs labeled by version
```

### Automated Release Workflow
Use the automated PowerShell release engine in `scripts/`:

```powershell
# Bump patch version (1.0.0 -> 1.0.1) and generate signed release APK
.\scripts\bump_and_release.ps1 -BumpType patch

# Bump minor version (1.0.0 -> 1.1.0) and push directly to GitHub
.\scripts\bump_and_release.ps1 -BumpType minor -GitPush
```

---

## 🏗️ Architecture & Tech Stack

```
com.uasready/
├── domain/                      # Pure Kotlin Core
│   ├── model/                   # Aircraft, Pilot, Weather, SpaceWeather, Airspace, Assessment
│   └── engine/                  # Deterministic Rules Engine & Category Evaluators
│       └── rules/               # Weather, Airspace, SpaceWeather, Daylight, Aircraft, Pilot
├── data/                        # Repository Layer & Telemetry Feed Handlers
│   ├── repository/              # LiveWeather, LiveSpaceWeather, Solar, Airspace, Fleet
│   └── scenario/                # 10 Scenario Field Drills (Simulators)
└── ui/                          # Presentation Layer (Jetpack Compose & Material 3)
    ├── components/              # StatusBanners, MetricCards, AuditCards
    ├── screens/                 # Home, Assessment, Map, Timeline, Fleet, Pilot, Checklists, Settings
    ├── navigation/              # Navigation Host & Route Destinations
    └── theme/                   # High-Contrast Public-Safety Color Palette & Typography
```

- **Architecture**: Clean Architecture following SOLID design principles.
- **Language**: Kotlin 1.9.24
- **UI Toolkit**: Jetpack Compose & Material 3 (100% Declarative)
- **Networking**: OkHttp 4.12 & Kotlinx Serialization
- **GIS / Mapping**: OSMDroid 6.1.18 (OpenStreetMap)
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

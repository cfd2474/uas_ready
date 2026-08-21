# UASReady — Android Application Generation Prompt

Build a production-quality Android application named **UASReady**.

## Product purpose

UASReady is a **preflight decision-support and operational readiness application for small Unmanned Aircraft Systems (sUAS)**.

The primary purpose is to answer one question:

> **"Can I safely and legally conduct this UAS flight under the current and forecast conditions?"**

UASReady is **not** a flight controller, flight log, mission planner, or click-through checklist application.

It is a **reference and assessment tool** that gathers live environmental, astronomical, space-weather, airspace, aircraft, and pilot information and evaluates those conditions using a transparent rules engine.

The primary output must always be one of:

- 🟢 **GO**
- 🟡 **CAUTION**
- 🔴 **NO-GO**

The application should prioritize **live data** and should require an active internet connection to perform a flight assessment. Do not implement an offline/cached-data GO mode.

The initial target organization is a **single public-safety organization**, but the architecture should be clean and modular.

---

# Platform

Build for:

- Android phones
- Android tablets
- Portrait orientation
- Landscape orientation
- Modern Android versions
- Material 3 design principles

The UI must be responsive and adapt appropriately to phone and tablet screen sizes.

Use a professional **public-safety / aviation hybrid interface**.

Do not make it look like a consumer weather application.

Prioritize:

- High contrast
- Large status indicators
- Extremely clear warnings
- Fast information recognition
- Minimal unnecessary animation
- Excellent usability outdoors
- Usability while wearing gloves where practical
- Dark-mode-first design

The application name displayed throughout the application is:

**UASReady**

Do not add a tagline unless necessary.

---

# Core user workflow

The primary workflow should be:

1. Launch UASReady
2. Confirm or select flight location
3. Select aircraft
4. Select pilot operating authority
5. Select pilot
6. Define planned flight window
7. Retrieve current/live data
8. Evaluate current conditions
9. Evaluate forecast conditions throughout the flight window
10. Run the rules engine
11. Display GO / CAUTION / NO-GO
12. Allow the user to inspect exactly why the result was reached

The application should make the final assessment visible within seconds.

---

# HOME / FLIGHT READINESS SCREEN

The primary screen should be extremely simple.

Example:

```text
UASREADY

FLIGHT READINESS

              GO
         ALL CRITERIA MET

Location
Corona, CA

Weather
82°F
Wind 8 MPH
Gusts 14 MPH

Airspace
No active restrictions

Space Weather
Kp 3
Normal GNSS conditions

Daylight
Sunset 19:42
4h 51m remaining

Aircraft
DJI Mavic 3 Thermal

Pilot
Part 107

Flight Window
14:00 — 16:00

VIEW FULL ASSESSMENT
```

The primary GO / CAUTION / NO-GO indicator should dominate the screen.

Use the following semantic hierarchy:

### GO

All required criteria are satisfied.

### CAUTION

The flight may be conducted, but one or more conditions require awareness or additional consideration.

### NO-GO

One or more hard safety or regulatory criteria are not satisfied.

Do not use a numerical "risk score" as the primary assessment.

The assessment must be understandable without interpreting an arbitrary score.

---

# TRANSPARENT RULES ENGINE

The most important technical component is a **transparent, deterministic rules engine**.

Do not use an opaque AI model to determine GO/NO-GO.

Every result must be generated from explicit rules.

Each rule should have:

- Rule ID
- Category
- Description
- Data source
- Input value
- Threshold
- Applicable aircraft
- Applicable pilot authority
- Result
- Severity
- Human-readable explanation

Example:

```text
Rule: WIND-001

Aircraft:
DJI Mavic 3 Thermal

Parameter:
Wind gust

Current:
29 MPH

Aircraft limit:
34 MPH

Result:
CAUTION

Explanation:
Forecast wind gusts are approaching the aircraft's maximum
operating wind speed.
```

The assessment engine should evaluate both **current conditions and the entire planned flight window**.

The worst applicable condition during the flight window should influence the final assessment.

---

# ASSESSMENT CATEGORIES

Create separate assessment modules for:

1. Weather
2. Aircraft limitations
3. Space weather / GNSS
4. Airspace
5. FAA regulatory information
6. Sunrise / sunset / civil twilight
7. Pilot operating authority
8. Pilot qualifications
9. Flight window
10. Data freshness / availability

Each category should independently return:

- GO
- CAUTION
- NO-GO
- UNKNOWN

However, because connectivity is mandatory, critical unavailable data should prevent the application from falsely declaring GO.

---

# WEATHER

Retrieve all relevant weather information that could affect sUAS operations.

Include, where available:

- Temperature
- Apparent temperature
- Wind speed
- Wind gust
- Wind direction
- Visibility
- Cloud cover
- Cloud ceiling
- Precipitation
- Precipitation probability
- Precipitation intensity
- Relative humidity
- Pressure
- Weather conditions
- Severe weather alerts
- Thunderstorm information
- Hourly forecast
- Relevant forecast warnings

The weather module must evaluate:

### Current conditions

AND

### Forecast conditions

throughout the planned flight window.

Do not simply show weather.

**Interpret it against the selected aircraft's operating limitations.**

Example:

```text
WIND

Current:
18 MPH

Maximum aircraft wind:
34 MPH

Forecast maximum:
31 MPH

STATUS
CAUTION

Forecast gusts approach the aircraft's
maximum operating wind speed.
```

---

# AIRCRAFT PROFILES

Implement an aircraft database.

Include predefined profiles for common commercial sUAS.

Initially include profiles for:

- DJI Mavic 3 Enterprise
- DJI Mavic 3 Thermal
- DJI Mavic 3E
- DJI Matrice 30
- DJI Matrice 30T
- DJI Matrice 350 RTK
- DJI Matrice 300 RTK
- Autel EVO Max 4T
- Skydio X10
- Skydio X2D
- Parrot Anafi USA

The exact specifications should be populated from authoritative manufacturer documentation/data sources where possible.

Aircraft profiles must include at minimum:

- Manufacturer
- Model
- Maximum wind
- Maximum gust if applicable
- Minimum operating temperature
- Maximum operating temperature
- Maximum operating altitude
- Maximum takeoff altitude where applicable
- Precipitation/water limitations
- GNSS requirements
- Night-operation capability
- Relevant battery limitations
- Other manufacturer environmental limitations

The application must compare environmental conditions against these values automatically.

---

# CUSTOM AIRCRAFT

Allow the user to create a custom aircraft based on an existing preset.

For example:

```text
DJI Mavic 3 Thermal
        ↓
CREATE CUSTOM AIRCRAFT
        ↓
Corona Fire Mavic 3T #4
```

The custom aircraft should allow editing of applicable operational parameters.

Do not require the user to manually enter a new aircraft from scratch if a commercial preset already exists.

---

# PILOT OPERATING AUTHORITY

The pilot profile must distinguish between:

### Part 107

and

### COA/COW

The rules engine must apply different operational criteria depending on the selected authority.

Structure the architecture so the rules for each authority are independent.

For example:

```text
Pilot Authority
    ├── Part 107
    │     ├── Applicable operational rules
    │     ├── Night requirements
    │     ├── Airspace requirements
    │     └── Pilot requirements
    │
    └── COA/COW
          ├── Applicable operational rules
          ├── Agency requirements
          ├── Airspace requirements
          └── Pilot requirements
```

Do not mix the two regulatory frameworks.

---

# PILOT INFORMATION

The application should maintain a basic pilot profile.

Track only information relevant to flight readiness.

Include:

- Pilot name
- Operating authority
- Part 107 status where applicable
- Recurrent training status
- COA/COW qualification status where applicable
- Night qualification where applicable
- Other applicable agency qualification information

The system should determine whether the pilot satisfies the applicable requirements.

Example:

```text
PILOT

Mike

Authority:
Part 107

Part 107:
CURRENT

Night:
CURRENT

STATUS:
GO
```

---

# LOCATION

Use the device's GPS to automatically determine the initial flight location.

The user must also be able to:

- Pan the map
- Zoom
- Select another location
- Drop a location marker
- Enter coordinates manually
- Return to current GPS location

The selected location must drive:

- Weather
- Airspace
- Airports
- TFRs
- NOTAMs
- Sunrise
- Sunset
- Civil twilight
- Space weather considerations
- Other geographically relevant data

Display:

- Latitude
- Longitude
- Elevation where available
- Human-readable location

---

# MAP

Provide a professional interactive map.

Support appropriate layers where data is available:

- Standard map
- Satellite
- Airspace
- Airports
- UAS Facility Maps
- TFRs
- Relevant aviation restrictions
- Weather radar
- Wind information

The map should clearly show the selected launch location.

Do not overwhelm the initial screen with every layer. Make layers available through a map layer control.

---

# FAA / AIRSPACE INFORMATION

Use appropriate live aviation data sources.

Retrieve relevant:

- TFRs
- NOTAMs
- Controlled airspace
- Airports
- Special-use airspace
- UAS Facility Maps
- Other relevant FAA restrictions
- LAANC-related information where technically appropriate

The application-generation system should select appropriate APIs and authoritative data sources.

Prioritize:

1. FAA/government sources
2. NOAA/government sources
3. Manufacturer/authoritative aviation sources
4. High-quality commercial APIs where necessary

Do not use an unofficial source when an authoritative source is available.

---

# SPACE WEATHER / GNSS

Include:

- Current Kp index
- Forecast Kp
- NOAA space weather alerts
- Geomagnetic storm classification
- GNSS/GPS risk indicator

Provide an operational interpretation rather than merely displaying Kp.

Example:

```text
SPACE WEATHER

Kp 6

CAUTION

Elevated geomagnetic activity may affect
GNSS reliability and positioning accuracy.
```

The threshold values should be implemented as explicit configurable rules in the code.

---

# SUN / LIGHT

Calculate:

- Sunrise
- Sunset
- Civil dawn
- Civil dusk
- Daylight remaining
- Time until sunset
- Current daylight/night status

The primary display should show:

```text
DAYLIGHT

SUNSET
19:42

DAYLIGHT REMAINING
4h 51m
```

If the flight window extends into darkness, evaluate that against the selected pilot authority and aircraft capabilities.

---

# FLIGHT WINDOW

The user must specify:

**Start time**

and

**End time**

Example:

```text
FLIGHT WINDOW

14:00
to
16:00
```

The rules engine must evaluate forecast conditions at intervals throughout the entire flight window.

Example:

```text
14:00   GO
15:00   GO
16:00   CAUTION

OVERALL
CAUTION

Reason:
Forecast gusts increase above the caution threshold
near the end of the planned flight window.
```

The application must not evaluate only the current weather and assume that conditions will remain constant.

---

# WARNING TYPES

Every warning should be classified.

### ENVIRONMENTAL

Examples:

- High wind
- High gusts
- Extreme temperature
- Precipitation
- Low visibility
- Low cloud ceiling
- Thunderstorm

### REGULATORY

Examples:

- TFR
- Controlled airspace
- NOTAM
- Pilot authority limitation
- Night restriction

### OPERATIONAL

Examples:

- Daylight ending
- GNSS risk
- Forecast conditions deteriorating
- Aircraft approaching environmental limit

Each warning should show:

- Severity
- Current value
- Applicable threshold
- Explanation
- Data source
- Data age

---

# DATA FRESHNESS

Every live data category should expose its last update time.

Example:

```text
WEATHER
Updated 2 minutes ago

FAA
Updated 7 minutes ago

SPACE WEATHER
Updated 4 minutes ago
```

If a source is stale:

```text
⚠ DATA STALE

FAA data has not updated recently.

Assessment reliability may be reduced.
```

Critical data unavailable because of connectivity should prevent a false GO determination.

The application requires connectivity for flight assessment.

Do not allow cached data to silently substitute for live data.

---

# REFERENCE CHECKLISTS

Include preconfigured reference checklists.

Initial categories:

### Aircraft Preflight

- Aircraft condition
- Propellers
- Batteries
- Controller
- SD card
- Firmware
- GPS
- Compass
- Home point
- RTH altitude
- Payload

### Launch

- Area clear
- Personnel clear
- GPS acquired
- Home point confirmed
- RTH confirmed
- Airspace confirmed

### Postflight

- Aircraft inspection
- Battery condition
- Damage
- Data download

These are **READ-ONLY reference checklists**.

Do not make them a required click-through workflow.

The user should be able to view them but does not need to check individual boxes.

---

# CSV IMPORT

Allow additional reference checklists to be imported through CSV.

The imported information should be stored as additional read-only reference material.

Provide a clear CSV format/documentation.

Do not allow imported checklist information to silently modify the rules engine.

Checklist/reference content and safety decision logic must remain separate.

---

# DATA ARCHITECTURE

Design the application around modular data models.

Suggested primary models:

```text
Aircraft
AircraftLimitations
Pilot
PilotAuthority
PilotQualification
Assessment
AssessmentResult
AssessmentRule
WeatherObservation
WeatherForecast
SpaceWeather
AirspaceRestriction
AviationNotice
SunData
FlightWindow
Location
ReferenceChecklist
ChecklistItem
DataSource
```

Keep the rules engine independent from the UI.

The UI should consume assessment results rather than implement decision logic itself.

---

# ASSESSMENT ENGINE

Create a dedicated assessment engine.

Conceptually:

```text
LIVE DATA
   ↓
NORMALIZATION
   ↓
RULE ENGINE
   ↓
CATEGORY RESULTS
   ↓
OVERALL ASSESSMENT
   ↓
GO / CAUTION / NO-GO
```

The engine should produce an explainable result such as:

```text
OVERALL
CAUTION

REASONS

🟢 Weather
Acceptable

🟡 Wind
Forecast gusts approach aircraft limit

🟢 Temperature
Within aircraft limits

🟢 Precipitation
No significant precipitation forecast

🟡 GNSS
Elevated Kp

🟢 Airspace
No active restriction detected

🟢 Pilot
Qualified

🟢 Daylight
Flight window remains within daylight
```

---

# RESULT PRIORITY

Use deterministic severity aggregation.

Suggested priority:

```text
NO-GO
   ↓
CAUTION
   ↓
GO
```

If any hard NO-GO rule applies, the overall result is NO-GO.

If no NO-GO rules apply but one or more CAUTION rules apply, the result is CAUTION.

If all required rules pass, result is GO.

Do not allow an arbitrary weighted score to override a hard rule.

---

# DETAIL SCREEN

When the user taps the overall result, display a complete assessment.

Example:

```text
FLIGHT ASSESSMENT

🔴 NO-GO

WEATHER
🟢 GO

WIND
🔴 NO-GO

29 MPH
Aircraft limit: 25 MPH

PRECIPITATION
🟢 GO

TEMPERATURE
🟢 GO

SPACE WEATHER
🟡 CAUTION

Kp 6

AIRSPACE
🟢 GO

PILOT
🟢 GO

DAYLIGHT
🟢 GO
```

Every individual result must be traceable to an explicit rule.

---

# API / DATA PROVIDERS

The application-generation system should select appropriate APIs automatically.

Strongly prioritize authoritative and live sources.

Use appropriate services for:

- Weather
- Geocoding
- Mapping
- FAA aviation data
- TFRs
- NOTAMs
- Space weather
- Sunrise/sunset
- GPS/device location

Do not hard-code live environmental values.

Use dependency injection or a service abstraction layer so APIs can be replaced later without rewriting the assessment engine.

---

# ERROR HANDLING

Never silently fail.

If an API fails:

```text
WEATHER

⚠ DATA UNAVAILABLE

Unable to retrieve current weather.

Assessment cannot be completed.
```

The application must clearly distinguish:

- GO
- CAUTION
- NO-GO
- DATA UNAVAILABLE

Do not interpret missing information as GO.

---

# DEVICE SENSORS

Use relevant available device sensors where appropriate, including:

- GPS
- Location
- Compass/magnetometer
- Barometer/altimeter if available
- Accelerometer
- Gyroscope

However, do not pretend a phone sensor provides aviation-grade information.

Clearly identify sensor-derived information.

---

# UI DESIGN

Use a modern **public-safety aviation** design.

Dark-mode-first.

High contrast.

Large GO / CAUTION / NO-GO status indicator.

Use restrained color semantics:

- Green = GO
- Yellow/amber = CAUTION
- Red = NO-GO
- Gray = unavailable/unknown

Do not use color alone to communicate status. Pair color with text/icons.

The application should feel appropriate for:

- Fire department UAS operations
- Police UAS operations
- Search and rescue
- Emergency management
- Commercial aviation operations

Avoid making it look like a consumer weather app.

---

# NAVIGATION

Use a simple navigation structure:

### Home
Flight readiness

### Assessment
Detailed GO/CAUTION/NO-GO evaluation

### Map
Location and aviation information

### Aircraft
Select/configure aircraft

### Pilot
Select operating authority and pilot information

### Reference
Read-only operational checklists/reference material

### Settings
Units, API/data preferences, application information

Do not add unnecessary screens.

---

# MVP PRIORITIES

Prioritize functional implementation in this order:

1. Application shell
2. Location/GPS
3. Weather
4. Aircraft profiles
5. Pilot authority
6. Flight window
7. Sunrise/sunset/civil twilight
8. Kp/space weather
9. Airspace/FAA data
10. Transparent rules engine
11. GO/CAUTION/NO-GO dashboard
12. Detailed assessment
13. Map layers
14. Reference checklists
15. CSV import
16. Error/data freshness handling

---

# IMPORTANT PRODUCT PRINCIPLE

UASReady should **not simply display aviation information**.

It should interpret that information.

The user should not have to manually determine:

> "The Mavic 3 Thermal has a 34 MPH wind limit and the forecast calls for 31 MPH gusts."

UASReady should automatically say:

> 🟡 **CAUTION**
>
> Forecast gusts of 31 MPH are approaching the aircraft's 34 MPH maximum wind limit.

Likewise, it should automatically recognize combinations of conditions and provide a clear explanation.

The application should make the pilot's decision **faster, more informed, and more defensible**.

---

# DEVELOPMENT QUALITY

Generate clean, maintainable, production-quality code.

Use:

- Strong separation of concerns
- MVVM or equivalent modern Android architecture
- Repository/service pattern for external data
- Dependency injection
- Local persistence where appropriate
- Unit-testable rules engine
- Unit tests for every assessment rule
- Responsive layouts
- Proper lifecycle handling
- API error handling
- Loading states
- Empty states
- Permission handling
- Location permission handling
- Network availability detection

Most importantly:

**The rules engine must be independently testable without the Android UI or live APIs.**

Create mock data so the application can demonstrate:

- GO scenario
- CAUTION scenario
- NO-GO scenario
- Missing-data scenario
- Deteriorating forecast scenario
- Night-operation scenario
- High-Kp scenario
- Airspace restriction scenario
- Aircraft wind-limit scenario

---

# FINAL DESIGN GOAL

When a UAS pilot opens UASReady, they should immediately be able to answer:

> **Where am I flying?**
>
> **What am I flying?**
>
> **Who is flying?**
>
> **When am I flying?**
>
> **What are the current and forecast conditions?**
>
> **Are there regulatory or environmental restrictions?**
>
> **Can I fly?**

The primary result must be an immediate, explainable, and defensible GO / CAUTION / NO-GO decision.


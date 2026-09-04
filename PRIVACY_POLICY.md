# Privacy Policy for UASReady

**Effective Date:** September 4, 2026  
**Last Updated:** September 4, 2026

This Privacy Policy describes how **Taktical Application and Knowledge Solutions, LLC** ("Company", "we", "us", or "our") collects, uses, processes, and protects your information when you use our mobile application, **UASReady** (the "Application" or "App"), available on the Google Play Store.

We are committed to respecting your privacy and protecting any personal or operational data accessed through your use of the Application.

---

## 1. Company Information & Contact Details

If you have any questions, concerns, or requests regarding this Privacy Policy or our data practices, please contact us at:

- **Entity Name:** Taktical Application and Knowledge Solutions, LLC  
- **Mailing Address:**  
  30 N Gould St Ste R  
  Sheridan, WY 82801-6317  
  United States (US)  
- **Email:** [mike@tak-solutions.com](mailto:mike@tak-solutions.com)  

---

## 2. Information We Access and Collect

UASReady is a tactical preflight flight-readiness assessment tool for unmanned aircraft systems (sUAS). To evaluate flight safety parameters and airspace classifications, the Application accesses specific on-device information:

### A. Location Data (Precise and Approximate Location)
- **Permissions Requested:** `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`.
- **Purpose & Use:** The App accesses your device's geographical coordinates (latitude and longitude) solely to:
  1. Determine local FAA airspace boundaries, controlled sectors (Class B/C/D/E), and Special Use Airspace (SUA) within a 30 nautical mile radius of your launch location.
  2. Query localized surface meteorological data (wind speeds, gusts, temperature, precipitation, and cloud base ceiling).
  3. Calculate localized astronomical solar ephemeris (exact civil dawn, sunrise, sunset, and civil dusk for Part 107 daylight compliance).
  4. Identify the nearest manned airfield to resolve Common Traffic Advisory Frequencies (CTAF) and Tower frequencies.
- **Data Handling:** Location data is processed ephemerally on-device to formulate API requests to authorized public data sources (such as the Federal Aviation Administration and NOAA). Your location coordinates are **never** logged to our servers, sold, shared with advertising networks, or used for behavioral tracking.

### B. User-Generated Configuration Data
- **Aircraft Profiles & Fleet Limits:** Custom aircraft parameters, weight classifications, wind limits, and temperature thresholds that you manually enter.
- **Custom Checklist Items:** Checklist items or operational notes created by the user.
- **Data Handling:** All user-generated configurations are stored locally on your device in secure application preferences. They are not uploaded to or synchronized with external proprietary servers.

### C. Network & Telemetry Queries
To produce safety evaluations, the App connects to public, official government and scientific data providers over secure HTTPS:
- Federal Aviation Administration (FAA) OpenData Aeronautical Services
- National Oceanic and Atmospheric Administration (NOAA) Space Weather Prediction Center
- Open-Meteo Weather APIs
- National Weather Service (NWS)

These queries transmit only the bounding box coordinates or geographical point required to retrieve aeronautical and atmospheric telemetry. No personally identifiable information (PII) is transmitted in these requests.

---

## 3. Information We Do NOT Collect

We believe in strict data minimization. UASReady does **not**:
- Require you to create an account, register, or provide your name, phone number, or email address to use the App.
- Collect advertising identifiers (AAID / GAID) or device fingerprinting data.
- Include third-party tracking beacons, marketing trackers, or commercial advertising SDKs.
- Track your location in the background when the Application is closed or not in active use.
- Sell, rent, trade, or monetize any user data.

---

## 4. How We Use and Disclose Information

We do not disclose, sell, or transfer your information to third parties, except in the following limited circumstances:
- **Service & Telemetry Retrieval:** Transmitting non-identifying spatial coordinates to official weather and airspace APIs to render flight-readiness assessments.
- **Legal Compliance:** If required by applicable law, regulation, subpoena, or legal process issued by a governmental authority with appropriate jurisdiction.

---

## 5. Data Retention & Storage

- **Ephemeral Processing:** Live location telemetry used to calculate airspace intersections and weather is kept only in active device memory during your active session.
- **Local On-Device Storage:** User preferences, selected aircraft, and operational checklist additions reside solely on your physical device.
- **Data Deletion:** You may delete all application data and preferences at any time by clearing the App's storage via your Android device settings or by uninstalling the Application.

---

## 6. Security of Your Information

We prioritize the security of your operational data. All external communications to retrieve aeronautical, airspace, and weather data are conducted over Transport Layer Security (TLS/HTTPS). Because we do not operate a remote user account database, your local settings remain confined to your device's sandboxed operating system environment.

---

## 7. Children's Privacy (COPPA Compliance)

The Application is a commercial and professional tool designed for adult remote pilots and enterprise drone operators. UASReady is not directed to individuals under the age of 13, and we do not knowingly collect personal information from children under 13. If you believe that a child has provided us with personal information, please contact us at [mike@tak-solutions.com](mailto:mike@tak-solutions.com) so we can promptly resolve the matter.

---

## 8. International Data Transfers

The Application is targeted and optimized for flight operations within the United States. If you access the Application from outside the United States, please be aware that any communications with US-based servers (such as NOAA or the FAA) will involve the transfer of query parameters to the United States under standard HTTPS transmission protocols.

---

## 9. Changes to This Privacy Policy

We may update our Privacy Policy periodically to reflect enhancements in the Application or changes in legal requirements. Any modifications will be posted to this page with an updated "Last Updated" date. We encourage users to review this policy periodically.

---

## 10. Contact Us

If you have any questions or comments about this Privacy Policy, please contact:

**Taktical Application and Knowledge Solutions, LLC**  
30 N Gould St Ste R  
Sheridan, WY 82801-6317  
United States (US)  

**Email:** [mike@tak-solutions.com](mailto:mike@tak-solutions.com)  

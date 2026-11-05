# UASReady Release Notes

Newest first. Each release has one `## vX.Y.Z — YYYY-MM-DD` section, written for pilots.
`ReleaseNotesTest` fails if the newest section is not the version in `version.properties`.

## v1.4.0 — 2026-11-05

### Google Play "What's new" (≤ 500 characters)

```
• Live FAA TFR check for your launch point, altitude and flight window
• National-security no-drone zones, incl. the new US–Mexico border zone and the DC FRZ/SFRA
• Never shows "clear" when a source is unreachable: offline cards say "No live data"
• Your pilot type is remembered between launches
• Faster, cleaner airspace map; cards no longer cover map buttons
• More accurate airspace floors and restricted-area guidance
```

### New
- **Temporary Flight Restrictions (TFRs).** UASReady now checks every current FAA TFR against your launch point, your planned altitude and your flight window. Inside an active TFR is NO-GO; a scheduled TFR that is inactive during your window is a caution. TFR areas appear on the map.
- **National-security no-drone zones.** The app checks the FAA's national-security UAS restrictions (14 CFR § 99.7): military and other protected sites, ports of entry, and the 1 NM zone along the US–Mexico border in effect since October 20, 2026. Inside one is NO-GO and the waiver contact is shown; within 1 NM is a caution.
- **Washington DC.** Inside the DC Flight Restricted Zone is NO-GO (TSA/FAA waiver required). Elsewhere in the DC SFRA, the app shows the conditions of FAA NOTAM 5/7224.

### Improved
- **Honest when offline.** Home cards and the map show "No live data" instead of placeholder values, and the header shows "Offline" or "Partial data" instead of a green "Last update".
- **Never "clear" by default.** If the TFR feed or the national-security check can't be reached, the result is "unverified" (caution) — never "no restrictions".
- **Pilot type remembered.** You choose Licensed / Non-licensed once; change it any time in Settings.
- **Airspace accuracy.** Airspace floors defined above sea level are compared with the ground elevation at your launch point. Prohibited areas are NO-GO; restricted areas are a caution with the controlling-agency guidance. Failed airspace lookups are reported as unavailable, never as Class G.
- **Map.** Smoother interaction: layers are only redrawn when their data changes. The airspace and airport cards and the location bar no longer cover the layers and location buttons, and those buttons are larger.

### Sources
- Added to Settings → Government Sources: FAA Temporary Flight Restrictions (tfr.faa.gov) and the FAA UAS Data Delivery System.

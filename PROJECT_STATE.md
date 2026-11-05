# Project State: UAS Ready

## System Overview
- **App Name**: UASReady
- **Package**: `com.taksolutions.uasready`
- **Target Platform**: Android 8.0+ (API 26+)
- **Architecture**: Jetpack Compose + Clean Architecture + MVI/MVVM StateFlow + Deterministic Aviation Safety Engine
- **Target Device Profile**: DJI RC Pro Enterprise (5.5" IPS 1920x1080, fixed landscape canvas 640 × 360 dp)
- **Current Version**: `v1.3.49` (Build 57)
- **Key Store**: `D:\Code\ANDROID\APK Keys\AppSign.jks` (Key: `key0`)
- **Remote Repo**: `https://github.com/cfd2474/UAS_Ready.git` (Branch: `dev`)

---

## What Is In Progress

### Chunk 1 — Secrets & Repo Hygiene (owner approved 2026-11-04)
1. Move release signing credentials out of `app/build.gradle.kts` into gitignored `local.properties` (env-var fallback); debug builds must not need them.
2. Stop tracking release binaries: untrack `releases/**/*.apk` and `bundle/**/*.aab`, update `.gitignore`; release script keeps copying artifacts locally.
3. Back up the full repository (mirror clone outside the working tree) before any rewrite.
4. Rewrite history with `git filter-repo`: strip all `*.apk`/`*.aab` blobs and redact the leaked keystore password from every commit.
5. Verify: debug build + unit tests, repo size, no password string reachable from any ref.
6. Update this file (decisions, operational notes).
7. STOP — owner approval required before force-pushing `dev`, `main`, and tags to GitHub. Owner must rotate the keystore password (it remains retrievable from GitHub PR refs regardless of the rewrite).

**Status: steps 1–6 done locally (2026-11-04). Owner approved step 7 (force-push + delete orphan tags), but the agent's force-push was blocked by the Claude Code permission classifier → owner must run the push commands. Nothing pushed yet.**
- Local history rewritten (all SHAs changed). Repo ~1.53 GB → 180 MB. 0 APK/AAB blobs, 0 commits/trees containing the secret.
- Remaining 167 MB = abandoned `nasr_airspace.db.gz`, reachable ONLY via orphan tags `v1.5.6–v1.5.8`. Dropping those tags → 13 MB. Not done (not approved).
- Pre-rewrite backup: `D:\Projects\cursor\UAS_Ready_backup_2026-11-04.git` (mirror, all 25 refs). Keep until GitHub push is confirmed good.
- Tests: 33/34 pass; the 1 failure is the pre-existing date-dependent test (Chunk 2).

### Chunk 8 — Release v1.4.0 (owner requested 2026-11-05) — DONE (UNSIGNED BUNDLE)
1. Check the keystore opens with the password in `local.properties` (keytool, password via env var, never printed). **Result: wrong password** (`AppSign.jks` modified 2026-10-26) — same blocker as v1.3.50.
2. Release notes `docs/RELEASE_NOTES.md` (user-facing, `## v1.4.0 — date`) + Play "What's new" text (≤ 500 chars) + test that the newest section equals `version.properties`.
3. Check in-app source disclosures and `PRIVACY_POLICY.md` / `STORE_LISTING.md` against the new data sources (tfr.faa.gov, FAA national-security and Boundary_Airspace layers).
4. When the password works: `.\scriptsump_and_release.ps1 -BumpType minor` (no `-GitPush`: it pushes to `main`) → v1.4.0 (Build 58).
5. Verify: `apksigner verify` on APK, versionCode 58 / versionName 1.4.0 in the AAB, release APK smoke test on the emulator.
6. Commit + annotated tag `v1.4.0` on `dev`. Push is blocked until the owner force-pushes the rewritten history (a normal push is non-fast-forward).
7. Update this file; STOP.

**Status (2026-11-05):** steps 1–3 done. Step 1 failed (wrong keystore password), so nothing is bumped or built.
- Step 2: `docs/RELEASE_NOTES.md` (v1.4.0 section + 424-char Play "What's new") and `ReleaseNotesTest` are written but **deliberately uncommitted**: the test fails until `version.properties` says 1.4.0, so they go into the release commit together with the bump.
- Step 3 (committed): Settings → Government Sources lists FAA TFR (tfr.faa.gov) and FAA UAS Data Delivery System (udds-faa.opendata.arcgis.com); `STORE_LISTING.md` lists both and the new capabilities (full description trimmed to 3,922 / 4,000 chars); `PRIVACY_POLICY.md` "Last Updated" Nov 5 2026, purposes include TFR and national-security checks, and states the TFR list is fetched without location. The owner must paste the store text and republish the privacy policy where it is hosted.

- **Outcome (2026-11-05):** the owner confirmed the password in `local.properties` is the one they have; a real `packageRelease` still fails ("keystore password was incorrect"), and `APK Keysbackup-20261026\AppSign.jks` is byte-identical with the same result. Owner chose to go through Play upload-key handling and asked for a bundle only. Steps 4–5 replaced by: version bumped by hand to 1.4.0 / Build 58; `:app:packageReleaseBundle` (the unsigned intermediary bundle, does not trip the fail-fast signing check); repacked deflated (AGP's final sign step normally does this) → `bundle/UASReady-v1.4.0.aab` (15.0 MB, 231 entries, manifest 1.4.0, **no signature**); v1.3.49 bundle moved to `bundle/archive/`. No APK built, no emulator smoke test of a release build. Tests: 119 run, 0 fail, 8 skipped. Commit + tag `v1.4.0` on `dev`, not pushed.
- **Owner must sign before upload:** Play Console rejects unsigned bundles. Either sign with the upload key (`jarsigner -keystore <jks> UASReady-v1.4.0.aab key0`) or, after a Play upload-key reset, with the new upload key.

## Decisions
- **Classification separated from fetching** (2026-11-04, SOLID/SRP): `AirspaceRepository` returns raw `AirspaceSnapshot`; `AirspaceClassifier` (pure) decides relevance/class at the launch point; `CachingAirspaceRepository` decorates the live source.
- **Unknown ground elevation → treat MSL floors as possibly low** (over-report, never under-report) + `AIR-ELEV-001` caution. Rejected: falling back to GPS altitude (code defaults to 206 m when absent; unreliable).
- **Prohibited area = NO-GO; Restricted area = CAUTION** with agency wording (activation schedule unknown to the app). LAANC wording only for B/C/D/E-surface.
- **Airspace cache: ≤ 28 days (NASR cycle), ≤ 5 NM from the cached lookup**, clearly marked; overall verdict still requires live telemetry.
- **Last-known location trusted only ≤ 60 s; fresh-fix wait bounded at 20 s** (was 5 min / unbounded).
- **TFR source = tfr.faa.gov list + detail XML** (2026-11-05). Rejected: the WFS layer (no times or altitudes, drops some areas) and the FAA NOTAM API (needs credentials). Snapshot is all-or-nothing; per-NOTAM details cached by `mod_abs_time`. Ambiguities resolve conservatively: unknown ground → MSL floor treated as surface; non-Daily day codes → active all window; open-ended dates → in effect.
- **Pilot authority persisted** (2026-11-05, owner request): asked once, remembered in SharedPreferences (`uas_ready_pilot_prefs`), changeable in Settings/Pilot screens.
- **Header freshness = live data only** (2026-11-05): age counts from the last refresh that brought live data; OFFLINE red, PARTIAL amber (cached airspace counts as not live). Auto-refresh timing uses the last attempt.
- **Home cards show "No live data"** instead of fallback values when a source is missing.
- **National-security UAS restrictions from FAA layers, not hard-coded geometry** (2026-11-05, chunk 6): `DoD_Mar_13` (99.7 sites incl. border strip) + `Boundary_Airspace` DC SFRA/FRZ. Server point intersection decides "inside" (full resolution, holes respected); simplified envelope geometry only for the 1 NM caution and map. Rejected: bundling a border line (stale when FAA amends) and a 30 NM DCA ring (FAA publishes the real polygons).
- **Border strip widened 0.15 NM** (2026-11-05): FAA polygon ≈ 0.87 NM in CA/AZ vs NOTAM "1NM"; the NOTAM text is the legal definition, so the app errs toward NO-GO.
- **TFR state matching uses NOTAM text states** (2026-11-05): the list's single `state` missed CA, NM, MD, VA.
- **Unverified TFRs are CAUTION, not GO** (2026-11-04). The live app has no TFR feed; it previously showed GO "No TFRs" — a false safety claim (and a Play "misleading claims" risk). Consequence: live assessments can no longer reach overall GO until a real TFR feed is integrated. `AirspaceInfo.tfrDataSourced` defaults to false so nothing claims coverage by accident.
- **Airspace lookup failure → DATA_UNAVAILABLE**, never Class G. Either FAA query failing fails the whole lookup (partial airspace data would understate restrictions).
- **Removed fabricated UASFM 400 ft ceiling** from the live repo (UASFM queries were removed in v1.3.34; the GO "conforms with UASFM ceiling" rule was unbacked).
- **Live-network tests are opt-in** (`-PliveTests`) so the default suite is deterministic.
- **MockWebServer** (test-only) used for repository failure tests; JDK `com.sun.net.httpserver` is not on the Android unit-test compile classpath.
- **Release notes in `docs/RELEASE_NOTES.md`, enforced by `ReleaseNotesTest`** (2026-11-05, CLAUDE.md §9), committed with the version bump.
- **Keystore not rotated; repo to be made private** (owner, 2026-11-05). The leaked password stays valid; the owner accepts that, with the repository private.
- **Keep `version.properties` + `scripts/bump_and_release.ps1`** release flow (owner, 2026-11-04). CLAUDE.md `VERSION`-file convention not adopted.
- **App ships via Google Play Console (public).** `AppSign.jks` is the Play upload key; release builds must stay signed with it.
- **Signing secrets in `local.properties` / env vars only**; release packaging fails fast if absent (an unsigned AAB would be rejected by Play anyway — fail early and clearly).
- **Release binaries not tracked in git** (owner approved cleanup); they live in local `releases/` and `bundle/` and are uploaded to Play.
- **History rewritten with `git filter-repo`** rather than only untracking, so fresh clones are small and the password is gone from branch history.

## Operational notes
- **Test emulator:** AVD `uasready-rcpro` (API 30 google_apis x86_64, 1920×1080 @ 480 dpi landscape = RC Pro 640×360 dp). Start: `emulator -avd uasready-rcpro -port 5620 -no-snapshot -no-boot-anim -no-audio` → serial `emulator-5620`. Other emulators on this machine (5580, 5600) belong to other projects — leave them alone.
- **Mock GPS on emulator:** `adb emu geo fix <lon> <lat>` only reaches the app while it has an active location request, and the app reuses any last-known fix < 5 min old. To move regions: `settings put secure location_mode 0` then `3` (clears last-known), tap Refresh, and keep sending `geo fix` until the app logs `AirspaceRepo: Successfully resolved…`. Driver: `python scripts/survey/emulator_airspace_survey.py [REGION_KEYS...]` (outputs to scripts/survey/out/, gitignored).
- **FAA ArcGIS quota is shared org-wide** — bulk exploration scripts can be throttled by other users' traffic. Check header `x-esri-org-request-units-per-min`; pace queries; point queries are cheap (2 units).
- Emulator driver reads `MainViewModel: Airspace classified: primary=… auth=… specialUse=… zones=… groundFt=… cached=…` (added in chunk 4). Cache test: `adb shell svc wifi disable; svc data disable` → refresh at a previously visited point → `cached=true`; re-enable after.
- Survey: `./gradlew testDebugUnitTest -PliveTests --tests '*AirspaceAccuracySurveyTest*'` → report at `app/build/reports/airspace-survey.md` (~3 min).
- TFR feed logs: `TfrRepo: TFR feed: N current TFRs (M mapped), K details downloaded` and `MainViewModel: TFRs evaluated: active=[…] scheduled=[…] unmappedRegional=[…] …`. TFR detail cache: app files dir `tfr_cache.json`. `pm clear` wipes it (and the saved pilot) → next refresh re-downloads all details.
- National-security logs: `SecurityAirspace: National security: atPoint=[…] nearby=N dc=…` and `MainViewModel: National security evaluated: …` / `National security unverified: …`. Live tests: `./gradlew testDebugUnitTest -PliveTests --tests '*LiveSecurityAirspace*'` (~75 s; waits 65 s on a quota error).
- After `adb install -r` the app is stopped; relaunch with `adb shell monkey -p com.taksolutions.uasready -c android.intent.category.LAUNCHER 1` before driving it. The first refresh after launch uses the previous location — match the expected `state=` in the log before trusting a result.
- Map benchmark: open the map, then `python scripts/perf/map_bench.py LABEL` (gfxinfo frames + `MapOverlays: rebuilt …` counts). gfxinfo varies ±20 points between identical runs on the emulator — compare several runs, not one.
- adb is not on PATH in Git Bash: `/c/Users/Michael/AppData/Local/Android/Sdk/platform-tools/adb.exe`.
- Git Bash + adb: set `MSYS_NO_PATHCONV=1`, or `/sdcard/...` paths are rewritten to `C:/Program Files/Git/sdcard/...`.
- Release signing needs these keys in `local.properties` (gitignored): `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. After rotating the keystore password, update them there.
- `git filter-repo` installed via `python -m pip install --user git-filter-repo`; invoke as `python -m git_filter_repo`. It deletes the `origin` remote; re-add with `git remote add origin https://github.com/cfd2474/UAS_Ready.git`.
- After the rewrite, `dev` has no upstream; first push must be `git push --force-with-lease -u origin dev`.
- `./gradlew testDebugUnitTest | tail` reports exit 0 even on failure (pipe). Check `app/build/test-results/` or the Gradle exit code directly.
- Live FAA tests: `./gradlew testDebugUnitTest -PliveTests` (skipped otherwise).
- `validateSigningRelease` reporting UP-TO-DATE does NOT prove the keystore opens. Only `assembleRelease`/`bundleRelease` actually decrypts the key.
- `bump_and_release.ps1` writes the version bump BEFORE building; if the build fails, revert with `git checkout -- version.properties` and move the archived artifacts back, or the next run skips a version. Its "Previous:" line prints the already-bumped name (cosmetic bug).
- `AppSign.jks` was modified 2026-10-26; the password from the old build file no longer opens it. The `backup-20261026` copy is byte-identical (same failure).
- Unsigned bundle without touching signing config: `.\gradlew.bat :app:packageReleaseBundle` → `app/build/intermediates/intermediary_bundle/release/packageReleaseBundle/intermediary-bundle.aab` (stored uncompressed, ~50 MB; repack deflated). `app/build/outputs/bundle/release/app-release.aab` is only written by a successful signed `bundleRelease` — it may be stale.

### Chunk 2 — Safety & Correctness (owner approved 2026-11-04) — CODE DONE, RELEASE BLOCKED
1. Tests: pin `testCoronaLocal1707IsDaylight` to 2026-08-22 America/Los_Angeles; move the 2 live-FAA tests to a separate class gated by `-PliveTests`.
2. Airspace repo: any FAA query failure (exception / non-200 / unparseable) → `Result.failure` → engine reports DATA_UNAVAILABLE. Never silently Class G. Remove the fabricated `uasFacilityMapMaxAltitudeFt = 400.0` (UASFM lookups were removed in v1.3.34). Close connections.
3. TFR honesty: `AirspaceInfo.tfrDataSourced` (default false). When false, TFR rule is CAUTION "TFRs not verified — check FAA TFR list / B4UFLY" instead of a false GO "No TFRs"; Home card text follows.
4. ViewModel: one refresh job at a time (cancel superseded refresh so stale results can't overwrite newer); run weather/space/airspace/terrain fetches in parallel; move CTAF + SQLite lookups to `Dispatchers.IO`.
5. Unit tests for 2–4 (deterministic, no network).
6. Verify (build + tests), patch release v1.3.50 via `bump_and_release.ps1`, tag, update this file.
7. STOP for owner review. Live TFR feed integration → proposed as its own chunk (needs FAA TFR API research + `docs/PLATFORM_REFERENCE.md` entry).

**Status (2026-11-04):** steps 1–5 done and committed (no version bump). Tests: 41 pass, 2 live-FAA skipped by default; live run (`-PliveTests`) also 2/2 pass.
Step 6 BLOCKED: signed release build fails — "keystore password was incorrect". The password copied from the old build file matches byte-for-byte, but `AppSign.jks` was modified 2026-10-26 (after v1.3.49), so the keystore changed before this session. Half-run release was rolled back (version.properties back to 57 / 1.3.49; v1.3.49 artifacts restored to current). Owner must put the current keystore password in `local.properties`, then run `.\scripts\bump_and_release.ps1 -BumpType patch` → v1.3.50 (Build 58).
Not unit-tested: ViewModel job cancellation (MainViewModel needs an Android Application; no ViewModel test harness yet).

### Chunk 3 — Airspace Accuracy Survey (owner requested 2026-11-04) — DONE, awaiting review
Goal: verify the live FAA airspace source and the app's interpretation of it across US regions, on the JVM and on an emulator. Test + diagnose; fixes are proposed for the next chunk.
1. Record the FAA ArcGIS service contract (fields, codes, limits) in `docs/PLATFORM_REFERENCE.md`, marked verified.
2. Build a region matrix (~25 points): Class B/C/D/E-surface airports coast to coast, Class G rural, P/R areas, FL-floor R areas, Class B shelves over high terrain.
3. Opt-in survey test (`-PliveTests`): run the app's `LiveAirspaceRepository` at each point and compare with an independent truth: FAA server-side point intersection at full resolution + correct floor logic (floor vs ground elevation + 400 ft AGL; FL floors excluded). Write a report.
4. Emulator end-to-end: dedicated AVD (RC Pro profile, 1920×1080 landscape), install debug APK, mock GPS to a subset of regions, capture Airspace card/map + logs; confirm the device matches the JVM result.
5. Update this file + platform reference with findings; STOP with discrepancy report and proposed fixes.
Known before survey (verified against service data 2026-11-04): FL floors are in `LOWER_UOM='FL'` (code checks `LOWER_CODE=="FL"`, never true) → 37 SUA with FL180–FL310 floors treated as low-level. Floors compared as raw MSL, not AGL.

**Results (2026-11-04)** — full report: `docs/airspace-survey-2026-11-04.md`; contract facts: `docs/PLATFORM_REFERENCE.md`.
- JVM survey (`AirspaceAccuracySurveyTest`, opt-in `-PliveTests`): **27/30 agree** with FAA full-resolution truth across West, Northwest, Mountain, Southwest, Plains, Midwest, South, Southeast, Northeast, Gulf, Alaska, Hawaii; all classes B/C/D/E-surface/G/P/R incl. D-under-B-shelf, B shelf ~597 ft AGL, MOA overhead.
- Emulator (AVD `uasready-rcpro`): 17 regions run end-to-end; device results identical to JVM (same class, same polygon counts).
- **3/30 wrong — FL-floor defect confirmed** (R-5002G NJ, R-4008 MD, R-5107F NM): app says SPECIAL_USE + authorization required; truth Class G (floors FL200–FL250). Reproduced on device ("Special Use • LAANC Approval Required").
- High-terrain shelves: 1,115 samples in 10 western cities, none < 400 ft AGL (closest 591). Raw-MSL filter safe today by margin, not by logic.
- **FAA ArcGIS quota is org-wide (6000 units/min shared with all FAA-service users worldwide; baseline 2000–4000 used by others).** Each app lookup costs 2 units, but users can be throttled at any time by others' traffic → "Airspace Unavailable" (pre-chunk-2 code showed Class G).
- `AirspaceAccuracySurveyTest` intentionally FAILS until the FL-floor fix lands (it is the regression test for it).

### Chunk 4 — Airspace Accuracy Fixes (owner approved 2026-11-04) — DONE, awaiting review
Design: repository fetches raw FAA zones (floor + reference kept); pure `AirspaceClassifier` decides relevance/class at the launch point from ground elevation (Open-Meteo DEM via terrain repo) + planned altitude; `CachingAirspaceRepository` decorator serves the last good snapshot (≤28 days, ≤5 NM) when live fails. Unknown ground elevation → MSL floors treated as possibly low (conservative, flagged CAUTION).
Steps: (1) model + classifier with correct floor semantics (FL/SFC=AGL/MSL−ground), exceededTransferLimit = failure; (2) P → NO-GO, R → CAUTION with controlling-agency wording, no LAANC wording for SUA; Home card text; (3) 429 retry once + cache decorator + cached/elevation-unknown cautions; (4) location: last-known ≤60 s, single-update timeout (was unbounded); (5) longitude box scaled by 1/cos(lat) (airspace + warning zones); (6) unit tests, survey 30/30, emulator re-run, state + platform reference.
**Results (2026-11-04):** JVM survey **30/30** (was 27/30); live FAA integration tests 2/2; unit tests 64 pass / 3 live skipped; emulator 16 regions all correct + offline cache verified. Report: `docs/airspace-survey-2026-11-04-postfix.md`.
Behaviour changes a pilot will notice: Prohibited area (e.g. P-56) is now NO-GO (was CAUTION); Restricted area shows controlling-agency guidance, not "LAANC Approval Required"; FL-floor restricted areas no longer flagged; Home card shows "Prohibited Area"/"Restricted Area" and "Cached:" when serving cached airspace.
Pre-existing issues seen during emulator runs (NOT fixed, not in scope): (a) offline, the Weather card still shows plausible values (75°F, Vis 10 SM) under a DATA_UNAVAILABLE badge; (b) header "Last update · 0m" stays green while offline; (c) pilot authority is not persisted — the pilot prompt reappears on every launch; (d) Warren Grove NJ visibility NO-GO looked like real fog on second visit (current 1.2 SM) — likely not a bug.
Not done: polygon holes (inner rings) are still ignored — not observed to matter in 30-point survey; unverified.
Original proposal:
1. Floor semantics per platform reference: `LOWER_UOM='FL'` → value×100 ft MSL; `LOWER_CODE='SFC'` → AGL; `MSL` → minus ground elevation; include airspace only if floor < planned altitude AGL (400). Fixes the FL defect and makes the terrain case correct by logic.
2. Restricted/Prohibited guidance: replace "LAANC Approval Required" (LAANC cannot authorize P/R areas) with controlling-agency guidance.
3. Quota resilience: one retry with backoff on ArcGIS 429; cache last successful airspace per area for the NASR cycle (airspace changes on a 28/56-day cycle) so throttling degrades to "cached, dated" rather than unavailable. Longer term: consider a bundled/periodically-downloaded airspace dataset (like `airport_warning_zones.db`).
4. Launch location staleness: `DeviceLocationManager` reuses any last-known fix < 5 min old — tighten (fresh fix, or last-known only if very recent/accurate).
5. Search box: 0.55° longitude is only ~25 NM at 40°N / ~22 NM at 48°N vs the "30 NM" map claim — scale longitude by 1/cos(lat).
6. Re-run JVM survey + emulator regions; survey must reach 30/30.
Also observed, not investigated: Warren Grove NJ weather card showed NO-GO "forecast visibility 0.2 SM" while current visibility read 10.0 SM — check visibility units/forecast parsing.

### Chunk 5 — Offline Honesty, Pilot Persistence, Live TFR Feed (owner approved 2026-11-05) — DONE, awaiting review
1. Home cards never show invented values: pure presenter for card text; no live data → "No live data" (remove fabricated fallbacks 75°F / Wind 8 MPH / Vis 10 SM / Kp 2.0 / 12+ Sats / HDOP / Full Daylight). Unit tests.
2. Header freshness chip reflects real data: pure `TelemetryFreshness` (LIVE / PARTIAL / OFFLINE) from source results; "last update" time only advances when live data arrived; chip turns amber/red with "Partial"/"Offline". Auto-refresh scheduled on attempt time. Unit tests.
3. Persist pilot authority (SharedPreferences behind `PilotRepository`); skip the start-up prompt when a choice is saved; still changeable in Settings / Pilot screen. Unit tests with a fake store.
4. Live TFR feed: research + verify an official FAA source, record it in `docs/PLATFORM_REFERENCE.md`; `TfrRepository` (fetch/parse) + pure evaluation at the launch point (active in the flight window, floor below planned altitude); TFR rules use it (`tfrDataSourced=true` only when the feed answered). Feed failure → stays "TFRs unverified" CAUTION.
5. Tests: MockWebServer TFR parsing/failure, evaluation, rules; opt-in live test.
6. Emulator check: offline cards/chip, pilot choice survives restart, a point inside a current TFR.
7. Update this file; STOP for owner review (release v1.4.0 still blocked on keystore password).

**Results (2026-11-05):** unit tests 94 pass / 4 live skipped; live TFR test 1/1 (65 notices, 50 mapped; active TFR 6/5156 NH detected at its centroid). Emulator: NH TFR → NO-GO "Class G • ACTIVE TFR"; Kansas → "Class G • No TFRs"; offline → chip "Offline • live data 0m ago" (red), Weather/GNSS/Kp/120-min cards "No live data", Airspace "Cached: Class G • TFRs unverified"; pilot prompt did not reappear after force-stop + relaunch, nor after `install -r`.
Extra fix found during the emulator check: the 120 Minute Forecast card showed "Window Cleared" (GO) with no weather data; it now shows "No live data".
TFR design: list + per-NOTAM detail XML (cached on device by `mod_abs_time`); pure `TfrEvaluator`; mapped TFR areas also appear on the map as RESTRICTED / TFR zones. Details in `docs/PLATFORM_REFERENCE.md` "FAA TFR feed".
Known limits: (a) regional TFRs without a boundary (US–Mexico border TX/AZ, DC 5/7224) give CAUTION statewide in that state, so every TX/AZ/DC assessment is CAUTION while they last. A border-distance check would fix this. (b) 14 CFR 99.7 national-security UAS sites (nationwide NOTAM 6/3366) are not evaluated. The FAA ArcGIS layer `DoD_Mar_13` (2,291 polygons, surface–400 ft) could cover them and is proposed next. (c) First refresh after install downloads ~65 detail files (~37 s on the emulator); later refreshes fetch the list only. (d) Manual location entry has no state code, so unmapped regional TFRs give CAUTION.

**⚠️ Correction found 2026-11-05 (after chunk 5 was committed, before release):** limit (a) is worse than stated. The FAA list gives each border NOTAM a single state: 6/9954 covers CA + AZ but is listed "AZ"; 6/9978 covers AZ + NM + TX but is listed "TX". So commit `7e9e7f1` gives **no caution in California or New Mexico border areas** (San Ysidro, Calexico, Columbus NM could show GO). 5/7224 (DC SFRA, 30 NM ring incl. MD/VA) is listed "DC", so MD/VA pilots inside the SFRA get no caution either. **Do not release `7e9e7f1` without chunk 6.** → Fixed by chunk 6 (text states + FAA layers).

### Chunk 6 — Border & National-Security UAS Restrictions (owner approved 2026-11-05) — DONE, awaiting review
Evidence: the three border NOTAM texts (6/9954 ZLA, 6/9978 ZAB, 6/9956 ZHU) prohibit UAS "WI 1NM INLAND FROM THE U.S. SIDE OF THE U.S./MEXICO BORDER", SFC–400 ft AGL, 2026-10-20 0000 UTC – 2027-10-19 2359 UTC (feed vertical limits wrongly say 0–0). The FAA ArcGIS layer `DoD_Mar_13` ("National Security UAS Flight Restrictions") now holds the three border strips (`20261016-DHS-ZLA-1`, `-ZAB-2`, `-ZHU-3`, ~0.9 NM wide) plus ~2,290 99.7 sites (ports of entry, National Defense Areas, bases).
1. Record the layer contract, the border NOTAM facts, and the list-state discrepancy in `docs/PLATFORM_REFERENCE.md`.
2. `NationalSecurityUasRepository`: envelope query around the launch point (polygons, WGS84), paging, all-or-nothing; HTTP/429 errors → failure.
3. Pure evaluator: launch point inside a site → NO-GO (`AIR-NSUAS-001`, facility + waiver contact); nearby sites → map zones; layer unreachable → CAUTION "national-security sites unverified".
4. Unmapped TFRs: the border notices are covered by the layer's strips (no statewide caution once the layer has answered). DC 5/7224 → caution within the 30 NM SFRA ring around the DCA VOR/DME. For any other unmapped regional notice, take the states from the NOTAM text, not the list's single state.
5. Wire into MainViewModel (parallel fetch, freshness, map); update the GO explanation.
6. Tests: unit + MockWebServer + opt-in live probes (San Ysidro, Nogales, Brownsville = inside; 5 NM inland = outside; Bethesda MD = SFRA caution).
7. Emulator check at the border, inland and at DC; update this file; STOP for review.

**Results (2026-11-05):** unit tests 114 run / 106 pass / 8 live skipped / 0 fail. Live tests (`*LiveSecurityAirspace*`) 4/4: border strip found at San Ysidro, Nogales, El Paso, Brownsville; 0.93 NM point treated as inside; 1.3 NM point → nearby caution; 5 NM inland clear; DCA = FRZ, Gaithersburg = SFRA, 39.25,-77.45 = neither; live border NOTAMs parse text states CA,AZ / AZ,NM,TX / TX.
Emulator: San Ysidro 32.550,-117.03 → NO-GO "Class D • NO-DRONE ZONE" (ZLA-1, + San Ysidro port of entry nearby); Nogales +5 NM → no border caution (was statewide CAUTION); Deming NM → no border caution; Bethesda → NO-GO "Class G • DC FRZ" (plus live security TFR 4/9383). Gaithersburg twice hit the FAA org-wide quota (429) → "Partial data" chip, national security unverified, and the fallback correctly listed 5/7224 for an MD pilot; the SFRA-only card was verified by unit test + live JVM test, not on the emulator.
Found during the work: (1) the FAA border polygons are ≈ 0.87 NM wide in CA/AZ (about one statute mile) while the NOTAMs say 1 NM → border strips are widened by 0.15 NM (NO-GO); recorded as a source disagreement in `docs/PLATFORM_REFERENCE.md`. (2) My first probe points were wrong (an earlier query used a 3 km buffer); the live test caught it.
Design: `ArcGisQueryClient` (shared transport, extracted from `LiveAirspaceRepository`), `LiveSecurityAirspaceRepository` (3 queries, all-or-nothing, not cached), pure `SecurityAirspaceEvaluator`, `SecurityAirspaceRules` (AIR-NSUAS-001..004, AIR-DC-001/002), `UnmappedTfrCoverage` (which unmapped TFRs another layer covers + their states), `AirspaceCardText` presenter. TFR cache entries carry `parserVersion`; v1 entries are re-downloaded once.
Known limits: (a) the FAA quota is org-wide; when other users exhaust it the app shows national-security restrictions as unverified (CAUTION), never clear. (b) Not cached, so offline = unverified. (c) The 1 NM proximity check uses geometry simplified to ~50 m. (d) DC SFRA conditions are shown as text; the app does not check registration/VLOS.

### Chunk 7 — Map Performance (owner approved 2026-11-05) — DONE, awaiting review
Problem: `MapScreen`'s `AndroidView.update` runs on every recomposition (each tap, legend toggle, loading flag) and rebuilds every polygon and airport-marker bitmap; tap listeners capture stale data. Also found: the map's bottom bar shows "12+ Sats Visible / HDOP <= 1.5" with no live data (same invented-value problem fixed on Home in chunk 5).
1. Baseline on the emulator at a dense location (San Diego): `dumpsys gfxinfo` janky-frame % and overlay rebuild count/time while tapping, toggling layers and panning.
2. Pure `AirspaceInspector` (what a tap point is inside) with unit tests; drop MapScreen's duplicate geometry helpers in favour of `GeoMath`.
3. `MapOverlayController` (plain class): owns the osmdroid overlays, rebuilds a layer only when its inputs change, caches marker bitmaps, one tap handler that reads the current data.
4. `MapScreen` delegates to the controller; `update` becomes cheap.
5. Map bottom bar: "No live data" instead of invented GNSS values.
6. Re-measure, emulator check (taps, layer toggles, basemap switch, recenter); tests.
   Added after the step-1 baseline (San Diego, emulator): rebuilds cost 7–13 ms per tap, but **drawing** the airspace polygons dominates — 91 % janky frames (median 25 ms, p95 48 ms) with all layers vs 38 % (13 / 31 ms) with airspace layers off. So step 6 also measures per-frame drawing changes one at a time and keeps only what helps.
7. Update this file; STOP for review.
   Owner request during the chunk (2026-11-05): map cards (inspection, airport) and the location bar must not cover or sit under the layers or location buttons.

**Results (2026-11-05):** unit tests 117 run / 109 pass / 8 live skipped / 0 fail.
- Rebuilds: San Diego (110 overlays, 4,920 vertices): before, every tap/toggle/recomposition rebuilt all overlays (7–13 ms on the UI thread each; 6 in one benchmark run). After: 0 rebuilds while tapping/panning/switching basemaps; a layer toggle rebuilds only that layer (1–5 ms). Taps always inspect current data.
- Drawing (emulator, `scripts/perf/map_bench.py`): polygon **outlines** are the dominant per-frame cost — outlines off: 40 % janky / median 14 ms vs 71–95 % / 20–28 ms with them. Fills, osmdroid downgrade mode, hairline strokes, no anti-aliasing and polyline outlines made no change beyond run-to-run noise (the unchanged build measured 71 % and 95 % in two runs). No drawing change kept. Emulator GPU (host Vulkan) is not the RC Pro's; measure on the device before choosing a drawing fix.
- Layout: right-hand control column (layers + location buttons, 48 dp, 8 dp from the edge); cards and location bar end before it. Root cause of the buttons touching the screen edge: Material enlarges a 34 dp IconButton to its 48 dp minimum touch target, overflowing 7 dp each side. The header's menu button (MainActivity) still touches the right edge — likely the same cause, not changed.
- Map bottom bar: "GNSS: No live data / Needs NOAA space weather" instead of "12+ Sats Visible / HDOP <= 1.5" (code change; not exercised offline on the emulator).
Design: `ui/map/MapOverlayController` (layer ownership, identity-keyed rebuilds, single tap path), `ui/map/BitmapMarkerIcons` (cached icons behind `MarkerIconFactory`), pure `ui/presentation/AirspaceInspector`, `GeoMath.distanceMeters`; MapScreen lost its duplicate geometry helpers (1,096 → ~815 lines).
Open: the inspection card can still cover the basemap selector and the zoom buttons on the left when it has several layers.

---

## What Has Been Completed

### 1. Google Play Store Policy Compliance: Government Sources & Non-Affiliation Disclaimers (v1.3.49, Build 57)
- **Policy Remediation & Store Listing (`STORE_LISTING.md`)**:
  - Authored compliant Google Play Store Listing documentation in `STORE_LISTING.md` including Short Description (79 characters) and Full Description.
  - Added prominent **IMPORTANT DISCLAIMER** at the very beginning of the store listing stating that UASReady is an independent application developed by Taktical Application and Knowledge Solutions, LLC, and does not represent, and is not affiliated with or endorsed by the FAA, NOAA, or any government entity.
  - Added dedicated **SOURCES OF GOVERNMENT INFORMATION** section with official functional `.gov` URLs for FAA Aeronautical Data, FAA Open GIS ArcGIS Services, FAA 14 CFR Part 107 Small UAS Regulations, NOAA Space Weather Prediction Center (SWPC), and National Weather Service (NWS).
- **In-App Attributions & Disclaimer UI (`SettingsScreen.kt`)**:
  - Renamed category to **"Government Sources & Legal Notice"** with `Icons.Default.Gavel`.
  - Added high-contrast, amber-bordered **DISCLAIMER OF NON-OFFICIAL STATUS** card detailing independent status and non-affiliation with FAA/NOAA.
  - Implemented interactive `GovernmentSourceItem` cards providing exact `.gov` links with one-tap web browser launching via Compose `LocalUriHandler.openUri()`.
  - Removed obsolete references to openAIP across `HomeScreen.kt` status indicator, `MainActivity.kt` navigation drawer, and `SettingsScreen.kt`.
- **Automated Verification**:
  - Executed `./gradlew testDebugUnitTest` verifying 100% of tests pass green.
- **Release Archiving Compliance**:
  - Release engine (`bump_and_release.ps1`) generated signed release APK and AAB for `v1.3.49` (Build 57).
  - Archived `v1.3.48` artifacts to `releases/archive/` and `bundle/archive/`.
  - Current release folders strictly hold `releases/current/UASReady-v1.3.49.apk` and `bundle/UASReady-v1.3.49.aab`.

### 2. Airspace Floor Filtering (< 500' Lower Limit Only) (v1.3.48, Build 56)
- **Airspace Floor Filtering Implementation**:
  - Filtered both FAA Class Airspace (B, C, D, E) and Special Use Airspace (SUA: Prohibited, Restricted, MOA, Alert, Warning) to strictly exclude any airspace with a floor/starting altitude of 500 ft or greater (`lowerVal >= 500.0 || lowerCode == "FL"`).
  - Only airspace sectors touching the surface or starting below 500' AGL/MSL are rendered on the map and evaluated for airspace authorization.
  - Correctly treats drone operations below elevated shelves (e.g. Class B sectors starting at 1,500' or 3,000' MSL, Class C outer shelves at 1,200', Class E5 transition areas at 700'/1200' AGL, and high MOAs) as uncontrolled Class G operations without false authorization requirements.
- **UI & Disclaimer Synchronization**:
  - Updated `MapScreen.kt` top disclaimer banner to: `"Surface to 500' Airspace within 30 NM radius"`.
- **Automated Verification**:
  - Added unit test assertions in `DataLayerTest.kt` verifying `airspace.zones.all { it.floorFt < 500.0 }` and `airspace.zones.none { it.floorFt >= 500.0 }` across both San Francisco and Ontario/Corona scenarios.
  - Verified 100% of unit tests pass green (`./gradlew testDebugUnitTest`).
- **Release Archiving Compliance**:
  - Automated release engine (`bump_and_release.ps1`) incremented version to `v1.3.48` (Build 56).
  - Moved `UASReady-v1.3.47.apk` to `releases/archive/` and `UASReady-v1.3.47.aab` to `bundle/archive/`.
  - Current release folders contain strictly active artifacts: `releases/current/UASReady-v1.3.48.apk` and `bundle/UASReady-v1.3.48.aab`.

### 2. High Risk Zone Bowtie Additional 30% Reduction (5,250m Extent) (v1.3.47, Build 55)
- **Bowtie Dimension Optimization**:
  - Reduced total flare length from threshold by an additional 30%: from 7,500m to **5,250 m** (~2.83 NM).
  - Corridor extension past runway threshold reduced from 1,500m to **1,050 m**.
  - Flaring fan expands at 15% divergence from 1,200m corridor out to **2,460 m** full width at the 5,250m boundary.
  - Preserved separate **3,000m (3km)** runway centerline buffer polygon.
- **Database & UI Synchronization**:
  - Recompiled all 9,646 CONUS records into `app/src/main/assets/airport_warning_zones.db`.
  - Updated `MapScreen.kt` inspection card subtitle to reflect *"5.25km extent"*.
- **Automated Verification**:
  - All unit tests pass green (`./gradlew testDebugUnitTest`).
- **Release Archiving Compliance**:
  - Automated release engine (`bump_and_release.ps1`) incremented version to `v1.3.47` (Build 55).
  - Moved `UASReady-v1.3.46.apk` to `releases/archive/` and `UASReady-v1.3.46.aab` to `bundle/archive/`.
  - Current release folders contain strictly active artifacts: `releases/current/UASReady-v1.3.47.apk` and `bundle/UASReady-v1.3.47.aab`.

### 2. High Risk Zone Bowtie (50% Length) & Separate 3km Runway Buffer (v1.3.46, Build 54)
- **Geometry Restructuring**:
  - Removed outer 6km warning polygon and separated the runway buffer from the approach bowtie.
  - **High Risk Zone (Bowtie)**: Reduced total length by 50% (1,200m runway corridor + 1,500m constant corridor + 15% flare to 7,500m total distance from threshold).
  - **Runway Buffer (3km)**: Separate stadium/capsule buffer of 3,000m radius around runway centrelines.
- **Database Recompilation (`airport_warning_zones.db`)**:
  - Rebuilt spatial database for all 4,823 active CONUS airports (9,646 records).
  - Database footprint decreased from 8.82 MB to **5.64 MB** with float32 binary packing and spatial bounding box indexes.
- **Model & Architecture**:
  - Updated `AirportWarningZone.kt` with `zoneType` (`HIGH_RISK_BOWTIE` vs `RUNWAY_BUFFER_3KM`) and `zoneName`.
  - Updated `AirportWarningZoneRepository.kt` to unpack and expose distinct zone types.
- **Tactical Map & UI (`MapScreen.kt`)**:
  - Labeled bowtie zone as **"High Risk Zone"** with `#EE8815` orange accent.
  - Labeled buffer zone as **"Runway Buffer (3km)"** with `#FFCC00` yellow accent.
  - Updated advisory notice on inspection cards: *"Advisory: Airport proximity warning zone, monitor local traffic."*
  - Updated legend toggle to *"Airport Warning / High Risk"*.
- **Automated Verification**:
  - Updated `DataLayerTest.kt` unit tests verifying both `HIGH_RISK_BOWTIE` and `RUNWAY_BUFFER_3KM` for Ontario (`KONT`) and San Francisco (`KSFO`).
  - 100% of unit tests pass green (`./gradlew testDebugUnitTest`).
- **Release Archiving Compliance**:
  - Automated release engine (`bump_and_release.ps1`) incremented version to `v1.3.46` (Build 54).
  - Moved `UASReady-v1.3.45.apk` to `releases/archive/` and `UASReady-v1.3.45.aab` to `bundle/archive/`.
  - Current release folders contain strictly active artifacts: `releases/current/UASReady-v1.3.46.apk` and `bundle/UASReady-v1.3.46.aab`.

### 2. CONUS Airport Warning Zones (DJI GEO 2.0 Bow-Tie & Runway Buffer Overlay) (v1.3.45, Build 53)
- **Geometry Engine Validation & Dataset**:
  - Validated geometry engine against DJI live API fixtures (`validate_geofence.py`): Check A analytic area achieved **0.0 ppm error**; Check B IoU against live published DJI polygons achieved **up to 0.998 IoU**.
  - Generated all 9,646 CONUS warning zones (Enhanced Warning 4,000m buffer + Warning 6,000m buffer with 15km approach bow-tie corridor) from OurAirports data.
- **High-Performance Spatial Database (`airport_warning_zones.db`)**:
  - Bundled indexed SQLite database in `app/src/main/assets/airport_warning_zones.db`.
  - Coordinates stored as binary packed float32 byte buffers for minimum footprint and zero JSON parsing overhead.
  - Spatial bounding box queries for 30 NM radius execute in **0.36 to 0.63 ms**.
- **Domain & Repository Architecture**:
  - Created `AirportWarningZone` model and `AirportWarningZoneRepository` for clean architecture separation and testability.
  - Automatic background queries triggered in `MainViewModel.kt` during GPS acquisition, position updates, and telemetry refresh cycles.
- **Tactical Map Rendering & Multi-Layer Inspection (`MapScreen.kt`)**:
  - Rendered warning zones as translucent polygons adhering to DJI palette: `#EE8815` (Level 3 Enhanced Warning, 4km buffer + 15km bow-tie) and `#FFCC00` (Level 0 Warning, 6km buffer + 15km bow-tie).
  - Added "Airport Warning Zones (DJI)" layer toggle with `#EE8815` orange accent in the "AIRSPACE LAYERS" floating legend.
  - Multi-layer tap inspection card shows combined count of aeronautical airspace sectors and airport warning zones, displaying airport ident/name, level badge, buffer distance, and advisory instructions.
- **Automated Verification**:
  - Added unit tests `testAirportWarningZonesDatabaseQueryCoronaOntario` and `testAirportWarningZonesSanFrancisco` in `DataLayerTest.kt`.
  - Verified 100% of unit tests pass green (`./gradlew testDebugUnitTest`).
- **Release Artifacts & Archiving Compliance**:
  - Bumped version in `version.properties` to `VERSION_CODE=53`, `VERSION_NAME=1.3.45`.
  - Compiled signed release Android App Bundle `bundle/UASReady-v1.3.45.aab` and signed release APK `releases/current/UASReady-v1.3.45.apk`.
  - Enforced release archiving rules: moved historic `v1.3.43` and `v1.3.44` APKs from `releases/current/` to `releases/archive/` (leaving only `v1.3.45` as current), and moved historic AABs to `bundle/archive/`.
  - Updated `scripts/bump_and_release.ps1` to automatically build both `.apk` and `.aab` bundles and archive prior versions on future releases.

### 2. 30 NM Airport Overlay & CTAF Comms Frequency Markers (v1.3.44, Build 52)
- **30 NM Radius Spatial Airport Query**:
  - Added `lat` and `lon` fields to `AirportCtafResult` in `CtafLookupHelper.kt`.
  - Implemented `findAirportsWithinRadius(lat, lon, radiusNm = 30.0)` returning all airports within 30 NM sorted ascending by distance from launch point.
  - Added modular `initializeFromJson(jsonText)` for instant loading and unit testing without Android context dependencies.
- **ViewModel & State Management**:
  - Added `nearbyAirports: List<AirportCtafResult>` to `MainUiState`.
  - Automatically populated `nearbyAirports` on GPS acquisition, location updates, and telemetry refresh cycles in `MainViewModel.kt`.
- **High-Contrast Tactical Map Markers (`MapScreen.kt`)**:
  - Implemented custom `createAirportMarkerDrawable(context, ident, freqMhz)` producing a high-contrast aviation pill marker (`✈ $ident $freqMhz`) with deep slate background (`#0F172A`), sky cyan border (`#38BDF8`), and crisp typography.
  - Displayed markers directly over all airports within the 30 NM radius circle.
  - Added "Airports & Comms (30 NM)" layer toggle with cyan accent in the "AIRSPACE LAYERS" floating legend.
- **Interactive Airport Comms Callout Card**:
  - Tapping any airport marker opens an airport detail card positioned above the bottom telemetry bar.
  - Displays Airport Identifier, Airport Name, Primary Comms Frequency (e.g. `120.6 MHz`), Frequency Type badge (`TOWER`, `CTAF`, `UNICOM`), Distance in NM from launch point, GPS coordinates, and traffic monitoring advisory.
  - Sized and padded for glove-friendly touch targets on the DJI RC Pro Enterprise canvas (640 × 360 dp).
- **Automated Verification**:
  - Added `testAirportsWithinRadiusCoronaOntario()` in `DataLayerTest.kt` verifying airports within 30 NM radius are returned with CTAF frequencies, coordinates, and sorted ascending by distance.
  - All 32 unit tests pass green (`./gradlew testDebugUnitTest`).
- **Release Artifacts**:
  - Bumped version in `version.properties` to `VERSION_CODE=52`, `VERSION_NAME=1.3.44`.
  - Generated signed release App Bundle `bundle/UASReady-v1.3.44.aab` and signed release APK `releases/current/UASReady-v1.3.44.apk`.

### 3. Google Play Target API 36 Compliance (v1.3.43, Build 51)
- **Target & Compile SDK 36**: Updated `compileSdk = 36` and `targetSdk = 36` in `app/build.gradle.kts` to satisfy Google Play's requirement for target API level 36.
- **Gradle Properties**: Added `android.suppressUnsupportedCompileSdk=36` to suppress legacy AGP compileSdk warning.
- **Version Bump**: Bumped to `VERSION_CODE=51`, `VERSION_NAME=1.3.43` so Google Play accepts the replacement bundle upload.
- **Artifacts**: Compiled signed release bundle `bundle/UASReady-v1.3.43.aab` and signed release APK `releases/current/UASReady-v1.3.43.apk`.

### 4. Application Bundle Compilation (`.aab`)
- **Bundle Generation**: Compiled signed release Android App Bundle using `./gradlew bundleRelease` with release signing keystore.
- **Dedicated Bundle Directory**: Created `bundle/` folder in repo root with versioned bundle `bundle/UASReady-v1.3.43.aab`.
- **Git Tracking**: Updated `.gitignore` to track release `.aab` bundles in the `bundle/` directory.

### 5. Full Package Refactor to `com.taksolutions.uasready` (v1.3.42, Build 50)
- **Official Application ID & Namespace**: Updated `applicationId = "com.taksolutions.uasready"` and `namespace = "com.taksolutions.uasready"` in `app/build.gradle.kts`.
- **Directory Hierarchy Migration**: Moved all source packages and classes from `app/src/main/java/com/uasready/` to `app/src/main/java/com/taksolutions/uasready/` and test suites to `app/src/test/java/com/taksolutions/uasready/`.
- **Full Codebase Refactor**: Migrated package declarations and import references across all 52 Kotlin and XML source/test files to `com.taksolutions.uasready`.
- **Automated Verification**: All 31 unit tests pass green under the new package namespace (`./gradlew testDebugUnitTest`).
- **Release Assembly**: Generated signed release APK `releases/current/UASReady-v1.3.42.apk`.

### 6. Non-Surface Class E Airspace Filtering & Acceleration (v1.3.41, Build 49)
- **Excluded Non-Surface Class E**: Filtered out non-surface Class E airspace (such as `CLASS_E5` 700 ft AGL / 1200 ft AGL transition areas and `CLASS_E6` enroute areas) that do not reach down to the surface (0 ft AGL/MSL), preventing non-surface transition layers from cluttering low-altitude UAS telemetry.
- **Retained Surface Class E**: Preserved all surface-based Class E airport designations (`CLASS_E2`, `CLASS_E3`, `CLASS_E4` surface extensions where `lowerVal <= 0.0` or `lowerCode == "SFC"`).
- **Concurrent Async Queries & Geometry Simplification**: Leveraged Kotlin coroutines `async` to query Class Airspace and Special Use Airspace concurrently with `maxAllowableOffset=0.001`, dropping network payload from ~4MB to ~45KB and speeding up queries by >4x.
- **Unit Test Coverage**: Validated against live FAA telemetry for San Francisco (46 controlled sectors) and Ontario/Corona (39 controlled sectors).

### 3. Pure Live FAA Airspace Engine (Purged All Hardcoded Sectors)
- **Zero Hardcoded Sectors**: Completely removed `regionalFallbackSectors`, artificial circle polygons, and hardcoded regional caches.
- **Pure Live Telemetry**: All airspace polygons, classifications, altitudes, and authorizations now derive 100% directly from live FAA OpenData servers (`Class_Airspace` and `Special_Use_Airspace`) without masking testing accuracy.
- **Uncontrolled Default**: When no controlled or special use airspace exists within the 30 NM radius (or offline), cleanly reports pure Class G uncontrolled airspace with empty polygon arrays.

### 4. Nationwide Live FAA Class & Special Use Airspace Integration
- **Official FAA Class Airspace Integration**: Updated `AirspaceRepository.kt` to query the official FAA OpenData `Class_Airspace` FeatureServer across the entire United States via 30 NM bounding box envelope.
- **Official FAA Special Use Airspace (SUA)**: Integrated live queries to FAA `Special_Use_Airspace` FeatureServer for Prohibited, Restricted, Warning, Alert, and Military Operations Area (MOA) boundaries nationwide.
- **MultiPolygon & Sector Boundary Extraction**: Built robust GeoJSON parser for `Polygon` and `MultiPolygon` geometries with altitude MSL floor/ceiling attributes and launch point containment detection.

### 5. Airspace-Only Map, Left-Center Zoom Controls, 30NM Disclaimer, & Local CTAF Detail
- **Removed UAS Facility Maps**: Completely purged UAS Facility Map API queries and grid cell overlays; map rendering and inspection is now 100% focused on pure aeronautical airspace sectors (Class B/C/D/E, Restricted/Prohibited, and MOA/SUA areas).
- **Map Disclaimer**: Added a top-center disclaimer banner stating *"Aeronautical Airspace within 30 NM radius"*.
- **Left-Center Map Zoom Controls**: Positioned dedicated glove-friendly Zoom In (`+`) and Zoom Out (`-`) touch targets at `Alignment.CenterStart` (left center vertically) adhering to DJI RC Pro Enterprise layout constraints. Disabled OS default zoom controls.
- **Local CTAF Detail Item**:
  - Bundled high-performance spatial database of 3,916 US airports with CTAF and Tower frequencies in [airports_ctaf.json](file:///d:/Projects/cursor/UAS_Ready/app/src/main/assets/airports_ctaf.json).
  - Created [CtafLookupHelper.kt](file:///d:/Projects/cursor/UAS_Ready/app/src/main/java/com/taksolutions/uasready/data/repository/CtafLookupHelper.kt) for instant Haversine distance spatial lookups.
  - Displayed local CTAF frequency, nearest airport identifier, and distance on the Home Screen Location metric card and Map Screen bottom telemetry bar.
- **Location Card**: Displayed City, State directly alongside GPS coordinates and local CTAF frequency.

"""Drive the UASReady app on the test emulator across US regions via mock GPS.

For each region: geo fix -> tap Refresh -> wait for the app's AirspaceRepo result log ->
screenshot the Home screen. Writes out/results.json next to this script.

Prerequisites: AVD `uasready-rcpro` running (see PROJECT_STATE.md operational notes), debug APK
installed, first-time setup completed (aircraft + pilot selected) so the Home screen is showing.
Usage: python scripts/survey/emulator_airspace_survey.py [REGION_KEY ...]
"""
import json
import os
import re
import subprocess
import sys
import time

ADB = os.environ.get("ADB", os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk", "platform-tools", "adb.exe"))
SERIAL = os.environ.get("UASREADY_EMULATOR", "emulator-5620")
OUT = os.environ.get("UASREADY_SURVEY_OUT", os.path.join(os.path.dirname(os.path.abspath(__file__)), "out"))
REFRESH_XY = (1715, 168)

REGIONS = [
    ("01_KSEA", "Seattle WA", 47.4502, -122.3088),
    ("02_KDEN", "Denver CO", 39.8561, -104.6737),
    ("03_KORD", "Chicago IL", 41.9742, -87.9073),
    ("04_KMIA", "Miami FL", 25.7959, -80.2870),
    ("05_KJFK", "New York NY", 40.6413, -73.7781),
    ("06_P56", "White House DC (P-56)", 38.8977, -77.0365),
    ("07_KABQ", "Albuquerque NM", 35.0402, -106.6090),
    ("08_KCNO", "Chino CA (Class D)", 33.9747, -117.6366),
    ("09_KBKE", "Baker City OR (Class E2)", 44.8452, -117.8314),
    ("10_KANSAS", "Rural Kansas (Class G)", 38.50, -99.00),
    ("11_R2505", "China Lake CA (R-2505)", 35.70, -117.65),
    ("12_W147B", "Gulf W-147B (FL230 floor)", 28.1644, -92.9940),
    ("13_PANC", "Anchorage AK", 61.1743, -149.9983),
    ("14_PHNL", "Honolulu HI", 21.3187, -157.9225),
    ("15_R5002G", "Warren Grove NJ (R-5002G FL200 only)", 39.6741, -74.3048),
    ("16_R4008", "Chesapeake MD (R-4008 FL250 only)", 37.9400, -76.3564),
]


def adb(*args, capture=True):
    return subprocess.run([ADB, "-s", SERIAL, *args], capture_output=capture, text=True, timeout=60).stdout


def screenshot(path):
    with open(path, "wb") as f:
        subprocess.run([ADB, "-s", SERIAL, "exec-out", "screencap", "-p"], stdout=f, timeout=60)


def wait_for_result(lat, lon, timeout_s=75):
    pat_ok = re.compile(r"Airspace classified: primary=(\w+) auth=(\w+) specialUse=(\w+) zones=(\d+) groundFt=(\S+) cached=(\w+)")
    pat_fail = re.compile(r"Airspace unavailable: (.*)")
    t0 = time.time()
    while time.time() - t0 < timeout_s:
        # The fix only reaches the framework while the app's single-update request is active, so keep sending it.
        adb("emu", "geo", "fix", str(lon), str(lat), "30")
        log = adb("logcat", "-d", "-s", "MainViewModel:*")
        m = pat_ok.search(log)
        if m:
            return {"status": "ok", "primary": m.group(1), "auth": m.group(2) == "true", "specialUse": m.group(3),
                    "zones": int(m.group(4)), "groundFt": m.group(5), "cached": m.group(6) == "true",
                    "seconds": round(time.time() - t0, 1)}
        m = pat_fail.search(log)
        if m:
            return {"status": "failed", "error": m.group(1).strip(), "seconds": round(time.time() - t0, 1)}
        time.sleep(2)
    return {"status": "timeout", "seconds": timeout_s}


os.makedirs(OUT, exist_ok=True)
only = set(sys.argv[1:])
results = []
for key, name, lat, lon in REGIONS:
    if only and key not in only:
        continue
    # Toggling location clears the framework's stale last-known fix, forcing the app to request a fresh one.
    adb("shell", "settings", "put", "secure", "location_mode", "0")
    adb("shell", "settings", "put", "secure", "location_mode", "3")
    time.sleep(5)  # let any in-flight refresh from the previous step finish logging before clearing
    adb("logcat", "-c")
    adb("shell", "input", "tap", str(REFRESH_XY[0]), str(REFRESH_XY[1]))
    r = wait_for_result(lat, lon)
    time.sleep(3)  # let Compose render the new state
    shot = os.path.join(OUT, f"{key}.png")
    screenshot(shot)
    r.update({"key": key, "name": name, "lat": lat, "lon": lon, "screenshot": shot})
    results.append(r)
    print(json.dumps(r), flush=True)
    time.sleep(6)  # be gentle with the shared FAA quota

with open(os.path.join(OUT, "results.json"), "w") as f:
    json.dump(results, f, indent=2)

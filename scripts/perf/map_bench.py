"""Map frame benchmark (chunk 7). Open the app's map screen first, then: python scripts/perf/map_bench.py LABEL
Taps 6 map points (closing each inspection popup), 4 pans, 2 basemap switches; prints dumpsys gfxinfo
frame stats and MapOverlays rebuild counts. Coordinates are for the 1920x1080 RC Pro / uasready-rcpro AVD.
Set ADB / UASREADY_EMULATOR to target another device."""
import os,subprocess,sys,time,re
ADB=os.environ.get("ADB", os.path.join(os.environ.get("LOCALAPPDATA",""),"Android","Sdk","platform-tools","adb.exe")); S=os.environ.get("UASREADY_EMULATOR","emulator-5620"); P="com.taksolutions.uasready"
def adb(*a): return subprocess.run([ADB,"-s",S,*a],capture_output=True,text=True,timeout=60).stdout
def tap(x,y): adb("shell","input","tap",str(x),str(y)); time.sleep(1.5)
def close_popup():
  adb("shell","uiautomator","dump","/sdcard/ui.xml"); x=adb("shell","cat","/sdcard/ui.xml")
  m=re.search(r'content-desc="Close"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',x)
  if m: a,b,c,d=map(int,m.groups()); tap((a+c)//2,(b+d)//2); return True
  return False
closed=0
def swipe(x1,y1,x2,y2): adb("shell","input","swipe",str(x1),str(y1),str(x2),str(y2),"400"); time.sleep(1.5)
label=sys.argv[1]
adb("shell","dumpsys","gfxinfo",P,"reset"); adb("logcat","-c")
for x,y in [(900,450),(1100,600),(700,470),(1300,420),(1000,700),(800,560)]:
  tap(x,y); closed+=close_popup()  # inspect, then close the popup
for a in [(1200,540,700,540),(700,540,1200,540),(960,700,960,350),(960,350,960,700)]: swipe(*a)
tap(280,360); time.sleep(3); tap(117,360); time.sleep(3)   # basemap TOPO then STREET
out=adb("shell","dumpsys","gfxinfo",P)
keys=["Total frames rendered","Janky frames","50th percentile","90th percentile","95th percentile","99th percentile","Number Slow UI thread","Number Slow bitmap uploads","Number Slow issue draw commands"]
print(label, "popups closed:", closed)
for k in keys:
  m=re.search(re.escape(k)+r"[^\n]*",out); print("  ",m.group(0) if m else k+": n/a")
log=adb("logcat","-d","-s","Choreographer:*","MapOverlays:*")
print("   Choreographer skipped-frame warnings:",len(re.findall("Skipped",log)), re.findall(r"Skipped (\d+) frames",log))
ms=[int(x) for x in re.findall(r"rebuilt .* in (\d+) ms",log)]
print("   overlay rebuilds:",len(ms)," total ms:",sum(ms)," max ms:",max(ms) if ms else 0, " sample:", [l.split(": ",1)[-1] for l in log.splitlines() if "rebuilt" in l][:1])

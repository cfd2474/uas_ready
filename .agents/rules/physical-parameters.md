---
trigger: always_on
---

# Agent Rule: Layout & Optimization — DJI RC Pro Enterprise

**Scope:** All UI/layout/rendering decisions in this project target the DJI RC Pro Enterprise as the sole primary device. Do not design for generic phones or tablets. When a layout choice conflicts with "standard Android best practice," the RC Pro Enterprise constraint wins.

## 1. Device facts (treat as fixed)

| Property | Value |
|---|---|
| Screen | 5.5" IPS, 1920 × 1080 px |
| Orientation | Landscape only, fixed. Never support portrait. |
| Pixel density | ~401 ppi. Assume `xxhdpi`, density 3.0 → **640 × 360 dp** logical canvas. Verify once via `DisplayMetrics` on first device run and pin the real value in `docs/device.md`; do not re-derive it per screen. |
| Touch | 10-point capacitive multi-touch |
| Refresh | 60 Hz → 16.6 ms frame budget |
| Brightness | ~1000 nits, used outdoors in direct sun |
| Form factor | Handheld controller with sticks below the screen; thumbs rest near the bottom corners |
| OS | DJI-customized Android (API level is low — confirm `Build.VERSION.SDK_INT` once and pin `minSdk` to it; do not assume modern Android APIs exist) |

## 2. Layout rules

- **Hardcode for 640 × 360 dp landscape.** Use a single `layout-land` resource set / single Compose layout. No `sw600dp` qualifiers, no responsive breakpoints, no portrait variants.
- **No system-UI assumptions.** Treat status bar and nav bar as possibly absent or non-standard. Use edge-to-edge and read `WindowInsets` at runtime rather than reserving fixed space.
- **Vertical space is the scarce resource (360 dp).** Prefer horizontal arrangement: side panels, horizontal toolbars, rail navigation. Never stack more than ~4 rows of controls.
- **Thumb zones:** Primary actions go in the bottom-left and bottom-right corners (within ~120 dp of each corner). Destructive or rarely used actions go top-center, away from stick-hand reach.
- **Touch targets:** Minimum 48 × 48 dp; prefer 56 dp for anything used during flight. Minimum 8 dp spacing between adjacent targets. Operator may be wearing gloves.
- **Map/video view is the background.** UI controls overlay it as translucent panels; the live view must never be pushed into a sub-region smaller than ~70% of the screen unless the user explicitly opens a full-screen panel.
- **No bottom sheets, no dialogs that cover the center.** Use side drawers (slide from left/right edge) and top-anchored snackbars. Center-of-screen is the live view.
- **Lists:** Horizontal scrolling or two-column grids. A single-column vertical list is almost always wrong on this canvas.
- **Text:** Body minimum 14 sp, labels minimum 12 sp, critical telemetry 18–24 sp. No text below 12 sp anywhere.

## 3. Sunlight / outdoor readability

- High-contrast palette: foreground/background contrast ratio ≥ 7:1 for telemetry and status; ≥ 4.5:1 everywhere else.
- Default to a dark theme with light text. Avoid mid-grey on grey.
- Status must never rely on color alone — pair with icon or text (green/red is unreadable in sun and for colorblind users).
- Overlay panels over video: use solid or ≥ 85% opaque backgrounds behind text. No thin text on translucent glass effects.

## 4. 60 fps performance rules

- **Frame budget 16.6 ms.** Any composable/view that exceeds ~8 ms in Layout Inspector / Perfetto must be fixed before merge.
- No nested scrolling containers. No `wrap_content` on children of scrolling parents.
- Overdraw: keep ≤ 2× on the live-view region. Do not put opaque backgrounds on layers above the video surface unless required by §3.
- Video/map surface uses `SurfaceView` (or `AndroidView` wrapping one in Compose), never `TextureView`, unless transformation/animation of the video itself is required.
- Compose: hoist state, use `remember`/`derivedStateOf`, avoid recomposing the full screen on telemetry ticks. Telemetry updates go to leaf composables only. Throttle telemetry UI updates to ≤ 10 Hz unless the value is a flight-critical number.
- Animations: 60 fps-safe only — opacity, translate, scale. No layout-triggering animations over the live view.
- Images/icons: vector drawables or pre-sized `xxhdpi` rasters. Never ship `xxxhdpi`-only assets that require downscale at runtime.
- No blur, no `RenderEffect`, no shadows with large elevation over the video region.

## 5. Multi-touch

- Support simultaneous interaction: e.g. pinch-zoom on map while tapping a side-panel button. Do not use `requestDisallowInterceptTouchEvent` broadly; scope it to the gesture region.
- Gesture conflicts: map/video region owns pan/pinch/rotate; overlay panels own tap/drag. Never attach swipe gestures to panels that sit over the map.

## 6. Physical controls

- Reserve for optional enhancement only; never make a function reachable *only* via hardware button (C1/C2/5D/dial). Every action must have a touch path.
- If mapping hardware keys, handle `onKeyDown` for `KEYCODE_*` events and document the mapping in `docs/device.md`.

## 7. Process rules for the agent

- Before creating any new screen, state the 640 × 360 dp region allocation (what goes where, in dp) in the PR/commit description.
- When asked to "make it responsive" or "support tablets," push back and cite this rule — single-device target is intentional.
- Do not add Material 3 adaptive layout libraries, `WindowSizeClass`, or foldable support.
- If a design choice here blocks a feature, document the conflict in `docs/device.md` rather than silently deviating.
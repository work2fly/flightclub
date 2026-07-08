# Flight Club — Linux Revival & TODO Feasibility Analysis

## Status: Running on Linux

The game compiles and runs on modern Linux (OpenJDK 11+) with no code changes required for the core game.
The Task Designer had two bugs that have been fixed (see below).

### Build & Run

```bash
./build.sh
java -cp out flightclub.startup.XCFrame           # default task, hangglider
java -cp out flightclub.startup.XCFrame default 0  # paraglider
java -cp out flightclub.startup.XCFrame default 3  # balloon
java -cp out flightclub.task.TaskFrame             # task designer
```

### Fixes Applied

1. **TaskDesigner crash on missing task file** — `openFile()` returns null when file doesn't exist, causing NPE in `parseFile()`. Added null check with proper IOException. The designer now starts cleanly with an empty canvas.

2. **TaskDesigner slider NPE on mouse drag** — `getSliders()`/`setSliders()` attempted to cast a null `clickPoint` to `TriggerPoint`. Casting null doesn't throw ClassCastException, so the try/catch was ineffective. Replaced with `instanceof` check.

3. **Balloon CLI argument** — Already working (pilot_type=3). The CHANGELOG note "(not working!)" appears to be stale — the commits that enabled balloon type *are* in the current master branch.

---

## Dan Burton's Original TODO — Feasibility Assessment

### Priority 1

| Item | Feasibility | Status |
|------|-------------|--------|
| **Terrain / finish TaskDesigner** | ✅ High | PARTIAL — Designer crashes fixed. Road parsing still stubbed. |
| **Shadows** | ⚠️ Medium | Not started |
| **Collision detection** | ✅ High | ✅ DONE — Bounding sphere collision with lateral push |
| **Database backend for league table** | ✅ High | Not started |

### Priority 2

| Item | Feasibility | Status |
|------|-------------|--------|
| **Sink (rectangles)** | ✅ High | ✅ DONE — SinkZone class + 4 zones in default task |
| **Blue thermals** | ✅ High | ✅ DONE — Invisible clouds via `visible` flag + 3 in default task |
| **Balloons** | ✅ Done | ✅ Already working (pilot_type=3) |
| **Thermal cycle (3 stages)** | ⚠️ Medium | ✅ DONE — Triggers now have building/full/dying phases |
| **Vector maps** | ⚠️ Medium | Not started |
| **Menus** | ✅ High | Not started |
| **Fast depth sort** | ⚠️ Medium | Not needed (polygon count is low) |
| **Exploding wings / falling** | ✅ High | Not started |

### Priority 3

| Item | Feasibility | Status |
|------|-------------|--------|
| **Total energy (ke + pe)** | ✅ High | ✅ DONE — Speed changes trade with altitude |
| **Caching/interpolation wrapper** | ✅ High | Not started |
| **Lens flare** | ⚠️ Low | Not started |
| **Wave clouds** | ⚠️ Medium | Not started (needs terrain first) |
| **Multi-cell clouds** | ✅ High | Not started |
| **Cloud algebra** | ⚠️ Medium | Not started |
| **Depth of convection / stratus** | ⚠️ Low | Not started |

---

## Additional Modernization Opportunities

| Improvement | Effort | Impact |
|-------------|--------|--------|
| **Proper build system (Gradle/Maven)** | 1 hour | Reproducible builds, dependency management, JAR packaging |
| **Runnable JAR with resources** | 30 min | Double-click to play on any OS with Java |
| **Remove Applet code** | 1 hour | Applets are dead (removed from JDK 17+). Extract to application-only. Needed for JDK 17+ compat. |
| **JDK 17/21 compatibility** | 2 hours | Remove deprecated AWT calls, replace `show()` with `setVisible(true)` etc. |
| **Keyboard rebinding** | 1 hour | Allow custom key mappings |
| **Resizable window** | 30 min | Currently hardcoded 700×370 |
| **Sound volume control** | 30 min | The vario beeps can be loud |
| **Save/load task from file chooser** | 1 hour | TaskDesigner currently hardcodes `t001.task` |
| **Frame-rate independent physics** | 2 hours | Clock already has `dt` parameter, but some code assumes fixed rate |

---

## Summary

The game is in surprisingly good shape for 20+ year old Java. It compiles and runs without modification on modern Linux/JDK 11. The main barriers to a fuller revival are:

1. **Applet removal** — needed before JDK 17+ (the `java.applet` package is gone)
2. **TaskDesigner completion** — road/hill support in the editor
3. **Energy model** — would make the flying physics much more satisfying

With AI assistance, most of Dan's original TODO items are genuinely achievable in a weekend of focused work. The codebase is clean, well-structured, and the 3D framework is simple enough to extend without deep graphics knowledge.

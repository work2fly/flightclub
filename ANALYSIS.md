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

| Item | Feasibility | Notes |
|------|-------------|-------|
| **Terrain / finish TaskDesigner** | ✅ High | The designer works now. Road parsing code is stubbed (`// roads - TODO`). An AI can generate the road/hill parsing logic to match the game's existing `RoadManager` and `Hill` classes. ~2-4 hours work. |
| **Shadows** | ⚠️ Medium | The 3D engine is software-rendered (no OpenGL). Adding shadows means projecting each Obj3d onto the terrain plane. Feasible but requires understanding the depth-sort system. ~1 day. |
| **Collision detection** | ✅ High | Simple bounding-sphere checks between gliders. The positions are already tracked in `Glider.p[]`. Straightforward to implement. ~2 hours. |
| **Database backend for league table** | ✅ High | SQLite + JDBC is trivial to add. Store task ID, pilot name, completion time. Could even add a local leaderboard UI panel. ~3 hours. |

### Priority 2

| Item | Feasibility | Notes |
|------|-------------|-------|
| **Sink (rectangles)** | ✅ High | Inverse of thermals — just negative lift zones. Add a `SinkSource` analogous to `LiftSource`. ~1 hour. |
| **Blue thermals** | ✅ High | Dan described exactly what to do: use Cloud class but with `shape3d = null`. Just thermals with no visible cloud above. ~30 min. |
| **Balloons** | ✅ Done | Already implemented and working. |
| **Thermal cycle (3 stages)** | ⚠️ Medium | Currently thermals are either on or off. Adding lifecycle state (forming → mature → dying) to `Trigger` class with time-based transitions. ~3 hours. |
| **Vector maps** | ⚠️ Medium | Dan noted he fudged this with view #5. A proper mini-map overlay on the canvas showing task/position would be ~4 hours. |
| **Menus** | ✅ High | Replace the keyboard-prompt system with an AWT menu or simple button panel for glider selection. ~2 hours. |
| **Fast depth sort** | ⚠️ Medium | Current sort in `Obj3dManager` is adequate for the polygon count. BSP or painter's algorithm refinements possible but may not be needed. |
| **Exploding wings / falling** | ✅ High | Fun cosmetic feature. On "crash" create particle debris with gravity physics. ~3 hours. |

### Priority 3

| Item | Feasibility | Notes |
|------|-------------|-------|
| **Total energy (ke + pe)** | ✅ High | Dan said he'd already written this. The polar model is in `GliderType`. Adding speed-trading (dive for speed, pull up for height) is ~2 hours of physics. |
| **Caching/interpolation wrapper** | ✅ High | A generic `LookupTable` class with linear interp. Standard stuff. ~1 hour. |
| **Lens flare** | ⚠️ Low | Possible in software rendering but would look dated. Better spent elsewhere. |
| **Wave clouds** | ⚠️ Medium | Stationary lift bands downwind of hills. Straightforward if hills exist. ~2 hours after terrain is done. |
| **Multi-cell clouds** | ✅ High | Merge nearby triggers into larger cloud shapes. ~2 hours. |
| **Cloud algebra** | ⚠️ Medium | Conceptually simple (bigger thermal = stronger + wider), implementation touches several classes. ~3 hours. |
| **Depth of convection / stratus** | ⚠️ Low | Atmospheric modelling beyond game scope. Cosmetic only. |

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

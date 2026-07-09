# Flight Club — Feature Ideas Backlog

A running list of innovative/novel/inspirational features discussed for the
Linux revival. Not all of these will get built, but they're captured here so
we don't lose good ideas.

Status legend: 💡 idea only · 🚧 in progress · ✅ done

---

## Flying Experience

- 💡 **Wind gradient / shear layers** — wind speed/direction changes with altitude,
  glider feels a push entering a new layer
- 🚧 **Ridge/slope soaring + terrain** — hills, mountains, ridges with windward lift
  and leeward sink/turbulence (see TERRAIN.md for design)
- 💡 **Rotor and turbulence zones** — random jolts behind hills when flying in rotor
- 💡 **Convergence lines** — long lines of strong, reliable lift where air masses meet
- 💡 **Inversion layer** — hard ceiling on thermalling; can break partway through a
  session for a dramatic cloudbase jump

## Gameplay & Challenge

- 💡 **XC distance leaderboard** — track/record furthest flight from launch
- 💡 **Retrieval / landing score** — bonus for landing near a road, penalty for
  landing in the middle of nowhere
- 💡 **Time trials** — race the task against the clock, rewards speed-to-fly decisions
- 💡 **Weather progression** — thermal strength/cloudbase scale with time of day
  (weak morning → strong midday → overdeveloped afternoon)
- 💡 **Guided tutorial mode** — on-screen prompts teaching thermalling/gliding basics

## Visual & Atmospheric

- 💡 **Time-of-day lighting** — sun angle changes terrain/cloud colour across a session
- 💡 **Cloud streets** — thermals align with wind, allowing dolphin-flying without circling
- 💡 **Rain shafts / virga** — visual warning under overdeveloped clouds; flying into
  one gives heavy sink
- 💡 **Terrain texture from altitude** — visual detail scales with height AGL

## Social & Modern

- 💡 **Ghost replay** — save best flight's position log, replay as a ghost to race against
- 💡 **Shareable task files** — export/import task files via simple dialog
- 💡 **Real-world site recreation** — heightmap-based hill generation for real sites
- 💡 **QR code / URL task sharing** — share a task as a link (needs applet removal first)

## Physics / Realism

- 💡 **Speed-to-fly director** — McCready-style optimal speed indicator between thermals
- 💡 **Wing loading** — pilot weight slider affecting speed/sink/turn radius
- 💡 **Asymmetric collapse** — paraglider-specific hazard in rotor/strong shear

---

## Priority picks (from initial review)

1. Weather progression — cheap to build, big feel improvement
2. Ghost replay — save/replay position log
3. Cloud streets — align triggers with wind
4. XC distance leaderboard — track max distance from launch

## Currently active work

**Terrain + ridge soaring** (see `TERRAIN.md` for full design) — hills/mountains/ridges
with physically modelled windward lift and leeward sink/turbulence.

### Ridge soaring — status and open issues (as of last session)

Implemented in `client/Ridge.java`, wired into `Task`, `GliderManager`,
`NodeManager`, `GliderAI`. Fixed so far:
- ✅ Ridge lift was originally scaled by raw wind speed (~0.1), way below
  glider sink rates (~0.06-0.15) — never produced usable lift. Fixed by
  using a fixed `LIFT_UNIT` (like `Cloud.LIFT_UNIT`) scaled by slope angle
  instead, gated by a minimum crosswind threshold.
- ✅ Ground collision was checked *before* the tick's motion was applied,
  and never clamped position to the surface — could tunnel through fast.
  Fixed to check after motion, using final position, clamped to `ground`.
- ✅ Slope faces rendered as one giant polygon each — this engine culls a
  whole polygon if any single vertex falls outside the camera's near view,
  causing faces to vanish up close. Fixed by subdividing into `SEGMENTS`
  quads per slope (same trick `Terrain.java` already used for its grid).
- ✅ Sink zones were centered directly on top of thermal trigger clusters
  (both used the same `HEXAGON`-based coordinates), cancelling lift where
  gliders were trying to climb. This is likely what caused "flew under a
  cloud, got no thermal" and "gaggle thermalling downward" reports. Fixed
  by shrinking sink zones and centering them away from trigger points.
- ✅ Ridges were originally placed far from the task/launch point and
  outside any loaded node radius, so they never rendered or registered as
  lift sources near where testing happened. Repositioned closer to launch.

**Still open / to revisit:**
- AI gliders have no real terrain avoidance — `GliderAI` doesn't check for
  rising ground ahead, it only reacts to lift sources it's already
  decided to head toward. The ridge-soaring circuit line (`getCircuit()`)
  was moved clear of the slope base, but this doesn't stop an AI glider
  from being routed *through* a ridge if it's approaching from a
  different lift source or turnpoint glide. Needs an explicit "is there
  terrain between me and my target" check, or a simple avoidance
  steering behaviour (bank away when ground is rising fast underneath).
- Launch-to-ridge distance still needs practical tuning/playtesting - it
  was "almost too far to reach" even after moving it in. Worth deciding
  distances by actual glide ratio math per glider type rather than eyeballing.
- Confirm ridge lift "feels right" in an actual play session now that
  the LIFT_UNIT fix is in - lift values were sanity-checked once via
  debug print but not fully flown/tuned by feel yet.
- Leeward sink/turbulence effects haven't been specifically play-tested
  (focus so far has been getting windward lift working at all).

---

## Investigation: older v3.01 version with working ridge soaring

Found at https://www.urban75.org/useless/glider.html — a page hosting an older
build of Flight Club. The screenshot (`ridge_b.gif`) shows a rounded hill with
gliders ridge soaring, which doesn't match anything in our v3.02.09 codebase.

**Findings:**
- The page's popup (`hg_popup.htm`) embeds `XCGameApplet.class` — confirms this
  is the **v3.01** version (`hg_src_3_01` per the main README), not v3.02.09.
  Controls listed (`z`/`x` to turn) match v3.01; v3.02 uses arrow keys.
- Some individual `.class` files ARE downloadable from urban75 (200 OK):
  `XCGameApplet`, `Glider`, `GliderUser`, `Cloud`, `Hill`, `Circuit`, `Compass`,
  `Tail`, `DataSlider`, `MovementManager`. Others 404. A full set was not
  retrieved — worth revisiting with a proper class file listing/decompile if
  we want v3.01 source.
- Compared the current repo against a separately-found `xc_src_3_02_09.tar.gz`
  (Dan Burton's own 2003 source release). Diffed every file (whitespace-
  insensitive) — **no functional differences**, only comment/indentation
  cleanup that Wingman4l7 did later. Confirms the GitHub repo is a faithful
  copy of the real v3.02.09 release.
- Critically: `Hill.java` in the *original 2003 tarball* is the exact same
  empty stub (`getLift() {return 0;}`, etc) as in the current repo. The
  `GliderAI` code that casts lift sources to `Hill` and calls `getCircuit()`
  was scaffolding for ridge soaring that Dan never finished in v3.02. So the
  ridge lift shown on urban75 is genuinely from the earlier v3.01 codebase,
  which is a different, simpler version that was never merged forward.

**Possible next steps if we want v3.01's original ridge implementation:**
- Try to recover the full set of `.class` files from urban75 (some 404 -
  may need to guess additional class names, or check archive.org for the
  same page/directory with a full snapshot).
- Decompile recovered `.class` files (e.g. with `cfr` or `javap -c` for
  bytecode-level inspection) to see Dan's actual ridge lift formula and
  compare against the physically-modelled version we built from scratch
  in `Ridge.java`.
- Alternatively, treat our from-scratch `Ridge.java` as the spiritual
  successor - it already does more than v3.01 likely did (windward lift AND
  leeward sink/turbulence, subdivided rendering, terrain collision).

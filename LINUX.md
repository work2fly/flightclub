# Flight Club — Running on Linux

## Prerequisites

- **Java JDK 11 or later** (JDK 11 recommended, JDK 17+ may show deprecation issues with Applet API)
- **X11 display** (Wayland works via XWayland)

### Install Java (Ubuntu/Debian)

```bash
sudo apt install openjdk-11-jdk-headless
```

### Install Java (Fedora/RHEL)

```bash
sudo dnf install java-11-openjdk-devel
```

### Install Java (Arch)

```bash
sudo pacman -S jdk11-openjdk
```

## Build

```bash
./build.sh
```

This compiles all source files into `./out/` and copies the resource files (sounds, glider data).

## Run the Game

### Default (hangglider, 700×370 window)

```bash
java -cp out flightclub.startup.XCFrame
```

### Choose your glider

```bash
java -cp out flightclub.startup.XCFrame default 0   # paraglider
java -cp out flightclub.startup.XCFrame default 1   # hangglider (default)
java -cp out flightclub.startup.XCFrame default 2   # sailplane
java -cp out flightclub.startup.XCFrame default 3   # balloon
```

### Bigger window

Set window size with Java properties or environment variables:

```bash
# Using Java properties (recommended)
java -Dfc.width=1280 -Dfc.height=720 -cp out flightclub.startup.XCFrame

# Using environment variables
FC_WIDTH=1920 FC_HEIGHT=1080 java -cp out flightclub.startup.XCFrame

# Fullscreen-ish (set to your screen resolution)
java -Dfc.width=1920 -Dfc.height=1080 -cp out flightclub.startup.XCFrame
```

### Set number of AI gliders

```bash
# Format: [task] [pilot_type] [pgs hgs sps]
# 4 paragliders, 6 hanggliders, 3 sailplanes
java -cp out flightclub.startup.XCFrame default 1 4 6 3
```

### Combine options

```bash
# Big window, paraglider, lots of AI traffic
java -Dfc.width=1280 -Dfc.height=720 -cp out flightclub.startup.XCFrame default 0 5 5 5
```

## Run the Task Designer

```bash
java -cp out flightclub.task.TaskFrame
```

The task designer lets you create custom competition tasks with turn points and thermal triggers.

## Controls

| Key | Action |
|-----|--------|
| `y` | Launch / take off |
| Left/Right arrows | Turn left/right (in flight), choose glider (on ground) |
| Spacebar | Circle in thermal |
| `p` | Pause/Resume (offline only) |
| `q` | Fast simulation (offline only) |
| `1`-`7` | Camera views (see below) |
| `-` / `+` | Zoom out / in |
| Mouse drag | Rotate camera |

### Camera Views

| Key | View |
|-----|------|
| `1` | Follow your glider |
| `2` | Follow the gaggle |
| `3` | Plan view (from above) |
| `4` | Current node (from distance) |
| `5` | Entire task map |
| `6` | Pilot's eye view |
| `7` | Freeze camera position |

## Troubleshooting

### No window appears
- Check `echo $DISPLAY` returns something (e.g. `:0`)
- If running via SSH, use `ssh -X` for X11 forwarding

### Sound not working
- Ensure PulseAudio or PipeWire is running
- The vario beeps use `.wav` files in the `out/` directory

### "Error opening file" messages
- Make sure you run from the project root directory (where the `.txt` and `.wav` files are)
- Or use the build script which copies resources to `out/`

### Tiny text on HiDPI displays
- Use a larger window size: `-Dfc.width=1920 -Dfc.height=1080`
- Java AWT doesn't scale automatically on HiDPI — the bigger window helps

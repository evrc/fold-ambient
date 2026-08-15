# Fold Ambient

Fold Ambient is a native Android ambient dashboard designed primarily for the Samsung Galaxy Z Fold6 cover screen in tent or wedge mode.

It turns the outer display into a customizable, night-friendly desk surface with pages, layouts, widgets, media, lyrics, weather, and Fold6-aware automatic activation. The project is currently in v1 stabilization: the main roadmap is implemented, and the next work is product and UX validation on the physical Fold6.

## Screenshots

Screenshots and short device demos are not published yet.

## Why Fold Ambient?

Foldable phones can sit open at an angle with the cover screen visible, but Android's foldable APIs do not fully describe that cover-screen posture on the Galaxy Z Fold6. Fold Ambient is built around the behavior observed on the real device instead of assuming that generic fold-state APIs tell the whole story.

The goal is a practical ambient display for bedside, desk, and media use: glanceable, low-brightness, customizable, and pleasant on the Fold6 cover display.

## Features

- Native Android app built with Kotlin and Jetpack Compose.
- Fullscreen, edge-to-edge ambient dashboard with dark OLED-friendly visuals.
- Horizontally swipable pages with cyclic navigation and transient page indicators.
- Full, Duo, and Quad page layouts.
- Long-press edit mode with page controls, layout switching, widget replacement, rearranging, and widget configuration.
- Persisted pages, widget configuration, widget appearance, activation settings, and display settings.
- Built-in widgets:
  - digital clock with multiple styles and full-fit sizing;
  - analog clock;
  - date;
  - battery;
  - simple text;
  - empty spacer tile;
  - weather;
  - media playback;
  - lyrics.
- Weather widget backed by Open-Meteo, with city search/autocomplete and optional phone-location mode.
- Media widget backed by Android media-session APIs, with metadata, artwork, progress, play/pause, previous, next, seeking, and double-tap open-app behavior where available.
- Lyrics widget backed by LRCLIB, with synced/plain lyrics support, active-line tracking, smooth scrolling, caching, and tap-to-seek for synced lyrics where media seeking is supported.
- Automatic Fold6 ambient activation using validated cover-like window geometry, orientation, hinge angle, and optional charging state.
- Debounced activation state machine with exit hysteresis and manual pause.
- Ambient display behavior including scoped keep-screen-on, active/idle brightness, idle dimming, and subtle pixel shifting.
- Side-effect-free widget picker previews and lifecycle-aware live widget work.
- Focused JVM test safety net for persistence, activation, lyrics parsing, media state, weather mapping, cache behavior, and widget registry behavior.

Third-party Android AppWidget hosting has been investigated but is not exposed in v1. The picker intentionally hides that option until the full allocate, bind, configure, and cleanup flow is implemented.

## Example Layouts

```text
Clock | Weather
Date  | Battery
```

```text
Media  | Lyrics
Clock  | Empty
```

```text
Digital Clock
```

## Fold6 Support

Fold Ambient is based on physical Samsung Galaxy Z Fold6 testing. The recorded findings are in [docs/fold6-device-findings.md](docs/fold6-device-findings.md).

Important validated behavior:

- `Sensor.TYPE_HINGE_ANGLE` is exposed to a normal app on the tested Fold6.
- The hinge-angle sensor continues reporting while the app is on the cover display.
- `displayId` was observed as `0` on both cover and inner displays, so it is not used as the primary cover/inner discriminator.
- Jetpack WindowManager `FoldingFeature` is useful on the inner display but did not report cover-screen tent/wedge posture.
- Cover and inner display contexts are currently distinguished most clearly by window geometry and aspect ratio, without hardcoding exact pixel dimensions as a permanent detector.

Broader foldable-device support remains a future validation area.

## Tech Stack

- Kotlin
- Jetpack Compose
- AndroidX Activity and Lifecycle
- Android platform APIs for sensors, media sessions, notification-listener access, location, battery, display/window behavior, and app launching
- Open-Meteo for weather and city search
- LRCLIB for lyrics lookup
- Gradle with Android Gradle Plugin 9.0.1
- Kotlin 2.3.20
- Compose BOM 2026.03.01
- `compileSdk` 36, `targetSdk` 36, `minSdk` 31

Development is intended to work entirely from VS Code and the command line. Android Studio is not required.

## Building

Prerequisites:

- JDK 17
- Android SDK command-line tools
- Android SDK platform/build tools required by the Gradle project
- `adb` for device installation and launch

```bash
git clone https://github.com/evrc/fold-ambient.git
cd fold-ambient
./gradlew assembleDebug
```

The application id is currently:

```text
com.example.foldambient
```

That package id is development-oriented and may change before any wider distribution.

## Installing On A Device

```bash
./gradlew installDebug
adb shell am start -n com.example.foldambient/.MainActivity
```

For the intended experience, test on a physical Samsung Galaxy Z Fold6 cover screen in landscape tent/wedge posture.

## Permissions And Android Access

Fold Ambient keeps Android access narrow and feature-driven:

- `INTERNET` is used for weather, city search, and lyrics lookup.
- `ACCESS_COARSE_LOCATION` is used only for optional phone-location weather mode. Manual city search does not require location access.
- Notification Listener access is used for Android MediaSession discovery/control. Media and lyrics widgets can show limited or unavailable states until the user grants this special access in Android settings.
- `Sensor.TYPE_HINGE_ANGLE` is queried for Fold6 posture detection when available. No permission is required for that sensor.

## Development

Useful checks:

```bash
./gradlew test
./gradlew assembleDebug
```

The current JVM test count recorded by the roadmap is 87.

Project conventions and implementation notes live in [AGENTS.md](AGENTS.md). Current roadmap status and deferred work are tracked in [ROADMAP.md](ROADMAP.md).

## Project Status

Fold Ambient is an active personal/experimental project. The implemented roadmap is now in a v1 stabilization and validation posture.

Current focus:

```text
Product / UX validation and polish
```

Next work should primarily be driven by physical Fold6 usage rather than speculative architecture.

Intentionally deferred work includes full third-party Android AppWidget hosting, broader weather-provider architecture, broader foldable-device support, richer external integrations, and more flexible custom widget sizing.

## License

No license has been selected yet.

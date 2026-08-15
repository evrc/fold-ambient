# Fold Ambient — Roadmap

Fold Ambient is a native Android ambient-dashboard application designed primarily for the **Samsung Galaxy Z Fold6 cover screen**, especially while the device is positioned in **tent or wedge mode**.

The long-term goal is a customizable StandBy-style dashboard built around a reusable widget system. Music playback and synchronized lyrics are planned as widgets rather than as application-specific screens.

## Guiding principles

- Native Android using **Kotlin + Jetpack Compose**.
- Development must work entirely from **VS Code + command-line Android tooling**; Android Studio must not be required.
- Optimize the experience for the **Galaxy Z Fold6 cover display in landscape tent/wedge mode** first.
- Build reusable primitives before application-specific features.
- Keep layouts separate from widget implementations.
- Prefer official Android and AndroidX APIs.
- Keep dependencies minimal.
- Validate meaningful milestones on the physical Fold6.
- Avoid speculative abstraction: introduce infrastructure when a real feature requires it.
- Every development milestone should leave the project building and runnable.

---

# Phase 1 — Fold6 device diagnostics — Completed

Determine exactly what the Galaxy Z Fold6 exposes to a normal Android application in different physical configurations.

Investigate:

- cover display vs inner display;
- display IDs;
- window dimensions;
- rotation and orientation;
- Jetpack WindowManager `FoldingFeature`;
- `Sensor.TYPE_HINGE_ANGLE`, if exposed;
- closed posture;
- wedge posture;
- tent posture;
- fully unfolded posture.

The purpose of this phase is observation only. Application behavior should not yet depend on assumptions about Samsung's fold-state implementation.

Validated physical-device findings are recorded in `docs/fold6-device-findings.md`.

---

# Phase 2 — Ambient application shell — Completed

Build the basic runtime environment in which widgets will eventually live.

Implement:

- fullscreen / edge-to-edge Compose surface;
- correct landscape cover-screen behavior;
- status/navigation bar handling;
- screen-awake behavior;
- basic lifecycle handling;
- manual entry into the ambient dashboard;
- correct resizing when moving between cover and inner displays.

Automatic tent/wedge activation is intentionally deferred until the widget experience works.

The initial shell provides manual entry into a fullscreen ambient dashboard, keeps the screen awake only while ambient mode is active, hides system bars during ambient mode, and adapts its placeholder layout to wide cover-screen landscape geometry.

---

# Phase 3 — Widget and page engine — Completed

This is the architectural foundation of Fold Ambient.

Introduce the concepts of:

- `AmbientWidget`;
- `WidgetInstance`;
- `AmbientPage`;
- layout definitions;
- widget configuration;
- widget appearance;
- persistent page configuration.

Widgets and layouts must remain independent.

For example, a Duo page should be able to display:

```text
┌──────────────────────┬──────────────────────┐
│        Clock         │       Weather        │
└──────────────────────┴──────────────────────┘
```

or later:

```text
┌──────────────────────┬──────────────────────┐
│        Music         │        Lyrics        │
└──────────────────────┴──────────────────────┘
```

without the Duo layout knowing anything about either widget type.

Start with **dummy widgets** rather than real features so the framework itself can be tested first.

The initial engine introduces page, layout, widget instance, widget configuration, widget appearance, registry, renderer, and persistent page deck concepts. It currently renders a persisted default Duo page with dummy widgets.

---

# Phase 4 — Widget editing experience — Completed

Build an editing experience inspired by **StandBy Mode: Clock & Widget by Zetabit Tecnologia**, while keeping Fold Ambient's implementation and visual assets original.

Target interaction model:

- normal ambient mode shows only widgets;
- long press enters edit mode;
- widget slots become selectable;
- tapping a slot opens a widget picker;
- picker entries include a useful live preview;
- widgets can be replaced;
- widgets can be swapped/rearranged;
- changes are persisted.

The initial focus should be the **Duo** layout because it is especially appropriate for the Fold6 cover display in landscape.

The initial editing pass supports long-press entry into edit mode, selectable widget slots, tap-to-open widget picking, live preview entries, replacement, Duo slot swapping, and immediate persistence of page changes.

---

# Phase 5 — Pages and navigation — Completed

Allow multiple ambient pages.

Users should be able to swipe horizontally between configurations such as:

```text
Page 1
Clock | Weather

Page 2
Calendar | Battery

Page 3
Music | Lyrics
```

Persist:

- page order;
- selected widgets;
- widget configuration;
- active/default page.

Editing should eventually support adding, deleting and reordering pages.

The initial page/navigation pass adds a horizontally swipable page deck, three seeded dummy pages, persisted active/default page selection, and edit-mode controls for adding, deleting, and moving pages.

---

# Phase 6 — First built-in widgets — Completed

Implement several simple widgets before media integration.

Initial candidates:

## Digital Clock

Exercises periodic state updates.

Possible configuration:

- 12/24-hour display;
- seconds on/off;
- typography/style;
- date display.

## Analog Clock

Exercises custom Compose drawing and visual customization.

## Date / Calendar

Exercises mostly static/day-based state.

## Battery

Exercises event-driven Android system state.

Possible information:

- percentage;
- charging state;
- estimated state where available.

## Simple Text

Useful both as a test widget and as a genuinely customizable dashboard element.

## Weather

Introduces asynchronous/network-backed state.

The goal is not merely to add features. These widgets should validate that widgets with very different state models can all fit the same framework cleanly.

The initial built-in widget pass adds digital clock, analog clock, date, battery, and simple text widgets. Weather remains deferred until networking and provider choices are intentionally introduced.

---

# Phase 7 — Widget customization — Completed

Allow individual widget types to expose their own configuration.

Avoid one giant hard-coded application settings screen.

Conceptually:

```text
Clock settings
├── style
├── 24-hour mode
├── seconds
├── foreground
└── background
```

while another widget may expose completely different settings.

Configuration should be persisted independently for each `WidgetInstance`.

The initial customization pass lets widgets expose compact text and boolean configuration fields. The edit experience renders those fields for the selected widget instance, and changes are persisted through the existing page deck storage.

---

# Phase 8 — Layout expansion — Completed

Once Duo is stable, introduce additional layouts.

## Full

```text
┌────────────────────────────────────────────┐
│                                            │
│                   Widget                   │
│                                            │
└────────────────────────────────────────────┘
```

## Duo

```text
┌──────────────────────┬─────────────────────┐
│                      │                     │
│                      │                     │
└──────────────────────┴─────────────────────┘
```

## Quad

```text
┌──────────────────────┬─────────────────────┐
│                      │                     │
├──────────────────────┼─────────────────────┤
│                      │                     │
└──────────────────────┴─────────────────────┘
```

More flexible/resizable layouts can be considered later if the simpler model becomes limiting.

The initial layout expansion adds Full, Duo, and Quad as explicit page layouts. Edit mode can switch the selected page between them, resizing widget slots while keeping layout persistence in the page deck model.

---

# Phase 9 — Android third-party widgets — Completed

Investigate hosting standard Android AppWidgets inside Fold Ambient.

Likely technologies:

- `AppWidgetHost`;
- `AppWidgetManager`;
- `AppWidgetHostView`;
- Compose `AndroidView` interoperability.

The widget picker could eventually contain both:

```text
FOLD AMBIENT
────────────
Clock
Calendar
Battery
Weather
Music
Lyrics

PHONE WIDGETS
─────────────
Google Calendar
Home Assistant
Todoist
...
```

This should only be attempted after Fold Ambient's own widget architecture is stable.

The initial investigation adds a safe Android AppWidget host foundation: a selectable placeholder widget type, `AppWidgetHostView` rendering for an already-bound app widget ID, and documentation for the future binding/configuration/cleanup flow. A full phone-widget picker is intentionally deferred.

---

# Phase 10 — Media widget — Completed

Add a generic media widget rather than a YouTube-Music-specific screen.

Use Android's media-session APIs to discover and control active playback.

Capabilities should include:

- title;
- artist;
- album artwork;
- playback state;
- elapsed position;
- duration;
- play/pause;
- previous;
- next;
- seeking where supported.

YouTube Music is the primary initial target, but the architecture should work with other Android media applications when possible.

Media-session/platform interaction should remain behind a small service/repository abstraction instead of being embedded directly into the widget UI.

The initial media pass adds a generic media widget backed by a small platform repository around Android media-session APIs. It can display active-session metadata, artwork, playback state, position/duration, and basic transport controls when notification-listener access is enabled by the user.

---

# Phase 11 — Lyrics widget — Completed

Implement lyrics as an independent widget that can cooperate with the media widget without depending on its UI.

Responsibilities:

- observe current media metadata;
- normalize titles and artist names;
- query a lyrics provider;
- initially investigate LRCLIB;
- match tracks using metadata and duration;
- cache successful matches;
- support synchronized lyrics;
- fall back to plain lyrics;
- expose a graceful unavailable state.

For synchronized lyrics:

- follow media playback position;
- highlight the active line;
- scroll smoothly;
- immediately resynchronize after seeks or track changes.

The intended flagship layout is:

```text
┌──────────────────────┬─────────────────────────┐
│                      │                         │
│      Album art       │    previous lyric       │
│                      │                         │
│      Song title      │    CURRENT LYRIC        │
│      Artist          │                         │
│                      │    next lyric            │
│    ◀    ❚❚    ▶     │                         │
│      ━━━●━━━━        │                         │
└──────────────────────┴─────────────────────────┘
```

The initial lyrics pass adds an independent lyrics widget backed by current media-session metadata and LRCLIB lookups. It normalizes noisy track metadata, matches by metadata and duration where available, caches successful matches in memory, displays synchronized lyrics with active-line scrolling, and falls back to plain lyrics or graceful unavailable states.

---

# Phase 12 — Automatic ambient activation — Completed

Return to the Fold6 diagnostics collected in Phase 1, especially `docs/fold6-device-findings.md`, and design a reliable activation state machine.

Potential signals include:

- active cover display;
- hinge angle or fold state;
- tent/wedge posture;
- landscape orientation;
- charging state;
- device motion / stationary state.

Possible behavior:

```text
Normal phone use
        ↓
Cover screen + suitable posture
        ↓
Ambient mode
        ↓
Picked up / closed / unfolded / other exit condition
        ↓
Normal phone use
```

The exact behavior must be based on observed Fold6 behavior rather than guessed fold-state semantics.

Users should be able to configure the activation conditions.

The initial automatic activation pass adds an opt-in state machine based on validated Fold6 behavior: cover-like landscape window geometry, raw hinge angle, and optional charging. Hinge thresholds and cover aspect ratio are configurable from the entry screen, automatic sessions exit when conditions no longer match, and manual ambient entry remains independent.

---

# Phase 13 — OLED and desk-display behavior — Completed

Because the application may remain visible for long periods on an OLED display, introduce protections and ambient-specific behavior.

Investigate:

- true-black backgrounds;
- automatic low brightness;
- pixel shifting;
- subtle movement of persistent elements;
- reduced brightness after inactivity;
- limiting static high-contrast artwork;
- periodic layout/content changes where appropriate.

Screen-awake behavior should only be active when Fold Ambient actually needs it.

The initial OLED/desk-display pass keeps the existing true-black ambient surface, applies app-window brightness reduction only while ambient mode is active, dims further after inactivity, restores normal brightness outside ambient mode, and adds subtle periodic pixel shifting for widget content. `FLAG_KEEP_SCREEN_ON` remains scoped to active ambient mode only.

---

# Phase 14 — Visual polish — Completed

Once the architecture and functionality are stable, make Fold Ambient feel purpose-built rather than like a developer utility.

Potential work:

- typography;
- widget chrome;
- spacing;
- animations;
- page transitions;
- animated lyric scrolling;
- album-art transitions;
- subtle album-art-derived backgrounds;
- widget previews;
- coherent themes;
- night-friendly visuals.

The application may take visual and interaction inspiration from existing StandBy applications, particularly Zetabit's app, but should use its own visual identity, assets and implementation.

The initial visual polish pass gives Fold Ambient a stable night-friendly theme, tighter typography, smoother cyclic page transitions, cleaner edit-only picker/configuration surfaces, and subtly rounded media artwork while keeping ambient widgets borderless.

---

# Phase 15 — Settings and persistence

Consolidate user-facing configuration.

Potential settings:

- page configuration;
- preferred layouts;
- default page;
- automatic activation rules;
- charging requirements;
- screen-awake behavior;
- brightness behavior;
- OLED protection;
- media source preferences;
- lyrics settings;
- themes.

Use an appropriate lightweight Android persistence solution such as DataStore unless project requirements later justify something else.

---

# Future possibilities

Potential extensions after the core application is mature:

- Home Assistant widget;
- notification widget;
- richer calendar support;
- media queue;
- album/artist information;
- additional lyrics providers;
- multiple themes;
- custom widget sizing;
- drag-and-drop layouts;
- widget backgrounds derived from album art;
- user-created visual presets;
- broader tablet/phone support beyond the Fold6.

---

# Planned milestones

## v0.1 — Widget Dashboard

- ambient shell;
- Duo layout;
- pages;
- editing/picker UI;
- several built-in widgets;
- persistent configuration.

## v0.2 — Media Dashboard

- generic media-session integration;
- music widget;
- synchronized lyrics widget;
- polished Music + Lyrics Duo layout.

## v0.3 — Fold Ambient

- reliable Fold6 tent/wedge activation;
- lifecycle/screen management;
- OLED protections;
- user-configurable automatic behavior.

## v0.4 — Extensible Dashboard

- additional layouts;
- third-party Android AppWidgets;
- richer customization;
- broader widget ecosystem.

---

# Current status

The initial native Android project has been scaffolded successfully.

Phase 1 Fold6 device diagnostics are complete. Validated physical-device findings are recorded in `docs/fold6-device-findings.md`.

Current foundation:

- Kotlin;
- Jetpack Compose;
- single `:app` module;
- Android Gradle Plugin 9;
- command-line / VS Code development;
- successful debug build.

**Current active work:** Phase 15 — Settings and persistence.

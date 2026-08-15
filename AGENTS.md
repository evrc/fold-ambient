# Fold Ambient

Fold Ambient is an ambient dashboard designed primarily for the Samsung Galaxy Z Fold6 cover screen in tent/wedge mode.

## Project Guidelines

- Use Kotlin and Jetpack Compose for all application UI.
- Do not require Android Studio for development; the project must remain workable from VS Code and the command line.
- Prefer official Android APIs and AndroidX libraries.
- Keep platform-specific functionality isolated behind small interfaces or services.
- Run the relevant Gradle build/checks after changes.
- Use Conventional Commits for commit messages.
- Do not silently add major dependencies or architectural frameworks.
- Before implementing fold posture, display classification, or automatic ambient activation, consult `docs/fold6-device-findings.md` and prefer validated physical-device behavior over assumptions about foldable APIs.

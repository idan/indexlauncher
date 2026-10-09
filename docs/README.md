# Documentation guide

Index Launcher is a Kotlin/Jetpack Compose Android **HOME launcher**, inspired by Niagara and Action Launcher before it. A small, vertically scrolling favorites list and speedy alphabetical app access take priority over an icon grid; additional pages provide single-/dual-column widget layouts. Minimalism and speed are the design center.

## Where to start

1. [Project README](../README.md) — user-facing overview, controls, build/run instructions, and MIT license.
2. [Launcher behavior and implementation map](launcher-behavior.md) — implemented behavior, gesture coexistence, persistence, and code entry points.
3. [Platform decisions and future design](platform-decisions.md) — implemented API/appearance decisions, private-space and icon-pack plans, and longer-term ideas.
4. [Widget layout, gallery, and editing design](widgets-design.md) — current sizing/layout/editor contracts, profile-aware hosting, and explicit remaining gaps.
5. [Active TODO](../TODO.md) and [Changelog](../CHANGELOG.md) — pending implementation/validation and completed work, respectively.

## Product direction

- Recent Android only: **minSdk 35** (Android 15), **compileSdk/targetSdk 37** (Android 17). API 37 is not a maximum supported version.
- Keep Home **favorites-first**, vertically scrolling, wallpaper-backed, and grid-free. All apps is reached through a compact alphabetical rail; search is on demand, not a persistent Home text box.
- Up to five widget pages extend **only to the left** of the app page, without wrapping. Wide windows pin the current app view on the right and page widgets independently on the left. Folding returns to the app view. Reducing the count must not erase hidden widgets; home widgets remain independent of it.
- Honor Android package visibility and profile privacy. Do not add `QUERY_ALL_PACKAGES`. Available managed-profile apps/widgets are supported within platform policy; **private space is not implemented**, with initial support planned for API 36+.
- Preserve high-contrast Home styling and system dynamic color in settings, with Material 3 Expressive-style shapes/typography. Home and widget pages should leave room for the wallpaper.

## Verification

From the repository root, with Android SDK Platform 37 and the configured JDK 25 Gradle daemon available:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

App source compatibility is Java 17, but the daemon toolchain is selected separately in `gradle/gradle-daemon-jvm.properties`; see [build instructions](../README.md#build-and-run). Use your local SDK/JDK configuration rather than assuming another contributor's machine paths or emulator name. Keep another HOME launcher installed while testing.

Automated tests and prior API 37 emulator checks are not substitutes for the multi-version, managed-profile, accessibility, and UI integration checks in [TODO](../TODO.md).

## Maintaining these documents

Code is the ground truth for implemented behavior. Label future design explicitly; do not mark a feature implemented because its design is written down. Move completed tasks out of TODO into the changelog, retain rationale/contracts in the relevant design doc, and leave outstanding validation active. Record real release dates only when a release occurs.

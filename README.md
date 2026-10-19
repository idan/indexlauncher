# Index Launcher

Index Launcher is a vertical **list launcher** for Android, inspired by Niagara and Action Launcher before it. Keep a small list of favorites close at hand, reach the rest of your apps quickly through an alphabetical index, and swipe to additional pages for widgets in single- or dual-column layouts. The emphasis is on **minimalism and speed**: less home-screen clutter, fewer steps to the app you want, and room for your wallpaper to breathe.

## Features

- **Favorites first.** A bottom-aligned list of your everyday apps, with a swipe-to-star action for adding and removing favorites. Longer lists scroll; your chosen favorites persist across restarts.
- **Fast alphabetical access.** Tap or scrub the A–Z rail to jump through all apps, with a target bubble that follows your finger and a subtle haptic tick for each new entry while scrubbing. Only populated letters appear. Non-Latin app names keep their original labels, with romanized indexing and search aliases where available.
- **Search on demand.** Swipe up past the bottom of favorites to open focused, keyboard-ready app search, or use the search button in all apps. No permanent search box on Home.
- **Widgets without an icon grid.** Place widgets above favorites or on up to five pages to the left. Mix full-width widgets with pairs of half-width widgets; resize, reorder, and remove them in Edit mode. Hiding a page retains its widgets.
- **Foldable-friendly Home.** Wide windows show widgets on the left while keeping your current favorites, all-apps, or search view on the right. Folding returns to the app screen; the widget pane remembers its page.
- **An app-grouped widget gallery.** Browse providers and previews, including available work-profile widgets where administrator policy permits hosting.
- **Personal and work apps.** Available work apps appear alongside personal apps with badged icons and separate favorites. Pausing a work profile hides its apps without forgetting those favorites.
- **Wallpaper-forward appearance.** Static and live system wallpaper show through Home and widget pages. All apps and search add contrast for readability. Choose Device default, Circle, Rounded square, or Squircle for adaptive app icons; legacy artwork stays intact.

Index Launcher is in development and supports **Android 15+ (API 35)**. Private space, third-party icon packs, and direct favorite reordering are **not implemented**; see the [active backlog](TODO.md).

## Using the launcher

| Action | How |
| --- | --- |
| Browse all apps | Touch or scrub the rail on the right edge. Letters jump to sections; `#` covers numbers and names without a Latin index initial. |
| Return to favorites | Scrub to `★` or press Back in all apps. Back closes a revealed app row first. |
| Add/remove a favorite | Swipe left on an app row, then tap its star. This works in favorites, all apps, and search. |
| Search | Swipe up **past the end** of favorites, or tap the magnifying glass in all apps. Back or the close arrow returns to the previous view; Back may first dismiss the keyboard. |
| Open settings | Use the gear at the bottom of the rail to jump to the launcher tools, then tap **Index Launcher settings**. The gear does not open settings directly. |
| Edit widgets above favorites | Tap **Homescreen Widgets** below the settings tile in all apps. Use Add widget and Done; home long-presses do not open the editor. |
| Visit widget pages | Swipe right from the app page. Pages extend only to the left and never wrap. In wide windows, swipe within the left pane to browse widgets independently. Set the page count to **0–5** in settings. |
| Add/edit a widget on a left-side page | Long-press empty space to open the gallery, or long-press a widget to enter Edit mode. Empty pages also have an Add widget button. |

In widget Edit mode, drag the bottom handle for height or the side handle for half/full width when supported. Arrow controls change order; the trash control asks before removing a widget. Done or Back locks the layout. Providers control their own contents, so giving a widget more space does not guarantee that it will reflow. Home widgets and favorites scroll together in one vertical list, and home widgets temporarily hide in all apps and search.

Settings also opens Android's wallpaper picker. Index Launcher displays the system wallpaper without reading its pixels or requesting storage permissions. Its icon shape choice applies only inside this launcher, not to other launchers or the device's shape picker.

## Build and run

Open the repository in Android Studio with **Android SDK Platform 37 (Android 17)** installed. The app compiles against and targets API 37; this is not a maximum supported Android version.

The Gradle wrapper uses a **JDK 25 daemon**, selected by `gradle/gradle-daemon-jvm.properties`; app source compatibility is Java 17. For command-line builds, put a JDK's `java` on `PATH` or set `JAVA_HOME` (Android Studio's bundled runtime can bootstrap the wrapper). Gradle then selects the configured daemon and can download JDK 25 if needed. Set `ANDROID_HOME` or the SDK path in an untracked `local.properties` file if needed.

```sh
./gradlew :app:assembleDebug
```

Run the `app` configuration in Android Studio, or install `app/build/outputs/apk/debug/app-debug.apk`. Debug builds use `app.indexlauncher.debug` and appear as **Index Launcher (debug)**, so they can coexist with a release build. The release application ID is `app.indexlauncher` and must remain stable once published.

Open Index Launcher from your existing launcher first. To use it as Home, select it under **Settings → Apps → Default apps → Home app** (wording varies by device). Keep another launcher installed while testing.

For unit tests, build, and lint:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

To compile the UI tests without a device, run `./gradlew :app:assembleDebugAndroidTest`.

With an emulator or device connected, run the gesture, rail-haptic, adaptive-pane, search/state-restoration, and favorites/widget scrolling UI tests. Use an unfolded/wide emulator: two-pane tests skip devices with less than 760 dp of screen width.

```sh
./gradlew :app:connectedDebugAndroidTest
```

### Download a CI build

Every push to `main` builds a debug APK with [GitHub Actions](https://github.com/idan/indexlauncher/actions/workflows/build-apk.yml). Open a successful **Build debug APK** run and download the `index-launcher-debug-<commit>` artifact, then unzip it to get `app-debug.apk`. Artifacts are retained for 14 days. You can also start a build using **Run workflow** on the workflow page.

Each successful build also attaches `app-debug.apk` to a [draft release](https://github.com/idan/indexlauncher/releases) whose title and tag are `dev-<SHA>`, where `<SHA>` is the **last six characters** of the built commit's SHA. Re-running the same commit replaces the APK on its existing draft. Draft releases are visible only to users with write access until published.

CI builds use a runner-generated debug signing key, so installing a build from a later run may require uninstalling the previous CI build first (which deletes its app data). These are development builds, not signed release builds.

## Project documentation

- [Documentation guide](docs/README.md) — product direction and where to start contributing.
- [Launcher behavior and implementation map](docs/launcher-behavior.md) — current interactions, persistence, and code entry points.
- [Platform decisions](docs/platform-decisions.md) — Android/profile boundaries, icon customization, and future design.
- [Widget design](docs/widgets-design.md) — layout, sizing, gallery, editing, and known gaps.
- [TODO](TODO.md) — active implementation and validation work only.
- [Changelog](CHANGELOG.md) — completed work in the current development baseline.

## License

[MIT](LICENSE), copyright © 2026 Idan Gazit.

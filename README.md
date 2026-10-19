# Index Launcher

Index Launcher is a vertical **list launcher** for Android, inspired by Niagara and Action Launcher before it. Keep a small list of favorites close at hand, reach the rest of your apps quickly through an alphabetical index, and swipe to additional pages for widgets in single- or dual-column layouts. The emphasis is on **minimalism and speed**: less home-screen clutter, fewer steps to the app you want, and room for your wallpaper to breathe.

## Features

- **Favorites first.** A bottom-aligned list of your everyday apps, with a swipe-to-star action for adding and removing favorites. Longer lists scroll; your chosen favorites persist across restarts.
- **Fast alphabetical access.** Tap or scrub the A–Z rail to jump through all apps, with a target bubble that follows your finger and a subtle haptic tick for each new entry while scrubbing. Only populated letters appear. Non-Latin app names keep their original labels, with romanized indexing and search aliases where available.
- **Search on demand.** Use the search button in all apps to open focused, keyboard-ready app search. Scrolling favorites never opens search, and there is no permanent search box on Home.
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
| Return to favorites | Use Android’s Home button/swipe-up gesture, scrub to `★`, or press Back in all apps. Home and `★` reset the shared widgets/favorites list to the top, even if already showing favorites. Back closes a revealed app row first. |
| Add/remove a favorite | Swipe left on an app row, then tap its star. This works in favorites, all apps, and search. |
| Search | Tap the magnifying glass in all apps. Back or the close arrow returns to the previous view; Back may first dismiss the keyboard. |
| Open settings | Use the gear at the bottom of the rail to jump to the launcher tools, then tap **Index Launcher settings**. The gear does not open settings directly. |
| Edit widgets above favorites | Tap **Homescreen Widgets** below the settings tile in all apps. Use Add widget and Done; home long-presses do not open the editor. |
| Visit widget pages | Swipe right from the app page. Pages extend only to the left and never wrap. In wide windows, swipe within the left pane to browse widgets independently. Set the page count to **0–5** in settings. |
| Add/edit a widget on a left-side page | Long-press empty space to open the gallery, or long-press a widget to enter Edit mode. Empty pages also have an Add widget button. |

In widget Edit mode, drag the bottom handle for height or the side handle for half/full width when supported. Arrow controls change order; the trash control asks before removing a widget. Done or Back locks the layout. Providers control their own contents, so giving a widget more space does not guarantee that it will reflow. Home widgets and favorites scroll together in one vertical list, and home widgets temporarily hide in all apps and search.

Under **Settings → Appearance → Item spacing**, adjust the vertical padding above and below app icons (4–28 dp, default 12 dp). The saved setting applies to favorites, all apps, and search; icons stay the same size and compact rows retain a 48 dp touch target.

**Settings → Appearance → Widget spacing** independently adjusts outside padding, gaps between columns, and gaps between rows (each 0–40 dp). Defaults remain 20, 12, and 16 dp respectively. These settings apply above favorites and on every widget page; padding drawn inside a widget is still controlled by its app.

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

Every push to `main` runs unit tests and lint, builds a signed debug APK with [GitHub Actions](https://github.com/idan/indexlauncher/actions/workflows/build-apk.yml), and publishes a [GitHub prerelease](https://github.com/idan/indexlauncher/releases). Download `app-debug.apk` directly from a release's assets. You can also start a new build using **Run workflow** with the `main` branch selected; other branches do not sign or publish builds.

Build **N** uses the workflow run number as its Android `versionCode`, the version name `0.1.0-dev.N+<short-commit>`, and the tag `dev-N`. A new workflow run gets a higher number even when building the same commit; retrying an existing run keeps its number. This counter belongs to this workflow, not the whole repository: replacing the workflow requires preserving increasing version codes, for example with an offset.

Retries can resume an incomplete draft, but never replace an already-published release's APK or change its release status. Releases are published only after the APK upload succeeds. A successful build also uploads an `index-launcher-debug-<run-number>-<attempt>` Actions artifact containing the APK, retained for 14 days.

CI builds use a persistent development signing key, allowing newer CI builds to update older ones in place while retaining app data. **Switching from an old runner-signed or differently signed local build requires a one-time uninstall, which deletes app data.** Keep another launcher installed while testing. These are debuggable development APKs using `app.indexlauncher.debug`, not production builds.

### Signing configuration

The workflow requires these repository Actions secrets:

| Secret | Value |
| --- | --- |
| `DEV_KEYSTORE_BASE64` | Base64-encoded PKCS12 keystore containing the development private key |
| `DEV_KEYSTORE_PASSWORD` | Keystore password |
| `DEV_KEY_ALIAS` | Key alias, normally `indexlauncher-dev` |
| `DEV_KEY_PASSWORD` | Key password (the same password for this PKCS12 setup) |

Keep the original keystore and password backed up securely outside GitHub. CI decodes the key into a restricted-permission temporary file and removes it after the build; neither the key nor its credentials belong in Git or build artifacts. Missing signing secrets fail the build rather than publishing an APK with a new runner-generated key.

Ordinary local builds still use Android's default local debug key, version code `1`, and version name `0.1.0`. To build an APK compatible with CI installations, explicitly export `DEV_KEYSTORE_PATH` (the absolute path to the backed-up `.p12` file), `DEV_KEYSTORE_PASSWORD`, `DEV_KEY_ALIAS`, and `DEV_KEY_PASSWORD`, and pass `-PciVersionCode=N` with a code no lower than the installed build's. An optional `-PciVersionNameSuffix=-dev.N+<short-commit>` identifies the build. All four signing variables must be supplied together. Do not use an arbitrarily high local version code: later CI builds must catch up before they can update it.

### Promote a tested build

On GitHub, edit the chosen prerelease and clear **This is a pre-release**. You can also select **Set as the latest release**. Leave the tag and APK unchanged: promotion blesses the exact tested build, and retries will not demote it or overwrite its APK.

Promotion changes GitHub metadata only. The APK retains its development version name, signing key, debug package ID, and debuggable status. A future production app needs a separate production build/signing setup. Promoting an older build also does not make it an Android upgrade over a newer installed build; version codes still determine update ordering.

The launcher does not yet check for or install updates itself. External tools tracking this development channel must include prereleases; GitHub's `/releases/latest` endpoint excludes them.

## Project documentation

- [Documentation guide](docs/README.md) — product direction and where to start contributing.
- [Launcher behavior and implementation map](docs/launcher-behavior.md) — current interactions, persistence, and code entry points.
- [Platform decisions](docs/platform-decisions.md) — Android/profile boundaries, icon customization, and future design.
- [Widget design](docs/widgets-design.md) — layout, sizing, gallery, editing, and known gaps.
- [TODO](TODO.md) — active implementation and validation work only.
- [Changelog](CHANGELOG.md) — completed work in the current development baseline.

## License

[MIT](LICENSE), copyright © 2026 Idan Gazit.

# Platform decisions and future design

This document separates **implemented decisions** from **planned features**. [Launcher behavior](launcher-behavior.md) describes the current baseline, [TODO](../TODO.md) tracks active tasks, and [CHANGELOG](../CHANGELOG.md) records completed work. Keep API 35+ Home functional when adding features available only on newer releases.

## Android support, visibility, and profiles

**Implemented:**

- `app/build.gradle.kts` sets `minSdk = 35` and `compileSdk = targetSdk = 37`. API 37 is not a support ceiling. The release application ID is `app.indexlauncher`; debug adds `.debug` so both builds can coexist. Preserve the release ID once published.
- The Gradle wrapper selects a JDK 25 daemon through `gradle/gradle-daemon-jvm.properties`; Java source/target compatibility is 17. These are separate settings, not interchangeable build requirements.
- The manifest declares HOME/DEFAULT and LAUNCHER entry points, a non-exported settings activity, and narrow package queries for launchable activities **and widget providers** (`APPWIDGET_UPDATE`). No `QUERY_ALL_PACKAGES` permission. Backup is disabled in the manifest.
- `InstalledAppsRepository` uses `LauncherApps` for the current user and accessible **managed profiles**, excluding paused profiles and other profile types. Work icons are badged. Launch targets, favorites, search rows, and icon-cache keys distinguish profiles; current-user component IDs retain their old format for saved favorites.
- Package/profile availability callbacks and activity resume refresh the app and gallery catalogs. Saved favorites remain ordered and survive temporary app/profile unavailability. This is **not** private-space support.
- ICU `Any-Latin; Latin-ASCII` supplies non-Latin index keys without changing displayed names. A distinct label from the same activity's English-configured resources is an optional search alias, or an index fallback if romanization has no Latin initial. It is not guaranteed to be an English translation. Managed-profile resource loading still needs device validation.

### Private space (planned)

Initial support is intended for **API 36+**, while the launcher continues to support API 35 personal/work apps.

- Add `ACCESS_HIDDEN_PROFILES` and operate under the default HOME role for private-profile access. Identify the profile with `LauncherApps.getLauncherUserInfo()`; preserve existing profile-aware identities.
- When unlocked, place private apps in a **separate Private space section after ordinary alphabetical apps and before launcher tools**, never interleaved with the ordinary index. Provide a lock control and respond to profile availability and quiet-mode changes through `UserManager.isQuietModeEnabled()` / `requestQuietModeEnabled()`.
- When locked, hide private app names/icons/results in all apps, favorites, and search; immediately clear sensitive caches **without deleting saved favorites**. Guard against in-flight loads repopulating caches or results after locking. Keep an unlock entrypoint unless the system preference hides it; a “private space” search should offer the unlock entry without exposing app details.
- Respect the system's **hide Private space when locked** preference through `LauncherUserInfo.getUserConfig()` / `PRIVATE_SPACE_ENTRYPOINT_HIDDEN`, public starting at API 36. Do not use undocumented API 35 settings. Validate permission/role, lock, hiding, and search semantics on real API 36+ devices before claiming support.

References: [Android 15 private-space launcher requirements](https://developer.android.com/about/versions/15/behavior-changes-all#private-space), [LauncherUserInfo](https://developer.android.com/reference/android/content/pm/LauncherUserInfo).

## Icons and appearance

**Implemented:**

- `AppIconRepository` loads activity-specific icons off-main-thread into an 8 MiB bitmap cache. Cache/load keys include catalog version, shape, and profile-aware app ID. Work badging is applied after shape rendering.
- `IconShapeRepository` persists **Device default, Circle, Rounded square, or Squircle**. Device default lets Android draw the adaptive-icon mask. Custom presets draw adaptive foreground/background layers with launcher-owned masks; legacy single-image artwork stays unmasked to avoid cropping logos. Returning from settings refreshes all app views.
- There is no universal public preference for a user-selected *launcher icon shape* across devices. This launcher does not read or change Pixel's shape picker; Android's themed-icon setting is a separate concept. Third-party packs, per-app icon overrides, and themed app-icon rendering are not implemented. Index Launcher's **own** adaptive icon does include a monochrome variant.
- Settings is a separate dynamic-color `SettingsActivity` with Material 3 Expressive-style shapes and typography. Wallpaper selection is a **system picker action in settings**, not a Home control. Home uses `FLAG_SHOW_WALLPAPER`, with a contrast overlay for all apps/search that fades out while paging to widgets. Favorites and widget pages stay undimmed. No wallpaper pixel access or storage permission is needed.

### Icon packs (planned)

Android has no universal launcher icon-pack API. Choose supported common launcher-theme conventions before adding narrow intent `<queries>`; do not use `QUERY_ALL_PACKAGES`.

Start with component-to-drawable mappings in **`appfilter.xml`**, including resource XML and asset variants. Resolve resources from the selected pack, offer a persistent settings picker, and fall back to each app's original icon when a mapping is absent, loading fails, or the pack is removed. Keep activity/profile distinctions and apply work badges to the final icon.

Shape-vs-pack precedence is an **open decision**. Do not blindly mask arbitrary pack or legacy artwork. Test representative formats before declaring support; dynamic calendar/clock icons and per-app overrides need an explicit initial-release vs follow-up scope decision. Future private icons must obey the same lock-time cache rules as original icons.

Reference: [Adaptive icon design and masks](https://developer.android.com/develop/ui/views/launch/icon_design_adaptive).

## Favorites, widgets, and longer-term ideas

- Favorite toggling and insertion-order persistence are implemented; **direct favorite reordering is active work**. Preserve the ordered representation rather than replacing it with a set, and keep personal/work installations distinct.
- The shared two-column widget flow, Edit mode, app-grouped gallery, and home widgets above favorites are implemented. Sizing contracts and actual gaps are in [widget design](widgets-design.md). Work-provider enumeration does not bypass administrator allowlisting, and managed-profile hosting still needs device validation.
- Page counts are finite and left-only. Reducing the count retains hidden widgets; do not introduce wrapping. Wide windows pin apps on the physical right, with independently paged widgets on the left. Jetpack WindowManager supplies fold/hinge bounds; available pane space and font scale decide whether two panes fit. The `home` key remains independent of widget page count. Old right-side workspace data is intentionally not migrated.
- **Longer-term ideas, not current release commitments:** app shortcuts/intents in the revealed row action area, advanced search beyond app-name aliases, widget drag-to-reorder, and per-app icon overrides. The star is the only implemented row action. Any new row action must preserve vertical scrolling, rightward paging from apps, and accessibility equivalents.

## Validation policy

The existing suite covers JVM models, geometry/spacing logic, icon-shape persistence parsing, and widget packing/resizing/serialization. UI regression tests cover row/pager arbitration, rail-haptic requests, adaptive panes, search/state restoration, and shared home-widget/favorites scrolling. These tests and prior API 37 emulator checks do not establish complete API 35/36, managed-profile, provider-hosting, accessibility, or real-device foldable coverage. Keep these gaps in [TODO](../TODO.md) rather than treating implementation as release validation.

For each new feature, establish its API floor and visibility/permission requirements first; then specify persistence/cache migrations, unavailable-app/provider/pack fallbacks, focused tests, and on-device gesture/privacy checks. Update the behavior contract, design notes, active backlog, and changelog together.

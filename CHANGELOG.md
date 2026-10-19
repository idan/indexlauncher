# Changelog

Completed work is recorded here; pending work lives in [TODO.md](TODO.md). This initial entry summarizes the implemented development baseline, not a published release or a claim of full device coverage. The current build version is `0.1.0`.

## Unreleased

### Apps and navigation

- Android’s Home button/gesture now returns directly to favorites from all apps, search, or compact widget pages, without resetting the view on ordinary resumes or replaying the action after recreation. Home and selecting `★` also reset favorites to the very top, including offsets inside oversized home widgets.
- Made Home favorites-first, with a bottom-aligned vertical list and persistent insertion order. Left-swipe star actions add/remove favorites in Home, all apps, and search, with an accessibility equivalent.
- Added tap/scrub alphabetical navigation with a finger-following target bubble, populated-letter filtering, a `#` fallback, and sticky letter pills. The star and Back return to favorites; the gear jumps to launcher tools rather than opening settings directly.
- Added Android ICU romanization for non-Latin app indexing/sorting and alternate activity labels from English-configured resources for search where available, without replacing displayed names.
- Added available managed-profile apps, work-badged icons, profile-aware launch/favorite/search/cache identities, and package/profile availability refresh. Unavailable apps remain saved as favorites but are not displayed.
- Added an all-apps search button, focused app search, live name filtering, empty-results feedback, and return to the originating view. Removed the easily triggered swipe-up-to-search gesture; scrolling favorites no longer opens search.
- Kept rightward paging separate from leftward row actions, consistently in favorites, all apps, search, and RTL layouts. Widget pages extend only to the left, with a configurable count of 0–5 and no wraparound.
- Added adaptive Home: independently paged widgets on the left and a pinned app pane on the right in wide windows, with vertical-hinge exclusion and narrow/tabletop fallbacks. Folding returns to apps; app state and the selected widget page survive pane changes. Keyboard visibility does not change pane mode.
- Added page-order/pane-geometry unit tests and emulator UI coverage for row/pager arbitration, rail-haptic requests, pane changes, logical-page clamping, page-local state, and search query/result-scroll restoration.

### Widgets

- Added AppWidgetHost lifecycle management, binding consent, provider configuration, pending-selection restoration, and canceled/failed widget-ID cleanup.
- Added persistent home widgets above favorites, sharing one bottom-aligned vertical list and scrolling together, with temporary hiding during all apps/search. The Homescreen Widgets tool opens home Edit mode even with no widgets.
- Added shared single-/dual-column widget flow for home and left-side pages: full-width widgets, paired half-width widgets, provider-informed automatic sizing, and saved width/height/order choices.
- Added page-local Edit mode with height/width drag handles, reorder arrows, accessible sizing actions, removal confirmation, and Done/Back to lock. Left-side pages support long-press add/edit; home uses its launcher tool instead.
- Added an app-grouped, profile-aware widget gallery, including provider-only apps, generated/layout/image previews and fallback artwork. Work hosting remains subject to administrator allowlisting.
- Preserved widget bindings when reading ID-only placements and retained placements on hidden pages. Left-side pages now use fresh `left-N` keys and workspace settings, without migrating the old right-side workspace.

### Appearance and project setup

- Added persistent development signing and workflow-run-number versions for CI APKs, with automatic GitHub prereleases, retry-safe draft uploads, and preservation of published/promoted builds. Local builds retain their default debug signing and version unless explicitly configured.
- Added saved widget-spacing controls for outside padding and independent row/column gaps (0–40 dp), shared by home widgets and left-side widget pages without changing provider-internal padding.
- Added saved item-spacing controls for favorites, all apps, and search (4–28 dp vertical padding, keeping icons fixed-size and touch targets at least 48 dp).
- Displayed static/live system wallpaper without pixel access or storage permissions. Favorites and widget pages stay undimmed; all apps/search use a contrast scrim. Rail idle fade, return-home fade, and paging suppression keep navigation unobtrusive.
- Added persistent adaptive-icon shapes: Device default, Circle, Rounded square, and Squircle. Legacy icons retain their artwork; icon loading is off-main-thread with a bounded bitmap cache and work badging.
- Added a separate dynamic-color settings screen with wallpaper picker and widget-page count controls, plus an adaptive launcher icon with a monochrome variant.
- Established Android 15+ support with compile/target API 37, separate debug application identity, and JVM tests for app indexing/search, favorites, page identities, rail geometry, item/widget-spacing settings, icon-shape settings, widget packing/resizing, and placement serialization.
- Added an MIT license and reorganized documentation into a user-facing overview, current behavior/design references, and an active-only backlog.

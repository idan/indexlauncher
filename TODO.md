# Active TODO

Only incomplete implementation and validation work belongs here. Completed work is in [CHANGELOG.md](CHANGELOG.md); rationale and future ideas belong in [platform decisions](docs/platform-decisions.md) and [widget design](docs/widgets-design.md). Implemented does not mean verified across every supported Android version.

## Implementation

- [ ] Implement private-space discovery and a separate section/lock control, initially on API 36+ under the default-Home role. Do not mix private apps into ordinary A–Z sections.
- [ ] Implement private-space privacy across lists, favorites, search, and caches, including lock/unlock, availability changes, the system's hidden-entrypoint preference, and a “private space” unlock search result without exposing locked app details. See the [private-space design](docs/platform-decisions.md#private-space-planned).
- [ ] Add third-party icon-pack discovery, a persistent settings picker, common `appfilter.xml` mappings, and original-icon fallback for missing mappings, load failures, or pack removal. Decide supported formats and shape/pack precedence first; see [icon-pack design](docs/platform-decisions.md#icon-packs-planned).
- [ ] Add widget constraint feedback when even full width cannot satisfy provider metadata or a saved height is clamped. The current editor explains unavailable half-width and non-reflowing content, but not these adjustments. See [remaining widget work](docs/widgets-design.md#remaining-work).

## Validation and release checks

- [ ] Verify the first development-key-signed GitHub build and a subsequent higher-version build on a phone: confirm in-place installation retains favorites/settings/widgets, then promote a prerelease and verify a workflow retry leaves its APK and release status unchanged.
- [ ] Test current favorites, search/keyboard/Back transitions, rail timing/haptics, row gestures vs paging, widget editing, persistence, and accessibility on API 35, 36, and 37 devices/emulators. Add instrumentation/state-transition coverage where practical; JVM tests do not cover Android UI integration.
- [ ] Validate real-device fold/unfold transitions (favorites, all apps, search/IME and scroll position), off-center/occluding hinges, tabletop/rotation, split-screen, large font/display settings, widget provider reflow, TalkBack, and simultaneous app/widget Back handling. API 37 fold-emulator tests cover synthetic pane resizing and saved-state restoration; a manual outer/inner display search/IME round trip also preserved the query. This is not full hardware/provider coverage.
- [ ] Visually check all implemented icon shapes on light/dark wallpapers, including legacy icons and personal/work badges. When packs/private space land, verify their artwork and badge fallbacks too.
- [ ] Validate alternate-locale launcher activity labels on personal/work installs: same-package variants, missing translations, activity-specific labels, romanized sorting, and search aliases without changing displayed labels.
- [ ] Test work-widget enumeration, previews, consent, and configuration with allowlisted and denied providers, duplicate personal/work installs, and provider-only apps. Exercise profile pause/disappearance while browsing or binding, cancellation, and activity recreation.
- [ ] Test widget size reporting, half/full reflow, legacy placement migration, hidden/revealed pages, shared home-widget/favorites scrolling, and normal provider interaction across portrait/landscape, narrow windows, and large font/display settings. Include widgets with different resize modes and oversized metadata.
- [ ] After private-space implementation, verify default-Home role/permission boundaries, locked/hidden behavior, search privacy, and immediate sensitive-cache clearing on API 36+; ensure API 35 retains personal/work behavior without newer-API calls.
- [ ] After icon-pack implementation, test representative formats, missing/broken mappings, uninstall fallback, and the chosen shape/pack precedence.

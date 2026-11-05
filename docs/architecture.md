# InkNest architecture

InkNest follows the unidirectional data flow used by Now in Android:
repositories expose data; ViewModels produce observable UI state; routes collect state
with lifecycle awareness and connect actions; screens render state and callbacks.

## Boundaries

- `data/repository`: data contracts and implementations. DataStore owns saved preferences;
  source settings have a separate contract. Asset acknowledgements are loaded by a
  main-safe repository with an injectable IO dispatcher, never read inside a screen/route.
- `di`: application-scoped dependencies and ViewModel factories. Runtime wiring uses
  real platform dependencies in both build types. ReadingDataContainer shares the existing
  DataStore and Room instance. Graph initialization and interrupted temp-file cleanup run off main.
- `ui/settings`: `SettingsViewModel` owns one public `SettingsUiState`, including initial
  preference read failures, source actions and acknowledgements loading/retry. Internal
  cached preference state retains the last snapshot across temporary collection stops.
  `WhileSubscribed(5_000)` scopes upstream collection to active UI subscribers.
- `SettingsRoute`: lifecycle-aware collection, navigation, platform intents and ephemeral
  folder/confirmation dialogs. `SettingsScreen` and its content helpers receive only
  state/callbacks; they do not access repositories or perform IO.
- `ui/onboarding`: the route owns wizard collection and Back handling. The app supplies
  display preference state/actions; the route does not depend on another feature's ViewModel.
- Debug diagnostics have their own routes in `src/debug`; release has no probe controls.
  Runtime mock/preview sources are absent. StrictMode policies exist only in debug.
- `ui/InkNestApp`: composition root for theme, top-level navigation and feature routes.
  `ui/components` and `ui/theme` own shared UI behavior and design tokens.

## Deliberate scope

Keep one `:app` module and manual constructor injection at the current project size.
Do not add Hilt, a domain layer or pass-through use cases solely to resemble a sample's
module graph. Use repositories directly for simple preference/source operations.
Repository failures must not replace saved values with pretend success, and UI labels
remain Android string resources. Preserve cancellation when handling coroutine failures.

Settings has four categories: Reading & data, Appearance, Launcher and About. This is
product organization, separate from repository boundaries. Combining menus does not
combine preference storage with reading-source/cache storage.

Reference inspected locally: `C:/Users/thanh/Downloads/nowinandroid`, especially
`docs/ArchitectureLearningJourney.md`, `feature/settings/impl/.../SettingsViewModel.kt`
and `feature/foryou/impl/.../ForYouViewModel.kt`. Follow those separation/state principles
without copying the sample's scale or unrelated infrastructure.

## Reading data

Room owns current/history, full-folder inventory and statistics snapshots. DataStore owns
pins/preferences/source selection; no second store is created. Statistics is independent
of current-book parsing and inventory membership. Routes do not scan files or aggregate
statistics. See [Library](library.md), [Statistics](statistics.md) and
[Reading cache](reading-cache.md) for refresh/cancellation and source boundaries.

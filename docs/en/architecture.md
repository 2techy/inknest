# InkNest architecture

[Tiếng Việt](../vi/architecture.md) · [Documentation](README.md)

InkNest uses unidirectional data flow:
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
- Debug builds enable StrictMode policies; release builds provide no-op runtime diagnostics.
  Synthetic fixtures are confined to benchmark/profile source sets.
- `ui/InkNestApp`: composition root for theme, top-level navigation and feature routes.
  `ui/components` and `ui/theme` own shared UI behavior and design tokens.

## Deliberate scope

The production application uses one `:app` module and manual constructor injection. The reading database uses Room schema 6
with destructive cache fallback during development; preferences, pins and source selections
remain in DataStore. Stable upgrade compatibility is not promised.
ViewModels call repositories directly for preference and source operations.
Repository failures must not replace saved values with pretend success, and UI labels
remain Android string resources. Preserve cancellation when handling coroutine failures.

Settings has four categories: Reading & data, Appearance, Launcher and About. This is
product organization, separate from repository boundaries. Combining menus does not
combine preference storage with reading-source/cache storage.

## Reading data

Room owns current/history, full-folder inventory and statistics snapshots. DataStore owns
pins, preferences and source selection. Statistics is independent
of current-book parsing and inventory membership. Routes do not scan files or aggregate
statistics. See [Library](library.md), [Statistics](statistics.md) and
[Reading cache](reading-cache.md) for refresh/cancellation and source boundaries.


## State and work ownership

Settings and onboarding each own a SourceSetupStateHolder. They share saved source data through
repositories, while picker requests, in-flight actions and errors belong to each owner. Onboarding
exposes a single UiState; navigation records FirstRun versus Review and completion only closes
an open wizard. HOME bypasses setup for the current session without persisting completion.

A Compose STARTED effect calls AppViewModel, which delegates foreground reading/statistics updates
to an application-scoped ForegroundRefreshCoordinator. Activity handles platform intents, not
refresh sequencing. Startup has one owner; actual source changes and explicit refresh actions
remain repository operations. Concurrent callers share jobs without a freshness TTL.
AppViewModel also exposes app preferences for the theme/setup gate. The root does not
collect Settings' source/actions state; SettingsRoute owns that collection. Onboarding
completion is observed in its own composition scope so source updates do not invalidate
the entire Home scaffold.

Local Room/DataStore observations recover expected I/O/SQLite failures before sharing state.
Backoff is 1/2/4/8/16/30 seconds, cancellation is preserved and manual retry wakes pending reads.
Recovery never publishes an invented empty snapshot or initiates a SAF scan. Home combines the
independent repository states and keeps unaffected pins/stats/reading content.

ReadingOpenRepository prepares a source-validated, Android-independent opening request. Home and
Library ViewModels own ReadingOpenStateHolder; routes claim each Ready request once and dispatch
through ReadingItemDispatcher only while resumed. Pause/disposal cancels preparation and drops
pending requests. Intents and Context remain in the platform layer; requests are not persisted.

Source capability results and exception mapping live in data/source; source implementations do
not import repositories. Dispatchers are constructor dependencies. Repository and StatsViewModel
share a StatisticsClock from DI; tests replace it and dispatchers without changing system time.

The production graph remains in `:app`. The separate `:benchmark` instrumentation module
drives release-like benchmark and unminified profile variants through the real UI and SAF
boundaries. Synthetic setup exists only in those target source sets; release contains app
profiles but no fixture or setup entry point. See [benchmarking.md](benchmarking.md).

App language is platform configuration: AppLanguageController delegates to AppCompat, with
native per-app locale storage on Android 13+ and automatic AppCompat storage below it.
SettingsRoute combines that platform selection into screen state and owns the transient
language dialog. No locale is persisted in Room or app-preferences DataStore. Activity
recreation retains navigation and ViewModels; language changes never replay selection actions.

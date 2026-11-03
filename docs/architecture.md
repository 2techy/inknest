# InkNest architecture

InkNest follows the unidirectional data flow used by Now in Android:
repositories expose data; ViewModels produce observable UI state; routes collect state
with lifecycle awareness and connect actions; screens render state and callbacks.

## Boundaries

- `data/repository`: data contracts and implementations. DataStore owns saved preferences;
  source settings have a separate contract. Asset acknowledgements are loaded by a
  main-safe repository with an injectable IO dispatcher, never read inside a screen/route.
- `di`: application-scoped dependencies and ViewModel factories. Runtime wiring chooses
  real platform dependencies; debug/release source sets choose preview implementations.
- `ui/settings`: `SettingsViewModel` owns one public `SettingsUiState`, including initial
  preference read failures, source actions and acknowledgements loading/retry. Internal
  cached preference state retains the last snapshot across temporary collection stops.
  `WhileSubscribed(5_000)` scopes upstream collection to active UI subscribers.
- `SettingsRoute`: lifecycle-aware collection, navigation, platform intents and ephemeral
  folder/confirmation dialogs. `SettingsScreen` and its content helpers receive only
  state/callbacks; they do not access repositories or perform IO.
- `ui/onboarding`: the route owns wizard collection and Back handling. The app supplies
  display preference state/actions; the route does not depend on another feature's ViewModel.
- Debug preview has its own route in `src/debug`. Production Settings accepts a composable
  slot and does not depend on HomeViewModel or the source preview controller. The release
  slot is empty, without preview collection or controls.
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

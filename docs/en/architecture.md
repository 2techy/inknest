# InkNest architecture

[Tiếng Việt](../vi/architecture.md) · [Documentation](README.md)

Ownership and lifecycle contracts for the single production `:app` module, using Kotlin/Compose and manual constructor injection.

## Boundaries

| Layer | Responsibility |
| --- | --- |
| `data/source` | SAF access, bounded parsers, capability results and expected-error mapping; never imports repositories. |
| `data/repository` | Observable application data, source configuration, refresh and writes. |
| ViewModel | UI state and actions; calls repositories directly, without pass-through use cases. |
| Route | Lifecycle-aware collection, navigation, platform dispatch and transient dialogs. |
| Screen | Renders state/callbacks; no repositories, file I/O or parsing. |
| `di` | Shared DataStore, Room, loaders, clocks, dispatchers and ViewModel factories. |
| `ui/components`, `ui/theme` | Shared controls, gestures and design tokens. |

Graph initialization and interrupted-temp-file cleanup run off main. Asynchronous components receive dispatchers; repository and StatsViewModel share an injectable `StatisticsClock`.
`BookProbeDataSource` shares document resolution, EPUB/sidecar reads and checksum sampling. Feature-only helpers stay beside their feature; book models live with `ReadingItem`.

## Storage and observation

- Room schema 8 caches current/history, folder inventory and statistics. Development uses destructive cache fallback; stable upgrade compatibility is not promised.
- DataStore separately owns pins, preferences and source selections. Locale uses AppCompat storage, not Room/DataStore.
- Recover expected I/O/SQLite observation failures before sharing flows, with 1/2/4/8/16/30-second backoff and manual retry. Retain successful snapshots; recovery never invents empty data or starts a SAF scan.
- Preserve coroutine cancellation. Failed writes expose errors and never report success or replace saved values.

See [reading cache](reading-cache.md), [Library](library.md), [Statistics](statistics.md) and [source integration](koreader-integration.md) for feature contracts.

## State and work ownership

- A Compose STARTED effect calls AppViewModel → `ForegroundRefreshCoordinator` for foreground reading/statistics refresh. Activity owns HOME intents. Concurrent repository callers share jobs without a freshness TTL; tab changes never scan sources.
- Root theme/setup observes app preferences, not Settings source/action state. Onboarding completion has its own composition scope so source updates do not invalidate Home.
- Settings exposes one `SettingsUiState`; cached preference state retains its last snapshot across collection stops, with `WhileSubscribed(5_000)`. Acknowledgements load through a main-safe repository with independent retry/cache per component.
- Settings and onboarding own separate `SourceSetupStateHolder` instances. Repositories share saved config; each holder owns picker requests, errors and in-flight actions. Onboarding distinguishes FirstRun/Review; completion closes only an open wizard. HOME skips it for the session without saving completion.
- Home/Library ViewModels own `ReadingOpenStateHolder`. `ReadingOpenRepository` prepares Android-independent requests; resumed Routes claim each Ready request once and dispatch through `ReadingItemDispatcher`. Pause, HOME or disposal cancels pending work; requests are not persisted. Context/Intent stay in `platform`.

## Platform services

- `AppLanguageController` delegates to AppCompat: native per-app storage on Android 13+, automatic AppCompat storage below it. SettingsRoute owns the language dialog. Activity recreation retains navigation/ViewModels without replaying actions. AppCompat storage may produce debug StrictMode disk-access diagnostics on Android 12 and earlier. Only en/vi resources are packaged, with language splitting disabled; app metadata reloads after locale changes without rescanning books.
- `AppUpdateRepository` uses a GitHub source and separate keys in the existing DataStore. Settings owns update state; an independent STARTED effect delegates foreground checks through AppViewModel. Attempt/rate-limit times persist. Backgrounding cancels downloads; verified APKs remain private for explicit installation. Platform verification and FileProvider sharing are limited to the update directory.
- SettingsViewModel parses release-note Markdown using injected CommonMark off main. Screens render immutable Compose content without WebView or remote images; Routes dispatch only web links. The bundled BSD notice appears in acknowledgements. All build types share download/verification; unknown version names do not imply a newer release, and APK version/signature checks still apply. See [setup](setup.md) and [release](release.md).
- Debug enables StrictMode; release diagnostics are no-ops. The separate `:benchmark` module drives real UI/SAF against benchmark/profile variants. Fixtures/setup stay in those source sets; release contains generated profiles only. See [benchmarking](benchmarking.md).

No background update service, network sync or telemetry is configured.

# Working in InkNest

Repository-wide instructions for coding agents, following the human/agent documentation
separation described at [AGENTS.md](https://agents.md/). Use [README.md](README.md) for
setup and product usage. Keep shared rules here; no nested guides are currently needed.

## Context and repository map

InkNest Launcher is an offline Android launcher focused on reading and E-Ink devices.
Keep one production `:app` module, Kotlin/Compose and manual DI. `:benchmark` contains
instrumentation for separate performance-only app variants.
Package paths below are relative to `app/src/main/java/dev/twotechy/inknest/`:

| Path | Responsibility |
| --- | --- |
| `InkNestApplication.kt`, `MainActivity.kt` | Container initialization, Compose entry, HOME intents |
| `di/` | Shared graph, resources and ViewModel factories |
| `model/` | Application models and UI-independent request/result types |
| `data/repository/` | Contracts, observable data, refresh/import and source configuration |
| `data/source/` | SAF, bounded parsers and source capability/error types |
| `data/local/` | Preferences DataStore and Room reading/library/statistics cache |
| `platform/` | Android intents, image loaders, package integration and clocks |
| `ui/<feature>/` | ViewModel, UI state, Route and Screen |
| `ui/components/`, `ui/theme/` | Shared controls, gestures and design tokens |

Other important paths:

- `app/src/main/res/values/strings.xml`: production English text.
- `app/src/debug/`, `app/src/release/`: StrictMode versus no-op runtime diagnostics.
- `app/src/test/`, `app/src/androidTest/`: JVM and Android integration tests.
- `app/src/benchmark/`: synthetic fixture/setup, excluded from normal debug/release.
- `benchmark/src/main/`: Macrobenchmark, profile generation and device smoke tests.
- `app/schemas/`: exported Room schema history, not migration implementations.
- `app/src/main/generated/baselineProfiles/`: versioned, generated app profiles.
- `gradle/libs.versions.toml`: grouped dependency/plugin versions and aliases.
- `docs/`: detailed architecture, source contracts, design, setup and release guides.

## Architecture boundaries

- Follow Repository → ViewModel → UI state → Route/Screen. See `ui/home/HomeRoute.kt`
  and `ui/settings/SettingsViewModel.kt`. Screens receive state/callbacks, not repositories.
- Keep constructor injection and factories in `di/`. Reuse the existing DataStore,
  Room instance, image loaders and clocks. Do not add Hilt, production modules or
  pass-through use cases merely to reproduce another project's module graph.
- `data/source` must not import `data/repository`. Source types stay at the source
  boundary; repositories translate them into application state.
- Keep parsing, SAF traversal and database work main-safe. Inject dispatchers in
  changed asynchronous components and use `StatisticsClock` for statistics dates/zones.
- Preserve cancellation. Recover expected I/O/SQLite observation failures before sharing
  flows; retain successful snapshots and expose errors instead of fabricated empty data.
  Failed writes must not report success. Reuse `ObservationRecovery.kt`.
- Foreground refresh belongs to `ForegroundRefreshCoordinator`, called through
  `AppViewModel` and the Compose STARTED effect. Activity owns HOME intent handling.
  Preserve repository deduplication; tab changes must not initiate source scans.
- Root theme/setup observes app preferences, not full Settings source/action state.
  Keep onboarding completion observation scoped to avoid invalidating Home on source updates.
- Settings and onboarding own separate `SourceSetupStateHolder` instances. Share saved
  source data through repositories, not transient picker/error state.
- Book opening uses `ReadingOpenRepository`, VM-owned `ReadingOpenStateHolder` and
  `platform/ReadingItemDispatcher`. Keep Context/Intent out of ViewModels. Preserve
  one-time RESUMED dispatch, grant/source rechecks and pause/HOME/disposal cancellation.

## Development commands

Run from the repository root. Java 25 is specified in
`gradle/gradle-daemon-jvm.properties`; use a compatible JDK or Studio's bundled JBR.
Install Android SDK Platform 37 and Platform-Tools. Gradle resolves dependencies without
a separate install command. SDK location belongs in untracked `local.properties`.

Commands use Windows PowerShell. On POSIX use `bash ./gradlew` instead of
`.\gradlew.bat`; the wrapper currently has no executable bit in Git.

| Purpose | Command |
| --- | --- |
| Check toolchain | `.\gradlew.bat --version` |
| Check / apply Kotlin formatting | `.\gradlew.bat spotlessCheck` / `.\gradlew.bat spotlessApply` |
| Debug / unsigned optimized release | `.\gradlew.bat :app:assembleDebug :app:assembleRelease` |
| Kotlin compilation | `.\gradlew.bat :app:compileDebugKotlin` |
| JVM tests | `.\gradlew.bat :app:testDebugUnitTest` |
| Focused JVM test | `.\gradlew.bat :app:testDebugUnitTest --tests '*StatsViewModelTest'` |
| Android lint | `.\gradlew.bat :app:lintDebug :app:lintRelease` |
| Compile Android tests without a device | `.\gradlew.bat :app:assembleDebugAndroidTest` |
| Connected Android tests | `.\gradlew.bat :app:connectedDebugAndroidTest` |
| Performance APKs | `.\gradlew.bat :app:assembleBenchmark :benchmark:assembleBenchmark` |

Install on an authorized device using `:app:installDebug` or the [development guide's ADB command](docs/en/development.md).
With multiple devices, specify an ADB serial for installs and `-Device` for the benchmark
runner. Read native tests before running them on personal devices: they may exercise
provider grants/lifecycle. See [benchmarking](docs/en/benchmarking.md) for fixture permissions.
Do not invent a `:benchmark:lintBenchmark` task; this test module has no such task.

## Conventions and verification

- Follow `.editorconfig` and Spotless/ktlint: four spaces, UTF-8/LF, explicit imports,
  trailing commas and PascalCase composables. Avoid manual formatting exceptions.
- Use meaningful implementation names, such as `SafReadingOpenRepository` and
  `DataStoreUserPreferencesRepository`. Keep feature helpers beside their feature;
  put genuinely shared behavior in `ui/components`.
- Use string resources and `ui/theme` tokens, including gesture thresholds. Preserve
  static E-Ink transitions; Standard changes icon color, not navigation/layout contracts.
- Add dependencies to the appropriate commented catalog group and use module aliases.
  Room/KSP implementation code is generated during builds; never hand-edit build output.
- Add tests for meaningful behavior: failed writes, restoration, cancellation, source
  isolation and retained snapshots. Avoid tests that merely repeat drawing constants.
  Follow existing `*Test.kt` files; native Room/SAF behavior belongs in `androidTest`.
- Coroutine tests use `kotlinx-coroutines-test`; share the test scheduler across injected
  dispatchers and fake clocks. See `ui/stats/StatsViewModelTest.kt` in the test source set.
- For code changes, run format checks, relevant tests and affected debug/release builds
  and lint. Do not edit Kotlin while lint is running. For documentation-only work,
  validate links, commands and the diff rather than rerunning a device matrix.
- Use CodeScene safeguard on the complete code change set and review new/substantially
  changed classes when the integration is available. Fix degradation and report missing
  tooling explicitly; there is no Gradle task replacing the MCP safeguard.
- Never claim a check passed unless executed successfully. Distinguish dry runs from
  executed tests and emulator screenshots from physical E-Ink acceptance.

## Data, generated files and release boundaries

- Room is currently schema 6 with destructive cache fallback during development.
  DataStore owns pins/preferences/source selections separately. Do not silently change
  schemas or add migrations; stable upgrade behavior must be decided before stable release.
- Generate schemas through Room/KSP and profiles through the benchmark workflow.
  Do not hand-edit generated JSON/profile rules or delete schema history incidentally.
- Preserve primary-storage SAF containment checks, persisted read grants and bounded
  parsers. Do not replace a missing grant with a guessed path or broad storage-root access.
  Preparing an open request must not parse or copy the entire book.
- Keep fixtures, setup entry points and broad storage permissions in benchmark/profile
  source sets. Normal debug/release use real sources. Release excludes debug diagnostics.
  Preserve the unused LuaJ JSR-223 service exclusion in packaging.
- Do not log/commit private book contents, document URIs, keystores, passwords, signed APKs
  or mappings. `.gitignore` covers outputs/signing files and `app/release/`. Keep `plan.md`
  local via `.git/info/exclude` and measurement artifacts under ignored build paths.
- No network sync/telemetry is configured. Do not add either as an incidental change.
  The project uses the MIT license. Preserve third-party notices; signing is separate from licensing.

## Working protocol and completion

1. Read these instructions, affected code and the relevant linked contract.
2. Inspect Git status and preserve existing work. Plan nontrivial changes before editing.
3. Make the smallest coherent change; avoid unrelated refactors and dependency churn.
4. Update necessary tests/docs and run proportional verification. Do not suppress errors
   or weaken validation to make checks pass.
5. Inspect the final diff for generated noise, sensitive data and unintended changes.
6. Report changed files/behavior, checks actually run, skipped checks with reasons and
   material remaining risks. Clarify only critical ambiguities unresolved by evidence.

Do not commit, push, tag or publish unless requested. When committing, use Conventional
Commits without phase/milestone labels, matching existing Git history.

## Detailed contracts

- [Architecture](docs/en/architecture.md), [design system](docs/en/design-system.md)
- [Source integration](docs/en/koreader-integration.md), [reading cache/opening](docs/en/reading-cache.md)
- [Library](docs/en/library.md), [statistics](docs/en/statistics.md), [setup](docs/en/setup.md)
- [Release/signing](docs/en/release.md), [reliability](docs/en/reliability.md)
- [Benchmarks and profiles](docs/en/benchmarking.md)

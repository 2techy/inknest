# InkNest

**InkNest Launcher** is an offline, reading-first Android launcher by [2techy](https://2techy.dev).
It brings KOReader progress, a book library, reading statistics and pinned apps onto an
E-Ink-oriented Home screen. Books open in KOReader; InkNest does not include a reader engine.

InkNest is in active development (`0.1.0-dev`) and requires Android 8.0+ (API 26).
The primary development target is iReader Tango 2; Samsung Galaxy S25 Ultra is the
secondary target. Device-specific reading and E-Ink behavior need physical verification.

## Features

- **Home:** clock/date, current book/progress, continue reading, mini stats and pinned apps.
- **Library:** recursively scan multiple book folders, including books never opened;
  grid/list browsing, search, folder/status filters, sorting and opening in KOReader.
  EPUB metadata/raster covers are extracted; other formats use filename-based entries
  and placeholders where metadata is unavailable. See [format support](docs/library.md).
- **Statistics:** time/pages, daily average, streak, calendar totals, activity heatmap
  and daily/weekly/monthly/yearly chart views based on KOReader statistics.
- **Apps/launcher:** find and launch installed apps, pin/reorder shortcuts and optionally
  select InkNest as Android's default Home app.
- **Settings/setup:** optional onboarding, reading-source folders, E-Ink grayscale or
  Standard color icons and toggles for Home stats/pins. Both modes use static transitions.
- **Gestures:** swipe between main tabs and pull to refresh on supported screens,
  alongside navigation and refresh controls.

Apps/pins work without a reading source. Reading integration currently supports KOReader
(`org.koreader.launcher`) and primary internal storage via Android ExternalStorageProvider.
SD/cloud providers, reader write-back, network synchronization and OEM display-refresh APIs
are outside the current implementation.

## Build and run

### Prerequisites

- Android Studio, Android SDK Platform 37 and SDK Platform-Tools.
- Java 25, specified in [the daemon configuration](gradle/gradle-daemon-jvm.properties).
  Use a compatible JDK or Studio's bundled JBR. Java 11 in the app's compile options
  is the bytecode compatibility target, not the build JDK requirement.
- An Android 8.0+ device/emulator with debugging authorized for installation.

Open this checkout in Android Studio and sync Gradle. Configure the SDK through Studio
or untracked `local.properties`. Gradle resolves dependencies; no API keys, backend service
or separate package-install step is required. First sync needs access to the configured
Gradle/Maven repositories.

Windows PowerShell, if Java is not already configured:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
```

On macOS/Linux use `bash ./gradlew` instead of `.\gradlew.bat`. Build/dependency versions
are maintained in the [wrapper configuration](gradle/wrapper/gradle-wrapper.properties)
and [version catalog](gradle/libs.versions.toml).

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Select `app` in Android Studio
and run on your device, or install/launch from the repository root with ADB:

```powershell
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.twotechy.inknest/.MainActivity
```

With multiple devices, add `-s SERIAL` after `adb`, using the ID from `adb devices -l`.
Another signing key cannot update an existing installation; avoid uninstalling blindly
because it deletes local settings.

### Connect reading data

1. Install KOReader separately and open a book so its data directory exists.
2. During onboarding or in **Settings → Reading & data**, select KOReader and its data
   folder containing `settings.reader.lua` or `history.lua`.
3. Add book folders through the system picker and refresh Library. Statistics also
   requires KOReader's `settings/statistics.sqlite3`.
4. Optionally choose InkNest as the default launcher in **Settings → Launcher**.

Paths vary by device; no fixed `/koreader` or `/webdav` path is assumed. Access is limited
to granted trees. Failed refreshes retain cached content, but KOReader may not yet have
flushed its latest session. See [setup and recovery](docs/setup.md) for changing folders,
clearing cache, reviewing setup and restoring your previous default launcher.

## Architecture and layout

The production app uses Kotlin, Compose/Material 3, lifecycle-aware ViewModels,
coroutines/Flow and manual DI. Repositories feed UI state; Routes connect platform actions,
and Screens render state/callbacks. Room 3 caches reading/library/statistics, Preferences
DataStore saves settings/pins/source selections, and bounded LuaJ/EPUB/SQLite adapters
read KOReader files. Normal debug/release builds have no runtime mock-data mode.

```text
app/src/main/
  java/dev/twotechy/inknest/
    di/                  Shared graph and ViewModel factories
    data/                Repositories, local storage and source adapters
    model/               Application models
    platform/            Android integration, image loading and clocks
    ui/                  Feature state/routes/screens, shared controls and theme
  res/                   UI resources
  assets/licenses/       Third-party notices
  generated/baselineProfiles/  Versioned, generated app profiles
app/src/debug/           StrictMode diagnostics
app/src/release/         No-op diagnostics
app/src/test/            JVM tests
app/src/androidTest/     Native integration/lifecycle tests
app/src/benchmark/       Isolated synthetic fixture/setup for performance variants
app/schemas/             Exported Room schema snapshots
benchmark/               Performance instrumentation module
scripts/benchmark.ps1    Device benchmark/profile runner
docs/                    Detailed guides and implementation contracts
```

Room currently uses schema **6** with destructive cache rebuilding during development;
pins/preferences/source selections are stored separately. Stable upgrade compatibility
is not promised for this development version. Read [architecture](docs/architecture.md)
and [reading cache](docs/reading-cache.md) before changing persistence.

## Development and verification

From the repository root:

```powershell
.\gradlew.bat spotlessCheck
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug :app:lintRelease
.\gradlew.bat :app:assembleDebug :app:assembleRelease
```

Use `spotlessApply` to format Kotlin/Gradle sources. Select a focused JVM test with:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests '*StatsViewModelTest'
```

Android integration tests require a connected test device/emulator:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Tests cover Room, SAF/source access, book opening/lifecycle and gestures. Inspect selected
native tests before running on personal devices. JVM tests use JUnit 4 and coroutine test
dispatchers. See [AGENTS.md](AGENTS.md) for repository working conventions and verification.

### Performance and releases

The separate `:benchmark` module uses a synthetic dataset with release-like R8-optimized
builds and generates app profiles. Fixtures/setup are excluded from normal debug/release.
Benchmark instrumentation requires Android 11+ (API 30). See [benchmarking](docs/benchmarking.md)
for commands, conditions and limits; benchmark infrastructure alone implies no RAM savings.

`assembleRelease` outputs `app/build/outputs/apk/release/app-release-unsigned.apk`.
Sign before installation/distribution with your private keystore, preserving the same
certificate for updates. Follow [release signing](docs/release.md) and
[physical-device acceptance](docs/reliability.md). Keep keys, signed APKs and mappings out of Git.

## Further reading and licensing

- [Source integration/parser limits](docs/koreader-probe.md)
- [Library](docs/library.md) and [statistics semantics](docs/statistics.md)
- [Design system](docs/design-system.md)

No project-wide license file is currently included. Third-party notices are packaged in
[`app/src/main/assets/licenses`](app/src/main/assets/licenses) and shown in Settings → About.
Those notices do not assign a license to InkNest itself.

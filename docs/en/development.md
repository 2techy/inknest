# Build and develop InkNest

[Tiếng Việt](../vi/development.md) · [Documentation](README.md)

Set up the toolchain, build/install the app and choose the checks needed for a change.

## Prerequisites

- Android Studio, Android SDK Platform 37 and Platform-Tools; keep SDK location in untracked `local.properties`.
- Java 21 for app/benchmark source and bytecode targets. Set Studio's Gradle JDK to 21; its bundled JBR is suitable only if that version matches. The [daemon criteria](../../gradle/gradle-daemon-jvm.properties) select Java 21; verify with `--version`. Studio itself may use its bundled runtime.
- Authorized Android 8.0+ device/emulator; benchmark instrumentation requires Android 11+.

Open the project and sync Gradle. Dependencies download automatically on the first build; no backend/API keys or separate install step. Versions live in the [wrapper](../../gradle/wrapper/gradle-wrapper.properties) and [catalog](../../gradle/libs.versions.toml). Java library API availability still depends on the Android version.

Commands run from the repo root in PowerShell. Set `JAVA_HOME` to JDK 21 if needed. On macOS/Linux replace `.\gradlew.bat` with `bash ./gradlew` (no executable bit in Git). The benchmark script uses PowerShell and the Windows wrapper.

## Build and install

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.twotechy.inknest.debug/dev.twotechy.inknest.MainActivity
```

Alternatively run `app` in Studio or use `.\gradlew.bat :app:installDebug`. For multiple devices, use `adb -s SERIAL` or set `ANDROID_SERIAL` for Gradle device tasks.

| Variant | Application ID | APK |
| --- | --- | --- |
| Debug | `dev.twotechy.inknest.debug` | `app/build/outputs/apk/debug/app-debug.apk` |
| Unsigned optimized release | `dev.twotechy.inknest` | `app/build/outputs/apk/release/app-release-unsigned.apk` |

Normal debug/release builds use [real sources](setup.md). Debug and release use separate application IDs, so both can coexist with independent settings and SAF grants. Updating an installed copy requires compatible signing; use a separate test device/profile when changing signing identity. Uninstalling deletes local settings/pins. See [release signing](release.md).

Local version defaults to `0.1.0-dev` / code `1`. To reproduce release metadata, use its recorded values as quoted properties, for example:

```powershell
.\gradlew.bat :app:assembleRelease '-PinknestVersionName=2026.11.0' '-PinknestVersionCode=1000001'
```

Manual updates work with unknown version names without claiming the release is newer; APK code/signature checks still apply. Test available-update UI with an older CalVer, e.g. `.\gradlew.bat :app:assembleDebug '-PinknestVersionName=2026.10.0'`. Debug disables automatic checks. The debug updater rejects production APKs because their package differs. Test installation end to end with a release build using the matching signing identity.

## Verification

| Purpose | Command |
| --- | --- |
| Formatting check / apply | `.\gradlew.bat spotlessCheck` / `.\gradlew.bat spotlessApply` |
| Kotlin compilation | `.\gradlew.bat :app:compileDebugKotlin` |
| JVM tests | `.\gradlew.bat :app:testDebugUnitTest` |
| Focused JVM test | `.\gradlew.bat :app:testDebugUnitTest --tests '*StatsViewModelTest'` |
| Android lint | `.\gradlew.bat :app:lintDebug :app:lintRelease` |
| Debug/release APKs | `.\gradlew.bat :app:assembleDebug :app:assembleRelease` |
| Android test compilation | `.\gradlew.bat :app:assembleDebugAndroidTest` |
| Connected Android tests | `.\gradlew.bat :app:connectedDebugAndroidTest` |
| Performance APKs | `.\gradlew.bat :app:assembleBenchmark :benchmark:assembleBenchmark` |

For code changes, run formatting, relevant tests, affected debug/release builds and lint. Do not edit Kotlin while lint runs. Read native tests before running on personal devices; [opt-in SAF tests](reliability.md) can revoke configured grants. There is no `:benchmark:lintBenchmark` task.

Docs-only changes need link, command and diff validation. Report checks actually run and limitations; emulator layouts do not certify physical E-Ink. [Benchmarking](benchmarking.md) owns fixture/measurement procedures.

JVM repository tests use disk-backed Preferences `OkioStorage` with the real serializer; Android integration tests cover production Android storage. Coroutine tests share a scheduler across injected dispatchers and fake clocks.

## GitHub Actions

- [CI](../../.github/workflows/ci.yml): PRs into `master` (including drafts), pushes to `master` and manual dispatch. Automatic runs skip changes limited to `docs/**` and root Markdown. The full `build` checks formatting, debug/release JVM tests/lint and APK/Android-test compilation, without signing secrets.
- CI uses Ubuntu, Temurin 21 and `platforms;android-37.0`. Only pushes to `master` write Gradle cache. Failure reports last one day; manual `upload_debug_apk` retains a debug APK for one day, ordinary PRs upload none.
- [Android tests](../../.github/workflows/android-tests.yml): manual Actions → Android tests → Run workflow, using Android 14/API 34 x86_64. Real-source/grant-revocation tests stay opt-in and skipped; failure reports/logcat last one day. This does not certify Android 8 or physical E-Ink.
- [Release](release.md) and [benchmark/profile](benchmarking.md) workflows are separate. Actions are pinned, tokens read-only by default. Repository retention defaults to seven days; uploaded workflow artifacts last one day.

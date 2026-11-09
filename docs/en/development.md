# Build and develop InkNest

[Tiếng Việt](../vi/development.md) · [Documentation](README.md)

## Prerequisites

- Android Studio, Android SDK Platform 37 and SDK Platform-Tools.
- Java 21, as specified by [the Gradle daemon configuration](../../gradle/gradle-daemon-jvm.properties). Use a compatible JDK; Studio's bundled JBR is suitable only if its version matches. Both app and benchmark use Java 21 source/bytecode targets. AGP's built-in Kotlin inherits JVM target 21 from the Android compile options.
- An authorized Android 8.0+ device or emulator for the app; benchmark instrumentation requires Android 11+.

Open the repository in Android Studio and sync Gradle. Keep the SDK location in untracked `local.properties`. Dependencies resolve through Gradle; no backend, API keys or separate package installation is required. The first build needs access to the configured Gradle/Maven repositories. Versions live in the [wrapper configuration](../../gradle/wrapper/gradle-wrapper.properties) and [version catalog](../../gradle/libs.versions.toml).

Run commands from the repository root in PowerShell. If needed, set `JAVA_HOME` to your Java 21 installation. On macOS/Linux replace `.\gradlew.bat` with `bash ./gradlew`; the wrapper has no Git executable bit. The benchmark runner is a PowerShell script that invokes the Windows wrapper.

Configure Android Studio's Gradle JDK to use JDK 21 as well. The checked-in daemon JVM criteria select Java 21 for the build, even if the wrapper is launched by another compatible JDK. Verify the daemon JVM with `--version`. Android Studio itself can continue using its bundled runtime.

Java/JVM target 21 does not change the minimum Android version: the app still requires API 26. Android runs DEX produced by D8/R8, and available Java library APIs remain constrained by Android SDK support. Check compatibility before introducing newer APIs.

## Build and install

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.twotechy.inknest/.MainActivity
```

Alternatively run `app` from Android Studio or use `.\gradlew.bat :app:installDebug`. With multiple ADB devices, add `-s SERIAL` after `adb`; for Gradle device tasks select the target with `ANDROID_SERIAL`. An APK signed with another certificate cannot update an existing installation. Uninstalling deletes local settings and pins, so use a separate test device/profile when appropriate.

See [setup](setup.md) for real reading sources. Normal debug/release builds have no sample-data mode. [Release signing](release.md) is separate from building a debug APK.

## Verification

| Purpose | Command |
| --- | --- |
| Formatting check / apply | `.\gradlew.bat spotlessCheck` / `.\gradlew.bat spotlessApply` |
| Kotlin compilation | `.\gradlew.bat :app:compileDebugKotlin` |
| JVM tests | `.\gradlew.bat :app:testDebugUnitTest` |
| Focused JVM test | `.\gradlew.bat :app:testDebugUnitTest --tests '*StatsViewModelTest'` |
| Android lint | `.\gradlew.bat :app:lintDebug :app:lintRelease` |
| Debug and unsigned optimized release | `.\gradlew.bat :app:assembleDebug :app:assembleRelease` |
| Compile Android tests without a device | `.\gradlew.bat :app:assembleDebugAndroidTest` |
| Connected Android tests | `.\gradlew.bat :app:connectedDebugAndroidTest` |
| Performance APKs | `.\gradlew.bat :app:assembleBenchmark :benchmark:assembleBenchmark` |

For code changes run formatting, relevant tests, affected debug/release builds and lint. Do not edit Kotlin while lint runs. Read native tests before using a personal device: opt-in SAF tests can revoke configured grants. Use [reliability checks](reliability.md) for recovery and lifecycle scenarios. There is no `:benchmark:lintBenchmark` task.

Documentation-only changes need link, command and diff validation rather than a device matrix. Report which checks actually ran and any limitations; emulator layout checks do not certify E-Ink hardware. See [benchmarking](benchmarking.md) for isolated fixtures and performance measurements.

JVM repository tests create Preferences DataStore with `OkioStorage` and the real Preferences serializer on disk, so repeated writes work across host operating systems. Android integration tests exercise the production Android storage implementation.

## GitHub Actions

The [CI workflow](../../.github/workflows/ci.yml) runs on every pull request into `master`, including drafts, pushes to `master`, and manual dispatch. Its single `build` check runs formatting, JVM tests for debug/release, Android lint for debug/release, and builds debug/release APKs plus the Android test APK. Documentation-only changes use the same check. No signing secrets are needed for CI.

CI uses standard Ubuntu runners, Temurin 21 and SDK package `platforms;android-37.0`. Gradle cache writes are limited to pushes to `master`; PRs and manual workflows only read it. Failure reports expire after one day. A manual CI run can enable `upload_debug_apk` to retain a debug APK for one day; ordinary PR runs do not upload APKs.

Run the [Android tests workflow](../../.github/workflows/android-tests.yml) manually from Actions → Android tests → Run workflow. It uses one Android 14/API 34 x86_64 emulator and the default integration suite. Real-source/grant-revocation tests remain opt-in and are skipped. Failure reports/logcat expire after one day. Emulator results do not establish Android 8 or physical E-Ink acceptance; benchmarks and profile generation stay in the [device workflow](benchmarking.md).

Release automation is described in [release signing](release.md). Workflows use pinned actions with read-only tokens by default. The repository remains public and uses standard runners; cache stays within the free 10 GB repository limit. Artifact allowances belong to the organization and are not a separate allowance for each repository. Repository log/artifact retention defaults to seven days, with one-day retention for uploaded artifacts. No scheduled nightly builds or paid runners are configured.

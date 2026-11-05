# Build and sign an APK

Use the checked-in Gradle wrapper and Android Studio's bundled JDK. Dependency versions come
from the version catalog. `versionName` and `versionCode` in `app/build.gradle.kts` are the
single version source; About uses `BuildConfig.VERSION_NAME`.

## Local builds

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat spotlessCheck :app:assembleDebug :app:assembleRelease :app:lintDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. The optimized release build
is `app/build/outputs/apk/release/app-release-unsigned.apk`; it cannot be installed until signed.
Open source projects also sign installable release APKs. Signing keys are private and are not
the project's copyright license.

## Release signing

In Android Studio, use Build → Generate Signed App Bundle or APK → APK, select `app`, then
create/select your own keystore and choose release. Keep the key outside the repository and
back it up securely: future APK updates must use the same signing certificate. Keystores,
local signing properties, build output and Studio's `app/release/` output are ignored by Git.
Do not paste passwords into commands, commit signing files, or use the debug key for publication.

Verify the resulting APK with SDK `apksigner verify --verbose --print-certs <apk>` and record
its SHA-256 (`Get-FileHash <apk> -Algorithm SHA256`). Inspect the APK's package/version and
permissions before distribution. Keep mapping files privately with the matching artifact.

The same package signed with another key cannot replace an existing installation. Prefer a
separate test device/profile; uninstalling deletes local configuration and pins. This project
does not automatically uninstall, replace signing identities, tag, upload or publish builds.

## Data policy and release gates

Reading Room data is disposable during development: schema changes rebuild its cache. Pins,
preferences and source selections live in DataStore. Stable upgrade compatibility must be
decided before publishing a stable release. No migration framework is included in dev builds.

Backup and device transfer exclude InkNest data; SAF permissions and cache are device-local.
There is no network synchronization or telemetry. Debug-only source probes and StrictMode
diagnostics must be absent from the optimized release. Third-party notices remain in About.
No project license is selected automatically.

Before a public release, verify the signed artifact on the physical Tango 2: permissions,
reading source, Home role, app launch, book-opening fallback, Library, Stats and recovery.
Check E-Ink contrast/ghosting on the panel; emulator screenshots cannot establish these.
Keep the compatibility matrix and known limits with the release. Tag/publish only when requested.

## Known compatibility limits

The virtual Android 14 Tango-sized device has KOReader 2026.07.1. A cold direct book intent was
unreliable, so production uses the honest source-app fallback for this version. Physical Tango
book/sidecar/statistics and firmware verification remains separate. SD/cloud providers, reader
engine, write-back, OEM screen refresh and periodic background sync are outside current scope.

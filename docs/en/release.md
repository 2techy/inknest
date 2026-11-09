# Build and sign an APK

[Tiếng Việt](../vi/release.md) · [Documentation](README.md)

Use the checked-in Gradle wrapper and Java 25 as described in [development](development.md). Dependency versions come
from the version catalog. `versionName` and `versionCode` in `app/build.gradle.kts` are the
single version source; About uses `BuildConfig.VERSION_NAME`.

## Local builds

```powershell
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
There is no network synchronization or telemetry. Developer tools/source probes are absent from both build types. StrictMode
diagnostics must be absent from the optimized release. Third-party notices remain in About.
InkNest uses the [MIT license](../../LICENSE); bundled third-party components retain their own licenses.

Before a public release, verify the signed artifact on the physical Tango 2: permissions,
reading source, Home role, app launch, direct book opening, Library, Stats and recovery.
Check the launcher/source/opening flows on S25 using [device acceptance](reliability.md).
Check E-Ink contrast/ghosting on the panel; emulator screenshots cannot establish these.
Include device/firmware versions and known limits in release notes.

## Known compatibility limits

Library uses direct document intents without a debug/version unlock. Tango 2 with KOReader
v2026.07.1 needs its reader-owned FileProvider URI for primary-storage books: generic SAF
URIs can resolve to an inaccessible `/mnt/user/...` alias and leave the previous book open.
See [Reading cache](reading-cache.md) for the original-file URI adapter and recovery behavior.
Verify a newly signed release on Tango; APKs must use the existing release key for in-place updates.
SD/cloud providers, reader engine, write-back, OEM screen refresh and periodic background sync
are outside current scope.

# Build and sign an APK

[Tiếng Việt](../vi/release.md) · [Documentation](README.md)

Use the checked-in Gradle wrapper and Java 21 as described in [development](development.md). Dependency versions come
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
does not automatically uninstall or replace signing identities. The draft-release workflow below creates version tags; public release publication remains a maintainer action.

## GitHub draft releases

The [Draft release workflow](../../.github/workflows/release.yml) runs manually from Actions. Update `versionName` and increase `versionCode` through a PR, then merge after CI passes. Choose **Actions → Draft release → Run workflow**, select `master`, and start the run. Other branches and tags are skipped. No version input or manual tag creation is needed.

The run checks out the exact `master` commit selected when it starts, even if newer commits arrive during the build. It reads the version from the built APK, whose package must be `dev.twotechy.inknest`. Supported versions are `X.Y.Z`, optionally followed by `-dev`, `-alpha.N`, `-beta.N` or `-rc.N` (N starts at 1). The version code must be positive. An existing version tag must point to the same commit; an existing release must be a draft targeting that commit. Conflicts and published versions stop the run.

Build, signing and draft management use separate jobs. The build job checks formatting, debug/release JVM tests and lint, then builds the unsigned release without signing secrets. The signing job aligns/signs/verifies the APK using the repository's `release` environment; it does not check out app code. Only the draft job has release-write and artifact-cleanup permissions, and it also does not check out app code.

Configure environment `release` in repository settings with a deployment **branch** rule for `master`. Replace the previous `v*` tag rule if present: manual runs use a branch ref when entering the signing job. Use the existing release key, backed up outside GitHub. Set these environment secrets through GitHub settings or an authenticated local secret-management flow; do not paste values into issues, PRs, chat or commands containing literal passwords:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded existing release keystore |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Existing signing key alias |
| `ANDROID_KEY_PASSWORD` | Signing key password |

Set environment variable `ANDROID_SIGNING_CERT_SHA256` to the known certificate SHA-256 from a trusted previously signed APK (`apksigner verify --print-certs`); colons and whitespace are accepted. Missing secrets, an invalid fingerprint, a mismatched certificate or invalid tag/APK metadata fail the workflow. The keystore is decoded into temporary storage and deleted when signing exits; Gradle never receives signing secrets.

After build and signing succeed, the draft job creates tag `v<version>` at the checked-out commit and a **draft**, attaches `InkNest-<version>.apk` and `SHA256SUMS.txt`, and records the commit, version code and certificate fingerprint with generated release notes. Versions with suffixes are marked pre-release. Download that exact APK, verify its checksum/signature and complete [device acceptance](reliability.md) before choosing **Publish release**. Tag creation uses the workflow's `GITHUB_TOKEN`; no personal access token is needed. Pushing a tag does not start this workflow.

Release runs share one concurrency group and do not cancel the active run. A rerun may reuse a tag and update an existing draft only when their commits match; published releases are never overwritten. To retry an incomplete run, use **Re-run failed jobs** or **Re-run all jobs** on that run so it retains the original commit. Starting a new run after `master` changes cannot replace that version's tag. Intermediate unsigned/signed artifacts expire after one day and are deleted after successful release upload. If artifacts have expired, rerun all jobs. Release APKs remain in GitHub Releases, not in Git or long-lived Actions artifacts. If draft creation or upload fails after tag creation, the tag remains and the draft may be missing or incomplete; resolve the error, rerun the original run and verify both assets before publishing.

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

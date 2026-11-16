# Build and publish a release

[Tiếng Việt](../vi/release.md) · [Documentation](README.md)

The [Draft release workflow](../../.github/workflows/release.yml) builds and signs an APK, creates its version tag and prepares a draft on GitHub Releases. A maintainer reviews the APK and release notes before publishing.

## Configure signing

Create a repository environment named `release`. Under **Deployment branches and tags**, allow branch `master`. Store these credentials as **environment secrets**:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Release keystore encoded as Base64 |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Signing key alias |
| `ANDROID_KEY_PASSWORD` | Signing key password |

Add `ANDROID_SIGNING_CERT_SHA256` under **Environment variables**. Use the signing certificate's SHA-256 fingerprint from `keytool -list -v` on the keystore or `apksigner verify --print-certs` on a trusted signed APK. Colons and whitespace are accepted. The certificate fingerprint and APK checksum identify different things.

Keep a secure backup of the keystore outside the repository. Updates require the same signing certificate. Enter passwords through secret-management tools or password prompts; keep them out of commands, issues, logs and version control.

## Publish an APK

1. Merge the changes to `master` and confirm that CI passes.
2. Open **Actions → Draft release → Run workflow**, select `master` and start the run.
3. Wait for the build, signing and draft jobs to complete.
4. Open the draft in GitHub Releases. Review the title and generated changes, and describe the features, fixes and known limitations relevant to users.
5. Download `InkNest-<version>.apk` and `SHA256SUMS.txt`. Verify the checksum and signature, then complete [device acceptance](reliability.md) on the signed APK.
6. Choose **Publish release**.

Each draft includes an installable APK, a checksum file and an automatically generated changelog. Release notes omit the generic app introduction and installation/setup instructions. CalVer releases are not marked as pre-releases.

The in-app updater reads the latest published stable GitHub release. Preserve the exact asset names
`InkNest-<version>.apk` and `SHA256SUMS.txt`; the checksum entry must name that APK once. Publish both
assets together. Updater checks are optional and do not affect offline launcher/reading features.

The workflow runs only from `master` and builds the commit selected at dispatch. Commits merged during a run are included in a subsequent run. Release runs share a concurrency group; the active run is allowed to finish.

## Versioning

Release versions use **`YYYY.M.PATCH`**, for example `2026.11.0`.

| Field | Rule |
| --- | --- |
| Year and month | UTC creation time of the workflow run |
| PATCH | 0 if the month has no matching tags; otherwise the largest PATCH plus 1 |
| Tag | `v<version>`, for example `v2026.11.0` |
| Android version code | `1000000 + github.run_number` |

Only tags matching `vYYYY.M.PATCH` contribute to PATCH calculation, including tags attached to drafts. A failed run that has not created a tag does not reserve a PATCH number.

Publish in run-number order so Android version codes increase between distributed builds. Preserve the workflow's run-number sequence and offset; changing them requires checking the version codes already distributed. Codes above `2100000000` are rejected.

Local builds default to `versionName = "0.1.0-dev"` and `versionCode = 1`. Release builds supply Gradle properties `inknestVersionName` and `inknestVersionCode`. The built APK must match those values and package `dev.twotechy.inknest`. About displays `BuildConfig.VERSION_NAME`. See [development](development.md) for local build overrides.

## Retry a failed run

Use **Re-run failed jobs** to resume a failed stage, or **Re-run all jobs** to rebuild the same source commit. Reruns retain the version and code using the `release-version` artifact, which stores the version, code, commit and run ID for one day.

| Failure | Recovery |
| --- | --- |
| Missing signing credentials | Check the secrets and variable in environment `release`. |
| Certificate mismatch | Verify the keystore, alias and expected fingerprint. |
| Missing or expired artifacts | Start a new workflow run. |
| Tag conflict | A tag can be reused only for the same commit. |
| Release conflict | An existing release must be a draft targeting that commit. Published releases cannot be overwritten. |
| Draft or asset upload failure | Inspect the draft and retry the original run while its artifacts are available. Confirm both assets before publishing. |

A new run calculates its version from the tags available at that time. Reruns keep existing draft titles and notes. A tag created before a later failure remains in the repository.

Unsigned and signed transfer artifacts expire after one day and are deleted after successful asset upload. Version metadata expires after one day. Published APKs remain in GitHub Releases.

## Build and sign locally

Follow [development](development.md) for prerequisites, build/check commands and unsigned APK paths.

In Android Studio, choose **Build → Generate Signed App Bundle or APK → APK**, select `app`, choose the release keystore and build the release variant. Verify the APK with `apksigner verify --verbose --print-certs` and calculate its checksum with `Get-FileHash <apk> -Algorithm SHA256`. Keep matching mapping files privately.

An APK signed with another certificate cannot update an installed copy. Use a separate test device or profile when testing a different signing identity; uninstalling removes local settings and pins.

## Workflow isolation

The build job checks formatting, debug/release JVM tests and lint before assembling the unsigned release. It receives no signing secrets. The signing job aligns and signs the APK, verifies the certificate and computes its checksum. The keystore exists only in temporary storage and is deleted when the signing step exits.

The signing and draft jobs do not check out application code. Only the draft job has release-write and artifact-cleanup permissions. Tag creation uses `GITHUB_TOKEN`. Pushing a tag does not trigger the workflow.

## Acceptance and compatibility

Complete [device acceptance](reliability.md) on the signed APK and record device/firmware/KOReader versions. [Setup](setup.md) describes supported sources and recovery; [reading cache](reading-cache.md) covers URI/opening behavior.

The reading database is a rebuildable cache. Schema changes can recreate it; pins, preferences and source selections are stored separately in DataStore. App backup and device transfer exclude InkNest data, and SAF grants are device-local.

Third-party notices are in About. InkNest uses [MIT](../../LICENSE); bundled components retain their own licenses.

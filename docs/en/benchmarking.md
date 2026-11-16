# Startup and memory measurements

[Tiếng Việt](../vi/benchmarking.md) · [Documentation](README.md)

Run isolated smoke tests, compare startup/memory and generate app profiles. Production stays in `:app`; `:benchmark` is a self-instrumenting Android test module.

## Run

Use [Java 21 and the Gradle wrapper](development.md), plus an authorized Android 11+ ADB device. On Windows:

```powershell
./scripts/benchmark.ps1 -Mode Smoke -Device SERIAL -Label smoke
./scripts/benchmark.ps1 -Mode Measure -Device SERIAL -Label candidate
./scripts/benchmark.ps1 -Mode Profile -Device SERIAL -Label profiles
./scripts/benchmark.ps1 -Mode VerifyProfile -Device SERIAL -Label installation
```

- `-SkipBuild` reuses APKs. `-AllowEmulator` permits functional emulator runs; it does not suppress other failures or establish physical performance.
- Use a quiet physical device for comparisons. Keep font scale, resolution, installed apps, firmware, thermal/charging conditions unchanged; stop unrelated readers and concurrent Gradle/benchmark work. AndroidX requires at least 25% battery; record battery/temperature and inspect thermal warnings.
- The script installs APKs, prepares data and collects traces, JSON, checkpoint meminfo and device/commit metadata under ignored `benchmark/build/reports/<label>-<mode>`.
- Production preferences/grants are untouched. On emulators with production InkNest as default HOME, the script temporarily selects Pixel Launcher and restores HOME in `finally`; other launchers stay unchanged.

## Dataset and isolation

The `benchmark` target uses release R8 optimization, debug signing, application ID `dev.twotechy.inknest.benchmark` and shell profiling. `profile` disables shrinking for readable rules. Neither enables debug diagnostics; [release signing](release.md) is separate.

Setup creates 100 numbered synthetic EPUBs with 240×336 PNGs and a KOReader-compatible SQLite DB of 50,000 events, anchored to 2026-11-01. A marker versions the fixture; only `Download/InkNestBenchmark` is written. No private books/URIs or production Room schema changes are used.

The real picker grants the isolated target access and configures its own DataStore. Real SAF, EPUB/Lua parsing, Room import, thumbnails and UI run without fake repositories. Setup supports AOSP and Tango root-first Select/Confirm pickers and validates the exact fixture tree. The setup Activity/broad file permission exist only in benchmark/profile source sets; the Activity requires shell DUMP permission. Never distribute these variants.

## Comparison and interpretation

`StartupBenchmark` runs five cold starts, five warm starts with `CompilationMode.None`, and five Home → Library → Apps → Stats → Home journeys. Dataset preparation performs that initial journey before each test; this compilation mode adds no compilation warm-up iterations.

| Metric | Interpretation |
| --- | --- |
| Initial display | Compare medians; this is not full content readiness. |
| Navigation CPU frames | Compare sampled percentiles; inspect traces for repeatable timing regressions over 10%. |
| Memory | `dumpsys meminfo` after Home readiness on each startup and each complete journey: 15 checkpoints. Report PSS/RSS separately and their sampled maxima, not continuous peaks or OEM historical maxima. |

Compare an isolated checkout of the recorded baseline commit with only the harness/build overlay, keeping architecture code, fixture, package, compilation mode, device and five repetitions fixed. Archive metadata/raw reports locally, outside Git. Small samples and emulator GPU/host scheduling limit conclusions; repeat on Tango for device RAM claims. Architecture/reliability changes alone do not prove speed or memory gains.

## App profiles

`BaselineProfileGenerator` collects startup/navigation separately on unminified `profile`, requiring two stable iterations within three attempts. Retain InkNest classes only, excluding fixture classes. Profile mode writes versioned generated rules under `app/src/main/generated/baselineProfiles/`: merged `baseline-prof.txt` and separate `startup-prof.txt`; never hand-edit them.

AGP uses these for release R8 startup layout/ART packaging; ProfileInstaller installs the baseline on supported Android versions. Regenerate after significant startup/navigation changes, review the diff and rebuild release. The None-mode A/B test does not measure baseline compilation benefits.

`ProfileInstallationTest.packagedProfileCompilesAndStarts` separately verifies packaged-profile installation/compilation on optimized `benchmark` with `CompilationMode.Partial(BaselineProfileMode.Require)` and zero JIT warm-up. Its single startup sample is not an A/B result.

Firmware broadcast result 0 instead of ProfileInstaller install/skip delivery fails verification. Keep logs and record the limitation; do not suppress assertions or claim compilation from unprofiled measurements. Packaged assets and success on another device do not prove installation on that firmware.

Signed-release opening, HOME return and long reader sessions require [physical acceptance](reliability.md); synthetic results do not certify them.

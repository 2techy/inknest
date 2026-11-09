# Startup and memory measurements

[Tiếng Việt](../vi/benchmarking.md) · [Documentation](README.md)

`:benchmark` is a self-instrumenting Android test module. Production remains in `:app`.
The `benchmark` target inherits release R8 optimization and uses debug signing with
application ID `dev.twotechy.inknest.benchmark`. Its manifest is shell-profileable.
The `profile` target disables shrinking to generate readable rules; neither uses debug
diagnostics. Release signing remains the separate process in [release.md](release.md).

## Run

Use Java 25, an authorized Android 11+ ADB device, and the repository's Gradle wrapper. See [development prerequisites](development.md). On Windows:

```powershell
./scripts/benchmark.ps1 -Mode Smoke -Device SERIAL -Label smoke
./scripts/benchmark.ps1 -Mode Measure -Device SERIAL -Label candidate
./scripts/benchmark.ps1 -Mode Profile -Device SERIAL -Label profiles
./scripts/benchmark.ps1 -Mode VerifyProfile -Device SERIAL -Label installation
```

`-SkipBuild` reuses the existing APKs. `-AllowEmulator` explicitly permits functional
emulator runs; it suppresses only the emulator warning, not other benchmark failures.
Prefer a quiet physical device for performance conclusions. Stop unrelated readers,
keep font scale, resolution, installed apps, firmware and thermal conditions unchanged,
and do not run Gradle or another benchmark concurrently with measurement.
AndroidX requires at least 25% battery. Charging is supported, but use the same charging
condition for both candidates, record battery/temperature and inspect thermal warnings.

The script installs target/test APKs, prepares the dataset, runs instrumentation and
collects traces, JSON, checkpoint meminfo and device/commit metadata under ignored
`benchmark/build/reports/<label>-<mode>`. Existing production preferences and source grants
are untouched. If the emulator's default launcher is production InkNest, the script
temporarily selects Pixel Launcher to avoid concurrent foreground work and restores
the original HOME role in `finally`. Other device launchers are left alone.

## Dataset and isolation

The performance-only setup activity creates 100 numbered synthetic EPUBs, each with
a 240×336 PNG, and a KOReader-compatible SQLite database with 50,000 events. Event
dates are fixed relative to 2026-11-01; the fixture marker records its version. Only
`Download/InkNestBenchmark` is written. No private books, Tango URIs or production
Room schema changes are included.

Setup grants the isolated target access through the actual system folder picker,
then configures its separate DataStore. Production SAF, EPUB/Lua parsing, Room import,
thumbnail loading and UI are exercised without replacing repositories. The setup
driver supports the AOSP picker and Tango's root-first DocumentsUI with its separate
Select/Confirm flow; the activity validates the exact fixture tree before saving it.
The setup activity and broad file permission exist only in benchmark/profile source sets;
the activity requires the shell's DUMP permission. Never ship these variants.

## Comparison and interpretation

`StartupBenchmark` runs five cold and five warm starts with `CompilationMode.None`,
plus five Home → Library → Apps → Stats → Home journeys. Dataset preparation performs
the same initial journey before each test; Macrobenchmark records zero additional
compilation warm-up iterations for this mode. Compare median initial-display time
and sampled navigation CPU-frame percentiles, inspecting traces when a repeatable
timing regression exceeds 10%. Initial display is not a full content-ready metric.
Architecture and reliability improvements do not by themselves establish a speedup
or lower RAM usage. Keep those claims tied to comparable device measurements.

Memory is sampled with `dumpsys meminfo` after the Home content-ready checkpoint for
each startup and after each complete tab journey. Report PSS and RSS separately,
including their sampled maximum across the 15 checkpoints. These are not continuous
process peaks or the OEM's historical maximum. Emulator GPU/host scheduling and small
sample sizes limit conclusions; repeat on Tango before making device RAM claims.

For a controlled baseline, use an isolated checkout of the recorded baseline commit
and overlay only this harness/build configuration. Keep architecture code from that
commit, the same fixture, package, compilation mode, device and five repetitions.
Archive both metadata and raw reports locally. Do not commit measurement artifacts.

## App profiles

`BaselineProfileGenerator` collects startup and navigation rules separately against
the unminified `profile` target, requiring two stable iterations within three attempts.
Only InkNest classes are retained; fixture classes are excluded. Profile mode merges
both collections into `app/src/main/generated/baselineProfiles/baseline-prof.txt` and
writes startup rules separately as `startup-prof.txt`. These generated source rules
are versioned; traces and measurement reports are not.

AGP consumes that directory for release R8 startup layout and ART profile packaging.
ProfileInstaller installs the packaged baseline on supported Android versions.
Regenerate profiles after significant startup/navigation changes, review the diff,
then rebuild release. The unprofiled A/B test above deliberately uses compilation
mode None; it does not measure the benefit of ART baseline compilation.

`ProfileInstallationTest.packagedProfileCompilesAndStarts` is a separate smoke test
using `CompilationMode.Partial(BaselineProfileMode.Require)` with zero JIT warm-up.
Run it on the optimized benchmark target to verify installation/compilation of the
packaged profile; its single startup sample is not an A/B performance result.
If firmware returns broadcast result 0 instead of delivering ProfileInstaller's
install/skip actions, this verification fails explicitly. Record that device limitation
and retain the failure log; do not suppress the assertion or claim profile compilation
from an unprofiled measurement. APK assets and a passing supported-device verification
are separate evidence from installation on that firmware.

Physical signed-release book opening/return to HOME and a long reading session remain
separate acceptance checks; synthetic benchmark results cannot certify those flows.

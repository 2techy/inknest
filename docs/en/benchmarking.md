# Startup and memory measurements

[Tiếng Việt](../vi/benchmarking.md) · [Documentation](README.md) · [Results](benchmark-results.md)

## Run

Use [Java 21 and the Gradle wrapper](development.md), an authorized Android 11+ ADB device or emulator, and a distinct label for each run. Keep display settings and background workload consistent between comparisons.

```powershell
./scripts/benchmark.ps1 -Mode Smoke -Device SERIAL -Label smoke
./scripts/benchmark.ps1 -Mode Measure -Device SERIAL -Label candidate
./scripts/benchmark.ps1 -Mode Profile -Device SERIAL -Label profiles
./scripts/benchmark.ps1 -Mode VerifyProfile -Device SERIAL -Label installation
```

- `-AllowEmulator`: run on an emulator.
- `-SkipBuild`: reuse existing APKs.
- `-TestFilter`: select AndroidJUnitRunner classes or methods, separated by commas.

Run one scenario:

```powershell
./scripts/benchmark.ps1 -Mode Measure -Device emulator-5554 -AllowEmulator -SkipBuild -Label scroll -TestFilter 'dev.twotechy.inknest.perf.StartupBenchmark#libraryGridScroll'
```

The script installs APKs, prepares the fixture and exports results to ignored `benchmark/build/reports/<label>-<timestamp>-<mode>/`: instrumentation log, metadata, benchmark JSON, traces and memory checkpoints. On test failure it attempts to export partial results with `runStatus: failed`; inspect the log before retrying the failed scenario.

On emulators where InkNest is the default HOME, the runner temporarily selects Pixel Launcher and restores the original HOME when it exits. Keep the emulator open and avoid interacting during measurement.

## Dataset and isolation

Setup creates 1,000 synthetic EPUBs under `Download/InkNestBenchmark/books/a/b/c`, 200 history entries with sidecars and a KOReader-compatible SQLite database with 50,000 events anchored to 2026-11-01. A fixture-version change rebuilds `Download/InkNestBenchmark`.

The picker grants access to this fixture. Measurements use real SAF, parsers, Room, thumbnails and UI. The optimized `benchmark` target uses application ID `dev.twotechy.inknest.benchmark`; `profile` disables shrinking for profile generation. Their preferences and grants are separate from production.

Setup entry points and broad file permission exist only in benchmark/profile variants; control entry points require shell DUMP permission. Do not distribute these APKs.

## Comparison and interpretation

`StartupBenchmark` uses `CompilationMode.None` and five iterations per scenario. Fixture preparation and validation precede each test.

For total refresh/rebuild time, select `RefreshBenchmark#libraryRefresh`, `RefreshBenchmark#statisticsRefresh` or `RefreshBenchmark#clearCacheAndRefresh` with the full package prefix `dev.twotechy.inknest.perf.`. Each runs one iteration and includes data-readiness validation; statistics refresh waits for a new successful refresh attempt, which may reuse an unchanged snapshot.

| Scenario | Procedure / metric |
| --- | --- |
| Cold / warm startup | `StartupMode.COLD` / `WARM`; initial display time. |
| Main tabs | Home → Library → Apps → Stats → Home; CPU frame percentiles. |
| Unchanged HOME return | Let fixture files settle for six seconds, prime reading outside measurement, then return from system Settings through an explicit HOME intent; foreground refresh span. |
| Changed HOME return | Update history/current sidecar and add a 77-second statistics event before returning; foreground refresh span. |
| Library indexing (`indexLibrary`) | Clear target cache while Library is visible; index span and CPU frames. |
| Library grid scroll | Six gestures down and six up within the grid; require visible book titles to change; CPU frames. |

The harness validates completed refreshes, imported progress, 200 tracked items, statistics totals and the full catalog. `TraceSectionMetric` reports summed `InkNest.foregroundRefresh` / `InkNest.library.index` spans and their counts. Each completed iteration saves a `dumpsys meminfo` checkpoint; report sampled PSS/RSS separately.

Each scenario also saves a Perfetto trace. Within foreground refresh, `InkNest.reading.refresh` and `InkNest.statistics.refresh` run concurrently; do not add their durations. Statistics detail spans are `InkNest.statistics.fingerprint`, `InkNest.statistics.snapshot.copy`, `InkNest.statistics.import.rows`, `InkNest.statistics.summarize` and `InkNest.statistics.publish`. The rows span includes SQLite event reading and Room batch writes. `InkNest.statistics.skip` marks reuse of an unchanged import. During successful nonempty Library indexing, `InkNest.library.firstMetadata` ends after the first metadata batch is committed; `InkNest.library.metadataBatch` covers each metadata read/batch-write operation. These are nested in the index span, so do not add them to the index total. Trace names contain no source paths or document IDs.

Reading uses `InkNest.reading.read` for a full source read, `InkNest.reading.fingerprint` for metadata checks and `InkNest.reading.skip` for successful reuse. Compare control builds with the same quiet-period/priming setup.

Compare the same fixture, harness, compilation mode, display settings and device/host conditions. Keep dated summaries in [results](benchmark-results.md), including build identity and source run; retain raw artifacts outside Git. Memory files use shared device storage, so select checkpoints belonging to the measured scenarios and run.

## App profiles

`Profile` collects startup/navigation rules on the unminified target, requiring two stable iterations within three attempts. It writes generated InkNest rules, excluding fixtures, to `app/src/main/generated/baselineProfiles/baseline-prof.txt` and `startup-prof.txt`.

Regenerate after significant startup/navigation changes, review the generated diff and rebuild release. Do not edit profile rules by hand.

`VerifyProfile` checks packaged-profile installation and startup on optimized `benchmark` using `CompilationMode.Partial(BaselineProfileMode.Require)`, without JIT warm-up. If verification fails, inspect the instrumentation log and ProfileInstaller broadcast result before regenerating or retrying.

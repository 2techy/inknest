# Benchmark results

[Tiếng Việt](../vi/benchmark-results.md) · [Procedure](benchmarking.md)

AVD: `Tango_2_Virtual`, Android 14/API 34, x86_64, 862×1648, density 320, font scale 1.0. Fixture: 1,000 books, 200 history entries and 50,000 events. Optimized `benchmark`, `CompilationMode.None`.

## 2026-11-19 — Statistics import optimization

Foreground refresh median (s), five iterations per scenario; 2/2 passed.

| Scenario | Before | After | Change |
| --- | --- | --- | --- |
| `homeReturnUnchanged` | 9.375 | 5.494 | −41.40% |
| `homeReturnChanged` | 9.323 | 6.066 | −34.93% |

Report: `home-step4-trace-Measure`; reference: `step3-20261119-001645-Measure`. APK `8FC9FD14…`, base `a116650` + local changes. Separate runs; AVD restarted before the latest run.

## 2026-11-19 — SAF direct lookup

**Operations** — total operation plus data-readiness validation, in seconds. One iteration per build; both builds passed 3/3. Reference: master `e8453e5`, with identical instrumentation on both builds.

| Operation | Master | SAF direct lookup |
| --- | --- | --- |
| `libraryRefresh` | 150.55 | 7.61 |
| `statisticsRefresh` | 7.40 | 7.36 |
| `clearCacheAndRefresh` | 169.91 | 29.56 |

**Scenarios** — original benchmark suite. Five iterations per measured case; scalar values are medians. Reference: 2026-11-18. `—` means no new measurement.

| Scenario | Metric | Reference | SAF direct lookup |
| --- | --- | --- | --- |
| `homeReturnUnchanged` | Foreground refresh (s) | 140.17 | 9.38 |
| `homeReturnChanged` | Foreground refresh (s) | 140.39 | 9.32 |
| `coldStartup` | Initial display (ms) | 712.29 | — |
| `warmStartup` | Initial display (ms) | 424.77 | — |
| `libraryScanAndEnrich` | Scan span (s) | 29.69 | — |
| `mainTabNavigation` | CPU frame P50 / P95 / P99 (ms) | 50.75 / 85.61 / 94.95 | — |
| `libraryGridScroll` | CPU frame P50 / P95 / P99 (ms) | 30.16 / 76.00 / 89.55 | — |

Reports: `refresh-master-20261119-005402-Measure`, `refresh-step3-20261119-005402-Measure`, `step3-20261119-001645-Measure`. Reference sources: `baseline-step1-user-20261118-201053-Measure`, `scroll-grid-selection-fix-20261118-Measure`.

Additional master check, `homeReturnChanged` (2 iterations): 140.98 / 147.05 s; median 144.01 s (`master-control-20261119-003029-Measure`).

Raw JSON, traces and memory checkpoints remain under ignored `benchmark/build/reports/<run>/` in their source checkout.

## 2026-11-18 — emulator reference

- Base: `81fb756`, with a benchmark/tracing overlay; optimized `benchmark`, `CompilationMode.None`.
- AVD: `Tango_2_Virtual`, Android 14/API 34, x86_64, 862×1648, density 320, font scale 1.0.
- Fixture: 1,000 books under `books/a/b/c`, 200 history entries, 50,000 events; five iterations per scenario.
- Target APK SHA-256: `39F4E81F2492AF050E4726DEF957FF9E306FD81E0E7F96707C6EA4C21B2FF6A7`.

Values in ms; startup measures initial display. Refresh/scan use median summed operation spans.

| Scenario | Metric | Value |
| --- | --- | --- |
| `coldStartup` | Initial display, median | 712.29 |
| `warmStartup` | Initial display, median | 424.77 |
| `homeReturnUnchanged` | Foreground refresh, median | 140174.27 |
| `homeReturnChanged` | Foreground refresh, median | 140393.37 |
| `libraryScanAndEnrich` | Scan/enrich, median | 29693.26 |
| `mainTabNavigation` | CPU frame P50 / P95 / P99 | 50.75 / 85.61 / 94.95 |
| `libraryGridScroll` | CPU frame P50 / P95 / P99 | 30.16 / 76.00 / 89.55 |

Keep the harness, fixture and host conditions consistent for comparisons; emulator timing depends on host CPU/GPU load. Raw reports remain under ignored `benchmark/build/reports/<run>/`.

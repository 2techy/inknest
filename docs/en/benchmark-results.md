# Benchmark results

[Tiếng Việt](../vi/benchmark-results.md) · [Procedure](benchmarking.md)

AVD: `Tango_2_Virtual`, Android 14/API 34, x86_64, 862×1648, density 320, font scale 1.0. Fixture: 1,000 books, 200 history entries and 50,000 events. Optimized `benchmark`, `CompilationMode.None`.

Scenario labels use current names; raw reports retain their original identifiers.

## 2026-11-19 — Library indexing

Each build passed 2/2 scenarios × five iterations, with identical instrumentation. Changes are relative to batch 4; indexing/first metadata/memory use medians, frames use P95.

| Metric | Batch 4 | Batch 16 | Batch 32 |
| --- | --- | --- | --- |
| Index (s) | 28.415 | 24.002 (−15.53%) | 23.525 (−17.21%) |
| First metadata (s) | 0.728 | 0.943 (+29.52%) | 1.286 (+76.75%) |
| Metadata batches | 250 | 63 (−74.80%) | 32 (−87.20%) |
| Index frames | 256 | 68 (−73.44%) | 38 (−85.16%) |
| Index CPU frame P95 (ms) | 27.044 | 32.011 (+18.37%) | 36.272 (+34.12%) |
| Scroll CPU frame P95 (ms) | 31.014 | 30.283 (−2.36%) | 32.917 (+6.14%) |
| Index PSS (MiB) | 62.319 | 60.220 (−3.37%) | 66.036 (+5.96%) |
| Index RSS (MiB) | 169.531 | 167.230 (−1.36%) | 171.773 (+1.32%) |
| Scroll PSS (MiB) | 61.375 | 61.217 (−0.26%) | 61.615 (+0.39%) |
| Scroll RSS (MiB) | 165.832 | 165.414 (−0.25%) | 166.148 (+0.19%) |

Selected batch 16: batch 32 saves a further 1.99% total time but delays first metadata by 344 ms and worsens frame P95. No additional catalog throttle.

Reports: `library-batch{4,16,32}-step7-20261119-142719-Measure`; APK `F7489923…` / `4468F946…` / `CCF27EDB…`; instrumentation `90009B4E…`. Base `461b484` + catalog changes.

## 2026-11-19 — Cover cache and WebP thumbnails

Two scenarios passed, five iterations each. Compared with the 2026-11-18 reference; differences include intervening changes, not an isolated WebP comparison. Scan and memory values are medians; frame values are percentiles.

| Scenario | Metric | Reference | Current | Change |
| --- | --- | --- | --- | --- |
| `indexLibrary` | Scan (s) | 29.693 | 25.541 | −13.98% |
| `libraryGridScroll` | CPU frame P50 (ms) | 30.163 | 15.975 | −47.04% |
| `libraryGridScroll` | CPU frame P95 (ms) | 76.004 | 28.266 | −62.81% |
| `libraryGridScroll` | CPU frame P99 (ms) | 89.551 | 32.776 | −63.40% |
| `indexLibrary` | PSS (MiB) | 73.749 | 66.737 | −9.51% |
| `indexLibrary` | RSS (MiB) | 166.336 | 174.656 | +5.00% |
| `libraryGridScroll` | PSS (MiB) | 70.942 | 61.345 | −13.53% |
| `libraryGridScroll` | RSS (MiB) | 163.492 | 165.496 | +1.23% |

Report: `cover-step6-Measure`; APK `FCC6BC00…`, base `8f173cd` + cover changes. References: `baseline-step1-user-20261118-201053-Measure` (scan), `scroll-grid-selection-fix-20261118-Measure` (scroll). Memory is sampled after each journey.

## 2026-11-19 — Reading refresh optimization

Foreground refresh median (s), five iterations per scenario; both builds passed 2/2. Same instrumentation and six-second quiet-period/HOME priming setup.

| Scenario | Before | After | Change |
| --- | --- | --- | --- |
| `homeReturnUnchanged` | 6.039 | 2.398 | −60.30% |
| `homeReturnChanged` | 6.668 | 6.761 | +1.41% |

Unchanged: reading skipped 5/5; changed: full read 5/5.

Reports: `reading-control-step5-20261119-112754-Measure` / `reading-step5-20261119-112754-Measure`. APK `A290C83F…` → `9481CE10…`; instrumentation `90009B4E…`. Base `1d8bc47`, with reading-skip changes in the candidate.

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
| `indexLibrary` | Scan span (s) | 29.69 | — |
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
| `indexLibrary` | Index, median | 29693.26 |
| `mainTabNavigation` | CPU frame P50 / P95 / P99 | 50.75 / 85.61 / 94.95 |
| `libraryGridScroll` | CPU frame P50 / P95 / P99 | 30.16 / 76.00 / 89.55 |

Keep the harness, fixture and host conditions consistent for comparisons; emulator timing depends on host CPU/GPU load. Raw reports remain under ignored `benchmark/build/reports/<run>/`.

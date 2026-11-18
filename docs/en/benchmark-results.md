# Benchmark results

[Tiếng Việt](../vi/benchmark-results.md) · [Procedure](benchmarking.md)

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

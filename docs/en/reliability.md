# Reliability and device acceptance

[Tiếng Việt](../vi/reliability.md) · [Documentation](README.md)

## Prepare a test environment

Use an authorized test device or emulator and synthetic or non-private books. Build and install using [development](development.md). Record the app variant/version, Android/firmware, KOReader version, display settings and dataset characteristics when reporting a result. Keep logs, traces and measurements under ignored build directories.

Read Android tests before running them. `SourceAccessIntegrationTest` runs only with the `source_access_integration=true` instrumentation argument, requires configured reading data and deliberately revokes a Books grant. Use a test device and restore grants through the system picker afterward. The default suite does not opt into that test.

## Source and cache recovery

| Scenario | Procedure and expected behavior |
| --- | --- |
| Invalid selection | Select a non-KOReader folder as its data folder or cancel the picker. The active configuration and cached content remain unchanged. |
| Lost access | Revoke a test Books grant, refresh and restart. Report access loss, preserve cached content and keep other roots usable. Reselect the affected folder to recover. |
| Partial source failure | Make one Books root unavailable. Refresh all roots; available roots still update and the failed root retains its snapshot. |
| Source isolation | Switch source while refreshing. Late jobs must not publish data into the new configuration. |
| Corrupt input | Use malformed Lua/EPUB/SQLite and invalid statistics schemas. Expose an error or partial metadata as appropriate; never fabricate a successful empty snapshot. |
| Snapshot consistency | Repeat refreshes, including SQLite WAL activity. Totals must not double count; a failed or cancelled import retains the previous active generation. |
| Clear/disconnect | Clear cache, then disconnect. Cache clear rebuilds selected sources; disconnect removes source data/grants. Both preserve pins and appearance settings and leave original files untouched. |

## Lifecycle and interaction

- Open two different books in alternating order from Home/Library, both with KOReader cold and warm. Check the actual book and saved position in KOReader; successful intent dispatch alone is insufficient.
- Background the app or press HOME during preparation. A pending request must not launch later; returning to the app must not replay a dispatched request.
- Recreate the Activity and simulate background process death. Verify cached content, destination, query/filter/scroll state and Stats selections restore where supported.
- Test saving preferences under failure: the UI must not report success or replace the saved value.
- Exercise system Back, default Home selection, app launch, pins, tab swipes and pull refresh. Tab/range/clock changes must not start a source scan.
- Change app language, font scale (including 150%), window width, date and timezone. Check wrapping, reachable controls, calendar boundaries and DST without changing source data.
- Inspect debug StrictMode output for unexpected app I/O on main. Account separately for AppCompat locale-storage diagnostics on Android 12 and earlier, described in [setup](setup.md).

Native Room/SAF/lifecycle behavior belongs in Android integration tests; aggregation, source error mapping and ViewModel behavior also have focused JVM coverage. See [architecture](architecture.md) for ownership and recovery contracts.

## Physical-device acceptance

On iReader Tango 2, verify contrast, ghosting, refresh behavior, touch targets, cold/warm opening and return after a long KOReader session on the actual panel. Check launcher/source/opening flows on Samsung Galaxy S25 Ultra as the secondary development target. Recheck after firmware or KOReader updates and after release signing changes.

Measure refresh latency and memory using a fixed dataset and comparable conditions. Distinguish PSS from RSS, sampled values from continuous peaks and emulator results from hardware measurements. Use [benchmarking](benchmarking.md) for controlled synthetic comparisons. Screenshots and synthetic benchmarks cannot certify physical E-Ink behavior or real-reader correctness.

Before distribution, complete [signed-artifact verification](release.md). These procedures define acceptance work; they do not assert that a particular device/version has passed.

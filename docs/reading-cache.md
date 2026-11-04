# Current reading cache

Both debug and release use `CachedReadingRepository`. Home observes Room-backed current reading
and an independent `ReadingSyncStatus`; pins, clock and statistics remain separate. No runtime
sample-data path exists. Reading metadata/progress/cover enrichment and tracked history are later
steps; current titles use the actual filename, and unknown author/progress/cover stay unknown.
The current card links to reading Settings; the production reader opener is a separate change.

## Data and source boundaries

`KoreaderCurrentSource` reuses the bounded Lua/SAF sources. A valid lastfile takes priority. Missing
or invalid current settings fall back to the first valid history path in source array order.
Missing both sources or corrupt data cannot masquerade as a successfully empty source. A current
file missing/outside grants retains its identity and filename with an availability warning.
No current-book choice is inferred from statistics or basename/title matches.

Room schema v1 is exported in `app/schemas`. `reading_items` stores source/path identity, filename
and availability. `reading_sources` stores the current pointer, attempt/success timestamps and
error. The source/path domain identity remains stable across primary-storage aliases. Physical
cache rows additionally use a fingerprint of the two tree assignments, so a candidate Books
selection cannot overwrite the active partition before DataStore switches to it.

Candidate read/validation and cache write precede DataStore activation. A failed candidate keeps
active config/cache. Unactivated partitions remain invisible and are removed by clear/disconnect.
Grant cleanup checks the actual saved config, including after caller cancellation. There is no
transaction spanning DataStore and Room; the partition/staging order handles the crash gap.

## Refresh and failures

Activity foreground (`onStart`), Home refresh and Settings refresh share application-owned jobs.
Concurrent callers await the same active job. Cancelling an await does not cancel other callers.
Source changes cancel obsolete jobs, and active key plus coroutine cancellation are checked before
writing. Late cancellation cleanup cannot clear another job's running state.

Source errors update sync metadata without erasing the last successful current pointer. Home
keeps the cached card and shows a compact stale/error notice. Source identity and cache observations
are checked together, preventing previous-source rows/status from appearing under a new config.
Expected cache write failures are surfaced even if persisting their error record fails.
Disconnect clears reading rows/source keys/grants but preserves pins/preferences. Clear cache
removes reading rows, then reloads the configured source. Switching tabs/clock ticks never starts
source parsing.

## Verification

On 2026-11-04, 41 shared JVM tests passed in both variants. Six targeted Android tests exercised
actual Room persistence/reopen, A/B/A, concurrent refresh/caller cancellation, failure recovery,
candidate isolation and late jobs, success-only Empty, disconnect isolation and actual Lua current
selection. Two explicit opt-in SAF tests verified invalid root handling and real permission loss
with the configured cache. The virtual app kept its cached book after grant loss plus process
restart, then recovered through both native folder pickers. Both grants were restored.

Debug/release builds, lint and Spotless passed. Optimized release was installed and verified with
real KOReader data: current cache, refresh, clear/reload and restart worked; pins remained intact.
Home was inspected at the existing virtual 862×1648/density320 setup and font scale 150%, then the
original font scale and debug build were restored. These results do not establish Tango hardware
compatibility or production direct-open support.

Versions were checked against the official [Room releases](https://developer.android.com/jetpack/androidx/releases/room3),
[SQLite releases](https://developer.android.com/jetpack/androidx/releases/sqlite) and
[KSP release](https://github.com/google/ksp/releases/tag/2.3.13).

# Current reading cache

Both debug and release use `CachedReadingRepository`. Home observes Room-backed current reading
and an independent `ReadingSyncStatus`; pins, clock and statistics remain separate. No runtime
sample-data path exists. Current reading and tracked history share the same cache and source
refresh. Sidecar progress/status and EPUB title/author/cover enrich resolved books; unsupported or
missing fields remain unknown. The current card opens the configured reader through
`ReadingItemOpener`, without writing synthetic progress or last-read timestamps.

## Data and source boundaries

`KoreaderCurrentSource` reuses the bounded Lua/SAF sources. A valid lastfile takes priority. Missing
or invalid current settings fall back to the first valid history path in source array order.
Missing both sources or corrupt data cannot masquerade as a successfully empty source. A current
file missing/outside grants retains its identity and filename with an availability warning.
No current-book choice is inferred from statistics or basename/title matches.

Room schema v5 is exported in `app/schemas`. During development, schema changes do not carry
migration code: increment the Room version when changing the schema, and allow Room to recreate
the disposable reading database when no migration exists. Preferences, pins and SAF configuration
live in the separate DataStore and are unaffected by this cache reset. A foreground refresh
rebuilds reading data from the configured source. `reading_items` stores source/path identity, availability,
metadata, progress/status, source last-read time and nullable history order. `reading_sources`
stores the current pointer, attempt/success timestamps, error and skipped-history count. The source/path domain identity remains stable across primary-storage aliases. Physical
cache rows additionally use a fingerprint of the two tree assignments, so a candidate Books
selection cannot overwrite the active partition before DataStore switches to it.

Candidate read/validation and cache write precede DataStore activation. A failed candidate keeps
active config/cache. Unactivated partitions remain invisible and are removed by clear/disconnect.
Grant cleanup checks the actual saved config, including after caller cancellation. There is no
transaction spanning DataStore and Room; the partition/staging order handles the crash gap.

## Metadata, covers and tracked history

History follows the source array order. Canonical-path duplicates keep their first occurrence;
same-title books at different paths remain separate. Source timestamps are validated seconds and
converted to epoch milliseconds without overflow. Individual malformed entries are skipped with a
warning/count. A structurally broken history document retains the previous tracked list; failed
fallback selection cannot publish a false empty state. A successful replacement, its current
pointer and history order are committed in one Room transaction. Missing/unresolved books remain
tracked with their availability. `ReadingRepository.trackedItems` exposes the active partition for
reading-status enrichment. Library membership comes from the separate Books-folder inventory;
Home does not add a Recent section.

Sidecars are reread on refresh, so a lower `percent_finished` replaces a higher previous value.
Only finite values in [0, 1] are accepted. Explicit `reading`, `complete` and `abandoned` statuses
map to known states; 100% alone does not imply Finished.

EPUB2/3 extraction reuses the bounded ZIP/XML adapter. Valid title/author survive unsupported or
missing artwork. Private PNG thumbnails have a longest edge of at most 480 pixels and are written
through a temporary file plus atomic rename. Their keys include source/partition, canonical path
and source size/mtime. Unchanged known fingerprints reuse metadata and existing covers; unknown
fingerprints are reread. Changed files use a new key. Old unreferenced thumbnails are pruned after
24 hours, allowing concurrent candidate reads to stage files safely. Clear/disconnect removes the
cover cache along with reading rows. No bitmap is stored in ViewModels or saved state.

## Opening the reader

`ReadingItemOpener` is a main-safe platform boundary shared by Home and Library. Each action
re-resolves the original document, validates the selected source again, then dispatches ACTION_VIEW
with its content URI/MIME, ClipData and temporary read permission to the configured reader.
Package/intent resolution runs off the main thread. Opening does not parse or copy the EPUB.

There is no persisted confirmation/version gate or app-only fallback. Missing app/file/grant,
unsupported handler and changed-source outcomes are explicit. File errors offer refresh; access
or configuration errors offer Settings. `Dispatched` means Android accepted the intent, not that
KOReader confirmed opening the file. Progress/history/current-book cache remain source-driven.
Old open-mode/version preference keys are ignored; no preference migration or second store is added.

On some devices (confirmed on Tango 2), ExternalStorageProvider descriptors resolve inside
KOReader to `/mnt/user/...`, which its Lua file checks cannot reopen. After validating the SAF
source, primary-storage documents use KOReader's own `content://org.koreader.launcher.provider/external/`
URI when that authority belongs to the selected reader. Its provider resolves the same file to
`/storage/emulated/0/...`; no file is copied or imported. Data and ClipData stay consistent, and
foreign URI-grant flags are removed because the receiving app owns that provider. If unavailable,
other readers/providers retain the original SAF intent. No `file://` exposure bypass is used.

The mapping rejects invalid/traversal IDs and other volumes. A focused regression covers encoded
names, exact path/MIME preservation, absent/wrong provider ownership and grant handling. Debug
book probes use the same opener, rather than a separate intent implementation.


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
removes reading rows, the Books inventory and covers, then reloads the configured sources.
Books-only configurations can also clear/rebuild their inventory; cache size includes thumbnails. Switching tabs/clock ticks never starts
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
compatibility. Production direct-open now follows the URI dispatch contract above.

Versions were checked against the official [Room releases](https://developer.android.com/jetpack/androidx/releases/room3),
[SQLite releases](https://developer.android.com/jetpack/androidx/releases/sqlite) and
[KSP release](https://github.com/google/ksp/releases/tag/2.3.13).

The reading-core extension was checked on 2026-11-05 with six selected JVM tests and targeted
Android checks for metadata/thumbnail reuse and invalidation, EPUB2/3 and missing
artwork, decreasing progress, path deduplication, corrupt/partial history and opener failures.
Existing cache persistence/candidate-cancellation tests also passed. Optimized release opened the
configured KOReader app cold and warm using real virtual-device data; actual metadata/covers were
inspected at the existing viewport and 150% font scale. No full launcher regression or hardware
E-Ink acceptance was repeated.

Statistics snapshots share the reading database (schema 6), but retain their own source
identity and repository. Cache clear/disconnect includes raw events, daily summaries, import tokens and snapshot timestamps.
See [Statistics](statistics.md).

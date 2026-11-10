# Current reading cache

[Tiếng Việt](../vi/reading-cache.md) · [Documentation](README.md)

Both debug and release use `CachedReadingRepository`. Home observes Room-backed current reading
and an independent `ReadingSyncStatus`; pins, clock and statistics remain separate. No runtime
sample-data path exists. Current reading and tracked history share the same cache and source
refresh. Sidecar progress/status and EPUB title/author/cover enrich resolved books; unsupported or
missing fields remain unknown. The current card opens the configured reader through
`ReadingOpenRepository` / `ReadingItemDispatcher`, without writing synthetic progress or last-read timestamps.

## Data and source boundaries

`KoreaderCurrentSource` reuses the bounded Lua/SAF sources. A valid lastfile takes priority. Missing
or invalid current settings fall back to the first valid history path in source array order.
Missing both sources or corrupt data cannot masquerade as a successfully empty source. A current
file missing/outside grants retains its identity and filename with an availability warning.
No current-book choice is inferred from statistics or basename/title matches.

Room schema 6 is exported in `app/schemas`. During development, schema changes do not carry
migration code: increment the Room version when changing the schema, and allow Room to recreate
the disposable reading database when no migration exists. Preferences, pins and SAF configuration
live in the separate DataStore and are unaffected by this cache reset. A foreground refresh
rebuilds reading data from the configured source. `reading_items` stores source/path identity, availability,
metadata, progress/status, source last-read time and nullable history order. `reading_sources`
stores the current pointer, attempt/success timestamps, error and skipped-history count. The source/path domain identity remains stable across primary-storage aliases. Physical
cache rows additionally use a fingerprint of the KOReader tree and sorted Books trees, so a candidate Books
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

Home and Library ViewModels own opening state. ReadingOpenRepository re-resolves the original
document and validates the selected source. The resumed Route claims each request once and uses
ReadingItemDispatcher to dispatch ACTION_VIEW with its URI/MIME, ClipData and read permission.
Package/intent resolution runs off main; dispatch runs on the injected main dispatcher. Opening
does not parse or copy the EPUB. Pause/disposal drops pending work; requests are not persisted.

Opening reports distinct outcomes for a missing app, file or grant, an unsupported handler,
and a changed source. File errors offer refresh; access
or configuration errors offer Settings. `Dispatched` means Android accepted the intent, not that
KOReader confirmed opening the file. Progress/history/current-book cache remain source-driven.

On some devices (confirmed on Tango 2), ExternalStorageProvider descriptors resolve inside
KOReader to `/mnt/user/...`, which its Lua file checks cannot reopen. After validating the SAF
source, primary-storage documents use KOReader's own `content://org.koreader.launcher.provider/external/`
URI when that authority belongs to the selected reader. Its provider resolves the same file to
`/storage/emulated/0/...`; no file is copied or imported. Data and ClipData stay consistent, and
foreign URI-grant flags are removed because the receiving app owns that provider. If unavailable,
other readers/providers retain the original SAF intent. No `file://` exposure bypass is used.

The mapping rejects invalid/traversal IDs and other volumes. Regression tests cover encoded
names, exact path/MIME preservation, provider ownership and grant handling. Debug and release
use the same opening flow.


## Refresh and failures

A lifecycle-aware STARTED effect delegates foreground updates through AppViewModel and the shared refresh coordinator. Home/Settings refresh use the same application-owned repository jobs.
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


Statistics shares schema 6 but owns its source identity and repository. Cache clear/disconnect includes raw events, daily summaries, import tokens and snapshot timestamps. See [statistics](statistics.md) and [reliability checks](reliability.md).

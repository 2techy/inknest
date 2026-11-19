# Current reading cache

[Tiếng Việt](../vi/reading-cache.md) · [Documentation](README.md)

Current reading, tracked history, refresh and opening contracts for `RoomReadingRepository`, shared by debug/release. Home observes Room data and independent `ReadingSyncStatus`; pins, clock and statistics remain separate. Missing metadata stays unknown; opening never fabricates progress/last-read times.

## Data and source boundaries

- `KoreaderCurrentReadingDataSource` prioritizes valid `lastfile`, falling back to the first valid history path in array order. Missing/corrupt sources differ from successful empty data. A missing/out-of-grant current file retains identity/filename with an availability warning. Never infer current reading from statistics or title/basename matches.
- Room schema 8 is exported in `app/schemas`; development uses destructive cache fallback, without stable upgrade guarantees. DataStore separately owns preferences, pins and SAF config. Foreground refresh rebuilds disposable data.
- `reading_items` stores source/path identity, availability, metadata, progress/status, source last-read time and nullable history order. `reading_sources` stores the current pointer, attempt/success times, errors and skipped-history count.
- Domain identity survives primary-storage aliases. Cache rows additionally partition by KOReader-tree/sorted-Books-tree fingerprint. Candidate read/validation/cache write precedes DataStore activation; failure preserves active config/cache. Invisible candidate partitions are removed by clear/disconnect.
- DataStore/Room have no shared transaction; staging order handles the crash gap. Grant cleanup uses the actual saved config even after caller cancellation.

## Metadata, covers and tracked history

History preserves array order; canonical-path duplicates keep the first entry, while equal titles at different paths remain distinct. Validate source seconds before overflow-safe conversion to epoch milliseconds. Skip individual malformed entries with warning/count; a broken document retains the previous list, and failed fallback selection never publishes false empty data.

Commit replacement history, current pointer and order in one Room transaction. Unresolved books remain tracked with availability. `ReadingRepository.trackedItems` enriches reading status; [Library](library.md) membership comes from folder inventory. Home has no Recent section.

Full reads reread sidecars: finite `percent_finished` in [0, 1] may decrease. Explicit `reading`, `complete`, `abandoned` map to known states; 100% alone is not Finished.

Book title/author survive missing/unsupported artwork. [Bounded extraction](koreader-integration.md) produces private color WebP thumbnails (lossy quality 85), longest edge at most 480px, through temporary write/atomic rename. Keys include the thumbnail format version, source/partition, canonical path and size/mtime. PNG cache entries are re-extracted from the book on refresh; there is no format migration. Known unchanged fingerprints reuse metadata/covers; unknown ones reread and changed files use new keys. Prune unreferenced thumbnails after 24 hours to allow concurrent candidate staging. Clear/disconnect removes covers. No bitmap belongs in ViewModels/saved state; decoded-cache rules are in [design system](design-system.md).

## Saved page position

Home derives position from the same sidecar: `round(percent_finished × doc_pages)`, using the original fraction, not the displayed integer percentage. Valid progress in 0..1 and positive integer totals produce 0..total. `last_xpointer` works without `last_page`; invalid/missing totals retain valid progress/status, while missing progress leaves position unknown.

This is a derived position in KOReader's saved layout, which can change with reader settings, not a publisher page label. Never supply it through statistics/title matching.

## Opening the reader

Home/Library ViewModels own opening state. `ReadingOpenRepository` re-resolves the original document and validates its source. The RESUMED Route claims each request once, dispatching ACTION_VIEW with URI/MIME, ClipData and read permission through `ReadingItemDispatcher`. Package/handler resolution runs off main; dispatch uses injected main. Preparation never parses/copies the whole book. Pause/HOME/disposal cancels pending work; requests are not persisted.

Distinct failures cover missing app/file/grant, unsupported handler and changed source. File errors offer refresh; access/configuration errors offer Settings. `Dispatched` means Android accepted the intent, not confirmed reader success. Progress/history/current reading remain source-driven.

On Tango 2, ExternalStorageProvider descriptors can resolve in KOReader to inaccessible `/mnt/user/...` paths. After SAF validation, primary-storage documents use `content://org.koreader.launcher.provider/external/` only when that authority belongs to the selected reader. It resolves the same file via `/storage/emulated/0/...`, without copying/importing. Data/ClipData remain consistent; foreign grant flags are removed because the receiver owns the provider. Otherwise retain the original SAF intent; never use `file://`.

Reject malformed/traversal IDs and other volumes. Regression coverage includes encoded names, exact path/MIME, provider ownership and grants. See [device acceptance](reliability.md) for actual opening/return checks.

Known book extensions use reader-compatible MIME types (including `application/fb2`), even when SAF reports `application/octet-stream` or an incompatible alias. Unknown extensions retain the provider MIME, falling back to `application/octet-stream`. This does not establish reader support for a format.

## Refresh and failures

Foreground ownership follows [architecture](architecture.md). Home/Settings share application-owned repository jobs; concurrent callers await the same job, and cancelling one waiter does not cancel others. Source changes cancel obsolete jobs; active key/cancellation are checked before writes. Late cleanup cannot clear another job's running state.

After a successful cache save, refresh may reuse an in-memory fingerprint of settings/history, every current/history book and sidecar, and known file absences. Recheck all configured tree grants, metadata, saved cache identity and cover existence before skipping. Unknown, changing or recent metadata (less than five seconds old at full-read start) disables reuse, as do read failures, warnings, skipped entries, cache observation/write errors and the capture-budget limit. Skip updates the attempt time while retaining the success time and cached content. Clear, disconnect, source replacement and process restart require a full read. The [source integration](koreader-integration.md) size/mtime limitations still apply.

- Source errors update sync metadata without erasing the last successful current pointer. Home retains cached content with a stale/error notice. Check source identity and observations together to prevent previous-source data/status appearing under new config.
- Surface expected cache-write failures even if recording the error also fails.
- Disconnect clears source rows/keys/grants, preserving pins/preferences. Clear cache removes reading data, Books inventory and covers, then reloads selected sources; Books-only configs also rebuild. Cache size includes thumbnails.
- Statistics owns its source identity/repository within the shared DB. Clear/disconnect includes events, summaries, import tokens and timestamps. See [statistics](statistics.md). Tab/clock changes never parse sources.

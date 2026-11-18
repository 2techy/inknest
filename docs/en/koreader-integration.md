# KOReader source integration

[Tiếng Việt](../vi/koreader-integration.md) · [Documentation](README.md)

SAF access, parser limits and error contracts for real KOReader sources in debug/release. For folder-selection steps, see [setup](setup.md).

## Configuration and access

Preferences DataStore stores source identity, selected `org.koreader.launcher` package, KOReader tree and independent Books trees. The system picker supplies persisted read grants. Invalid/cancelled selections retain active config; obsolete grants are released. Disconnect removes source keys/grants while preserving pins, appearance and onboarding. Room caches reading data; see [reading cache](reading-cache.md).

Only primary internal storage through Android ExternalStorageProvider is supported. Other providers/volumes are rejected. Document IDs are validated by volume/path segments; children must remain within the granted root. Source paths resolve only into configured Books or KOReader trees. Primary aliases are accepted only after canonical filesystem identity is checked on the device. Unicode, case and spaces are retained. Dot segments (`.`/`..`), sibling-prefix matches and title/basename guessing are rejected. A missing grant never triggers guessed paths or broader storage access.

## Parsers and limits

Resolve configured primary-storage paths by querying the document ID directly. If it is unavailable, traverse child documents once using exact display names. Preserve case and Unicode; do not add case folding or fuzzy matching. Validate containment for both direct and fallback IDs.

Each reading refresh and statistics import captures persisted read grants once for that operation. Provider query/open calls still enforce access if a grant is revoked mid-operation. Opening a book and computing its checksum check current grants independently.

| Input | Bounds and behavior |
| --- | --- |
| Lua | LuaJ 3.0.1, fresh globals without standard libraries, at most 8 MiB and 5 million instructions. A private DebugLib instruction hook is initialized before its bootstrap globals are removed; cancellation interrupts evaluation. Only typed Kotlin DTOs leave the source. |
| Current/history | `settings.reader.lua` provides typed `lastfile`; `history.lua` preserves contiguous array order, bounded to 100,000 keys. Empty data, missing files, invalid types and provider failures are distinct outcomes. |
| Sidecars | Exact adjacent `<stem>.sdr/metadata.<extension>.lua`; finite progress in [0, 1] and optional summary status. Centralized or out-of-grant sidecars are unavailable; another book's sidecar is never substituted. |
| EPUB | One private temporary copy, at most 256 MiB; ZIP at most 100,000 entries; XML at most 2 MiB, 50,000 nodes and depth budget 64. DTD/external entities and absolute/escaping archive paths are rejected. Container/OPF yields title, first creator and EPUB2/3 cover reference. Cover data is bounded to 8 MiB and checked through bitmap bounds. Android-decodable raster covers are supported; SVG covers are unsupported. |
| Additional book formats | PDF/CBZ/MOBI/AZW3 use one temporary copy up to 256 MiB; FB2 up to 64 MiB. CBZ: 100,000 entries, ComicInfo XML uses the EPUB XML budgets. FB2 streams up to 1,000,000 nodes/depth 64; only the referenced base64 cover is retained (12 MiB encoded, 8 MiB decoded). MOBI: PalmDB offsets are checked against file length, header records at most 2 MiB and EXTH at most 10,000 entries. Embedded covers at most 8 MiB; descriptions become plain text capped at 32 KiB UTF-8. DRM is not decrypted. Temporary files are removed on completion/error/cancellation. |
| Statistics | Copy `settings/statistics.sqlite3` and observed WAL/SHM into a fresh private directory, at most 256 MiB total and one million rows. A mutex covers copy/validate/read. Before/after observable metadata must match; SQLite quick_check and required `book`/`page_stat_data` columns must pass. Open only the private copy, read-only. |

Statistics retries transient transport/SQLite/change failures up to three times with fresh directories and cancellable 100/200ms backoff. Unsupported schemas are a distinct outcome. Streams, cursors, databases and temporary directories are closed/removed on success, failure or cancellation. Metadata/thumbnail storage is described in [reading cache](reading-cache.md); folder traversal bounds are in [library](library.md).

SAF cannot copy atomically while KOReader writes. Size/mtime may be missing or unchanged despite writes, and KOReader may not have flushed its current session. A validated snapshot is an observation, not a transactionally current reading total.

## Error and ownership boundaries

Source capability results and expected-error classification remain in `data/source`; source code does not import repositories. `ProbeCapabilities` classifies expected source failures and timestamps; cancellation and unexpected programming errors propagate. Exact resolution and parsing stay in the source layer. SQLite copy, integrity checks, schema checks and querying are separate operations within the same mutex/retry/cleanup boundary. Each `BoundedXmlReader` owns one parser/stack/budget per XML entry.

Provider query/open calls normalize OEM `IllegalArgumentException` into `IOException` while preserving its cause. Direct `FileNotFoundException` maps to `MissingSourceFile`; URI validation, parser errors, cancellation and permission failures retain their own handling. Disappearing files must not crash Home or erase successful cached data. Use [reliability checks](reliability.md) to exercise recovery.

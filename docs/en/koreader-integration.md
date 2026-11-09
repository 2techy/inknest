# KOReader source integration

[Tiếng Việt](../vi/koreader-integration.md) · [Documentation](README.md)

## Configuration and access

Debug and release use the same real sources. Settings → Reading & data selects the installed `org.koreader.launcher` package, a KOReader data folder and one or more independent Books folders through the system document-tree picker with persisted read grants. Invalid selections and cancellation preserve the active configuration. Obsolete grants are released; disconnect removes source keys and grants while preserving pins, appearance and onboarding.

Source identity, selected package, KOReader tree and Books trees share the existing Preferences DataStore. Room holds cached reading data. Developer tools, probe routes and runtime sample data are absent in both build types; only debug enables StrictMode logging. See [reading cache](reading-cache.md) for refresh and opening.

Only primary internal storage through Android ExternalStorageProvider is supported. Other providers/volumes are rejected. Document IDs are validated by volume/path segments; children must remain within the granted root. Source paths resolve only into configured Books or KOReader trees. Primary aliases are accepted only after canonical filesystem identity is checked on the device. Unicode, case and spaces are retained. Dot segments, sibling-prefix matches and title/basename guessing are rejected. A missing grant never triggers guessed paths or broader storage access.

## Parsers and limits

| Input | Bounds and behavior |
| --- | --- |
| Lua | LuaJ 3.0.1, fresh globals without standard libraries, at most 8 MiB and 5 million instructions. A private DebugLib instruction hook is initialized before its bootstrap globals are removed; cancellation interrupts evaluation. Only typed Kotlin DTOs leave the source. |
| Current/history | `settings.reader.lua` provides typed `lastfile`; `history.lua` preserves contiguous array order, bounded to 100,000 keys. Empty data, missing files, invalid types and provider failures are distinct outcomes. |
| Sidecars | Exact adjacent `<stem>.sdr/metadata.<extension>.lua`; finite progress in [0, 1] and optional summary status. Centralized or out-of-grant sidecars are unavailable; another book's sidecar is never substituted. |
| EPUB | One private temporary copy, at most 256 MiB; ZIP at most 100,000 entries; XML at most 2 MiB, 50,000 nodes and depth budget 64. DTD/external entities and absolute/escaping archive paths are rejected. Container/OPF yields title, first creator and EPUB2/3 cover reference. Cover data is bounded to 8 MiB and checked through bitmap bounds. Android-decodable raster covers are supported; SVG covers are unsupported. |
| Statistics | Copy `settings/statistics.sqlite3` and observed WAL/SHM into a fresh private directory, at most 256 MiB total and one million rows. A mutex covers copy/validate/read. Before/after observable metadata must match; SQLite quick_check and required `book`/`page_stat_data` columns must pass. Open only the private copy, read-only. |

Statistics retries transient transport/SQLite/change failures up to three times with fresh directories and cancellable 100/200ms backoff. Unsupported schemas are a distinct outcome. Streams, cursors, databases and temporary directories are closed/removed on success, failure or cancellation. Metadata/thumbnail storage is described in [reading cache](reading-cache.md); folder traversal bounds are in [library](library.md).

SAF cannot copy atomically while KOReader writes. Size/mtime may be missing or unchanged despite writes, and KOReader may not have flushed its current session. A validated snapshot is an observation, not a transactionally current reading total.

## Error and ownership boundaries

Source capability results and expected-error classification remain in `data/source`; source code does not import repositories. `ProbeCapabilities` classifies expected source failures and timestamps; cancellation and unexpected programming errors propagate. Exact resolution and parsing stay in the source layer. SQLite copy, integrity checks, schema checks and querying are separate operations within the same mutex/retry/cleanup boundary. Each `BoundedXmlReader` owns one parser/stack/budget per XML entry.

Provider query/open calls normalize OEM `IllegalArgumentException` into `IOException` while preserving its cause. Direct `FileNotFoundException` maps to `MissingSourceFile`; URI validation, parser errors, cancellation and permission failures retain their own handling. Disappearing files must not crash Home or erase successful cached data. Use [reliability checks](reliability.md) to exercise recovery.

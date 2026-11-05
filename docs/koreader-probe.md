# KOReader source integration

InkNest uses one source implementation in debug and release. **Settings → Reading & data**
selects the installed `org.koreader.launcher` package and a KOReader data folder
plus one or more independent Books folders. Each uses the system document-tree picker and persisted read permission. Changing
folders validates the new grant before committing configuration; invalid selection/cancellation
preserves the old configuration. Obsolete grants are released. Disconnect removes source keys
and their grants, preserving pins, display preferences and onboarding.

Source identity, the set of Books tree URIs, KOReader tree URI and selected package share the existing preferences
DataStore. No second DataStore is created. Production refresh now populates the Room current-book
cache shared by Home and Settings. Developer tools and their diagnostic repository have been removed
from debug and release; debug StrictMode logging remains enabled.
Reading cache architecture and current verification are described in [Reading cache](reading-cache.md).
Debug and release contain no runtime sample-data mode.

## Supported access and identity

Only primary internal storage through Android ExternalStorageProvider is supported. Other
providers/volumes are explicitly unsupported. Document IDs are checked by volume/path segments;
children must remain inside the selected tree. No absolute path is inferred from URI text.
Source paths resolve only into the configured Books or KOReader trees. Primary aliases are
accepted only after canonical filesystem identity is verified on the running device. Unicode,
case and spaces are retained. Dot segments, sibling-prefix matches and title/basename matching
are rejected. Missing or revoked files are rechecked before any open intent.

## Parsers and limits

- LuaJ 3.0.1 uses fresh globals without standard libraries. A private DebugLib instruction hook
  is initialized and removed with its empty bootstrap `package` table before evaluation.
  Files are bounded to 8 MiB and execution to 5 million instructions. Cancellation interrupts
  the hook; subsequent parses remain usable. Only typed Kotlin DTOs leave the data source.
- `settings.reader.lua` supplies typed `lastfile`; `history.lua` preserves contiguous array order.
  Empty data, missing files, invalid types and provider failures have different outcomes.
- Sidecars are the exact adjacent `<stem>.sdr/metadata.<extension>.lua`, with finite progress
  in [0, 1] and optional summary status. Centralized/out-of-grant sidecars are unavailable;
  another book's sidecar is never substituted.
- EPUB is copied once to a private temporary directory, bounded to 256 MiB. ZIP has at most
  100,000 entries; XML is bounded to 2 MiB, 50,000 nodes and depth 64. DTD/external entities,
  absolute/escaping archive paths are rejected. Container/OPF yields title, first creator and
  EPUB2/3 cover reference. Cover bytes are bounded to 8 MiB and verified with bitmap bounds;
  raster support follows Android's decoder. SVG covers are unsupported. Thumbnail storage/UI
  belongs to the later metadata milestone.
- Statistics reads `settings/statistics.sqlite3` and any observed WAL/SHM into a fresh private
  directory, at most 256 MiB total. A mutex covers copy/validate/read. Observable metadata before
  and after copy must match; SQLite quick_check and required book/page_stat_data columns must
  pass. Transient transport/SQLite/change errors retry once with a new directory. Unsupported
  schema is a distinct outcome. Only the private copy is opened read-only. Streams, cursors,
  databases and temporary directories are closed/removed on completion, failure or cancellation.

**Snapshot limits:** SAF cannot atomically copy files while KOReader writes. Size/mtime may be
missing or unchanged despite writes. KOReader may not have flushed current statistics. A passing
snapshot is a validated observation, not a transactionally current reading total.

## Historical source-integration verification

The observations below record the original integration gate, including its former diagnostic
UI and open-mode decisions. Current direct opening is described in [Set up InkNest](setup.md).

InkNest: `0.1.0-dev` (versionCode 1), working changes based on `567783e`.
No raw Lua, book contents or personal document URIs are recorded in this document.

| Target | Platform / source | Observation | Result / limits |
| --- | --- | --- | --- |
| Tango 2, 2026-11-03 | Android 14; KOReader org.koreader.launcher v2026.07.1; firmware not captured | KOReader=`koreader`, Books=`webdav`; independent persisted grants, cancel/restart; real lastfile and 2 history entries | Access and typed Lua parsing passed. Resolved book/sidecar/EPUB/statistics and open-mode recheck still require Tango. |
| Tango_2_Virtual, 2026-11-04 | Android 14 x86_64; build UE1A.230829.050/12077443; 862×1648, density 320; KOReader v2026.07.1 | KOReader=`koreader`, Books=`Download/Calibre Library`; real lastfile/history resolve through grants | Current/history identity passed on virtual, including KOReader-owned quickstart under its own tree. |
| Same virtual | Real adjacent EPUB sidecar, OPF and raster cover; real statistics database | Valid progress/status, title/author/cover, readable book/page counts | Passed. Counts are observations and may change after native reading. Actual WAL behavior additionally covered by Android tests. |
| Same virtual | Native A/B/A through original URIs | Warm runs reached correct books and saved positions (419/12794 and 1/7) | Warm pass; first cold VIEW unreliable. **Use SourceAppOnly**, not DirectConfirmed. Normal cold source-app launch passed. |
| Same virtual | Actual system picker/process recreation | Invalid reader folder preserves config; lost Books grant independent of KOReader; kill while picker open, then select or HOME | Passed. Original grants restored after permission test. |
| Same virtual, optimized release | Production source selection and diagnostic refresh | Same real source layer; no debug tools/mock fixtures | Source selection, successful refresh timestamp and absence of debug tools verified at runtime. Production Home/current cache is integrated; see the reading-cache guide. |

## Verification scope

The current pass has 41 shared JVM tests passing in both variants. Existing coverage retains
bounded Lua/parser recovery, configuration compatibility, path boundaries and HOME/process-death
regressions. Seven Android ZIP/XML/SQLite/WAL/identity tests passed during source-probe refactoring.
The current-cache pass adds six targeted Android tests and repeats two real SAF access tests.
The latter require explicit opt-in and an existing current-book cache; default runs never revoke
device grants. Both configured grants were restored through Settings after the opt-in test.

Debug/release builds, lint and Spotless passed. Optimized release was verified with actual
KOReader data for current cache, manual refresh, clear/reload and restart. No broad launcher/device
matrix was repeated. See [Reading cache](reading-cache.md) for the current architecture and gate.
Virtual results do not establish Tango hardware compatibility or direct-open support. Full-folder Library, metadata and statistics now use this source layer; see their dedicated guides.

## Maintainability boundaries

Production repositories use shared capability results. `ProbeCapabilities` owns
expected-source-error classification and timestamps; cancellation and unexpected programming
errors propagate. Exact book resolution and parsing stay in the source layer. SQLite snapshot
copy, integrity validation, schema validation and querying are separate operations under the same
mutex/retry/cleanup boundary. `BoundedXmlReader` owns one XML parser/stack/budget per entry.

# KOReader integration probe

InkNest uses one source implementation in debug and release. **Settings → Reading & data**
selects the installed `org.koreader.launcher` package and two independent folders: KOReader data
and Books. Each uses the system document-tree picker and persisted read permission. Changing
folders validates the new grant before committing configuration; invalid selection/cancellation
preserves the old configuration. Obsolete grants are released. Disconnect removes source keys
and their grants, preserving pins, display preferences and onboarding.

Source identity, tree URIs, selected package and verified open mode share the existing preferences
DataStore. No second DataStore or reading database is created. Refresh currently checks source
capabilities; it does not populate Home/Library. Reading cache and its clear action await the
reading-core implementation. Debug and release contain no runtime sample-data mode. Home stays empty until real reading-cache integration.

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

## Debug diagnostics and open-mode decision

**Settings → About → Developer tools → Reading source probe** exposes the shared source layer
with independent, timestamped capability outcomes. Route owns platform dispatch; ViewModel
owns jobs/saved pending picker state; Screen only renders state/callbacks. Re-run/source change,
configuration recreation and HOME cancel/close diagnostics without stale results. Pending picker
results survive process recreation; HOME clears the pending route without reopening overlays.
Probe controls and Developer tools are absent from release. Runtime mock repositories and preview controls have been removed from debug as well.

The probe opens the source app normally and can send original document URIs with MIME,
ACTION_VIEW, read permission and ClipData. Its A/B confirmations require observed correct book
and saved position in both cold and warm runs. Intent dispatch alone never confirms direct-open.
Open mode is recorded for the selected package/version; version changes invalidate confirmation.
`SourceAppOnly` is the accepted fallback. See the observed decision below.

## Compatibility matrix

InkNest: `0.1.0-dev` (versionCode 1), working changes based on `567783e`.
No raw Lua, book contents or personal document URIs are recorded in this document.

| Target | Platform / source | Observation | Result / limits |
| --- | --- | --- | --- |
| Tango 2, 2026-11-03 | Android 14; KOReader org.koreader.launcher v2026.07.1; firmware not captured | KOReader=`koreader`, Books=`webdav`; independent persisted grants, cancel/restart; real lastfile and 2 history entries | Access and typed Lua parsing passed. Resolved book/sidecar/EPUB/statistics and open-mode recheck still require Tango. |
| Tango_2_Virtual, 2026-11-04 | Android 14 x86_64; build UE1A.230829.050/12077443; 862×1648, density 320; KOReader v2026.07.1 | KOReader=`koreader`, Books=`Download/Calibre Library`; real lastfile/history resolve through grants | Current/history identity passed on virtual, including KOReader-owned quickstart under its own tree. |
| Same virtual | Real adjacent EPUB sidecar, OPF and raster cover; real statistics database | Valid progress/status, title/author/cover, readable book/page counts | Passed. Counts are observations and may change after native reading. Actual WAL behavior additionally covered by Android tests. |
| Same virtual | Native A/B/A through original URIs | Warm runs reached correct books and saved positions (419/12794 and 1/7) | Warm pass; first cold VIEW unreliable. **Use SourceAppOnly**, not DirectConfirmed. Normal cold source-app launch passed. |
| Same virtual | Actual system picker/process recreation | Invalid reader folder preserves config; lost Books grant independent of KOReader; kill while picker open, then select or HOME | Passed. Original grants restored after permission test. |
| Same virtual, optimized release | Production source selection and diagnostic refresh | Same real source layer; no debug tools/mock fixtures | Source selection, successful refresh timestamp and absence of debug tools verified at runtime. Home reading-cache integration remains deferred. |

## Verification scope

Unit tests cover bounded Lua failures/recovery, configuration compatibility, resolver boundaries
and lifecycle/navigation state. Android tests cover real ZIP/XML/Bitmap and SQLite/WAL behavior,
query cancellation, corrupt/schema failures, fresh-snapshot retry/cleanup, book identity and sidecar
separation. Opt-in source-access integration tests exercise actual configured grants without
running destructive permission checks by default. Formatting, debug/release builds, lint and both
unit-test variants are required. Current verification (2026-11-04): 43 shared unit tests passed in both debug and release after removing redundant wiring,
framework-behavior and delegation tests. Source-error classifications, parser limits, persistence
and lifecycle regressions remain covered. Seven Android parser/SQLite/identity tests passed again after the
refactor. Two opt-in real SAF access tests passed earlier; their permission-revocation scenario
was not repeated for this structural change. Debug/release APK builds, Spotless and lint completed
(lint warnings remain; no errors). Optimized release source refresh was verified before the
refactor; the shared parser/snapshot changes were then covered by both release compilation and
Android platform tests. No broad launcher/device behavior matrix was repeated.

Phase implementation and virtual source validation are complete; the Tango-specific compatibility
acceptance above remains explicit. Virtual results do not establish physical E-Ink behavior or
hardware direct-open support. Home/Library/cache/statistics screens are later milestones.


## Maintainability boundaries

The probe repository coordinates independently observed capabilities. `ProbeCapabilities` owns
expected-source-error classification and timestamps; cancellation and unexpected programming
errors propagate. Exact book resolution and parsing stay in the source layer. SQLite snapshot
copy, integrity validation, schema validation and querying are separate operations under the same
mutex/retry/cleanup boundary. `BoundedXmlReader` owns one XML parser/stack/budget per entry.

Debug `SourceProbeRoute` owns visibility, lifecycle and picker/dialog wiring. `SourceAppProbeRoute`
binds installed apps and A/B confirmations; `SourceProbeLauncher` handles Android package-version
lookup, original-file re-resolution and intent dispatch. Screens remain state/callback renderers.

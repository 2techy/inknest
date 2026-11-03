# KOReader probe

Debug builds expose **Settings → About → Developer tools → Reading source probe**.
This screen uses actual Android document-tree grants, independently of sample source settings.
Select a KOReader directory and optionally a Books directory. Cancel preserves the previous
configuration. Recheck access queries the provider and checks persisted read permission.

Currently supported storage is the primary Android ExternalStorageProvider only. Document IDs
are treated as volume/path segments; arbitrary providers and other volumes are rejected. No
absolute file path is inferred from a URI, and no broad storage/write permission is requested.

A successful access check means the selected directory is readable. Use **Read KOReader data**
to read `settings.reader.lua` and `history.lua` relatively from that tree. Results distinguish
missing files, invalid data, lost permission, provider failure and parser limits. A success can
still represent no current file or an empty history. This does not establish sidecar/EPUB/statistics compatibility. Production Settings
continues to use its prior source boundary until the real integration is connected.

Source keys share the existing DataStore but do not alter pins or appearance/onboarding keys.
Mock selections never write these keys. Source package selection and disconnection/grant cleanup
will be connected as the remaining M2 capabilities are implemented.

Virtual QA uses dedicated empty folders under Download/InkNestProbe. These directories establish
SAF access only, not reader compatibility. Tango/KOReader content and direct-open verification
remain hardware acceptance work; M2 is not complete.

## Tango access check — 2026-11-03

Tango 2, Android 14, KOReader `org.koreader.launcher` v2026.07.1:
KOReader tree is `koreader`; Books tree is `webdav`, as confirmed by the user.
Both read grants and the saved configuration survived an app process restart.
Recheck and picker cancellation passed. The initial `Books` directory selection was
superseded by `webdav`. This establishes directory access only; content parsing and
reader compatibility remain untested.

## Lua implementation and current verification

The data layer uses LuaJ 3.0.1 with fresh globals for each file. No standard host libraries
are installed. A private DebugLib instruction hook is initialized then removed from the Lua
namespace along with its empty bootstrap package table. Execution is bounded to 5 million
instructions and observes coroutine cancellation; input is bounded to 8 MiB. IO runs on IO,
execution/DTO mapping on Default. The dependency notice is included in About acknowledgements.

`ReaderLuaDataSource` validates types and preserves history array order. Sparse/corrupt history
is an error, not a successful empty list. Only Kotlin data leaves the source layer. The probe
serializes read jobs, cancels on disposal/source change and does not log Lua or book paths.

Virtual fixtures: current file plus two history entries; instruction-limited current settings
with independent successful history; then valid current settings parsed again. Unit tests also
cover cancellation followed by successful parsing. Tango SAF grants were verified previously,
and Tango was subsequently reconnected: real reader settings and two history entries parsed successfully.
Path-to-book identity resolution, sidecars, EPUB, statistics snapshots and direct-open remain
pending. Reading data on Home remains unchanged.

API reference used for the private hook: https://www.luaj.org/luaj/3.0/api/org/luaj/vm2/lib/DebugLib.html

## Tango Lua check — 2026-11-03

The debug probe read the configured KOReader tree through SAF after reinstalling the parser
build. Reader settings returned a nonempty current-file path, and history returned two entries.
A separate read-only structure check counted two file fields in the original history file.
Settings/history sizes were 14,477 / 444 bytes. No book paths or Lua payloads were logged or
saved as fixtures; KOReader files were not modified. Both configured tree grants remained valid.

| Capability | Result |
| --- | --- |
| KOReader and webdav tree read access | Supported on this Tango setup |
| settings.reader.lua, typed lastfile | Supported; path recorded |
| history.lua, typed array entries | Supported; 2 entries |
| Resolve current/history paths into granted webdav files | Not tested / pending implementation |
| Sidecar, EPUB, statistics, direct-open | Not tested / pending implementation |

This validates parsing on KOReader v2026.07.1, not the entire M2 gate. The probe was left open
on Tango showing current/history results.

## Code boundaries

- `SourceProbeRoute`: lifecycle-aware collection, picker and dialog ownership.
- `SourceProbeScreen`: state/callback rendering, without repositories or Android storage access.
- `SourceProbeViewModel` / `SourceProbeUiState`: access actions, cancellation and stale-result guards.
- `ReaderProbeRepository` / `ReaderProbeResult`: serialized, independently typed capability outcomes.
- `SourceFileReader`: testable transport contract; `SafTreeReader` enforces granted-tree boundaries.
- `ReaderLuaDataSource` / `BoundedLuaLoader`: strict DTO conversion and bounded isolated evaluation.

Completed scope is persisted tree access plus bounded reader-settings/history diagnostics.
This does not mark M2's unresolved identity/sidecar/metadata/statistics/open-mode gate complete.

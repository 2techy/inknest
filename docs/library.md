# Library

Library indexes all recognized book files recursively inside the selected Books SAF tree, including
books never opened in KOReader. KOReader history is optional enrichment for reading status,
progress and last-read time, not the source of library membership. Files outside the selected
folder do not appear just because they exist in reader history.

## Data flow

`SafBookCatalogSource` lists the granted primary-storage tree through ContentResolver. It validates
that every document remains inside the root, skips hidden/sidecar folders, prevents repeated-directory
traversal, supports cancellation and bounds depth to 64 and total entries to 100,000. Recognized
extensions include EPUB, PDF, DjVu, MOBI/AZW, FB2, text, comic archives and common document formats.
It does not scan other device folders or request broad storage access.

`FolderLibraryRepository` owns an application-scoped shared scan and a Room inventory partitioned
by source identity and Books tree. A complete discovery replaces membership transactionally; a
listing failure preserves the previous inventory. Metadata/cover enrichment is published in small
batches, reusing known file fingerprints. A completed inventory survives restart; an interrupted
one resumes. Initial setup, folder changes and cache reset trigger indexing. Manual Refresh library
picks up added/deleted files; tab switches and clock ticks do not scan the folder.

The library tables share the existing reading Room database, currently schema v3. Development uses
destructive version fallback without migration code. Preferences, pins and tree grants are separate.
Clearing reading cache also clears the inventory/covers and lets the active folder rebuild. Source
changes cancel obsolete scans; observations and writes are checked against the active folder.

EPUB metadata and raster covers use the existing bounded adapter. Previously validated document
URIs can be read directly after checking their tree grant and containment, avoiding a directory
search for every file. Other formats or missing artwork use a plain book jacket with filename and
format. Unsupported/missing author and progress are omitted from the shelf instead of repeated
technical placeholders. New files retain unknown reading status; lack of history is not proof of
an unread state.

## Screen and interactions

The shelf uses the shared header/design tokens, a compact horizontally scrollable status strip,
grid/list mode, search and a static sort/refresh menu. Book covers and titles are the main content;
there is no permanent diagnostics/count paragraph above them. Grid captions are compact; list
rows show more metadata. Search matches title/known author ignoring case. Editing/selection stays
local so background filtering cannot reorder typed characters; late IME commits after closing are
ignored. Small query/filter/sort/view choices live in SavedStateHandle; lazy states restore scroll.

Default order is last-read descending, with title A–Z and progress descending alternatives.
Unknown values follow known values and ties retain stable folder order/identity. Status filters use
explicit KOReader status; 100% alone does not imply Finished.

With version-confirmed direct opening, tapping a book opens that book. Otherwise tapping shows its
book information and folder path with an explicit Open KOReader action; the dialog explains that
the reader must be used to choose the book. This keeps source-app fallback honest without placing
integration details across the whole shelf. Background/HOME dismisses the dialog and pending launch.
No opening action fabricates reading history or progress.

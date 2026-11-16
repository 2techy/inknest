# Library

[Tiếng Việt](../vi/library.md) · [Documentation](README.md)

Library indexes all recognized book files recursively inside the selected Books SAF trees, including
books never opened in KOReader. KOReader history is optional enrichment for reading status,
progress and last-read time, not the source of library membership. Files outside the selected
folder do not appear just because they exist in reader history.

## Data flow

`SafBookCatalogSource` lists the granted primary-storage tree through ContentResolver. It validates
that every document remains inside each root, skips hidden/sidecar folders, prevents repeated-directory
traversal, supports cancellation and bounds depth to 64 and total entries to 100,000. Recognized
extensions include EPUB, PDF, DjVu, MOBI/AZW, FB2, text, comic archives and common document formats.
It does not scan other device folders or request broad storage access.

| Recognized extensions | Metadata and artwork |
| --- | --- |
| `epub` | Extracted title, first creator and supported raster cover when available. |
| `pdf`, `djvu`, `djv`, `mobi`, `azw`, `azw3`, `fb2`, `txt`, `cbz`, `cbr`, `rtf`, `doc`, `docx`, `html`, `htm`, `chm`, `pdb` | Filename-based entries and placeholder covers. |

Recognition means the file can appear in Library; opening still requires a compatible KOReader handler. Reading status/progress can be enriched from KOReader independently of embedded metadata support.

`FolderLibraryRepository` owns an application-scoped shared scan and a Room inventory partitioned
by source identity and Books tree. A complete discovery replaces membership transactionally; a
listing failure preserves the previous inventory. Metadata/cover enrichment is published in small
batches, reusing known file fingerprints. A completed inventory survives restart; an interrupted
one resumes. Initial setup, folder changes and cache reset trigger indexing. Manual Refresh library
picks up added/deleted files; tab switches and clock ticks do not scan the folder.

The library tables share the existing reading Room database, currently schema v8. Development uses
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
grid/list mode, search and a static sort/refresh menu. Book covers and titles are the main content.
Grid and list captions show only title and known author. Search matches title/known author ignoring case. Editing/selection stays
local so background filtering cannot reorder typed characters; late IME commits after closing are
ignored. Small query/filter/sort/view choices live in SavedStateHandle; lazy states restore scroll.

Default order is last-read descending, with title A–Z and progress descending alternatives.
Unknown values follow known values and ties retain stable folder order/identity. Status filters use
explicit KOReader status; 100% alone does not imply Finished.

Tapping or long-pressing a book opens one Book details page. Its filled reading button opens the
original document through `ReadingOpenRepository` and `ReadingItemDispatcher`. Details hides main
navigation and tab swipes. Back preserves the shelf query, filters and scroll; returning from the
reader preserves details. HOME closes details and cancels pending dispatch. Unsupported opening requests display an error.
Reading history and progress remain sourced from KOReader.

## Browsing authors

Library offers All books (flat shelf) and Authors (name/count rows leading to a book shelf).
The source-folder submenu remains available with one configured root. It filters
configured sources, not filesystem subdirectories. Search/status filtering happens before
author counts; book sorting applies within shelves. Authors are normalized for whitespace
and case without removing accents, splitting creator strings or guessing from filenames.
Missing metadata is grouped under Unknown author, last in the list.

Browse mode and selected author are restored through SavedStateHandle.
Back closes search first, then returns from an author shelf to the author index. Navbar stays
visible. Browsing uses cached catalog projections and never initiates a source scan.

Library keeps Search and More on the toolbar. More groups Browse library, Folders,
Book layout and Sort by into single-level submenus; Refresh stays a direct action.
Author index omits book-only layout/sorting. Submenus provide a back-arrow header and mark the
current selection; dismiss/Back closes the popup and backgrounding clears it.
Folder/browse choices stay inside More, without a summary below the page title. Search uses the
shared inline Apps/Library field with a single-line placeholder and 48dp touch bounds.
Actions are contextual rather than limited by screen width.

## Book details

The unframed hero places the cover beside the full title, author, progress/pages, a static 4dp
track and last-read timestamp. The filled reading button spans the content below it. Typography,
cover sizing and spacing reuse Home/design tokens; narrow widths and large fonts stack the hero.
Finished status replaces the shelf percentage badge even without 100% progress; 100% alone is
still not Finished. Captions never repeat status or access errors.

Optional EPUB descriptions are cleaned into plain text, preserve paragraph breaks and are capped
at 32 KiB. They start at four lines and expand inline; absent descriptions reserve no space.
Reading statistics shows total time, recorded pages and a numeric reading-day count. History and
file information expand inline rather than opening more destinations. History includes first/last
reading dates and newest-first daily totals, loaded 30 days at a time. File details shows format,
path and availability. Missing books and inaccessible sources remain explicit states.

Statistics uses imported KOReader book IDs linked by compatible partialMD5, never by title/author.
Only the opened book is sampled (at most 12 KiB); no full-file copy is performed for identity.
Providers without seek support leave statistics unlinked. Checksum cache hits still recheck SAF
grants/containment and file fingerprints. Multiple IDs with the same checksum use existing
book/day/layout page semantics. Missing statistics differs from a successful empty history.
Description/accordion choices and history limits restore with the selected book identity.

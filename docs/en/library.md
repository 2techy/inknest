# Library

[Tiếng Việt](../vi/library.md) · [Documentation](README.md)

Browse recognized books in your selected Books folders, including subfolders and books never opened in KOReader. History enriches reading status/progress; it does not determine library membership.

## Screen and interactions

- Search titles/known authors without case sensitivity. Filter by reading status; More offers browsing, source folders, grid/list layout, sorting and Refresh.
- Default sort is last-read descending; alternatives are title A–Z and progress descending. Unknown values follow known values; ties keep stable folder/identity order. Status comes from KOReader: 100% alone does not mean Finished, and no history does not prove Unread.
- A folder choice filters configured roots, not filesystem subdirectories. Refresh scans all roots. An unavailable root keeps its snapshot while available roots update.
- Query, filters, sort, layout and scroll restore when returning. Search editing stays local to avoid background filtering reordering input; late IME commits after closing are ignored.
- Tap or long-press a book to open details. Back preserves the shelf; returning from KOReader preserves details. HOME closes details and cancels pending opening. Details hides main navigation/tab swipes.

## Browsing authors

More → Browse library switches between All books and Authors. Authors shows name/count rows leading to shelves. Search/status filters apply before counts; book sorting applies inside shelves. The author index omits book layout/sort controls.

Names normalize whitespace/case without removing accents, splitting creator strings or guessing from filenames. Missing authors appear last under Unknown author. Browse mode/author restore through SavedStateHandle. Back closes search before returning from an author shelf to the index. Browsing cached data never scans sources.

## Book details

The reading button opens the original document through the [shared opening flow](reading-cache.md). Missing books and inaccessible sources have explicit states.

- Last-read appears below progress when known; history omits first/last-read summaries. The filled reading action stays below the hero at full width. Books without progress use a compact cover; unmatched statistics show a plain empty state, with Retry reserved for errors.
- Book descriptions are cleaned to plain text, preserve paragraphs and cap at 32 KiB. They expand inline from four lines; absent descriptions reserve no space.
- Statistics shows total time, recorded pages and reading-day count. Expand history for newest-first daily totals, five days per page with Previous/Next controls; file details shows format, path and availability.
- Description/expander state and history page restore with the selected book identity.

Statistics links imported KOReader IDs by compatible partialMD5 within the source, never title/author. Only the selected book is sampled, at most 12 KiB, without a full-file copy. Providers without seek support leave statistics unlinked. Checksum-cache hits recheck grants, containment and file fingerprints; cache clear prevents pending work restoring deleted rows. Multiple matching IDs retain [book/day/layout page semantics](statistics.md). Unavailable statistics differs from successfully empty history.

## Data flow

| Recognized extensions | Metadata and artwork |
| --- | --- |
| `epub` | Title, first creator and supported raster cover when available. |
| `pdf` | Filename and a thumbnail of the first page; password-protected files use a placeholder. |
| `fb2` | Title, first author, plain-text annotation and referenced raster cover. |
| `cbz` | ComicInfo Title/Writer/Summary when present; FrontCover or the first naturally sorted image. |
| `mobi`, `azw3` | Unencrypted KF7/KF8, including hybrid files: title, first author, description and embedded cover/thumbnail. |
| `djvu`, `djv`, `azw`, `txt`, `cbr`, `rtf`, `doc`, `docx`, `html`, `htm`, `chm`, `pdb` | Filename and placeholder cover. |

Recognition permits listing; opening still needs a compatible KOReader handler. Missing author/progress stays unknown and is omitted from shelf captions.

`SafBookCatalogSource` lists granted primary-storage trees through ContentResolver, checks containment, skips hidden/sidecar directories, prevents repeated traversal and supports cancellation. Limits are depth 64 and 100,000 entries. It never scans outside configured roots or requests broad storage access.

`FolderLibraryRepository` shares an application-scoped scan and partitions Room inventory by source/Books tree. Complete discovery replaces membership transactionally; listing failure retains inventory. Enrichment publishes small batches and reuses known fingerprints. Validated URIs still recheck grants/containment before direct reads. Completed inventory survives restart; interrupted work resumes.

Initial setup, folder changes and cache reset trigger indexing; manual Refresh discovers added/deleted files. Tab/clock changes do not scan. Source changes cancel obsolete scans; observations/writes check the active folder. Clear reading cache removes inventory/covers and rebuilds active roots. See [cache/storage](reading-cache.md), [parser limits](koreader-integration.md) and [design tokens](design-system.md).

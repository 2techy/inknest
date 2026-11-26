# Reading statistics

[Tiếng Việt](../vi/statistics.md) · [Documentation](README.md)

Read time, recorded pages and reading activity from KOReader's `settings/statistics.sqlite3`. Library inventory and reading history do not supply these totals.

## Screen and units

Choose Time/Pages or Refresh in the header menu; pull-to-refresh is also available. Units apply to Today, daily average, streak, totals, chart and heatmap. Daily average includes all 30 calendar days through today, including zero-activity days. A streak ends today if active; otherwise yesterday's streak remains until today ends. Missing page data is unknown, not zero.

Totals cover This week (Monday onward), This month, This year and All time. The trend filter changes only the chart's range/total:

| Filter | Calendar window | Columns |
| --- | --- | --- |
| Week | Current Monday–Sunday | Days |
| Month | Current month | Monday–Sunday weeks clipped at month edges, labelled by day range |
| Year | Current year | January–December |
| All | First recorded year through current year | Years; one current-year column when empty |

Current buckets include days through today; future slots are blank, past gaps zero. Color uses blue bars and highlights the bucket containing today in clay when it has a positive value. Labels/bars scroll together while the value axis stays visible. Unit/period changes use cached daily summaries without fetching sources.

The responsive heatmap shows 16–52 Monday–Sunday week columns. Future dates are absent; unknown days have a diagonal mark. Four activity shades use lower 25/50/75-percentile cutoffs of known positive values in the visible window, separately for Time/Pages. Zero/unknown values do not affect thresholds; equal positive values use shade 3. Shades show relative activity, not fixed minute/page thresholds. See [design system](design-system.md) for presentation rules.

## Day details

In **Reading trend → Week**, tap a column or its day label to see reading time, recorded pages and books for that date. Today and past days can open, including dates with no activity. Back restores the chart position and filters.

Books are ordered by time spent reading. InkNest shows the shared profile title when the checksum links to a single profile in the selected source. Other books use their imported KOReader title, or **Unknown book title** if the source has no title. Opening the detail reads the existing cache. A failed refresh keeps the saved figures with a warning.

Tap a book with a chevron to open **Book details**. The link appears when a cached checksum identifies one file in the current Library. Opening the book's details from Library can populate that checksum. Back returns to the day you were viewing.

## Page counting contract

- Each positive-duration valid row contributes all its seconds. Pages deduplicate `(book ID, page, total_pages layout)` within each local day. Revisits retain time; a different book, recorded layout or day is a separate observation. Period totals sum daily counts.
- Invalid/missing page values retain valid time but make that day's pages unknown. Missing legacy `total_pages` uses an unspecified layout; counts are never inferred from titles, duration or row count.
- These are recorded pages, not sessions, completed books or globally unique pages. KOReader can rescale history through its [`page_stat` view](https://github.com/koreader/koreader/blob/master/plugins/statistics.koplugin/main.lua); InkNest preserves recorded layouts, so totals can differ after font/layout changes.

## Persistence and refresh

A missing DB, denied grant or unsupported schema is unavailable. Only a successful empty snapshot means zero activity; no runtime mock data is used. [Source integration](koreader-integration.md) owns SQLite copy/validation, retry limits and SAF snapshot limitations; unflushed reading is absent. Malformed numeric/time rows are rejected; missing page metadata retains valid time.

- Import streams at most 500 events per batch into a private Room staging generation. Raw page/layout events remain on disk; SQL precomputes daily seconds/pages. UI state contains only summaries, generation, timezone and snapshot time.
- A fully closed successful snapshot activates atomically. Each retry uses a separate generation; failure/cancellation retains the previous active one. Refresh replaces rather than appends.
- Within the running process, refresh reuses the active generation when the DB/WAL/SHM fingerprint matches the last successful import, the source and timezone match, and Room still holds that generation. Skipping retains the snapshot timestamp; the refresh attempt still completes. Cache clear, source changes, rebinning or process restart require another import. Fingerprint rules and limitations are in [source integration](koreader-integration.md).
- An import token registered before copying is invalidated by cache clear. Cleanup follows Room's committed pointer even if cancellation races activation; abandoned generations are removed after operations and at startup.
- The KOReader tree partitions statistics; changing Books does not invalidate it. Clear/disconnect removes events, summaries, tokens and timestamps while preserving preferences/pins. See [cache storage](reading-cache.md).
- Startup, foreground return and Home/Settings/Stats refresh share an application-owned job. Source transitions prevent older snapshots repopulating disconnected data. Tab, unit, period, search and clock changes never read SAF.

## Calendar and state

Epoch-millisecond timestamps use the current system timezone. Rows crossing midnight belong to their start day using ZoneId, including DST. Timezone changes re-bin raw rows in batches of at most 500 into a new generation without SAF or changing the source timestamp. Future summaries stay on disk and appear only when their day arrives. Keep seconds until display.

Room handles SQL/bounded imports, IO handles snapshot/source work and Default handles dashboard projection; Screens do no parsing. StatsViewModel saves unit/period (restoring legacy granularity only when no period is saved); Routes collect lifecycle-aware state. The injected shared `StatisticsClock` checks date/zone once per minute, with application-scoped timezone maintenance. Tests replace clocks/dispatchers.

## Home reading rhythm

Home uses the same daily summaries for a time-only calendar of up to 52 weeks, projected on injected Default. Mini stats show today's time and the time-based streak. The default-enabled Reading rhythm preference is independent of other Home sections.

No snapshot hides the calendar; a successful empty snapshot shows it empty. Refresh errors retain cached content with a stale notice. Opening Stats preserves its unit.

## Per-book statistics in Library

Book details uses a generation-scoped book-ID/checksum index over Room events. Its bounded import activates atomically with the snapshot; rebinning copies it and clear/pruning removes it. Daily history paginates newest first using the same page/unknown semantics. The dashboard continues to observe daily summaries, without raw per-book events in UI state. See [Library](library.md) for identity matching.

Each imported book references a profile in `books`. The source title remains available for books without a local file or checksum. The profile lookup uses a shared file profile when the checksum identifies a single profile in the selected source. Rebinning retains these references.

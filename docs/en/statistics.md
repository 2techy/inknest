# Reading statistics

[Tiếng Việt](../vi/statistics.md) · [Documentation](README.md)

Statistics reads KOReader's `settings/statistics.sqlite3` through the selected KOReader
SAF tree. Books inventory and reading history do not supply duration or page counts.
A missing database, denied grant or unsupported schema is unavailable; only a successful
empty snapshot means zero recorded reading. Runtime mock data is not used.

## Screen and units

The header's three-dot menu selects Time/Pages with the active choice marked, and offers
Refresh. Pull-to-refresh remains available. The unit controls Today, Daily avg, the streak's
activity criterion, period totals, heatmap intensity and the chart. The screen orders three
outlined icon cards for Today, daily average and current streak, then trend, activity calendar
and period totals. Sections follow Home: 24dp before the next section divider, 12dp
from divider to heading, and 12dp from heatmap/totals headings to content. The trend filter
retains compact internal spacing and its 48dp touch bounds. Daily average covers the last 30 calendar days including today, with
zero-activity days included. The streak ends today when today has activity; otherwise yesterday
may continue it until today ends. Missing page data is unknown, not zero.

Reading totals show This week (Monday onward), This month, This year and All time.
The filter inside Reading trend changes only the chart's range and total:

| Filter | Calendar window | Columns |
| --- | --- | --- |
| Week | Current Monday–Sunday | Days |
| Month | Current month | Monday–Sunday weeks clipped at month edges, labelled by day range |
| Year | Current year | January–December |
| All | First recorded year through current year | Years; one current-year column when empty |

Current buckets include only days through today; future slots are blank and past gaps are
zero. Labels and bars scroll together when needed, while the value axis stays visible.
Highlights, heatmap and reading totals retain their own periods. All modes use cached daily
amounts without another source fetch.

The heatmap spans the full content width with a responsive 16–52 Monday–Sunday week
columns (about 26 weeks at the Tango-sized width). A 48dp weekday-label gutter grows with
font scale. Square cells are capped at 10dp, with roughly 3dp horizontal and 2dp vertical gaps. It retains month/weekday axes but has no week-count caption
or Less/More legend. Future dates remain absent; unknown days retain their diagonal mark.
Empty cells have pale 0.75dp outlines; activity uses solid grayscale and 2dp corner rounding.

Intensity is relative to known positive values in the visible window, separately for Time
and Pages. The lower 25/50/75-percentile cutoffs split activity into four shades. Zero and
unknown days do not shift the thresholds. When all active values are equal (including one
active day), shade 3 keeps that activity visible instead of blending into empty cells. The
shading reflects relative activity, not fixed minute/page thresholds. Totals and
recorded page semantics are unchanged.

Library filters and Stats period tabs share ChoiceTab: 12sp/16sp medium text, a 32dp minimum
visible surface and at least 48dp touch bounds. Stats groups its period tabs in one compact outlined
segmented control; the selected surface fills the visible 32dp frame while the touch bounds
remain at least 48dp. Its narrow options menu aligns with the trailing header edge. Text follows system font scale; pages scroll vertically, and overflowing
tab strips and charts scroll horizontally. Charts cap bar width to avoid large black blocks
for sparse data and use readable minute/hour/page scale steps.

## Page counting contract

Each valid positive-duration row contributes its full seconds. Pages use the source's
recorded `(book ID, page, total_pages layout)` identity, deduplicated within each local day.
Revisiting the same page on the same day does not add another page, but its time is retained.
Another book, another recorded layout or another local day is a separate page observation.
Period totals and monthly chart buckets sum those daily page counts, so all views agree.
These are recorded reading pages, not completed books, sessions or globally unique pages.

Invalid/missing page values retain valid duration and make that day's page count unknown.
Time remains usable. An absent legacy `total_pages` column uses an unspecified layout
identity; it is not inferred from a title. Counts are not estimated from time or row count.

KOReader's own [statistics implementation](https://github.com/koreader/koreader/blob/master/plugins/statistics.koplugin/main.lua)
can rescale historical page records through its `page_stat` view to a book's current page
count. InkNest currently preserves recorded layouts instead. Font/layout changes can therefore
produce totals different from KOReader's rescaled view and dashboard.

## Persistence and refresh

Production uses a bounded snapshot reader. It copies SQLite and
existing WAL/SHM into a private temporary directory, validates metadata, integrity and required
columns, opens read-only and removes the copy in finally. Source change/cancellation interrupts
queries. Up to three attempts handle transient snapshot failures; SAF remains best effort while KOReader writes.
Unflushed reading is absent. At most one million rows and 256 MiB of source files are accepted.
Malformed numeric/time values are rejected; missing page metadata does not discard valid time.

The reader streams at most 500 events per batch into a private Room staging generation.
Room schema 8 keeps raw page/layout events on disk and precomputes daily seconds/page counts
in SQL. UI state exposes only daily summaries, the generation, timezone and snapshot timestamp;
it never retains the full event list or per-event page sets.

A successful, fully closed snapshot activates its generation atomically. Retry attempts use
separate generations; failure/cancellation preserves the previous active snapshot. An import token
registered before copying is invalidated by cache clear, so pending copies cannot repopulate it.
Cleanup uses Room's committed pointer, including when cancellation races with activation. Abandoned
generations are removed after each operation and pruned at startup. Refresh does not append or double count.
The KOReader tree partitions this cache; changing Books does not invalidate statistics.
Clear reading cache/disconnect includes statistics, preserving UI preferences and pins.
Development schema changes rebuild disposable reading data without migrations.

Startup, foreground return and explicit Home/Settings/Stats refresh update statistics. Concurrent
callers share an application-owned job. Tab, unit, chart period, search and clock changes never read SAF.
The reading-source transition mutex prevents older snapshots repopulating disconnected data.

## Calendar and state

Timestamps are epoch milliseconds internally, interpreted in the current system timezone.
Rows crossing midnight belong to their start day, calculated with Java ZoneId (including DST).
Timezone changes re-bin raw Room rows in batches of at most 500 into another staging generation,
without rereading SAF or changing the source update timestamp. Future summaries remain on disk;
Home and Stats exclude them until their calendar day arrives. Duration and page data remain
independent of the current book. Time arithmetic retains seconds until display.

SQL aggregation and bounded imports run through Room, source/snapshot I/O on IO and dashboard
projection on Default; no parsing is done by a Screen. Home mini stats and the Stats dashboard share
the same daily summaries. Unit/period changes do not read raw rows, and source changes select
the corresponding cached generation.
StatsViewModel owns unit/period in SavedStateHandle (restoring legacy granularity choices when no period is saved). Route collects lifecycle-aware state; Screen
renders state/callbacks. The calendar checks date/timezone at minute resolution; repository timezone maintenance is application-scoped.

Repository and StatsViewModel use the same injected StatisticsClock. Its shared state checks
date/zone once per minute; tests use a fake clock and injected dispatchers. Startup refresh
belongs to the foreground coordinator, while later KOReader source changes still refresh automatically.

## Home reading rhythm

Home shows a time-only calendar between the current book and pinned apps. It uses the same
responsive 16–52 week density, labels, square cells and grayscale intensity as Stats.
Future dates are absent. The clock mini stats show today's reading time and a time-based
reading streak, including yesterday's streak until today ends when today has no activity.
The three Stats highlights (Today, daily average and streak) share one row with centered values above their labels; text wraps at
larger font scales without shrinking it.

Home projects up to 52 weeks of daily summaries from StatisticsRepository on an injected
Default dispatcher and observes the shared StatisticsClock. No snapshot hides the calendar;
a successfully imported empty snapshot shows an empty calendar. Refresh errors retain the
cached calendar and expose the stale-data notice. Opening Stats preserves its selected unit.
Appearance has an independent, default-enabled Reading rhythm toggle in DataStore.

## Per-book statistics in Library

Book details queries the same raw events in Room through a generation-scoped book-ID/checksum
index. Importing that index is bounded and atomic with snapshot activation; rebinning copies it,
and cache clear/pruning removes it. Daily history is paginated newest first. The existing daily
book/page/layout deduplication and unknown-page semantics apply unchanged. The main dashboard
continues to observe only daily summaries and does not retain per-book raw events in UI state.

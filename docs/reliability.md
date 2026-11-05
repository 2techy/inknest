# Reliability and performance checks

## Current evidence

The 2026-11-05 local pass uses the Android 14 x86_64 Tango-sized virtual device
(862×1648, density 320), KOReader 2026.07.1, 12 book files with covers and four
statistics rows totaling 248 seconds. These observations are not a physical-device benchmark.

| Area | Verification |
| --- | --- |
| Statistics source | Cache matches all four real source rows and 248 seconds |
| Snapshot safety | Android WAL/retry, invalid schema, corrupt SQLite and malformed numeric rows |
| Cache consistency | Atomic replace, repeated refresh without double count, error retains previous data, shared request and source switch |
| Calendar | Year/month/day boundaries, Monday week, DST, timezone re-bin and cross-midnight start day |
| Native restoration | Stats destination and 12-month range survive background process death; cached summary restores |
| Layout | Stats ranges, Home mini stats, chart and scrolling at font scale 150%; Library/Apps remain reachable |
| Main-thread I/O | StrictMode found private-directory access during synchronous graph creation; graph now initialized off main; native rerun has no logged violations |
| Temporary data | Source snapshot/EPUB copies close and delete in finally; startup cleans interrupted InkNest temp directories and cover writes before new source jobs |
| Backup | Backup disabled; explicit cloud/device-transfer exclusions, without enabling backup to silence lint |

A sampled debug cold start reported Android `TotalTime` 592 ms; PSS after visiting data tabs
was 139,897 KiB. These are single emulator samples, not a regression threshold or Tango
performance claim. Artifacts are local under `app/build/phase-b-qa/`.

Source reads, Room work, metadata and statistics aggregation run off main. App icons have a
bounded decoded cache; thumbnails are at most 480 px with a shared 4 MiB decoded cover cache.
Statistics keeps raw events in Room and streams source imports/re-binning in batches of at most
500 rows. Home and Stats retain daily summaries. Calendar checks do not read SAF; timezone
maintenance is application-scoped and rebuilds summaries from local Room rows. Tab/range/search/clock changes
do not initiate folder scans. Application-owned shared refreshes survive individual caller
cancellation; source changes cancel obsolete requests. Expected failures preserve content.

## Physical acceptance still required

On Tango, measure source refresh, cold/warm launch and memory with the real book dataset.
Inspect text/lines and ghosting on the E-Ink panel while changing tabs, clock minute and
refreshing. Record firmware and dataset with results. Screenshots cannot certify these.

Recheck source access and opener after KOReader/firmware updates. A direct-open confirmation
belongs to the observed package version; otherwise use source-app fallback. The signed public
release must still be checked on Tango/S25 before publication. Do not repeat the entire older
launcher matrix unless a changed path or failure justifies it.


## Tango RAM follow-up — 2026-11-05

The signed release using batched statistics imports was sampled on Tango 2 (Android 14,
824x1648 at density 320) with 11 Library books. Warm tab rounds were about 85–88 MiB PSS;
the first Apps sample reached 110 MiB. Three Stats refreshes sampled at about 250 ms plus
meminfo command latency stayed within 84–88 MiB PSS. Stats values matched the old release.

The old installed release had individual tab samples of 88–121 MiB PSS. It had a longer-lived
process and fewer baseline repetitions, so these observations do not establish a percentage
reduction or a worst-case peak. RSS and the OEM's historical memory maximum are different
metrics. Graphics remained about 43 MiB of private memory in the final Stats sample.
Raw evidence and exact limits are local in `app/build/reports/ram-performance-tango/summary.md`.
A controlled A/B and long reading-session return remain follow-up checks.

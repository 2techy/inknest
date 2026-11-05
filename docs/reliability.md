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
bounded decoded cache; thumbnails are at most 480 px. Statistics clock collection stops with
UI subscribers; it re-bins cached events without reading SAF. Tab/range/search/clock changes
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

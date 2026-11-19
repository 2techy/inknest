# Kết quả benchmark

[English](../en/benchmark-results.md) · [Quy trình](benchmarking.md)

AVD: `Tango_2_Virtual`, Android 14/API 34, x86_64, 862×1648, density 320, font scale 1.0. Fixture: 1.000 sách, 200 history và 50.000 events. Target `benchmark` tối ưu, `CompilationMode.None`.

Tên scenario dùng thống nhất theo code hiện tại; report thô giữ identifier gốc.

## 2026-11-19 — Benchmark cuối

Pass 7/7 scenario × năm vòng. So với mốc 2026-11-18; operation/startup dùng median, frame dùng P95. Đây là so sánh tích lũy, không tách riêng tác động của refactor hoặc baseline profile.

| Scenario | Chỉ số | Tham chiếu | Cuối | Thay đổi |
| --- | --- | --- | --- | --- |
| `coldStartup` | Hiển thị đầu (ms) | 712.29 | 350.24 | −50.83% |
| `warmStartup` | Hiển thị đầu (ms) | 424.77 | 133.81 | −68.50% |
| `homeReturnUnchanged` | Foreground refresh (s) | 140.174 | 2.001 | −98.57% |
| `homeReturnChanged` | Foreground refresh (s) | 140.393 | 5.813 | −95.86% |
| `indexLibrary` | Index (s) | 29.693 | 20.261 | −31.76% |
| `mainTabNavigation` | CPU frame P95 (ms) | 85.61 | 37.14 | −56.61% |
| `libraryGridScroll` | CPU frame P95 (ms) | 76.00 | 21.35 | −71.90% |

Report: `final-Measure`; base `32fc7ec` + profile sinh mới; APK `5C75446F…`. Report tham chiếu: `baseline-step1-user-20261118-201053-Measure` và `scroll-grid-selection-fix-20261118-Measure` (scroll). `CompilationMode.None`; lượt này không đo lợi ích của profile sinh lại.

Xác minh cài đặt profile sinh lại: pass (`final-profile-clean-20261119-191031-352-VerifyProfile`, APK `E4B9A1C3…`).

## 2026-11-19 — Lập chỉ mục Thư viện

Mỗi bản pass 2/2 scenario × năm vòng, cùng instrumentation. Phần trăm so với batch 4; index/metadata đầu/bộ nhớ dùng median, frame dùng P95.

| Chỉ số | Batch 4 | Batch 16 | Batch 32 |
| --- | --- | --- | --- |
| Index (s) | 28.415 | 24.002 (−15.53%) | 23.525 (−17.21%) |
| First metadata (s) | 0.728 | 0.943 (+29.52%) | 1.286 (+76.75%) |
| Metadata batches | 250 | 63 (−74.80%) | 32 (−87.20%) |
| Index frames | 256 | 68 (−73.44%) | 38 (−85.16%) |
| Index CPU frame P95 (ms) | 27.044 | 32.011 (+18.37%) | 36.272 (+34.12%) |
| Scroll CPU frame P95 (ms) | 31.014 | 30.283 (−2.36%) | 32.917 (+6.14%) |
| Index PSS (MiB) | 62.319 | 60.220 (−3.37%) | 66.036 (+5.96%) |
| Index RSS (MiB) | 169.531 | 167.230 (−1.36%) | 171.773 (+1.32%) |
| Scroll PSS (MiB) | 61.375 | 61.217 (−0.26%) | 61.615 (+0.39%) |
| Scroll RSS (MiB) | 165.832 | 165.414 (−0.25%) | 166.148 (+0.19%) |

Chọn batch 16: batch 32 chỉ giảm thêm 1,99% tổng thời gian nhưng metadata đầu trễ thêm 344 ms và frame P95 xấu hơn. Chưa thêm throttle catalog.

Reports: `library-batch{4,16,32}-step7-20261119-142719-Measure`; APK `F7489923…` / `4468F946…` / `CCF27EDB…`; instrumentation `90009B4E…`. Base `461b484` + catalog changes.

## 2026-11-19 — Cache bìa và thumbnail WebP

Hai scenario pass, mỗi case năm vòng. So với baseline 2026-11-18; chênh lệch gồm cả thay đổi xen giữa, chưa tách riêng tác động WebP. Scan và bộ nhớ dùng median; frame dùng percentile.

| Scenario | Chỉ số | Baseline | Hiện tại | Thay đổi |
| --- | --- | --- | --- | --- |
| `indexLibrary` | Scan (s) | 29.693 | 25.541 | −13.98% |
| `libraryGridScroll` | CPU frame P50 (ms) | 30.163 | 15.975 | −47.04% |
| `libraryGridScroll` | CPU frame P95 (ms) | 76.004 | 28.266 | −62.81% |
| `libraryGridScroll` | CPU frame P99 (ms) | 89.551 | 32.776 | −63.40% |
| `indexLibrary` | PSS (MiB) | 73.749 | 66.737 | −9.51% |
| `indexLibrary` | RSS (MiB) | 166.336 | 174.656 | +5.00% |
| `libraryGridScroll` | PSS (MiB) | 70.942 | 61.345 | −13.53% |
| `libraryGridScroll` | RSS (MiB) | 163.492 | 165.496 | +1.23% |

Report: `cover-step6-Measure`; APK `FCC6BC00…`, code gốc `8f173cd` + thay đổi bìa. Tham chiếu: `baseline-step1-user-20261118-201053-Measure` (scan), `scroll-grid-selection-fix-20261118-Measure` (cuộn). Bộ nhớ lấy mẫu sau mỗi journey.

## 2026-11-19 — Tối ưu làm mới dữ liệu đọc

Median foreground refresh (s), năm vòng mỗi scenario; cả hai bản pass 2/2. Cùng instrumentation và setup chờ ổn định sáu giây/prime qua HOME.

| Scenario | Trước | Sau | Thay đổi |
| --- | --- | --- | --- |
| `homeReturnUnchanged` | 6.039 | 2.398 | −60.30% |
| `homeReturnChanged` | 6.668 | 6.761 | +1.41% |

Nguồn không đổi: reading skip 5/5; nguồn đổi: đọc đầy đủ 5/5.

Report: `reading-control-step5-20261119-112754-Measure` / `reading-step5-20261119-112754-Measure`. APK `A290C83F…` → `9481CE10…`; instrumentation `90009B4E…`. Code gốc `1d8bc47`, candidate thêm reading skip.

## 2026-11-19 — Tối ưu import thống kê

Median foreground refresh (s), năm vòng mỗi scenario; pass 2/2.

| Scenario | Trước | Sau | Thay đổi |
| --- | --- | --- | --- |
| `homeReturnUnchanged` | 9.375 | 5.494 | −41.40% |
| `homeReturnChanged` | 9.323 | 6.066 | −34.93% |

Report: `home-step4-trace-Measure`; tham chiếu: `step3-20261119-001645-Measure`. APK `8FC9FD14…`, code gốc `a116650` + thay đổi local. Các lượt đo riêng; AVD đã khởi động lại trước lượt mới nhất.

## 2026-11-19 — SAF direct lookup

**Operations** — tổng tác vụ và kiểm tra dữ liệu sẵn sàng, dùng giây. Một vòng mỗi bản; cả hai đạt 3/3. Tham chiếu: master `e8453e5`, cùng instrumentation cho hai bản.

| Operation | Master | SAF direct lookup |
| --- | --- | --- |
| `libraryRefresh` | 150.55 | 7.61 |
| `statisticsRefresh` | 7.40 | 7.36 |
| `clearCacheAndRefresh` | 169.91 | 29.56 |

**Scenarios** — bộ benchmark ban đầu. Năm vòng mỗi case đã đo; giá trị đơn là median. Tham chiếu: 2026-11-18. `—` là chưa có số đo mới.

| Scenario | Chỉ số | Tham chiếu | SAF direct lookup |
| --- | --- | --- | --- |
| `homeReturnUnchanged` | Foreground refresh (s) | 140.17 | 9.38 |
| `homeReturnChanged` | Foreground refresh (s) | 140.39 | 9.32 |
| `coldStartup` | Initial display (ms) | 712.29 | — |
| `warmStartup` | Initial display (ms) | 424.77 | — |
| `indexLibrary` | Scan span (s) | 29.69 | — |
| `mainTabNavigation` | CPU frame P50 / P95 / P99 (ms) | 50.75 / 85.61 / 94.95 | — |
| `libraryGridScroll` | CPU frame P50 / P95 / P99 (ms) | 30.16 / 76.00 / 89.55 | — |

Report: `refresh-master-20261119-005402-Measure`, `refresh-step3-20261119-005402-Measure`, `step3-20261119-001645-Measure`. Nguồn tham chiếu: `baseline-step1-user-20261118-201053-Measure`, `scroll-grid-selection-fix-20261118-Measure`.

Đối chứng master bổ sung, `homeReturnChanged` (2 vòng): 140.98 / 147.05 s; median 144.01 s (`master-control-20261119-003029-Measure`).

JSON, trace và memory checkpoint gốc nằm tại `benchmark/build/reports/<run>/` được Git bỏ qua trong checkout tương ứng.

## 2026-11-18 — tham chiếu emulator

- Code gốc: `81fb756`, kèm phần bổ sung benchmark/tracing; `benchmark` tối ưu, `CompilationMode.None`.
- AVD: `Tango_2_Virtual`, Android 14/API 34, x86_64, 862×1648, density 320, font scale 1.0.
- Fixture: 1.000 sách tại `books/a/b/c`, 200 history, 50.000 events; năm vòng mỗi case.
- SHA-256 APK target: `39F4E81F2492AF050E4726DEF957FF9E306FD81E0E7F96707C6EA4C21B2FF6A7`.

Giá trị dùng ms; startup đo hiển thị đầu. Refresh/scan dùng median tổng span tác vụ.

| Kịch bản | Chỉ số | Giá trị |
| --- | --- | --- |
| `coldStartup` | Hiển thị đầu, median | 712.29 |
| `warmStartup` | Hiển thị đầu, median | 424.77 |
| `homeReturnUnchanged` | Foreground refresh, median | 140174.27 |
| `homeReturnChanged` | Foreground refresh, median | 140393.37 |
| `indexLibrary` | Index, median | 29693.26 |
| `mainTabNavigation` | CPU frame P50 / P95 / P99 | 50.75 / 85.61 / 94.95 |
| `libraryGridScroll` | CPU frame P50 / P95 / P99 | 30.16 / 76.00 / 89.55 |

Giữ harness, fixture và điều kiện host nhất quán khi so sánh; timing emulator chịu ảnh hưởng tải CPU/GPU của host. Report gốc nằm tại `benchmark/build/reports/<run>/` được Git bỏ qua.

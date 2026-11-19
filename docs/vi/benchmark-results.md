# Kết quả benchmark

[English](../en/benchmark-results.md) · [Quy trình](benchmarking.md)

AVD: `Tango_2_Virtual`, Android 14/API 34, x86_64, 862×1648, density 320, font scale 1.0. Fixture: 1.000 sách, 200 history và 50.000 events. Target `benchmark` tối ưu, `CompilationMode.None`.

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
| `libraryScanAndEnrich` | Scan span (s) | 29.69 | — |
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
| `libraryScanAndEnrich` | Scan/enrich, median | 29693.26 |
| `mainTabNavigation` | CPU frame P50 / P95 / P99 | 50.75 / 85.61 / 94.95 |
| `libraryGridScroll` | CPU frame P50 / P95 / P99 | 30.16 / 76.00 / 89.55 |

Giữ harness, fixture và điều kiện host nhất quán khi so sánh; timing emulator chịu ảnh hưởng tải CPU/GPU của host. Report gốc nằm tại `benchmark/build/reports/<run>/` được Git bỏ qua.

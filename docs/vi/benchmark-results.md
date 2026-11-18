# Kết quả benchmark

[English](../en/benchmark-results.md) · [Quy trình](benchmarking.md)

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

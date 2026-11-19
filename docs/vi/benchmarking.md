# Đo thời gian khởi động và bộ nhớ

[English](../en/benchmarking.md) · [Mục lục](README.md) · [Kết quả](benchmark-results.md)

## Chạy công cụ

Dùng [Java 21 và Gradle wrapper](development.md), thiết bị Android 11+ hoặc emulator đã cấp quyền ADB và label riêng cho mỗi lượt. Giữ cấu hình hiển thị và tải nền nhất quán giữa các lần so sánh.

```powershell
./scripts/benchmark.ps1 -Mode Smoke -Device SERIAL -Label smoke
./scripts/benchmark.ps1 -Mode Measure -Device SERIAL -Label candidate
./scripts/benchmark.ps1 -Mode Profile -Device SERIAL -Label profiles
./scripts/benchmark.ps1 -Mode VerifyProfile -Device SERIAL -Label installation
```

- `-AllowEmulator`: chạy trên emulator.
- `-SkipBuild`: dùng lại APK hiện có.
- `-TestFilter`: chọn class hoặc method AndroidJUnitRunner, phân cách bằng dấu phẩy.

Chạy một case:

```powershell
./scripts/benchmark.ps1 -Mode Measure -Device emulator-5554 -AllowEmulator -SkipBuild -Label scroll -TestFilter 'dev.twotechy.inknest.perf.StartupBenchmark#libraryGridScroll'
```

Script cài APK, chuẩn bị fixture và xuất kết quả vào `benchmark/build/reports/<label>-<timestamp>-<mode>/` được Git bỏ qua: log instrumentation, metadata, JSON benchmark, trace và memory checkpoint. Khi test lỗi, script thử xuất kết quả một phần với `runStatus: failed`; xem log trước khi chạy lại case lỗi.

Trên emulator dùng InkNest làm HOME mặc định, runner tạm chọn Pixel Launcher và khôi phục HOME ban đầu khi kết thúc. Giữ emulator mở và tránh thao tác trong lúc đo.

## Bộ dữ liệu và cách ly

Setup tạo 1.000 EPUB giả tại `Download/InkNestBenchmark/books/a/b/c`, 200 history kèm sidecar và database SQLite tương thích KOReader với 50.000 events, mốc 2026-11-01. Khi phiên bản fixture đổi, setup dựng lại `Download/InkNestBenchmark`.

Picker cấp quyền truy cập fixture này. Phép đo dùng SAF, parser, Room, thumbnail và UI thật. Target `benchmark` tối ưu dùng application ID `dev.twotechy.inknest.benchmark`; `profile` tắt shrinking để tạo profile. Tùy chọn và quyền của các target này tách khỏi production.

Điểm setup và quyền tệp rộng chỉ có trong biến thể benchmark/profile; điểm điều khiển cần quyền DUMP của shell. Không phân phối các APK này.

## So sánh và diễn giải

`StartupBenchmark` dùng `CompilationMode.None`, năm vòng mỗi case. Chuẩn bị và kiểm tra fixture diễn ra trước mỗi test.

Để đo tổng thời gian refresh/rebuild, chọn `RefreshBenchmark#libraryRefresh`, `RefreshBenchmark#statisticsRefresh` hoặc `RefreshBenchmark#clearCacheAndRefresh` với tiền tố package đầy đủ `dev.twotechy.inknest.perf.`. Mỗi case chạy một vòng, gồm kiểm tra dữ liệu sẵn sàng; refresh statistics chờ một lần làm mới mới hoàn tất thành công, có thể dùng lại bản chụp không đổi.

| Case | Thao tác / chỉ số |
| --- | --- |
| Cold / warm startup | `StartupMode.COLD` / `WARM`; thời gian hiển thị đầu. |
| Các tab chính | Home → Library → Apps → Stats → Home; phân vị CPU frame. |
| HOME return nguồn không đổi | Chờ tệp fixture ổn định sáu giây, prime reading ngoài vùng đo, rồi quay về từ Settings hệ thống bằng HOME intent tường minh; span foreground refresh. |
| HOME return nguồn thay đổi | Sửa history/sidecar hiện tại và thêm event statistics 77 giây trước khi quay lại; span foreground refresh. |
| Lập chỉ mục Thư viện (`indexLibrary`) | Xóa cache target khi Thư viện hiển thị; span index và CPU frame. |
| Cuộn lưới Thư viện | Sáu cử chỉ xuống và sáu lên trong lưới; yêu cầu tên sách hiển thị thay đổi; CPU frame. |

Harness kiểm tra refresh hoàn tất, tiến độ đã import, 200 mục tracked, tổng statistics và catalog đầy đủ. `TraceSectionMetric` báo tổng span `InkNest.foregroundRefresh` / `InkNest.library.index` cùng số lần chạy. Mỗi vòng hoàn tất lưu một checkpoint `dumpsys meminfo`; báo PSS/RSS lấy mẫu riêng biệt.

Mỗi scenario cũng lưu trace Perfetto. Trong foreground refresh, `InkNest.reading.refresh` và `InkNest.statistics.refresh` chạy đồng thời; không cộng thời lượng hai nhánh. Các span chi tiết statistics gồm `InkNest.statistics.fingerprint`, `InkNest.statistics.snapshot.copy`, `InkNest.statistics.import.rows`, `InkNest.statistics.summarize` và `InkNest.statistics.publish`. Span rows gồm đọc event SQLite và ghi lô vào Room. `InkNest.statistics.skip` đánh dấu dùng lại bản import không đổi. Khi lập chỉ mục Library không rỗng thành công, `InkNest.library.firstMetadata` kết thúc sau khi commit lô metadata đầu; `InkNest.library.metadataBatch` bao mỗi thao tác đọc metadata/ghi lô. Các span này nằm trong span index nên không cộng vào tổng thời gian index. Tên trace không chứa đường dẫn nguồn hoặc document ID.

Reading dùng `InkNest.reading.read` cho lần đọc nguồn đầy đủ, `InkNest.reading.fingerprint` để kiểm tra metadata và `InkNest.reading.skip` khi dùng lại thành công. Đo bản đối chứng với cùng thời gian ổn định/prime này.

So sánh cùng fixture, harness, chế độ biên dịch, cấu hình hiển thị và điều kiện thiết bị/host. Lưu bản tóm tắt có ngày trong [kết quả](benchmark-results.md), kèm định danh build và lượt nguồn; giữ artifact gốc ngoài Git. File memory dùng vùng lưu chung trên thiết bị nên cần chọn checkpoint đúng case và lượt đo.

## Profile ứng dụng

`Profile` thu quy tắc startup/navigation trên target chưa minify, cần hai vòng ổn định trong ba lần thử. Công cụ ghi quy tắc InkNest được sinh, loại fixture, vào `app/src/main/generated/baselineProfiles/baseline-prof.txt` và `startup-prof.txt`.

Tạo lại sau thay đổi lớn startup/navigation, kiểm tra diff được sinh và build lại release. Không sửa tay quy tắc profile.

`VerifyProfile` kiểm tra cài profile đóng gói và khởi động trên `benchmark` tối ưu bằng `CompilationMode.Partial(BaselineProfileMode.Require)`, không làm nóng JIT. Nếu xác minh lỗi, xem log instrumentation và kết quả broadcast ProfileInstaller trước khi tạo lại hoặc chạy lại.

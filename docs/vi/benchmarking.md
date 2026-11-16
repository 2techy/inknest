# Đo thời gian khởi động và bộ nhớ

[English](../en/benchmarking.md) · [Mục lục](README.md)

Chạy smoke test cách ly, so sánh khởi động/bộ nhớ và tạo profile. Production ở `:app`; `:benchmark` là module Android test tự chạy instrumentation.

## Chạy công cụ

Dùng [Java 21 và Gradle wrapper](development.md), thiết bị Android 11+ đã cấp quyền ADB. Trên Windows:

```powershell
./scripts/benchmark.ps1 -Mode Smoke -Device SERIAL -Label smoke
./scripts/benchmark.ps1 -Mode Measure -Device SERIAL -Label candidate
./scripts/benchmark.ps1 -Mode Profile -Device SERIAL -Label profiles
./scripts/benchmark.ps1 -Mode VerifyProfile -Device SERIAL -Label installation
```

- `-SkipBuild` dùng lại APK. `-AllowEmulator` cho phép chạy chức năng trên giả lập; không bỏ lỗi khác hay chứng minh hiệu năng thật.
- So sánh trên thiết bị thật ít tác vụ nền. Giữ cỡ chữ, độ phân giải, app cài, firmware, nhiệt độ/điều kiện sạc; đóng trình đọc khác, không chạy Gradle/benchmark đồng thời. AndroidX cần pin ít nhất 25%; ghi pin/nhiệt và xem cảnh báo nhiệt.
- Script cài APK, tạo dữ liệu, thu trace, JSON, meminfo tại checkpoint và metadata thiết bị/commit vào `benchmark/build/reports/<label>-<mode>` được Git bỏ qua.
- Không đổi tùy chọn/quyền production. Trên giả lập có InkNest production làm HOME, script tạm chọn Pixel Launcher và khôi phục trong `finally`; launcher khác giữ nguyên.

## Bộ dữ liệu và cách ly

Target `benchmark` dùng tối ưu R8 release, ký debug, application ID `dev.twotechy.inknest.benchmark` và cho shell profiling. `profile` tắt shrinking để quy tắc dễ đọc. Cả hai không bật chẩn đoán debug; [ký release](release.md) tách riêng.

Setup tạo 100 EPUB giả đánh số, chứa PNG 240×336 và DB SQLite tương thích KOReader có 50.000 sự kiện, mốc 2026-11-01. Marker ghi phiên bản fixture; chỉ ghi `Download/InkNestBenchmark`. Không dùng sách/URI riêng hoặc đổi Room schema production.

Picker thật cấp quyền cho target cách ly, cấu hình DataStore riêng. SAF, phân tích EPUB/Lua, nhập Room, thumbnail và UI thật chạy không thay repository giả. Setup hỗ trợ picker AOSP và Tango chọn gốc trước rồi Select/Confirm, kiểm tra đúng cây fixture. Activity setup/quyền tệp rộng chỉ ở source set benchmark/profile; Activity cần quyền DUMP của shell. Không phân phối các biến thể này.

## So sánh và diễn giải

`StartupBenchmark` chạy năm cold start, năm warm start với `CompilationMode.None`, và năm lượt Trang chủ → Thư viện → Ứng dụng → Thống kê → Trang chủ. Chuẩn bị dữ liệu thực hiện lượt đầu đó trước mỗi test; chế độ này không thêm vòng làm nóng biên dịch.

| Chỉ số | Diễn giải |
| --- | --- |
| Hiển thị đầu tiên | So trung vị; chưa phải nội dung sẵn sàng đầy đủ. |
| CPU frame điều hướng | So phân vị lấy mẫu; xem trace khi chậm trên 10% có thể lặp lại. |
| Bộ nhớ | `dumpsys meminfo` sau Trang chủ sẵn sàng mỗi lần khởi động và sau mỗi lượt điều hướng: 15 checkpoint. Báo PSS/RSS riêng và cực đại lấy mẫu, không phải đỉnh liên tục/lịch sử OEM. |

So với checkout cách ly của commit baseline đã ghi, chỉ phủ harness/build; giữ code kiến trúc, fixture, package, chế độ biên dịch, thiết bị và năm lần lặp. Lưu metadata/báo cáo gốc cục bộ ngoài Git. Mẫu ít và GPU/lịch chạy host giả lập giới hạn kết luận; đo lại Tango để khẳng định RAM thiết bị. Đổi kiến trúc/độ tin cậy tự nó không chứng minh tăng tốc/giảm RAM.

## Profile ứng dụng

`BaselineProfileGenerator` thu riêng startup/navigation trên `profile` chưa minify, cần hai vòng ổn định trong ba lần thử. Chỉ giữ lớp InkNest, loại lớp fixture. Chế độ Profile ghi quy tắc sinh có quản lý phiên bản tại `app/src/main/generated/baselineProfiles/`: `baseline-prof.txt` gộp và `startup-prof.txt` riêng; không sửa tay.

AGP dùng chúng cho bố cục startup R8/đóng gói ART release; ProfileInstaller cài baseline trên Android hỗ trợ. Tạo lại khi đổi lớn startup/navigation, rà diff và build release lại. A/B chế độ None không đo lợi ích biên dịch baseline.

`ProfileInstallationTest.packagedProfileCompilesAndStarts` xác minh riêng cài/biên dịch profile đóng gói trên `benchmark` tối ưu, dùng `CompilationMode.Partial(BaselineProfileMode.Require)`, không làm nóng JIT. Một mẫu startup không phải kết quả A/B.

Firmware trả broadcast 0 thay vì chuyển install/skip ProfileInstaller làm xác minh thất bại. Giữ log và ghi giới hạn; không bỏ assertion hay kết luận biên dịch từ phép đo không profile. Có asset và đạt trên máy khác không chứng minh cài được trên firmware đó.

Mở sách release đã ký, về HOME và phiên đọc dài cần [nghiệm thu thật](reliability.md); dữ liệu giả không chứng nhận các luồng này.

# Đo thời gian khởi động và bộ nhớ

[English](../en/benchmarking.md) · [Mục lục](README.md)

`:benchmark` là module Android test tự instrumentation; production vẫn ở `:app`. Biến thể `benchmark` kế thừa R8 release, dùng khóa debug với application ID `dev.twotechy.inknest.benchmark`, manifest cho phép shell profiling. Biến thể `profile` tắt shrinking để tạo rule đọc được; cả hai không dùng debug diagnostics. [Ký release](release.md) là quy trình riêng.

## Chạy công cụ

Dùng Java 21, thiết bị ADB Android 11 trở lên đã cấp quyền và Gradle wrapper của repo. Xem [công cụ phát triển](development.md). Trên Windows:

```powershell
./scripts/benchmark.ps1 -Mode Smoke -Device SERIAL -Label smoke
./scripts/benchmark.ps1 -Mode Measure -Device SERIAL -Label candidate
./scripts/benchmark.ps1 -Mode Profile -Device SERIAL -Label profiles
./scripts/benchmark.ps1 -Mode VerifyProfile -Device SERIAL -Label installation
```

`-SkipBuild` dùng APK sẵn có. `-AllowEmulator` cho phép chạy chức năng trên giả lập, chỉ bỏ cảnh báo giả lập chứ không bỏ lỗi benchmark. Kết luận hiệu năng nên dùng thiết bị thật ít tác vụ nền. Dừng trình đọc khác; giữ cỡ chữ, độ phân giải, app đã cài, firmware và nhiệt độ nhất quán. Không chạy Gradle hoặc benchmark khác trong lúc đo.

AndroidX yêu cầu pin ít nhất 25%. Có thể vừa sạc vừa đo nhưng giữ cùng điều kiện sạc cho hai bản, ghi pin/nhiệt độ và xem cảnh báo thermal.

Script cài APK đích/test, chuẩn bị dataset, chạy instrumentation và thu trace, JSON, meminfo tại checkpoint cùng metadata thiết bị/commit vào `benchmark/build/reports/<label>-<mode>` được bỏ qua bởi Git. Không đổi tùy chọn/quyền nguồn production. Nếu launcher mặc định của giả lập là InkNest production, script tạm chọn Pixel Launcher để tránh công việc foreground cạnh tranh và khôi phục HOME trong `finally`. Launcher khác không bị đổi.

## Dataset và cách ly

Activity chỉ dành cho hiệu năng tạo 100 EPUB tổng hợp đánh số, mỗi tệp có PNG 240×336, cùng database tương thích KOReader có 50.000 event. Ngày cố định tương đối với 2026-11-01; marker fixture ghi phiên bản. Chỉ ghi vào `Download/InkNestBenchmark`. Không chứa sách riêng, URI Tango hay thay đổi schema Room production.

Thiết lập cấp quyền cho app đích cách ly qua trình chọn thư mục thật và cấu hình DataStore riêng. Luồng SAF, parser EPUB/Lua, import Room, tải ảnh và UI production được chạy, không thay repository. Driver hỗ trợ picker AOSP và DocumentsUI Tango chọn gốc trước với luồng Select/Confirm riêng; Activity xác minh đúng cây fixture trước khi lưu.

Activity thiết lập và quyền tệp rộng chỉ ở source set benchmark/profile; Activity yêu cầu quyền DUMP của shell. Không phân phối các biến thể này cho người dùng.

## So sánh và diễn giải

`StartupBenchmark` chạy năm lần cold start, năm warm start với `CompilationMode.None`, cùng năm vòng Home → Library → Apps → Stats → Home. Chuẩn bị dataset đi cùng hành trình trước mỗi test; Macrobenchmark có 0 vòng làm nóng biên dịch bổ sung ở chế độ này. So sánh median thời gian hiển thị đầu và percentile CPU-frame điều hướng lấy mẫu; xem trace nếu chậm hơn lặp lại được trên 10%. Hiển thị đầu không phải lúc toàn bộ nội dung sẵn sàng. Thay đổi kiến trúc/độ tin cậy không tự chứng minh nhanh hơn hoặc giảm RAM; cần đo cùng điều kiện.

Bộ nhớ dùng `dumpsys meminfo` sau checkpoint Home sẵn nội dung ở mỗi startup và sau từng vòng tab. Báo PSS/RSS riêng, gồm sampled maximum trong 15 checkpoint. Không coi đây là peak liên tục hoặc maximum lịch sử của OEM. GPU/lập lịch host giả lập và ít mẫu giới hạn kết luận; đo lại trên Tango trước khi tuyên bố RAM thiết bị.

Để có baseline kiểm soát, dùng checkout cách ly của commit baseline, chỉ phủ harness/cấu hình build này. Giữ code kiến trúc commit đó, fixture, package, compilation mode, thiết bị và năm lần lặp giống nhau. Lưu metadata và báo cáo gốc cục bộ, không commit artifact đo.

## Profile ứng dụng

`BaselineProfileGenerator` thu rule startup và điều hướng riêng trên `profile` chưa minify, cần hai vòng ổn định trong ba lần thử. Chỉ giữ class InkNest, loại fixture. Chế độ Profile gộp hai bộ vào `app/src/main/generated/baselineProfiles/baseline-prof.txt`, ghi startup riêng trong `startup-prof.txt`. Các rule sinh này được quản lý phiên bản; trace/báo cáo đo thì không.

AGP dùng thư mục đó để sắp mã startup R8 release và đóng gói ART profile. ProfileInstaller cài baseline đóng gói trên Android hỗ trợ. Tạo lại profile sau thay đổi startup/điều hướng lớn, rà diff rồi build release. A/B không profile với compilation mode None không đo lợi ích ART baseline compilation.

`ProfileInstallationTest.packagedProfileCompilesAndStarts` là smoke test riêng dùng `CompilationMode.Partial(BaselineProfileMode.Require)`, không làm nóng JIT. Chạy trên benchmark tối ưu để kiểm tra cài/biên dịch profile; một mẫu startup không phải phép đo A/B. Nếu firmware trả broadcast result 0 thay vì chuyển install/skip action cho ProfileInstaller, test báo lỗi rõ ràng. Ghi nhận giới hạn thiết bị, giữ log; không bỏ assertion hay suy biên dịch profile từ phép đo không profile. Asset APK và test thành công trên thiết bị hỗ trợ là bằng chứng riêng với việc cài trên firmware đó.

Mở sách/quay HOME bằng release đã ký và quay lại sau phiên đọc dài cần [nghiệm thu riêng](reliability.md); benchmark tổng hợp không xác nhận các luồng này.

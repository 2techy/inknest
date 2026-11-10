# Đo thời gian khởi động và bộ nhớ

[English](../en/benchmarking.md) · [Mục lục](README.md)

`:benchmark` là module kiểm thử Android tự chạy instrumentation. Ứng dụng chính vẫn nằm trong `:app`. Biến thể `benchmark` kế thừa tối ưu R8 của release, ký bằng khóa debug và có application ID `dev.twotechy.inknest.benchmark`; manifest cho phép shell thu thập thông tin hiệu năng. Biến thể `profile` tắt rút gọn mã để tạo quy tắc profile dễ đọc. Cả hai không bật chẩn đoán debug. [Ký bản release](release.md) là quy trình riêng.

## Chạy công cụ

Dùng Java 21, thiết bị Android 11 trở lên đã cấp quyền ADB và Gradle wrapper của dự án. Xem [công cụ phát triển](development.md). Trên Windows:

```powershell
./scripts/benchmark.ps1 -Mode Smoke -Device SERIAL -Label smoke
./scripts/benchmark.ps1 -Mode Measure -Device SERIAL -Label candidate
./scripts/benchmark.ps1 -Mode Profile -Device SERIAL -Label profiles
./scripts/benchmark.ps1 -Mode VerifyProfile -Device SERIAL -Label installation
```

`-SkipBuild` dùng lại APK đã có. `-AllowEmulator` cho phép kiểm tra chức năng trên giả lập; cờ này chỉ bỏ cảnh báo giả lập, không bỏ qua lỗi benchmark khác. Để kết luận về hiệu năng, nên đo trên thiết bị thật có ít tác vụ nền. Đóng các trình đọc không liên quan; giữ nguyên cỡ chữ, độ phân giải, ứng dụng đã cài, firmware và điều kiện nhiệt độ. Không chạy Gradle hoặc benchmark khác trong lúc đo.

AndroidX yêu cầu pin còn ít nhất 25%. Có thể đo khi sạc, nhưng phải giữ cùng điều kiện sạc giữa hai bản được so sánh, ghi mức pin/nhiệt độ và xem các cảnh báo nhiệt.

Script cài APK ứng dụng và APK kiểm thử, chuẩn bị bộ dữ liệu, chạy instrumentation rồi thu trace, JSON, meminfo tại các mốc đo và thông tin thiết bị/commit vào `benchmark/build/reports/<label>-<mode>`, thư mục được Git bỏ qua. Tùy chọn và quyền nguồn của ứng dụng chính không bị đổi. Nếu InkNest bản thông thường đang là trình khởi chạy mặc định của giả lập, script tạm chọn Pixel Launcher để tránh hai ứng dụng cùng thực hiện tác vụ khi hoạt động, rồi khôi phục vai trò HOME trong `finally`. Các trình khởi chạy khác không bị thay đổi.

## Bộ dữ liệu và cách ly môi trường

Activity thiết lập chỉ dành cho đo hiệu năng tạo 100 EPUB mẫu được tạo tự động và đánh số, mỗi tệp chứa một PNG 240×336, cùng cơ sở dữ liệu SQLite tương thích KOReader có 50.000 sự kiện. Ngày của sự kiện được cố định theo mốc 2026-11-01; tệp đánh dấu bộ dữ liệu kiểm thử ghi lại phiên bản. Chỉ ghi vào `Download/InkNestBenchmark`. Không dùng sách riêng, URI của Tango hoặc thay đổi schema Room của ứng dụng chính.

Quá trình thiết lập cấp quyền cho ứng dụng đo riêng qua trình chọn thư mục thật của hệ thống, rồi cấu hình DataStore riêng. Luồng SAF, phân tích EPUB/Lua, nhập Room, tải ảnh thu nhỏ và giao diện của ứng dụng chính đều được chạy, không thay repository bằng bản giả. Bộ điều khiển thiết lập hỗ trợ trình chọn AOSP và DocumentsUI của Tango, nơi phải chọn thư mục gốc trước rồi thực hiện hai bước Select/Confirm riêng. Activity kiểm tra đúng cây thư mục dữ liệu thử nghiệm trước khi lưu.

Activity thiết lập và quyền truy cập tệp diện rộng chỉ nằm trong source set benchmark/profile; Activity yêu cầu quyền DUMP của shell. Không phân phối các biến thể này cho người dùng.

## So sánh và diễn giải kết quả

`StartupBenchmark` chạy năm lần khởi động nguội (cold start), năm lần khởi động ấm (warm start) với `CompilationMode.None`, cùng năm lượt Trang chủ → Thư viện → Ứng dụng → Thống kê → Trang chủ. Bước chuẩn bị dữ liệu thực hiện cùng hành trình ban đầu trước mỗi bài kiểm thử; ở chế độ này, Macrobenchmark không chạy thêm vòng làm nóng biên dịch.

So sánh trung vị thời gian hiển thị đầu tiên và các phân vị thời gian CPU xử lý khung hình được lấy mẫu khi điều hướng. Nếu thời gian chậm hơn trên 10% và lặp lại được, hãy xem trace. Hiển thị đầu tiên chưa có nghĩa là toàn bộ nội dung đã sẵn sàng. Thay đổi kiến trúc hoặc cải thiện độ tin cậy không tự chứng minh ứng dụng nhanh hơn hay dùng ít RAM hơn; các kết luận đó cần phép đo trên thiết bị trong điều kiện tương đương.

Bộ nhớ được lấy mẫu bằng `dumpsys meminfo` sau khi nội dung Trang chủ sẵn sàng ở mỗi lần khởi động và sau mỗi lượt chuyển qua các tab. Báo PSS và RSS riêng, gồm giá trị lớn nhất lấy được trong 15 mốc đo. Đây không phải đỉnh bộ nhớ được theo dõi liên tục hay mức tối đa trong lịch sử do OEM ghi nhận. GPU của giả lập, việc lập lịch trên máy chạy giả lập và số mẫu ít đều giới hạn kết luận; cần đo lại trên Tango trước khi khẳng định mức RAM trên thiết bị.

Để có bản đối chứng được kiểm soát, dùng bản checkout riêng của commit đối chứng đã ghi nhận, chỉ áp thêm bộ công cụ đo và cấu hình biên dịch này. Giữ nguyên mã kiến trúc của commit đó, đồng thời dùng cùng bộ dữ liệu, package, chế độ biên dịch, thiết bị và năm lần lặp. Lưu thông tin phép đo và báo cáo gốc cục bộ, không đưa kết quả đo vào Git.

## Profile ứng dụng

`BaselineProfileGenerator` thu riêng quy tắc khởi động và điều hướng trên biến thể `profile` chưa rút gọn mã, yêu cầu có hai vòng ổn định trong ba lần thử. Chỉ giữ các lớp InkNest, loại các lớp tạo dữ liệu thử nghiệm. Chế độ Profile gộp hai bộ quy tắc vào `app/src/main/generated/baselineProfiles/baseline-prof.txt`, đồng thời ghi quy tắc khởi động riêng vào `startup-prof.txt`. Các tệp quy tắc được sinh này được quản lý phiên bản; trace và báo cáo đo thì không.

AGP dùng thư mục đó để R8 sắp xếp mã khởi động của release và đóng gói ART profile. ProfileInstaller cài baseline profile đã đóng gói trên các phiên bản Android được hỗ trợ. Sau thay đổi lớn về khởi động/điều hướng, hãy tạo lại profile, rà soát diff rồi biên dịch lại release. Phép đo A/B không dùng profile ở trên chạy với chế độ biên dịch None, nên không đo lợi ích của việc biên dịch ART theo baseline profile.

`ProfileInstallationTest.packagedProfileCompilesAndStarts` là bài kiểm tra nhanh riêng, dùng `CompilationMode.Partial(BaselineProfileMode.Require)` và không có vòng làm nóng JIT. Chạy trên biến thể benchmark đã tối ưu để xác minh profile đóng gói được cài và biên dịch. Một mẫu khởi động của bài này không phải kết quả so sánh hiệu năng A/B.

Nếu firmware trả kết quả broadcast 0 thay vì chuyển các action install/skip cho ProfileInstaller, bài kiểm tra sẽ báo lỗi rõ ràng. Ghi lại giới hạn thiết bị và giữ log lỗi; không bỏ assertion hoặc dùng phép đo không profile để khẳng định profile đã được biên dịch. Việc có profile trong APK và kiểm tra thành công trên thiết bị được hỗ trợ không chứng minh profile đã cài được trên firmware gặp lỗi đó.

Mở sách bằng APK release đã ký trên thiết bị thật, quay về HOME và quay lại sau phiên đọc dài cần [nghiệm thu riêng](reliability.md). Benchmark dùng dữ liệu mẫu tự tạo không xác nhận các luồng này.

# Build và ký APK

[English](../en/release.md) · [Mục lục](README.md)

Dùng Gradle wrapper trong repo và Java 21 theo [phát triển](development.md). Phiên bản dependency từ version catalog. Build cục bộ mặc định dùng `versionName = "0.1.0-dev"` và `versionCode = 1`; workflow release ghi đè qua Gradle properties `inknestVersionName` và `inknestVersionCode`. Giới thiệu dùng `BuildConfig.VERSION_NAME` của bản build.

## Build cục bộ

```powershell
.\gradlew.bat spotlessCheck :app:assembleDebug :app:assembleRelease :app:lintDebug
```

APK debug ở `app/build/outputs/apk/debug/app-debug.apk`. Release tối ưu ở `app/build/outputs/apk/release/app-release-unsigned.apk`, cần ký trước khi cài. Dự án open source vẫn ký APK phát hành; khóa ký là riêng tư, không phải giấy phép bản quyền dự án.

## Ký bản phát hành

Trong Android Studio chọn Build → Generate Signed App Bundle or APK → APK, chọn `app`, tạo/chọn keystore riêng và biến thể release. Giữ khóa ngoài repo và sao lưu an toàn; bản cập nhật phải dùng cùng chứng chỉ. Keystore, signing properties cục bộ, output build và thư mục `app/release/` của Studio được Git bỏ qua. Không dán mật khẩu vào lệnh, commit tệp ký hoặc dùng khóa debug để phân phối.

Kiểm tra APK bằng SDK `apksigner verify --verbose --print-certs <apk>`, ghi SHA-256 bằng `Get-FileHash <apk> -Algorithm SHA256`. Kiểm tra package/phiên bản/quyền trước phân phối. Giữ mapping riêng cùng artifact tương ứng.

Cùng package nhưng khóa khác không thể thay bản đã cài. Ưu tiên thiết bị/profile thử nghiệm riêng; gỡ app xóa cấu hình và ghim. Dự án không tự gỡ app hoặc thay danh tính ký. Workflow draft release bên dưới tự tạo tag phiên bản; maintainer vẫn quyết định xuất bản công khai.

## Draft release trên GitHub

[Workflow Draft release](../../.github/workflows/release.yml) được chạy thủ công từ Actions. Merge các thay đổi cần phát hành khi CI đạt, rồi chọn **Actions → Draft release → Run workflow**, chọn `master` và bắt đầu chạy. Nhánh khác và tag bị bỏ qua. Không cần nhập phiên bản, tạo PR đổi phiên bản hoặc tạo tag thủ công.

Run checkout đúng commit trên `master` được chọn khi bắt đầu, kể cả khi có commit mới trong lúc build. Workflow sinh CalVer `YYYY.M.PATCH` theo thời điểm tạo run ban đầu ở UTC, ví dụ `2026.11.0`. PATCH bắt đầu từ 0 nếu tháng đó chưa có tag khớp; nếu có, lấy PATCH lớn nhất trong các tag của tháng rồi cộng một. Tag của draft cũng được tính; bỏ qua tag phát triển cũ và tag có hậu tố. Workflow truyền phiên bản này cùng `versionCode = 1000000 + github.run_number` vào Gradle, rồi đối chiếu cả hai trong APK đã build và kiểm tra package `dev.twotechy.inknest`. Publish theo thứ tự run number để version code của bản cập nhật luôn tăng. Giữ nguyên chuỗi run number và giá trị cộng thêm của workflow release; nếu đổi phải đối chiếu version code đã phân phối. Code vượt giới hạn Android 2100000000 bị từ chối.

Phiên bản, code, commit và run ID được lưu trong artifact nhỏ `release-version` trong một ngày. Chạy lại toàn bộ job dùng lại các giá trị này, kể cả khi đã sang tháng UTC mới hoặc xuất hiện tag mới. Nếu metadata hết hạn hoặc chưa được upload, tạo run mới; workflow sẽ tính phiên bản theo các tag đang có. Run lỗi trước khi tạo tag không giữ chỗ PATCH. Tag phiên bản đã tồn tại phải trỏ đến cùng commit; release đã tồn tại phải là draft có target commit tương ứng. Phiên bản đã publish hoặc xung đột làm run dừng.

Build, ký và quản lý draft là ba job riêng. Job build kiểm tra định dạng, JVM tests/lint debug/release rồi build release chưa ký, không nhận signing secrets. Job ký căn chỉnh/ký/xác minh APK qua environment `release` của repo và không checkout code app. Chỉ job draft có quyền ghi release/dọn artifact; job này cũng không checkout code app.

Cấu hình environment `release` trong repository settings với deployment **branch** rule cho `master`. Thay tag rule `v*` cũ nếu có: run thủ công dùng ref của nhánh khi vào job ký. Dùng khóa release hiện có, sao lưu ngoài GitHub. Đặt environment secrets qua GitHub settings hoặc quy trình quản lý secret cục bộ đã xác thực; không dán giá trị vào issue, PR, chat hoặc lệnh chứa mật khẩu trực tiếp:

| Secret | Giá trị |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Keystore release hiện có mã hóa Base64 |
| `ANDROID_KEYSTORE_PASSWORD` | Mật khẩu keystore |
| `ANDROID_KEY_ALIAS` | Alias khóa ký hiện có |
| `ANDROID_KEY_PASSWORD` | Mật khẩu khóa ký |

Đặt environment variable `ANDROID_SIGNING_CERT_SHA256` bằng SHA-256 chứng chỉ từ APK đã ký đáng tin cậy trước đó (`apksigner verify --print-certs`); chấp nhận dấu hai chấm và khoảng trắng. Thiếu secret, fingerprint sai, chứng chỉ không khớp hoặc tag/metadata APK sai làm workflow thất bại. Keystore chỉ giải mã vào thư mục tạm và bị xóa khi bước ký kết thúc; Gradle không nhận signing secrets.

Sau khi build và ký thành công, job draft tạo tag `v<version>` tại commit đã checkout và tạo **draft**, đính kèm `InkNest-<version>.apk`, `SHA256SUMS.txt`, ghi commit/version code/fingerprint cùng release notes tự sinh. Các bản CalVer này không được đánh dấu pre-release. Tải đúng APK đó, xác minh checksum/chữ ký và hoàn tất [nghiệm thu thiết bị](reliability.md) trước khi chọn **Publish release**. Tag được tạo bằng `GITHUB_TOKEN` của workflow; không cần personal access token. Push tag không kích hoạt workflow này.

Các run release dùng chung một concurrency group và không hủy run đang chạy. Rerun chỉ dùng lại tag và cập nhật draft khi commit tương ứng khớp; không ghi đè release đã publish. Để thử lại run chưa hoàn tất, chọn **Re-run failed jobs** hoặc **Re-run all jobs** trên run đó để giữ nguyên commit ban đầu. Run mới tính phiên bản mới theo tag đang có. Artifact chưa ký/đã ký trung chuyển hết hạn sau một ngày, được xóa sau upload thành công; metadata phiên bản cũng hết hạn sau một ngày. Nếu artifact cần dùng đã hết hạn, tạo run mới. APK release nằm trong GitHub Releases, không lưu trong Git hoặc Actions artifact dài hạn. Nếu tạo draft hoặc upload lỗi sau tạo tag, tag vẫn tồn tại còn draft có thể thiếu hoặc chưa được tạo; sửa lỗi, rerun run ban đầu khi artifact còn khả dụng và xác minh đủ hai asset trước khi publish.

## Chính sách dữ liệu và điều kiện phát hành

Dữ liệu Room đọc có thể tạo lại khi phát triển: đổi schema sẽ tạo lại cache. Ghim, tùy chọn và lựa chọn nguồn ở DataStore. Phải quyết định tương thích nâng cấp trước bản stable; chưa có hệ thống migration trong bản dev.

Backup và chuyển thiết bị loại dữ liệu InkNest; quyền SAF/cache chỉ thuộc thiết bị. Không đồng bộ mạng hoặc telemetry. Cả hai build type không có developer tools/source probe. Release tối ưu không được chứa chẩn đoán StrictMode. Thông báo bên thứ ba giữ trong Giới thiệu. InkNest dùng [MIT](../../LICENSE); thành phần bên thứ ba giữ giấy phép riêng.

Trước bản public, kiểm tra artifact đã ký trên Tango 2 thật: quyền, nguồn đọc, vai trò Home, mở app/sách trực tiếp, Thư viện, Thống kê và phục hồi. Kiểm tra luồng trên S25 theo [nghiệm thu](reliability.md). Kiểm tra tương phản/bóng mờ trên panel E-Ink; ảnh giả lập không xác nhận được. Release note ghi phiên bản thiết bị/firmware và giới hạn đã biết.

## Giới hạn tương thích

Thư viện dùng intent tài liệu trực tiếp, không có cổng debug/phiên bản. Tango 2 với KOReader v2026.07.1 cần URI FileProvider thuộc trình đọc cho sách bộ nhớ chính: URI SAF thông thường có thể thành alias `/mnt/user/...` không truy cập được và để sách cũ mở. Xem [cache đọc](reading-cache.md) về adapter URI tệp gốc và xử lý lỗi. Kiểm tra lại release mới ký trên Tango; cập nhật tại chỗ phải dùng khóa release hiện có.

Chưa hỗ trợ provider SD/đám mây, bộ máy đọc riêng, ghi ngược dữ liệu, làm mới màn hình OEM hoặc đồng bộ nền định kỳ.

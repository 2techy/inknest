# Build và phát hành

[English](../en/release.md) · [Mục lục](README.md)

[Workflow Draft release](../../.github/workflows/release.yml) build, ký APK, tạo tag phiên bản và chuẩn bị bản nháp trên GitHub Releases. Người duy trì kiểm tra APK và release notes trước khi xuất bản.

## Cấu hình ký

Tạo environment tên `release` trong repository. Tại **Deployment branches and tags**, cho phép nhánh `master`. Lưu thông tin ký dưới dạng **environment secrets**:

| Secret | Giá trị |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Keystore phát hành được mã hóa Base64 |
| `ANDROID_KEYSTORE_PASSWORD` | Mật khẩu keystore |
| `ANDROID_KEY_ALIAS` | Alias của khóa ký |
| `ANDROID_KEY_PASSWORD` | Mật khẩu khóa ký |

Thêm `ANDROID_SIGNING_CERT_SHA256` tại **Environment variables**. Dùng fingerprint SHA-256 của chứng chỉ ký, lấy bằng `keytool -list -v` với keystore hoặc `apksigner verify --print-certs` với APK đã ký đáng tin cậy. Chấp nhận dấu hai chấm và khoảng trắng. Fingerprint chứng chỉ và checksum APK nhận diện hai đối tượng khác nhau.

Sao lưu keystore an toàn ngoài repository. Các bản cập nhật phải dùng cùng chứng chỉ ký. Nhập mật khẩu qua công cụ quản lý secret hoặc lời nhắc mật khẩu; không đưa vào lệnh, issue, log hay hệ thống quản lý phiên bản.

## Phát hành APK

1. Merge các thay đổi vào `master` và xác nhận CI thành công.
2. Mở **Actions → Draft release → Run workflow**, chọn `master` và bắt đầu chạy.
3. Chờ các job build, ký và tạo bản nháp hoàn tất.
4. Mở bản nháp trong GitHub Releases. Rà tiêu đề, danh sách thay đổi tự sinh và mô tả các tính năng, lỗi đã sửa cùng giới hạn liên quan đến người dùng.
5. Tải `InkNest-<version>.apk` và `SHA256SUMS.txt`. Kiểm tra checksum, chữ ký rồi thực hiện [nghiệm thu thiết bị](reliability.md) trên APK đã ký.
6. Chọn **Publish release**.

Mỗi bản nháp có APK cài đặt được, tệp checksum và release notes kèm hướng dẫn cài đặt cùng liên kết thiết lập. Các bản CalVer không được đánh dấu pre-release.

Workflow chỉ chạy từ `master` và build commit được chọn khi bắt đầu run. Commit merge trong lúc đang chạy thuộc run tiếp theo. Các run release dùng chung concurrency group và để run đang thực hiện hoàn tất.

## Đánh phiên bản

Phiên bản phát hành có dạng **`YYYY.M.PATCH`**, ví dụ `2026.11.0`.

| Trường | Quy tắc |
| --- | --- |
| Năm và tháng | Thời điểm tạo workflow run theo UTC |
| PATCH | 0 nếu tháng chưa có tag khớp; nếu có, lấy PATCH lớn nhất cộng một |
| Tag | `v<version>`, ví dụ `v2026.11.0` |
| Android version code | `1000000 + github.run_number` |

Chỉ tag có dạng `vYYYY.M.PATCH` được tính khi xác định PATCH, kể cả tag gắn với bản nháp. Run lỗi trước khi tạo tag không giữ chỗ số PATCH.

Xuất bản theo thứ tự run number để Android version code luôn tăng giữa các bản phân phối. Giữ nguyên chuỗi run number và giá trị cộng thêm của workflow; nếu đổi cần đối chiếu với version code đã phân phối. Code vượt `2100000000` bị từ chối.

Build cục bộ mặc định dùng `versionName = "0.1.0-dev"` và `versionCode = 1`. Build phát hành truyền Gradle properties `inknestVersionName` và `inknestVersionCode`. APK phải có đúng các giá trị này và package `dev.twotechy.inknest`. Giới thiệu hiển thị `BuildConfig.VERSION_NAME`. Xem [phát triển](development.md) để truyền phiên bản khi build cục bộ.

## Chạy lại khi có lỗi

Chọn **Re-run failed jobs** để tiếp tục từ bước lỗi, hoặc **Re-run all jobs** để build lại cùng commit. Run chạy lại giữ nguyên phiên bản và code nhờ artifact `release-version`, lưu phiên bản, code, commit và run ID trong một ngày.

| Lỗi | Cách xử lý |
| --- | --- |
| Thiếu thông tin ký | Kiểm tra secrets và variable trong environment `release`. |
| Chứng chỉ không khớp | Kiểm tra keystore, alias và fingerprint mong đợi. |
| Artifact thiếu hoặc hết hạn | Tạo workflow run mới. |
| Xung đột tag | Chỉ dùng lại tag nếu cùng commit. |
| Xung đột release | Release đã tồn tại phải là bản nháp trỏ đến cùng commit. Không ghi đè bản đã xuất bản. |
| Lỗi tạo bản nháp hoặc tải asset lên | Kiểm tra bản nháp và chạy lại run ban đầu khi artifact còn khả dụng. Xác nhận đủ hai asset trước khi xuất bản. |

Run mới tính phiên bản từ các tag đang có. Run chạy lại giữ tiêu đề và notes đã biên tập. Tag đã tạo vẫn tồn tại nếu bước tiếp theo gặp lỗi.

Artifact APK chưa ký và đã ký dùng để chuyển giữa các job hết hạn sau một ngày, được xóa sau khi tải asset lên thành công. Metadata phiên bản hết hạn sau một ngày. APK đã phát hành được lưu trong GitHub Releases.

## Build và ký cục bộ

Dùng Java 21 và Gradle wrapper theo [phát triển](development.md):

```powershell
.\gradlew.bat spotlessCheck :app:assembleDebug :app:assembleRelease :app:lintDebug :app:lintRelease
```

| Bản build | APK |
| --- | --- |
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Release tối ưu chưa ký | `app/build/outputs/apk/release/app-release-unsigned.apk` |

Trong Android Studio, chọn **Build → Generate Signed App Bundle or APK → APK**, chọn `app`, keystore phát hành và biến thể release. Kiểm tra APK bằng `apksigner verify --verbose --print-certs`, tính checksum bằng `Get-FileHash <apk> -Algorithm SHA256`. Giữ riêng tệp mapping tương ứng với từng bản build.

APK ký bằng chứng chỉ khác không thể cập nhật bản đã cài. Dùng thiết bị hoặc profile thử nghiệm riêng khi kiểm tra khóa ký khác; gỡ ứng dụng sẽ xóa cấu hình cục bộ và ghim.

## Cách ly workflow

Job build kiểm tra định dạng, JVM tests và lint debug/release trước khi tạo APK release chưa ký. Job này không nhận signing secrets. Job ký căn chỉnh và ký APK, xác minh chứng chỉ rồi tính checksum. Keystore chỉ tồn tại trong thư mục tạm và bị xóa khi bước ký kết thúc.

Job ký và tạo bản nháp không checkout mã ứng dụng. Chỉ job tạo bản nháp có quyền ghi release và dọn artifact. Tag được tạo bằng `GITHUB_TOKEN`. Push tag không kích hoạt workflow.

## Nghiệm thu và tương thích

Kiểm tra quyền truy cập, thiết lập nguồn đọc, vai trò Home, mở ứng dụng, mở sách, Thư viện, Thống kê và phục hồi trên APK đã ký. Kiểm tra độ tương phản và bóng mờ E-Ink trên màn hình thật; xem [độ tin cậy](reliability.md) để thực hiện các bước theo thiết bị. Ghi rõ thiết bị, firmware và phiên bản KOReader khi mô tả tương thích hoặc lỗi đã biết.

Kết nối dữ liệu đọc hỗ trợ KOReader và bộ nhớ trong chính. Xem [cache đọc và mở sách](reading-cache.md) về xử lý URI và phục hồi. Provider SD/đám mây, bộ máy đọc riêng, ghi ngược dữ liệu, điều khiển làm mới màn hình OEM và đồng bộ nền định kỳ nằm ngoài phạm vi hỗ trợ.

Cơ sở dữ liệu đọc là cache có thể tạo lại. Đổi schema có thể tạo lại cache; ghim, tùy chọn và lựa chọn nguồn được lưu riêng trong DataStore. Sao lưu và chuyển thiết bị không bao gồm dữ liệu InkNest; quyền SAF thuộc từng thiết bị.

Chẩn đoán runtime chỉ hoạt động trong bản debug. Thông báo giấy phép bên thứ ba nằm trong Giới thiệu. InkNest dùng [giấy phép MIT](../../LICENSE); thành phần đi kèm giữ giấy phép riêng.

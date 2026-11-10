# Biên dịch và phát hành

[English](../en/release.md) · [Mục lục](README.md)

[Workflow Draft release](../../.github/workflows/release.yml) biên dịch, ký APK, tạo tag phiên bản và chuẩn bị bản nháp trên GitHub Releases. Người duy trì kiểm tra APK và ghi chú phát hành trước khi xuất bản.

## Cấu hình ký

Tạo môi trường (environment) tên `release` trong kho mã. Tại **Deployment branches and tags**, cho phép nhánh `master`. Lưu thông tin bí mật dùng để ký vào **environment secrets**:

| Secret | Giá trị |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Keystore phát hành được mã hóa Base64 |
| `ANDROID_KEYSTORE_PASSWORD` | Mật khẩu keystore |
| `ANDROID_KEY_ALIAS` | Tên định danh (alias) của khóa ký |
| `ANDROID_KEY_PASSWORD` | Mật khẩu khóa ký |

Thêm `ANDROID_SIGNING_CERT_SHA256` tại **Environment variables**. Dùng dấu vân tay SHA-256 (fingerprint) của chứng chỉ ký, lấy bằng `keytool -list -v` với keystore hoặc `apksigner verify --print-certs` với APK đã ký đáng tin cậy. Chấp nhận dấu hai chấm và khoảng trắng. Dấu vân tay chứng chỉ nhận diện chứng chỉ ký; mã kiểm tra toàn vẹn (checksum) APK nhận diện nội dung tệp APK.

Sao lưu keystore ở nơi an toàn ngoài kho mã. Các bản cập nhật phải dùng cùng chứng chỉ ký. Nhập mật khẩu qua công cụ quản lý thông tin bí mật hoặc hộp nhắc nhập mật khẩu; không đưa vào lệnh, issue, log hay hệ thống quản lý phiên bản.

## Phát hành APK

1. Hợp nhất các thay đổi vào `master` và xác nhận CI thành công.
2. Mở **Actions → Draft release → Run workflow**, chọn `master` và bắt đầu chạy.
3. Chờ các job biên dịch, ký và tạo bản nháp hoàn tất.
4. Mở bản nháp trong GitHub Releases. Rà tiêu đề, danh sách thay đổi tự sinh và mô tả các tính năng, lỗi đã sửa cùng giới hạn liên quan đến người dùng.
5. Tải `InkNest-<version>.apk` và `SHA256SUMS.txt`. Kiểm tra checksum, chữ ký rồi thực hiện [nghiệm thu thiết bị](reliability.md) trên APK đã ký.
6. Chọn **Publish release**.

Mỗi bản nháp có APK cài đặt được, tệp checksum và ghi chú phát hành kèm hướng dẫn cài đặt cùng liên kết thiết lập. Các bản dùng phiên bản theo lịch (CalVer) không được đánh dấu là bản tiền phát hành (pre-release).

Workflow chỉ chạy từ `master` và biên dịch commit được chọn khi bắt đầu lần chạy. Commit được hợp nhất trong lúc đó sẽ thuộc lần chạy sau. Các lần chạy phát hành dùng chung nhóm kiểm soát chạy đồng thời; lần đang chạy được phép hoàn tất.

## Đánh phiên bản

Phiên bản phát hành có dạng **`YYYY.M.PATCH`**, ví dụ `2026.11.0`.

| Trường | Quy tắc |
| --- | --- |
| Năm và tháng | Thời điểm tạo lần chạy workflow, tính theo UTC |
| PATCH | 0 nếu tháng chưa có tag khớp; nếu có, lấy PATCH lớn nhất cộng một |
| Tag | `v<version>`, ví dụ `v2026.11.0` |
| Mã phiên bản Android | `1000000 + github.run_number` |

Chỉ tag có dạng `vYYYY.M.PATCH` được tính khi xác định PATCH, kể cả tag gắn với bản nháp. Lần chạy lỗi trước khi tạo tag không giữ chỗ số PATCH.

Xuất bản theo thứ tự số lần chạy (run number) để mã phiên bản Android luôn tăng giữa các bản phân phối. Giữ nguyên chuỗi số lần chạy và giá trị cộng thêm của workflow; nếu thay đổi, phải đối chiếu với mã phiên bản đã phân phối. Mã lớn hơn `2100000000` sẽ bị từ chối.

Bản dựng cục bộ mặc định dùng `versionName = "0.1.0-dev"` và `versionCode = 1`. Khi biên dịch bản phát hành, workflow truyền các thuộc tính Gradle `inknestVersionName` và `inknestVersionCode`. APK phải có đúng các giá trị này và package `dev.twotechy.inknest`. Giới thiệu hiển thị `BuildConfig.VERSION_NAME`. Xem [phát triển](development.md) để truyền phiên bản khi biên dịch cục bộ.

## Chạy lại khi có lỗi

Chọn **Re-run failed jobs** để tiếp tục từ bước lỗi, hoặc **Re-run all jobs** để biên dịch lại cùng commit. Lần chạy lại giữ nguyên tên và mã phiên bản nhờ tệp kết quả (artifact) `release-version`, lưu tên phiên bản, mã phiên bản, commit và ID lần chạy trong một ngày.

| Lỗi | Cách xử lý |
| --- | --- |
| Thiếu thông tin ký | Kiểm tra thông tin bí mật và biến trong môi trường `release`. |
| Chứng chỉ không khớp | Kiểm tra keystore, alias và fingerprint mong đợi. |
| Tệp kết quả thiếu hoặc hết hạn | Bắt đầu lần chạy workflow mới. |
| Xung đột tag | Chỉ dùng lại tag nếu cùng commit. |
| Xung đột bản phát hành | Bản phát hành đã tồn tại phải là bản nháp trỏ đến cùng commit. Không ghi đè bản đã xuất bản. |
| Lỗi tạo bản nháp hoặc tải tệp đính kèm lên | Kiểm tra bản nháp và chạy lại lần ban đầu khi các tệp kết quả vẫn còn. Xác nhận đủ hai tệp đính kèm trước khi xuất bản. |

Lần chạy mới tính phiên bản từ các tag đang có. Chạy lại lần cũ giữ nguyên tiêu đề và ghi chú đã biên tập. Tag đã tạo vẫn tồn tại nếu bước tiếp theo gặp lỗi.

Các APK chưa ký và đã ký dùng để chuyển giữa các job được giữ trong một ngày và bị xóa sau khi tải thành công lên bản phát hành. Thông tin phiên bản được giữ trong một ngày. APK đã phát hành được lưu trong GitHub Releases.

## Biên dịch và ký cục bộ

Dùng Java 21 và Gradle wrapper theo [phát triển](development.md):

```powershell
.\gradlew.bat spotlessCheck :app:assembleDebug :app:assembleRelease :app:lintDebug :app:lintRelease
```

| Bản dựng | APK |
| --- | --- |
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Release tối ưu chưa ký | `app/build/outputs/apk/release/app-release-unsigned.apk` |

Trong Android Studio, chọn **Build → Generate Signed App Bundle or APK → APK**, chọn `app`, keystore phát hành và biến thể release. Kiểm tra APK bằng `apksigner verify --verbose --print-certs`, tính checksum bằng `Get-FileHash <apk> -Algorithm SHA256`. Giữ riêng tệp mapping tương ứng với từng bản dựng.

APK ký bằng chứng chỉ khác không thể cập nhật bản đã cài. Khi thử khóa ký khác, dùng thiết bị hoặc hồ sơ người dùng riêng; gỡ ứng dụng sẽ xóa cấu hình cục bộ và ghim.

## Cách ly workflow

Job biên dịch kiểm tra định dạng, chạy kiểm thử JVM và lint cho debug/release trước khi tạo APK release chưa ký. Job này không nhận thông tin bí mật dùng để ký. Job ký căn chỉnh và ký APK, xác minh chứng chỉ rồi tính checksum. Keystore chỉ tồn tại trong thư mục tạm và bị xóa khi bước ký kết thúc.

Job ký và job tạo bản nháp không lấy mã nguồn ứng dụng về. Chỉ job tạo bản nháp có quyền ghi bản phát hành và dọn tệp kết quả. Tag được tạo bằng `GITHUB_TOKEN`. Push tag không kích hoạt workflow.

## Nghiệm thu và tương thích

Kiểm tra quyền truy cập, thiết lập nguồn đọc, vai trò ứng dụng Trang chủ mặc định, mở ứng dụng, mở sách, Thư viện, Thống kê và phục hồi trên APK đã ký. Kiểm tra độ tương phản và bóng mờ E-Ink trên màn hình thật; xem [độ tin cậy](reliability.md) để thực hiện các bước theo thiết bị. Ghi rõ thiết bị, firmware và phiên bản KOReader khi mô tả tương thích hoặc lỗi đã biết.

Kết nối dữ liệu đọc hỗ trợ KOReader và bộ nhớ trong chính. Xem [bộ nhớ đệm đọc và mở sách](reading-cache.md) về xử lý URI và phục hồi. Hiện chưa hỗ trợ nhà cung cấp tài liệu trên thẻ SD/đám mây, bộ máy đọc sách tích hợp, ghi dữ liệu ngược về nguồn, điều khiển làm mới màn hình của OEM hoặc đồng bộ nền định kỳ.

Cơ sở dữ liệu đọc là bộ nhớ đệm có thể tạo lại. Thay đổi schema có thể khiến bộ nhớ đệm được tạo lại; ghim, tùy chọn và lựa chọn nguồn được lưu riêng trong DataStore. Sao lưu và chuyển thiết bị không bao gồm dữ liệu InkNest; quyền SAF thuộc từng thiết bị.

Chẩn đoán khi chạy chỉ hoạt động trong bản debug. Thông báo giấy phép bên thứ ba nằm trong Giới thiệu. InkNest dùng [giấy phép MIT](../../LICENSE); thành phần đi kèm giữ giấy phép riêng.

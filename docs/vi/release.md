# Build và ký APK

[English](../en/release.md) · [Mục lục](README.md)

Dùng Gradle wrapper trong repo và Java 25 theo [phát triển](development.md). Phiên bản dependency từ version catalog. `versionName` và `versionCode` trong `app/build.gradle.kts` là nguồn phiên bản duy nhất; Giới thiệu dùng `BuildConfig.VERSION_NAME`.

## Build cục bộ

```powershell
.\gradlew.bat spotlessCheck :app:assembleDebug :app:assembleRelease :app:lintDebug
```

APK debug ở `app/build/outputs/apk/debug/app-debug.apk`. Release tối ưu ở `app/build/outputs/apk/release/app-release-unsigned.apk`, cần ký trước khi cài. Dự án open source vẫn ký APK phát hành; khóa ký là riêng tư, không phải giấy phép bản quyền dự án.

## Ký bản phát hành

Trong Android Studio chọn Build → Generate Signed App Bundle or APK → APK, chọn `app`, tạo/chọn keystore riêng và biến thể release. Giữ khóa ngoài repo và sao lưu an toàn; bản cập nhật phải dùng cùng chứng chỉ. Keystore, signing properties cục bộ, output build và thư mục `app/release/` của Studio được Git bỏ qua. Không dán mật khẩu vào lệnh, commit tệp ký hoặc dùng khóa debug để phân phối.

Kiểm tra APK bằng SDK `apksigner verify --verbose --print-certs <apk>`, ghi SHA-256 bằng `Get-FileHash <apk> -Algorithm SHA256`. Kiểm tra package/phiên bản/quyền trước phân phối. Giữ mapping riêng cùng artifact tương ứng.

Cùng package nhưng khóa khác không thể thay bản đã cài. Ưu tiên thiết bị/profile thử nghiệm riêng; gỡ app xóa cấu hình và ghim. Dự án không tự gỡ app, thay danh tính ký, tag, upload hay xuất bản build.

## Chính sách dữ liệu và điều kiện phát hành

Dữ liệu Room đọc có thể tạo lại khi phát triển: đổi schema sẽ tạo lại cache. Ghim, tùy chọn và lựa chọn nguồn ở DataStore. Phải quyết định tương thích nâng cấp trước bản stable; chưa có hệ thống migration trong bản dev.

Backup và chuyển thiết bị loại dữ liệu InkNest; quyền SAF/cache chỉ thuộc thiết bị. Không đồng bộ mạng hoặc telemetry. Cả hai build type không có developer tools/source probe. Release tối ưu không được chứa chẩn đoán StrictMode. Thông báo bên thứ ba giữ trong Giới thiệu. InkNest dùng [MIT](../../LICENSE); thành phần bên thứ ba giữ giấy phép riêng.

Trước bản public, kiểm tra artifact đã ký trên Tango 2 thật: quyền, nguồn đọc, vai trò Home, mở app/sách trực tiếp, Thư viện, Thống kê và phục hồi. Kiểm tra luồng trên S25 theo [nghiệm thu](reliability.md). Kiểm tra tương phản/bóng mờ trên panel E-Ink; ảnh giả lập không xác nhận được. Release note ghi phiên bản thiết bị/firmware và giới hạn đã biết.

## Giới hạn tương thích

Thư viện dùng intent tài liệu trực tiếp, không có cổng debug/phiên bản. Tango 2 với KOReader v2026.07.1 cần URI FileProvider thuộc trình đọc cho sách bộ nhớ chính: URI SAF thông thường có thể thành alias `/mnt/user/...` không truy cập được và để sách cũ mở. Xem [cache đọc](reading-cache.md) về adapter URI tệp gốc và xử lý lỗi. Kiểm tra lại release mới ký trên Tango; cập nhật tại chỗ phải dùng khóa release hiện có.

Chưa hỗ trợ provider SD/đám mây, bộ máy đọc riêng, ghi ngược dữ liệu, làm mới màn hình OEM hoặc đồng bộ nền định kỳ.

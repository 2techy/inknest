# Biên dịch và phát triển InkNest

[English](../en/development.md) · [Mục lục](README.md)

## Công cụ cần có

- Android Studio, Android SDK Platform 37 và SDK Platform-Tools.
- Java 21, theo [cấu hình Gradle daemon](../../gradle/gradle-daemon-jvm.properties). Dùng JDK tương thích; JBR đi kèm Studio chỉ phù hợp nếu đúng phiên bản. Ứng dụng và benchmark đều dùng Java 21 cho mức ngôn ngữ nguồn và bytecode đích. Kotlin tích hợp trong AGP kế thừa JVM target 21 từ tùy chọn biên dịch Android.
- Thiết bị hoặc giả lập Android 8.0 trở lên đã cấp quyền gỡ lỗi để chạy ứng dụng; kiểm thử đo hiệu năng bằng instrumentation cần Android 11 trở lên.

Mở kho mã bằng Android Studio và đồng bộ Gradle. Lưu đường dẫn SDK trong `local.properties`, không đưa tệp này vào Git. Gradle tự tải các thư viện phụ thuộc; không cần máy chủ backend, khóa API hoặc bước cài gói riêng. Lần biên dịch đầu tiên cần truy cập các kho Gradle/Maven đã cấu hình. Phiên bản công cụ và thư viện được khai báo trong [cấu hình wrapper](../../gradle/wrapper/gradle-wrapper.properties) và [danh mục phiên bản](../../gradle/libs.versions.toml).

Chạy các lệnh PowerShell tại thư mục gốc của kho mã. Nếu cần, đặt `JAVA_HOME` tới JDK 21. Trên macOS/Linux, thay `.\gradlew.bat` bằng `bash ./gradlew`; wrapper chưa được đánh dấu có quyền thực thi trong Git. Script benchmark dùng PowerShell và gọi wrapper Windows.

Đặt Gradle JDK của Android Studio thành JDK 21. Cấu hình lựa chọn JVM cho Gradle daemon trong kho mã yêu cầu Java 21, kể cả khi wrapper được khởi chạy bằng JDK tương thích khác. Kiểm tra JVM của daemon bằng `--version`. Bản thân Android Studio vẫn có thể dùng môi trường chạy đi kèm.

D8/R8 chuyển bytecode JVM thành DEX cho Android 8.0 trở lên (API 26). API thư viện Java khả dụng phụ thuộc phiên bản Android trên thiết bị; kiểm tra tương thích nền tảng khi dùng API.

## Biên dịch và cài đặt

Bản dựng cục bộ dùng tên phiên bản `0.1.0-dev` và mã phiên bản `1`. [Quy trình phát hành](release.md)
tự tạo phiên bản theo lịch (CalVer) cùng mã phiên bản tăng dần. Để dùng cùng thông tin phiên bản
khi biên dịch trên máy, truyền các thuộc tính Gradle trong dấu nháy, ví dụ
`.\gradlew.bat :app:assembleRelease '-PinknestVersionName=2026.11.0' '-PinknestVersionCode=1000001'`.
Khi tái tạo một bản phát hành, dùng đúng giá trị đã ghi trong lần chạy tương ứng.

Cập nhật thủ công với phiên bản mặc định `0.1.0-dev` dùng cùng luồng tải trong ứng dụng.
Tên phiên bản không nhận diện được không bị kết luận là cũ hơn, nhưng vẫn có thể tải và xác minh bản
phát hành mới nhất. Mã phiên bản APK và chữ ký vẫn phải hợp lệ trước khi cài; không có fallback riêng
sang trang web. Để thử trạng thái có bản mới trên debug, dùng CalVer cũ,
ví dụ `:app:assembleDebug '-PinknestVersionName=2026.10.0'`. Debug vẫn không tự kiểm tra; APK GitHub
không thể thay thế bản debug ký bằng khóa khác.

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.twotechy.inknest/.MainActivity
```

Có thể chạy `app` từ Android Studio hoặc dùng `.\gradlew.bat :app:installDebug`. Nếu có nhiều thiết bị ADB, thêm `-s SERIAL` sau `adb`; với tác vụ thiết bị của Gradle, chọn đích bằng `ANDROID_SERIAL`. APK ký bằng chứng chỉ khác không thể cập nhật bản đã cài. Gỡ ứng dụng sẽ xóa cài đặt và ứng dụng đã ghim, nên cân nhắc dùng thiết bị hoặc hồ sơ người dùng riêng để kiểm thử.

Xem [thiết lập](setup.md) để kết nối nguồn đọc thật. Các bản debug/release thông thường không có chế độ dữ liệu mẫu. [Ký bản phát hành](release.md) là quy trình riêng, tách khỏi việc tạo APK debug.

## Kiểm tra

| Mục đích | Lệnh |
| --- | --- |
| Kiểm tra / áp dụng định dạng | `.\gradlew.bat spotlessCheck` / `.\gradlew.bat spotlessApply` |
| Biên dịch Kotlin | `.\gradlew.bat :app:compileDebugKotlin` |
| Kiểm thử JVM | `.\gradlew.bat :app:testDebugUnitTest` |
| Chạy riêng một nhóm kiểm thử JVM | `.\gradlew.bat :app:testDebugUnitTest --tests '*StatsViewModelTest'` |
| Lint Android | `.\gradlew.bat :app:lintDebug :app:lintRelease` |
| Tạo APK debug và release tối ưu chưa ký | `.\gradlew.bat :app:assembleDebug :app:assembleRelease` |
| Biên dịch bộ kiểm thử Android không cần thiết bị | `.\gradlew.bat :app:assembleDebugAndroidTest` |
| Chạy kiểm thử Android trên thiết bị kết nối | `.\gradlew.bat :app:connectedDebugAndroidTest` |
| APK đo hiệu năng | `.\gradlew.bat :app:assembleBenchmark :benchmark:assembleBenchmark` |

Khi thay đổi mã, chạy kiểm tra định dạng, các bài kiểm thử liên quan, biên dịch những biến thể debug/release bị ảnh hưởng và chạy lint. Không sửa Kotlin trong lúc lint đang chạy. Đọc các bài kiểm thử dùng API Android thật trước khi chạy trên thiết bị cá nhân: bài kiểm thử SAF chỉ bật khi chủ động chọn có thể thu hồi quyền đã cấu hình. Xem [kiểm tra độ tin cậy](reliability.md) cho phục hồi và vòng đời. Không có task `:benchmark:lintBenchmark`.

Với thay đổi chỉ gồm tài liệu, kiểm tra liên kết, lệnh và diff; không cần chạy bộ kiểm thử trên nhiều thiết bị. Báo rõ các kiểm tra thực sự đã chạy và giới hạn còn lại. Kiểm tra bố cục trên giả lập không xác nhận được hành vi trên phần cứng E-Ink. Xem [đo hiệu năng](benchmarking.md) về dữ liệu mẫu cách ly và phép đo.

Các bài kiểm thử JVM cho repository tạo Preferences DataStore bằng `OkioStorage` và bộ tuần tự hóa Preferences thật trên đĩa, để thao tác ghi nhiều lần hoạt động trên các hệ điều hành dùng để phát triển. Kiểm thử tích hợp Android kiểm tra phần lưu trữ Android được dùng trong ứng dụng chính.

## GitHub Actions

[Workflow CI](../../.github/workflows/ci.yml) chạy với pull request vào `master` (kể cả bản nháp), push vào `master` và khi chạy thủ công. Các lần chạy tự động dùng `paths-ignore` để bỏ qua thay đổi chỉ gồm `docs/**` và các tệp Markdown ở gốc kho mã. Các thay đổi khác và lần chạy thủ công thực hiện đầy đủ bước `build`: kiểm tra định dạng, chạy kiểm thử JVM và Android lint cho debug/release, biên dịch APK debug/release cùng APK kiểm thử Android. CI không cần thông tin bí mật dùng để ký.

CI dùng runner Ubuntu tiêu chuẩn, Temurin 21 và SDK package `platforms;android-37.0`. Chỉ push vào `master` được ghi Gradle cache; PR và workflow thủ công chỉ đọc. Báo cáo lỗi được lưu trong một ngày. Khi chạy CI thủ công có thể bật `upload_debug_apk` để giữ APK debug một ngày; PR thông thường không tải APK lên.

Chạy [workflow Android tests](../../.github/workflows/android-tests.yml) thủ công tại Actions → Android tests → Run workflow. Workflow dùng một giả lập Android 14/API 34 x86_64 và bộ kiểm thử tích hợp mặc định. Các bài dùng nguồn thật hoặc thu hồi quyền chỉ chạy khi chủ động bật, nên được bỏ qua ở đây. Báo cáo/logcat khi có lỗi được lưu trong một ngày. Kết quả giả lập không xác nhận khả năng hoạt động trên Android 8 hoặc E-Ink thật; đo hiệu năng và tạo profile vẫn theo [quy trình trên thiết bị](benchmarking.md).

Tự động hóa release được mô tả trong [ký bản phát hành](release.md). Các workflow cố định phiên bản action và mặc định dùng token chỉ đọc. Kho mã mặc định giữ log và tệp kết quả (artifact) trong bảy ngày; artifact được tải lên trong các workflow này được giữ một ngày.

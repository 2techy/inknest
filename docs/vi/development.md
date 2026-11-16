# Biên dịch và phát triển InkNest

[English](../en/development.md) · [Mục lục](README.md)

Chuẩn bị công cụ, biên dịch/cài ứng dụng và chọn kiểm tra phù hợp cho thay đổi.

## Công cụ cần có

- Android Studio, Android SDK Platform 37 và Platform-Tools; lưu vị trí SDK trong `local.properties` không theo dõi bởi Git.
- Java 21 cho source/bytecode app và benchmark. Đặt Gradle JDK của Studio là 21; JBR đi kèm chỉ phù hợp nếu đúng phiên bản. [Cấu hình daemon](../../gradle/gradle-daemon-jvm.properties) chọn Java 21; kiểm tra bằng `--version`. Studio vẫn có thể dùng runtime đi kèm.
- Thiết bị/giả lập Android 8.0+ đã cấp quyền; benchmark instrumentation cần Android 11+.

Mở dự án và sync Gradle. Lần đầu tự tải phụ thuộc; không cần backend/khóa API hay bước cài riêng. Phiên bản ở [wrapper](../../gradle/wrapper/gradle-wrapper.properties) và [catalog](../../gradle/libs.versions.toml). API thư viện Java vẫn phụ thuộc phiên bản Android.

Chạy PowerShell tại gốc repo. Đặt `JAVA_HOME` tới JDK 21 nếu cần. Trên macOS/Linux thay `.\gradlew.bat` bằng `bash ./gradlew` (Git chưa có executable bit). Script benchmark dùng PowerShell và wrapper Windows.

## Biên dịch và cài đặt

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.twotechy.inknest.debug/dev.twotechy.inknest.MainActivity
```

Có thể chạy `app` trong Studio hoặc `.\gradlew.bat :app:installDebug`. Nhiều thiết bị thì dùng `adb -s SERIAL` hoặc đặt `ANDROID_SERIAL` cho task thiết bị Gradle.

| Biến thể | Application ID | APK |
| --- | --- | --- |
| Debug | `dev.twotechy.inknest.debug` | `app/build/outputs/apk/debug/app-debug.apk` |
| Release tối ưu chưa ký | `dev.twotechy.inknest` | `app/build/outputs/apk/release/app-release-unsigned.apk` |

Debug/release thường dùng [nguồn thật](setup.md). Debug và release có application ID riêng, cài song song với cài đặt và quyền SAF độc lập. Cập nhật bản đã cài cần chữ ký tương thích; dùng thiết bị/profile thử riêng khi đổi khóa ký. Gỡ ứng dụng xóa cài đặt/ghim. Xem [ký phát hành](release.md).

Phiên bản cục bộ mặc định `0.1.0-dev` / mã `1`. Để tái tạo metadata release, truyền đúng giá trị đã ghi bằng thuộc tính có nháy, ví dụ:

```powershell
.\gradlew.bat :app:assembleRelease '-PinknestVersionName=2026.11.0' '-PinknestVersionCode=1000001'
```

Cập nhật thủ công vẫn dùng được khi tên phiên bản không nhận diện, không kết luận bản tìm thấy mới hơn; vẫn kiểm tra mã/chữ ký APK. Thử UI có cập nhật bằng CalVer cũ, như `.\gradlew.bat :app:assembleDebug '-PinknestVersionName=2026.10.0'`. Debug tắt kiểm tra tự động. Trình cập nhật debug từ chối APK production do khác package. Thử cài đặt toàn luồng bằng bản release có cùng khóa ký.

## Kiểm tra

| Mục đích | Lệnh |
| --- | --- |
| Kiểm tra / áp dụng định dạng | `.\gradlew.bat spotlessCheck` / `.\gradlew.bat spotlessApply` |
| Biên dịch Kotlin | `.\gradlew.bat :app:compileDebugKotlin` |
| Kiểm thử JVM | `.\gradlew.bat :app:testDebugUnitTest` |
| Kiểm thử JVM riêng | `.\gradlew.bat :app:testDebugUnitTest --tests '*StatsViewModelTest'` |
| Lint Android | `.\gradlew.bat :app:lintDebug :app:lintRelease` |
| APK debug/release | `.\gradlew.bat :app:assembleDebug :app:assembleRelease` |
| Biên dịch kiểm thử Android | `.\gradlew.bat :app:assembleDebugAndroidTest` |
| Kiểm thử Android kết nối | `.\gradlew.bat :app:connectedDebugAndroidTest` |
| APK đo hiệu năng | `.\gradlew.bat :app:assembleBenchmark :benchmark:assembleBenchmark` |

Đổi code thì chạy định dạng, kiểm thử liên quan, build debug/release bị ảnh hưởng và lint. Không sửa Kotlin khi lint chạy. Đọc native test trước khi chạy thiết bị cá nhân; [SAF test opt-in](reliability.md) có thể thu hồi quyền đã cấu hình. Không có task `:benchmark:lintBenchmark`.

Docs-only cần kiểm tra link, lệnh và diff. Báo kiểm tra thực chạy cùng giới hạn; bố cục giả lập không chứng nhận E-Ink thật. [Đo hiệu năng](benchmarking.md) giữ quy trình fixture/phép đo.

JVM repository test dùng Preferences `OkioStorage` trên đĩa với serializer thật; Android integration test kiểm tra lưu trữ Android production. Coroutine test dùng chung scheduler cho dispatcher và đồng hồ giả được inject.

## GitHub Actions

- [CI](../../.github/workflows/ci.yml): PR vào `master` (kể cả nháp), push vào `master` và chạy thủ công. Tự động bỏ qua thay đổi chỉ gồm `docs/**` và Markdown gốc. Bước `build` đầy đủ kiểm tra định dạng, JVM test/lint debug/release và biên dịch APK/Android test, không cần secret ký.
- CI dùng Ubuntu, Temurin 21 và `platforms;android-37.0`. Chỉ push `master` ghi Gradle cache. Báo cáo lỗi giữ một ngày; `upload_debug_apk` thủ công giữ APK debug một ngày, PR thường không tải APK lên.
- [Android tests](../../.github/workflows/android-tests.yml): chạy thủ công Actions → Android tests → Run workflow, dùng Android 14/API 34 x86_64. Test nguồn thật/thu hồi quyền vẫn opt-in và được bỏ qua; báo cáo lỗi/logcat giữ một ngày. Không chứng nhận Android 8 hoặc E-Ink thật.
- [Release](release.md) và [benchmark/profile](benchmarking.md) có quy trình riêng. Action được pin, token mặc định chỉ đọc. Retention repo mặc định bảy ngày; artifact workflow tải lên giữ một ngày.

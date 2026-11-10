# Build và phát triển InkNest

[English](../en/development.md) · [Mục lục](README.md)

## Công cụ cần có

- Android Studio, Android SDK Platform 37 và SDK Platform-Tools.
- Java 21, theo [cấu hình Gradle daemon](../../gradle/gradle-daemon-jvm.properties). Dùng JDK tương thích; JBR đi kèm Studio chỉ phù hợp nếu đúng phiên bản. App và benchmark đều dùng source/bytecode target Java 21. Kotlin tích hợp trong AGP kế thừa JVM target 21 từ compile options Android.
- Thiết bị hoặc giả lập Android 8.0 trở lên đã cho phép debug để chạy app; instrumentation đo hiệu năng cần Android 11 trở lên.

Mở repo bằng Android Studio và sync Gradle. Đường dẫn SDK nằm trong `local.properties` không được đưa vào Git. Gradle tự tải dependency; không cần backend, API key hoặc bước cài package riêng. Build đầu tiên cần truy cập kho Gradle/Maven đã cấu hình. Phiên bản nằm trong [cấu hình wrapper](../../gradle/wrapper/gradle-wrapper.properties) và [version catalog](../../gradle/libs.versions.toml).

Chạy lệnh PowerShell từ gốc repo. Nếu cần, đặt `JAVA_HOME` tới JDK 21. Trên macOS/Linux, thay `.\gradlew.bat` bằng `bash ./gradlew`; wrapper chưa có executable bit trong Git. Script benchmark dùng PowerShell và gọi wrapper Windows.

Đặt Gradle JDK của Android Studio thành JDK 21. Tiêu chí JVM daemon trong repo chọn Java 21 để build, kể cả khi wrapper khởi chạy bằng JDK tương thích khác. Kiểm tra daemon JVM bằng `--version`. Bản thân Android Studio vẫn có thể dùng runtime đi kèm.

Java/JVM target 21 không đổi Android tối thiểu: app vẫn yêu cầu API 26. Android chạy DEX do D8/R8 tạo ra; API thư viện Java khả dụng vẫn phụ thuộc Android SDK. Kiểm tra tương thích trước khi dùng API mới hơn.

## Build và cài đặt

Build cục bộ dùng phiên bản `0.1.0-dev` và code `1`. [Workflow release](release.md) tự sinh
CalVer cùng version code tăng dần mà không sửa source. Để tái hiện metadata của workflow
trên máy, truyền Gradle properties có dấu nháy, ví dụ
`.\gradlew.bat :app:assembleRelease '-PinknestVersionName=2026.11.0' '-PinknestVersionCode=1000001'`.
Khi tái hiện bản release, dùng đúng các giá trị được ghi lại trong run tương ứng.

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.twotechy.inknest/.MainActivity
```

Có thể chạy `app` từ Android Studio hoặc dùng `.\gradlew.bat :app:installDebug`. Nếu có nhiều thiết bị ADB, thêm `-s SERIAL` sau `adb`; với tác vụ thiết bị của Gradle, chọn đích bằng `ANDROID_SERIAL`. APK ký bằng chứng chỉ khác không thể cập nhật bản đã cài. Gỡ ứng dụng xóa cài đặt và ghim, nên dùng thiết bị/profile thử nghiệm riêng khi phù hợp.

Xem [thiết lập](setup.md) để kết nối nguồn đọc thật. Debug/release thông thường không có chế độ dữ liệu mẫu. [Ký bản phát hành](release.md) là bước riêng với build APK debug.

## Kiểm tra

| Mục đích | Lệnh |
| --- | --- |
| Kiểm tra / áp dụng định dạng | `.\gradlew.bat spotlessCheck` / `.\gradlew.bat spotlessApply` |
| Biên dịch Kotlin | `.\gradlew.bat :app:compileDebugKotlin` |
| JVM test | `.\gradlew.bat :app:testDebugUnitTest` |
| Chạy riêng JVM test | `.\gradlew.bat :app:testDebugUnitTest --tests '*StatsViewModelTest'` |
| Lint Android | `.\gradlew.bat :app:lintDebug :app:lintRelease` |
| Debug và release tối ưu chưa ký | `.\gradlew.bat :app:assembleDebug :app:assembleRelease` |
| Biên dịch Android test không cần thiết bị | `.\gradlew.bat :app:assembleDebugAndroidTest` |
| Android test trên thiết bị kết nối | `.\gradlew.bat :app:connectedDebugAndroidTest` |
| APK đo hiệu năng | `.\gradlew.bat :app:assembleBenchmark :benchmark:assembleBenchmark` |

Với thay đổi code, chạy kiểm tra định dạng, test liên quan, build debug/release bị ảnh hưởng và lint. Không sửa Kotlin khi lint đang chạy. Đọc native test trước khi dùng thiết bị cá nhân: test SAF opt-in có thể thu hồi quyền đã cấu hình. Xem [kiểm tra độ tin cậy](reliability.md) cho phục hồi và vòng đời. Không có task `:benchmark:lintBenchmark`.

Thay đổi chỉ gồm tài liệu cần kiểm tra link, lệnh và diff thay vì chạy ma trận thiết bị. Báo rõ kiểm tra thực sự đã chạy và giới hạn; kiểm tra bố cục trên giả lập không xác nhận phần cứng E-Ink. Xem [đo hiệu năng](benchmarking.md) về dữ liệu mẫu cách ly và phép đo.

JVM test của repository tạo Preferences DataStore bằng `OkioStorage` và serializer Preferences thật trên đĩa, để ghi lặp lại được trên các hệ điều hành máy phát triển. Android integration test kiểm tra triển khai storage Android dùng trong production.

## GitHub Actions

[Workflow CI](../../.github/workflows/ci.yml) chạy với mọi pull request vào `master`, kể cả draft, push vào `master` và khi chạy thủ công. Check duy nhất `build` kiểm tra định dạng, JVM tests debug/release, Android lint debug/release và build APK debug/release cùng Android test APK. Thay đổi chỉ gồm docs vẫn chạy check này. CI không cần signing secrets.

CI dùng runner Ubuntu tiêu chuẩn, Temurin 21 và SDK package `platforms;android-37.0`. Chỉ push vào `master` được ghi Gradle cache; PR và workflow thủ công chỉ đọc. Report lỗi hết hạn sau một ngày. Khi chạy CI thủ công có thể bật `upload_debug_apk` để giữ APK debug một ngày; PR thông thường không upload APK.

Chạy [workflow Android tests](../../.github/workflows/android-tests.yml) thủ công tại Actions → Android tests → Run workflow. Workflow dùng một emulator Android 14/API 34 x86_64 và bộ integration test mặc định. Test dùng nguồn thật/thu hồi quyền vẫn là opt-in và bị skip. Report/logcat khi lỗi hết hạn sau một ngày. Kết quả emulator không xác nhận Android 8 hoặc E-Ink thật; benchmark và tạo profile vẫn theo [quy trình thiết bị](benchmarking.md).

Tự động hóa release được mô tả trong [ký bản phát hành](release.md). Workflow pin action và mặc định dùng token chỉ đọc. Repo giữ public, dùng runner tiêu chuẩn; cache trong giới hạn miễn phí 10 GB của repo. Allowance artifact thuộc organization, không có phần riêng cho mỗi repo. Retention log/artifact mặc định của repo là bảy ngày, artifact upload giữ một ngày. Không cấu hình nightly build hoặc runner trả phí.

# Kiến trúc InkNest

[English](../en/architecture.md) · [Mục lục](README.md)

Ranh giới trách nhiệm và vòng đời của module production duy nhất `:app`, dùng Kotlin/Compose và tiêm phụ thuộc thủ công qua hàm khởi tạo.

## Ranh giới trách nhiệm

| Tầng | Trách nhiệm |
| --- | --- |
| `data/source` | Truy cập SAF, parser có giới hạn, kết quả khả năng nguồn và ánh xạ lỗi dự kiến; không import repository. |
| `data/repository` | Dữ liệu ứng dụng có thể theo dõi, cấu hình nguồn, làm mới và ghi dữ liệu. |
| ViewModel | Trạng thái/thao tác giao diện; gọi repository trực tiếp, không thêm use case chỉ để chuyển tiếp. |
| Route | Theo dõi theo vòng đời, điều hướng, gọi nền tảng và hộp thoại tạm thời. |
| Screen | Hiển thị trạng thái/callback; không truy cập repository, đọc tệp hay phân tích dữ liệu. |
| `di` | DataStore, Room, bộ tải ảnh, đồng hồ, dispatcher dùng chung và factory ViewModel. |
| `ui/components`, `ui/theme` | Điều khiển, cử chỉ và token thiết kế dùng chung. |

Khởi tạo đồ thị phụ thuộc và dọn tệp tạm bị gián đoạn chạy ngoài luồng chính. Thành phần bất đồng bộ nhận dispatcher; repository và StatsViewModel dùng chung `StatisticsClock` có thể thay trong kiểm thử.
`data/source` chia theo trách nhiệm: `apps`, `updates`, `books`, `saf`, `koreader`, `formats` và `stats`. Contract và kiểu dữ liệu tại ranh giới nằm trong nhóm sở hữu; lớp triển khai contract nằm trong package con `impl` của nhóm đó. I/O có giới hạn/hỗ trợ hủy dùng chung nằm trong `io`; kết quả capability và ánh xạ lỗi nằm trong `capability`. Parser Lua của KOReader nằm trong `koreader/lua`.

`formats/<format>` chứa parser từng định dạng; `formats/xml` và `formats/archive` chứa helper phân tích dùng chung. `BookFileExtensions` và `BookMimeTypes` nằm trong `formats` cùng kết quả/lỗi metadata.

`BookProbeDataSource` dùng chung việc phân giải tài liệu, đọc metadata/sidecar có giới hạn và lấy mẫu checksum. Helper riêng nằm cạnh tính năng; model sách nằm cùng `ReadingItem`.

## Lưu trữ và theo dõi

- Room schema 8 lưu đệm sách hiện tại/lịch sử, danh mục thư mục và thống kê. Trong giai đoạn phát triển, cache có thể bị xóa và tạo lại; chưa cam kết tương thích nâng cấp ổn định.
- DataStore lưu riêng ghim, tùy chọn và lựa chọn nguồn. Locale dùng lưu trữ AppCompat, không dùng Room/DataStore.
- Xử lý lỗi theo dõi I/O/SQLite dự kiến trước khi chia sẻ flow, thử lại sau 1/2/4/8/16/30 giây và cho phép thử lại thủ công. Giữ bản chụp thành công; phục hồi không tạo dữ liệu rỗng giả hoặc quét SAF.
- Giữ tín hiệu hủy coroutine. Ghi thất bại phải báo lỗi, không báo thành công hoặc thay giá trị đã lưu.

Xem contract tại [bộ nhớ đệm đọc](reading-cache.md), [Thư viện](library.md), [Thống kê](statistics.md) và [kết nối nguồn](koreader-integration.md).

## Trạng thái và tác vụ

- Effect Compose STARTED gọi AppViewModel → `ForegroundRefreshCoordinator` để làm mới dữ liệu đọc/thống kê khi ở foreground. Activity xử lý HOME intent. Các bên gọi repository đồng thời dùng chung tác vụ, không có freshness TTL; đổi tab không quét nguồn.
- Chủ đề/thiết lập ở gốc chỉ theo dõi tùy chọn ứng dụng, không theo dõi trạng thái nguồn/thao tác Cài đặt. Hoàn tất thiết lập có composition scope riêng để đổi nguồn không làm cập nhật lại Trang chủ.
- Cài đặt cung cấp một `SettingsUiState`; trạng thái tùy chọn giữ bản chụp gần nhất khi ngừng theo dõi, dùng `WhileSubscribed(5_000)`. Giấy phép tải qua repository không chặn luồng chính, với retry/cache riêng từng thành phần.
- Cài đặt và thiết lập ban đầu có `SourceSetupStateHolder` riêng. Repository chia sẻ cấu hình đã lưu; mỗi holder giữ yêu cầu chọn thư mục, lỗi và tác vụ của mình. Thiết lập phân biệt FirstRun/Review; hoàn tất chỉ đóng wizard đang mở. HOME bỏ qua trong phiên, không lưu hoàn tất.
- ViewModel Trang chủ/Thư viện quản lý `ReadingOpenStateHolder`. `ReadingOpenRepository` chuẩn bị yêu cầu không phụ thuộc Android; Route ở RESUMED nhận mỗi Ready đúng một lần và gửi qua `ReadingItemDispatcher`. Pause, HOME hoặc disposal hủy tác vụ chờ; không lưu yêu cầu để phát lại. Context/Intent nằm ở `platform`.

## Dịch vụ nền tảng

- `AppLanguageController` dùng AppCompat: lưu ngôn ngữ riêng của hệ thống trên Android 13+, lưu tự động AppCompat trên bản cũ. SettingsRoute giữ hộp thoại ngôn ngữ. Tạo lại Activity giữ điều hướng/ViewModel, không phát lại thao tác. Lưu trữ AppCompat có thể phát cảnh báo StrictMode về đĩa trên Android 12 trở xuống trong debug. Chỉ đóng gói en/vi, tắt tách gói ngôn ngữ; đổi locale tải lại thông tin ứng dụng, không quét sách.
- `AppUpdateRepository` dùng nguồn GitHub và khóa riêng trong DataStore hiện có. Cài đặt giữ trạng thái cập nhật; effect STARTED riêng gọi kiểm tra qua AppViewModel. Lưu mốc thử/giới hạn yêu cầu. Xuống nền hủy tải dở; APK đã xác minh ở bộ nhớ riêng chờ cài chủ động. Xác minh nền tảng và chia sẻ FileProvider chỉ dùng thư mục cập nhật.
- SettingsViewModel phân tích Markdown bằng CommonMark được inject, ngoài luồng chính. Screen hiển thị nội dung Compose bất biến, không WebView/ảnh từ xa; Route chỉ mở liên kết web. Thông báo BSD có trong giấy phép. Mọi loại bản dựng dùng chung tải/xác minh; tên phiên bản không nhận diện không được coi là cũ hơn, và vẫn kiểm tra mã phiên bản/chữ ký APK. Xem [thiết lập](setup.md) và [phát hành](release.md).
- Debug bật StrictMode; release không chạy chẩn đoán. Module `:benchmark` riêng dùng UI/SAF thật với biến thể benchmark/profile. Fixture/thiết lập chỉ nằm trong các source set đó; release chỉ chứa profile đã tạo. Xem [đo hiệu năng](benchmarking.md).

Không có dịch vụ cập nhật nền, đồng bộ mạng hoặc telemetry.

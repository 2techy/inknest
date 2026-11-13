# Kiến trúc InkNest

[English](../en/architecture.md) · [Mục lục](README.md)

InkNest dùng luồng dữ liệu một chiều: repository cung cấp dữ liệu, ViewModel chuyển dữ liệu thành trạng thái giao diện (UI state), Route theo dõi trạng thái theo vòng đời và kết nối các thao tác, còn Screen hiển thị trạng thái và gọi callback khi người dùng tương tác.

## Ranh giới trách nhiệm

- `data/repository`: định nghĩa các hợp đồng dữ liệu và phần triển khai. DataStore lưu tùy chọn của người dùng; cấu hình nguồn có hợp đồng riêng. Nội dung giấy phép bên thứ ba trong asset được tải qua repository, với IO dispatcher có thể truyền vào để tránh chặn luồng chính. Screen và Route không tự đọc tệp.
- `di`: quản lý các đối tượng dùng chung trong toàn ứng dụng và factory tạo ViewModel. Cả hai loại bản dựng đều dùng các thành phần nền tảng thật. ReadingDataContainer dùng chung DataStore và Room sẵn có. Việc khởi tạo đồ thị phụ thuộc và dọn tệp tạm từ tác vụ bị gián đoạn chạy ngoài luồng chính.
- `ui/settings`: `SettingsViewModel` cung cấp một `SettingsUiState` công khai, bao gồm lỗi đọc tùy chọn ban đầu, thao tác với nguồn và trạng thái tải/thử tải lại giấy phép. Trạng thái nội bộ giữ lại dữ liệu gần nhất khi việc theo dõi tạm dừng. `WhileSubscribed(5_000)` giới hạn việc thu thập dữ liệu từ luồng phía trên theo các thành phần giao diện đang đăng ký theo dõi.
- `SettingsRoute`: theo dõi trạng thái theo vòng đời, xử lý điều hướng, intent của nền tảng và các hộp thoại chọn thư mục/xác nhận tạm thời. `SettingsScreen` cùng các hàm hỗ trợ chỉ nhận trạng thái và callback, không truy cập repository hay thực hiện I/O.
- `ui/onboarding`: Route theo dõi trạng thái của quy trình thiết lập ban đầu và xử lý thao tác Quay lại. Ứng dụng cung cấp tùy chọn hiển thị cùng các thao tác tương ứng; Route không phụ thuộc ViewModel của tính năng khác.
- Bản debug bật StrictMode; bản release dùng các hàm chẩn đoán không thực hiện thao tác nào. Dữ liệu mẫu tự tạo phục vụ kiểm thử chỉ nằm trong các source set benchmark/profile.
- `ui/InkNestApp`: nơi kết nối giao diện chủ đề, điều hướng chính và Route của từng tính năng. `ui/components` và `ui/theme` chứa hành vi giao diện cùng các giá trị thiết kế dùng chung.

## Phạm vi kiến trúc và lưu trữ

Cập nhật ứng dụng có `AppUpdateRepository`, nguồn GitHub và khóa riêng trong DataStore hiện có.
Cài đặt theo dõi trạng thái cập nhật; tùy chọn chủ đề/Trang chủ không chứa trạng thái này. Effect STARTED
riêng gọi AppViewModel để kiểm tra ở foreground. Bản release kiểm tra tối đa mỗi 24 giờ, lưu mốc thử và
giới hạn yêu cầu của GitHub. Xuống background sẽ hủy tải dở; APK đã xác minh được giữ trong bộ nhớ riêng
để người dùng chủ động cài sau. Lớp nền tảng xác minh APK và FileProvider chỉ chia sẻ thư mục cập nhật.
Không dùng dịch vụ nền, đồng bộ mạng hoặc telemetry.

SettingsViewModel phân tích Markdown của ghi chú bằng CommonMark với dispatcher truyền vào, chạy ngoài
luồng chính. Screen nhận văn bản có định dạng bất biến để hiển thị bằng Compose/token giao diện, không
dùng WebView hoặc tải ảnh từ xa. Route mở liên kết web; thông báo BSD của CommonMark có trong giấy phép
bên thứ ba hiện có.

Giao diện cập nhật có một nút chính đổi theo trạng thái kiểm tra, tải, hủy và cài.
Các loại bản dựng dùng chung luồng tải/xác minh. Tên phiên bản không nhận diện được vẫn cho tải bản đã
tìm thấy, không kết luận là bản mới hơn. Bộ xác minh nền tảng vẫn chặn mã phiên bản APK không tăng
và chữ ký không tương thích trước khi cài.

Ứng dụng chính chỉ có một module `:app`, dùng cơ chế tiêm phụ thuộc thủ công qua hàm khởi tạo. Cơ sở dữ liệu đọc dùng Room schema 6. Trong giai đoạn phát triển, nếu không có migration phù hợp, Room được phép xóa và tạo lại bộ nhớ đệm; tùy chọn, ứng dụng đã ghim và lựa chọn nguồn vẫn nằm riêng trong DataStore. Dự án chưa cam kết giữ nguyên dữ liệu qua các lần nâng cấp.

ViewModel gọi trực tiếp repository để thay đổi tùy chọn và cấu hình nguồn. Khi repository gặp lỗi, ứng dụng không được thay giá trị đã lưu hoặc báo thành công giả. Nhãn giao diện dùng tài nguyên chuỗi của Android. Khi xử lý lỗi coroutine, phải tiếp tục truyền tín hiệu hủy tác vụ.

Cài đặt có bốn nhóm: Đọc và dữ liệu, Giao diện, Trình khởi chạy, Giới thiệu. Cách chia menu này phục vụ người dùng và không thay đổi ranh giới repository: tùy chọn ứng dụng vẫn được lưu riêng với nguồn đọc và bộ nhớ đệm đọc.

## Dữ liệu đọc

Room lưu sách đang đọc, lịch sử đọc, danh mục sách quét từ thư mục và các bản chụp dữ liệu thống kê. DataStore lưu ứng dụng đã ghim, tùy chọn và lựa chọn nguồn. Thống kê không phụ thuộc việc phân tích sách đang đọc hay việc sách có nằm trong Thư viện hay không. Route không quét tệp hoặc tổng hợp thống kê. Xem [thư viện](library.md), [thống kê](statistics.md) và [bộ nhớ đệm đọc](reading-cache.md) để biết quy tắc làm mới, hủy tác vụ và cách ly nguồn.

## Trạng thái và trách nhiệm quản lý tác vụ

Cài đặt và thiết lập ban đầu có `SourceSetupStateHolder` riêng. Hai nơi chia sẻ cấu hình đã lưu qua repository, nhưng tự quản lý yêu cầu chọn thư mục, tác vụ đang chạy và lỗi của mình. Thiết lập ban đầu cung cấp một UiState duy nhất; trạng thái điều hướng phân biệt FirstRun với Review. Hoàn tất chỉ đóng quy trình thiết lập đang mở. Nhấn HOME bỏ qua thiết lập trong phiên hiện tại, không lưu rằng người dùng đã hoàn tất.

Effect Compose ở trạng thái STARTED gọi AppViewModel, sau đó chuyển việc cập nhật dữ liệu đọc và thống kê cho `ForegroundRefreshCoordinator` dùng chung trong toàn ứng dụng. Activity xử lý intent của nền tảng, không điều phối thứ tự làm mới. Việc làm mới lúc khởi động chỉ do một nơi phụ trách; thay đổi nguồn và yêu cầu làm mới chủ động vẫn được xử lý ở repository. Các yêu cầu đồng thời dùng chung tác vụ, không áp dụng thời hạn coi dữ liệu là còn mới (freshness TTL).

AppViewModel cũng cung cấp tùy chọn ứng dụng để chọn giao diện chủ đề và quyết định có hiển thị thiết lập ban đầu hay không. Thành phần gốc không theo dõi trạng thái nguồn/thao tác của Cài đặt; việc này thuộc SettingsRoute. Trạng thái hoàn tất thiết lập được theo dõi trong một phạm vi composition riêng, tránh khiến toàn bộ khung Trang chủ phải cập nhật lại mỗi khi nguồn thay đổi.

Khi theo dõi Room/DataStore, ứng dụng xử lý các lỗi I/O/SQLite dự kiến trước khi chia sẻ luồng trạng thái. Các lần thử lại cách nhau 1/2/4/8/16/30 giây; tác vụ vẫn có thể bị hủy, và yêu cầu thử lại thủ công sẽ đánh thức lần đọc đang chờ. Quá trình phục hồi không phát dữ liệu rỗng giả và không quét SAF. Trang chủ kết hợp trạng thái từ các repository độc lập, giữ nguyên phần ghim, thống kê hoặc sách đọc không bị ảnh hưởng bởi lỗi.

`ReadingOpenRepository` kiểm tra nguồn và chuẩn bị yêu cầu mở sách không phụ thuộc Android. ViewModel của Trang chủ và Thư viện quản lý `ReadingOpenStateHolder`. Route tiếp nhận mỗi yêu cầu Ready đúng một lần và chỉ gửi qua `ReadingItemDispatcher` khi đang ở trạng thái RESUMED. Khi chuyển sang Pause hoặc bị loại khỏi composition, Route hủy bước chuẩn bị và bỏ yêu cầu đang chờ. Context/Intent nằm ở tầng platform; yêu cầu mở sách không được lưu để khôi phục về sau.

Kết quả kiểm tra khả năng truy cập nguồn và việc ánh xạ ngoại lệ thuộc `data/source`; mã nguồn ở tầng này không import repository. Dispatcher được truyền qua hàm khởi tạo. Repository và StatsViewModel dùng chung `StatisticsClock` do DI cung cấp; kiểm thử thay đồng hồ và dispatcher mà không đổi giờ hệ thống.

Đồ thị phụ thuộc của ứng dụng chính nằm trong `:app`. Module instrumentation riêng `:benchmark` kiểm thử biến thể benchmark có cấu hình gần với release và biến thể profile chưa rút gọn mã, thông qua giao diện và SAF thật. Thiết lập dữ liệu mẫu chỉ có trong các source set tương ứng; bản release chứa profile ứng dụng nhưng không chứa dữ liệu kiểm thử hay điểm truy cập thiết lập đó. Xem [đo hiệu năng](benchmarking.md).

Ngôn ngữ ứng dụng do nền tảng quản lý. AppLanguageController dùng AppCompat: Android 13 trở lên lưu ngôn ngữ riêng cho ứng dụng bằng cơ chế hệ thống; các bản cũ dùng cơ chế lưu tự động của AppCompat. SettingsRoute kết hợp lựa chọn này vào trạng thái màn hình và quản lý hộp thoại chọn ngôn ngữ tạm thời. Không lưu locale trong Room hoặc DataStore tùy chọn ứng dụng. Việc tạo lại Activity giữ nguyên điều hướng và ViewModel; đổi ngôn ngữ không thực hiện lại thao tác chọn trước đó.

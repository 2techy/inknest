# Kiến trúc InkNest

[English](../en/architecture.md) · [Mục lục](README.md)

InkNest dùng luồng dữ liệu một chiều: repository cung cấp dữ liệu; ViewModel tạo UI state quan sát được; Route thu thập state theo lifecycle và kết nối thao tác; Screen hiển thị state/callback.

## Ranh giới trách nhiệm

- `data/repository`: hợp đồng và triển khai dữ liệu. DataStore giữ tùy chọn; cấu hình nguồn có hợp đồng riêng. Repository đọc thông báo giấy phép từ asset an toàn với main thread, có IO dispatcher inject được; Screen/Route không đọc tệp.
- `di`: dependency cấp ứng dụng và ViewModel factory. Hai build type dùng dependency nền tảng thật. ReadingDataContainer chia sẻ DataStore và Room hiện có. Khởi tạo graph và dọn tệp tạm bị gián đoạn chạy ngoài main.
- `ui/settings`: `SettingsViewModel` quản lý một `SettingsUiState` công khai, gồm lỗi đọc tùy chọn ban đầu, thao tác nguồn và tải/thử lại giấy phép. State nội bộ giữ snapshot khi tạm ngừng thu thập. `WhileSubscribed(5_000)` giới hạn upstream theo subscriber UI đang hoạt động.
- `SettingsRoute`: thu thập theo lifecycle, điều hướng, intent nền tảng và hộp thoại thư mục/xác nhận tạm thời. `SettingsScreen` cùng helper chỉ nhận state/callback, không truy cập repository hay I/O.
- `ui/onboarding`: Route quản lý state wizard và Back. App cung cấp state/thao tác hiển thị; Route không phụ thuộc ViewModel tính năng khác.
- Cả debug và release không có route công cụ developer/source probe hay nguồn dữ liệu mẫu runtime. StrictMode chỉ bật ở debug; release dùng runtime diagnostics không thực hiện thao tác.
- `ui/InkNestApp`: composition root cho theme, điều hướng chính và feature route. `ui/components`, `ui/theme` sở hữu hành vi UI và token dùng chung.

## Quy mô và lưu trữ

Production có một module `:app`, inject qua constructor thủ công. Database đọc dùng Room schema 6 với destructive cache fallback trong giai đoạn phát triển; tùy chọn, ghim và lựa chọn nguồn nằm trong DataStore. Chưa cam kết nâng cấp ổn định. Graph hiện tại không yêu cầu framework DI, domain layer hay use case chỉ chuyển tiếp. Tác vụ tùy chọn/nguồn đơn giản dùng trực tiếp repository. Lỗi không được thay giá trị lưu bằng thành công giả; nhãn UI là string resource. Xử lý coroutine phải giữ cancellation.

Cài đặt có Đọc và dữ liệu, Giao diện, Trình khởi chạy, Giới thiệu. Đây là cách nhóm sản phẩm, không gộp ranh giới lưu tùy chọn với nguồn/cache đọc.

## Dữ liệu đọc

Room giữ sách hiện tại/lịch sử, danh mục quét thư mục và snapshot thống kê. DataStore giữ ghim/tùy chọn/lựa chọn nguồn; không tạo store thứ hai. Thống kê độc lập với parser sách hiện tại và thành viên thư viện. Route không quét tệp hay tổng hợp thống kê. Xem [thư viện](library.md), [thống kê](statistics.md), [cache đọc](reading-cache.md) về làm mới, hủy và ranh giới nguồn.

## State và vòng đời công việc

Cài đặt và thiết lập ban đầu có `SourceSetupStateHolder` riêng. Chúng chia sẻ cấu hình đã lưu qua repository, còn picker, tác vụ đang chạy và lỗi thuộc từng owner. Onboarding cung cấp một UiState; điều hướng phân biệt FirstRun/Review, hoàn tất chỉ đóng wizard đang mở. HOME bỏ qua thiết lập trong phiên hiện tại mà không lưu hoàn tất.

Effect Compose STARTED gọi AppViewModel, rồi chuyển cho `ForegroundRefreshCoordinator` cấp ứng dụng. Activity xử lý intent nền tảng thay vì sắp thứ tự refresh. Startup có một owner; đổi nguồn và làm mới rõ ràng vẫn thuộc repository. Yêu cầu đồng thời chia sẻ job, không có freshness TTL.

AppViewModel cung cấp tùy chọn cho theme/cổng thiết lập. Root không thu thập state nguồn/thao tác Settings; SettingsRoute sở hữu phần này. Quan sát hoàn tất onboarding được giới hạn composition riêng để cập nhật nguồn không làm vô hiệu toàn bộ Home scaffold.

Quan sát Room/DataStore phục hồi lỗi I/O/SQLite dự kiến trước khi share state. Backoff là 1/2/4/8/16/30 giây, giữ cancellation và thao tác thử lại đánh thức lần đọc đang chờ. Phục hồi không phát snapshot rỗng giả và không quét SAF. Home kết hợp các repository độc lập, giữ nội dung ghim/thống kê/đọc không bị ảnh hưởng.

`ReadingOpenRepository` chuẩn bị yêu cầu mở độc lập Android, đã kiểm tra nguồn. ViewModel Trang chủ/Thư viện sở hữu `ReadingOpenStateHolder`; Route nhận mỗi yêu cầu Ready một lần và dispatch qua `ReadingItemDispatcher` khi RESUMED. Pause/disposal hủy chuẩn bị và bỏ yêu cầu chờ. Context/Intent ở tầng platform; yêu cầu không lưu bền.

Capability và ánh xạ exception thuộc `data/source`; source không import repository. Dispatcher là dependency constructor. Repository và StatsViewModel dùng chung `StatisticsClock` từ DI; test thay đồng hồ/dispatcher, không đổi giờ hệ thống.

Graph production nằm trong `:app`. Module instrumentation `:benchmark` điều khiển biến thể benchmark gần release và profile chưa minify qua UI/SAF thật. Thiết lập tổng hợp chỉ có ở source set tương ứng; release chứa profile, không chứa fixture hoặc setup entry point. Xem [đo hiệu năng](benchmarking.md).

Ngôn ngữ là cấu hình nền tảng: AppLanguageController ủy quyền AppCompat, dùng lưu locale native trên Android 13+ và AppCompat tự lưu ở bản cũ. SettingsRoute kết hợp lựa chọn này vào state, sở hữu hộp thoại tạm. Không lưu locale trong Room hoặc app-preferences DataStore. Activity recreation giữ điều hướng và ViewModel; đổi ngôn ngữ không phát lại thao tác chọn.

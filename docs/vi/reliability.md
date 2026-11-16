# Độ tin cậy và nghiệm thu trên thiết bị

[English](../en/reliability.md) · [Mục lục](README.md)

Kiểm tra có thể lặp lại cho phục hồi, vòng đời và nghiệm thu thiết bị thật. Quy trình không khẳng định thiết bị/phiên bản nào đã đạt.

## Chuẩn bị môi trường kiểm thử

Dùng thiết bị thử nghiệm hoặc giả lập đã cấp quyền gỡ lỗi, cùng sách mẫu tự tạo hoặc sách không chứa dữ liệu riêng tư. Biên dịch và cài đặt theo [hướng dẫn phát triển](development.md). Khi báo kết quả, ghi rõ biến thể/phiên bản ứng dụng, Android/firmware, phiên bản KOReader, cấu hình hiển thị và đặc điểm bộ dữ liệu. Lưu log, trace và kết quả đo trong thư mục build được Git bỏ qua.

Đọc mã kiểm thử Android trước khi chạy. `SourceAccessIntegrationTest` chỉ chạy khi có tham số instrumentation `source_access_integration=true`, yêu cầu đã cấu hình dữ liệu đọc và sẽ chủ động thu hồi quyền truy cập một thư mục sách. Hãy dùng thiết bị thử nghiệm, rồi cấp lại quyền qua trình chọn thư mục hệ thống. Bộ kiểm thử mặc định không bật bài kiểm tra này.

## Phục hồi nguồn và bộ nhớ đệm

| Tình huống | Thao tác và kết quả mong đợi |
| --- | --- |
| Chọn sai thư mục | Chọn thư mục không phải dữ liệu KOReader hoặc hủy trình chọn. Cấu hình và nội dung đã lưu đang dùng phải được giữ nguyên. |
| Mất quyền truy cập | Thu hồi quyền một thư mục sách thử nghiệm, làm mới rồi khởi động lại. Ứng dụng phải báo mất quyền, giữ nội dung đã lưu và vẫn dùng được các thư mục khác. Chọn lại thư mục bị ảnh hưởng để khôi phục. |
| Một nguồn bị lỗi | Làm một thư mục sách không truy cập được, rồi làm mới tất cả. Các thư mục còn truy cập được vẫn cập nhật; thư mục lỗi giữ bản chụp dữ liệu cũ. |
| Cách ly nguồn | Đổi nguồn trong khi làm mới. Tác vụ cũ hoàn tất muộn không được đưa dữ liệu vào cấu hình mới. |
| Dữ liệu đầu vào bị hỏng | Dùng Lua/EPUB/SQLite hỏng hoặc schema thống kê không hợp lệ. Ứng dụng phải báo lỗi hoặc hiển thị phần thông tin còn đọc được, tùy trường hợp; không được giả lập kết quả đọc thành công nhưng rỗng. |
| Tính nhất quán của bản chụp | Làm mới nhiều lần, kể cả khi SQLite đang ghi WAL. Tổng không được đếm trùng; nhập dữ liệu thất bại hoặc bị hủy phải giữ phiên bản dữ liệu đang dùng trước đó. |
| Xóa bộ nhớ đệm/ngắt kết nối | Xóa bộ nhớ đệm rồi ngắt kết nối. Xóa bộ nhớ đệm phải tạo lại dữ liệu từ nguồn đã chọn; ngắt kết nối phải xóa dữ liệu/quyền của nguồn. Cả hai giữ ứng dụng đã ghim, tùy chọn giao diện và không sửa tệp gốc. |

## Vòng đời và tương tác

- Mở luân phiên hai cuốn sách từ Trang chủ/Thư viện, cả khi KOReader chưa chạy (cold) và đã chạy (warm). Kiểm tra đúng sách và vị trí đọc đã lưu trong KOReader; gửi intent thành công chưa đủ.
- Chuyển ứng dụng xuống nền hoặc nhấn HOME khi đang chuẩn bị mở sách. Yêu cầu đang chờ không được tự mở về sau; quay lại ứng dụng không được gửi lại yêu cầu đã gửi.
- Tạo lại Activity và mô phỏng tiến trình bị hệ thống kết thúc khi chạy nền. Kiểm tra việc khôi phục nội dung đã lưu, trang hiện tại, từ khóa, bộ lọc, vị trí cuộn và lựa chọn Thống kê trong phạm vi được hỗ trợ.
- Gây lỗi khi lưu tùy chọn: giao diện không được báo thành công hoặc thay giá trị đã lưu.
- Kiểm tra nút Quay lại của hệ thống, chọn ứng dụng Trang chủ mặc định, mở ứng dụng, ghim, vuốt tab và kéo làm mới. Đổi tab, khoảng thời gian hoặc đồng hồ không được quét nguồn.
- Đổi ngôn ngữ ứng dụng, cỡ chữ (gồm mức 150%), chiều rộng cửa sổ, ngày và múi giờ. Kiểm tra xuống dòng, khả năng tiếp cận các nút điều khiển, ranh giới ngày/tháng và giờ mùa hè (DST), không thay đổi dữ liệu nguồn.
- Xem log StrictMode của bản debug để tìm I/O ngoài dự kiến trên luồng chính. Phân biệt với thông báo do AppCompat lưu ngôn ngữ trên Android 12 trở xuống, được mô tả trong [kiến trúc](architecture.md).

Hành vi Room, SAF và vòng đời trên Android được kiểm tra bằng kiểm thử tích hợp Android. Phần tổng hợp dữ liệu, ánh xạ lỗi nguồn và ViewModel cũng có các bài kiểm thử JVM riêng. Xem [kiến trúc](architecture.md) về phân chia trách nhiệm và quy tắc phục hồi.

## Nghiệm thu trên thiết bị thật

Trên iReader Tango 2, kiểm tra trực tiếp độ tương phản, bóng mờ, cách làm mới màn hình, vùng chạm, mở sách khi KOReader chưa chạy/đã chạy và quay về sau phiên đọc dài. Kiểm tra luồng trình khởi chạy, kết nối nguồn và mở sách trên Samsung Galaxy S25 Ultra, thiết bị phát triển phụ. Kiểm tra lại sau khi cập nhật firmware, KOReader hoặc thay đổi chứng chỉ ký bản release.

Dùng [đo hiệu năng](benchmarking.md) để so độ trễ/bộ nhớ có kiểm soát. Ảnh chụp và phép đo dữ liệu giả không chứng nhận E-Ink thật hoặc việc mở đúng sách.

Trước khi phân phối, hoàn tất [kiểm tra APK đã ký](release.md).

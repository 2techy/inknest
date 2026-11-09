# Thiết lập InkNest

[English](../en/setup.md) · [Mục lục](README.md)

Build/cài APK theo [hướng dẫn phát triển](development.md) hoặc [ký bản phát hành](release.md), mở InkNest và chọn E-Ink hoặc Tiêu chuẩn. Thiết lập không bắt buộc; Bỏ qua mở Trang chủ. Có thể mở lại tại **Cài đặt → Giới thiệu → Xem lại thiết lập**.

## Kết nối KOReader

1. Cài KOReader riêng và mở sách một lần để tạo thư mục dữ liệu.
2. Trong InkNest, mở **Cài đặt → Đọc và dữ liệu**, chọn ứng dụng nguồn KOReader.
3. Chọn **Thư mục dữ liệu KOReader** chứa `settings.reader.lua` hoặc `history.lua`.
4. Dùng **Thêm thư mục sách** để chọn một hoặc nhiều thư mục; có quét thư mục con. Chạm thư mục đã chọn để thay thế, hoặc xóa khỏi InkNest mà không xóa tệp. Thư mục chồng lấn bị từ chối vì thư mục cha đã bao gồm thư mục con. Android chỉ cấp quyền đọc cho từng cây đã chọn. Các điều khiển này cũng có trong thiết lập ban đầu.
5. Làm mới dữ liệu. Thư viện gồm cả sách chưa từng mở. Sách đang đọc và trạng thái đến từ KOReader; Thống kê cần `settings/statistics.sqlite3` của KOReader.

Đường dẫn tùy cách cài đặt; chọn qua trình chọn hệ thống. InkNest không giả định đường dẫn cố định `/koreader`, `/webdav` hoặc Downloads. Chưa hỗ trợ nhà cung cấp tài liệu SD/đám mây. Không chọn cả gốc bộ nhớ để khắc phục thiếu dữ liệu.

Thư viện mặc định gộp mọi thư mục sách. Menu thư mục lọc theo một nguồn và kết hợp với tìm kiếm, trạng thái, sắp xếp. Làm mới quét mọi nguồn đã cấu hình; nguồn không truy cập được giữ cache, các nguồn khác vẫn tiếp tục. Khi mất quyền, chọn lại đúng thư mục đó.

Bản phát triển cũ có thể dùng khóa cấu hình thư mục sách trước đây. Khi nâng cấp từ các bản đó, chọn lại thư mục sách; thư mục KOReader, ghim và tùy chọn hiển thị được lưu riêng. Làm mới thất bại giữ nội dung đã lưu.

**Xóa bộ nhớ đệm đọc** tạo lại dữ liệu đọc và bìa mà không đổi ghim/giao diện. **Ngắt kết nối nguồn** xóa cấu hình nguồn và cache đọc, không xóa sách hoặc dữ liệu KOReader.

## Đọc sách và trình khởi chạy

Chạm sách trong Thư viện để mở tài liệu gốc bằng KOReader đã chọn; nhấn giữ để xem chi tiết. Trang chủ dùng cùng luồng mở tài liệu. Thiếu tệp, quyền hoặc trình xử lý thì báo lỗi, không chỉ mở KOReader rồi bỏ qua sách đã chọn. Android nhận intent không chứng minh KOReader đã mở đúng sách; dữ liệu đọc được làm mới khi quay lại.

Có thể đặt launcher mặc định tại **Cài đặt → Trình khởi chạy**. Để đổi lại, mở Cài đặt Android → Ứng dụng → Ứng dụng mặc định → Ứng dụng Trang chủ và chọn launcher trước đó; tên mục tùy firmware. Khi InkNest giữ vai trò Home, HOME quay về Trang chủ InkNest. Ứng dụng và ghim vẫn hoạt động khi chưa kết nối nguồn đọc.

E-Ink chuyển biểu tượng ứng dụng sang thang xám; Tiêu chuẩn giữ màu gốc. Cả hai dùng chuyển trạng thái tĩnh. Giao diện nhóm Ngôn ngữ và Chế độ hiển thị dưới Chung, mở hộp thoại chọn. Các mục trên Trang chủ có thể ẩn thống kê nhanh và ứng dụng ghim mà không xóa ghim. Cỡ chữ theo hệ thống. Tần suất làm mới màn hình vẫn do firmware quản lý.

## Ngôn ngữ ứng dụng

**Cài đặt → Giao diện → Ngôn ngữ** có Theo hệ thống, English và Tiếng Việt. Theo hệ thống dùng danh sách locale Android; ngôn ngữ không hỗ trợ quay về tiếng Anh. Tên English và Tiếng Việt giữ nguyên ngôn ngữ riêng. Thiết lập ban đầu theo ngôn ngữ ứng dụng, không có bước chọn riêng. Đổi ngôn ngữ tái tạo Activity nhưng giữ trang Cài đặt hiện tại và đóng popup.

InkNest dùng locale theo ứng dụng của AndroidX AppCompat, không lưu thêm trong DataStore. Android 13 trở lên chia sẻ lựa chọn với cài đặt ngôn ngữ ứng dụng của hệ thống. Bản Android cũ dùng lưu trữ tự động của AppCompat; cơ chế này có thể tạo chẩn đoán truy cập đĩa StrictMode trên debug ở Android 12 trở xuống, theo [tài liệu Android](https://developer.android.com/guide/topics/resources/app-languages).

Resource tiếng Anh là mặc định; tiếng Việt nằm trong `values-vi`. Chỉ đóng gói locale en/vi, kể cả dependency; App Bundle không tách ngôn ngữ để cả hai dùng được ngoại tuyến. Ngày, chú thích biểu đồ và định dạng số theo locale UI. Đổi ngôn ngữ không đổi múi giờ, ngày đọc đã lưu hay cách tính chuỗi ngày. Metadata sách, tên ứng dụng ngoài và văn bản giấy phép giữ nội dung từ nguồn. Metadata ứng dụng đã cài được tải lại khi locale hiệu lực thay đổi; duyệt sách không cần quét lại nguồn.

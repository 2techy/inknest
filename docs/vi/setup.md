# Thiết lập InkNest

[English](../en/setup.md) · [Mục lục](README.md)

Tải và cài APK từ [bản phát hành mới nhất](https://github.com/2techy/inknest/releases/latest), hoặc tự biên dịch theo [hướng dẫn phát triển](development.md) hay [ký bản phát hành](release.md). Mở InkNest và chọn chế độ hiển thị E-Ink hoặc Tiêu chuẩn. Bạn có thể bỏ qua thiết lập để vào Trang chủ, rồi mở lại bất cứ lúc nào tại **Cài đặt → Giới thiệu → Xem lại thiết lập**.

## Kết nối KOReader

1. Cài KOReader riêng và mở một cuốn sách để KOReader tạo thư mục dữ liệu.
2. Trong InkNest, mở **Cài đặt → Đọc và dữ liệu**, rồi chọn ứng dụng KOReader.
3. Chọn **Thư mục dữ liệu KOReader** có chứa `settings.reader.lua` hoặc `history.lua`.
4. Chọn **Thêm thư mục sách** để thêm một hoặc nhiều thư mục. InkNest sẽ quét cả thư mục con. Chạm thư mục đã chọn để thay bằng thư mục khác; xóa thư mục khỏi InkNest không xóa tệp gốc. Không thể chọn đồng thời các thư mục lồng nhau vì thư mục cha đã bao gồm thư mục con. Android chỉ cấp quyền đọc trong từng cây thư mục bạn chọn. Bạn cũng có thể thực hiện các thao tác này trong thiết lập ban đầu.
5. Làm mới dữ liệu. Thư viện sẽ có cả các tệp được hỗ trợ chưa từng mở. Sách đang đọc trên Trang chủ và trạng thái đọc lấy từ KOReader; Thống kê cần cơ sở dữ liệu `settings/statistics.sqlite3` của KOReader.

Vị trí thư mục tùy cách cài đặt, nên hãy chọn qua trình chọn thư mục của hệ thống. InkNest không mặc định dữ liệu nằm ở `/koreader`, `/webdav` hay Downloads. Hiện chưa hỗ trợ nhà cung cấp tài liệu trên thẻ SD hoặc đám mây. Không chọn toàn bộ gốc bộ nhớ để khắc phục việc thiếu dữ liệu.

Theo mặc định, Thư viện gộp sách từ tất cả thư mục đã chọn. Menu thư mục cho phép lọc theo một nguồn, kết hợp với tìm kiếm, trạng thái đọc và cách sắp xếp. Làm mới sẽ quét tất cả thư mục đã cấu hình. Nếu một thư mục không truy cập được, sách đã lưu của thư mục đó vẫn được giữ, còn các thư mục khác tiếp tục cập nhật. Khi mất quyền, hãy chọn lại đúng thư mục đó.

Làm mới thất bại không xóa nội dung đã lưu. **Xóa bộ nhớ đệm đọc** tạo lại dữ liệu đọc và bìa, giữ nguyên ứng dụng đã ghim cùng tùy chọn giao diện. **Ngắt kết nối nguồn** xóa cấu hình nguồn và bộ nhớ đệm đọc, không xóa sách hoặc dữ liệu KOReader.

## Đọc sách và dùng trình khởi chạy

Chạm sách trong Thư viện để mở tài liệu gốc bằng KOReader đã chọn; nhấn giữ để xem chi tiết. Trang chủ dùng cùng cách mở sách. Nếu thiếu tệp, mất quyền hoặc không có trình xử lý tương thích, InkNest sẽ báo lỗi. Việc Android nhận intent chưa chứng minh KOReader đã mở đúng sách; dữ liệu đọc được cập nhật lại từ KOReader khi bạn quay về InkNest.

Bạn có thể đặt InkNest làm trình khởi chạy mặc định tại **Cài đặt → Trình khởi chạy**. Để đổi lại, mở **Cài đặt Android → Ứng dụng → Ứng dụng mặc định → Ứng dụng Trang chủ** và chọn trình khởi chạy trước đó; tên các mục có thể khác tùy firmware. Khi InkNest là ứng dụng Trang chủ mặc định, nhấn HOME sẽ về Trang chủ InkNest. Danh sách ứng dụng và ứng dụng đã ghim vẫn dùng được khi chưa kết nối nguồn đọc.

Chế độ E-Ink hiển thị biểu tượng ứng dụng bằng thang xám; Tiêu chuẩn giữ màu gốc. Cả hai đều không dùng hiệu ứng chuyển động khi chuyển trạng thái. Trong Giao diện, phần Chung có Ngôn ngữ và Chế độ hiển thị; chạm để mở hộp thoại lựa chọn. Phần Các mục trên Trang chủ cho phép ẩn thống kê nhanh và ứng dụng đã ghim mà không xóa ghim. Cỡ chữ theo cài đặt hệ thống. Việc làm mới màn hình vẫn do firmware của thiết bị quản lý.

## Ngôn ngữ ứng dụng

**Cài đặt → Giao diện → Ngôn ngữ** có ba lựa chọn: Theo hệ thống, English và Tiếng Việt. Theo hệ thống dùng danh sách ngôn ngữ Android; nếu ngôn ngữ không được hỗ trợ, ứng dụng dùng tiếng Anh. Tên English và Tiếng Việt luôn giữ nguyên cách viết trong ngôn ngữ tương ứng. Thiết lập ban đầu dùng ngôn ngữ của ứng dụng, không có bước chọn ngôn ngữ riêng. Đổi ngôn ngữ sẽ tạo lại Activity, nhưng vẫn giữ trang Cài đặt hiện tại và đóng các menu đang mở.

InkNest dùng cơ chế ngôn ngữ riêng cho từng ứng dụng của AndroidX AppCompat, không lưu thêm lựa chọn này trong DataStore. Trên Android 13 trở lên, lựa chọn được dùng chung với cài đặt ngôn ngữ ứng dụng của hệ thống. Các bản Android cũ dùng cơ chế lưu tự động của AppCompat. Trên Android 12 trở xuống, cơ chế này có thể phát sinh thông báo StrictMode về truy cập đĩa trong bản debug, như mô tả trong [tài liệu Android](https://developer.android.com/guide/topics/resources/app-languages).

Tài nguyên tiếng Anh là mặc định; tiếng Việt nằm trong `values-vi`. Bản phân phối chỉ đóng gói tài nguyên en/vi, kể cả của thư viện phụ thuộc. App Bundle không tách gói ngôn ngữ, nên cả hai luôn có sẵn khi ngoại tuyến. Nhãn ngày, chú thích biểu đồ và định dạng số theo ngôn ngữ giao diện. Đổi ngôn ngữ không đổi múi giờ, ngày đọc đã lưu hoặc cách tính chuỗi ngày đọc. Thông tin sách, tên ứng dụng bên ngoài và nội dung giấy phép giữ nguyên từ nguồn. Thông tin ứng dụng đã cài được tải lại khi ngôn ngữ giao diện thực tế thay đổi; duyệt sách không cần quét lại nguồn.

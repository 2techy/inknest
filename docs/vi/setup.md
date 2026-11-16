# Thiết lập InkNest

[English](../en/setup.md) · [Mục lục](README.md)

Cài đặt InkNest, kết nối nguồn đọc, cập nhật ứng dụng và khôi phục quyền truy cập thư mục.

Tải APK từ [bản phát hành mới nhất](https://github.com/2techy/inknest/releases/latest), hoặc [biên dịch từ mã nguồn](development.md). Mở InkNest và chọn E-Ink hoặc Tiêu chuẩn. Có thể bỏ qua thiết lập và quay lại tại Cài đặt → Giới thiệu → Xem lại thiết lập.

## Kết nối KOReader

1. Cài riêng KOReader và mở một sách để tạo thư mục cài đặt.
2. Trong Cài đặt → Đọc và dữ liệu, chọn KOReader.
3. Chọn thư mục dữ liệu chứa `settings.reader.lua` hoặc `history.lua`.
4. Thêm một hoặc nhiều thư mục sách; bao gồm cả thư mục con. Chạm thư mục để thay, hoặc gỡ khỏi InkNest mà không xóa tệp. Không chấp nhận các thư mục gốc chồng lấn.
5. Làm mới. Thư viện gồm cả sách được nhận diện nhưng chưa từng mở; sách hiện tại/trạng thái lấy từ KOReader. Thống kê cần `settings/statistics.sqlite3`.

Chọn thư mục qua trình chọn của Android. Chỉ hỗ trợ bộ nhớ trong chính; không chọn cả gốc bộ nhớ để vượt qua lỗi quyền. Đường dẫn tùy cách cài đặt.

| Vấn đề hoặc thao tác | Kết quả / cách khôi phục |
| --- | --- |
| Mất quyền | Chọn lại đúng thư mục bị ảnh hưởng. |
| Một nguồn không làm mới được | Giữ sách đã lưu của nguồn đó; các nguồn khác vẫn cập nhật. |
| Làm mới thất bại | Giữ nội dung đã lưu và hiển thị lỗi. |
| Xóa bộ nhớ đệm đọc | Tạo lại dữ liệu đọc và bìa; giữ ghim và giao diện. |
| Ngắt kết nối | Xóa cấu hình/cache/quyền nguồn, không xóa sách hoặc dữ liệu KOReader. |

Thư viện gộp các thư mục sách; menu thư mục lọc một nguồn gốc. Làm mới quét mọi nguồn. Xem [Thư viện](library.md) và [Thống kê](statistics.md).

## Đọc sách và dùng trình khởi chạy

- Chạm hoặc nhấn giữ sách trong Thư viện để xem chi tiết, rồi dùng nút đọc để mở KOReader. Trang chủ mở trực tiếp sách hiện tại. Thiếu tệp, quyền hoặc trình xử lý sẽ báo lỗi; dữ liệu đọc cập nhật từ KOReader khi quay lại.
- Đặt ứng dụng Trang chủ mặc định tại Cài đặt → Trình khởi chạy. Để đổi lại, vào Cài đặt Android → Ứng dụng → Ứng dụng mặc định → Ứng dụng Trang chủ; tên mục tùy firmware. HOME về Trang chủ InkNest khi ứng dụng giữ vai trò này. Ứng dụng/ghim dùng được khi chưa có nguồn đọc.
- E-Ink dùng biểu tượng thang xám; Tiêu chuẩn giữ màu gốc. Cả hai dùng chuyển trạng thái tĩnh. Cài đặt → Giao diện điều chỉnh chế độ và các mục Trang chủ, gồm thống kê nhanh, nhịp đọc và ghim; ẩn ghim không xóa ghim. Cỡ chữ theo Android; firmware quản lý làm mới màn hình.

## Cập nhật ứng dụng

Mở Cài đặt → Giới thiệu → Cập nhật ứng dụng. Nếu chưa lưu kết quả thành công, trang sẽ kiểm tra; nếu có thì giữ kết quả đó. Bản release chính thức có thể tự kiểm tra tối đa mỗi 24 giờ khi InkNest mở; có thể tắt. Kiểm tra/tải cần mạng; đọc và trình khởi chạy vẫn dùng ngoại tuyến.

1. Chọn **Kiểm tra cập nhật**, rồi **Tải bản cập nhật** khi có. Mở **Có gì mới** để đọc ghi chú; liên kết mở bằng trình duyệt.
2. Giữ InkNest mở đến khi chuẩn bị xong. Rời ứng dụng hủy lượt tải dở; nút **Hủy** cũng dừng tải.
3. Chọn **Cài đặt bản cập nhật**. Nếu Android yêu cầu quyền cài từ InkNest, cấp quyền, quay lại và chọn Cài đặt lần nữa. Xác nhận trong trình cài Android.

APK hoàn tất được giữ khi hủy cài và xác minh lại trước khi thử tiếp. Checksum, package, phiên bản, Android và chữ ký phải hợp lệ. Lỗi mạng giữ kết quả/thời điểm kiểm tra trước đó. Liên kết GitHub mở trang phát hành. Xem [phát triển](development.md) để thử trên bản dựng cục bộ.

## Ngôn ngữ ứng dụng

Cài đặt → Giao diện → Ngôn ngữ có Theo hệ thống, English và Tiếng Việt. Ngôn ngữ hệ thống chưa hỗ trợ sẽ dùng tiếng Anh. Cả hai có sẵn ngoại tuyến; Android 13+ cũng cho chọn trong cài đặt ngôn ngữ ứng dụng của hệ thống.

Đổi ngôn ngữ giữ trang Cài đặt hiện tại và đóng popup. Ngày/số theo locale giao diện; không đổi múi giờ, ngày đọc hoặc cách tính chuỗi. Thông tin sách, tên ứng dụng ngoài và giấy phép giữ theo nguồn. Xem [kiến trúc](architecture.md) về lưu locale và chẩn đoán debug.

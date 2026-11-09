# Độ tin cậy và nghiệm thu thiết bị

[English](../en/reliability.md) · [Mục lục](README.md)

## Chuẩn bị môi trường

Dùng thiết bị thử nghiệm hoặc giả lập đã cho phép debug cùng sách tổng hợp/không riêng tư. Build/cài theo [phát triển](development.md). Khi báo kết quả, ghi biến thể/phiên bản app, Android/firmware, KOReader, cấu hình hiển thị và đặc điểm dataset. Giữ log, trace và phép đo trong thư mục build được Git bỏ qua.

Đọc Android test trước khi chạy. `SourceAccessIntegrationTest` chỉ chạy khi có instrumentation argument `source_access_integration=true`, cần dữ liệu đọc đã cấu hình và chủ động thu hồi quyền thư mục sách. Dùng thiết bị thử nghiệm, sau đó khôi phục quyền qua picker hệ thống. Bộ test mặc định không bật test này.

## Phục hồi nguồn và cache

| Tình huống | Thao tác và hành vi mong đợi |
| --- | --- |
| Chọn sai | Chọn thư mục không phải dữ liệu KOReader hoặc hủy picker. Giữ cấu hình/cache đang hoạt động. |
| Mất quyền | Thu hồi quyền thư mục sách thử nghiệm, refresh và restart. Báo mất quyền, giữ cache, nguồn khác vẫn dùng được. Chọn lại thư mục để phục hồi. |
| Một nguồn lỗi | Làm một thư mục sách không truy cập được, refresh mọi nguồn. Nguồn còn truy cập được vẫn cập nhật; nguồn lỗi giữ snapshot. |
| Cách ly nguồn | Đổi nguồn khi refresh. Job muộn không phát dữ liệu vào cấu hình mới. |
| Đầu vào hỏng | Dùng Lua/EPUB/SQLite hỏng và schema thống kê sai. Báo lỗi hoặc metadata một phần phù hợp; không tạo snapshot rỗng thành công giả. |
| Nhất quán snapshot | Refresh lặp lại, kể cả lúc SQLite có WAL. Không đếm trùng; import lỗi/hủy giữ generation trước. |
| Xóa/ngắt kết nối | Xóa cache rồi ngắt kết nối. Xóa cache tạo lại từ nguồn đã chọn; ngắt kết nối xóa dữ liệu/quyền nguồn. Cả hai giữ ghim/giao diện và không đổi tệp gốc. |

## Vòng đời và tương tác

- Mở luân phiên hai sách từ Trang chủ/Thư viện, khi KOReader cold và warm. Kiểm tra sách thật/vị trí lưu trong KOReader; gửi intent thành công chưa đủ.
- Background hoặc HOME khi đang chuẩn bị mở. Yêu cầu chờ không được tự mở sau; quay lại không phát lại yêu cầu đã gửi.
- Tái tạo Activity và mô phỏng process death khi chạy nền. Kiểm tra cache, trang hiện tại, query/filter/cuộn và lựa chọn Thống kê được khôi phục trong phạm vi hỗ trợ.
- Gây lỗi lưu tùy chọn: UI không báo thành công hoặc thay giá trị đã lưu.
- Kiểm tra Back hệ thống, chọn Home mặc định, mở app, ghim, vuốt tab và kéo làm mới. Đổi tab/mức thời gian/đồng hồ không quét nguồn.
- Đổi ngôn ngữ, cỡ chữ gồm 150%, chiều rộng cửa sổ, ngày và múi giờ. Kiểm tra xuống dòng, điều khiển truy cập được, ranh giới lịch/DST mà không đổi dữ liệu nguồn.
- Xem StrictMode debug cho I/O app ngoài dự kiến trên main. Phân biệt chẩn đoán lưu locale AppCompat ở Android 12 trở xuống, mô tả trong [thiết lập](setup.md).

Hành vi Room/SAF/lifecycle native thuộc Android test; tổng hợp, ánh xạ lỗi nguồn và ViewModel cũng có JVM test tập trung. Xem [kiến trúc](architecture.md) về trách nhiệm/phục hồi.

## Nghiệm thu thiết bị thật

Trên iReader Tango 2, kiểm tra tương phản, bóng mờ, làm mới, vùng chạm, mở cold/warm và quay lại sau phiên KOReader dài trên màn hình thật. Kiểm tra launcher/nguồn/mở sách trên Samsung Galaxy S25 Ultra là thiết bị mục tiêu phụ. Kiểm tra lại khi đổi firmware, KOReader hoặc chứng chỉ ký release.

Đo độ trễ refresh và bộ nhớ với dataset cố định, điều kiện so sánh được. Phân biệt PSS/RSS, mẫu rời rạc/peak liên tục, giả lập/phần cứng thật. Dùng [đo hiệu năng](benchmarking.md) cho so sánh tổng hợp có kiểm soát. Ảnh chụp và benchmark tổng hợp không xác nhận hành vi E-Ink vật lý hay mở sách đúng trong trình đọc thật.

Trước phân phối, hoàn tất [kiểm tra artifact đã ký](release.md). Đây là quy trình nghiệm thu, không khẳng định thiết bị/phiên bản cụ thể đã đạt.

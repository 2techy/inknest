# Kết nối nguồn KOReader

[English](../en/koreader-integration.md) · [Mục lục](README.md)

Contract truy cập SAF, giới hạn parser và lỗi của nguồn KOReader thật trong debug/release. Các bước chọn thư mục ở [thiết lập](setup.md).

## Cấu hình và quyền truy cập

Preferences DataStore lưu định danh nguồn, package `org.koreader.launcher` đã chọn, cây KOReader và các cây sách độc lập. Trình chọn hệ thống cấp quyền đọc lưu lâu dài. Chọn sai/hủy giữ cấu hình hiện hành; giải phóng quyền cũ không dùng. Ngắt kết nối xóa khóa/quyền nguồn, giữ ghim, giao diện và thiết lập. Room lưu cache; xem [cache đọc](reading-cache.md).

Chỉ hỗ trợ bộ nhớ trong chính qua ExternalStorageProvider Android; từ chối provider/volume khác. Kiểm tra document ID theo volume/đoạn đường dẫn; con phải trong gốc đã cấp quyền. Chỉ phân giải đường dẫn trong cây sách/KOReader đã cấu hình. Bí danh bộ nhớ chính phải khớp định danh filesystem chuẩn trên thiết bị. Giữ Unicode, hoa thường và khoảng trắng; từ chối đoạn `.`/`..`, khớp tiền tố thư mục anh em và đoán tên sách/tên tệp. Thiếu quyền không dẫn tới đoán đường dẫn/mở rộng truy cập.

## Parser và giới hạn

Phân giải đường dẫn trong bộ nhớ chính đã cấu hình bằng cách query document ID trực tiếp. Nếu không tìm được, duyệt document con một lần theo đúng display name. Giữ hoa thường và Unicode; không thêm case folding hoặc khớp gần đúng. Kiểm tra containment cho cả ID trực tiếp và ID fallback.

Mỗi lần refresh reading và import statistics lấy snapshot quyền đọc đã lưu một lần cho operation đó. Query/open của provider vẫn kiểm tra truy cập khi quyền bị thu hồi giữa chừng. Mở sách và tính checksum kiểm tra quyền hiện tại độc lập.

| Đầu vào | Giới hạn và hành vi |
| --- | --- |
| Lua | LuaJ 3.0.1, globals mới không thư viện chuẩn, tối đa 8 MiB và 5 triệu lệnh. Hook DebugLib riêng khởi tạo trước khi xóa globals bootstrap; hủy ngắt thực thi. Chỉ DTO Kotlin có kiểu ra khỏi tầng nguồn. |
| Hiện tại/lịch sử | `settings.reader.lua` có `lastfile` đã kiểm tra kiểu; `history.lua` giữ mảng liên tục, tối đa 100.000 khóa. Phân biệt rỗng, thiếu tệp, sai kiểu và lỗi provider. |
| Sidecar | Chính xác `<stem>.sdr/metadata.<extension>.lua` cạnh sách; tiến độ hữu hạn [0, 1] và summary status tùy chọn. Sidecar tập trung/ngoài quyền không khả dụng; không lấy của sách khác. |
| EPUB | Một bản sao tạm riêng tối đa 256 MiB; ZIP tối đa 100.000 mục; XML tối đa 2 MiB, 50.000 node, độ sâu 64. Từ chối DTD/entity ngoài và đường dẫn archive tuyệt đối/thoát gốc. Container/OPF cho tên, tác giả đầu và bìa EPUB2/3. Bìa tối đa 8 MiB, kiểm tra bitmap bounds; hỗ trợ raster Android giải mã được, không SVG. |
| Định dạng sách bổ sung | PDF/CBZ/MOBI/AZW3 dùng một bản sao tạm tối đa 256 MiB; FB2 tối đa 64 MiB. CBZ: 100.000 mục, XML ComicInfo dùng giới hạn XML EPUB. FB2 đọc streaming tối đa 1.000.000 node/độ sâu 64; chỉ giữ bìa base64 được tham chiếu (12 MiB mã hóa, 8 MiB giải mã). MOBI: kiểm tra offset PalmDB theo độ dài tệp, record header tối đa 2 MiB và EXTH tối đa 10.000 mục. Bìa nhúng tối đa 8 MiB; mô tả chuyển thành văn bản thuần tối đa 32 KiB UTF-8. Không giải mã DRM. Xóa tệp tạm khi hoàn tất/lỗi/hủy. |
| Thống kê | Sao chép `settings/statistics.sqlite3` cùng WAL/SHM quan sát được vào thư mục riêng mới, tối đa 256 MiB và một triệu dòng. Mutex bao copy/validate/read. Metadata trước/sau phải khớp; SQLite quick_check và cột `book`/`page_stat_data` bắt buộc phải đạt. Chỉ mở bản sao riêng, read-only. |

Thống kê thử tối đa ba lần khi lỗi truyền tải/SQLite/thay đổi tạm thời, dùng thư mục mới và backoff 100/200ms có thể hủy. Schema chưa hỗ trợ có kết quả riêng. Đóng/xóa stream, cursor, database, thư mục tạm khi thành công, lỗi hoặc hủy. [Cache đọc](reading-cache.md) quy định metadata/thumbnail; [Thư viện](library.md) quy định giới hạn duyệt.

SAF không sao chép nguyên tử khi KOReader ghi. Size/mtime có thể thiếu/không đổi dù đã ghi; phiên hiện tại có thể chưa flush. Bản chụp hợp lệ chỉ là quan sát, không bảo đảm tổng hiện thời nhất quán theo giao dịch.

## Lỗi và trách nhiệm

Kết quả capability và phân loại lỗi dự kiến nằm trong `data/source`, không import repository. `ProbeCapabilities` phân loại lỗi/timestamp; truyền tiếp hủy và lỗi lập trình ngoài dự kiến. Phân giải/parsing ở tầng nguồn. Copy SQLite, kiểm tra integrity, schema và truy vấn tách riêng trong cùng mutex/retry/cleanup. Mỗi `BoundedXmlReader` giữ parser/stack/budget riêng cho từng mục XML.

Query/open provider chuyển `IllegalArgumentException` OEM thành `IOException`, giữ cause. `FileNotFoundException` trực tiếp thành `MissingSourceFile`; xác thực URI, lỗi parser, hủy và quyền giữ cách xử lý riêng. Tệp biến mất không làm sập Trang chủ hoặc xóa cache thành công. Xem [kiểm tra phục hồi](reliability.md).

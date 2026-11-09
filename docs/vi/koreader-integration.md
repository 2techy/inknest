# Kết nối nguồn KOReader

[English](../en/koreader-integration.md) · [Mục lục](README.md)

## Cấu hình và truy cập

Debug và release dùng cùng nguồn thật. Cài đặt → Đọc và dữ liệu chọn package `org.koreader.launcher` đã cài, thư mục dữ liệu KOReader và một hoặc nhiều thư mục sách độc lập qua trình chọn cây tài liệu hệ thống, với quyền đọc được lưu bền. Lựa chọn không hợp lệ hoặc hủy giữ cấu hình đang hoạt động. Quyền không còn dùng được giải phóng; ngắt kết nối xóa khóa nguồn/quyền, giữ ghim, giao diện và trạng thái thiết lập.

Định danh nguồn, package, cây KOReader và các cây sách dùng chung Preferences DataStore hiện có. Room giữ cache đọc. Cả hai build type không có công cụ developer, route probe hay dữ liệu mẫu runtime; chỉ debug bật log StrictMode. Xem [cache đọc](reading-cache.md) về làm mới và mở sách.

Chỉ hỗ trợ bộ nhớ trong chính qua Android ExternalStorageProvider; từ chối provider/volume khác. Document ID được kiểm tra theo volume và từng đoạn đường dẫn; tài liệu con phải nằm trong gốc được cấp quyền. Đường dẫn nguồn chỉ được phân giải trong cây sách hoặc KOReader đã cấu hình. Alias bộ nhớ chính chỉ được chấp nhận sau khi kiểm tra định danh filesystem chuẩn trên thiết bị. Giữ Unicode, hoa/thường và dấu cách. Từ chối đoạn chấm, tiền tố thư mục anh em và đoán bằng tên sách/tên tệp. Thiếu quyền không dẫn tới đoán đường dẫn hay mở rộng truy cập bộ nhớ.

## Parser và giới hạn

| Đầu vào | Giới hạn và hành vi |
| --- | --- |
| Lua | LuaJ 3.0.1, globals mới không có thư viện chuẩn, tối đa 8 MiB và 5 triệu lệnh. Hook lệnh DebugLib riêng được khởi tạo trước khi xóa globals bootstrap; cancellation ngắt thực thi. Chỉ DTO Kotlin có kiểu rời tầng source. |
| Sách hiện tại/lịch sử | `settings.reader.lua` cung cấp `lastfile` có kiểu; `history.lua` giữ thứ tự mảng liên tục, tối đa 100.000 khóa. Dữ liệu rỗng, thiếu tệp, sai kiểu và lỗi provider là kết quả khác nhau. |
| Sidecar | Đúng tệp liền kề `<stem>.sdr/metadata.<extension>.lua`; tiến độ hữu hạn trong [0, 1], trạng thái summary tùy chọn. Không dùng sidecar tập trung/ngoài quyền hoặc thay bằng sidecar sách khác. |
| EPUB | Một bản sao tạm riêng, tối đa 256 MiB; ZIP tối đa 100.000 entry; XML tối đa 2 MiB, 50.000 node, ngân sách độ sâu 64. Từ chối DTD/entity ngoài và đường dẫn archive tuyệt đối/thoát gốc. Container/OPF cung cấp tên, tác giả đầu và tham chiếu bìa EPUB2/3. Dữ liệu bìa tối đa 8 MiB, kiểm tra kích thước bitmap. Hỗ trợ ảnh raster Android giải mã được; chưa hỗ trợ SVG. |
| Thống kê | Sao chép `settings/statistics.sqlite3` và WAL/SHM quan sát được vào thư mục riêng mới, tổng tối đa 256 MiB và một triệu dòng. Mutex bao trùm sao chép/kiểm tra/đọc. Metadata quan sát trước/sau phải khớp; SQLite quick_check và cột bắt buộc trong `book`/`page_stat_data` phải hợp lệ. Chỉ mở bản sao riêng ở chế độ chỉ đọc. |

Thống kê thử lại lỗi truyền tải/SQLite/thay đổi tạm thời tối đa ba lần, mỗi lần dùng thư mục mới và backoff 100/200ms có thể hủy. Schema không hỗ trợ là kết quả riêng. Stream, cursor, database và thư mục tạm được đóng/xóa khi thành công, lỗi hoặc hủy. Lưu metadata/ảnh thu nhỏ được mô tả trong [cache đọc](reading-cache.md); giới hạn duyệt thư mục nằm ở [thư viện](library.md).

SAF không sao chép nguyên tử khi KOReader đang ghi. Size/mtime có thể thiếu hoặc không đổi dù có ghi, KOReader có thể chưa flush phiên hiện tại. Snapshot hợp lệ là một lần quan sát, không bảo đảm tổng đọc mới nhất theo giao dịch.

## Ranh giới lỗi và trách nhiệm

Capability nguồn và phân loại lỗi dự kiến thuộc `data/source`; source không import repository. `ProbeCapabilities` phân loại lỗi nguồn dự kiến và timestamp; cancellation/lỗi lập trình ngoài dự kiến được truyền tiếp. Phân giải chính xác và parser ở tầng source. Sao chép SQLite, kiểm tra toàn vẹn, schema và truy vấn là các bước riêng trong cùng ranh giới mutex/thử lại/dọn dẹp. Mỗi `BoundedXmlReader` sở hữu parser/stack/ngân sách riêng cho một entry XML.

Query/open provider chuyển `IllegalArgumentException` của OEM thành `IOException` và giữ nguyên cause. `FileNotFoundException` trực tiếp thành `MissingSourceFile`; kiểm tra URI, lỗi parser, cancellation và lỗi quyền giữ xử lý riêng. Tệp biến mất không được làm Home crash hoặc xóa cache thành công. Dùng [kiểm tra độ tin cậy](reliability.md) để kiểm tra phục hồi.

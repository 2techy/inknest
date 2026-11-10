# Kết nối nguồn KOReader

[English](../en/koreader-integration.md) · [Mục lục](README.md)

## Cấu hình và quyền truy cập

Debug và release dùng cùng nguồn dữ liệu thật. Trong **Cài đặt → Đọc và dữ liệu**, người dùng chọn ứng dụng `org.koreader.launcher` đã cài, thư mục dữ liệu KOReader và một hoặc nhiều thư mục sách độc lập. Các thư mục được chọn qua trình chọn cây tài liệu của hệ thống, với quyền đọc được lưu để dùng cho những lần sau. Lựa chọn không hợp lệ hoặc hủy thao tác sẽ giữ cấu hình đang dùng. Quyền không còn cần thiết được giải phóng; ngắt kết nối xóa khóa nguồn và quyền truy cập, nhưng giữ ứng dụng đã ghim, giao diện và trạng thái thiết lập ban đầu.

Định danh nguồn, package đã chọn, cây thư mục KOReader và các cây thư mục sách được lưu chung trong Preferences DataStore sẵn có. Room lưu bộ nhớ đệm đọc. Cả hai loại bản dựng đều không có công cụ dành cho lập trình viên, màn hình thăm dò hay chế độ chạy bằng dữ liệu mẫu; chỉ debug bật ghi log StrictMode. Xem [bộ nhớ đệm đọc](reading-cache.md) về làm mới và mở sách.

Chỉ hỗ trợ bộ nhớ trong chính qua ExternalStorageProvider của Android; provider hoặc volume khác sẽ bị từ chối. Document ID được kiểm tra theo volume và từng đoạn đường dẫn, bảo đảm tài liệu con luôn nằm trong thư mục gốc được cấp quyền. Đường dẫn từ nguồn chỉ được phân giải trong các cây thư mục sách hoặc KOReader đã cấu hình.

Bí danh đường dẫn của bộ nhớ chính chỉ được chấp nhận sau khi đối chiếu đường dẫn chuẩn để xác nhận cùng vị trí trên thiết bị. Unicode, chữ hoa/thường và dấu cách được giữ nguyên. Không chấp nhận đoạn đường dẫn `.`/`..`, không coi hai thư mục là một chỉ vì tên có chung tiền tố, và không đoán tệp theo tên sách/tên tệp. Thiếu quyền không được khắc phục bằng cách đoán đường dẫn hoặc mở rộng phạm vi truy cập bộ nhớ.

## Bộ phân tích và giới hạn

| Đầu vào | Giới hạn và cách xử lý |
| --- | --- |
| Lua | LuaJ 3.0.1, mỗi lần chạy có môi trường globals mới không chứa thư viện chuẩn; tối đa 8 MiB và 5 triệu lệnh. Hook đếm lệnh DebugLib riêng được khởi tạo trước khi xóa các globals dùng để khởi động; hủy tác vụ sẽ ngắt thực thi. Chỉ DTO Kotlin có kiểu được trả ra khỏi tầng nguồn. |
| Sách hiện tại/lịch sử | `settings.reader.lua` cung cấp `lastfile` đã kiểm tra kiểu; `history.lua` giữ thứ tự mảng liên tục, tối đa 100.000 khóa. Dữ liệu rỗng, thiếu tệp, sai kiểu và lỗi provider được phân biệt thành các kết quả riêng. |
| Sidecar | Chỉ đọc đúng tệp đi kèm `<stem>.sdr/metadata.<extension>.lua`; tiến độ phải là số hữu hạn trong [0, 1], trạng thái summary có thể không có. Không dùng sidecar ở vị trí tập trung hoặc ngoài quyền truy cập, không thay bằng sidecar của sách khác. |
| EPUB | Một bản sao tạm trong vùng riêng, tối đa 256 MiB; ZIP tối đa 100.000 mục; XML tối đa 2 MiB, 50.000 nút và sâu 64 cấp. Từ chối DTD/thực thể ngoài và đường dẫn tuyệt đối hoặc thoát khỏi gốc tệp nén. Container/OPF cung cấp tên sách, tác giả đầu tiên và tham chiếu bìa EPUB2/3. Dữ liệu bìa tối đa 8 MiB, có kiểm tra kích thước bitmap. Hỗ trợ bìa raster mà Android giải mã được; chưa hỗ trợ SVG. |
| Thống kê | Sao chép `settings/statistics.sqlite3` và WAL/SHM tìm thấy vào thư mục riêng mới; tổng tối đa 256 MiB và một triệu dòng. Mutex bảo vệ toàn bộ quá trình sao chép/kiểm tra/đọc. Thông tin tệp quan sát được trước và sau phải khớp; SQLite quick_check và các cột bắt buộc trong `book`/`page_stat_data` phải hợp lệ. Chỉ mở bản sao riêng, ở chế độ chỉ đọc. |

Khi đọc thống kê gặp lỗi truyền dữ liệu, SQLite hoặc thay đổi tệp có tính tạm thời, ứng dụng thử lại tối đa ba lần, dùng thư mục mới cho mỗi lần và chờ 100/200ms giữa các lần thử. Thời gian chờ có thể bị hủy. Schema không được hỗ trợ là kết quả riêng. Luồng đọc, cursor và cơ sở dữ liệu luôn được đóng, thư mục tạm luôn được xóa, dù tác vụ thành công, thất bại hay bị hủy. Xem cách lưu thông tin sách/ảnh thu nhỏ trong [bộ nhớ đệm đọc](reading-cache.md), và giới hạn duyệt thư mục trong [thư viện](library.md).

SAF không bảo đảm bản sao nhất quán như một giao dịch khi KOReader đang ghi. Kích thước hoặc thời điểm sửa tệp (size/mtime) có thể không được cung cấp, hoặc không đổi dù có dữ liệu mới; KOReader cũng có thể chưa ghi phiên đọc hiện tại xuống đĩa. Vì vậy, bản chụp đã kiểm tra chỉ phản ánh dữ liệu quan sát được, không bảo đảm là tổng đọc mới nhất tại một thời điểm giao dịch.

## Phân loại lỗi và phân chia trách nhiệm

Kết quả kiểm tra khả năng truy cập nguồn và phân loại lỗi dự kiến thuộc `data/source`; mã ở tầng nguồn không import repository. `ProbeCapabilities` phân loại lỗi nguồn dự kiến và xử lý thông tin thời điểm; tín hiệu hủy và lỗi lập trình ngoài dự kiến được truyền tiếp. Việc phân giải chính xác tài liệu và phân tích nội dung nằm ở tầng nguồn.

Sao chép SQLite, kiểm tra tính toàn vẹn, kiểm tra schema và truy vấn là các bước riêng, nhưng dùng chung cơ chế mutex, thử lại và dọn dẹp. Mỗi `BoundedXmlReader` quản lý một parser, ngăn xếp và bộ giới hạn tài nguyên riêng cho từng mục XML.

Khi truy vấn hoặc mở tài liệu qua provider, `IllegalArgumentException` do triển khai OEM phát ra được chuyển thành `IOException`, giữ nguyên nguyên nhân gốc. `FileNotFoundException` trực tiếp được ánh xạ thành `MissingSourceFile`; kiểm tra URI, lỗi phân tích, hủy tác vụ và lỗi quyền có cách xử lý riêng. Tệp biến mất không được làm Trang chủ gặp sự cố hoặc xóa dữ liệu đã lưu thành công. Dùng [kiểm tra độ tin cậy](reliability.md) để kiểm chứng khả năng phục hồi.

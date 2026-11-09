# Thư viện

[English](../en/library.md) · [Mục lục](README.md)

Thư viện lập danh mục tệp sách nhận diện được trong mọi cây thư mục sách SAF đã chọn, quét đệ quy kể cả sách chưa mở bằng KOReader. Lịch sử KOReader bổ sung trạng thái, tiến độ, thời điểm đọc, không quyết định thành viên thư viện. Sách ngoài thư mục đã chọn không xuất hiện chỉ vì có trong lịch sử.

## Luồng dữ liệu và định dạng

`SafBookCatalogSource` liệt kê cây bộ nhớ chính được cấp quyền qua ContentResolver. Mỗi tài liệu phải nằm trong gốc tương ứng; bỏ mục ẩn/thư mục sidecar, ngăn duyệt lặp, hỗ trợ hủy, giới hạn độ sâu 64 và tổng 100.000 entry. Nhận diện EPUB, PDF, DjVu, MOBI/AZW, FB2, văn bản, comic archive và các định dạng tài liệu phổ biến. Không quét thư mục ngoài cấu hình hay yêu cầu quyền bộ nhớ rộng.

| Phần mở rộng nhận diện | Metadata và bìa |
| --- | --- |
| `epub` | Trích xuất tên, tác giả đầu tiên và bìa raster hỗ trợ nếu có. |
| `pdf`, `djvu`, `djv`, `mobi`, `azw`, `azw3`, `fb2`, `txt`, `cbz`, `cbr`, `rtf`, `doc`, `docx`, `html`, `htm`, `chm`, `pdb` | Mục theo tên tệp và bìa thay thế. |

Nhận diện nghĩa là tệp có thể xuất hiện trong Thư viện; mở sách vẫn cần handler KOReader tương thích. Trạng thái/tiến độ có thể được bổ sung từ KOReader độc lập với hỗ trợ metadata nhúng.

`FolderLibraryRepository` quản lý scan chia sẻ cấp ứng dụng và inventory Room theo định danh nguồn/cây sách. Khám phá hoàn chỉnh thay thành viên theo giao dịch; lỗi liệt kê giữ inventory trước. Metadata/bìa được bổ sung theo lô nhỏ, dùng lại fingerprint. Inventory hoàn tất tồn tại sau restart; tác vụ gián đoạn được tiếp tục. Thiết lập đầu, đổi thư mục và reset cache kích hoạt lập danh mục; Làm mới thư viện nhận tệp thêm/xóa. Chuyển tab hay đồng hồ tick không quét.

Bảng thư viện nằm trong database đọc Room schema 6, dùng destructive fallback khi phát triển, chưa có migration. Tùy chọn, ghim và quyền cây tách riêng. Xóa cache đọc xóa inventory/bìa để nguồn hiện tại tạo lại. Đổi nguồn hủy scan cũ; quan sát/ghi kiểm tra thư mục đang hoạt động.

Chỉ EPUB được trích xuất metadata và bìa raster bằng adapter có giới hạn. URI đã kiểm tra có thể đọc trực tiếp sau kiểm tra quyền/containment, tránh tìm lại thư mục cho từng sách. Định dạng khác hoặc thiếu bìa dùng hình sách thay thế, tên tệp và định dạng. Không hiện tác giả/tiến độ thiếu bằng nhãn kỹ thuật lặp lại. Sách mới có trạng thái chưa biết; thiếu lịch sử không chứng minh chưa đọc.

## Màn hình và thao tác

Kệ dùng header/token chung, dải lọc trạng thái ngang gọn, chế độ lưới/danh sách, tìm kiếm và menu sắp xếp/làm mới tĩnh. Bìa/tên sách là nội dung chính, không có đoạn chẩn đoán/số lượng cố định phía trên. Lưới có chú thích gọn; danh sách có thêm metadata. Tìm theo tên/tác giả đã biết, không phân biệt hoa thường. State chỉnh sửa/chọn chữ ở local để lọc nền không đảo ký tự; bỏ IME commit đến muộn sau đóng tìm kiếm. Query/filter/sort/view lưu trong SavedStateHandle; lazy state khôi phục cuộn.

Mặc định sắp thời điểm đọc giảm dần; có tên A–Z và tiến độ giảm dần. Giá trị chưa biết đứng sau đã biết, trường hợp bằng nhau giữ thứ tự/định danh thư mục ổn định. Bộ lọc dùng trạng thái KOReader rõ ràng; 100% không tự thành Đã đọc.

Chạm sách mở tài liệu gốc trong KOReader qua `ReadingOpenRepository` và `ReadingItemDispatcher`. Nhấn giữ mở chi tiết/đường dẫn, có thể mở sách từ hộp thoại. Lưới và danh sách dùng cùng thao tác. Background/HOME đóng chi tiết và yêu cầu mở chờ; giữ query, filter và cuộn. Không cần xác nhận debug hoặc mở khóa theo phiên bản; yêu cầu không hỗ trợ không âm thầm chỉ mở app. Mở sách không tạo lịch sử/tiến độ giả.

## Duyệt tác giả

Thư viện có Tất cả sách và Tác giả. Tác giả hiển thị tên/số lượng, dẫn vào kệ sách. Menu nguồn vẫn có khi chỉ cấu hình một gốc; lọc nguồn đã cấu hình, không phải thư mục con filesystem. Tìm kiếm/trạng thái áp dụng trước khi đếm tác giả; sắp sách áp dụng trong kệ. Chuẩn hóa khoảng trắng và hoa/thường, không bỏ dấu, tách chuỗi creator hoặc đoán từ tên tệp. Thiếu metadata vào Không rõ tác giả, ở cuối danh sách.

Chế độ duyệt/tác giả chọn nằm trong SavedStateHandle, không tạo preference/cột database. Back đóng tìm kiếm trước, sau đó từ kệ tác giả về danh sách tác giả. Thanh điều hướng vẫn hiện. Duyệt dùng dữ liệu cache, không quét nguồn.

Toolbar có tìm kiếm và menu thêm. Menu nhóm duyệt, thư mục, bố cục và sắp xếp thành submenu một cấp; làm mới là thao tác trực tiếp. Danh sách tác giả không hiện tùy chọn bố cục/sắp sách. Submenu có header quay lại và đánh dấu lựa chọn; dismiss/Back đóng popup, background xóa popup. Không có dòng tóm tắt dưới tiêu đề trang. Tìm kiếm dùng chung trường inline với Ứng dụng, placeholder một dòng và vùng chạm 48dp. Thao tác theo ngữ cảnh, không theo ngưỡng chiều rộng.

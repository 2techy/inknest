# Thư viện

[English](../en/library.md) · [Mục lục](README.md)

Duyệt sách được nhận diện trong các thư mục sách đã chọn, gồm thư mục con và sách chưa mở bằng KOReader. Lịch sử bổ sung trạng thái/tiến độ, không quyết định danh sách sách.

## Màn hình và thao tác

- Tìm tên sách/tác giả đã biết, không phân biệt hoa thường. Lọc trạng thái đọc; menu thêm có chế độ duyệt, thư mục nguồn, lưới/danh sách, sắp xếp và làm mới.
- Mặc định xếp lần đọc gần nhất trước; có tên A–Z và tiến độ giảm dần. Giá trị chưa biết đứng sau; khi bằng nhau giữ thứ tự thư mục/định danh ổn định. Trạng thái lấy từ KOReader: 100% không tự thành Đã đọc, không có lịch sử chưa đủ kết luận Chưa đọc.
- Chọn thư mục lọc các nguồn gốc đã cấu hình, không duyệt thư mục con. Làm mới quét mọi nguồn. Nguồn không truy cập được giữ bản chụp cũ, nguồn khác vẫn cập nhật.
- Từ khóa, bộ lọc, thứ tự, bố cục và vị trí cuộn được khôi phục khi quay lại. Trạng thái nhập giữ cục bộ để lọc nền không đảo ký tự; bỏ nội dung IME gửi muộn sau khi đóng tìm kiếm.
- Chạm hoặc nhấn giữ sách để xem chi tiết. Back giữ kệ; quay lại từ KOReader giữ chi tiết. HOME đóng chi tiết và hủy mở sách đang chờ. Chi tiết ẩn điều hướng chính/vuốt tab.

## Duyệt theo tác giả

Menu thêm → Duyệt thư viện đổi giữa Tất cả sách và Tác giả. Danh sách tác giả hiện tên/số sách, chọn để mở kệ. Tìm kiếm/lọc trạng thái áp dụng trước khi đếm; sắp xếp áp dụng trong kệ. Danh sách tác giả ẩn bố cục/sắp xếp dành cho sách.

Tên được chuẩn hóa khoảng trắng/hoa thường, không bỏ dấu, tách chuỗi tác giả hay đoán từ tên tệp. Sách thiếu tác giả nằm cuối dưới Không rõ tác giả. SavedStateHandle khôi phục chế độ/tác giả. Back đóng tìm kiếm trước rồi từ kệ về danh sách tác giả. Duyệt cache không quét nguồn.

## Chi tiết sách

Nút đọc mở tài liệu gốc qua [luồng mở dùng chung](reading-cache.md). Sách mất hoặc nguồn không truy cập được có trạng thái riêng.

- Mô tả EPUB làm sạch thành văn bản, giữ đoạn và giới hạn 32 KiB. Mở rộng tại chỗ từ bốn dòng; thiếu mô tả không chừa khoảng trống.
- Thống kê có tổng thời gian, số trang ghi nhận và số ngày đọc. Mở lịch sử để xem ngày đầu/cuối và tổng ngày mới nhất trước, tải 30 ngày mỗi lần; thông tin tệp có định dạng, đường dẫn và khả năng truy cập.
- Trạng thái mô tả/mục mở rộng và giới hạn lịch sử được khôi phục theo định danh sách.

Thống kê ghép ID KOReader đã nhập bằng partialMD5 tương thích trong đúng nguồn, không ghép tên sách/tác giả. Chỉ lấy mẫu sách đang chọn, tối đa 12 KiB, không sao chép cả tệp. Provider không seek được thì chưa liên kết. Cache checksum vẫn kiểm tra quyền, phạm vi và fingerprint; xóa cache ngăn tác vụ chờ tạo lại hàng đã xóa. Nhiều ID khớp giữ [quy tắc đếm book/ngày/layout](statistics.md). Thống kê không khả dụng khác lịch sử rỗng thành công.

## Luồng dữ liệu

| Phần mở rộng được nhận diện | Thông tin sách và bìa |
| --- | --- |
| `epub` | Tên, tác giả đầu tiên và bìa raster được hỗ trợ, nếu có. |
| `pdf`, `djvu`, `djv`, `mobi`, `azw`, `azw3`, `fb2`, `txt`, `cbz`, `cbr`, `rtf`, `doc`, `docx`, `html`, `htm`, `chm`, `pdb` | Tên tệp và bìa mặc định. |

Nhận diện cho phép liệt kê; mở vẫn cần trình xử lý KOReader tương thích. Thiếu tác giả/tiến độ giữ chưa xác định và không hiện trên kệ.

`SafBookCatalogSource` liệt kê cây bộ nhớ chính đã cấp quyền qua ContentResolver, kiểm tra phạm vi, bỏ thư mục ẩn/sidecar, tránh duyệt lặp và hỗ trợ hủy. Giới hạn độ sâu 64 và 100.000 mục. Không quét ngoài nguồn hoặc yêu cầu quyền bộ nhớ diện rộng.

`FolderLibraryRepository` dùng tác vụ quét cấp ứng dụng, phân vùng Room theo nguồn/cây sách. Liệt kê đầy đủ thay danh sách trong giao dịch; lỗi giữ danh mục cũ. Bổ sung metadata theo lô nhỏ, dùng lại fingerprint đã biết. URI đã xác thực vẫn kiểm tra quyền/phạm vi trước khi đọc trực tiếp. Danh mục hoàn tất giữ qua khởi động; tác vụ gián đoạn tiếp tục.

Thiết lập đầu, đổi thư mục và xóa cache kích hoạt lập danh mục; làm mới thủ công phát hiện tệp thêm/xóa. Đổi tab/đồng hồ không quét. Đổi nguồn hủy tác vụ cũ; theo dõi/ghi kiểm tra thư mục hiện hành. Xóa cache đọc xóa danh mục/bìa và tạo lại nguồn đang dùng. Xem [cache/lưu trữ](reading-cache.md), [giới hạn parser](koreader-integration.md) và [token thiết kế](design-system.md).

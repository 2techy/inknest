# Thư viện

[English](../en/library.md) · [Mục lục](README.md)

Thư viện lập danh mục tất cả tệp sách được nhận diện trong các thư mục sách đã chọn qua SAF, bao gồm thư mục con và cả sách chưa từng mở bằng KOReader. Lịch sử KOReader chỉ bổ sung trạng thái, tiến độ và lần đọc gần nhất; danh sách sách trong Thư viện do các thư mục đã chọn quyết định. Sách nằm ngoài những thư mục này sẽ không xuất hiện dù có trong lịch sử đọc.

## Luồng dữ liệu và định dạng

`SafBookCatalogSource` dùng ContentResolver để liệt kê nội dung trong các cây thư mục được cấp quyền trên bộ nhớ chính. Mỗi tài liệu phải nằm trong thư mục gốc tương ứng. Quá trình duyệt bỏ qua thư mục ẩn và thư mục sidecar, tránh duyệt lặp, hỗ trợ hủy tác vụ, giới hạn độ sâu ở 64 cấp và tổng số mục ở 100.000. Các định dạng được nhận diện gồm EPUB, PDF, DjVu, MOBI/AZW, FB2, văn bản, truyện tranh dạng tệp nén và các định dạng tài liệu phổ biến. Ứng dụng không quét thư mục ngoài cấu hình hoặc yêu cầu quyền truy cập bộ nhớ diện rộng.

| Phần mở rộng được nhận diện | Thông tin sách và bìa |
| --- | --- |
| `epub` | Trích xuất tên sách, tác giả đầu tiên và bìa dạng ảnh raster được hỗ trợ, nếu có. |
| `pdf`, `djvu`, `djv`, `mobi`, `azw`, `azw3`, `fb2`, `txt`, `cbz`, `cbr`, `rtf`, `doc`, `docx`, `html`, `htm`, `chm`, `pdb` | Hiển thị theo tên tệp, dùng bìa mặc định. |

Tệp được nhận diện có thể xuất hiện trong Thư viện, nhưng chỉ mở được khi KOReader có trình xử lý tương thích. Trạng thái và tiến độ vẫn có thể lấy từ KOReader ngay cả khi InkNest không đọc được thông tin nhúng trong tệp.

`FolderLibraryRepository` quản lý tác vụ quét dùng chung trong toàn ứng dụng và danh mục Room được phân vùng theo định danh nguồn/cây thư mục sách. Sau khi liệt kê đầy đủ, danh sách sách được thay thế trong một giao dịch; nếu liệt kê thất bại, danh mục cũ được giữ nguyên. Thông tin sách và bìa được bổ sung theo từng lô nhỏ, tận dụng thông tin nhận diện tệp (fingerprint) đã biết để tránh đọc lại. Danh mục hoàn chỉnh được giữ qua lần khởi động tiếp theo; quá trình bị gián đoạn sẽ được tiếp tục. Thiết lập lần đầu, đổi thư mục và xóa bộ nhớ đệm đều kích hoạt lập danh mục. Làm mới thư viện cập nhật tệp mới thêm hoặc đã xóa; chuyển tab và đồng hồ chạy không gây quét thư mục.

Các bảng thư viện dùng chung cơ sở dữ liệu đọc Room schema 6. Trong giai đoạn phát triển, khi đổi phiên bản mà không có migration, Room được phép xóa và tạo lại bộ nhớ đệm. Tùy chọn, ứng dụng đã ghim và quyền truy cập thư mục được quản lý riêng. Xóa bộ nhớ đệm đọc cũng xóa danh mục/bìa để tạo lại từ thư mục đang dùng. Đổi nguồn hủy tác vụ quét cũ; cả việc theo dõi dữ liệu lẫn ghi dữ liệu đều kiểm tra thư mục đang có hiệu lực.

Chỉ EPUB được trích xuất thông tin và bìa raster, thông qua bộ đọc có giới hạn tài nguyên. URI tài liệu đã xác thực có thể được đọc trực tiếp sau khi kiểm tra quyền và bảo đảm tài liệu vẫn nằm trong cây thư mục được cấp quyền, tránh phải tìm lại từng tệp. Định dạng khác hoặc sách thiếu bìa dùng hình bìa mặc định, tên tệp và định dạng. Nếu thiếu tác giả hoặc tiến độ, kệ sách bỏ qua những thông tin đó thay vì lặp lại nhãn kỹ thuật. Sách mới có trạng thái đọc chưa xác định; không có lịch sử chưa đủ để kết luận là chưa đọc.

## Màn hình và thao tác

Kệ sách dùng phần đầu trang và các giá trị thiết kế chung, có dải lọc trạng thái cuộn ngang, chế độ lưới/danh sách, tìm kiếm và menu sắp xếp/làm mới không có hiệu ứng chuyển động. Bìa và tên sách là nội dung chính. Dạng lưới có chú thích ngắn; dạng danh sách hiển thị thêm thông tin sách.

Tìm kiếm theo tên sách hoặc tác giả đã biết, không phân biệt hoa thường. Trạng thái nhập và chọn văn bản được giữ cục bộ để việc lọc ở nền không làm đảo thứ tự ký tự đang gõ. Nội dung do bộ gõ gửi đến sau khi đã đóng tìm kiếm sẽ bị bỏ qua. Từ khóa, bộ lọc, cách sắp xếp và kiểu hiển thị được lưu trong SavedStateHandle; trạng thái của thành phần lazy giúp khôi phục vị trí cuộn.

Mặc định, sách được xếp theo lần đọc gần nhất, mới trước. Các lựa chọn khác là tên A–Z và tiến độ từ cao xuống thấp. Giá trị chưa biết đứng sau giá trị đã biết; khi bằng nhau, thứ tự thư mục/định danh ổn định được dùng để phân định. Bộ lọc dựa trên trạng thái KOReader ghi rõ; tiến độ 100% không tự động có nghĩa là Đã đọc.

Chạm sách để mở tài liệu gốc trong KOReader đã chọn, thông qua `ReadingOpenRepository` và `ReadingItemDispatcher`. Nhấn giữ để xem thông tin chi tiết và đường dẫn; hộp thoại này cũng có nút mở sách. Dạng lưới và danh sách dùng cùng các thao tác. Khi ứng dụng chuyển xuống nền hoặc người dùng nhấn HOME, hộp thoại chi tiết được đóng và yêu cầu mở đang chờ bị bỏ; từ khóa, bộ lọc và vị trí cuộn vẫn được giữ. Yêu cầu mở không được hỗ trợ sẽ hiển thị lỗi. Lịch sử và tiến độ đọc luôn lấy từ KOReader.

## Duyệt theo tác giả

Thư viện có hai chế độ: Tất cả sách và Tác giả. Chế độ Tác giả hiển thị tên cùng số lượng sách; chọn một tác giả để mở kệ sách tương ứng. Menu thư mục nguồn vẫn có khi chỉ cấu hình một thư mục gốc. Menu này lọc theo các nguồn đã chọn, không duyệt từng thư mục con trong hệ thống tệp. Tìm kiếm và lọc trạng thái được áp dụng trước khi đếm sách theo tác giả; sắp xếp sách được áp dụng trong từng kệ.

Tên tác giả được chuẩn hóa khoảng trắng và chữ hoa/thường, nhưng không bỏ dấu, tách chuỗi tên tác giả hoặc đoán từ tên tệp. Sách thiếu thông tin tác giả được gom vào Không rõ tác giả, nằm cuối danh sách.

Chế độ duyệt và tác giả đang chọn được khôi phục qua SavedStateHandle, không tạo thêm tùy chọn lưu lâu dài hoặc cột cơ sở dữ liệu. Nút Quay lại đóng tìm kiếm trước, sau đó đưa người dùng từ kệ của một tác giả về danh sách tác giả. Thanh điều hướng luôn hiện. Việc duyệt dùng danh mục đã lưu trong bộ nhớ đệm và không quét lại nguồn.

Thanh công cụ có nút tìm kiếm và menu thêm. Menu thêm chia các lựa chọn duyệt thư viện, thư mục, bố cục và sắp xếp thành menu con một cấp; làm mới là thao tác trực tiếp. Danh sách tác giả không hiển thị lựa chọn bố cục/sắp xếp dành cho sách. Menu con có mũi tên quay lại ở đầu và đánh dấu lựa chọn hiện tại. Thao tác đóng hoặc Quay lại sẽ đóng menu; chuyển ứng dụng xuống nền cũng xóa menu đang mở. Không hiển thị dòng tóm tắt dưới tiêu đề trang.

Thư viện và Ứng dụng dùng chung ô tìm kiếm ngay trên thanh công cụ, với gợi ý một dòng và vùng chạm 48dp. Các thao tác hiện ra theo ngữ cảnh, không bị giới hạn bởi ngưỡng chiều rộng màn hình.

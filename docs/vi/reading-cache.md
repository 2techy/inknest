# Bộ nhớ đệm đọc và mở sách

[English](../en/reading-cache.md) · [Mục lục](README.md)

Contract sách hiện tại, lịch sử, làm mới và mở sách của `CachedReadingRepository`, dùng chung debug/release. Trang chủ theo dõi Room và `ReadingSyncStatus` độc lập; ghim, đồng hồ và thống kê tách riêng. Metadata thiếu giữ chưa xác định; mở sách không tạo tiến độ/thời điểm đọc giả.

## Ranh giới dữ liệu và nguồn

- `KoreaderCurrentSource` ưu tiên `lastfile` hợp lệ, dự phòng bằng đường dẫn hợp lệ đầu tiên theo thứ tự lịch sử. Nguồn thiếu/hỏng khác dữ liệu rỗng thành công. Sách hiện tại mất/ngoài quyền giữ định danh/tên tệp, kèm cảnh báo truy cập. Không suy sách hiện tại từ thống kê hoặc tên sách/tên tệp.
- Room schema 8 xuất tại `app/schemas`; giai đoạn phát triển cho xóa/tạo lại cache, chưa bảo đảm nâng cấp ổn định. DataStore giữ riêng tùy chọn, ghim và cấu hình SAF. Làm mới foreground dựng lại cache.
- `reading_items` lưu định danh nguồn/đường dẫn, truy cập, metadata, tiến độ/trạng thái, thời điểm đọc từ nguồn và thứ tự lịch sử nullable. `reading_sources` lưu con trỏ hiện tại, mốc thử/thành công, lỗi và số mục lịch sử bỏ qua.
- Định danh miền giữ qua bí danh bộ nhớ chính. Hàng cache phân vùng thêm bằng fingerprint cây KOReader/các cây sách đã sắp xếp. Đọc/kiểm tra/ghi cache cấu hình ứng viên trước khi kích hoạt DataStore; lỗi giữ cấu hình/cache hiện hành. Phân vùng ứng viên ẩn bị xóa khi xóa cache/ngắt kết nối.
- DataStore/Room không có giao dịch chung; thứ tự staging xử lý khoảng dừng giữa hai bước. Dọn quyền dựa trên cấu hình thực đã lưu, kể cả sau hủy bên gọi.

## Metadata, bìa và lịch sử

Lịch sử giữ thứ tự mảng; đường dẫn chuẩn trùng giữ mục đầu, cùng tên khác đường dẫn vẫn riêng. Kiểm tra giây nguồn trước khi đổi an toàn sang mili giây epoch. Bỏ từng mục sai kèm cảnh báo/số lượng; tài liệu hỏng giữ danh sách cũ, chọn dự phòng thất bại không phát rỗng giả.

Ghi danh sách mới, con trỏ hiện tại và thứ tự trong một giao dịch Room. Sách không phân giải được vẫn theo dõi với trạng thái truy cập. `ReadingRepository.trackedItems` bổ sung trạng thái; danh sách [Thư viện](library.md) lấy từ danh mục thư mục. Trang chủ không có mục Gần đây.

Sidecar đọc lại khi làm mới: `percent_finished` hữu hạn trong [0, 1] có thể giảm. Trạng thái rõ `reading`, `complete`, `abandoned` được ánh xạ; 100% riêng lẻ không là Đã đọc.

Tên/tác giả EPUB giữ khi thiếu/không hỗ trợ bìa. [Trích xuất có giới hạn](koreader-integration.md) tạo PNG riêng, cạnh dài tối đa 480px, ghi tạm/đổi tên nguyên tử. Khóa gồm nguồn/phân vùng, đường dẫn chuẩn và size/mtime. Fingerprint đã biết không đổi dùng lại metadata/bìa; chưa biết thì đọc lại, tệp đổi dùng khóa mới. Dọn ảnh không còn tham chiếu sau 24 giờ để ứng viên đồng thời ghi tạm an toàn. Xóa cache/ngắt kết nối xóa bìa. Không giữ bitmap trong ViewModel/saved state; cache giải mã theo [hệ thống thiết kế](design-system.md).

## Vị trí trang đã lưu

Trang chủ tính từ cùng sidecar: `round(percent_finished × doc_pages)`, dùng phần trăm gốc, không dùng số nguyên hiển thị. Tiến độ 0..1 và tổng nguyên dương cho kết quả 0..tổng. Có `last_xpointer` không cần `last_page`; tổng thiếu/sai giữ tiến độ/trạng thái hợp lệ, thiếu tiến độ thì vị trí chưa biết.

Đây là vị trí suy từ layout KOReader đã lưu, có thể đổi theo cài đặt đọc, không phải nhãn trang nhà xuất bản. Không ghép từ thống kê/tên sách.

## Mở trình đọc

ViewModel Trang chủ/Thư viện giữ trạng thái mở. `ReadingOpenRepository` phân giải lại tài liệu gốc và kiểm tra nguồn. Route RESUMED nhận mỗi yêu cầu một lần, gửi ACTION_VIEW với URI/MIME, ClipData và quyền đọc qua `ReadingItemDispatcher`. Tìm package/handler ngoài main; gửi dùng main được inject. Chuẩn bị không phân tích/sao chép cả sách. Pause/HOME/disposal hủy chờ; không lưu yêu cầu để phát lại.

Lỗi phân biệt thiếu ứng dụng/tệp/quyền, handler chưa hỗ trợ và nguồn đổi. Lỗi tệp cho làm mới; lỗi quyền/cấu hình mở Cài đặt. `Dispatched` chỉ là Android nhận intent, chưa xác nhận trình đọc thành công. Tiến độ/lịch sử/sách hiện tại vẫn theo nguồn.

Trên Tango 2, descriptor ExternalStorageProvider có thể được KOReader phân giải thành `/mnt/user/...` không mở được. Sau xác thực SAF, tài liệu bộ nhớ chính chỉ dùng `content://org.koreader.launcher.provider/external/` khi authority thuộc trình đọc đã chọn. Nó mở cùng tệp qua `/storage/emulated/0/...`, không sao chép/nhập lại. Data/ClipData nhất quán; bỏ cờ quyền ngoài vì bên nhận sở hữu provider. Trường hợp khác giữ intent SAF gốc; không dùng `file://`.

Từ chối ID sai/thoát thư mục và volume khác. Kiểm thử gồm tên mã hóa, đúng đường dẫn/MIME, chủ provider và quyền. Xem [nghiệm thu thiết bị](reliability.md) để kiểm tra mở/quay lại thực tế.

## Làm mới và lỗi

Trách nhiệm foreground theo [kiến trúc](architecture.md). Trang chủ/Cài đặt dùng chung tác vụ repository cấp ứng dụng; bên gọi đồng thời chờ cùng job, hủy một bên không hủy bên khác. Đổi nguồn hủy job cũ; kiểm tra khóa hiện hành/tín hiệu hủy trước ghi. Dọn muộn không xóa trạng thái chạy của job khác.

- Lỗi nguồn cập nhật metadata đồng bộ, không xóa con trỏ thành công gần nhất. Trang chủ giữ cache, kèm báo cũ/lỗi. Kiểm tra định danh cùng observation để dữ liệu/trạng thái nguồn cũ không hiện dưới cấu hình mới.
- Báo lỗi ghi cache dự kiến kể cả khi ghi bản lỗi cũng thất bại.
- Ngắt kết nối xóa hàng/khóa/quyền nguồn, giữ ghim/tùy chọn. Xóa cache xóa dữ liệu đọc, danh mục và bìa rồi nạp lại nguồn đã chọn; cấu hình chỉ có sách cũng dựng lại. Dung lượng cache gồm ảnh thu nhỏ.
- Thống kê có định danh/repository riêng trong DB chung. Xóa cache/ngắt kết nối gồm sự kiện, tổng, token nhập và timestamp. Xem [thống kê](statistics.md). Đổi tab/đồng hồ không phân tích nguồn.

# Cache đọc và mở sách

[English](../en/reading-cache.md) · [Mục lục](README.md)

Debug và release dùng `CachedReadingRepository`. Trang chủ quan sát sách đang đọc từ Room và `ReadingSyncStatus` độc lập; ghim, đồng hồ và thống kê tách riêng. Không có dữ liệu mẫu runtime. Sách hiện tại và lịch sử theo dõi chia sẻ cache/refresh nguồn. Sidecar bổ sung tiến độ/trạng thái; EPUB bổ sung tên/tác giả/bìa. Trường thiếu hoặc không hỗ trợ giữ trạng thái chưa biết. Mở sách qua `ReadingOpenRepository` / `ReadingItemDispatcher` không tạo tiến độ hoặc thời điểm đọc giả.

## Ranh giới dữ liệu và nguồn

`KoreaderCurrentSource` dùng lại Lua/SAF có giới hạn. `lastfile` hợp lệ được ưu tiên; settings thiếu/sai dùng đường dẫn lịch sử hợp lệ đầu tiên theo thứ tự mảng. Thiếu cả hai nguồn hoặc dữ liệu hỏng không được coi là nguồn rỗng thành công. Sách hiện tại mất tệp/ngoài quyền vẫn giữ định danh và tên tệp kèm cảnh báo khả dụng. Không suy sách hiện tại từ thống kê hoặc trùng tên.

Room schema 6 được xuất trong `app/schemas`. Trong giai đoạn phát triển, đổi schema cần tăng version và cho Room tạo lại cache khi không có migration. Tùy chọn, ghim và cấu hình SAF trong DataStore riêng không bị reset cache. Foreground refresh tạo lại dữ liệu từ nguồn. `reading_items` giữ định danh nguồn/đường dẫn, khả dụng, metadata, tiến độ/trạng thái, thời điểm đọc và thứ tự lịch sử nullable. `reading_sources` giữ con trỏ sách hiện tại, thời điểm thử/thành công, lỗi và số mục lịch sử bỏ qua. Định danh miền nguồn/đường dẫn ổn định qua alias bộ nhớ chính; hàng cache thêm fingerprint cấu hình cây để nguồn ứng viên không ghi đè phân vùng đang hoạt động trước khi DataStore chuyển sang nó.

Đọc/kiểm tra ứng viên và ghi cache diễn ra trước kích hoạt DataStore. Ứng viên thất bại giữ cấu hình/cache cũ. Phân vùng chưa kích hoạt không hiển thị, được xóa khi clear/disconnect. Dọn quyền kiểm tra cấu hình thực sự đã lưu, kể cả khi caller hủy. Không có giao dịch chung DataStore–Room; thứ tự staging/phân vùng xử lý khoảng trống khi process dừng.

## Metadata, bìa và lịch sử

Lịch sử theo thứ tự mảng nguồn. Đường dẫn chuẩn trùng giữ lần đầu; cùng tên nhưng khác đường dẫn vẫn là sách khác. Timestamp nguồn là giây hợp lệ, đổi sang epoch milliseconds không tràn. Mục sai riêng lẻ bị bỏ qua kèm cảnh báo/số lượng; cả cấu trúc lịch sử hỏng giữ danh sách cũ. Chọn fallback thất bại không được phát trạng thái rỗng giả. Thay danh sách, con trỏ hiện tại và thứ tự lịch sử trong một giao dịch Room. Sách không phân giải được vẫn được theo dõi với trạng thái khả dụng. `ReadingRepository.trackedItems` cung cấp phân vùng hiện tại để bổ sung trạng thái; thành viên Thư viện do danh mục thư mục riêng quyết định. Trang chủ không thêm mục gần đây.

Đọc lại sidecar khi refresh nên `percent_finished` thấp hơn thay được giá trị cũ cao hơn. Chỉ nhận giá trị hữu hạn [0, 1]. Trạng thái rõ ràng `reading`, `complete`, `abandoned` ánh xạ sang trạng thái đã biết; 100% không tự nghĩa là Đã đọc.

EPUB2/3 dùng adapter ZIP/XML có giới hạn. Tên/tác giả hợp lệ vẫn giữ khi thiếu/không hỗ trợ bìa. Ảnh PNG riêng có cạnh dài tối đa 480px, ghi qua tệp tạm rồi atomic rename. Khóa gồm nguồn/phân vùng, đường dẫn chuẩn và size/mtime nguồn. Fingerprint đã biết không đổi dùng lại metadata/bìa; fingerprint chưa biết được đọc lại, tệp đổi dùng khóa mới. Bìa không còn tham chiếu được dọn sau 24 giờ để ứng viên đồng thời có thể staging an toàn. Clear/disconnect xóa cả cache bìa. Không lưu bitmap trong ViewModel/saved state.

## Mở trình đọc

ViewModel Trang chủ/Thư viện quản lý state mở. `ReadingOpenRepository` phân giải lại tài liệu gốc, kiểm tra nguồn đã chọn. Route RESUMED nhận từng yêu cầu một lần, dùng `ReadingItemDispatcher` gửi ACTION_VIEW với URI/MIME, ClipData và quyền đọc. Chuẩn bị/kiểm tra trình xử lý chạy ngoài main; gửi intent trên main dispatcher inject được. Mở sách không parse hay sao chép EPUB. Pause/disposal bỏ công việc chờ; yêu cầu không lưu bền.

Luồng mở phân biệt các trường hợp thiếu ứng dụng, tệp hoặc quyền; trình xử lý không hỗ trợ; và nguồn đã thay đổi. Lỗi tệp đề xuất làm mới; lỗi quyền/cấu hình đề xuất Cài đặt. `Dispatched` nghĩa là Android nhận intent, không phải KOReader xác nhận sách. Tiến độ/lịch sử/sách hiện tại vẫn lấy từ nguồn.

Trên một số thiết bị như Tango 2, descriptor ExternalStorageProvider có thể thành `/mnt/user/...` mà Lua KOReader không mở lại được. Sau kiểm tra SAF, tài liệu bộ nhớ chính dùng URI `content://org.koreader.launcher.provider/external/` của KOReader nếu authority thuộc trình đọc đã chọn. Provider ánh xạ cùng tệp tới `/storage/emulated/0/...`; không sao chép/import. Data và ClipData nhất quán; bỏ cờ cấp quyền URI vì app nhận sở hữu provider. Nếu không áp dụng được thì giữ intent SAF gốc. Không dùng cách lách `file://`.

Ánh xạ từ chối ID sai/traversal và volume khác. Test hồi quy bao gồm tên mã hóa, đường dẫn/MIME chính xác, sai/thiếu chủ sở hữu provider và xử lý quyền. Debug và release dùng cùng luồng mở.

## Làm mới và lỗi

Effect STARTED theo lifecycle ủy quyền qua AppViewModel và coordinator chung. Làm mới từ Trang chủ/Cài đặt dùng cùng job repository cấp ứng dụng. Caller đồng thời chờ cùng job; hủy chờ không hủy caller khác. Đổi nguồn hủy job cũ; kiểm tra khóa đang hoạt động và cancellation trước khi ghi. Dọn dẹp muộn không xóa state đang chạy của job khác.

Lỗi nguồn cập nhật metadata đồng bộ, không xóa con trỏ sách thành công trước đó. Trang chủ giữ thẻ đã lưu và hiện cảnh báo cũ/lỗi gọn. Quan sát định danh nguồn và cache cùng nhau ngăn dữ liệu nguồn trước xuất hiện dưới cấu hình mới. Lỗi ghi cache dự kiến vẫn hiển thị kể cả ghi bản ghi lỗi cũng thất bại.

Ngắt kết nối xóa hàng đọc/khóa nguồn/quyền, giữ ghim/tùy chọn. Xóa cache loại dữ liệu đọc, danh mục sách và bìa rồi tải lại nguồn; cấu hình chỉ có sách cũng tạo lại danh mục được. Dung lượng cache gồm ảnh thu nhỏ. Chuyển tab hoặc đồng hồ tick không parse nguồn.

Thống kê dùng chung schema 6 nhưng có định danh nguồn/repository riêng. Clear/disconnect gồm raw event, tổng ngày, import token và thời điểm snapshot. Xem [thống kê](statistics.md) và [kiểm tra độ tin cậy](reliability.md).

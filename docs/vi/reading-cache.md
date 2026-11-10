# Bộ nhớ đệm đọc và mở sách

[English](../en/reading-cache.md) · [Mục lục](README.md)

Cả bản debug và release đều dùng `CachedReadingRepository`. Trang chủ theo dõi sách đang đọc được lưu trong Room và trạng thái `ReadingSyncStatus` riêng; ứng dụng đã ghim, đồng hồ và thống kê được theo dõi độc lập. Ứng dụng không có chế độ chạy bằng dữ liệu mẫu. Sách đang đọc và lịch sử dùng chung bộ nhớ đệm, cùng được làm mới từ nguồn. Tệp sidecar bổ sung tiến độ/trạng thái; EPUB bổ sung tên sách, tác giả và bìa. Trường thiếu hoặc không được hỗ trợ giữ trạng thái chưa xác định. Mở sách qua `ReadingOpenRepository` / `ReadingItemDispatcher` không tự tạo tiến độ hoặc thời điểm đọc giả.

## Ranh giới dữ liệu và nguồn

`KoreaderCurrentSource` dùng lại các bộ đọc Lua/SAF có giới hạn tài nguyên. Giá trị `lastfile` hợp lệ được ưu tiên. Nếu cấu hình sách hiện tại thiếu hoặc sai, ứng dụng chọn đường dẫn hợp lệ đầu tiên theo thứ tự mảng lịch sử. Thiếu cả hai nguồn hoặc dữ liệu bị hỏng phải được phân biệt với trường hợp đọc thành công nhưng không có sách. Nếu sách hiện tại bị mất tệp hoặc nằm ngoài phạm vi được cấp quyền, ứng dụng vẫn giữ định danh và tên tệp, kèm cảnh báo không thể truy cập. Không suy đoán sách hiện tại từ thống kê hoặc từ tên sách/tên tệp trùng nhau.

Room schema 6 được xuất trong `app/schemas`. Trong giai đoạn phát triển, thay đổi schema phải tăng phiên bản Room; nếu không có migration, Room được phép xóa và tạo lại cơ sở dữ liệu đọc. Tùy chọn, ứng dụng đã ghim và cấu hình SAF nằm riêng trong DataStore nên không bị ảnh hưởng. Khi ứng dụng trở lại hoạt động, việc làm mới sẽ tạo lại dữ liệu đọc từ nguồn đã cấu hình.

`reading_items` lưu định danh nguồn/đường dẫn, khả năng truy cập, thông tin sách, tiến độ/trạng thái, thời điểm đọc từ nguồn và thứ tự lịch sử có thể để trống. `reading_sources` lưu con trỏ tới sách hiện tại, thời điểm thử cập nhật/cập nhật thành công, lỗi và số mục lịch sử bị bỏ qua. Định danh nguồn/đường dẫn ở tầng mô hình không đổi khi dùng các bí danh đường dẫn của cùng bộ nhớ chính. Mỗi hàng trong bộ nhớ đệm còn có fingerprint của cây KOReader và danh sách cây sách đã sắp xếp, để lựa chọn thư mục mới chưa được kích hoạt không ghi đè phân vùng đang dùng.

Cấu hình mới được đọc, kiểm tra và ghi vào bộ nhớ đệm trước khi được kích hoạt trong DataStore. Nếu thất bại, cấu hình và dữ liệu cũ vẫn được giữ. Phân vùng chưa kích hoạt không hiển thị và sẽ bị xóa khi xóa bộ nhớ đệm hoặc ngắt kết nối. Việc thu hồi quyền không còn dùng luôn dựa trên cấu hình thực sự đã lưu, kể cả khi bên gọi hủy tác vụ. DataStore và Room không có giao dịch chung; thứ tự ghi tạm và phân vùng dữ liệu giúp xử lý trường hợp tiến trình dừng giữa hai bước.

## Thông tin sách, bìa và lịch sử đọc

Lịch sử giữ thứ tự mảng từ nguồn. Các đường dẫn chuẩn trùng nhau chỉ giữ lần xuất hiện đầu tiên; sách cùng tên ở đường dẫn khác nhau vẫn là hai sách riêng. Thời điểm từ nguồn phải là giá trị giây hợp lệ và được đổi sang mili giây tính từ epoch mà không tràn số. Từng mục sai được bỏ qua kèm cảnh báo và số lượng. Nếu toàn bộ cấu trúc lịch sử bị hỏng, danh sách cũ được giữ nguyên; việc chọn sách dự phòng thất bại không được phát trạng thái rỗng giả.

Danh sách mới, con trỏ sách hiện tại và thứ tự lịch sử được ghi trong cùng một giao dịch Room. Sách bị mất hoặc không phân giải được đường dẫn vẫn được theo dõi, kèm trạng thái truy cập. `ReadingRepository.trackedItems` cung cấp dữ liệu trong phân vùng đang dùng để bổ sung trạng thái đọc. Danh sách sách trong Thư viện do danh mục thư mục riêng quyết định; Trang chủ không thêm mục đọc gần đây.

Tệp sidecar được đọc lại khi làm mới, nên `percent_finished` thấp hơn có thể thay thế giá trị cao hơn đã lưu. Chỉ chấp nhận số hữu hạn trong [0, 1]. Các trạng thái được ghi rõ là `reading`, `complete`, `abandoned` được ánh xạ sang trạng thái tương ứng; tiến độ 100% không tự động có nghĩa là Đã đọc.

EPUB2/3 được trích xuất qua bộ đọc ZIP/XML có giới hạn tài nguyên. Tên sách và tác giả hợp lệ vẫn được giữ nếu bìa thiếu hoặc không được hỗ trợ. Ảnh thu nhỏ PNG được lưu riêng trong vùng dữ liệu ứng dụng, với cạnh dài nhất tối đa 480px; ảnh được ghi vào tệp tạm rồi đổi tên bằng thao tác nguyên tử.

Khóa ảnh gồm nguồn/phân vùng, đường dẫn chuẩn và kích thước/thời điểm sửa tệp nguồn (size/mtime). Nếu fingerprint đã biết không đổi, ứng dụng dùng lại thông tin sách và bìa; nếu chưa biết fingerprint thì đọc lại. Tệp thay đổi sẽ dùng khóa mới. Ảnh cũ không còn được tham chiếu sẽ được dọn sau 24 giờ, để các tác vụ đọc cấu hình mới chạy đồng thời có thể ghi tạm an toàn. Xóa bộ nhớ đệm hoặc ngắt kết nối cũng xóa bìa. Không lưu bitmap trong ViewModel hoặc trạng thái được khôi phục.

## Mở trình đọc

ViewModel của Trang chủ và Thư viện quản lý trạng thái mở sách. `ReadingOpenRepository` phân giải lại tài liệu gốc và kiểm tra nguồn đã chọn. Route chỉ tiếp nhận yêu cầu khi ở trạng thái RESUMED, mỗi yêu cầu đúng một lần, rồi dùng `ReadingItemDispatcher` gửi ACTION_VIEW với URI/MIME, ClipData và quyền đọc. Việc tìm ứng dụng/trình xử lý intent chạy ngoài luồng chính; gửi intent chạy trên main dispatcher được truyền vào. Mở sách không phân tích hoặc sao chép EPUB. Khi Route tạm dừng hoặc bị loại khỏi composition, công việc đang chờ bị bỏ; yêu cầu không được lưu để thực hiện lại về sau.

Luồng mở phân biệt rõ: thiếu ứng dụng, thiếu tệp, mất quyền, không có trình xử lý tương thích hoặc nguồn đã thay đổi. Lỗi tệp gợi ý làm mới; lỗi quyền/cấu hình gợi ý mở Cài đặt. `Dispatched` chỉ có nghĩa là Android đã nhận intent, chưa phải xác nhận KOReader đã mở sách. Tiến độ, lịch sử và sách hiện tại vẫn được cập nhật từ nguồn.

Trên một số thiết bị, đã xác nhận ở Tango 2, mô tả tệp từ ExternalStorageProvider có thể được KOReader phân giải thành `/mnt/user/...`, khiến mã Lua của trình đọc không mở lại được. Sau khi kiểm tra nguồn SAF, tài liệu trên bộ nhớ chính sẽ dùng URI `content://org.koreader.launcher.provider/external/` của KOReader nếu authority đó thuộc trình đọc đã chọn. Provider này ánh xạ tới cùng tệp qua `/storage/emulated/0/...`; không sao chép hay nhập lại sách. Data và ClipData dùng URI nhất quán; các cờ cấp quyền URI cho ứng dụng khác được bỏ vì bên nhận sở hữu provider. Nếu không dùng được cách này, ứng dụng giữ intent SAF gốc. Không dùng `file://` để vượt cơ chế bảo vệ URI.

Việc ánh xạ từ chối ID sai, đường dẫn thoát thư mục và volume khác. Kiểm thử hồi quy bao gồm tên được mã hóa, giữ đúng đường dẫn/MIME, kiểm tra chủ sở hữu provider và xử lý quyền. Debug và release dùng cùng luồng mở.

## Làm mới và xử lý lỗi

Effect theo vòng đời ở trạng thái STARTED chuyển việc cập nhật qua AppViewModel và bộ điều phối làm mới chung. Yêu cầu làm mới từ Trang chủ/Cài đặt dùng cùng tác vụ repository cấp ứng dụng. Các bên gọi đồng thời chờ chung tác vụ đang chạy; một bên hủy chờ không làm hủy yêu cầu của bên khác. Đổi nguồn hủy tác vụ cũ. Trước khi ghi, repository kiểm tra khóa nguồn đang có hiệu lực và trạng thái hủy coroutine. Việc dọn dẹp của tác vụ cũ kết thúc muộn không được xóa trạng thái đang chạy của tác vụ khác.

Lỗi nguồn cập nhật thông tin đồng bộ nhưng không xóa con trỏ sách từ lần đọc thành công gần nhất. Trang chủ giữ thẻ sách đã lưu, kèm thông báo ngắn về dữ liệu cũ hoặc lỗi. Định danh nguồn và dữ liệu đệm được kiểm tra cùng nhau để dữ liệu/trạng thái của nguồn trước không xuất hiện dưới cấu hình mới. Lỗi ghi bộ nhớ đệm dự kiến vẫn phải hiển thị, kể cả khi không thể lưu bản ghi mô tả lỗi đó.

Ngắt kết nối xóa dữ liệu đọc, khóa nguồn và quyền truy cập, giữ ứng dụng đã ghim cùng tùy chọn. Xóa bộ nhớ đệm xóa dữ liệu đọc, danh mục sách và bìa rồi tải lại các nguồn đã cấu hình. Cấu hình chỉ có thư mục sách cũng có thể xóa và tạo lại danh mục. Dung lượng bộ nhớ đệm bao gồm ảnh thu nhỏ. Chuyển tab hoặc đồng hồ chạy không kích hoạt phân tích nguồn.

Thống kê dùng chung schema 6 nhưng có định danh nguồn và repository riêng. Xóa bộ nhớ đệm/ngắt kết nối bao gồm sự kiện gốc, tổng theo ngày, token nhập dữ liệu và thời điểm chụp dữ liệu. Xem [thống kê](statistics.md) và [kiểm tra độ tin cậy](reliability.md).

# Thống kê đọc

[English](../en/statistics.md) · [Mục lục](README.md)

Tính năng Thống kê đọc dữ liệu từ `settings/statistics.sqlite3` trong cây thư mục KOReader đã chọn qua SAF. Danh mục sách và lịch sử đọc không cung cấp thời lượng hay số trang. Nếu thiếu cơ sở dữ liệu, mất quyền truy cập hoặc schema không được hỗ trợ, thống kê sẽ ở trạng thái không khả dụng. Chỉ khi đọc thành công một bản chụp rỗng mới có thể kết luận chưa ghi nhận hoạt động đọc. Ứng dụng không dùng dữ liệu giả khi chạy.

## Màn hình và đơn vị

Lựa chọn Thời gian/Trang áp dụng đồng thời cho Hôm nay, trung bình ngày, tiêu chí tính chuỗi ngày đọc, tổng theo kỳ, sắc độ lịch hoạt động và biểu đồ. Hôm nay chỉ hiện đơn vị đang chọn. Các phần trên màn hình lần lượt là Hôm nay, trung bình ngày/chuỗi ngày đọc, xu hướng, lịch hoạt động và tổng theo kỳ.

Trung bình ngày được tính trên 30 ngày lịch gần nhất, gồm hôm nay và cả những ngày không đọc. Nếu hôm nay có đọc, chuỗi ngày đọc được tính đến hôm nay. Nếu chưa đọc hôm nay, chuỗi kết thúc ở hôm qua vẫn được giữ cho tới hết ngày. Thiếu dữ liệu trang có nghĩa là chưa xác định, không phải 0.

Tổng theo kỳ gồm tuần này tính từ thứ Hai, tháng này, năm nay và toàn bộ thời gian. Biểu đồ có các mức: Ngày (30 ngày gần nhất), Tuần (12 tuần bắt đầu từ thứ Hai, gồm tuần hiện tại chưa kết thúc), Tháng (12 tháng gồm tháng hiện tại), Năm (mỗi năm từ năm đầu có dữ liệu). Khoảng không có dữ liệu được điền 0. Mọi mức đều cộng từ cùng các tổng theo ngày đã lưu, không đọc lại nguồn. Đổi mức thời gian của biểu đồ không thay đổi tổng theo kỳ, trung bình, chuỗi ngày đọc hay lịch hoạt động; đơn vị đã chọn áp dụng cho mọi mức.

Lịch hoạt động dạng bản đồ nhiệt trải hết chiều rộng nội dung, tự điều chỉnh từ 16 đến 52 cột tuần; mỗi tuần gồm thứ Hai đến Chủ nhật. Với chiều rộng tương đương Tango, lịch hiển thị khoảng 26 tuần. Lề dành cho nhãn thứ rộng 48dp và tăng theo cỡ chữ. Ô vuông có cạnh tối đa 10dp, cách nhau khoảng 3dp theo chiều ngang và 2dp theo chiều dọc. Lịch giữ nhãn tháng/thứ, không có chú thích số tuần hoặc thang chú giải ít/nhiều. Ngày tương lai không hiển thị; ngày chưa xác định có dấu gạch chéo. Ô không có hoạt động có viền nhạt 0.75dp; ô có hoạt động dùng màu xám đặc, bo góc 2dp.

Sắc độ được tính tương đối từ các giá trị dương đã biết trong khoảng đang hiển thị, riêng cho thời gian và số trang. Các ngưỡng phân vị dưới 25/50/75 chia hoạt động thành bốn mức xám. Ngày có giá trị 0 hoặc chưa xác định không ảnh hưởng ngưỡng. Khi tất cả giá trị dương bằng nhau, kể cả chỉ có một ngày đọc, ứng dụng dùng mức xám 3 để dễ nhìn. Màu ô biểu thị mức hoạt động tương đối, không tương ứng với ngưỡng phút/trang cố định và không thay đổi cách tính tổng.

Bộ lọc Thư viện và tab chọn đơn vị Thống kê dùng chung ChoiceTab: chữ có độ đậm medium, cỡ 12sp và chiều cao dòng 16sp, phần hiển thị cao tối thiểu 32dp, vùng chạm tối thiểu 48dp. Tab chọn mức thời gian dùng cùng kiểu chữ gọn. Chữ theo cỡ hệ thống; trang cuộn dọc, dải tab cuộn ngang khi không đủ chỗ. Chiều rộng cột biểu đồ được giới hạn để khi ít dữ liệu, cột không thành khối đen quá lớn.

## Quy tắc đếm trang

Mỗi dòng có thời lượng dương hợp lệ đóng góp toàn bộ số giây đọc. Trang được nhận diện bằng bộ `(book ID, page, total_pages layout)` từ nguồn và loại trùng trong từng ngày theo giờ địa phương. Đọc lại cùng trang trong cùng ngày không tăng số trang nhưng vẫn cộng thời gian. Trang ở sách khác, bố cục đã ghi nhận khác hoặc ngày địa phương khác được tính riêng. Tổng theo kỳ và cột biểu đồ tháng cộng số trang từng ngày để các màn hình cho cùng kết quả. Đây là số trang đọc được ghi nhận, không phải số sách hoàn thành, số phiên đọc hay số trang duy nhất trên toàn bộ lịch sử.

Nếu giá trị trang thiếu hoặc sai, thời lượng hợp lệ vẫn được giữ, nhưng số trang của ngày đó trở thành chưa xác định. Thống kê thời gian vẫn dùng được. Schema cũ không có cột `total_pages` sẽ dùng định danh bố cục chưa xác định, không suy ra từ tên sách. Không ước tính số trang từ thời gian đọc hoặc số dòng dữ liệu.

[Phần triển khai thống kê của KOReader](https://github.com/koreader/koreader/blob/master/plugins/statistics.koplugin/main.lua) có thể quy đổi lịch sử trang qua view `page_stat` theo tổng số trang hiện tại của sách. InkNest giữ bố cục tại thời điểm ghi nhận, nên khi đổi phông chữ/bố cục, tổng có thể khác với view đã quy đổi hoặc bảng thống kê của KOReader.

## Lưu trữ và làm mới

Ứng dụng dùng bộ đọc bản chụp có giới hạn tài nguyên. SQLite và các tệp WAL/SHM hiện có được sao chép vào thư mục tạm riêng; bộ đọc kiểm tra thông tin tệp, tính toàn vẹn và các cột bắt buộc, mở ở chế độ chỉ đọc rồi xóa bản sao trong finally. Đổi nguồn hoặc hủy tác vụ sẽ ngắt truy vấn. Lỗi tạm thời khi tạo bản chụp được thử lại tối đa ba lần. SAF không bảo đảm lấy được bản chụp nhất quán khi KOReader đang ghi; phần phiên đọc chưa được ghi xuống đĩa sẽ chưa xuất hiện. Giới hạn đầu vào là một triệu dòng và 256 MiB tệp nguồn. Giá trị số/thời gian sai bị từ chối; thiếu thông tin trang không làm mất thời lượng hợp lệ.

Bộ đọc chuyển dần dữ liệu vào một phiên bản dữ liệu tạm riêng (generation) trong Room, mỗi lô tối đa 500 sự kiện. Room schema 6 giữ sự kiện trang/bố cục gốc trên đĩa và tính sẵn số giây/số trang theo ngày bằng SQL. Trạng thái giao diện chỉ chứa tổng theo ngày, phiên bản dữ liệu, múi giờ và thời điểm chụp, không giữ toàn bộ danh sách sự kiện hoặc các tập trang của từng sự kiện.

Chỉ sau khi đọc thành công và đóng hoàn toàn bản chụp, phiên bản dữ liệu mới được kích hoạt bằng thao tác nguyên tử. Mỗi lần thử dùng một phiên bản dữ liệu riêng; lỗi hoặc hủy tác vụ sẽ giữ bản chụp đang dùng trước đó. Token nhập dữ liệu được đăng ký trước khi sao chép và bị vô hiệu khi xóa bộ nhớ đệm, nên bản sao đang chờ không thể nạp lại dữ liệu vừa xóa.

Việc dọn dẹp dựa trên con trỏ đã được Room xác nhận ghi, kể cả khi hủy tác vụ xảy ra đồng thời với kích hoạt dữ liệu. Các phiên bản dữ liệu bỏ dở được xóa sau mỗi thao tác và dọn lại lúc khởi động. Làm mới thay thế dữ liệu, không nối thêm gây đếm trùng.

Bộ nhớ đệm được phân vùng theo cây thư mục KOReader; đổi thư mục sách không làm mất thống kê. Xóa bộ nhớ đệm đọc hoặc ngắt kết nối cũng xóa thống kê, nhưng giữ tùy chọn giao diện và ứng dụng đã ghim. Khi đổi schema trong giai đoạn phát triển, dữ liệu đệm đọc được tạo lại, không có migration.

Thống kê được cập nhật lúc khởi động, khi ứng dụng trở lại hoạt động và khi người dùng yêu cầu làm mới từ Trang chủ/Cài đặt/Thống kê. Các bên gọi đồng thời dùng chung tác vụ cấp ứng dụng. Đổi tab, đơn vị, mức thời gian, từ khóa tìm kiếm hoặc đồng hồ không đọc SAF. Mutex điều phối việc chuyển nguồn đọc ngăn bản chụp cũ nạp lại dữ liệu của nguồn đã ngắt kết nối.

## Ngày, múi giờ và trạng thái

Thời điểm nội bộ được lưu bằng mili giây tính từ epoch và diễn giải theo múi giờ hệ thống hiện tại. Dòng dữ liệu kéo dài qua nửa đêm được tính vào ngày bắt đầu, dùng Java ZoneId và tính cả giờ mùa hè (DST). Khi đổi múi giờ, các dòng gốc trong Room được nhóm lại theo ngày, mỗi lô tối đa 500 dòng, vào phiên bản dữ liệu tạm khác; không đọc lại SAF hoặc đổi thời điểm cập nhật nguồn. Tổng của ngày tương lai vẫn nằm trên đĩa nhưng bị ẩn khỏi Trang chủ/Thống kê cho tới đúng ngày đó. Thời lượng và số trang không phụ thuộc sách hiện tại. Phép tính thời gian giữ đơn vị giây cho tới khi hiển thị.

Room thực hiện tổng hợp SQL và nhập dữ liệu theo giới hạn. I/O nguồn/bản chụp chạy trên IO dispatcher; việc chuyển dữ liệu thành nội dung bảng thống kê chạy trên Default dispatcher. Screen không phân tích dữ liệu. Thống kê nhanh trên Trang chủ và màn hình Thống kê dùng chung tổng theo ngày. Đổi đơn vị/mức thời gian không đọc lại các dòng gốc; đổi nguồn chọn phiên bản dữ liệu đệm tương ứng.

StatsViewModel lưu đơn vị và mức thời gian trong SavedStateHandle. Route theo dõi trạng thái theo vòng đời; Screen chỉ nhận trạng thái và callback. Việc duy trì múi giờ ở repository có phạm vi toàn ứng dụng. Repository và StatsViewModel dùng chung `StatisticsClock` được truyền vào; đồng hồ kiểm tra ngày/múi giờ mỗi phút. Kiểm thử dùng đồng hồ giả và dispatcher được truyền vào. Làm mới lúc khởi động thuộc bộ điều phối làm mới khi ứng dụng hoạt động; những lần đổi nguồn KOReader sau đó vẫn tự làm mới.

Màn hình không lặp lại chú thích kỳ tính trung bình hoặc đoạn giải thích cách đếm trang. Tổng trên biểu đồ chỉ hiện giá trị và đơn vị. Lịch tự điều chỉnh mật độ để dùng hết chiều rộng nội dung.

## Nhịp đọc trên Trang chủ

Trang chủ hiển thị lịch theo thời gian đọc giữa sách hiện tại và ứng dụng đã ghim. Lịch dùng cùng mật độ 16–52 tuần, nhãn, ô vuông và sắc độ như Thống kê, không hiển thị ngày tương lai. Thống kê nhanh cạnh đồng hồ hiện thời gian đọc hôm nay và chuỗi ngày tính theo thời gian; nếu hôm nay chưa đọc, chuỗi đến hôm qua được giữ cho tới hết ngày. Ba chỉ số Hôm nay, trung bình ngày và chuỗi ngày trên màn hình Thống kê nằm cùng một hàng, giá trị căn giữa phía trên nhãn; khi tăng cỡ chữ, văn bản xuống dòng thay vì bị thu nhỏ.

Trang chủ lấy tối đa 52 tuần tổng theo ngày từ StatisticsRepository, xử lý trên Default dispatcher được truyền vào và theo dõi `StatisticsClock` dùng chung. Khi chưa có bản chụp, lịch bị ẩn; nếu đã nhập thành công một bản chụp rỗng, lịch hiện nhưng chưa có hoạt động. Lỗi làm mới giữ lịch đã lưu kèm cảnh báo dữ liệu cũ. Mở Thống kê vẫn giữ đơn vị đã chọn. Trong Giao diện, công tắc Nhịp đọc hoạt động độc lập, mặc định bật và được lưu trong DataStore.

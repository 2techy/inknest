# Thống kê đọc

[English](../en/statistics.md) · [Mục lục](README.md)

Thống kê đọc `settings/statistics.sqlite3` qua cây SAF KOReader đã chọn. Danh mục sách/lịch sử không cung cấp thời gian hoặc số trang. Thiếu database, mất quyền hoặc schema không hỗ trợ là không khả dụng; chỉ snapshot rỗng thành công mới nghĩa là chưa ghi nhận đọc. Không dùng dữ liệu mẫu runtime.

## Màn hình và đơn vị

Lựa chọn Thời gian/Trang áp dụng cho Hôm nay, trung bình ngày, tiêu chí chuỗi ngày, tổng kỳ, sắc độ lịch nhiệt và biểu đồ. Hôm nay chỉ hiện đơn vị chọn. Thứ tự là Hôm nay, trung bình/chuỗi ngày, xu hướng, lịch hoạt động, tổng kỳ.

Trung bình ngày tính 30 ngày lịch gần nhất gồm hôm nay và cả ngày không đọc. Chuỗi kết thúc hôm nay nếu có hoạt động; nếu chưa có thì chuỗi đến hôm qua vẫn tiếp tục cho tới hết hôm nay. Thiếu dữ liệu trang nghĩa là chưa biết, không phải 0.

Tổng gồm tuần này từ thứ Hai, tháng này, năm nay và toàn bộ. Biểu đồ có Ngày (30 ngày gần nhất), Tuần (12 tuần từ thứ Hai, gồm tuần hiện tại chưa đủ), Tháng (12 tháng gồm tháng hiện tại), Năm (mỗi năm từ năm đầu có dữ liệu). Khoảng trống được bù 0; mọi chế độ cộng cùng tổng ngày đã lưu, không đọc lại nguồn. Đổi mức thời gian không đổi tổng kỳ, trung bình, chuỗi hay lịch nhiệt. Đơn vị áp dụng cho mọi chế độ.

Lịch nhiệt phủ chiều rộng nội dung với 16–52 cột tuần thứ Hai–Chủ nhật, khoảng 26 tuần ở chiều rộng tương đương Tango. Lề nhãn thứ 48dp tăng theo cỡ chữ. Ô vuông tối đa 10dp, cách ngang 3dp/dọc 2dp. Giữ trục tháng/thứ, không có chú thích số tuần hay ít/nhiều. Ngày tương lai không hiện; ngày chưa biết có dấu chéo. Ô rỗng có viền nhẹ 0.75dp; ô hoạt động dùng thang xám đặc, góc 2dp.

Sắc độ tương đối theo giá trị dương đã biết trong vùng hiện tại, riêng cho thời gian/trang. Ngưỡng phân vị dưới 25/50/75 chia thành bốn mức. Giá trị 0/chưa biết không đổi ngưỡng. Khi mọi giá trị dương bằng nhau, kể cả chỉ một ngày, dùng mức 3 để nhìn rõ. Sắc độ không đại diện ngưỡng phút/trang cố định và không đổi cách đếm.

Bộ lọc Thư viện và tab đơn vị dùng ChoiceTab: chữ medium 12/16sp, bề mặt cao tối thiểu 32dp, vùng chạm tối thiểu 48dp. Tab mức thời gian dùng cùng typography gọn. Chữ theo cỡ hệ thống; trang cuộn dọc, dải tab tràn cuộn ngang. Biểu đồ giới hạn chiều rộng cột để dữ liệu thưa không thành khối đen lớn.

## Hợp đồng đếm trang

Mỗi dòng có thời lượng dương hợp lệ đóng góp toàn bộ số giây. Trang dùng định danh `(book ID, page, total_pages layout)` từ nguồn, loại trùng trong từng ngày địa phương. Đọc lại cùng trang trong ngày không thêm trang nhưng giữ thời gian. Khác sách, layout ghi nhận hoặc ngày địa phương là lần ghi nhận riêng. Tổng kỳ và cột tháng cộng số trang từng ngày để các màn hình thống nhất. Đây là trang đọc được ghi nhận, không phải số sách hoàn thành, phiên đọc hay trang duy nhất toàn cục.

Trang thiếu/sai giữ thời lượng hợp lệ và làm số trang ngày đó chưa biết; thời gian vẫn dùng được. Schema cũ thiếu `total_pages` dùng định danh layout không xác định, không suy từ tên sách. Không ước tính trang từ thời gian hoặc số dòng.

[Triển khai thống kê KOReader](https://github.com/koreader/koreader/blob/master/plugins/statistics.koplugin/main.lua) có thể quy đổi lịch sử qua view `page_stat` về số trang hiện tại của sách. InkNest giữ layout lúc ghi nhận nên thay phông/layout có thể làm tổng khác view đã quy đổi hoặc dashboard KOReader.

## Lưu trữ và làm mới

Production dùng bộ đọc snapshot có giới hạn. SQLite và WAL/SHM hiện có được sao chép vào thư mục tạm riêng; kiểm tra metadata, toàn vẹn và cột bắt buộc, mở chỉ đọc rồi xóa trong finally. Đổi nguồn/cancellation ngắt query. Tối đa ba lần thử cho lỗi snapshot tạm thời; SAF vẫn là best effort khi KOReader đang ghi. Phiên chưa flush không xuất hiện. Giới hạn một triệu dòng và 256 MiB tệp nguồn. Giá trị số/thời gian sai bị từ chối; thiếu metadata trang không bỏ thời gian hợp lệ.

Bộ đọc stream tối đa 500 event mỗi lô vào generation staging riêng của Room. Schema 6 giữ raw page/layout event trên đĩa và tính trước giây/trang mỗi ngày bằng SQL. UI state chỉ có tổng ngày, generation, múi giờ và timestamp snapshot, không giữ toàn bộ event hoặc tập trang từng event.

Snapshot thành công, đóng hoàn toàn mới kích hoạt generation nguyên tử. Mỗi lần thử dùng generation riêng; lỗi/hủy giữ snapshot cũ. Import token đăng ký trước sao chép bị vô hiệu khi clear cache nên bản sao đang chờ không thể nạp lại. Dọn dẹp dựa trên con trỏ Room đã commit, kể cả cancellation đua với kích hoạt. Generation bỏ dở được xóa sau tác vụ và dọn lúc startup. Refresh không nối thêm gây đếm trùng.

Cache chia theo cây KOReader; thay cây sách không vô hiệu thống kê. Clear/disconnect bao gồm thống kê nhưng giữ tùy chọn UI/ghim. Đổi schema trong giai đoạn phát triển tạo lại cache dùng một lần, không có migration.

Startup, quay lại foreground và làm mới từ Trang chủ/Cài đặt/Thống kê cập nhật thống kê. Caller đồng thời dùng chung job cấp ứng dụng. Đổi tab, đơn vị, mức thời gian, tìm kiếm hay đồng hồ không đọc SAF. Mutex chuyển nguồn ngăn snapshot cũ khôi phục dữ liệu đã ngắt kết nối.

## Lịch và state

Timestamp nội bộ là epoch milliseconds, diễn giải theo múi giờ hệ thống hiện tại. Dòng qua nửa đêm thuộc ngày bắt đầu, tính bằng Java ZoneId gồm DST. Đổi múi giờ gom lại raw Room theo lô tối đa 500 sang generation staging khác, không đọc SAF hoặc đổi timestamp cập nhật nguồn. Tổng ngày tương lai vẫn nằm trên đĩa; Trang chủ/Thống kê loại khỏi UI đến đúng ngày. Thời gian/trang độc lập sách hiện tại; giữ đơn vị giây đến khi hiển thị.

Room xử lý SQL aggregation/import có giới hạn; I/O nguồn/snapshot trên IO, phép chiếu dashboard trên Default. Screen không parse. Thống kê nhanh Trang chủ và dashboard dùng cùng tổng ngày. Đổi đơn vị/mức thời gian không đọc raw row; đổi nguồn chọn generation tương ứng.

StatsViewModel giữ đơn vị/mức thời gian trong SavedStateHandle. Route thu thập theo lifecycle, Screen nhận state/callback. Đồng hồ kiểm tra ngày/múi giờ mỗi phút; duy trì múi giờ ở repository cấp ứng dụng. Repository/StatsViewModel dùng chung `StatisticsClock` inject được; test dùng đồng hồ giả/dispatcher inject. Startup refresh thuộc foreground coordinator; thay nguồn KOReader sau đó tự refresh.

Dashboard không lặp chú thích kỳ trung bình hay giải thích cách đếm trang. Tổng biểu đồ chỉ có giá trị/đơn vị. Lịch thích ứng toàn chiều rộng.

## Nhịp đọc trên Trang chủ

Trang chủ có lịch theo thời gian giữa sách hiện tại và ghim, cùng mật độ 16–52 tuần, nhãn, ô vuông và sắc độ như Thống kê. Không hiện tương lai. Thống kê nhanh cạnh đồng hồ hiện thời gian hôm nay và chuỗi theo thời gian, giữ chuỗi đến hôm qua khi hôm nay chưa đọc. Ba chỉ số Thống kê nằm một hàng, giá trị căn giữa trên nhãn; chữ lớn được xuống dòng, không thu nhỏ.

Trang chủ chiếu tối đa 52 tuần tổng ngày từ StatisticsRepository trên Default dispatcher inject được và quan sát StatisticsClock chung. Chưa có snapshot thì ẩn lịch; snapshot rỗng thành công hiện lịch rỗng. Lỗi refresh giữ lịch cache kèm cảnh báo dữ liệu cũ. Mở Thống kê giữ đơn vị đã chọn. Giao diện có công tắc Nhịp đọc độc lập, mặc định bật, lưu trong DataStore.

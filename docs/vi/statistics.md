# Thống kê đọc

[English](../en/statistics.md) · [Mục lục](README.md)

Xem thời gian, số trang ghi nhận và hoạt động đọc từ `settings/statistics.sqlite3` của KOReader. Danh mục Thư viện và lịch sử đọc không cung cấp các tổng này.

## Màn hình và đơn vị

Chọn Thời gian/Số trang hoặc làm mới trong menu đầu trang; cũng có kéo để làm mới. Đơn vị áp dụng cho Hôm nay, trung bình ngày, chuỗi đọc, tổng, biểu đồ và lịch. Trung bình gồm đủ 30 ngày lịch đến hôm nay, kể cả ngày không đọc. Chuỗi kết thúc hôm nay nếu có hoạt động; nếu chưa có, giữ chuỗi hôm qua đến hết hôm nay. Thiếu trang là chưa xác định, không phải 0.

Tổng gồm tuần này từ thứ Hai, tháng này, năm nay và toàn bộ thời gian. Bộ lọc xu hướng chỉ đổi khoảng/tổng của biểu đồ:

| Bộ lọc | Khoảng lịch | Cột |
| --- | --- | --- |
| Tuần | Thứ Hai–Chủ nhật hiện tại | Ngày |
| Tháng | Tháng hiện tại | Tuần thứ Hai–Chủ nhật cắt tại ranh giới tháng, ghi nhãn khoảng ngày |
| Năm | Năm hiện tại | Tháng 1–12 |
| Tất cả | Năm đầu có dữ liệu đến năm hiện tại | Năm; một cột năm hiện tại khi rỗng |

Cột hiện tại chỉ cộng đến hôm nay; tương lai để trống, khoảng đã qua không đọc là 0. Nhãn/cột cuộn cùng nhau, trục giá trị cố định. Đổi đơn vị/kỳ dùng tổng ngày đã lưu, không đọc nguồn.

Lịch tự điều chỉnh 16–52 cột tuần thứ Hai–Chủ nhật. Ngày tương lai ẩn; ngày chưa xác định có gạch chéo. Bốn mức xám dùng ngưỡng phân vị dưới 25/50/75 của giá trị dương đã biết trong vùng hiển thị, riêng cho Thời gian/Số trang. Giá trị 0/chưa biết không ảnh hưởng ngưỡng; các giá trị dương bằng nhau dùng mức 3. Sắc độ biểu thị hoạt động tương đối, không phải ngưỡng phút/trang cố định. Xem [hệ thống thiết kế](design-system.md) về trình bày.

## Quy tắc đếm trang

- Mỗi dòng hợp lệ có thời lượng dương đóng góp đủ số giây. Trang loại trùng `(book ID, page, total_pages layout)` trong từng ngày địa phương. Đọc lại vẫn cộng thời gian; khác sách, bố cục ghi nhận hoặc ngày được tính riêng. Tổng kỳ cộng số trang từng ngày.
- Trang thiếu/sai giữ thời gian hợp lệ nhưng làm số trang ngày đó chưa xác định. Schema cũ thiếu `total_pages` dùng layout chưa xác định; không suy từ tên, thời gian hay số dòng.
- Đây là trang ghi nhận, không phải phiên, sách hoàn thành hay trang duy nhất toàn lịch sử. KOReader có thể quy đổi lịch sử qua [view `page_stat`](https://github.com/koreader/koreader/blob/master/plugins/statistics.koplugin/main.lua); InkNest giữ layout đã ghi, nên tổng có thể khác sau đổi phông/bố cục.

## Lưu trữ và làm mới

Thiếu DB, mất quyền hoặc schema chưa hỗ trợ là không khả dụng. Chỉ bản chụp rỗng thành công mới có nghĩa không có hoạt động; không dùng dữ liệu giả. [Kết nối nguồn](koreader-integration.md) quy định sao chép/xác thực SQLite, retry và giới hạn bản chụp SAF; phiên đọc chưa ghi xuống đĩa chưa xuất hiện. Dòng số/thời gian sai bị loại; thiếu metadata trang vẫn giữ thời gian hợp lệ.

- Nhập tối đa 500 sự kiện mỗi lô vào generation tạm riêng trong Room. Sự kiện trang/layout gốc ở trên đĩa; SQL tính trước giây/trang theo ngày. UI state chỉ có tổng, generation, múi giờ và thời điểm chụp.
- Bản chụp thành công đã đóng hoàn toàn được kích hoạt nguyên tử. Mỗi lần thử dùng generation riêng; lỗi/hủy giữ bản đang dùng trước đó. Làm mới thay thế, không nối thêm.
- Token nhập đăng ký trước sao chép bị vô hiệu khi xóa cache. Dọn dẹp theo con trỏ Room đã commit, kể cả khi hủy đồng thời với kích hoạt; xóa generation bỏ dở sau thao tác và lúc khởi động.
- Cây KOReader phân vùng thống kê; đổi thư mục sách không làm mất dữ liệu. Xóa cache/ngắt kết nối xóa sự kiện, tổng, token và timestamp, giữ tùy chọn/ghim. Xem [lưu trữ cache](reading-cache.md).
- Khởi động, trở lại foreground và làm mới từ Trang chủ/Cài đặt/Thống kê dùng chung tác vụ cấp ứng dụng. Chuyển nguồn ngăn bản cũ nạp lại dữ liệu đã ngắt. Đổi tab, đơn vị, kỳ, tìm kiếm và đồng hồ không đọc SAF.

## Ngày, múi giờ và trạng thái

Timestamp mili giây epoch dùng múi giờ hệ thống hiện tại. Dòng qua nửa đêm thuộc ngày bắt đầu theo ZoneId, kể cả DST. Đổi múi giờ chia lại sự kiện gốc theo lô tối đa 500 vào generation mới, không đọc SAF hoặc đổi timestamp nguồn. Tổng ngày tương lai giữ trên đĩa, chỉ hiện khi ngày đến. Giữ số giây đến bước hiển thị.

Room xử lý SQL/nhập có giới hạn, IO xử lý nguồn/bản chụp, Default tính dữ liệu dashboard; Screen không phân tích. StatsViewModel lưu đơn vị/kỳ (chỉ khôi phục granularity cũ nếu chưa lưu kỳ); Route theo dõi theo vòng đời. `StatisticsClock` dùng chung được inject, kiểm tra ngày/múi giờ mỗi phút; bảo trì múi giờ thuộc cấp ứng dụng. Kiểm thử thay đồng hồ/dispatcher.

## Nhịp đọc trên Trang chủ

Trang chủ dùng cùng tổng ngày cho lịch chỉ tính thời gian, tối đa 52 tuần, tính trên Default được inject. Thống kê nhanh hiện thời gian hôm nay và chuỗi theo thời gian. Tùy chọn Nhịp đọc bật mặc định, độc lập các mục Trang chủ khác.

Chưa có bản chụp thì ẩn lịch; bản rỗng thành công hiện lịch rỗng. Lỗi làm mới giữ cache và báo dữ liệu cũ. Mở Thống kê giữ đơn vị đang chọn.

## Thống kê từng sách trong Thư viện

Chi tiết sách dùng chỉ mục book-ID/checksum theo generation trên sự kiện Room. Nhập có giới hạn kích hoạt nguyên tử cùng bản chụp; chia lại ngày sao chép chỉ mục, xóa cache/dọn dẹp xóa nó. Lịch sử phân trang mới nhất trước, giữ quy tắc trang/chưa xác định. Dashboard chỉ theo dõi tổng ngày, không giữ sự kiện từng sách trong UI state. Xem [Thư viện](library.md) về ghép định danh.

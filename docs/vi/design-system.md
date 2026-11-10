# Hệ thống thiết kế InkNest

[English](../en/design-system.md) · [Mục lục](README.md)

## Giá trị thiết kế và kiểu chữ

Các giá trị thiết kế dùng chung (design token) được định nghĩa trong [ui/theme](../../app/src/main/java/dev/twotechy/inknest/ui/theme). Các token nền tảng (`InkSpacing`, `InkRadius`, `InkBorder`, `InkPalette`, `InkTextStyles`) là cơ sở cho token bố cục/thành phần, rồi đến khung trang và điều khiển dùng chung. Kiểu chữ, màu và hình dạng của Material được suy ra từ các định nghĩa này; màn hình dùng lại để tránh khai báo trùng kích thước.

Toàn bộ văn bản dùng phông sans-serif của hệ thống, không tăng khoảng cách ký tự. Cỡ chữ và chiều cao dòng tính bằng sp, thay đổi theo mức phóng to chữ của hệ thống.

| Token kiểu chữ | Cỡ chữ / chiều cao dòng (sp) | Độ đậm |
| --- | --- | --- |
| displayClock | 44 / 52 | 400 |
| pageTitle | 22 / 28 | 600 |
| subPageTitle | 20 / 26 | 600 |
| cardTitle | 18 / 24 | 600 |
| sectionTitle | 16 / 22 | 600 |
| bodyLarge | 16 / 24 | 400 |
| bodyMedium | 14 / 20 | 400 |
| bodySmall | 12 / 16 | 400 |
| labelLarge | 14 / 20 | 500 |
| labelMedium | 12 / 16 | 500 |
| labelSmall | 11 / 16 | 400 |

Tên sách dùng cardTitle; nhãn điều hướng đang chọn dùng độ đậm 600. Hướng dẫn quan trọng dùng kiểu chữ nội dung thay vì chú thích phụ.

## Khoảng cách và kích thước

| Token khoảng cách | dp |
| --- | --- |
| xs | 4 |
| sm | 8 |
| md | 12 |
| lg | 16 |
| xl | 20 |
| xxl | 24 |
| xxxl | 32 |
| huge | 40 |

Phần đầu trang cách nội dung 8dp; các phần cách nhau 24dp; tiêu đề phần cách nội dung 12dp. Khoảng đệm trong thẻ là 16dp, hoặc 12dp với thẻ gọn. Các ô trong lưới ứng dụng cách nhau 12dp theo chiều ngang và 16dp theo chiều dọc. Các đoạn chữ liên quan cách nhau 4dp; khoảng cách giữa biểu tượng và chữ, cũng như giữa các nút cùng nhóm, là 12dp. Bán kính bo góc có các mức 4/8/12dp; viền mảnh dày 1dp, viền mặc định dày 1.25dp.

| Thành phần | Kích thước cơ sở |
| --- | --- |
| Phần đầu trang / vùng chạm thao tác | tối thiểu 48dp / 48×48dp |
| Nút thao tác | tối thiểu 112×48dp; khoảng đệm ngang 16dp, dọc 12dp |
| Hàng cài đặt / lựa chọn đơn / công tắc | tối thiểu 64 / 56 / 64dp |
| Vùng đầu hàng cài đặt / biểu tượng / mũi tên điều hướng / dấu chọn | 32 / 24 / 20 / 24dp |
| Khung / biểu tượng trong lưới ứng dụng | 48 / 36dp |
| Khung / biểu tượng trong danh sách ứng dụng | 40 / 28dp |
| Ô ứng dụng | rộng tối thiểu 84dp; khoảng đệm 4dp; biểu tượng cách nhãn 8dp |
| Bìa sách đang đọc | 23% chiều rộng khả dụng, giới hạn 64–112dp |
| Thanh tiến độ đọc | 4dp |
| Menu ngữ cảnh | rộng 160–280dp; cuộn được |
| ChoiceTab | phần hiển thị cao tối thiểu 32dp, vùng chạm tối thiểu 48×48dp; chữ có độ đậm medium, cỡ 12sp / chiều cao dòng 16sp |

Các thành phần tăng chiều cao khi chữ xuống dòng hoặc được phóng to. Nhãn ứng dụng có tối đa hai dòng; Trang chủ luôn chừa đủ chỗ cho hai dòng. Thẻ sách chuyển sang xếp bìa và thông tin theo chiều dọc khi không đủ chỗ cho chữ. Không lưu bitmap trong ViewModel hoặc trạng thái được khôi phục.

## Bố cục và điều hướng

`AppScaffold` quản lý khoảng chừa cho giao diện hệ thống, lề trang, chiều rộng nội dung và điều hướng thích ứng. `ScreenScaffold` quản lý phần đầu trang, vùng cuộn và phần chân trang nếu có. Route không thêm khoảng đệm trang lần nữa.

Lề trang là 12dp khi chiều rộng nội dung dưới 360dp, 16dp từ 360dp đến dưới 600dp và 32dp từ 600dp trở lên. Nội dung rộng tối đa 800dp. Khoảng đệm trên/dưới lần lượt là 4/16dp. Phần đầu trang căn trái và căn giữa theo chiều dọc; nút Quay lại chiếm trọn vùng 48dp. Các trang chính không có nút Quay lại.

Các trang chính dùng thanh điều hướng dưới; từ chiều rộng cửa sổ 600dp, thanh này chuyển thành thanh điều hướng dọc có thể cuộn. Trang chi tiết Cài đặt và thiết lập ban đầu ẩn thanh điều hướng. Nhấn HOME về Trang chủ và đóng các thành phần giao diện tạm thời. Mỗi mục điều hướng dùng biểu tượng 24dp, nhãn cỡ 12sp / chiều cao dòng 16sp, khoảng cách 4dp và chiều cao tối thiểu 64dp. Mục đang chọn dùng biểu tượng đặc và vạch đánh dấu 32×3dp phía trên, không đổi nền. Khi không đủ chỗ cho nhãn, các biểu tượng có mô tả trợ năng, kèm chú thích cho trang đang chọn. Bố cục thích ứng theo chiều rộng cửa sổ và cỡ chữ, không theo mẫu thiết bị.

## Màu và hình ảnh

| Vai trò | Hex |
| --- | --- |
| Màu chữ chính | #181818 |
| Chữ phụ | #333333 |
| Màu ít nhấn mạnh / viền đậm | #777777 |
| Viền tiêu chuẩn | #A0A0A0 |
| Nền nhạt | #F2F2F2 |
| Nền giấy | #FFFFFF |

Cả hai chế độ đều dùng giao diện thang xám. E-Ink dùng đường viền phân chia bố cục đậm hơn và biểu tượng ứng dụng thang xám; Tiêu chuẩn dùng viền nhạt hơn và giữ màu gốc của biểu tượng. Chữ nhỏ có nội dung cần đọc dùng màu chính/phụ. Các vai trò màu của Material, kể cả màu lỗi, được ánh xạ vào bảng màu này; lớp phủ màu (tint) được đặt trong suốt. Lỗi có dấu hiệu nhận biết ngoài màu sắc. Dấu chọn, menu và hộp thoại không có hiệu ứng chuyển động trang trí, gợn sóng khi chạm, bóng đổ hoặc hiệu ứng nổi.

Biểu tượng dùng bộ nhớ đệm RAM chung, tối đa 100 ảnh ở kích thước 128px. Khóa gồm component/phiên bản, ngôn ngữ và mật độ điểm ảnh; khi chưa có ảnh trong bộ nhớ đệm, ứng dụng tải trên IO dispatcher và tạm hiện chữ cái đầu. Trang chủ và Thư viện dùng chung hình sách mặc định không chuyển động; ảnh được giải mã trên IO dispatcher. Bộ nhớ đệm LRU cho bìa đã giải mã giữ tối đa 4 MiB dữ liệu ảnh; khóa gồm URI bất biến và kích thước hiển thị chia theo các mức 64px, tối đa 480px. Giới hạn này không tính ảnh mà giao diện đang giữ tham chiếu hoặc bộ đệm tạm. Giải mã tuần tự, yêu cầu trùng dùng lại kết quả. Xóa bộ nhớ đệm/ngắt kết nối sẽ vô hiệu dữ liệu đệm và ngăn tác vụ cũ nạp lại dữ liệu. Không dùng hiệu ứng mờ chuyển ảnh (crossfade) hoặc ánh sáng lướt khi tải (shimmer).

## Bố cục từng tính năng

Trang chủ lần lượt hiển thị phần đồng hồ tổng quan, sách đang đọc, nhịp đọc nếu bật và ứng dụng đã ghim. Thống kê nhanh chuyển xuống dưới đồng hồ khi thiếu chỗ; khi ẩn thì không chừa cột trống. Một đường kẻ phân cách phần tổng quan. Thông báo nằm trong hàng gọn có viền. Thẻ sách có một đường viền và thanh tiến độ trải hết chiều rộng. Trang chủ hiển thị hàng ứng dụng đã ghim đầu tiên theo thứ tự lưu; Xem tất cả mở trang Ứng dụng để xem toàn bộ mục ghim.

Cài đặt gồm Đọc và dữ liệu, Giao diện, Trình khởi chạy, Giới thiệu. Mỗi hàng dùng tiêu đề cỡ 16sp, độ đậm regular; mô tả cỡ 12sp, cách tiêu đề 4dp. Tiêu đề phần dùng chữ 14sp, độ đậm medium, không có đường kẻ trang trí; các phần cách nhau 24dp, tiêu đề cách nội dung 12dp. Hàng điều hướng/công tắc có viền phân cách mảnh và căn lề trang. Tên/đường dẫn thư mục là một vùng chạm; mỗi thư mục sách có nút xóa riêng 48dp kèm xác nhận. Thông tin chỉ đọc không có mũi tên điều hướng. Các lệnh được thực hiện bằng nút có viền.

Trong Giao diện, phần Chung gồm Ngôn ngữ và Chế độ hiển thị, mỗi mục mở hộp thoại lựa chọn không có hiệu ứng chuyển động. Phần Các mục trên Trang chủ cho phép bật/tắt thống kê nhanh, nhịp đọc và ứng dụng đã ghim độc lập. Giới thiệu hiển thị thương hiệu, phiên bản/tác giả, mục cập nhật đang bị vô hiệu hóa, liên kết trang web/thiết lập và cuối cùng là giấy phép bên thứ ba. Mỗi giấy phép có trang chi tiết cuộn được. Việc tải, thử lại và lưu đệm được quản lý riêng cho từng thành phần, hoạt động ngoại tuyến.

Thiết lập ban đầu có ba bước, thanh tiến độ tĩnh dày 3dp, số bước hiển thị và thông tin tiến độ cho công cụ trợ năng. Nội dung cuộn phía trên phần chân trang cố định; nút chính và Bỏ qua có vùng chạm đủ lớn. Bước kết nối dữ liệu đọc dùng lại các điều khiển nguồn thật; yêu cầu đặt trình khởi chạy chỉ xảy ra khi người dùng chủ động chọn. Lỗi lưu được thông báo qua trợ năng mà không ngắt lời nội dung đang đọc; trong lúc lưu, thao tác điều hướng và hoàn tất bị vô hiệu hóa.

Thống kê hiển thị Hôm nay, trung bình ngày và chuỗi ngày đọc trong ba cột bằng nhau, tiếp theo là xu hướng, lịch hoạt động và tổng theo kỳ. Thời gian/Trang áp dụng cho mọi giá trị đọc. Lịch hoạt động dạng bản đồ nhiệt tự điều chỉnh từ 16 đến 52 tuần, với ô vuông tối đa 10dp, khoảng cách ngang/dọc 3/2dp, lề nhãn thứ 48dp tăng theo cỡ chữ, viền ô không có hoạt động 0.75dp và bán kính bo góc 2dp. Giữ nhãn tháng/thứ, không hiện ngày tương lai. Trang chủ dùng cùng lịch theo thời gian. Xem [thống kê](statistics.md) về sắc độ và cách đếm.

## Cử chỉ và làm mới

`InkGestureTokens` định nghĩa ngưỡng vuốt tab 40dp, kéo làm mới 56dp. Sau khi vượt ngưỡng di chuyển tối thiểu của hệ thống (touch slop), cử chỉ được khóa hướng nếu khoảng di chuyển theo một trục lớn hơn 1.5× trục kia. Thao tác chỉ thực hiện khi thả tay; hủy cử chỉ hoặc đa chạm không kích hoạt thao tác. Vùng vuốt Quay lại ở cạnh màn hình và thao tác cuộn ngang đã được thành phần con tiếp nhận được ưu tiên.

Vuốt tab theo Thư viện → Thống kê → Trang chủ → Ứng dụng → Cài đặt, không quay vòng. Cử chỉ chỉ áp dụng cho nội dung trang chính, không áp dụng cho thanh điều hướng, chi tiết Cài đặt, thiết lập ban đầu, tìm kiếm/bộ gõ hoặc hộp thoại và các lớp giao diện modal. Trạng thái có thể lưu giúp khôi phục trang đang mở; HOME hủy hiệu lực của cử chỉ đang thực hiện.

Trang chủ, Thư viện, Thống kê và Ứng dụng cung cấp `RefreshAction` qua `ScreenScaffold`. Có thể bắt đầu kéo khi đang ở đầu trang, kể cả nội dung ngắn hoặc rỗng. Chỉ báo đang làm mới có màu đơn sắc, hiển thị đè bên dưới phần đầu trang, không đẩy nội dung hoặc chạy hiệu ứng chuyển động. Vẫn có nút làm mới/thử lại. Chưa có nguồn thì vô hiệu làm mới dữ liệu đọc; chuyển tab không làm mới. Các thông báo về dữ liệu đệm/lỗi vẫn được giữ trong nội dung, không hiển thị thêm nhãn đang làm mới trùng lặp. Đánh giá độ nét và bóng mờ E-Ink trên phần cứng thật theo [kiểm tra độ tin cậy](reliability.md).

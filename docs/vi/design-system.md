# Hệ thống thiết kế InkNest

[English](../en/design-system.md) · [Mục lục](README.md)

## Token và kiểu chữ

Định nghĩa nằm trong [ui/theme](../../app/src/main/java/dev/twotechy/inknest/ui/theme). Token nền tảng (`InkSpacing`, `InkRadius`, `InkBorder`, `InkPalette`, `InkTextStyles`) cung cấp giá trị cho token bố cục/thành phần, sau đó đến scaffold và điều khiển dùng chung. Typography, màu và hình dạng Material được ánh xạ từ các định nghĩa này; màn hình dùng lại thay vì lặp kích thước.

Toàn bộ chữ dùng sans-serif hệ thống, giãn ký tự bằng 0. Cỡ chữ và chiều cao dòng dùng sp, theo tỷ lệ phông chữ hệ thống.

| Token chữ | Cỡ chữ / chiều cao dòng (sp) | Độ đậm |
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

Header cách nội dung 8dp; các phần cách nhau 24dp; tiêu đề phần cách nội dung 12dp. Padding thẻ là 16dp, thẻ gọn là 12dp. Lưới ứng dụng có khoảng ngang 12dp, dọc 16dp. Chữ liên quan cách nhau 4dp; biểu tượng/chữ và nút cùng nhóm cách nhau 12dp. Bán kính là 4/8/12dp; viền mảnh 1dp, mặc định 1.25dp.

| Thành phần | Kích thước cơ sở |
| --- | --- |
| Header / vùng chạm thao tác | tối thiểu 48dp / 48×48dp |
| Nút thao tác | tối thiểu 112×48dp; padding ngang 16dp, dọc 12dp |
| Hàng cài đặt / radio / công tắc | tối thiểu 64 / 56 / 64dp |
| Ô đầu hàng / biểu tượng / chevron / dấu chọn | 32 / 24 / 20 / 24dp |
| Khung / biểu tượng trong lưới ứng dụng | 48 / 36dp |
| Khung / biểu tượng trong danh sách ứng dụng | 40 / 28dp |
| Ô ứng dụng | rộng tối thiểu 84dp; padding 4dp; biểu tượng cách nhãn 8dp |
| Bìa sách đang đọc | 23% chiều rộng khả dụng, giới hạn 64–112dp |
| Thanh tiến độ đọc | 4dp |
| Menu ngữ cảnh | rộng 160–280dp; cuộn được |
| ChoiceTab | bề mặt tối thiểu 32dp, vùng chạm tối thiểu 48×48dp; chữ medium 12/16sp |

Chiều cao tối thiểu tăng khi chữ xuống dòng hoặc phóng to. Nhãn ứng dụng tối đa hai dòng; Trang chủ chừa đủ hai dòng. Thẻ sách xếp bìa và chi tiết theo chiều dọc khi thiếu chỗ cho chữ. Bitmap không nằm trong ViewModel hoặc trạng thái đã lưu.

## Bố cục và điều hướng

`AppScaffold` quản lý inset hệ thống, lề trang, chiều rộng nội dung và điều hướng thích ứng. `ScreenScaffold` quản lý header, cuộn và footer tùy chọn. Route không thêm lại padding trang.

Lề là 12dp khi nội dung dưới 360dp, 16dp khi dưới 600dp và 32dp ở mức lớn hơn; nội dung rộng tối đa 800dp. Padding trên/dưới là 4/16dp. Header căn trái, giữa theo chiều dọc; nút Quay lại chiếm đủ ô 48dp. Trang chính không có nút Quay lại.

Các trang chính dùng điều hướng dưới, chuyển sang rail cuộn được khi cửa sổ rộng từ 600dp. Trang chi tiết Cài đặt và thiết lập ban đầu ẩn điều hướng. HOME về Trang chủ và đóng UI tạm thời. Điều hướng dùng biểu tượng 24dp, nhãn 12/16sp, khoảng cách 4dp, hàng cao tối thiểu 64dp. Mục chọn dùng biểu tượng đặc và một vạch trên 32×3dp, không có nền chọn. Khi thiếu chỗ cho nhãn, biểu tượng có mô tả trợ năng và một chú thích cho trang đang chọn. Bố cục theo chiều rộng cửa sổ và cỡ chữ, không theo mẫu thiết bị.

## Màu và hình ảnh

| Vai trò | Hex |
| --- | --- |
| Màu chữ chính | #181818 |
| Chữ phụ | #333333 |
| Màu dịu / viền đậm | #777777 |
| Viền tiêu chuẩn | #A0A0A0 |
| Nền nhẹ | #F2F2F2 |
| Giấy | #FFFFFF |

Hai chế độ đều dùng giao diện thang xám. E-Ink dùng viền cấu trúc đậm hơn và biểu tượng thang xám; Tiêu chuẩn dùng viền nhẹ hơn và màu gốc biểu tượng. Chữ nhỏ mang thông tin dùng màu chính/phụ. Vai trò Material, kể cả lỗi, được ánh xạ vào bảng màu; tint trong suốt. Lỗi không chỉ thể hiện bằng màu. Dấu chọn, menu và hộp thoại tĩnh, không có hiệu ứng trang trí, ripple, bóng hoặc elevation.

Biểu tượng dùng cache RAM chung tối đa 100 ảnh ở 128px, khóa theo component/phiên bản và locale/density; khi chưa có thì tải trên IO và hiện chữ cái đầu. Trang chủ/Thư viện dùng chung bìa thay thế hình sách tĩnh và giải mã trên IO. LRU bìa đã giải mã có ngân sách 4 MiB cho bộ nhớ ảnh được giữ; khóa gồm URI bất biến và nhóm kích thước 64px, tối đa 480px. Ngân sách không gồm tham chiếu UI đang hoạt động hay bộ đệm tạm. Giải mã tuần tự, yêu cầu trùng dùng lại kết quả. Xóa cache/ngắt kết nối vô hiệu cache và ngăn công việc cũ nạp lại. Không dùng crossfade hay shimmer.

## Phân cấp tính năng

Trang chủ xếp đồng hồ, sách đang đọc, nhịp đọc tùy chọn và ứng dụng ghim. Thống kê chuyển xuống dưới đồng hồ khi thiếu chỗ; khi ẩn không giữ cột trống. Một đường phân cách ngăn phần tổng quan. Thông báo dùng hàng viền gọn. Thẻ sách có một viền và thanh tiến độ toàn chiều rộng. Trang chủ hiện hàng ghim đầu theo thứ tự lưu; Xem tất cả mở Ứng dụng để truy cập mọi mục ghim.

Cài đặt gồm Đọc và dữ liệu, Giao diện, Trình khởi chạy, Giới thiệu. Hàng dùng tiêu đề regular 16sp, mô tả 12sp, cách nhau 4dp. Tiêu đề phần dùng medium 14sp, không có đường trang trí; khoảng cách là 24/12dp. Hàng điều hướng/công tắc có viền phân cách mảnh và căn lề trang. Tên/đường dẫn thư mục là một vùng chạm; mỗi thư mục sách có nút xóa riêng 48dp kèm xác nhận. Metadata chỉ đọc không có chevron. Nút viền thực hiện lệnh.

Giao diện nhóm Ngôn ngữ và Chế độ hiển thị trong phần Chung, mở hộp thoại chọn tĩnh; Các mục trên Trang chủ điều khiển thống kê nhanh, nhịp đọc và ghim độc lập. Giới thiệu hiện thương hiệu, phiên bản/tác giả, mục cập nhật chưa hoạt động, liên kết website/thiết lập và giấy phép bên thứ ba ở cuối. Mỗi giấy phép có trang chi tiết cuộn được; tải/thử lại/cache độc lập theo thành phần và hoạt động ngoại tuyến.

Thiết lập ban đầu có ba bước, vạch tiến độ tĩnh 3dp, số bước và ngữ nghĩa trợ năng. Nội dung cuộn phía trên footer cố định; nút chính và Bỏ qua có vùng chạm đủ lớn. Thiết lập đọc dùng lại điều khiển nguồn thật; yêu cầu launcher là thao tác rõ ràng. Lỗi lưu được thông báo qua trợ năng, trong lúc lưu không cho điều hướng/hoàn tất.

Thống kê có Hôm nay, trung bình ngày và chuỗi ngày ở ba cột bằng nhau, sau đó là xu hướng, lịch hoạt động và tổng. Thời gian/Trang áp dụng cho mọi giá trị đọc. Lịch nhiệt thích ứng 16–52 tuần, ô vuông tối đa 10dp, khoảng ngang/dọc 3/2dp, lề nhãn thứ 48dp tăng theo cỡ chữ, viền ô rỗng 0.75dp và góc 2dp. Giữ nhãn tháng/thứ, không hiện ngày tương lai. Trang chủ dùng cùng lịch theo thời gian. Xem [thống kê](statistics.md) về sắc độ và cách đếm.

## Cử chỉ và làm mới

`InkGestureTokens` định nghĩa ngưỡng vuốt tab 40dp, kéo làm mới 56dp. Sau touch slop hệ thống, khóa hướng khi một trục vượt trục kia 1.5×. Thao tác chạy khi thả tay; hủy hoặc đa chạm không kích hoạt. Vùng cạnh Back hệ thống và cuộn ngang do thành phần con xử lý được ưu tiên.

Vuốt tab theo Thư viện → Thống kê → Trang chủ → Ứng dụng → Cài đặt, không quay vòng. Chỉ áp dụng cho nội dung cấp cao nhất, không gồm thanh điều hướng, chi tiết Cài đặt, thiết lập ban đầu, tìm kiếm/IME và bề mặt modal. Trạng thái saveable khôi phục trang; HOME vô hiệu cử chỉ đang chạy.

Trang chủ, Thư viện, Thống kê và Ứng dụng cung cấp `RefreshAction` qua `ScreenScaffold`. Kéo bắt đầu ở đầu nội dung, kể cả nội dung ngắn/rỗng. Chỉ báo đơn sắc phủ dưới header, không dịch nội dung hay chuyển động. Vẫn có nút làm mới/thử lại. Chưa có nguồn thì vô hiệu làm mới dữ liệu đọc; chuyển tab không làm mới. Nội dung giữ thông báo cache/lỗi, không lặp nhãn đang chạy. Đánh giá độ nét và bóng mờ E-Ink trên phần cứng thật theo [kiểm tra độ tin cậy](reliability.md).

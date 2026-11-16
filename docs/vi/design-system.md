# Hệ thống thiết kế InkNest

[English](../en/design-system.md) · [Mục lục](README.md)

Quy tắc dùng chung để giao diện E-Ink dễ đọc và tĩnh. Tra giá trị token trực tiếp tại [ui/theme](../../app/src/main/java/dev/twotechy/inknest/ui/theme).

## Token và kiểu chữ

Token nền (`InkSpacing`, `InkRadius`, `InkBorder`, `InkPalette`, `InkTextStyles`) cung cấp giá trị cho token thành phần/bố cục và typography/colors/shapes Material. Screen dùng lại, không chép kích thước riêng.

Thương hiệu/đồng hồ Trang chủ dùng serif hệ thống; phần khác dùng sans-serif. Chữ dùng sp và theo cỡ hệ thống. Hướng dẫn thiết yếu dùng kiểu body; tên sách dài được xuống dòng đầy đủ, nhãn ứng dụng tối đa hai dòng. Logo Trang chủ dùng hình gốc, không có đệm adaptive-icon.

## Khoảng cách và kích thước

- Dùng token section, card, bìa và điều khiển chung; không duy trì bản sao từng màn hình.
- Vùng chạm tối thiểu 48×48dp. Phần hiển thị nhỏ hơn như ChoiceTab vẫn giữ vùng chạm lớn. Chiều cao tăng theo xuống dòng/cỡ chữ; không thu nhỏ chữ để vừa.
- Màn hẹp xếp bìa/chi tiết hoặc nhóm chỉ số theo chiều dọc. Nội dung tùy chọn bị ẩn không chừa cột/khối rỗng.

## Bố cục và điều hướng

`AppScaffold` quản lý inset, lề, chiều rộng và điều hướng thích ứng; `ScreenScaffold` quản lý header, cuộn và footer tùy chọn. Route không thêm lề trang lần nữa. Thích ứng theo chiều rộng/cỡ chữ, không theo model thiết bị.

- Thứ tự chính: Trang chủ → Thư viện → Thống kê → Ứng dụng. Điều hướng đáy đổi sang rail cuộn được tại chiều rộng cửa sổ 600dp. Header căn theo lề nội dung; trang chính không có nút Back.
- Cài đặt mở từ Trang chủ/thao tác cấu hình, ẩn điều hướng chính và về tab xuất phát. Chi tiết Cài đặt, thiết lập ban đầu và Chi tiết sách ẩn điều hướng chính/vuốt tab.
- HOME về Trang chủ, đóng UI tạm và vô hiệu cử chỉ. Giữ đích/từ khóa/vị trí cuộn khi được hỗ trợ.
- Lựa chọn dùng icon đặc và vạch phía trên. Khi nhãn không vừa, icon có nhãn trợ năng dùng chung chú thích đích đang chọn. Tab/biểu đồ tràn cuộn ngang, nội dung cuộn dọc.

## Màu sắc và ảnh

Cả hai chế độ dùng UI thang xám. E-Ink tăng viền cấu trúc và chuyển icon ứng dụng sang xám; Tiêu chuẩn dùng viền nhẹ, icon màu gốc. Chữ nhỏ có thông tin dùng màu chính/phụ. Lỗi và lựa chọn phải hiểu được mà không chỉ dựa vào màu.

Chuyển trạng thái, menu và dialog giữ tĩnh: không animation trang trí, ripple, bóng, elevation, shimmer hoặc crossfade.

| Cache ảnh | Contract |
| --- | --- |
| Icon ứng dụng | RAM cache chung, 100 icon ở 128px; khóa gồm component/version và locale/density. Cache miss tải trên IO, dự phòng bằng chữ cái đầu. |
| Bìa đã giải mã | LRU chung 4 MiB bộ nhớ giữ lại; URI bất biến và nhóm kích thước 64px, tối đa 480px. Tham chiếu UI/buffer tạm ngoài ngân sách. Giải mã tuần tự, yêu cầu trùng dùng lại kết quả. |

Trang chủ/Thư viện dùng chung bìa mặc định tĩnh và giải mã IO. Xóa cache/ngắt kết nối vô hiệu bìa đã giải mã, chặn tác vụ cũ nạp lại. Không lưu bitmap trong ViewModel hoặc saved state. Ảnh thu nhỏ trên đĩa theo [contract cache đọc](reading-cache.md).

## Thứ bậc nội dung

Dùng header, điều khiển và token chung. Giữ thao tác chính dễ tới, nội dung tùy chọn có thể thu gọn, lỗi/retry hiển thị mà không thay dữ liệu thành công. Hàng điều hướng/toggle Cài đặt dùng divider; metadata chỉ đọc không có chevron. Thiết lập cuộn trên footer cố định, có ngữ nghĩa bước/tiến độ và thông báo lỗi lưu; đang lưu khóa điều hướng/hoàn tất.

Bộ lọc Thư viện và kỳ Thống kê dùng chung nhóm tab rộng bằng nhau, cuộn khi cần. Menu dùng token chung, vùng chạm theo chuẩn trợ năng và trạng thái lựa chọn rõ ràng.

Hành vi tính năng nằm ở [thiết lập](setup.md), [Thư viện](library.md) và [Thống kê](statistics.md), không mô tả màn hình lần hai ở đây.

## Cử chỉ và làm mới

- `InkGestureTokens` giữ ngưỡng: vuốt tab 40dp, kéo làm mới 56dp. Sau touch slop hệ thống, khóa hướng khi một trục vượt trục kia 1.5×. Kích hoạt một lần khi thả; hủy/đa chạm không kích hoạt. Tôn trọng mép Back và cuộn ngang đã được thành phần con xử lý.
- Vuốt vòng Trang chủ → Thư viện → Thống kê → Ứng dụng, mỗi cử chỉ một tab. Loại trừ điều hướng, Cài đặt, thiết lập, Chi tiết sách, tìm kiếm/IME và modal.
- Trang chủ/Thư viện/Thống kê/Ứng dụng cung cấp `RefreshAction` qua `ScreenScaffold`. Kéo từ đầu trang, kể cả nội dung rỗng/ngắn. Chỉ báo đơn sắc tĩnh phủ dưới header, không đẩy nội dung; giữ nút/retry thay thế. Tắt làm mới đọc khi chưa có nguồn; đổi tab không làm mới.

Kiểm tra xuống dòng, vùng chạm và điều khiển với chữ lớn/màn hẹp. Tương phản/ghosting cần [nghiệm thu thiết bị](reliability.md).

# Đóng góp cho InkNest

[English](../en/contributing.md) · [Mục lục](README.md)

## Báo lỗi hoặc đề xuất tính năng

Báo lỗi hoặc gửi đề xuất tại mục Issues của kho mã. Khi báo lỗi, cung cấp phiên bản/biến thể ứng dụng, mẫu thiết bị, phiên bản Android và firmware, phiên bản KOReader nếu liên quan, các bước tái hiện, kết quả mong đợi và kết quả thực tế. Ghi rõ kết quả từ giả lập hay thiết bị thật. Chỉ kèm đoạn log ngắn hoặc ảnh chụp đã loại thông tin riêng tư khi cần.

Không tải lên sách riêng, URI tài liệu, cơ sở dữ liệu nguồn, khóa ký hoặc mật khẩu. Dùng dữ liệu ví dụ tự tạo và che đường dẫn/thông tin cá nhân trong ảnh. Khi đề xuất tính năng, mô tả vấn đề người dùng gặp phải, cách sử dụng mong muốn và các ràng buộc ngoại tuyến/E-Ink trước khi đề xuất chi tiết triển khai.

## Chuẩn bị pull request

1. Làm theo [hướng dẫn thiết lập phát triển](development.md), đọc mã và các bài kiểm thử hiện có, giữ thay đổi đúng phạm vi.
2. Tuân theo [kiến trúc](architecture.md) và [hệ thống thiết kế](design-system.md). Giữ một module ứng dụng chính `:app`, cơ chế tiêm phụ thuộc thủ công và luồng Repository → ViewModel → UI state → Route/Screen.
3. Tuân theo `.editorconfig`, dùng tài nguyên chuỗi và các giá trị giao diện/cử chỉ dùng chung. Chỉ thêm thư viện phụ thuộc khi cần, khai báo vào nhóm phù hợp trong danh mục phiên bản. Không sửa thủ công schema Room hoặc profile được sinh tự động.
4. Thêm kiểm thử cho hành vi thay đổi, nhất là xử lý lỗi, hủy tác vụ, khôi phục, cách ly nguồn và giữ bản chụp dữ liệu cũ. Kiểm thử coroutine dùng chung bộ lập lịch cho các dispatcher được truyền vào và dùng đồng hồ giả; hành vi Room/SAF trên Android được kiểm tra bằng kiểm thử Android.
5. Chạy các [kiểm tra](development.md) phù hợp. PR cần nêu vấn đề, hành vi sau thay đổi, kiểm tra thực sự đã chạy và giới hạn còn lại. Với thay đổi giao diện, kèm hình ảnh hoặc kết quả kiểm tra bố cục và ghi riêng kết quả nghiệm thu trên thiết bị E-Ink thật.
6. Rà soát diff cuối để loại thay đổi ngoài phạm vi, dữ liệu riêng tư và tệp đầu ra sinh tự động. Dùng Conventional Commits khi tạo commit.

Thay đổi phải giữ khả năng hủy coroutine, báo lỗi khi ghi thất bại và giữ dữ liệu từ lần đọc thành công trước đó khi gặp lỗi đọc dự kiến. Không thêm đồng bộ mạng, thu thập dữ liệu sử dụng (telemetry), quyền truy cập bộ nhớ diện rộng cho ứng dụng chính hoặc migration schema ngoài phạm vi công việc. Thay đổi schema bộ nhớ đệm cần được rà soát rõ về hành vi khi nâng cấp. Không đưa khóa/chứng chỉ ký hoặc tệp kết quả biên dịch/đo lường vào Git.

## Kiểm tra pull request

CI bỏ qua PR chỉ thay đổi `docs/**` và các tệp Markdown ở gốc kho mã. Các PR khác vào `master`, kể cả bản nháp, chạy bước `build`: kiểm tra định dạng, kiểm thử JVM và lint cho debug/release, biên dịch APK ứng dụng và kiểm thử. Các kiểm tra áp dụng cho PR phải thành công và các trao đổi rà soát phải được giải quyết trước khi squash merge. Không bắt buộc có thêm người duyệt phê duyệt. Kiểm thử trên giả lập Android được chạy thủ công; đo hiệu năng và nghiệm thu thiết bị thật thực hiện riêng. Xem [GitHub Actions](development.md#github-actions) về tệp kết quả và [báo cáo bảo mật](../../SECURITY.md) cho vấn đề nhạy cảm.

## Tài liệu và bản dịch

Mỗi trang trong `docs/en` có bản cùng tên trong `docs/vi`. Tiếng Anh là bản đối chiếu kỹ thuật; cập nhật cả hai cùng lúc và giữ liên kết đổi ngôn ngữ. Giữ nguyên định danh trong mã, lệnh, đường dẫn và đơn vị. Nhãn giao diện phải khớp tài nguyên chuỗi Android tương ứng; diễn đạt bằng tiếng Việt tự nhiên và dùng thuật ngữ nhất quán.

Tài liệu mô tả hành vi được hỗ trợ, cách thiết lập, hợp đồng kỹ thuật và các bước kiểm tra có thể tái hiện. Dùng ví dụ người đọc có thể thực hiện trong môi trường của mình. Kiểm tra liên kết tương đối, liên kết đến tiêu đề và ví dụ lệnh khi chuyển tệp hoặc đổi tiêu đề.

## Giấy phép

Mã nguồn và tài liệu InkNest dùng [MIT](../../LICENSE). Giữ thông báo bản quyền/giấy phép bên thứ ba; các thành phần đó giữ điều khoản riêng. Chứng chỉ ký APK độc lập với giấy phép mã nguồn.

# Đóng góp cho InkNest

[English](../en/contributing.md) · [Mục lục](README.md)

Báo lỗi, chuẩn bị pull request tập trung và đồng bộ hai ngôn ngữ tài liệu.

## Báo lỗi hoặc đề xuất tính năng

Dùng [Issues](https://github.com/2techy/inknest/issues). Lỗi cần phiên bản/biến thể app, thiết bị, Android/firmware, KOReader liên quan, bước tái hiện và kết quả mong đợi/thực tế. Phân biệt giả lập/thiết bị thật; chỉ thêm log/ảnh cần thiết đã ẩn dữ liệu riêng. Tính năng nên nêu vấn đề người dùng và quy trình ngoại tuyến/E-Ink.

Không tải sách riêng, URI tài liệu, DB nguồn, khóa ký hoặc mật khẩu lên. Dùng ví dụ giả và ẩn đường dẫn cá nhân. Vấn đề nhạy cảm theo [báo cáo bảo mật](../../SECURITY.md).

## Chuẩn bị pull request

1. Theo [phát triển](development.md), đọc code/test hiện có và giữ thay đổi tập trung.
2. Giữ [kiến trúc](architecture.md), [thiết kế](design-system.md), string resource và token chung. Thêm phụ thuộc qua catalog; không sửa tay schema/profile sinh tự động.
3. Kiểm thử lỗi có ý nghĩa, hủy, khôi phục, cách ly nguồn và giữ bản chụp. Theo test hiện có; hành vi Room/SAF native thuộc Android test.
4. Chạy [kiểm tra](development.md) phù hợp. Mô tả vấn đề, hành vi sau đổi, kiểm tra thực chạy và giới hạn. Đổi UI cần bằng chứng bố cục; nghiệm thu E-Ink thật tách riêng.
5. Rà diff tránh thay đổi ngoài việc, dữ liệu sinh/riêng tư. Dùng Conventional Commits khi commit.

Không thêm đồng bộ mạng, telemetry, quyền bộ nhớ rộng hoặc migration ngoài yêu cầu. Đổi schema cần xem xét nâng cấp rõ ràng. Giữ khóa ký và artifact build/đo ngoài Git.

## Kiểm tra pull request

Theo [quy tắc CI và artifact](development.md#github-actions). Các check áp dụng phải đạt, giải quyết trao đổi trước squash merge; không bắt buộc thêm người phê duyệt. Test giả lập chạy thủ công; benchmark/nghiệm thu thiết bị tách riêng.

## Tài liệu và bản dịch

Đồng bộ trang cùng tên trong `docs/en` và `docs/vi`, lấy tiếng Anh đối chiếu kỹ thuật. Giữ link đổi ngôn ngữ, định danh, lệnh, đường dẫn và đơn vị. Nhãn UI khớp string resource; tiếng Việt tự nhiên, nhất quán.

Chỉ viết đủ để thực hiện tác vụ hoặc hiểu contract. Mỗi chi tiết có một nơi chính, nơi khác dẫn link. Kiểm tra link tương đối, anchor và lệnh sau sửa.

## Giấy phép

Code/docs dùng [MIT](../../LICENSE). Giữ thông báo và điều khoản bên thứ ba; chứng chỉ ký độc lập với giấy phép.

# Đóng góp cho InkNest

[English](../en/contributing.md) · [Mục lục](README.md)

## Báo lỗi hoặc đề xuất tính năng

Dùng issue tracker của repository. Khi báo lỗi, cung cấp phiên bản/biến thể app, mẫu thiết bị, phiên bản Android và firmware, phiên bản KOReader nếu liên quan, bước tái hiện, hành vi mong đợi và thực tế. Phân biệt giả lập với thiết bị thật. Chỉ kèm log/ảnh đã loại dữ liệu riêng tư khi cần.

Không tải lên sách riêng, document URI, database nguồn, khóa ký hoặc mật khẩu. Dùng ví dụ tổng hợp và che đường dẫn/thông tin cá nhân trong ảnh. Khi đề xuất tính năng, mô tả vấn đề người dùng, luồng mong đợi và ràng buộc ngoại tuyến/E-Ink trước chi tiết triển khai.

## Chuẩn bị pull request

1. Theo [thiết lập phát triển](development.md), đọc code/test hiện có và giữ thay đổi tập trung.
2. Theo [kiến trúc](architecture.md) và [hệ thống thiết kế](design-system.md). Giữ một module production `:app`, DI thủ công và luồng Repository → ViewModel → UI state → Route/Screen.
3. Tuân theo `.editorconfig`, dùng string resource và token theme/cử chỉ chung. Chỉ thêm dependency cần thiết qua nhóm phù hợp trong version catalog. Không sửa tay schema Room hoặc profile được sinh.
4. Thêm test cho hành vi có ý nghĩa, nhất là lỗi, hủy, khôi phục, cách ly nguồn và giữ snapshot. Coroutine test dùng chung scheduler cho dispatcher và đồng hồ giả được inject; hành vi Room/SAF native thuộc Android test.
5. Chạy [kiểm tra](development.md) phù hợp. PR nêu vấn đề, hành vi sau thay đổi, kiểm tra đã chạy và giới hạn còn lại. Với UI, kèm bằng chứng bố cục và phân biệt nghiệm thu E-Ink vật lý.
6. Rà diff cuối để bỏ thay đổi ngoài phạm vi, dữ liệu riêng tư và output sinh tự động. Dùng Conventional Commits khi tạo commit.

Đóng góp phải giữ khả năng hủy coroutine, báo lỗi ghi và giữ dữ liệu thành công khi đọc gặp lỗi dự kiến. Không tiện thể thêm đồng bộ mạng, telemetry, quyền truy cập bộ nhớ rộng cho production hoặc migration schema. Thay đổi schema cache cần được rà soát rõ về hành vi nâng cấp. Không đưa vật liệu ký và artifact build/đo lường vào Git.

## Kiểm tra pull request

Mọi PR vào `master`, kể cả draft và thay đổi tài liệu, chạy check `build`: định dạng, JVM tests debug/release, lint debug/release và biên dịch APK/test. Giữ check xanh, giải quyết review conversation trước khi squash merge. Không bắt buộc người duyệt thứ hai approve. Test emulator Android chạy thủ công; benchmark và nghiệm thu thiết bị thật thực hiện riêng. Xem [GitHub Actions](development.md#github-actions) về artifact và [báo cáo bảo mật](../../SECURITY.md) cho vấn đề nhạy cảm.

## Tài liệu và bản dịch

Mỗi trang trong `docs/en` có bản cùng tên trong `docs/vi`. Tiếng Anh là bản đối chiếu kỹ thuật; cập nhật cả hai cùng lúc và giữ liên kết đổi ngôn ngữ. Không dịch identifier, lệnh, đường dẫn hoặc đơn vị. Nhãn UI khớp Android string resource tương ứng; dùng tiếng Việt tự nhiên và thuật ngữ thống nhất.

Tài liệu mô tả hành vi được hỗ trợ, cách thiết lập, hợp đồng kỹ thuật và các bước kiểm tra có thể tái hiện. Dùng ví dụ người đọc có thể thực hiện trong môi trường của mình. Kiểm tra liên kết tương đối, anchor và ví dụ lệnh khi chuyển tệp hoặc đổi tiêu đề.

## Giấy phép

Code và tài liệu InkNest dùng [MIT](../../LICENSE). Giữ thông báo bản quyền/giấy phép bên thứ ba; các thành phần đó giữ điều khoản riêng. Chứng chỉ ký APK độc lập với giấy phép mã nguồn.

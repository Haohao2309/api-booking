# Quy tắc bắt buộc cho AI — Web Booking API

## 1. Trước khi sửa code

- Đọc `README.md`, `docs/API-CONTRACT.md` và `docs/QUY-TRINH-PHAT-TRIEN-CHUC-NANG.md`.
- Kiểm tra `git status` và giữ nguyên mọi thay đổi chưa commit của người dùng.
- Không tự ý xóa, reset, checkout hoặc ghi đè file khi chưa được yêu cầu rõ ràng.
- Tìm code hiện có trước khi tạo entity, migration, DTO, service, controller hoặc endpoint mới.
- Nếu nghiệp vụ, quyền, tiền, ngày tháng hoặc contract chưa rõ, phải ghi giả định và hỏi lại trước phần có rủi ro.

## 2. Kiến trúc bắt buộc

- Luồng chuẩn: Controller → Service → Repository → PostgreSQL.
- Controller chỉ xử lý route, DTO, validation hình thức và HTTP response.
- Service xử lý use case, quyền trên bản ghi, trạng thái, transaction và nghiệp vụ.
- Repository chỉ truy vấn/lưu trữ; không đặt quyết định nghiệp vụ trong repository.
- Không trả trực tiếp JPA entity cho client; dùng request/response DTO.
- Tôn trọng API contract: `{ success, message, data, status }`.
- API danh sách phải phân trang server với `page`, `size`, `totalItems`, `totalPages`.
- Không trả password hash, secret, stack trace, SQL hoặc dữ liệu nội bộ.

## 3. Quy tắc bảo mật và booking

- Quyền phải được kiểm tra ở backend theo role và chủ sở hữu bản ghi.
- Không tin role, userId, tổng tiền hoặc trạng thái do client gửi.
- Thay đổi trạng thái booking phải qua use case rõ ràng; không cho sửa `status` tùy ý.
- Khoảng lưu trú dùng quy ước `[checkIn, checkOut)`.
- Tiền dùng kiểu chính xác như `BigDecimal` và kiểu database `numeric` phù hợp.
- Đặt, đổi, hủy booking phải xử lý transaction, gửi lặp, xung đột và hai người thao tác đồng thời.
- Migration phải có version; không dùng `ddl-auto=update/create` thay cho migration trên dữ liệu thật.
- Không sửa migration đã chạy ở môi trường dùng chung; tạo migration mới.

## 4. Khi triển khai chức năng

1. Mô tả vai trò, mục tiêu, luồng chính và luồng lỗi.
2. Xác định dữ liệu, quyền, trạng thái, API và ảnh hưởng schema.
3. Cập nhật API contract trước khi nối frontend.
4. Tạo DTO/service/controller/repository/migration đúng nhu cầu; không tạo tầng thừa.
5. Viết test cho nghiệp vụ, quyền, lỗi, transaction và hồi quy quan trọng.
6. Cập nhật tài liệu, env và migration khi cần.
7. Chạy kiểm tra phù hợp và báo cáo đúng kết quả thực tế.

## 5. Kiểm tra bắt buộc

```powershell
.\mvnw.cmd --batch-mode verify
git diff --check
```

- Không báo “đã test” nếu chưa chạy lệnh.
- Nếu lệnh thất bại, phải nêu nguyên nhân, phạm vi ảnh hưởng và phần chưa xác minh.
- Trước khi hoàn tất, kiểm tra diff, migration, secret, log nhạy cảm và cấu hình môi trường.

## 6. Quy tắc bàn giao

- Tạo tài liệu trong `docs/changes/YYYY-MM-DD-ten-chuc-nang.md` khi thay đổi không chỉ là chỉnh sửa nhỏ.
- Ghi rõ file đã thêm/sửa/xóa, endpoint, schema, migration, test đã chạy, giới hạn còn lại và cách kiểm tra lại.
- Không tự commit, push, merge hoặc tạo PR nếu người dùng chưa yêu cầu.
- Khi thay đổi cả hai repository, ghi rõ dependency và thứ tự triển khai.

## 7. Cách phản hồi của AI

- Nói rõ đã kiểm tra gì, đã thay đổi file nào và kiểm tra nào đã chạy.
- Không bịa endpoint, bảng database, role, package, kết quả test hoặc trạng thái triển khai.
- Ưu tiên giải pháp nhỏ, rõ ràng, dễ review và phù hợp cấu trúc hiện tại.
- Nếu phát hiện lỗi ngoài phạm vi, không tự sửa; báo lại riêng để người dùng quyết định.

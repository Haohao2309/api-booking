# Quên mật khẩu bằng mã email

## 1. Thông tin

- Ngày: 21/11/2026
- Loại: chức năng mới
- Repository và nhánh: api-booking, `feature/forgot-password` tách từ `main` (`ab3744d`)
- Trạng thái: đã thực hiện local, chưa commit
- Issue/PR: Chưa tạo
- Công việc liên quan: thay cho phần quên mật khẩu trong nhánh `forgot_password`. Nhánh đó build fail (52 lỗi compile ở `RoomController`), trùng version V2/V3 với `main`, và commit merge `6b4859b` đã làm mất logic ở `AuthService`/`AuthController`. Nhánh `forgot_password` được giữ nguyên, không sửa.

## 2. Kết quả đã thực hiện

- Mục đích: khách quên mật khẩu tự đặt lại bằng mã 6 số gửi qua email.
- Trước thay đổi: không có cách đặt lại mật khẩu khi không đăng nhập được.
- Sau thay đổi: 3 bước forgot-password → verify-reset-code → reset-password (xem `docs/API-CONTRACT.md` mục 7).
- Quy tắc:
  - Không lộ email nào đã đăng ký: forgot-password luôn trả cùng một message; mọi lỗi mã trả cùng `INVALID_RESET_CODE`; email không tồn tại vẫn tốn một lần BCrypt để thời gian phản hồi gần tương đương.
  - Mã sinh bằng `SecureRandom`, chỉ lưu hash BCrypt, hiệu lực 10 phút, dùng một lần, khóa sau 5 lần sai. Mã mới vô hiệu hóa mã cũ; không phát mã mới trong vòng 60 giây.
  - Tài khoản bị khóa không nhận mã và không đặt lại được.
  - Đặt lại thành công tăng `security_version`, nên mọi phiên đăng nhập cũ bị hủy ở request kế tiếp. Ghi audit `PASSWORD_RESET_REQUESTED` và `PASSWORD_RESET`.
  - Mọi bước đều khóa hàng `app_users` của tài khoản, nên hai request đồng thời không dùng được cùng một mã hai lần và không lách được bộ đếm lần sai. Lần nhập sai vẫn được commit dù request trả lỗi (`noRollbackFor`).
  - Email gửi sau khi transaction commit, không gọi SMTP trong transaction. SMTP lỗi chỉ ghi log (không ghi địa chỉ người nhận hay mã); người dùng yêu cầu lại sau 60 giây.

## 3. File đã thêm/sửa/xóa/đổi tên

| Repository | Đường dẫn file | Loại thay đổi | Vai trò, nội dung đã đổi và lý do |
| --- | --- | --- | --- |
| api-booking | src/main/resources/db/migration/V13__password_reset_codes.sql | Thêm | Bảng `password_reset_codes` (user_id, code_hash, expires_at, failed_attempts, used_at, created_at) |
| api-booking | src/main/java/com/example/api_booking/repository/PasswordResetRepository.java | Thêm | JDBC đọc/ghi mã, vô hiệu mã cũ, đếm lần sai, đánh dấu đã dùng |
| api-booking | src/main/java/com/example/api_booking/repository/UserRepository.java | Sửa | Thêm `findByEmail` trả `Optional`; `byEmail` cũ ném 401 nên không hợp với luồng công khai |
| api-booking | src/main/java/com/example/api_booking/service/PasswordResetService.java | Thêm | Use case request/verify/reset |
| api-booking | src/main/java/com/example/api_booking/service/PasswordResetMailer.java | Thêm | Gửi mã qua SMTP khi `MAIL_ENABLED=true` |
| api-booking | src/main/java/com/example/api_booking/dto/ForgotPasswordRequest.java, VerifyResetCodeRequest.java, ResetPasswordRequest.java | Thêm | DTO request có validation |
| api-booking | src/main/java/com/example/api_booking/controller/AuthController.java | Sửa | 3 endpoint mới, dùng chung mapping `/api/auth` và `/api/v1/auth` |
| api-booking | src/main/java/com/example/api_booking/config/SecurityConfig.java | Sửa | permitAll cho 6 path POST mới; CSRF vẫn bắt buộc |
| api-booking | pom.xml | Sửa | Thêm `spring-boot-starter-mail` |
| api-booking | src/main/resources/application.properties, .env.example | Sửa | Cấu hình `MAIL_*`; health chỉ kiểm tra SMTP khi bật mail |
| api-booking | src/test/java/com/example/api_booking/PasswordResetTests.java | Thêm | 4 test tích hợp |
| api-booking | docs/API-CONTRACT.md, README.md | Sửa | Contract mục 7 và dòng endpoint trong README |

Tên biến `MAIL_*`, `app.mail.enabled`, `app.mail.from` trùng với phần email của bản triển khai đầy đủ trên `feature/new-main`, để sau này gộp không phải đổi cấu hình.

## 4. Ảnh hưởng API, dữ liệu và cấu hình

- Endpoint: 3 endpoint mới, envelope chuẩn; không có danh sách nên không phân trang.
- Schema: migration mới V13, chỉ thêm bảng, không đụng dữ liệu cũ.
- Config: `MAIL_ENABLED` (mặc định false), `MAIL_FROM`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_AUTH`, `MAIL_STARTTLS`. Khi `MAIL_ENABLED=false` mã vẫn được tạo nhưng không gửi đi, log ghi cảnh báo, nên chưa dùng được với người dùng thật.
- Frontend: `LoginForm` mới có link `#forgot-password`, chưa có màn hình gọi API.

## 5. Kiểm tra đã thực hiện

| Lệnh hoặc kịch bản | Môi trường | Kết quả thực tế | Bằng chứng/ghi chú |
| --- | --- | --- | --- |
| `.\mvnw.cmd --batch-mode verify` | Local, JDK 23 target 21, H2 + Flyway V1–V13 | Đạt | 13 test: 9 cũ + 4 mới trong `PasswordResetTests` |
| `git diff --check` | Local | Đạt | |

4 test mới bao phủ:
- Luồng đầy đủ: phiên cũ bị hủy, mật khẩu cũ không đăng nhập được, mã chỉ dùng một lần, confirm sai trả 400, có audit.
- Email không tồn tại trả cùng message và không gửi mail; thiếu CSRF trả 403; mã sai định dạng trả 400.
- Mã bị khóa sau 5 lần sai.
- Mã hết hạn bị từ chối; không phát mã mới trong 60 giây; mã mới vô hiệu mã cũ.

Chưa chạy: migration V13 trên PostgreSQL thật (CI có bước khởi động jar với PostgreSQL); gửi SMTP thật; test hai request đồng thời.

## 6. Cách chạy hoặc xác minh lại

1. Cấu hình SMTP trong `.env`: `MAIL_ENABLED=true`, `MAIL_FROM`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`.
2. Lấy CSRF qua `GET /api/v1/auth/csrf`, gọi `POST /api/v1/auth/forgot-password` với email đã đăng ký.
3. Lấy mã trong hộp thư, gọi `verify-reset-code` rồi `reset-password`.
4. Mong đợi: đăng nhập bằng mật khẩu mới được; mật khẩu cũ và phiên cũ trả 401; dùng lại mã trả 400.

## 7. Giới hạn và việc còn lại

- Chưa giới hạn theo IP, nên một người vẫn gọi forgot-password được cho nhiều email khác nhau. Cần rate limit chung (README đã ghi là chưa có).
- Email dạng văn bản, chưa dùng bảng `email_templates`.
- Chưa có màn hình frontend.

## 8. Triển khai, rollback và liên kết

- Thứ tự: merge sau `main` hiện tại; không phụ thuộc nhánh khác.
- Migration: V13 tự chạy qua Flyway khi khởi động.
- Rollback: code cũ bỏ qua bảng `password_reset_codes`. Không xóa bảng đã tạo trên môi trường dùng chung; nếu cần thì tạo migration mới.
- Kết quả sau triển khai: Chưa triển khai.

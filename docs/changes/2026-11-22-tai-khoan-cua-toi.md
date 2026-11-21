# Tài khoản của tôi: xem, sửa hồ sơ và đổi mật khẩu

## 1. Thông tin

- Ngày: 22/11/2026
- Loại: chức năng mới (đưa phần đã viết trên `feature/new-main` lên nhánh riêng)
- Repository và nhánh: api-booking, `feature/customer-profile` tách từ `main` (`82c541d`)
- Trạng thái: đã commit local, chưa push

## 2. Kết quả đã thực hiện

- `GET /api/v1/users/me`, `PATCH /api/v1/users/me`, `PUT /api/v1/users/me/password` (contract mục 8 trong `docs/API-CONTRACT.md`).
- Quy tắc:
  - Chỉ thao tác trên tài khoản đang đăng nhập.
  - PATCH phân biệt field không gửi và `null`; số điện thoại là duy nhất.
  - Đổi mật khẩu kiểm tra mật khẩu cũ và tăng `security_version` để hủy mọi phiên.
  - Ghi audit `PROFILE_UPDATED`, `PASSWORD_CHANGED`.

## 3. File đã thêm/sửa

| Repository | Đường dẫn file | Loại thay đổi | Vai trò, nội dung đã đổi và lý do |
| --- | --- | --- | --- |
| api-booking | src/main/java/com/example/api_booking/controller/UserController.java | Thêm | 3 endpoint `/api/v1/users/me` |
| api-booking | src/main/java/com/example/api_booking/dto/ProfileUpdateRequest.java | Thêm | DTO cho PATCH, có cờ "đã gửi" cho từng field |
| api-booking | src/main/java/com/example/api_booking/service/UserService.java | Sửa | Thêm `updateProfile`, `changePassword` (phần đã tách khỏi nhánh nền) |
| api-booking | src/test/java/com/example/api_booking/ProfileApiTests.java | Thêm | 2 test tích hợp |
| api-booking | docs/API-CONTRACT.md | Sửa | Mục 8 |

## 4. Ảnh hưởng API, dữ liệu và cấu hình

- Không có migration mới: cột `phone`, `full_name`, `security_version` đã có từ V2/V3.
- Không đổi env, dependency hay SecurityConfig: `/api/v1/users/me` thuộc nhóm cần đăng nhập.

## 5. Kiểm tra đã thực hiện

| Lệnh | Môi trường | Kết quả | Ghi chú |
| --- | --- | --- | --- |
| `.\mvnw.cmd --batch-mode verify` | Local, H2 + Flyway V1–V13 | Đạt | 15 test, 2 test mới |
| `git diff --check` | Local | Đạt | |

Test mới bao phủ:
- Chưa đăng nhập trả 401.
- PATCH từng field; email/role gửi trong body bị bỏ qua; `phone: null`; fullName blank; body rỗng; phone sai định dạng; phone trùng trả 409; thiếu CSRF trả 403.
- Đổi mật khẩu với mật khẩu cũ sai, xác nhận sai, mật khẩu quá ngắn; khi thành công thì phiên cũ trả 401 và mật khẩu cũ không đăng nhập được.

Chưa chạy trên PostgreSQL thật.

## 6. Cách xác minh lại

Đăng nhập, gọi `GET`/`PATCH /api/v1/users/me`, đổi mật khẩu, rồi kiểm tra phiên cũ bị đăng xuất.

## 7. Giới hạn

- Đăng ký công khai chưa nhận số điện thoại (thiết kế WBO có trường này).
- Chưa xác minh số điện thoại.

## 8. Triển khai

Merge sau `main` hiện tại, không cần migration. Rollback bằng revert.

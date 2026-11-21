# Catalog công khai: tìm phòng, chi tiết phòng, dịch vụ, gợi ý địa điểm

## 1. Thông tin

- Ngày: 22/11/2026
- Loại: chức năng mới (đưa phần đã viết trên `feature/new-main` lên nhánh riêng)
- Repository và nhánh: api-booking, `feature/customer-catalog`, nối tiếp `feature/customer-profile`
- Trạng thái: đã commit local, chưa push

## 2. Kết quả đã thực hiện

- `GET /api/v1/rooms`, `GET /api/v1/rooms/{id}`, `GET /api/v1/services`, `GET /api/v1/suggestions` (contract mục 9 trong `docs/API-CONTRACT.md`).
- Chỉ phần công khai. Các API quản trị phòng (`/api/v1/admin/rooms/**`) thuộc nhánh catalog của nhóm quản lý, chưa có ở đây.
- Quy tắc:
  - Không cần đăng nhập.
  - Lọc và đếm ở database rồi mới phân trang.
  - Phòng bị khóa hoặc thuộc loại phòng ngừng bán không hiện.
  - Tìm theo ngày loại phòng có đêm đã giữ/bán trong `[checkIn, checkOut)`.
  - Giá/sức chứa lấy từ loại phòng.

## 3. File đã thêm/sửa

| Repository | Đường dẫn file | Loại thay đổi | Vai trò, nội dung đã đổi và lý do |
| --- | --- | --- | --- |
| api-booking | src/main/java/com/example/api_booking/controller/RoomController.java | Thêm | `GET /rooms`, `GET /rooms/{id}` |
| api-booking | src/main/java/com/example/api_booking/service/RoomService.java | Thêm | Tìm phòng, chi tiết công khai; bản trên `feature/new-main` bỏ phần quản trị |
| api-booking | src/main/java/com/example/api_booking/controller/ServiceController.java, SuggestionController.java | Thêm | `GET /services`, `GET /suggestions` |
| api-booking | src/main/java/com/example/api_booking/service/ServiceService.java, SuggestionService.java | Thêm | Kiểm tra `hotelId`, gọi repository |
| api-booking | src/main/java/com/example/api_booking/config/SecurityConfig.java | Sửa | Cho phép công khai 4 đường GET |
| api-booking | src/test/java/com/example/api_booking/CatalogApiTests.java | Thêm | 4 test tích hợp |
| api-booking | docs/API-CONTRACT.md | Sửa | Mục 9 |

Repository, entity, DTO và migration đã có trên `main` từ nhánh nền, không đổi.

## 4. Ảnh hưởng API, dữ liệu và cấu hình

- Không có migration, env hay dependency mới.
- Chưa có API quản trị để nhập khách sạn/loại phòng/phòng/dịch vụ/gợi ý: dữ liệu phải nạp bằng SQL có kiểm soát cho tới khi nhánh quản lý được merge. Database trống thì các danh sách trả rỗng (200).

## 5. Kiểm tra đã thực hiện

| Lệnh | Môi trường | Kết quả | Ghi chú |
| --- | --- | --- | --- |
| `.\mvnw.cmd --batch-mode verify` | Local, H2 + Flyway V1–V13 | Đạt | 19 test, 4 test catalog mới |
| `git diff --check` | Local | Đạt | |

Test mới bao phủ:
- Truy cập không cần đăng nhập.
- Ẩn phòng khóa và loại phòng ngừng bán; giá theo loại phòng.
- Loại phòng có đêm đã bán; ngày trả trùng đêm đã bán vẫn hợp lệ theo `[checkIn, checkOut)`.
- Lọc số khách, giá; phân trang và tổng số.
- 8 trường hợp tham số sai trả 400.
- Chi tiết có ảnh/tiện ích, phòng khóa trả 404.
- Dịch vụ/gợi ý chỉ lấy mục đang hoạt động của đúng khách sạn.

Chưa chạy trên PostgreSQL thật.

## 6. Cách xác minh lại

Nạp một khách sạn, loại phòng, phòng, dịch vụ, gợi ý; gọi các endpoint ở mục 2 không kèm cookie.

## 7. Giới hạn

- Kết quả trả theo **phòng vật lý**, trong khi thiết kế WBO hiển thị theo **loại phòng** ("3 loại phòng phù hợp · còn bán 3 phòng"). Muốn đúng thiết kế cần thêm API tìm theo loại phòng có đếm phòng trống; cần chốt trước khi làm.
- DTO phòng chưa có tên khách sạn, mô tả loại phòng, giờ nhận/trả phòng.
- Chưa có danh sách khách sạn công khai (`GET /hotels`).

## 8. Triển khai

Merge sau `feature/customer-profile`. Rollback bằng revert.

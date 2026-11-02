# Booking API — nền tảng backend

Spring Boot 4.1.1, Java target 21, Maven Wrapper, PostgreSQL. Repository frontend: https://github.com/Haohao2309/web-booking.

## Quy chuẩn và tài liệu bàn giao

- [Quy trình phát triển View–Model–Controller](docs/QUY-TRINH-PHAT-TRIEN-CHUC-NANG.md).
- [Hợp đồng response và phân trang](docs/API-CONTRACT.md): quy chuẩn mục tiêu `{success, message, data, status}`; mọi danh sách phân trang server. Runtime hiện chưa chuyển đổi sang hợp đồng này.
- [Mẫu bàn giao chức năng](docs/templates/FEATURE-REPORT.md).
- [Bản ghi đồng bộ quy chuẩn](docs/changes/2026-11-02-quy-chuan-va-tai-lieu-ban-giao.md).

Bắt buộc sau mỗi chức năng/sửa lỗi/refactor/cấu hình/tài liệu: có bản ghi trong `docs/changes/` trước khi bàn giao hoặc gửi PR review. Ghi hành vi đã làm, file thêm/sửa/xóa/đổi tên và lý do, ảnh hưởng, kiểm tra thực tế, giới hạn, cách xác minh và triển khai. PR phải dẫn link tới bản ghi. Không dùng commit message thay thế tài liệu.

## Phạm vi hiện tại

Đã có cấu hình database, session cookie HttpOnly, CSRF, CORS allowlist, rule vai trò, health, validation error handler, kiểm thử và GitHub Actions. Chưa có entity/repository/controller nghiệp vụ đăng nhập, đăng ký, phòng, đặt phòng, hóa đơn hoặc thanh toán. Không có tài khoản mặc định và không cung cấp API giả báo thành công. UserDetailsService hiện từ chối tất cả tài khoản cho tới khi có xác thực qua database.

## Chuẩn bị

- JDK 21 trở lên, tương thích Spring Boot 4.1.1; CI dùng Temurin 21. Máy phát triển hiện có JDK 23 và đã build với release 21. Nếu JAVA_HOME đang trỏ JDK 17/8, đổi về JDK 21+.
- Maven Wrapper đi kèm, không cần cài Maven riêng; lần đầu cần mạng để tải dependency.
- Docker Desktop chạy Linux containers, hoặc PostgreSQL riêng.
- Cổng backend 8080, frontend 5173, database Docker 5433 trên localhost. Cổng 5433 tránh đụng PostgreSQL đang cài trên máy ở 5432.

## Chạy local trên Windows

Trong thư mục có pom.xml:

```powershell
Copy-Item .env.example .env
# Sửa DB_PASSWORD trong .env thành mật khẩu local riêng.
# Chỉ khi JAVA_HOME chưa đúng; thay đường dẫn bằng JDK trên máy:
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-23'
.\scripts\start-local.ps1
```

Script đọc .env như dữ liệu KEY=value (không thực thi nội dung), khởi động PostgreSQL bằng docker compose up -d --wait, rồi chạy Maven. Dùng giá trị không bọc dấu nháy và không thêm comment cuối dòng. .env được gitignore; không commit mật khẩu. Docker Compose tự đọc .env, nhưng Spring Boot không tự đọc file này; script nạp các biến vào process để Spring dùng.

Nếu dùng database riêng: tạo database/user trước, sửa DB_URL/DB_USERNAME/DB_PASSWORD và chạy `./scripts/start-local.ps1 -SkipDatabase`. Không trỏ test hoặc setup vào database production.

Trên Linux/macOS: export các biến DB_URL, DB_USERNAME, DB_PASSWORD (và các tùy chọn cần thiết), chạy `docker compose up -d --wait postgres` nếu dùng Compose, rồi `bash ./mvnw spring-boot:run`. Các lệnh Maven bên dưới dùng `bash ./mvnw` thay `./mvnw.cmd`.

## Endpoint nền tảng

| Endpoint | Kết quả |
| --- | --- |
| GET /api/health | 200 với status UP nếu PostgreSQL sẵn sàng; không lộ chi tiết database |
| GET /api/auth/csrf | 200 với headerName X-CSRF-TOKEN và token; tạo session khi cần |
| GET /api/auth/me | 401 khi chưa xác thực; controller tài khoản chưa được triển khai |
| POST /api/auth/login hoặc register | 403 nếu thiếu/sai CSRF; controller nghiệp vụ chưa triển khai |

Frontend gọi GET /api/auth/csrf với credentials trước mỗi mutation, giữ cookie phiên và gửi token JSON trong X-CSRF-TOKEN. Token gắn với session, không dùng được với session khác. Giữ Spring Security XOR/BREACH handling mặc định; không dùng XSRF-TOKEN cookie đọc bằng JavaScript. CSRF token không phải access token.

CORS chỉ cho các origin trong FRONTEND_ORIGINS, phân cách dấu phẩy. Mặc định http://localhost:5173. Vite frontend proxy /api tới http://localhost:8080. Production nên reverse proxy cùng origin, đặt SESSION_COOKIE_SECURE=true qua HTTPS; xác định lại SameSite nếu triển khai khác site. Không tắt CSRF để xử lý lỗi CORS.

## Phân quyền nền tảng

- Công khai: health, CSRF, GET rooms; auth login/register cho phép qua authorization nhưng vẫn yêu cầu CSRF.
- ADMIN: tạo/sửa/xóa phòng và /api/admin/**.
- ADMIN/RECEPTIONIST: danh sách toàn bộ booking, confirm/check-in/check-out.
- CUSTOMER: tạo booking.
- Các request còn lại yêu cầu xác thực.

Các rule trên là hàng rào HTTP, chưa phải nghiệp vụ hoàn chỉnh. Khi bổ sung service phải kiểm tra chủ đơn cho GET booking theo ID/hủy đơn, kiểm soát includeInactive cho phòng và giới hạn dữ liệu trả về. Tạo user CUSTOMER ở đăng ký công khai; không nhận role từ client. Xác thực phải đổi session ID khi login, lưu SecurityContext và vô hiệu session khi logout. Chưa có endpoint logout ở bước setup.

## Database và cấu hình

DB_URL mặc định jdbc:postgresql://localhost:5433/booking; DB_USERNAME mặc định booking; DB_PASSWORD bắt buộc. PORT mặc định 8080. SESSION_COOKIE_SECURE=false chỉ phù hợp local HTTP. Session hết hạn sau 30 phút không hoạt động; HttpOnly và SameSite=Lax.

JPA dùng ddl-auto=validate và open-in-view=false. Hiện chưa có bảng/entity nghiệp vụ, không tự tạo schema bằng Hibernate. Khi thêm model cần migration có version (ví dụ Flyway), unique constraints, transaction/lock để chống đặt trùng và audit log cho nhân viên. Không dùng ddl-auto=create/update trên database thật.

Docker volume booking_postgres giữ dữ liệu khi dừng container. `docker compose stop` dừng PostgreSQL, không xóa volume. Không cần xóa volume để restart; thay POSTGRES_PASSWORD không tự đổi password trong volume đã khởi tạo.

## Kiểm tra và CI

```powershell
.\mvnw.cmd --batch-mode verify
```

Test dùng H2 riêng với PostgreSQL compatibility mode, không cần Docker hoặc DB_PASSWORD. Kiểm tra health không lộ dữ liệu, JSON 401, CSRF round trip/session isolation, CORS allowlist và phân quyền admin/lễ tân/khách bằng controller chỉ tồn tại trong test. H2 không thay thế kiểm thử PostgreSQL về SQL/transaction.

GitHub Actions chạy verify trên Java 21; sau đó chạy jar thật với PostgreSQL 17 service và chờ /api/health UP. GitHub Actions không publish/deploy ứng dụng.

Tài liệu tham chiếu: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html (CSRF endpoint và SPA), https://docs.spring.io/spring-boot/system-requirements.html.

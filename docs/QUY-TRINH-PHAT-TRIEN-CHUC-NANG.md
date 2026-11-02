# Quy chuẩn phát triển chức năng — Room / Stay

Áp dụng cho frontend React `web-booking` và backend Spring Boot `api-booking`, với ba vai trò `ADMIN`, `CUSTOMER`, `RECEPTIONIST`.

Đây là quy chuẩn đề xuất để áp dụng từ các PR tiếp theo. Tài liệu không khẳng định toàn bộ kiến trúc, công cụ hoặc nghiệp vụ dưới đây đã được triển khai. Backend hiện mới có nền tảng database, security, health, CSRF và CI; các API nghiệp vụ còn cần phát triển.

## 1. Mục tiêu và cách sử dụng

Mỗi chức năng phải trả lời được bảy câu hỏi trước khi hoàn tất:

1. Ai sử dụng, để đạt kết quả gì?
2. Được phép làm gì, trên dữ liệu của ai, trong trạng thái nào?
3. Dữ liệu nào được tạo hoặc thay đổi, có ảnh hưởng dữ liệu cũ không?
4. API nào cung cấp chức năng và frontend sử dụng như thế nào?
5. Nếu thao tác thất bại, bị lặp, hoặc có người thao tác đồng thời thì xử lý ra sao?
6. Bằng chứng nào cho thấy hành vi đúng và không làm hỏng chức năng cũ?
7. Triển khai, theo dõi và quay lui bằng cách nào?

Mỗi hạng mục trong checklist phải có kết quả hoặc ghi “Không áp dụng” kèm lý do. Không cần tạo entity, migration, service hoặc bộ test mới nếu thay đổi thực tế không cần chúng. Quy trình đầy đủ không có nghĩa mọi sửa đổi đều phải chạm tất cả tầng.

## 2. Kiến trúc và trách nhiệm

Trong hai project tách rời, React đảm nhiệm View. Model của backend gồm dữ liệu và nghiệp vụ, không chỉ entity JPA. Controller tiếp nhận REST request và giao tiếp với lớp nghiệp vụ.

Luồng chuẩn:

```text
Người dùng
  → React Page / Form
  → Hook / API client
  → HTTP request
  → Security filter: phiên, CSRF, quyền HTTP
  → REST Controller: request DTO, validation hình thức
  → Service: quyền trên bản ghi, quy tắc nghiệp vụ, transaction
  → Repository: truy vấn, khóa/ràng buộc cần thiết
  → PostgreSQL
  → Response DTO
  → React hiển thị kết quả
```

| Thành phần | Chịu trách nhiệm | Không đặt tại đây |
| --- | --- | --- |
| View: page/component | Hiển thị, nhận thao tác, kiểm tra đầu vào hỗ trợ người dùng, trạng thái giao diện | SQL, mật khẩu hệ thống, quyền quyết định cuối cùng, tính giá cuối cùng |
| Hook | Điều phối dữ liệu và trạng thái có thể tái sử dụng | Một bản sao riêng của quy tắc nghiệp vụ backend |
| API client | URL, HTTP method, credentials, CSRF, chuẩn hóa lỗi | JSX, điều hướng của mọi màn hình, quyết định quyền nghiệp vụ |
| Controller | Route, đọc DTO, gọi service, trả HTTP status/response | Transaction nghiệp vụ dài, truy vấn DB trực tiếp, tự tính tiền |
| Service | Use case, điều kiện nghiệp vụ, quyền theo bản ghi, transaction | Chi tiết giao diện, đối tượng HTTP khi không cần |
| Entity/value object | Biểu diễn dữ liệu, quan hệ, các bất biến phù hợp của đối tượng | Trả trực tiếp cho client, đọc request hoặc session HTTP |
| Repository | Truy vấn, phân trang, thao tác lưu trữ và lock | Xác định khách có được hủy đơn hay không |
| Database | Ràng buộc dữ liệu, tính nhất quán, index | Thay thế toàn bộ validation và thông báo nghiệp vụ |

Không cần tạo interface cho mọi service hoặc repository tùy biến chỉ để đủ số lớp. Ưu tiên cấu trúc rõ ràng, ít tầng thừa.

## 3. Phân loại công việc trước khi bắt đầu

| Loại thay đổi | Ví dụ trong dự án | Việc phải chú ý |
| --- | --- | --- |
| Đối tượng nghiệp vụ mới | Loại phòng, tiện nghi, dịch vụ phát sinh | Có thật sự cần entity/bảng mới? Quan hệ, vòng đời, quyền, migration |
| Mở rộng đối tượng đã có | Thêm diện tích phòng hoặc số điện thoại khách | Dữ liệu cũ, default/null, DTO, tương thích API |
| Use case mới trên đối tượng cũ | Hủy booking, gia hạn lưu trú | Điều kiện trước/sau, trạng thái, transaction, quyền trên bản ghi |
| Thay đổi quy tắc đang có | Thay cách tính phí hủy | Hiệu lực từ khi nào, ảnh hưởng đơn cũ, cách kiểm thử |
| Sửa lỗi | Hai người đặt trùng một phòng | Tái hiện, nguyên nhân gốc, dữ liệu đã sai, test hồi quy |
| Chỉ sửa View | Bố cục danh sách phòng | Responsive, accessibility, không làm đổi hợp đồng API vô ý |
| Refactor | Tách logic đặt phòng khỏi controller | Giữ nguyên hành vi, tránh trộn với đổi nghiệp vụ |
| Hạ tầng/cấu hình | Đổi CORS, timeout, pipeline | Các môi trường, secret, kiểm tra runtime và rollback cấu hình |

Một công việc có thể thuộc nhiều loại. Ví dụ thêm dịch vụ vào booking vừa tạo đối tượng mới, vừa thêm use case và thay đổi giá.

## 4. Giai đoạn 1 — Viết yêu cầu và tiêu chí nghiệm thu

### 4.1. Mô tả use case

Tạo issue hoặc bản mô tả gồm:

- Tên chức năng và vấn đề cần giải quyết.
- Vai trò sử dụng, mục tiêu và điểm bắt đầu thao tác.
- Điều kiện trước khi thực hiện.
- Luồng chính theo thứ tự.
- Luồng thay thế: không có dữ liệu, đầu vào sai, không đủ quyền, xung đột, mất mạng.
- Kết quả thành công và dữ liệu/trạng thái sau thao tác.
- Phạm vi bao gồm và phần để sau.
- Các điều kiện nghiệm thu có thể kiểm chứng.

Ví dụ “Khách hàng hủy đặt phòng”:

```text
Người thực hiện: CUSTOMER.
Điều kiện: đã đăng nhập, booking thuộc khách hiện tại.
Trạng thái cho phép: theo chính sách được chốt, ví dụ PENDING/CONFIRMED.
Thao tác: mở chi tiết → chọn Hủy → xác nhận.
Thành công: booking chuyển CANCELLED, có thời điểm/người hủy.
Từ chối: đơn người khác; đã check-in; quá hạn hủy; trạng thái đã đổi.
Chưa bao gồm: hoàn tiền tự động nếu chưa có tích hợp thanh toán.
```

Không tự coi ví dụ trên là chính sách kinh doanh đã được chốt. Thời hạn hủy, tiền cọc và quyền nhân viên phải được xác định riêng.

### 4.2. Ma trận quyền

Không chỉ ghi “admin có toàn quyền”. Ghi cụ thể theo thao tác và phạm vi dữ liệu:

| Thao tác | CUSTOMER | RECEPTIONIST | ADMIN | Điều kiện bổ sung |
| --- | --- | --- | --- | --- |
| Xem phòng công khai | Có | Có | Có | Không lộ dữ liệu vận hành nội bộ |
| Xem booking | Đơn của mình | Theo phạm vi được giao | Theo chính sách quản trị | Kiểm tra trên server |
| Tạo/sửa phòng | Không | Theo yêu cầu thực tế | Có | Không phá booking đang hiệu lực |
| Check-in/out | Không | Có | Có nếu được cấp | Đúng trạng thái và thời gian nghiệp vụ |

Bảng này là mẫu để chốt cho từng chức năng; không phải toàn bộ quyền sản phẩm đã được phê duyệt.

### 4.3. Dữ liệu, thời gian và tiền

- Liệt kê field, kiểu, bắt buộc/tùy chọn, giới hạn và dữ liệu nhạy cảm.
- Ngày lưu trú dùng quy ước rõ ràng; phân biệt ngày nhận phòng với thời điểm thực tế nhận phòng.
- Thống nhất múi giờ cơ sở lưu trú và cách lưu timestamp; không dùng ngày UTC thay ngày địa phương một cách ngầm định.
- Định nghĩa khoảng lưu trú, ví dụ `[checkIn, checkOut)` để hai booking có thể nối tiếp tại ngày trả/nhận.
- Tiền dùng kiểu chính xác ở backend/database; quy định đơn vị VND và cách làm tròn. Java có thể dùng BigDecimal, PostgreSQL dùng numeric theo miền dữ liệu đã chọn.
- Giá đã chốt trong đơn cần giữ được lịch sử; đổi giá phòng hiện tại không được tự thay tổng tiền đơn cũ.

### 4.4. Đầu ra bắt buộc

Trước khi code phải có mô tả use case, tiêu chí nghiệm thu, ma trận quyền và những quyết định nghiệp vụ ảnh hưởng dữ liệu/API. Điểm chưa rõ có thể ghi giả định; điểm quyết định tiền, quyền hoặc dữ liệu không thể suy ra thì cần được chốt trước phần phụ thuộc.

## 5. Giai đoạn 2 — Khảo sát ảnh hưởng và chuẩn bị Git

1. Đọc README, AGENTS.md nếu có và cấu hình hiện hành của cả hai repository.
2. Kiểm tra code chưa commit; giữ nguyên công việc khác của người dùng.
3. Fetch remote, kiểm tra PR phụ thuộc và chọn base đã chứa các điều kiện cần.
4. Tìm màn hình, endpoint, DTO, service, entity, truy vấn và test liên quan; không tạo trùng đối tượng đã có.
5. Xác định dữ liệu cũ nào sẽ chịu ảnh hưởng, client nào đang dùng endpoint, báo cáo/tác vụ nền nào liên quan.
6. Xác định rủi ro: đổi schema, quyền, tiền, concurrency, external API hoặc thay đổi hành vi cũ.
7. Tạo nhánh từ base đúng, mỗi nhánh có mục đích rõ ràng.

Quy ước nhánh theo yêu cầu của dự án:

```text
quyeen/room-type-api
quyeen/room-type-view
quyeen/booking-cancel-api
quyeen/booking-cancel-view
quyeen/fix-booking-overlap
```

Hai repository thường cần hai nhánh và hai PR. Không cố giữ số PR giống nhau; GitHub đánh số độc lập theo repository. PR phải dẫn link tới PR còn lại và ghi thứ tự merge/deploy.

## 6. Giai đoạn 3 — Thiết kế hợp đồng API trước khi triển khai

### 6.1. Nội dung hợp đồng

Mỗi endpoint ghi rõ:

- HTTP method, path, query/path parameters.
- Request DTO: field, kiểu, format, required/null/default.
- Response DTO và HTTP status khi thành công.
- Quyền vai trò, quyền theo chủ sở hữu và trạng thái.
- Lỗi có thể xảy ra, mã lỗi và điều kiện sinh lỗi.
- Transaction, thao tác đồng thời và cơ chế chống tạo lặp nếu cần.
- Ảnh hưởng tương thích với frontend đang chạy.

Ưu tiên tài liệu OpenAPI khi bắt đầu xây API nghiệp vụ; nếu chưa có công cụ thì ghi hợp đồng rõ trong tài liệu cùng PR. Không tự sinh đặc tả rồi mặc định nó đúng: phải đối chiếu request/response thực tế.

### 6.2. Quy ước REST

| Mục đích | Mẫu |
| --- | --- |
| Danh sách | `GET /api/room-types` |
| Chi tiết | `GET /api/room-types/{id}` |
| Tạo | `POST /api/room-types` → 201 và Location khi phù hợp |
| Cập nhật một phần | `PATCH /api/room-types/{id}` |
| Xóa nếu nghiệp vụ cho phép | `DELETE /api/room-types/{id}` → 200 với envelope và data: null |
| Thao tác vòng đời | `POST /api/bookings/{id}/cancel` |

Thao tác thay đổi trạng thái có quy tắc riêng nên đi qua use case rõ ràng. Không cung cấp API tùy ý ghi `status` nếu việc đó cho phép bỏ qua xác nhận/check-in/check-out.

GET không được tạo/hủy booking hoặc thay đổi dữ liệu nghiệp vụ. Không gửi JSON body trong GET nếu query/path đã biểu diễn được nhu cầu.

### 6.3. Response và lỗi

- Quy chuẩn đã chốt: mọi JSON API ứng dụng bọc `{success, message, data, status}`. Xem [hợp đồng chi tiết](API-CONTRACT.md).
- `status` là số và khớp HTTP status; `success` true với 2xx, false với 4xx/5xx. Không trả 200 cho một lỗi nghiệp vụ.
- Kết quả nằm trong data; thao tác không có dữ liệu trả 200 với data: null, không dùng 204 kèm body.
- Mọi API danh sách phải phân trang phía server: data có `{items, page, size, totalItems, totalPages}`. page bắt đầu từ 0, mặc định size 20, tối đa 100.
- Validate page/size/sort, áp dụng quyền và filter trước phân trang/count. View phải dùng metadata server, không lọc riêng một trang để giả lập tìm toàn bộ dữ liệu.
- Khi đổi filter/sort/size, reset page về 0. Các danh mục cho dropdown cũng dùng tìm kiếm/phân trang khi cần.
- Response DTO chỉ chứa dữ liệu client cần; không trả password hash, dữ liệu tài khoản nội bộ hoặc quan hệ entity vô hạn.

Mẫu lỗi theo quy chuẩn mới:

```json
{
  "success": false,
  "message": "Đặt phòng đã đổi trạng thái. Vui lòng tải lại.",
  "data": { "code": "BOOKING_STATE_CONFLICT" },
  "status": 409
}
```

`data.code` ổn định để client xử lý; `message` phù hợp cho người dùng; `data.fieldErrors` chỉ có khi cần, `data.traceId` chỉ thêm khi server có mã đối chiếu thật. Runtime hiện vẫn theo contract cũ, nên cần chuyển đổi đồng bộ controller, security handler, Axios, list UI, test và CI. Không trả stack trace/SQL.

Quy ước status: 400 cho request/validation sai, 401 chưa xác thực, 403 thiếu quyền/CSRF không hợp lệ, 404 không tìm thấy, 409 xung đột trạng thái/trùng dữ liệu, 500 lỗi hệ thống. Thống nhất 400 cho validation ở giai đoạn này. Với bản ghi không thuộc người dùng, chọn chính sách 403 hoặc 404 nhất quán để tránh lộ sự tồn tại của dữ liệu.

### 6.4. Tương thích giữa hai project

- Thêm field tùy chọn thường dễ tương thích hơn đổi tên hoặc đổi kiểu field.
- Không bỏ field hoặc đổi ý nghĩa API khi frontend cũ còn dùng.
- Khi đổi contract phá vỡ tương thích, có kế hoạch chuyển tiếp/version theo nhu cầu thực tế.
- Không tạo API version mới cho mọi thay đổi nhỏ.
- Quyết định thứ tự triển khai dựa trên tương thích, không chỉ thứ tự merge PR.

## 7. Quy chuẩn Model — dữ liệu và nghiệp vụ backend

### 7.1. Khi có đối tượng mới

1. Định nghĩa khái niệm: đối tượng có ID/vòng đời riêng hay chỉ là thuộc tính/value object?
2. Tìm đối tượng tương tự hiện có để tránh tên khác nhưng cùng nghĩa.
3. Chọn quan hệ: một-nhiều, nhiều-nhiều, bắt buộc/tùy chọn; ai sở hữu quan hệ?
4. Định nghĩa bất biến: tên/mã không trùng, giá không âm, ngày hợp lệ, trạng thái hợp lệ.
5. Xác định có cần bảng riêng, bảng liên kết, lịch sử thay đổi hoặc dữ liệu danh mục hay không.
6. Thiết kế khóa chính, foreign key, unique/check/not-null constraints và index theo truy vấn thực tế.
7. Xác định hành vi xóa, ngừng hoạt động và ảnh hưởng dữ liệu tham chiếu.
8. Tạo migration nếu schema/dữ liệu khởi tạo thay đổi.
9. Tạo entity, request/response DTO, repository và service đúng nhu cầu.
10. Test các bất biến và use case, không chỉ test getter/setter.

Ví dụ loại phòng đang được phòng/booking tham chiếu: cần xác định ngừng sử dụng thay vì xóa mất tham chiếu. Không áp dụng soft delete cho mọi bảng theo thói quen; chọn theo yêu cầu giữ lịch sử và vận hành.

### 7.2. Migration

- Dùng migration có version khi bắt đầu tạo model nghiệp vụ; bổ sung công cụ migration và CI trong PR nền tảng schema đầu tiên.
- Không dùng `ddl-auto=update/create` để thay migration trên môi trường có dữ liệu thật.
- Không sửa migration đã áp dụng ở môi trường dùng chung; tạo migration mới.
- Kiểm tra cả database trống và database phiên bản trước có dữ liệu đại diện.
- Với field bắt buộc mới: thường thêm nullable/default phù hợp → backfill → kiểm tra → thêm NOT NULL ở bước phù hợp.
- Không gán số liệu giả cho dữ liệu cũ chỉ để migration chạy, ví dụ tự gán diện tích 0 cho tất cả phòng.
- Đánh giá thời gian chạy và lock khi backfill/tạo index trên bảng lớn.
- Xác định backup/restore hoặc migration bù; không mặc định có thể “undo” mọi migration.

### 7.3. DTO và validation

- Dùng DTO riêng cho tạo/cập nhật/response khi trường hoặc quyền khác nhau.
- Chỉ nhận field được phép chỉnh sửa; không bind trực tiếp request vào entity.
- Bean Validation kiểm tra hình thức: required, độ dài, số dương, format.
- Service kiểm tra nghiệp vụ: phòng còn kinh doanh, đủ sức chứa, đơn thuộc khách, chuyển trạng thái hợp lệ.
- Database giữ ràng buộc cuối cùng như unique/foreign key/check.
- Với PATCH phải phân biệt “không gửi field” và “gửi null” khi ý nghĩa khác nhau.
- Tiền cuối cùng, chủ tài khoản và role phải do server quyết định; không tin field client tự gửi.

### 7.4. Service và transaction

- Mỗi use case quan trọng có điểm vào service rõ ràng, ví dụ `cancelBooking` hoặc `checkIn`.
- Transaction bao trùm tập thao tác phải thành công/thất bại cùng nhau.
- Đọc danh sách/chi tiết có thể dùng transaction read-only khi cần; không mở transaction không cần thiết.
- Không giữ khóa DB trong lúc chờ cổng thanh toán/email/network lâu.
- Thiết kế gửi thông báo hoặc tích hợp ngoài sau commit; dùng outbox/retry khi yêu cầu độ tin cậy thực tế cần.
- Khi cùng tài nguyên có thể bị cập nhật đồng thời, chọn optimistic lock, pessimistic lock hoặc ràng buộc DB phù hợp.
- Bắt xung đột và chuyển thành lỗi nghiệp vụ có thể xử lý, thay vì trả 500 chung.

Đặt phòng cần xử lý đặc biệt: kiểm tra phòng trống rồi insert đơn trong hai bước độc lập có thể tạo trùng. Cả đường tạo, đổi ngày, đổi phòng và gia hạn đều phải đi qua cùng cơ chế bảo vệ. Nếu lock theo bản ghi phòng thì mọi đường ghi liên quan phải tuân thủ; nếu dùng exclusion constraint thì cần thiết kế khoảng ngày/trạng thái và migration phù hợp. Chọn một chiến lược được kiểm thử bằng hai request đồng thời trên PostgreSQL thật.

Với tạo đơn hoặc thanh toán có nguy cơ lặp do mất mạng, cân nhắc idempotency key: khóa gắn người gọi/use case, ràng buộc duy nhất, kiểm tra cùng payload và có thời hạn lưu. Khóa nút frontend không thay thế cơ chế này.

### 7.5. Repository và truy vấn

- Chỉ tải dữ liệu được phép xem; ưu tiên truy vấn theo cả ID và owner khi phù hợp.
- Dùng pagination/filter/sort phía server cho danh sách lớn.
- Phát hiện N+1, fetch quan hệ cần dùng và tránh trả lazy proxy ra API.
- Index theo điều kiện lọc/join/sort thực tế; không thêm index cho mọi cột.
- Không ghép SQL từ đầu vào người dùng; tham số hóa truy vấn và allowlist sort field.

## 8. Quy chuẩn Controller và security

Controller phải ngắn, thể hiện luồng request → service → response:

1. Khai báo endpoint và HTTP method rõ ràng.
2. Đọc path/query/body vào kiểu dữ liệu và DTO thích hợp.
3. Kích hoạt validation.
4. Lấy danh tính đã xác thực từ security context qua cách thống nhất; không nhận customerId đáng tin từ browser.
5. Gọi service thực hiện use case.
6. Trả response DTO và status đúng hợp đồng.
7. Để exception handler tập trung chuyển lỗi nghiệp vụ sang HTTP response.

Quyền cần hai lớp bổ trợ nhau:

- Security filter/method security xác định ai được gọi nhóm thao tác.
- Service/query kiểm tra người đó có được thao tác trên bản ghi cụ thể, ở trạng thái hiện tại hay không.

Với nền tảng session hiện tại:

- Giữ CSRF cho thao tác ghi; frontend lấy token qua `/api/auth/csrf` và gửi `X-CSRF-TOKEN`.
- CORS dùng origin cụ thể cùng credentials; CORS không thay thế xác thực/phân quyền.
- Khi triển khai auth, đổi session ID khi login, lưu SecurityContext đúng cách và vô hiệu phiên khi logout.
- Đăng ký công khai chỉ tạo CUSTOMER; việc cấp quyền nhân viên phải có luồng riêng.
- Không log mật khẩu, cookie phiên, CSRF token hoặc dữ liệu giấy tờ nhạy cảm.
- Các thao tác thay role, giá, trạng thái lưu trú hoặc hủy đơn cần xác định dữ liệu audit: ai, làm gì, khi nào, đối tượng nào và lý do khi cần.

Không dùng catch mọi Exception rồi trả 200 với `success:false`. Không để người dùng phải phân tích chuỗi lỗi để biết HTTP request đã thất bại.

## 9. Quy chuẩn View — React

### 9.1. Cấu trúc theo feature

Mẫu tổ chức đề xuất; giữ cách đặt tên hiện có hoặc refactor riêng nếu đổi:

```text
src/
  api/                 # client chung và adapter API
  components/          # thành phần dùng chung
  features/
    room-types/
      pages/
      components/
      hooks/           # chỉ thêm khi cần logic riêng/tái sử dụng
  hooks/
  routes/
  store/
  utils/
```

- Page điều phối màn hình; component biểu diễn phần giao diện có ý nghĩa.
- Dùng API client chung; không viết một Axios/fetch riêng thiếu cookie/CSRF trong component mới.
- State form/UI để cục bộ; chỉ đưa vào global store khi nhiều phần ứng dụng thật sự cần.
- Không lưu cùng một dữ liệu ở nhiều nơi mà không có quy tắc đồng bộ.
- Không thêm thư viện chỉ để thay một nhu cầu nhỏ đã giải quyết được bằng thư viện hiện có.

### 9.2. Các trạng thái bắt buộc phải thiết kế

| Tình huống | Hành vi cần có |
| --- | --- |
| Tải lần đầu | Loading rõ, không hiển thị dữ liệu của bản ghi trước |
| Danh sách rỗng | Thông báo phù hợp, gợi ý thao tác tiếp theo nếu có |
| API lỗi | Thông báo có thể hiểu, retry cho thao tác đọc khi phù hợp |
| Gửi form | Chặn gửi lặp, giữ đầu vào nếu thất bại |
| Thành công | Hiển thị kết quả server đã xác nhận và cập nhật dữ liệu liên quan |
| Validation lỗi | Gắn lỗi đúng field, giúp chuyển focus tới lỗi đầu tiên |
| Phiên hết hạn | Xóa trạng thái xác thực, chuyển về đăng nhập theo quy tắc chung |
| Không đủ quyền | Trang/thông báo phù hợp; không để form tiếp tục thao tác |
| Bản ghi đã xóa/ngừng hoạt động | Thông báo và đường quay lại |
| Xung đột 409 | Giải thích dữ liệu đã đổi, tải lại và yêu cầu người dùng quyết định tiếp |
| Mạng timeout sau thao tác ghi | Không khẳng định thao tác thất bại chắc chắn; cho kiểm tra trạng thái trước khi gửi lại |

### 9.3. Form

- Label rõ, field required có chỉ dẫn, kiểu input và autocomplete phù hợp.
- Dùng quy tắc validation đã thống nhất; frontend hỗ trợ trải nghiệm, backend vẫn kiểm tra lại.
- Thống nhất cách chuẩn hóa chuỗi; không trim/biến đổi mật khẩu tự động.
- Hiển thị lỗi server cho field khi có; lỗi toàn form dùng thông báo chung.
- Khi sửa đối tượng A rồi chuyển sang B, reset giá trị và lỗi đúng đối tượng.
- Không gửi read-only fields chỉ vì chúng có trong response.
- Xác nhận thao tác có hậu quả như hủy đơn; không thêm xác nhận cho mọi lần lưu thông thường.
- Với dữ liệu chưa lưu, cân nhắc cảnh báo rời trang theo mức độ mất dữ liệu.

### 9.4. Data fetching và cập nhật

- Hủy/bỏ qua request cũ khi component unmount hoặc tham số đổi.
- Không để response chậm của lần tìm trước ghi đè kết quả lần tìm sau.
- Thay điều kiện tìm phải làm rõ dữ liệu đang thuộc bộ lọc nào; không cho đặt theo bộ lọc cũ ngoài ý muốn.
- Sau mutation, cập nhật hoặc refetch tất cả vùng chịu ảnh hưởng: chi tiết, danh sách, số đếm và trạng thái nút liên quan.
- Không tự retry tạo đơn/thanh toán nếu chưa có cơ chế idempotency.
- Chỉ optimistic update khi có thể phục hồi và được nghiệp vụ cho phép; thao tác giá/trạng thái quan trọng thường nên chờ server.
- Khi bổ sung cache phía client, chốt key theo tham số và cách invalidation; không bắt buộc thêm thư viện cache ngay.

### 9.5. Quyền, accessibility và responsive

- Route guard và menu theo role; dữ liệu nhạy cảm vẫn phải được server giới hạn.
- Hỗ trợ bàn phím, focus nhìn thấy được, label, thông báo lỗi/status có ngữ nghĩa phù hợp.
- Modal cần quản lý focus và trả focus về nút mở khi đóng.
- Không truyền đạt trạng thái chỉ bằng màu.
- Kiểm tra bảng/form ở mobile, nội dung dài, giá trị tiền lớn và tên khách dài.
- Không dùng dữ liệu giả để báo thao tác thành công trong môi trường thật. Fixture/mock chỉ dùng cho phát triển/test và phải nhận diện được.

## 10. Checklist riêng theo từng loại công việc

### 10.1. Đối tượng hoàn toàn mới — ví dụ Loại phòng

1. Chốt ý nghĩa, field, vòng đời và ma trận quyền.
2. Quyết định entity/bảng mới hay có thể dùng cấu trúc hiện tại.
3. Thiết kế quan hệ với phòng và tác động lên dữ liệu đang có.
4. Chốt contract danh sách/chi tiết/tạo/sửa/ngừng hoạt động; chỉ làm CRUD thực sự cần.
5. Viết migration, constraints, index và dữ liệu khởi tạo nếu cần.
6. Triển khai entity/DTO/repository/service/controller và security.
7. Test validation, trùng mã, quan hệ, quyền và hành vi khi đang được sử dụng.
8. Thêm route/page/form/API adapter và trạng thái UI.
9. Test tích hợp, nghiệm thu ba vai trò liên quan, mobile và lỗi API.
10. Tạo PR ở hai repo, chốt thứ tự triển khai, migrate và smoke test.

### 10.2. Đối tượng đã có, thêm thuộc tính — ví dụ Diện tích phòng

1. Xem field tương tự đã có hoặc có thể suy ra không; tránh lưu hai nguồn mâu thuẫn.
2. Chốt đơn vị, miền giá trị, quyền xem/sửa và bắt buộc hay tùy chọn.
3. Kiểm tra dữ liệu cũ: chưa biết diện tích có được để null không? Ai bổ sung?
4. Thiết kế migration và backfill không làm gián đoạn app cũ.
5. Cập nhật entity, DTO, mapping, validation và truy vấn/báo cáo liên quan.
6. Cập nhật contract và các client cần dùng; tránh đổi nghĩa field cũ.
7. Thêm field form/chi tiết/bộ lọc nếu đúng yêu cầu; hiển thị “Chưa cập nhật” với dữ liệu chưa có.
8. Test tạo mới, cập nhật, bản ghi cũ, giới hạn giá trị và payload thiếu field.
9. Nếu thêm field vào lọc/sort, đánh giá index và hiệu năng.

Không cần tạo controller hoặc service mới nếu use case hiện tại vẫn phù hợp.

### 10.3. Thêm use case cho đối tượng cũ — ví dụ Gia hạn booking

1. Viết điều kiện gia hạn, người được làm, thời điểm được làm và cách tính thêm tiền.
2. Xác định trạng thái trước/sau, các khoản đã thanh toán và lịch sử cần lưu.
3. Tìm logic phòng trống/tính giá đã có để dùng lại thống nhất.
4. Thiết kế endpoint, request DTO và response/lỗi xung đột.
5. Quyết định transaction và lock để gia hạn không trùng với booking khác.
6. Viết service use case mới; chỉ đổi schema khi dữ liệu hiện tại không đủ.
7. Thêm controller/security và audit nếu cần.
8. View chỉ hiện hành động khi phù hợp, nhưng vẫn xử lý server từ chối do trạng thái mới thay đổi.
9. Test quyền, ngày không hợp lệ, trùng lịch, tính tiền, rollback transaction và hai thao tác đồng thời khi có rủi ro.
10. Test hồi quy đặt mới/hủy/đổi phòng, không chỉ gia hạn.

### 10.4. Thay đổi quy tắc đang có — ví dụ Phí hủy

1. Ghi rõ quy tắc cũ và mới bằng ví dụ số cụ thể.
2. Chốt ngày hiệu lực; áp dụng cho đơn mới, đơn cũ hay theo thời điểm đặt?
3. Đánh giá các mức giá/chính sách đã lưu trong booking và khả năng tái hiện hóa đơn cũ.
4. Thiết kế version/snapshot chính sách nếu phải giữ lịch sử.
5. Cập nhật service, contract trả breakdown và thông báo trên View.
6. Test các mốc ngay trước/đúng/ngay sau ngưỡng thời gian, mức tiền và cách làm tròn.
7. Cập nhật tài liệu người dùng, thông báo thay đổi và kế hoạch triển khai phù hợp.

### 10.5. Sửa lỗi

1. Thu thập bước tái hiện, kết quả mong muốn/thực tế, môi trường, phiên bản và request reference; không đưa thông tin nhạy cảm vào issue.
2. Phân loại mức độ ảnh hưởng và xác định có cần xử lý khẩn cấp không.
3. Tái hiện lỗi trên môi trường phù hợp; với lỗi khó tái hiện, ghi rõ bằng chứng và giới hạn.
4. Tìm nguyên nhân gốc ở View, contract, controller, service, truy vấn hoặc dữ liệu.
5. Nếu lỗi có thể lặp và ảnh hưởng đáng kể, thêm test hồi quy thể hiện hành vi đúng; tốt nhất thấy test thất bại trước bản sửa.
6. Sửa phạm vi đủ giải quyết nguyên nhân, tránh trộn refactor lớn.
7. Kiểm tra dữ liệu đã sai có cần sửa không. Nếu có, tách script/migration sửa dữ liệu và kế hoạch backup/kiểm chứng; sửa code không tự chữa dữ liệu cũ.
8. Chạy test liên quan và các luồng lân cận có cùng nguyên nhân.
9. Viết PR nêu trigger, trước/sau, nguyên nhân, bản sửa và validation.
10. Sau triển khai, xác minh lỗi hết và không phát sinh dấu hiệu mới.

Với chỉnh sửa thuần trình bày, kiểm tra trực quan phù hợp có thể đủ; không tạo test chỉ để phản chiếu CSS hoặc tên hàm.

### 10.6. Sửa View hoặc refactor

- Sửa View: chụp/so sánh màn hình liên quan, kiểm tra responsive/keyboard/error states, giữ contract nếu không có yêu cầu khác.
- Refactor: mô tả hành vi cần giữ, dùng kiểm thử có sẵn làm hàng rào, tránh đổi API/schema/quy tắc cùng lúc nếu không cần.
- Hạ tầng: kiểm tra cả dev/CI/production, quyền truy cập, cấu hình bắt buộc, secret và cách rollback; chạy kiểm tra runtime sau build.

## 11. Giai đoạn kiểm thử và tiêu chí hoàn tất

### 11.1. Chọn kiểm thử theo rủi ro

| Tầng | Cần chứng minh | Ví dụ |
| --- | --- | --- |
| Unit nghiệp vụ | Điều kiện và phép tính đúng | Phí hủy, chuyển trạng thái, giới hạn số khách |
| Repository/integration | Query, mapping, constraints, transaction đúng | Không đọc đơn người khác, unique, lock |
| Controller/security | Contract và HTTP status/quyền đúng | 400/401/403/404/409; CSRF; không lộ field |
| Frontend logic | Dữ liệu/form/request được xử lý đúng | Không gửi khi lấy CSRF thất bại, bỏ response cũ |
| UI/browser | Người dùng hoàn thành được thao tác | Nhập form, lỗi field, bàn phím, mobile |
| End-to-end | Luồng quan trọng chạy xuyên hai project | Khách đặt → lễ tân xác nhận → nhận/trả phòng |
| Migration/runtime | App chạy được với schema mới/dữ liệu cũ | Nâng cấp DB rồi health và use case chính |

Không bắt buộc test mọi dòng code hoặc đạt tỷ lệ coverage tùy ý. Ưu tiên các bất biến và lỗi gây sai dữ liệu/tiền/quyền. Mock test không chứng minh PostgreSQL lock/constraint đúng; H2 không thay thế PostgreSQL cho các đặc tính này.

### 11.2. Ma trận tình huống tối thiểu cho chức năng có ghi dữ liệu

- Thành công với dữ liệu hợp lệ.
- Thiếu field, sai kiểu/format, giá trị biên.
- Chưa đăng nhập, sai role, đúng role nhưng sai chủ bản ghi.
- Bản ghi không tồn tại/ngừng hoạt động/trạng thái không phù hợp.
- DB hoặc API thất bại: không lưu dở dang, View không báo thành công.
- Gửi lặp/đồng thời nếu use case có rủi ro.
- Dữ liệu cũ sau migration nếu có đổi schema.
- Luồng cũ liên quan tiếp tục hoạt động.

### 11.3. Lệnh kiểm tra hiện tại

Frontend:

```powershell
npm run lint
npm test
npm run build
git diff --check
```

Backend, trong thư mục có pom.xml và JAVA_HOME phù hợp:

```powershell
.\mvnw.cmd --batch-mode verify
git diff --check
```

Chạy thêm integration/browser/concurrency test khi thay đổi đòi hỏi. Khi các kiểm tra phù hợp đã đạt, không chạy lặp lại bộ test rộng nhiều lần nếu không có thay đổi hoặc nghi vấn mới.

### 11.4. Definition of Done

- [ ] Tiêu chí nghiệm thu đã được đáp ứng.
- [ ] Contract frontend/backend khớp với request/response thực tế.
- [ ] Quyền role và quyền theo bản ghi được kiểm tra ở backend.
- [ ] Validation, lỗi, loading, empty và success phù hợp.
- [ ] Không làm sai dữ liệu cũ; migration/backfill đã kiểm chứng nếu có.
- [ ] Test theo rủi ro, lint/build và CI đạt; ghi rõ kiểm tra chưa thực hiện.
- [ ] Tài liệu, env example và hướng dẫn vận hành đã cập nhật khi cần.
- [ ] Có tài liệu bàn giao chức năng theo mục 16, liệt kê đầy đủ file thêm/sửa/xóa/đổi tên cùng lý do và kết quả kiểm tra; PR dẫn link tới tài liệu.
- [ ] Không commit secret, dữ liệu khách thật, log nhạy cảm hoặc build artifact.
- [ ] PR đủ thông tin review và đã xử lý feedback cần thiết.
- [ ] Có kế hoạch triển khai/rollback tương ứng rủi ro.
- [ ] Sau deploy đã smoke test và theo dõi kết quả; trước deploy chỉ đánh dấu “sẵn sàng triển khai”.

“Code xong”, “PR đã merge”, “đã triển khai” và “đã nghiệm thu” là các mốc khác nhau, cần báo cáo đúng mốc.

## 12. PR, review và phối hợp hai repository

1. Kiểm tra base branch, diff, file ngoài phạm vi và commit chưa push.
2. Commit theo đơn vị thay đổi có ý nghĩa, ví dụ `feat: add room type management`, `fix: prevent overlapping bookings`.
3. Push nhánh `quyeen/...`, mở PR vào base phù hợp.
   Trước khi báo hoàn tất phần phát triển hoặc gửi PR để review, phải có tài liệu bàn giao ở `docs/changes/` theo mục 16. Không dùng riêng commit message hoặc mô tả PR để thay thế tài liệu này.
4. PR backend và frontend dẫn link đến nhau; ghi dependency và thứ tự merge/deploy.
5. Nếu dùng stacked PR để review, ghi rõ base; khi PR nền đã merge, kiểm tra và đổi base về main khi phù hợp.
6. Chờ CI và review; sửa phản hồi bằng commit rõ ràng. Không tự merge khi người dùng muốn tự merge.
7. Giải quyết conflict bằng cách hiểu thay đổi hai phía, rồi kiểm tra lại vùng bị ảnh hưởng.

Mẫu PR:

```markdown
## Vấn đề và hành vi sau thay đổi
Khi ..., trước đây ...; sau thay đổi ...

## Thay đổi
- Backend / schema / View có liên quan.
- Contract, quyền hoặc quy tắc cần reviewer chú ý.

## Kiểm tra
- Lệnh/test và kết quả.
- Luồng thực tế đã nghiệm thu.
- Phần chưa kiểm tra và ảnh hưởng.

## Triển khai
- Migration/backfill nếu có.
- PR liên quan và thứ tự triển khai.
- Rollback/giới hạn tương thích.
```

Review cần kiểm tra nghiệp vụ và dữ liệu, không chỉ style: quyền theo chủ bản ghi, trạng thái, transaction, tổng tiền, migration, response DTO, request lặp, stale data và khả năng quan sát lỗi.

## 13. Triển khai từ staging đến production

### 13.1. Trước triển khai

- Chọn commit/version đã qua CI và review; build artifact có thể truy ra commit.
- Xác định ai triển khai và ai xác nhận nghiệm thu.
- Chuẩn bị env/secret đúng môi trường, database đúng đích, origin và HTTPS/cookie đúng cấu hình.
- Nếu schema/dữ liệu có rủi ro, tạo backup và xác minh phương án khôi phục.
- Ước lượng downtime/lock; dùng feature flag hoặc triển khai theo giai đoạn khi có nhu cầu tương thích/rủi ro thực tế.
- Không coi migration chạy được local là đủ để đảm bảo dữ liệu production an toàn.

### 13.2. Thứ tự triển khai

Với thay đổi tương thích, thường thực hiện:

```text
Migration mở rộng tương thích
→ Backend hỗ trợ contract mới và client cũ
→ Kiểm tra backend
→ Frontend dùng chức năng mới
→ Nghiệm thu
→ Loại bỏ phần cũ ở một đợt sau khi không còn client phụ thuộc
```

Đây là chiến lược expand/contract; không áp dụng máy móc. Nếu một phiên bản không thể chạy với schema/contract khác, phải thiết kế phối hợp hoặc cửa sổ bảo trì trước khi triển khai.

### 13.3. Smoke test sau triển khai

- Health và database kết nối được; không lộ chi tiết nội bộ.
- Frontend tải được, assets đúng version và `/api` đi đúng backend.
- Đăng nhập/session/CSRF/CORS hoạt động với hostname thật nếu chức năng auth đã được triển khai.
- Use case mới chạy thành công bằng dữ liệu/tài khoản kiểm thử được phép.
- Một số luồng cũ quan trọng vẫn hoạt động.
- Không tăng bất thường lỗi 5xx, độ trễ, xung đột DB hoặc thao tác nghiệp vụ thất bại.
- Ghi phiên bản, thời điểm triển khai và kết quả nghiệm thu.

Health UP chỉ chứng minh một phần hạ tầng, không chứng minh chức năng đặt phòng đúng.

### 13.4. Rollback và xử lý sự cố

- Chốt trước điều kiện rollback: lỗi quyền, sai tiền/dữ liệu, lỗi nghiêm trọng hoặc chỉ số vượt ngưỡng đã định.
- Nếu có flag, tắt chức năng khi cách đó đủ giảm tác động.
- Quay về artifact trước chỉ khi phiên bản cũ tương thích schema hiện tại.
- Với migration phá dữ liệu, cân nhắc sửa tiến (forward fix), migration bù hoặc khôi phục theo kế hoạch; không tự xóa cột/bảng để “quay lại”.
- Backup restore có thể làm mất các giao dịch phát sinh sau backup; cần phương án bảo toàn/đối soát các giao dịch đó trước khi thực hiện.
- Ghi nhận nguyên nhân, phạm vi ảnh hưởng và bổ sung kiểm thử/quy trình ngăn tái diễn.

## 14. Mẫu kế hoạch cho một chức năng cụ thể

Sao chép mẫu sau vào issue/tài liệu khi bắt đầu:

```markdown
# [Tên chức năng]

## A. Yêu cầu
- Vấn đề / mục tiêu:
- Vai trò:
- Điều kiện trước:
- Luồng chính:
- Luồng lỗi/thay thế:
- Kết quả sau thao tác:
- Phạm vi chưa làm:
- Tiêu chí nghiệm thu:

## B. Ảnh hưởng
- Đối tượng mới hay đã có:
- Màn hình/endpoint/service/bảng liên quan:
- Dữ liệu cũ và quy tắc tương thích:
- Ma trận quyền và trạng thái:
- Tiền/thời gian/concurrency nếu có:

## C. Thiết kế
- Schema, constraints, index, migration/backfill:
- Request/response/error contract:
- Service use case và transaction:
- Quyền role/owner:
- View, form và các trạng thái UI:

## D. Công việc
- [ ] Migration/model/repository nếu cần
- [ ] DTO/service/controller/security
- [ ] API adapter/route/page/form
- [ ] Error/loading/empty/success và dữ liệu sau mutation
- [ ] Kiểm thử theo rủi ro
- [ ] Tài liệu/env/CI

## E. Bàn giao
- Backend branch/PR:
- Frontend branch/PR:
- Tài liệu bàn giao trong docs/changes/:
- Kết quả kiểm tra:
- Giới hạn còn lại:
- Thứ tự merge/deploy:
- Smoke test:
- Rollback:
- Người xác nhận nghiệm thu:
```

## 15. Lộ trình áp dụng cho dự án hiện tại

Sau khi merge nền tảng, nên triển khai theo lát cắt đủ chạy xuyên frontend/backend:

1. **Tài khoản và phiên:** user/role, migration đầu tiên, đăng ký CUSTOMER, login/me/logout, phiên và quyền; hoàn thiện màn hình tài khoản với API thật.
2. **Danh mục và quản lý phòng:** model phòng/loại phòng theo yêu cầu đã chốt, tạo/sửa/ngừng hoạt động, danh sách công khai, pagination và quyền dữ liệu nội bộ.
3. **Đặt phòng:** khoảng lưu trú, sức chứa, giá đã chốt, transaction/chống trùng, tạo/xem đơn theo chủ sở hữu.
4. **Vòng đời lưu trú:** xác nhận, hủy, check-in/out, quy tắc chuyển trạng thái và audit.
5. **Nghiệp vụ bổ sung:** thanh toán/hóa đơn/dịch vụ/báo cáo theo yêu cầu tiếp theo, sau khi thống nhất hợp đồng và kiểm soát dữ liệu.

Mỗi lát cắt đi hết phân tích → contract → Model/Service/Controller → View → kiểm thử → PR → triển khai/nghiệm thu. Có thể phát triển View song song từ contract bằng fixture được đánh dấu rõ, nhưng chỉ nghiệm thu tích hợp sau khi chạy với API thật.

## 16. Tài liệu bàn giao bắt buộc sau mỗi công việc

### 16.1. Phạm vi và vị trí

Sau mỗi chức năng mới, mở rộng, sửa lỗi, refactor, thay đổi cấu hình hoặc tài liệu, người thực hiện phải viết một bản ghi bàn giao trước khi báo hoàn tất phần phát triển hoặc gửi PR để review. Bản ghi mô tả kết quả cuối cùng; không chép toàn bộ lịch sử trao đổi hay diff.

- Vị trí: `docs/changes/YYYY-MM-DD-ten-chuc-nang.md`.
- Mẫu: [FEATURE-REPORT.md](templates/FEATURE-REPORT.md).
- Một công việc có thể có nhiều commit nhưng dùng một bản ghi; cập nhật bản ghi nếu PR đổi phạm vi.
- Tên ngày dùng ngày địa phương của dự án; nếu trùng tên, thêm mã issue hoặc hậu tố rõ nghĩa.
- Công việc nhỏ có thể ghi ngắn, nhưng vẫn phải có hành vi/thay đổi, danh sách file, kiểm tra và giới hạn.
- Nếu ảnh hưởng cả hai project, mỗi repository có bản ghi phần của mình và dẫn link đối ứng khi đã có PR. Có thể dùng cùng nội dung tổng hợp cho cả hai nếu bảng phân biệt repository rõ ràng.
- Không ghi PR/commit giả. Khi chưa có PR, ghi “Chưa tạo”; bổ sung link sau khi có.

### 16.2. Nội dung bắt buộc

1. **Thông tin:** tên, loại thay đổi, ngày, repository, nhánh và trạng thái thực tế.
2. **Chức năng đã thực hiện:** mục đích, hành vi trước/sau, đối tượng/role chịu ảnh hưởng, luồng chính và quy tắc mới.
3. **File thay đổi:** bảng đường dẫn theo repository, thêm/sửa/xóa/đổi tên, vai trò của file, nội dung đã đổi và lý do. Ghi cả test, migration, cấu hình, CI và tài liệu liên quan; bỏ qua build artifact/file ignored không được bàn giao.
4. **Ảnh hưởng kỹ thuật:** endpoint/request/response, schema/migration/backfill, cấu hình/env, dependency và tương thích khi có. Không áp dụng thì ghi rõ.
5. **Kiểm tra:** lệnh hoặc kịch bản, môi trường, kết quả thực tế và các kiểm tra chưa chạy. Không ghi “pass” nếu chỉ suy đoán từ code.
6. **Cách kiểm tra lại:** bước chạy/thao tác và kết quả mong đợi để người nhận có thể xác minh.
7. **Giới hạn/còn lại:** tính năng chưa làm, rủi ro hoặc phụ thuộc cần xử lý tiếp.
8. **Triển khai và liên kết:** thứ tự merge/deploy, migration/rollback phù hợp, link issue/PR/tài liệu liên quan nếu có.

Với sửa lỗi phải ghi trigger và nguyên nhân gốc khi đã xác định, bản sửa và dữ liệu cũ cần khắc phục. Với thay đổi chỉ tài liệu phải ghi rõ chưa đổi runtime và không chạy test ứng dụng nếu không cần.

### 16.3. Kiểm tra trước khi bàn giao

- Đối chiếu `git status`, diff và diff PR theo base để tránh bỏ sót file mới hoặc liệt kê nhầm file công việc khác.
- Dùng đường dẫn tương đối theo repository trong bản ghi để GitHub và checkout khác đều mở được. File đổi tên ghi cả đường dẫn cũ và mới.
- Không đưa secret, token, dữ liệu khách thật hoặc log nhạy cảm vào bản ghi.
- PR phải dẫn link tới bản ghi; người review kiểm tra bảng file và bằng chứng kiểm thử.
- Phân biệt rõ: đã viết code/tài liệu, kiểm tra local, CI đạt, đã merge, đã deploy và đã nghiệm thu. Sau deploy cập nhật kết quả nếu công việc bao gồm triển khai.
- Thiếu bản ghi thì công việc chưa đủ điều kiện bàn giao, dù build/test đã đạt.

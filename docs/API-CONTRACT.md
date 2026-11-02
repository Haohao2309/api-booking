# Quy chuẩn response và phân trang

Quy chuẩn được chốt ngày 02/11/2026 cho `web-booking` và `api-booking`. Thay thế đề xuất response trực tiếp trước đây.

**Trạng thái:** đây là hợp đồng mục tiêu. Runtime hiện tại chưa được chuyển sang envelope; Axios vẫn unwrap JSON trực tiếp, backend nền tảng trả health/CSRF và lỗi theo cấu trúc cũ. Phải cập nhật backend, frontend, kiểm thử và CI smoke test đồng bộ trước khi sử dụng hợp đồng này. Tài liệu không có nghĩa các API nghiệp vụ đã được triển khai.

## 1. Envelope bắt buộc

Mọi response JSON của API ứng dụng có đúng bốn field ở cấp ngoài:

```json
{
  "success": true,
  "message": "Lấy thông tin phòng thành công.",
  "data": {
    "id": 12,
    "name": "Phòng Deluxe"
  },
  "status": 200
}
```

| Field | Kiểu và quy tắc |
| --- | --- |
| success | Boolean. true với HTTP 2xx; false với HTTP 4xx/5xx |
| message | String có nội dung, phù hợp cho người dùng. Không dùng làm mã để điều khiển logic |
| data | DTO kết quả, DTO trang, đối tượng lỗi hoặc null. Luôn có field này |
| status | Integer trùng chính xác HTTP status thực tế; không phải trạng thái nghiệp vụ hoặc chuỗi "OK" |

- HTTP status vẫn là tín hiệu chuẩn cho Axios, browser, proxy và monitoring. Không trả HTTP 200 cho lỗi rồi chỉ ghi status: 400 trong body.
- Trạng thái nghiệp vụ như PENDING/CANCELLED nằm trong `data.status`; không đặt vào field `status` ngoài cùng.
- Không thêm `data.data` do bọc hai lần. Backend không trả trực tiếp entity JPA.
- Field mở rộng thuộc DTO trong data, giữ envelope ổn định.
- Response tải file/stream, HEAD, CORS OPTIONS hoặc response do hạ tầng/proxy tạo không phải JSON ứng dụng nên không bọc JSON một cách cưỡng ép. Frontend vẫn phải xử lý trường hợp lỗi mạng/non-JSON.

## 2. Thành công

### Chi tiết hoặc cập nhật: HTTP 200

`data` chứa DTO sau khi server xác nhận. Khi cập nhật booking, trả trạng thái/giá trị mới nhất từ server.

### Tạo mới: HTTP 201

```json
{
  "success": true,
  "message": "Tạo phòng thành công.",
  "data": { "id": 12, "name": "Phòng Deluxe" },
  "status": 201
}
```

Thêm header Location trỏ tài nguyên mới khi phù hợp.

### Thành công không có dữ liệu: HTTP 200

```json
{
  "success": true,
  "message": "Đăng xuất thành công.",
  "data": null,
  "status": 200
}
```

Áp dụng cho logout hoặc xóa không cần trả đối tượng. Không dùng 204 cho endpoint JSON ứng dụng theo quy chuẩn này, vì 204 không được có response body.

## 3. Lỗi

```json
{
  "success": false,
  "message": "Thông tin phòng không hợp lệ.",
  "data": {
    "code": "VALIDATION_ERROR",
    "fieldErrors": {
      "capacity": "Sức chứa phải lớn hơn 0.",
      "pricePerNight": "Giá phòng không được âm."
    }
  },
  "status": 400
}
```

Quy ước data khi lỗi:

- `code`: string ổn định, bắt buộc; client xử lý theo code thay vì so sánh message.
- `fieldErrors`: object chỉ thêm khi có lỗi field; key khớp request DTO. Quy ước một thông báo ưu tiên cho mỗi field.
- `traceId`: string tùy chọn để đối chiếu log; chỉ trả khi server thực sự có mã tương ứng, không tạo giá trị minh họa trong runtime.
- Lỗi nghiệp vụ không gắn field có thể chỉ có code; không trả stack trace, SQL, password hash hoặc token.

Ví dụ xung đột, HTTP 409:

```json
{
  "success": false,
  "message": "Phòng không còn trống trong khoảng ngày đã chọn.",
  "data": { "code": "ROOM_NOT_AVAILABLE" },
  "status": 409
}
```

| HTTP status | Ý nghĩa | Ví dụ code |
| --- | --- | --- |
| 400 | JSON, tham số hoặc validation không hợp lệ | INVALID_REQUEST, VALIDATION_ERROR, INVALID_PAGINATION |
| 401 | Chưa đăng nhập/phiên không hợp lệ | AUTHENTICATION_REQUIRED |
| 403 | Không đủ quyền hoặc CSRF không hợp lệ | ACCESS_DENIED, CSRF_INVALID |
| 404 | Tài nguyên không tồn tại/không được phép tiết lộ | RESOURCE_NOT_FOUND |
| 405 | Method không được hỗ trợ | METHOD_NOT_ALLOWED |
| 409 | Xung đột dữ liệu hoặc trạng thái | ROOM_NOT_AVAILABLE, BOOKING_STATE_CONFLICT |
| 415 | Content type không được hỗ trợ | UNSUPPORTED_MEDIA_TYPE |
| 429 | Bị giới hạn tần suất khi cơ chế này có triển khai | RATE_LIMITED |
| 500 | Lỗi hệ thống ngoài dự kiến | INTERNAL_ERROR |
| 503 | Dịch vụ chưa sẵn sàng | SERVICE_UNAVAILABLE |

Ở giai đoạn này thống nhất 400 cho validation, không trộn 400 và 422 giữa các endpoint. Security filter cũng phải trả envelope cho lỗi xác thực/quyền/CSRF; exception handler của controller không tự bao phủ lỗi ở filter. Lỗi CORS preflight bị từ chối không được hiểu là request nghiệp vụ hợp lệ và không cần body JSON cho browser.

## 4. Mọi endpoint danh sách phải phân trang phía server

Áp dụng cho danh sách phòng, booking, tài khoản, loại phòng, tiện nghi, dịch vụ, log và các collection nghiệp vụ khác, kể cả danh mục dùng để chọn trong form.

Ví dụ:

```http
GET /api/rooms?page=0&size=20&sort=name,asc
```

| Tham số | Quy định |
| --- | --- |
| page | Integer >= 0; bắt đầu từ 0; mặc định 0 |
| size | Integer từ 1 đến 100; mặc định 20 |
| sort | Field và asc/desc theo allowlist riêng của endpoint. Nếu thiếu, mặc định id,asc |
| filter | Theo contract endpoint, ví dụ status/checkIn/checkOut/guests; validate và áp dụng trên server |

- Sai kiểu, page âm, size ngoài khoảng, sort field/direction không cho phép: trả 400, không âm thầm đổi giá trị.
- Khi sort theo field không unique, thêm id làm tiêu chí cuối để ổn định thứ tự. Chốt rõ trường ID nếu đối tượng dùng khóa khác.
- Kiểm tra quyền và giới hạn phạm vi dữ liệu, áp dụng filter/sort, rồi mới phân trang. totalItems cũng phải tính trong phạm vi người dùng được xem.
- Không lấy toàn bộ bản ghi vào Java/React rồi cắt mảng để gọi là phân trang server.
- Không thêm endpoint `get-all` không giới hạn để bỏ qua quy chuẩn. Dropdown dùng tìm kiếm/phân trang hoặc tải thêm khi cần.
- Export hàng loạt phải là use case riêng có quyền, giới hạn và thiết kế stream/job phù hợp; không dùng size cực lớn của API danh sách.

### Response danh sách, HTTP 200

```json
{
  "success": true,
  "message": "Lấy danh sách phòng thành công.",
  "data": {
    "items": [
      { "id": 12, "name": "Phòng Deluxe" },
      { "id": 18, "name": "Phòng Family" }
    ],
    "page": 0,
    "size": 2,
    "totalItems": 5,
    "totalPages": 3
  },
  "status": 200
}
```

- `items`: array DTO, không null.
- `page`: trang được yêu cầu; UI có thể hiển thị page + 1 cho người dùng.
- `size`: kích thước trang đã yêu cầu, không phải số phần tử thực tế trong items.
- `totalItems`: tổng số bản ghi khớp quyền/filter.
- `totalPages`: ceil(totalItems / size); bằng 0 nếu totalItems = 0.
- Không trả nguyên cấu trúc nội bộ Spring Page; map sang DTO trang ổn định ở trên.

### Không có kết quả

```json
{
  "success": true,
  "message": "Không có phòng phù hợp.",
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "totalItems": 0,
    "totalPages": 0
  },
  "status": 200
}
```

page hợp lệ nhưng vượt trang cuối: vẫn trả 200, items rỗng, giữ totalItems/totalPages thật và page được yêu cầu. Không trả 404. Sau khi xóa bản ghi cuối trang, View có thể chuyển về trang cuối hợp lệ rồi fetch lại.

Offset pagination không đảm bảo một snapshot bất biến nếu người khác đồng thời thêm/xóa dữ liệu. Use case cần snapshot/export chính xác phải có thiết kế riêng; sort ổn định chỉ giải quyết thứ tự khi dữ liệu không đổi.

## 5. Trách nhiệm mỗi tầng khi triển khai

### Backend

1. Tạo DTO dùng chung `ApiResponse<T>` gồm bốn field, và `PageResponse<T>` gồm metadata trang.
2. Controller/response helper đặt HTTP status và body status từ cùng một giá trị.
3. DTO của service chứa dữ liệu nghiệp vụ; không bắt service xử lý HttpServletResponse.
4. Dùng một chiến lược wrap rõ ràng; không vừa wrap thủ công vừa wrap tự động gây lồng envelope.
5. Exception handler và security handlers trả cùng cấu trúc lỗi.
6. Validate page/size/sort trước khi query; repository phân trang và count đúng phạm vi/filter.
7. Áp dụng envelope cho CSRF. Với health đang dùng Actuator, cần adapter/controller hoặc cách mapping được kiểm thử để giữ HTTP 503 khi DOWN và trả health status trong data.status.
8. Không bọc byte/file/stream, HEAD hoặc OPTIONS thành JSON.

### Frontend

1. API client kiểm tra envelope hợp lệ; status body phải khớp HTTP status.
2. Interceptor thành công trả `body.data` cho các adapter/page, do đó component không phải đọc nhiều cấp data.
3. Lỗi giữ HTTP status cùng message và data.code/fieldErrors để UI xử lý.
4. CSRF client đọc token từ data sau unwrap; không đọc body.headerName theo contract cũ.
5. Mọi list request gửi page/size và filter/sort cần thiết; UI render metadata server, không tự tính tổng từ một trang.
6. Có chuyển trang, chọn kích thước trang phù hợp và trạng thái loading/error/empty.
7. Khi đổi filter/sort/size, reset page về 0; bỏ qua request cũ.
8. Khi lọc trạng thái booking phải gửi filter lên server; lọc riêng items của trang hiện tại sẽ cho kết quả/tổng sai.

## 6. Kiểm thử và chuyển đổi hợp đồng

- Kiểm thử success/message/data/status tồn tại, đúng kiểu; body status = HTTP status.
- Kiểm thử tạo 201, thao tác không dữ liệu 200/null, các lỗi controller/security và trường hợp không được bọc hai lần.
- Kiểm thử trang đầu/cuối/rỗng/vượt cuối, default, giá trị không hợp lệ, sort ổn định và total theo quyền/filter.
- Kiểm thử client unwrap, lỗi envelope, CSRF mới, thay bộ lọc và phản hồi đến sai thứ tự.
- Update smoke test health/CI: đọc data.status = UP, đồng thời vẫn kiểm tra HTTP 200; DOWN phải giữ HTTP 503.
- Đây là thay đổi phá tương thích. Backend và frontend phải được triển khai theo kế hoạch phối hợp, hoặc có lớp tương thích/version tạm thời được xác định rõ.
- Không để tài liệu ghi đã hoàn tất chuyển đổi khi runtime còn dùng contract cũ.

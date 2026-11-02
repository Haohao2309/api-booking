# Đồng bộ quy chuẩn sang backend và bổ sung tài liệu bàn giao

## 1. Thông tin

- Ngày: 02/11/2026, múi giờ Asia/Bangkok.
- Loại: tài liệu.
- Repository: `web-booking` và `api-booking`.
- Trạng thái: đã cập nhật tài liệu local; chưa tạo PR cho công việc này, chưa triển khai.
- Nhánh tại thời điểm thực hiện: `hao/backend-integration` ở frontend và `hao/backend-setup` ở backend.
- Nhánh bàn giao tài liệu: `hao/docs-development-standards` ở cả hai repository, tạo từ `origin/main` sau khi các PR setup đã merge. Bộ tài liệu chưa commit từ các công việc trước được gom vào lần bàn giao này.

## 2. Kết quả đã thực hiện

Trước thay đổi, frontend có bản quy trình phát triển và hợp đồng response/phân trang chưa commit; backend chưa có bản sao. Quy trình chưa quy định một bản ghi file/hành vi bắt buộc cho mỗi công việc.

Sau thay đổi:

- Cả hai repository có bản quy trình View–Model–Controller và hợp đồng response `{success, message, data, status}`, phân trang server cho API danh sách.
- Thêm yêu cầu bắt buộc viết bản ghi trong `docs/changes/` trước khi bàn giao hoặc gửi PR review.
- Bản ghi phải nêu chức năng, file đã đổi và lý do, ảnh hưởng API/dữ liệu/cấu hình, kiểm thử, cách kiểm tra lại, giới hạn và triển khai.
- Thêm mẫu dùng chung và bản ghi này làm ví dụ cho công việc tài liệu.
- README của mỗi project dẫn tới các tài liệu để dễ tìm.

## 3. File thay đổi trong công việc này

| Repository | Đường dẫn | Loại thay đổi | Nội dung và lý do |
| --- | --- | --- | --- |
| web-booking | `docs/QUY-TRINH-PHAT-TRIEN-CHUC-NANG.md` | Sửa nội dung file đang untracked từ công việc trước | Thêm mục 16, checklist hoàn tất, quy tắc PR và đường dẫn báo cáo trong mẫu kế hoạch |
| web-booking | `docs/templates/FEATURE-REPORT.md` | Thêm | Mẫu báo cáo đủ 8 phần để dùng sau mỗi công việc |
| web-booking | `docs/changes/2026-11-02-quy-chuan-va-tai-lieu-ban-giao.md` | Thêm | Ghi kết quả và danh sách file của chính thay đổi này |
| web-booking | `README.md` | Sửa file đã có thay đổi từ trước | Bổ sung đường dẫn mẫu và quy định báo cáo bắt buộc; giữ nội dung đã viết trước đó |
| web-booking | `docs/API-CONTRACT.md` | Thêm vào lần bàn giao Git này | Hợp đồng envelope và phân trang đã viết ở công việc trước; nội dung không thay đổi trong bước đồng bộ backend |
| api-booking | `docs/QUY-TRINH-PHAT-TRIEN-CHUC-NANG.md` | Thêm bản sao | Backend có đầy đủ quy trình phát triển và yêu cầu bàn giao giống frontend |
| api-booking | `docs/API-CONTRACT.md` | Thêm bản sao | Backend có quy chuẩn envelope/phân trang mục tiêu đã chốt |
| api-booking | `docs/templates/FEATURE-REPORT.md` | Thêm bản sao | Dùng mẫu bàn giao thống nhất |
| api-booking | `docs/changes/2026-11-02-quy-chuan-va-tai-lieu-ban-giao.md` | Thêm bản sao | Có bản ghi công việc ngay trong backend |
| api-booking | `README.md` | Sửa | Thêm mục tài liệu, link quy trình, contract, mẫu và bản ghi |

`docs/API-CONTRACT.md` ở frontend đã tồn tại từ công việc trước và không sửa nội dung trong công việc này; được dùng làm nguồn sao chép sang backend. Không thay đổi source code, dependency, schema hoặc cấu hình runtime.

## 4. Ảnh hưởng kỹ thuật

- Envelope/phân trang là quy chuẩn mục tiêu; chưa chuyển đổi runtime, controller/security handlers, Axios, list UI hoặc CI smoke test.
- Không thêm migration/env/secret và không tác động dữ liệu.
- Cần duy trì bản quy chuẩn hai repository nhất quán khi có thay đổi sau này; cập nhật bản ghi và link PR liên quan.

## 5. Kiểm tra

- Kiểm tra diff whitespace ở cả hai repository và sự tồn tại các liên kết Markdown tương đối trong bộ tài liệu.
- So sánh hash của bốn tài liệu dùng chung để xác nhận các bản sao giống nhau.
- Lint/build/test ứng dụng không chạy lại: thay đổi chỉ là Markdown, không sửa hành vi ứng dụng.
- Công việc chỉ được bàn giao sau khi các kiểm tra tài liệu trên đạt; kết quả được xác nhận cùng lần bàn giao này.

## 6. Cách kiểm tra lại

1. Mở README của mỗi repository và truy cập các link tài liệu.
2. Đọc mục 16 của quy trình, kiểm tra checklist hoàn tất và yêu cầu link báo cáo trong PR.
3. Sao chép mẫu `docs/templates/FEATURE-REPORT.md` thành bản ghi riêng cho công việc tiếp theo.
4. Chạy `git diff --check`; đối chiếu file được liệt kê với diff thực tế của công việc, bao gồm file mới chưa track.

## 7. Giới hạn và việc còn lại

- Chưa tự động hóa kiểm tra sự hiện diện của báo cáo bằng CI; đây là quy tắc thực hiện và review.
- Chưa tạo report hồi tố cho toàn bộ các PR cũ.
- Các API nghiệp vụ và việc chuyển đổi response/phân trang runtime là công việc riêng.

## 8. Triển khai và liên kết

- Đây là tài liệu: không có thứ tự deploy hoặc migration; khi đưa lên Git nên review cả hai bản cùng nhau.
- Rollback: hoàn nguyên các thay đổi tài liệu của công việc nếu cần, giữ nguyên nội dung công việc trước.
- Chưa tạo PR; không có link PR để ghi nhận tại thời điểm viết.
- Người dùng sẽ tự mở PR từ `hao/docs-development-standards` vào `main` tại mỗi repository. Hai nhánh tài liệu độc lập, không có thứ tự merge bắt buộc.
- [Quy trình, mục 16](../QUY-TRINH-PHAT-TRIEN-CHUC-NANG.md#16-tài-liệu-bàn-giao-bắt-buộc-sau-mỗi-công-việc).
- [Hợp đồng response/phân trang](../API-CONTRACT.md).
- [Mẫu bàn giao](../templates/FEATURE-REPORT.md).

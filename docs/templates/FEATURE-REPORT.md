# [Tên chức năng hoặc công việc]

> Sao chép mẫu vào `docs/changes/YYYY-MM-DD-ten-chuc-nang.md`, thay các chỗ trống và bỏ hướng dẫn mẫu. Ghi “Không áp dụng” kèm lý do khi cần; không đoán kết quả kiểm tra. Có thể rút gọn công việc nhỏ nhưng giữ đủ phần 1–8.

## 1. Thông tin

- Ngày:
- Loại: chức năng mới / mở rộng / sửa lỗi / refactor / cấu hình / tài liệu.
- Repository và nhánh:
- Trạng thái: đã thực hiện local / sẵn sàng review / đã merge / đã deploy / đã nghiệm thu.
- Issue/PR: Chưa tạo hoặc link thật.
- Công việc/PR phụ thuộc:

## 2. Kết quả đã thực hiện

- Mục đích:
- Trước thay đổi:
- Sau thay đổi:
- Đối tượng, vai trò và các luồng chịu ảnh hưởng:
- Quy tắc nghiệp vụ/validation/quyền mới:
- Nếu sửa lỗi: điều kiện tái hiện, nguyên nhân đã xác định và cách sửa:

## 3. File đã thêm/sửa/xóa/đổi tên

| Repository | Đường dẫn file | Loại thay đổi | Vai trò, nội dung đã đổi và lý do |
| --- | --- | --- | --- |
| [repo] | [đường dẫn tương đối] | Thêm / Sửa / Xóa / Đổi tên | [Giải thích đủ để người nhận tìm đúng chỗ] |

Liệt kê cả test, migration, cấu hình và CI nếu có; không chỉ file màn hình/controller. Với đổi tên ghi `cũ → mới`. Không liệt kê thay đổi ngoài phạm vi như đã thực hiện trong công việc này.

## 4. Ảnh hưởng API, dữ liệu và cấu hình

- Endpoint, request/response, envelope và phân trang:
- Entity/schema/migration/backfill và ảnh hưởng dữ liệu cũ:
- Config/env/dependency mới hoặc thay đổi, không ghi giá trị secret:
- Quyền, transaction/concurrency nếu liên quan:
- Tương thích và phần cần cập nhật ở project còn lại:

## 5. Kiểm tra đã thực hiện

| Lệnh hoặc kịch bản | Môi trường | Kết quả thực tế | Bằng chứng/ghi chú |
| --- | --- | --- | --- |
| [lệnh/kịch bản] | [local/CI/staging; DB/JDK khi liên quan] | Đạt / Không đạt / Chưa chạy | [Số test, lỗi hoặc link CI thật] |

- Kiểm tra chưa thực hiện và lý do:

## 6. Cách chạy hoặc xác minh lại

1. Điều kiện chuẩn bị:
2. Lệnh chạy hoặc thao tác:
3. Kết quả mong đợi:
4. Kịch bản lỗi/quyền cần kiểm tra:

## 7. Giới hạn và việc còn lại

- Phần chưa triển khai/không thuộc phạm vi:
- Rủi ro, vấn đề còn biết hoặc phụ thuộc:
- Việc cần làm tiếp:

## 8. Triển khai, rollback và liên kết

- Thứ tự merge/deploy:
- Migration/config cần áp dụng:
- Cách rollback và giới hạn:
- Kết quả sau triển khai: Chưa triển khai hoặc ghi nhận thực tế.
- Link PR đối ứng, contract hoặc tài liệu liên quan:

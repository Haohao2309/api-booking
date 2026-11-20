CREATE TABLE email_templates (
 id BIGSERIAL PRIMARY KEY,template_code VARCHAR(80) NOT NULL UNIQUE,
 subject_template VARCHAR(255) NOT NULL,body_template TEXT NOT NULL,active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO email_templates(template_code,subject_template,body_template) VALUES
 ('PAYMENT_RECEIVED','Đã nhận tiền đơn {bookingCode}','Đã nhận thanh toán cho đơn {bookingCode}. Đơn đang chờ ADMIN duyệt; phòng tiếp tục được giữ. Đăng nhập và mở chi tiết đơn để tải chứng từ.'),
 ('BOOKING_CONFIRMED','Xác nhận đặt phòng {bookingCode}','Đơn {bookingCode} đã được ADMIN xác nhận. Đăng nhập và mở chi tiết đơn để tải chứng từ xác nhận.');
ALTER TABLE notification_outbox RENAME TO notifications;
ALTER TABLE notifications ADD COLUMN template_id BIGINT REFERENCES email_templates(id);
ALTER TABLE notifications ADD COLUMN last_error VARCHAR(255);
CREATE VIEW notification_outbox AS SELECT id,booking_id,event_key,recipient,subject,body,status,attempts,next_attempt_at,locked_until,claim_token,created_at,sent_at FROM notifications;
ALTER TABLE refunds ADD COLUMN locked_until TIMESTAMP WITH TIME ZONE;
ALTER TABLE refunds ADD COLUMN claim_token VARCHAR(36);
ALTER TABLE refunds ADD COLUMN last_error VARCHAR(255);
CREATE INDEX ix_refund_pending ON refunds(status,next_retry_at);

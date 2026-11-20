ALTER TABLE bookings ADD COLUMN hotel_id BIGINT REFERENCES hotels(id);
ALTER TABLE bookings ADD COLUMN booking_code VARCHAR(50);
UPDATE bookings SET booking_code='LEGACY-' || CAST(id AS VARCHAR);
ALTER TABLE bookings ALTER COLUMN booking_code SET NOT NULL;
ALTER TABLE bookings ADD CONSTRAINT uq_booking_code UNIQUE(booking_code);
ALTER TABLE bookings ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'VND';
ALTER TABLE bookings ADD COLUMN hold_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE bookings ADD COLUMN check_in_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE bookings ADD COLUMN cancellation_policy_snapshot VARCHAR(50);
ALTER TABLE bookings ADD COLUMN free_cancel_until TIMESTAMP WITH TIME ZONE;
ALTER TABLE bookings ADD COLUMN cancelled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE bookings ADD COLUMN cancellation_reason VARCHAR(500);
CREATE TABLE booking_rooms (
 id BIGSERIAL PRIMARY KEY,booking_id BIGINT NOT NULL REFERENCES bookings(id),room_id BIGINT NOT NULL REFERENCES rooms(id),
 room_label_snapshot VARCHAR(150) NOT NULL,price_per_night NUMERIC(12,2) NOT NULL CHECK(price_per_night>0),
 number_of_nights INT NOT NULL CHECK(number_of_nights>0),subtotal NUMERIC(14,2) NOT NULL CHECK(subtotal>0),
 UNIQUE(booking_id,room_id),UNIQUE(id,room_id)
);
CREATE TABLE room_inventory (
 id BIGSERIAL PRIMARY KEY,room_id BIGINT NOT NULL REFERENCES rooms(id),stay_date DATE NOT NULL,
 state VARCHAR(20) NOT NULL CHECK(state IN ('AVAILABLE','HELD','BOOKED')),
 booking_room_id BIGINT,hold_expires_at TIMESTAMP WITH TIME ZONE,version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(room_id,stay_date),FOREIGN KEY(booking_room_id,room_id) REFERENCES booking_rooms(id,room_id),
 CHECK((state='AVAILABLE' AND booking_room_id IS NULL AND hold_expires_at IS NULL) OR (state IN ('HELD','BOOKED') AND booking_room_id IS NOT NULL))
);
CREATE INDEX ix_inventory_booking ON room_inventory(booking_room_id);
CREATE TABLE booking_services (
 id BIGSERIAL PRIMARY KEY,booking_id BIGINT NOT NULL REFERENCES bookings(id),service_id BIGINT NOT NULL REFERENCES hotel_services(id),
 service_name_snapshot VARCHAR(120) NOT NULL,unit_snapshot VARCHAR(30) NOT NULL,
 quantity NUMERIC(10,2) NOT NULL CHECK(quantity>0),unit_price NUMERIC(12,2) NOT NULL CHECK(unit_price>=0),
 subtotal NUMERIC(14,2) NOT NULL CHECK(subtotal>=0),UNIQUE(booking_id,service_id)
);
CREATE TABLE payments (
 id BIGSERIAL PRIMARY KEY,booking_id BIGINT NOT NULL REFERENCES bookings(id),provider VARCHAR(20) NOT NULL CHECK(provider IN ('STRIPE','VNPAY')),
 merchant_reference VARCHAR(100) NOT NULL UNIQUE,provider_transaction_id VARCHAR(255),
 amount NUMERIC(14,2) NOT NULL CHECK(amount>0),currency VARCHAR(3) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','PAID','FAILED')),
 request_key VARCHAR(100) NOT NULL,payment_url TEXT,review_reason VARCHAR(50),
 applied_to_booking BOOLEAN NOT NULL DEFAULT FALSE,paid_at TIMESTAMP WITH TIME ZONE,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(booking_id,request_key),UNIQUE(provider,provider_transaction_id)
);
CREATE INDEX ix_payment_booking ON payments(booking_id,status);
CREATE INDEX ix_payment_review ON payments(review_reason,id);
CREATE TABLE refunds (
 id BIGSERIAL PRIMARY KEY,payment_id BIGINT NOT NULL REFERENCES payments(id),idempotency_key VARCHAR(100) NOT NULL UNIQUE,
 provider_refund_id VARCHAR(255),amount NUMERIC(14,2) NOT NULL CHECK(amount>0),reason VARCHAR(500) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','REFUNDED','FAILED')),
 retry_count INT NOT NULL DEFAULT 0,next_retry_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 refunded_at TIMESTAMP WITH TIME ZONE,created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(payment_id),UNIQUE(provider_refund_id)
);
CREATE TABLE payment_events (
 id BIGSERIAL PRIMARY KEY,provider VARCHAR(20) NOT NULL,event_key VARCHAR(255) NOT NULL,
 payment_id BIGINT NOT NULL REFERENCES payments(id),received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(provider,event_key)
);

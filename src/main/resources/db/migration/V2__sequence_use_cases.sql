ALTER TABLE app_users ADD COLUMN full_name VARCHAR(150);
ALTER TABLE app_users ADD COLUMN security_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE app_users ADD COLUMN last_login TIMESTAMP WITH TIME ZONE;
-- Fail instead of silently merging accounts if legacy emails collide after normalization.
UPDATE app_users SET email=lower(email);
ALTER TABLE app_users ADD CONSTRAINT ck_app_users_email_normalized CHECK(email=lower(email));
CREATE TABLE roles (id BIGINT PRIMARY KEY, name VARCHAR(32) NOT NULL UNIQUE);
INSERT INTO roles(id,name) VALUES (1,'ADMIN'),(2,'CUSTOMER'),(3,'RECEPTIONIST');
CREATE TABLE user_roles (user_id BIGINT NOT NULL REFERENCES app_users(id),role_id BIGINT NOT NULL REFERENCES roles(id),PRIMARY KEY(user_id,role_id));
INSERT INTO user_roles(user_id,role_id) SELECT u.id,r.id FROM app_users u JOIN roles r ON u.role=r.name;
CREATE TABLE rooms (
 id BIGSERIAL PRIMARY KEY, code VARCHAR(50) NOT NULL UNIQUE, name VARCHAR(150) NOT NULL,
 type VARCHAR(80) NOT NULL, price NUMERIC(12,2) NOT NULL CHECK(price>0),
 capacity INT NOT NULL CHECK(capacity BETWEEN 1 AND 100), locked BOOLEAN NOT NULL DEFAULT FALSE,
 version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE room_images(room_id BIGINT NOT NULL REFERENCES rooms(id),position INT NOT NULL,url VARCHAR(500) NOT NULL,PRIMARY KEY(room_id,position));
CREATE TABLE partners(id BIGSERIAL PRIMARY KEY,name VARCHAR(150) NOT NULL,contact VARCHAR(200) NOT NULL,tax_code VARCHAR(30) NOT NULL UNIQUE,address VARCHAR(500) NOT NULL,version BIGINT NOT NULL DEFAULT 0);
CREATE TABLE partner_room(partner_id BIGINT NOT NULL REFERENCES partners(id),room_id BIGINT NOT NULL REFERENCES rooms(id),PRIMARY KEY(partner_id,room_id));
CREATE INDEX ix_partner_room_room ON partner_room(room_id);
CREATE TABLE bookings(
 id BIGSERIAL PRIMARY KEY, room_id BIGINT NOT NULL REFERENCES rooms(id),customer_id BIGINT NOT NULL REFERENCES app_users(id),
 room_name VARCHAR(150) NOT NULL,customer_name VARCHAR(150),customer_email VARCHAR(320) NOT NULL,
 check_in DATE NOT NULL,check_out DATE NOT NULL,guests INT NOT NULL CHECK(guests>0),
 price_per_night NUMERIC(12,2) NOT NULL CHECK(price_per_night>0),total_price NUMERIC(14,2) NOT NULL CHECK(total_price>0),
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','CONFIRMED','CANCELLED')),
 request_key VARCHAR(100) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 confirmed_by BIGINT REFERENCES app_users(id),confirmed_at TIMESTAMP WITH TIME ZONE,
 CONSTRAINT ck_booking_dates CHECK(check_out>check_in),CONSTRAINT uq_booking_request UNIQUE(customer_id,request_key)
);
CREATE INDEX ix_booking_room_dates ON bookings(room_id,status,check_in,check_out);
CREATE INDEX ix_booking_customer ON bookings(customer_id,id);
CREATE INDEX ix_booking_status ON bookings(status,id);
CREATE TABLE notification_outbox(
 id BIGSERIAL PRIMARY KEY,booking_id BIGINT NOT NULL REFERENCES bookings(id),event_key VARCHAR(120) NOT NULL UNIQUE,
 recipient VARCHAR(320) NOT NULL,subject VARCHAR(255) NOT NULL,body TEXT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','SENT','FAILED')),
 attempts INT NOT NULL DEFAULT 0,next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 locked_until TIMESTAMP WITH TIME ZONE,claim_token VARCHAR(36),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,sent_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX ix_outbox_pending ON notification_outbox(status,next_attempt_at);
CREATE TABLE audit_events(id BIGSERIAL PRIMARY KEY,actor_id BIGINT NOT NULL REFERENCES app_users(id),action VARCHAR(60) NOT NULL,target_id BIGINT NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP);

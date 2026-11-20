-- Approved policy: configured per hotel, captured into each new booking.
-- No retroactive policy is assigned to existing bookings.
ALTER TABLE hotels ADD COLUMN cancellation_policy VARCHAR(50) NOT NULL DEFAULT 'FULL_REFUND_24H_V1';
ALTER TABLE hotels ADD CONSTRAINT ck_hotel_cancellation_policy CHECK(cancellation_policy IN ('FULL_REFUND_24H_V1'));

ALTER TABLE app_users ADD COLUMN phone VARCHAR(20);
ALTER TABLE app_users ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE app_users ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;
-- Existing creation timestamps are unknown. Only future inserts receive the current timestamp.
ALTER TABLE app_users ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE app_users ADD CONSTRAINT uq_app_users_phone UNIQUE(phone);

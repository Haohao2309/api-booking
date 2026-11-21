-- One-time codes for "forgot password". Only a BCrypt hash of the code is stored.
CREATE TABLE password_reset_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id),
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_password_reset_codes_attempts CHECK (failed_attempts >= 0)
);

CREATE INDEX idx_password_reset_codes_user ON password_reset_codes (user_id, id);

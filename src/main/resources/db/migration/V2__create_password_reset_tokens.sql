CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_password_reset_tokens_email_id ON password_reset_tokens (email, id DESC);

CREATE TABLE IF NOT EXISTS refresh_token_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_sessions_user_id
    ON refresh_token_sessions(user_id);

CREATE INDEX IF NOT EXISTS idx_refresh_token_sessions_expires_at
    ON refresh_token_sessions(expires_at);

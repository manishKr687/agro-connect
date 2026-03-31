CREATE TABLE IF NOT EXISTS blacklisted_tokens (
    token TEXT PRIMARY KEY,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_blacklisted_tokens_expires_at
    ON blacklisted_tokens (expires_at);

CREATE TABLE IF NOT EXISTS revoked_users (
    username VARCHAR(50) PRIMARY KEY,
    revoked_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS login_attempts (
    username VARCHAR(50) PRIMARY KEY,
    failure_count INTEGER NOT NULL,
    locked_until TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_login_attempts_locked_until
    ON login_attempts (locked_until);

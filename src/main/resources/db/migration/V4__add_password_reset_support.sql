ALTER TABLE users
    ADD COLUMN email VARCHAR(100);

ALTER TABLE users
    ADD COLUMN phone_number VARCHAR(20);

ALTER TABLE users
    ADD CONSTRAINT uk_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uk_users_phone_number UNIQUE (phone_number);

CREATE TABLE password_reset_challenges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    identifier VARCHAR(120) NOT NULL,
    secret_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_password_reset_challenges_lookup
    ON password_reset_challenges(channel, identifier, created_at DESC);

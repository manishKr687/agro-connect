CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'FARMER', 'RETAILER', 'AGENT'))
);

CREATE TABLE IF NOT EXISTS harvests (
    id             BIGSERIAL PRIMARY KEY,
    farmer_id      BIGINT REFERENCES users(id),
    crop_name      VARCHAR(100),
    quantity       DOUBLE PRECISION,
    harvest_date   DATE,
    expected_price DOUBLE PRECISION,
    status         VARCHAR(30) CHECK (status IN ('AVAILABLE', 'RESERVED', 'SOLD', 'WITHDRAWAL_REQUESTED', 'WITHDRAWN'))
);

CREATE TABLE IF NOT EXISTS demands (
    id                       BIGSERIAL PRIMARY KEY,
    retailer_id              BIGINT REFERENCES users(id),
    crop_name                VARCHAR(100),
    quantity                 DOUBLE PRECISION,
    required_date            DATE,
    target_price             DOUBLE PRECISION,
    requested_quantity       DOUBLE PRECISION,
    requested_required_date  DATE,
    requested_target_price   DOUBLE PRECISION,
    change_request_reason    VARCHAR(500),
    status                   VARCHAR(20) CHECK (status IN ('OPEN', 'RESERVED', 'FULFILLED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS delivery_tasks (
    id                BIGSERIAL PRIMARY KEY,
    harvest_id        BIGINT NOT NULL REFERENCES harvests(id),
    demand_id         BIGINT NOT NULL REFERENCES demands(id),
    assigned_agent_id BIGINT NOT NULL REFERENCES users(id),
    assigned_by_id    BIGINT NOT NULL REFERENCES users(id),
    status            VARCHAR(20) CHECK (status IN ('ASSIGNED', 'ACCEPTED', 'REJECTED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED')),
    assigned_at       TIMESTAMP,
    accepted_at       TIMESTAMP,
    picked_up_at      TIMESTAMP,
    in_transit_at     TIMESTAMP,
    delivered_at      TIMESTAMP,
    rejection_reason  VARCHAR(500)
);

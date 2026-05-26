CREATE TABLE IF NOT EXISTS subscriber (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    stripe_customer_id VARCHAR(255),
    plan_tier VARCHAR(32) NOT NULL,
    subscribed_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    last_payment_at TIMESTAMPTZ,
    CONSTRAINT uk_subscriber_email UNIQUE (email),
    CONSTRAINT uk_subscriber_stripe_customer_id UNIQUE (stripe_customer_id)
);

CREATE INDEX IF NOT EXISTS idx_subscriber_expires_at
    ON subscriber (expires_at);

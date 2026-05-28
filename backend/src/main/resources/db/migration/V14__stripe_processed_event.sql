CREATE TABLE IF NOT EXISTS stripe_processed_event (
    event_id     VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMPTZ  NOT NULL
);

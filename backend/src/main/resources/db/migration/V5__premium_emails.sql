ALTER TABLE subscriber
    ADD COLUMN IF NOT EXISTS cancellation_email_sent_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_subscriber_cancellation_email
    ON subscriber (expires_at, cancellation_email_sent_at);

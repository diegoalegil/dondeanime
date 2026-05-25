CREATE TABLE IF NOT EXISTS api_key (
    id BIGSERIAL PRIMARY KEY,
    api_key VARCHAR(96) NOT NULL UNIQUE,
    owner_email VARCHAR(255) NOT NULL,
    tier VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ,
    monthly_quota BIGINT NOT NULL,
    monthly_usage BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_api_key_owner_email ON api_key (owner_email);
CREATE INDEX IF NOT EXISTS idx_api_key_tier ON api_key (tier);

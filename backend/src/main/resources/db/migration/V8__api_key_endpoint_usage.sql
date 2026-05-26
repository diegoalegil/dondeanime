CREATE TABLE api_key_endpoint_usage (
    id BIGSERIAL PRIMARY KEY,
    api_key_id BIGINT NOT NULL REFERENCES api_key(id) ON DELETE CASCADE,
    endpoint VARCHAR(255) NOT NULL,
    monthly_usage BIGINT NOT NULL DEFAULT 0,
    last_used_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_api_key_endpoint_usage_key_endpoint UNIQUE (api_key_id, endpoint)
);

CREATE INDEX idx_api_key_endpoint_usage_api_key ON api_key_endpoint_usage(api_key_id);
CREATE INDEX idx_api_key_endpoint_usage_endpoint ON api_key_endpoint_usage(endpoint);

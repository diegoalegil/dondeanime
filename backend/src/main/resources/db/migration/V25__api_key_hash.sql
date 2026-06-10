-- Las API keys dejan de guardarse en claro: solo hash SHA-256 (hex) + preview para el admin.
ALTER TABLE api_key ADD COLUMN IF NOT EXISTS key_hash VARCHAR(64);
ALTER TABLE api_key ADD COLUMN IF NOT EXISTS key_preview VARCHAR(20);

UPDATE api_key
SET key_hash = encode(sha256(api_key::bytea), 'hex'),
    key_preview = CASE
        WHEN length(api_key) <= 16 THEN api_key
        ELSE left(api_key, 12) || '...' || right(api_key, 4)
    END
WHERE key_hash IS NULL;

ALTER TABLE api_key ALTER COLUMN key_hash SET NOT NULL;
ALTER TABLE api_key ALTER COLUMN key_preview SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_api_key_hash ON api_key (key_hash);

ALTER TABLE api_key DROP COLUMN IF EXISTS api_key;

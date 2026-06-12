-- Revocación de sesiones admin: denylist de jti hasta que expire el token.
-- Sin esto, un JWT admin filtrado vale 8h sí o sí; con logout el jti entra
-- aquí y el filtro lo rechaza aunque la firma/exp sigan siendo válidas.
CREATE TABLE IF NOT EXISTS admin_revoked_token (
    jti        VARCHAR(36) PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NOT NULL
);

-- Para purgar entradas ya expiradas (se hace al revocar, sin cron propio).
CREATE INDEX IF NOT EXISTS idx_admin_revoked_token_expires ON admin_revoked_token (expires_at);

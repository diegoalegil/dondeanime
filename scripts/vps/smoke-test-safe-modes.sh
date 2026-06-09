#!/usr/bin/env bash
# Smoke tests for safe VPS script modes. Intended for CI.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TMP_DIR="$(mktemp -d)"

cleanup() {
    rm -rf "$TMP_DIR"
}
trap cleanup EXIT

ENV_FILE="$TMP_DIR/env.prod"
cat > "$ENV_FILE" <<'ENV'
POSTGRES_USER=dondeanime_user
POSTGRES_DB=dondeanime
ADMIN_PASSWORD=old-admin-password
BACKUP_RETENTION_DAYS=30
BACKUP_VERIFY_MIN_RATIO_PERCENT=95
R2_BUCKET=dondeanime-backups
R2_ACCOUNT_ID=00000000000000000000000000000000
R2_ACCESS_KEY_ID=dummy-access-key
R2_SECRET_ACCESS_KEY=dummy-secret-key
R2_PREFIX=postgres
ENV
chmod 600 "$ENV_FILE"

cd "$ROOT_DIR"

PROJECT_DIR="$ROOT_DIR" ENV_FILE="$ENV_FILE" bash scripts/backup-postgres-r2.sh --check-config
PROJECT_DIR="$ROOT_DIR" ENV_FILE="$ENV_FILE" bash scripts/vps/verify-backup.sh --check-config
bash scripts/vps/setup-hardening.sh --dry-run >/dev/null
UPTIMEROBOT_API_KEY=dummy \
UPTIMEROBOT_ALERT_CONTACT_IDS=123456 \
UPTIMEROBOT_INTERVAL_SECONDS=300 \
UPTIMEROBOT_TIMEOUT_SECONDS=30 \
    bash scripts/vps/setup-uptimerobot-monitors.sh --dry-run >/dev/null
bash scripts/vps/rotate-secret.sh --env-file "$ENV_FILE" --secret ADMIN_PASSWORD --dry-run >/dev/null

printf 'rotated-admin-password\nrotated-admin-password\n' \
    | bash scripts/vps/rotate-secret.sh --env-file "$ENV_FILE" --secret ADMIN_PASSWORD >/dev/null
grep -Fq 'ADMIN_PASSWORD=rotated-admin-password' "$ENV_FILE"

printf 'Safe VPS script smoke tests OK\n'

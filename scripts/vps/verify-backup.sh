#!/usr/bin/env bash
# Verify the latest Cloudflare R2 Postgres backup without touching prod data.
#
# The script:
#   1. Downloads the latest dondeanime-postgres-*.sql.gz from R2.
#   2. Restores it into a temporary Postgres Docker container on 127.0.0.1:5444.
#   3. Compares key table row counts against the production Postgres container.
#   4. Sends a Telegram alert on failure when TELEGRAM_* env vars are configured.

set -euo pipefail

PROJECT_DIR="${PROJECT_DIR:-/opt/dondeanime}"
ENV_FILE="${ENV_FILE:-$PROJECT_DIR/.env.prod}"
VERIFY_DIR="${BACKUP_VERIFY_DIR:-$PROJECT_DIR/backup-verification}"
VERIFY_CONTAINER="${BACKUP_VERIFY_CONTAINER:-dondeanime_backup_verify}"
VERIFY_PORT="${BACKUP_VERIFY_POSTGRES_PORT:-5444}"
MIN_RATIO_PERCENT="${BACKUP_VERIFY_MIN_RATIO_PERCENT:-95}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-dondeanime_postgres_prod}"
AWS_CLI_IMAGE="${AWS_CLI_IMAGE:-amazon/aws-cli:2}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:16-alpine}"
CRON_FILE="/etc/cron.d/dondeanime-backup-verify"
ERROR_MESSAGE="Backup verification failed"

INSTALL_CRON=0
CHECK_CONFIG=0
if [[ "${1:-}" == "--install-cron" ]]; then
    INSTALL_CRON=1
elif [[ "${1:-}" == "--check-config" ]]; then
    CHECK_CONFIG=1
elif [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    cat <<'USAGE'
Usage:
  bash scripts/vps/verify-backup.sh
  bash scripts/vps/verify-backup.sh --check-config
  sudo bash scripts/vps/verify-backup.sh --install-cron

The default run verifies the latest R2 backup.
--check-config validates required env without touching Docker or R2.
--install-cron installs a weekly Sunday 04:00 UTC cron entry.
USAGE
    exit 0
elif [[ -n "${1:-}" ]]; then
    printf 'Unknown argument: %s\n' "$1" >&2
    exit 2
fi

if [[ "$#" -gt 1 ]]; then
    printf 'Only one argument is supported\n' >&2
    exit 2
fi

log() {
    printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*" >&2
}

fail() {
    ERROR_MESSAGE="$*"
    log "ERROR: $*"
    exit 1
}

require_cmd() {
    command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

load_env() {
    [[ -f "$ENV_FILE" ]] || fail "ENV_FILE not found: $ENV_FILE"
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
}

telegram_alert() {
    local text="$1"
    [[ "${TELEGRAM_ENABLED:-false}" == "true" ]] || return 0
    [[ -n "${TELEGRAM_BOT_TOKEN:-}" && -n "${TELEGRAM_CHAT_ID:-}" ]] || return 0

    curl -fsS \
        -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage" \
        --data-urlencode "chat_id=${TELEGRAM_CHAT_ID}" \
        --data-urlencode "text=${text}" \
        >/dev/null || true
}

cleanup() {
    local status=$?
    if command -v docker >/dev/null 2>&1; then
        docker rm -f "$VERIFY_CONTAINER" >/dev/null 2>&1 || true
    fi
    if [[ "$status" -ne 0 && "$CHECK_CONFIG" -eq 0 && "$INSTALL_CRON" -eq 0 ]]; then
        telegram_alert "DondeAnime backup verification FAILED: ${ERROR_MESSAGE}"
    fi
}
trap cleanup EXIT

install_cron() {
    [[ "${EUID:-$(id -u)}" -eq 0 ]] || fail "Run --install-cron as root with sudo"
    mkdir -p "$PROJECT_DIR/logs"
    cat > "$CRON_FILE" <<CRON
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

0 4 * * 0 deploy cd $PROJECT_DIR && scripts/vps/verify-backup.sh >> $PROJECT_DIR/logs/backup-verify.log 2>&1
CRON
    chmod 644 "$CRON_FILE"
    log "Installed weekly backup verification cron: $CRON_FILE"
}

require_r2_env() {
    [[ -n "${R2_BUCKET:-}" ]] || fail "R2_BUCKET is required"
    [[ -n "${R2_ACCOUNT_ID:-}" ]] || fail "R2_ACCOUNT_ID is required"
    [[ -n "${R2_ACCESS_KEY_ID:-}" ]] || fail "R2_ACCESS_KEY_ID is required"
    [[ -n "${R2_SECRET_ACCESS_KEY:-}" ]] || fail "R2_SECRET_ACCESS_KEY is required"
}

validate_common_config() {
    [[ "$MIN_RATIO_PERCENT" =~ ^[0-9]+$ ]] || fail "BACKUP_VERIFY_MIN_RATIO_PERCENT must be numeric"
    [[ "$MIN_RATIO_PERCENT" -ge 1 && "$MIN_RATIO_PERCENT" -le 100 ]] || fail "BACKUP_VERIFY_MIN_RATIO_PERCENT must be 1..100"
    [[ "$VERIFY_PORT" =~ ^[0-9]+$ ]] || fail "BACKUP_VERIFY_POSTGRES_PORT must be numeric"
    [[ -n "${POSTGRES_DB:-}" ]] || fail "POSTGRES_DB is required"
    [[ -n "${POSTGRES_USER:-}" ]] || fail "POSTGRES_USER is required"
}

check_config() {
    load_env
    require_r2_env
    validate_common_config
    log "Backup verification config OK for ENV_FILE=$ENV_FILE"
}

r2_endpoint() {
    printf '%s' "${R2_ENDPOINT:-https://$R2_ACCOUNT_ID.r2.cloudflarestorage.com}"
}

r2_prefix() {
    local prefix="${R2_PREFIX:-postgres}"
    prefix="${prefix#/}"
    prefix="${prefix%/}"
    printf '%s' "$prefix"
}

aws_cli() {
    docker run --rm \
        -e AWS_ACCESS_KEY_ID="$R2_ACCESS_KEY_ID" \
        -e AWS_SECRET_ACCESS_KEY="$R2_SECRET_ACCESS_KEY" \
        -v "$VERIFY_DIR:/work" \
        "$AWS_CLI_IMAGE" \
        "$@" \
        --endpoint-url "$(r2_endpoint)" \
        --region auto
}

remote_base_uri() {
    local prefix
    prefix="$(r2_prefix)"
    if [[ -n "$prefix" ]]; then
        printf 's3://%s/%s' "$R2_BUCKET" "$prefix"
    else
        printf 's3://%s' "$R2_BUCKET"
    fi
}

find_latest_backup_key() {
    local base_uri="$1"
    aws_cli s3 ls "$base_uri/" --recursive \
        | awk '$4 ~ /dondeanime-postgres-[0-9]{8}T[0-9]{6}Z\.sql\.gz$/ { print $4 }' \
        | sort \
        | tail -n 1
}

download_latest_backup() {
    mkdir -p "$VERIFY_DIR"
    chmod 700 "$VERIFY_DIR"

    local base_uri latest_key backup_name
    base_uri="$(remote_base_uri)"
    latest_key="$(find_latest_backup_key "$base_uri")"
    [[ -n "$latest_key" ]] || fail "No dondeanime-postgres-*.sql.gz backup found in R2"

    backup_name="$(basename "$latest_key")"
    log "Downloading latest backup: $latest_key"
    aws_cli s3 cp "s3://$R2_BUCKET/$latest_key" "/work/$backup_name" --only-show-errors

    if aws_cli s3 cp "s3://$R2_BUCKET/$latest_key.sha256" "/work/$backup_name.sha256" --only-show-errors >/dev/null 2>&1; then
        verify_checksum "$VERIFY_DIR/$backup_name" "$VERIFY_DIR/$backup_name.sha256"
    else
        log "WARN: checksum not found for $latest_key"
    fi

    printf '%s' "$VERIFY_DIR/$backup_name"
}

verify_checksum() {
    local backup_path="$1"
    local checksum_path="$2"
    local expected actual

    expected="$(awk '{ print $1 }' "$checksum_path")"
    actual="$(sha256sum "$backup_path" | awk '{ print $1 }')"
    [[ "$expected" == "$actual" ]] || fail "Checksum mismatch for $(basename "$backup_path")"
    log "Checksum OK"
}

wait_for_verify_postgres() {
    for _ in $(seq 1 30); do
        if docker exec "$VERIFY_CONTAINER" pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null 2>&1; then
            return 0
        fi
        sleep 2
    done
    fail "Temporary Postgres did not become ready"
}

restore_backup() {
    local backup_path="$1"
    local verify_password
    verify_password="verify-$(date -u +%s)"

    log "Starting temporary Postgres container $VERIFY_CONTAINER on 127.0.0.1:$VERIFY_PORT"
    docker rm -f "$VERIFY_CONTAINER" >/dev/null 2>&1 || true
    docker run -d \
        --name "$VERIFY_CONTAINER" \
        -e "POSTGRES_DB=$POSTGRES_DB" \
        -e "POSTGRES_USER=$POSTGRES_USER" \
        -e "POSTGRES_PASSWORD=$verify_password" \
        -p "127.0.0.1:${VERIFY_PORT}:5432" \
        "$POSTGRES_IMAGE" \
        >/dev/null

    wait_for_verify_postgres

    log "Restoring backup into temporary Postgres"
    gunzip -c "$backup_path" \
        | docker exec -i "$VERIFY_CONTAINER" \
            psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
            >/dev/null
}

table_count() {
    local container="$1"
    local table="$2"
    docker exec "$container" \
        psql -At -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
        -c "select count(*) from ${table};"
}

cleanup_old_verification_dumps() {
    # Los dumps descargados para verificar se acumulan en VERIFY_DIR.
    # Borra los de más de 14 días para no llenar el disco del VPS.
    [[ -d "$VERIFY_DIR" ]] || return 0
    find "$VERIFY_DIR" -maxdepth 1 -type f \
        \( -name 'dondeanime-postgres-*.sql.gz' -o -name 'dondeanime-postgres-*.sql.gz.sha256' \) \
        -mtime +14 -delete || true
}

compare_table() {
    local table="$1"
    local prod_count restored_count min_required

    prod_count="$(table_count "$POSTGRES_CONTAINER" "$table")"
    restored_count="$(table_count "$VERIFY_CONTAINER" "$table")"
    [[ "$prod_count" =~ ^[0-9]+$ ]] || fail "Invalid prod count for $table: $prod_count"
    [[ "$restored_count" =~ ^[0-9]+$ ]] || fail "Invalid restored count for $table: $restored_count"

    min_required=$(( (prod_count * MIN_RATIO_PERCENT + 99) / 100 ))
    log "$table: prod=$prod_count restored=$restored_count minimum=$min_required"
    [[ "$restored_count" -ge "$min_required" ]] || fail "$table restored count is below ${MIN_RATIO_PERCENT}% of prod"
}

verify_backup() {
    require_cmd docker
    require_cmd gunzip
    require_cmd sha256sum
    require_cmd awk
    require_cmd sort
    require_cmd tail
    require_cmd curl
    load_env
    require_r2_env
    validate_common_config

    local prod_running
    prod_running="$(docker inspect -f '{{.State.Running}}' "$POSTGRES_CONTAINER" 2>/dev/null || true)"
    [[ "$prod_running" == "true" ]] || fail "Production Postgres container is not running: $POSTGRES_CONTAINER"

    local backup_path
    backup_path="$(download_latest_backup)"
    restore_backup "$backup_path"

    compare_table "anime"
    compare_table "watch_provider"
    compare_table "affiliate_link"

    cleanup_old_verification_dumps

    log "Backup verification completed successfully"
}

if [[ "$INSTALL_CRON" -eq 1 ]]; then
    install_cron
elif [[ "$CHECK_CONFIG" -eq 1 ]]; then
    check_config
else
    verify_backup
fi

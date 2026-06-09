#!/usr/bin/env bash
# Interactively update one approved secret in .env.prod without printing it.

set -euo pipefail

PROJECT_DIR="${PROJECT_DIR:-/opt/dondeanime}"
ENV_FILE="${ENV_FILE:-$PROJECT_DIR/.env.prod}"
SECRET_NAME=""
RESTART_BACKEND=0
DRY_RUN=0

usage() {
    cat <<'USAGE'
Usage:
  bash scripts/vps/rotate-secret.sh --secret ADMIN_PASSWORD
  bash scripts/vps/rotate-secret.sh --secret JWT_SECRET --restart-backend
  bash scripts/vps/rotate-secret.sh --env-file /opt/dondeanime/.env.prod --secret RESEND_API_KEY
  bash scripts/vps/rotate-secret.sh --secret ADMIN_PASSWORD --dry-run

Allowed secrets:
  ADMIN_PASSWORD
  EMBEDDING_API_KEY
  JWT_SECRET
  PLAUSIBLE_API_KEY
  PREMIUM_ACCESS_TOKEN_SECRET
  RESEND_API_KEY
  R2_SECRET_ACCESS_KEY
  STRIPE_SECRET_KEY
  STRIPE_WEBHOOK_SECRET
  TELEGRAM_BOT_TOKEN
  TMDB_API_KEY
  TRAKT_CLIENT_SECRET
  TRAKT_TOKEN_ENCRYPTION_SECRET
  VAPID_PRIVATE_KEY
  VERCEL_DEPLOY_HOOK
USAGE
}

while [[ "$#" -gt 0 ]]; do
    case "$1" in
        --secret)
            SECRET_NAME="${2:-}"
            shift 2
            ;;
        --env-file)
            ENV_FILE="${2:-}"
            shift 2
            ;;
        --restart-backend)
            RESTART_BACKEND=1
            shift
            ;;
        --dry-run)
            DRY_RUN=1
            shift
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            printf 'Unknown argument: %s\n' "$1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

log() {
    printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"
}

fail() {
    log "ERROR: $*"
    exit 1
}

is_allowed_secret() {
    case "$1" in
        ADMIN_PASSWORD|EMBEDDING_API_KEY|JWT_SECRET|PLAUSIBLE_API_KEY|PREMIUM_ACCESS_TOKEN_SECRET|RESEND_API_KEY|R2_SECRET_ACCESS_KEY|STRIPE_SECRET_KEY|STRIPE_WEBHOOK_SECRET|TELEGRAM_BOT_TOKEN|TMDB_API_KEY|TRAKT_CLIENT_SECRET|TRAKT_TOKEN_ENCRYPTION_SECRET|VAPID_PRIVATE_KEY|VERCEL_DEPLOY_HOOK)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

read_secret_twice() {
    local first second
    printf 'New value for %s: ' "$SECRET_NAME" >&2
    IFS= read -rs first
    printf '\nRepeat new value for %s: ' "$SECRET_NAME" >&2
    IFS= read -rs second
    printf '\n' >&2

    [[ -n "$first" ]] || fail "Secret value cannot be empty"
    [[ "$first" == "$second" ]] || fail "Secret values do not match"
    printf '%s' "$first"
}

update_env_file() {
    local new_value="$1"
    local tmp

    [[ -f "$ENV_FILE" ]] || fail "ENV_FILE not found: $ENV_FILE"
    [[ -w "$ENV_FILE" ]] || fail "ENV_FILE is not writable: $ENV_FILE"

    tmp="$(mktemp "${ENV_FILE}.XXXXXX")"
    chmod 600 "$tmp"

    awk -v key="$SECRET_NAME" -v value="$new_value" '
        BEGIN { found = 0 }
        $0 ~ "^" key "=" {
            print key "=" value
            found = 1
            next
        }
        { print }
        END {
            if (found == 0) {
                print key "=" value
            }
        }
    ' "$ENV_FILE" > "$tmp"

    mv "$tmp" "$ENV_FILE"
    chmod 600 "$ENV_FILE"
}

restart_backend() {
    command -v docker >/dev/null 2>&1 || fail "docker is required to restart backend"
    docker compose -f "$PROJECT_DIR/docker-compose.prod.yml" --env-file "$ENV_FILE" up -d backend
}

main() {
    [[ -n "$SECRET_NAME" ]] || {
        usage >&2
        exit 2
    }
    is_allowed_secret "$SECRET_NAME" || fail "Secret is not allowed: $SECRET_NAME"
    [[ -f "$ENV_FILE" ]] || fail "ENV_FILE not found: $ENV_FILE"
    [[ -w "$ENV_FILE" ]] || fail "ENV_FILE is not writable: $ENV_FILE"

    if [[ "$DRY_RUN" -eq 1 ]]; then
        log "DRY RUN: would update $SECRET_NAME in $ENV_FILE"
        if [[ "$RESTART_BACKEND" -eq 1 ]]; then
            log "DRY RUN: would restart backend"
        fi
        return 0
    fi

    local new_value
    new_value="$(read_secret_twice)"
    update_env_file "$new_value"
    log "$SECRET_NAME updated in $ENV_FILE"

    if [[ "$RESTART_BACKEND" -eq 1 ]]; then
        restart_backend
        log "Backend restarted"
    else
        log "Backend not restarted. Run with --restart-backend when the app must reload this value."
    fi
}

main

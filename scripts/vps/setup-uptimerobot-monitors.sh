#!/usr/bin/env bash
# Create the external UptimeRobot monitors for DondeAnime.
#
# Required:
#   UPTIMEROBOT_API_KEY
#
# Optional:
#   UPTIMEROBOT_ALERT_CONTACT_IDS=123456,789012
#
# Telegram alerts are configured in UptimeRobot first. Pass the Telegram
# alert contact id here so the three monitors notify that contact.

set -euo pipefail

API_URL="https://api.uptimerobot.com/v2"
INTERVAL_SECONDS="${UPTIMEROBOT_INTERVAL_SECONDS:-300}"
TIMEOUT_SECONDS="${UPTIMEROBOT_TIMEOUT_SECONDS:-30}"
DRY_RUN=0
LIST_CONTACTS=0

for arg in "$@"; do
    case "$arg" in
        --dry-run)
            DRY_RUN=1
            ;;
        --list-contacts)
            LIST_CONTACTS=1
            ;;
        --help|-h)
            cat <<'USAGE'
Usage:
  UPTIMEROBOT_API_KEY=... bash scripts/vps/setup-uptimerobot-monitors.sh
  UPTIMEROBOT_API_KEY=... bash scripts/vps/setup-uptimerobot-monitors.sh --dry-run
  UPTIMEROBOT_API_KEY=... bash scripts/vps/setup-uptimerobot-monitors.sh --list-contacts

Optional:
  UPTIMEROBOT_ALERT_CONTACT_IDS=123456,789012
  UPTIMEROBOT_INTERVAL_SECONDS=300
  UPTIMEROBOT_TIMEOUT_SECONDS=30
USAGE
            exit 0
            ;;
        *)
            printf 'Unknown argument: %s\n' "$arg" >&2
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

require_cmd() {
    command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

require_api_key() {
    [[ -n "${UPTIMEROBOT_API_KEY:-}" ]] || fail "UPTIMEROBOT_API_KEY is required"
}

format_alert_contacts() {
    local ids="${UPTIMEROBOT_ALERT_CONTACT_IDS:-}"
    [[ -n "$ids" ]] || return 0

    local formatted=""
    local id
    IFS=',' read -ra parts <<< "$ids"
    for id in "${parts[@]}"; do
        id="${id//[[:space:]]/}"
        [[ -n "$id" ]] || continue
        [[ "$id" =~ ^[0-9]+$ ]] || fail "Invalid UptimeRobot alert contact id: $id"
        if [[ -n "$formatted" ]]; then
            formatted+="-"
        fi
        formatted+="${id}_0_0"
    done
    printf '%s' "$formatted"
}

post_uptimerobot() {
    local endpoint="$1"
    shift

    curl -fsS \
        -H "Cache-Control: no-cache" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        --data-urlencode "api_key=${UPTIMEROBOT_API_KEY}" \
        --data-urlencode "format=json" \
        "$@" \
        "${API_URL}/${endpoint}"
}

ensure_success() {
    local response="$1"
    local label="$2"

    if ! printf '%s' "$response" | grep -Eq '"stat"[[:space:]]*:[[:space:]]*"ok"'; then
        printf '%s\n' "$response" >&2
        fail "UptimeRobot API did not accept $label"
    fi
}

list_contacts() {
    require_api_key
    require_cmd curl
    post_uptimerobot "getAlertContacts"
}

create_monitor() {
    local friendly_name="$1"
    local url="$2"
    local alert_contacts="$3"

    if [[ "$DRY_RUN" -eq 1 ]]; then
        log "DRY RUN: would create monitor '${friendly_name}' -> ${url}, interval=${INTERVAL_SECONDS}s"
        return 0
    fi

    local args=(
        --data-urlencode "type=1"
        --data-urlencode "friendly_name=${friendly_name}"
        --data-urlencode "url=${url}"
        --data-urlencode "interval=${INTERVAL_SECONDS}"
        --data-urlencode "timeout=${TIMEOUT_SECONDS}"
    )

    if [[ -n "$alert_contacts" ]]; then
        args+=(--data-urlencode "alert_contacts=${alert_contacts}")
    fi

    log "Creating monitor '${friendly_name}'"
    local response
    response="$(post_uptimerobot "newMonitor" "${args[@]}")"
    ensure_success "$response" "$friendly_name"
}

main() {
    require_api_key
    require_cmd curl

    if [[ "$LIST_CONTACTS" -eq 1 ]]; then
        list_contacts
        return 0
    fi

    [[ "$INTERVAL_SECONDS" =~ ^[0-9]+$ ]] || fail "UPTIMEROBOT_INTERVAL_SECONDS must be numeric"
    [[ "$TIMEOUT_SECONDS" =~ ^[0-9]+$ ]] || fail "UPTIMEROBOT_TIMEOUT_SECONDS must be numeric"

    local alert_contacts
    alert_contacts="$(format_alert_contacts)"

    if [[ -z "$alert_contacts" ]]; then
        log "No UPTIMEROBOT_ALERT_CONTACT_IDS set; monitors will be created without alert contacts"
    fi

    create_monitor "DondeAnime frontend" "https://dondeanime.com" "$alert_contacts"
    create_monitor "DondeAnime API anime" "https://api.dondeanime.com/api/anime" "$alert_contacts"
    create_monitor "DondeAnime API health" "https://api.dondeanime.com/actuator/health" "$alert_contacts"

    log "UptimeRobot monitor setup completed"
}

main

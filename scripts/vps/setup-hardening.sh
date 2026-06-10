#!/usr/bin/env bash
# DondeAnime VPS hardening.
#
# Prepared for Diego to run manually on the production VPS after review.
# It configures fail2ban for SSH, disables password/root SSH login and
# enables unattended security upgrades. It must not be executed from CI.

set -euo pipefail

CHECK_ONLY=0
DRY_RUN=0
if [[ "${1:-}" == "--check" ]]; then
    CHECK_ONLY=1
elif [[ "${1:-}" == "--dry-run" ]]; then
    DRY_RUN=1
elif [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    cat <<'USAGE'
Usage:
  sudo bash scripts/vps/setup-hardening.sh
  sudo bash scripts/vps/setup-hardening.sh --check
  bash scripts/vps/setup-hardening.sh --dry-run

Run without arguments to apply hardening.
Run with --check to verify current fail2ban, sshd and unattended-upgrades state.
Run with --dry-run to print the planned changes without requiring root.
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
    printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"
}

fail() {
    log "ERROR: $*"
    exit 1
}

require_root() {
    [[ "${EUID:-$(id -u)}" -eq 0 ]] || fail "Run as root with sudo"
}

require_cmd() {
    command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

detect_sshd_test_command() {
    if command -v sshd >/dev/null 2>&1; then
        printf '%s' "$(command -v sshd)"
    elif [[ -x /usr/sbin/sshd ]]; then
        printf '%s' /usr/sbin/sshd
    else
        fail "sshd binary not found"
    fi
}

set_sshd_option() {
    local key="$1"
    local value="$2"
    local file="/etc/ssh/sshd_config"

    if grep -Eq "^[#[:space:]]*${key}[[:space:]]+" "$file"; then
        sed -i -E "s|^[#[:space:]]*${key}[[:space:]].*|${key} ${value}|" "$file"
    else
        printf '\n%s %s\n' "$key" "$value" >> "$file"
    fi
}

restart_ssh_service() {
    if systemctl list-unit-files | grep -q '^ssh\.service'; then
        systemctl restart ssh
    elif systemctl list-unit-files | grep -q '^sshd\.service'; then
        systemctl restart sshd
    else
        fail "No ssh/sshd systemd service found"
    fi
}

check_state() {
    log "Checking fail2ban sshd jail"
    systemctl is-enabled fail2ban
    systemctl is-active fail2ban
    fail2ban-client status sshd

    log "Checking effective SSH hardening"
    local sshd_bin
    sshd_bin="$(detect_sshd_test_command)"
    "$sshd_bin" -t
    "$sshd_bin" -T | grep -E '^(passwordauthentication|permitrootlogin|maxauthtries|pubkeyauthentication) '

    log "Checking unattended upgrades"
    systemctl is-enabled unattended-upgrades
    systemctl is-active unattended-upgrades
    grep -E 'APT::Periodic::(Update-Package-Lists|Unattended-Upgrade)' /etc/apt/apt.conf.d/20auto-upgrades
}

dry_run_plan() {
    cat <<'PLAN'
Dry run: no files will be changed and no services will be restarted.
Planned VPS hardening:
- install fail2ban and unattended-upgrades
- enable fail2ban sshd jail with maxretry=3, findtime=10m, bantime=1h
- back up /etc/ssh/sshd_config before editing it
- set PasswordAuthentication no
- set PermitRootLogin no
- set MaxAuthTries 3
- set PubkeyAuthentication yes
- validate sshd config before restarting SSH
- enable daily unattended security upgrades
PLAN
}

apply_hardening() {
    require_root
    require_cmd apt-get
    require_cmd systemctl
    require_cmd sed
    require_cmd grep

    log "Installing fail2ban and unattended-upgrades"
    apt-get update -qq
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq fail2ban unattended-upgrades

    log "Configuring fail2ban sshd jail"
    mkdir -p /etc/fail2ban/jail.d
    cat > /etc/fail2ban/jail.d/dondeanime-sshd.conf <<'JAIL'
[sshd]
enabled = true
port = ssh
filter = sshd
backend = systemd
maxretry = 3
findtime = 10m
bantime = 1h
JAIL

    systemctl enable --now fail2ban
    systemctl restart fail2ban

    log "Hardening SSH daemon config"
    local backup
    backup="/etc/ssh/sshd_config.dondeanime.$(date -u +%Y%m%dT%H%M%SZ).bak"
    cp /etc/ssh/sshd_config "$backup"
    chmod 600 "$backup"

    set_sshd_option "PasswordAuthentication" "no"
    set_sshd_option "PermitRootLogin" "no"
    set_sshd_option "MaxAuthTries" "3"
    set_sshd_option "PubkeyAuthentication" "yes"

    local sshd_bin
    sshd_bin="$(detect_sshd_test_command)"
    "$sshd_bin" -t
    restart_ssh_service

    log "Enabling unattended security upgrades"
    cat > /etc/apt/apt.conf.d/20auto-upgrades <<'APT'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
APT::Periodic::AutocleanInterval "7";
APT
    systemctl enable --now unattended-upgrades

    log "Hardening applied. Keep the current SSH session open and test a new deploy login before closing it."
    check_state
}

if [[ "$DRY_RUN" -eq 1 ]]; then
    dry_run_plan
elif [[ "$CHECK_ONLY" -eq 1 ]]; then
    require_root
    check_state
else
    apply_hardening
fi

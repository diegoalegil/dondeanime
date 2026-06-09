#!/usr/bin/env bash
# Validate that DEPLOY.md keeps the required operational controls.

set -euo pipefail

DOC="${1:-DEPLOY.md}"

[[ -f "$DOC" ]] || {
    printf 'Missing %s\n' "$DOC" >&2
    exit 1
}

required_text=(
    "### Backup de BD"
    "scripts/backup-postgres-r2.sh --check-config"
    "scripts/vps/verify-backup.sh --check-config"
    "scripts/vps/setup-hardening.sh --dry-run"
    "scripts/vps/setup-uptimerobot-monitors.sh --dry-run"
    "scripts/vps/rotate-secret.sh --dry-run"
    "docker compose -f docker-compose.prod.yml down"
)

for text in "${required_text[@]}"; do
    if ! grep -Fq "$text" "$DOC"; then
        printf 'Missing required deploy doc text: %s\n' "$text" >&2
        exit 1
    fi
done

if grep -Fq "docker compose -f docker-compose.prod.yml down -v" "$DOC"; then
    printf 'Deploy doc must not suggest down -v in production\n' >&2
    exit 1
fi

printf 'Deploy doc validation OK\n'

#!/usr/bin/env bash
# Validate that the secrets rotation playbook covers required secrets.

set -euo pipefail

DOC="${1:-SECRETS-ROTATION.md}"

[[ -f "$DOC" ]] || {
    printf 'Missing %s\n' "$DOC" >&2
    exit 1
}

required_sections=(
    "## ADMIN_PASSWORD"
    "## JWT_SECRET"
    "## RESEND_API_KEY"
    "## R2_SECRET_ACCESS_KEY"
    "## Secretos Con Rotacion Simple"
    "## POSTGRES_PASSWORD"
    "## Incidente De Fuga"
)

for section in "${required_sections[@]}"; do
    if ! grep -Fq "$section" "$DOC"; then
        printf 'Missing required section: %s\n' "$section" >&2
        exit 1
    fi
done

grep -Fq "scripts/vps/rotate-secret.sh" "$DOC" || {
    printf 'Missing rotate-secret script reference\n' >&2
    exit 1
}

required_secret_names=(
    "TMDB_API_KEY"
    "TRAKT_CLIENT_SECRET"
    "TRAKT_TOKEN_ENCRYPTION_SECRET"
    "STRIPE_SECRET_KEY"
    "STRIPE_WEBHOOK_SECRET"
    "TELEGRAM_BOT_TOKEN"
    "EMBEDDING_API_KEY"
    "VAPID_PRIVATE_KEY"
)

for secret_name in "${required_secret_names[@]}"; do
    if ! grep -Fq "$secret_name" "$DOC"; then
        printf 'Missing secret rotation coverage: %s\n' "$secret_name" >&2
        exit 1
    fi
done

printf 'Secrets rotation doc validation OK\n'

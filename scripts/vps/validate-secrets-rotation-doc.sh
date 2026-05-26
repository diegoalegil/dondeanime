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

printf 'Secrets rotation doc validation OK\n'

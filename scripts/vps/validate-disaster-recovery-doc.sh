#!/usr/bin/env bash
# Validate that the disaster recovery runbook keeps its required sections.

set -euo pipefail

DOC="${1:-DISASTER-RECOVERY.md}"

[[ -f "$DOC" ]] || {
    printf 'Missing %s\n' "$DOC" >&2
    exit 1
}

required_headings=(
    "## VPS Muerto"
    "## BD Corrupta"
    "## Cloudflare Caído"
    "## Vercel Caído"
    "## Drill Trimestral"
)

for heading in "${required_headings[@]}"; do
    if ! grep -Fq "$heading" "$DOC"; then
        printf 'Missing required heading: %s\n' "$heading" >&2
        exit 1
    fi
done

grep -Fq "scripts/vps/verify-backup.sh" "$DOC" || {
    printf 'Missing backup verification reference\n' >&2
    exit 1
}

grep -Fq "scripts/vps/verify-backup.sh --check-config" "$DOC" || {
    printf 'Missing backup verification preflight reference\n' >&2
    exit 1
}

grep -Fq "127.0.0.1:5444" "$DOC" || {
    printf 'Missing isolated restore port reference\n' >&2
    exit 1
}

grep -Fq ".env.prod" "$DOC" || {
    printf 'Missing env restoration reference\n' >&2
    exit 1
}

printf 'Disaster recovery doc validation OK\n'

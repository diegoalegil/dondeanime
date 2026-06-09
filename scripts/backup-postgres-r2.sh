#!/usr/bin/env bash
# ============================================================
# DondeAnime - Backup de Postgres a disco local + Cloudflare R2.
# ------------------------------------------------------------
# Uso en el VPS:
#   cd /opt/dondeanime
#   scripts/backup-postgres-r2.sh
#   scripts/backup-postgres-r2.sh --check-config
#
# Lee .env.prod por defecto. No imprime secretos.
# ============================================================

set -euo pipefail

log() {
    printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"
}

fail() {
    log "ERROR: $*"
    exit 1
}

require_cmd() {
    command -v "$1" >/dev/null 2>&1 || fail "Comando requerido no encontrado: $1"
}

CHECK_CONFIG=0
case "${1:-}" in
    --check-config)
        CHECK_CONFIG=1
        ;;
    --help|-h)
        cat <<'USAGE'
Uso:
  scripts/backup-postgres-r2.sh
  scripts/backup-postgres-r2.sh --check-config

--check-config valida .env.prod y la configuracion R2 sin tocar Docker,
sin crear dumps y sin subir nada.
USAGE
        exit 0
        ;;
    "")
        ;;
    *)
        fail "Argumento desconocido: $1"
        ;;
esac

if [[ "$#" -gt 1 ]]; then
    fail "Solo se admite un argumento"
fi

PROJECT_DIR="${PROJECT_DIR:-/opt/dondeanime}"
ENV_FILE="${ENV_FILE:-$PROJECT_DIR/.env.prod}"

if [[ -f "$ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
else
    fail "No existe ENV_FILE=$ENV_FILE"
fi

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-dondeanime_postgres_prod}"
BACKUP_DIR="${BACKUP_DIR:-$PROJECT_DIR/backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
AWS_CLI_IMAGE="${AWS_CLI_IMAGE:-amazon/aws-cli:2}"
R2_PREFIX="${R2_PREFIX:-postgres}"

r2_config_present() {
    [[ -n "${R2_BUCKET:-}" || -n "${R2_ACCOUNT_ID:-}" || -n "${R2_ACCESS_KEY_ID:-}" || -n "${R2_SECRET_ACCESS_KEY:-}" ]]
}

validate_config() {
    [[ -n "${POSTGRES_USER:-}" ]] || fail "POSTGRES_USER no esta definido"
    [[ -n "${POSTGRES_DB:-}" ]] || fail "POSTGRES_DB no esta definido"
    [[ "$BACKUP_RETENTION_DAYS" =~ ^[0-9]+$ ]] || fail "BACKUP_RETENTION_DAYS debe ser numerico"

    if r2_config_present; then
        [[ -n "${R2_BUCKET:-}" ]] || fail "R2_BUCKET no esta definido"
        [[ -n "${R2_ACCOUNT_ID:-}" ]] || fail "R2_ACCOUNT_ID no esta definido"
        [[ -n "${R2_ACCESS_KEY_ID:-}" ]] || fail "R2_ACCESS_KEY_ID no esta definido"
        [[ -n "${R2_SECRET_ACCESS_KEY:-}" ]] || fail "R2_SECRET_ACCESS_KEY no esta definido"
    fi
}

validate_config

if [[ "$CHECK_CONFIG" -eq 1 ]]; then
    log "Configuracion de backup OK para ENV_FILE=$ENV_FILE"
    exit 0
fi

require_cmd docker
require_cmd gzip
require_cmd find

container_running="$(docker inspect -f '{{.State.Running}}' "$POSTGRES_CONTAINER" 2>/dev/null || true)"
[[ "$container_running" == "true" ]] || fail "El contenedor $POSTGRES_CONTAINER no está corriendo"

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_name="dondeanime-postgres-$timestamp.sql.gz"
backup_path="$BACKUP_DIR/$backup_name"
tmp_path="$BACKUP_DIR/.$backup_name.tmp"

cleanup() {
    rm -f "$tmp_path"
}
trap cleanup EXIT

log "Creando backup local: $backup_path"
docker exec "$POSTGRES_CONTAINER" \
    pg_dump \
    -U "$POSTGRES_USER" \
    -d "$POSTGRES_DB" \
    --clean \
    --if-exists \
    --no-owner \
    --no-privileges \
    | gzip -9 > "$tmp_path"

mv "$tmp_path" "$backup_path"

if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$backup_path" > "$backup_path.sha256"
elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$backup_path" > "$backup_path.sha256"
else
    log "WARN: sha256sum/shasum no disponible; se omite checksum"
fi

log "Backup creado: $(du -h "$backup_path" | awk '{print $1}')"

if r2_config_present; then
    r2_endpoint="${R2_ENDPOINT:-https://$R2_ACCOUNT_ID.r2.cloudflarestorage.com}"
    r2_prefix_clean="${R2_PREFIX#/}"
    r2_prefix_clean="${r2_prefix_clean%/}"
    if [[ -n "$r2_prefix_clean" ]]; then
        remote_backup="s3://$R2_BUCKET/$r2_prefix_clean/$backup_name"
        remote_checksum="$remote_backup.sha256"
    else
        remote_backup="s3://$R2_BUCKET/$backup_name"
        remote_checksum="$remote_backup.sha256"
    fi

    log "Subiendo backup a R2: $remote_backup"
    docker run --rm \
        -e AWS_ACCESS_KEY_ID="$R2_ACCESS_KEY_ID" \
        -e AWS_SECRET_ACCESS_KEY="$R2_SECRET_ACCESS_KEY" \
        -v "$BACKUP_DIR:/backups:ro" \
        "$AWS_CLI_IMAGE" \
        s3 cp "/backups/$backup_name" "$remote_backup" \
        --endpoint-url "$r2_endpoint" \
        --region auto \
        --only-show-errors

    if [[ -f "$backup_path.sha256" ]]; then
        docker run --rm \
            -e AWS_ACCESS_KEY_ID="$R2_ACCESS_KEY_ID" \
            -e AWS_SECRET_ACCESS_KEY="$R2_SECRET_ACCESS_KEY" \
            -v "$BACKUP_DIR:/backups:ro" \
            "$AWS_CLI_IMAGE" \
            s3 cp "/backups/$backup_name.sha256" "$remote_checksum" \
            --endpoint-url "$r2_endpoint" \
            --region auto \
            --only-show-errors
    fi
else
    log "R2 no configurado; backup guardado solo en local"
fi

if [[ "$BACKUP_RETENTION_DAYS" =~ ^[0-9]+$ && "$BACKUP_RETENTION_DAYS" -gt 0 ]]; then
    log "Aplicando retención local: $BACKUP_RETENTION_DAYS días"
    find "$BACKUP_DIR" -type f \
        \( -name 'dondeanime-postgres-*.sql.gz' -o -name 'dondeanime-postgres-*.sql.gz.sha256' \) \
        -mtime +"$BACKUP_RETENTION_DAYS" \
        -delete
fi

log "Backup completado"

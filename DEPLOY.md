# Deploy a producción — DondeAnime

Guía operativa para desplegar y mantener el backend en Hetzner +
frontend en Vercel + DNS en Cloudflare.

---

## Arquitectura

```
Usuario  ─https://dondeanime.com──▶  Cloudflare (DNS + edge cache)
                                          │
                                          ▼
                                       Vercel  (frontend Astro, HTML estático)
                                          │ build-time fetch
                                          ▼
                              https://api.dondeanime.com
                                          │
                                          ▼
                                  Caddy (reverse proxy + SSL)
                                          │
                                          ▼
                              Backend Spring Boot (Docker)
                                          │
                                          ▼
                                Postgres 16 (Docker volumen)
```

- **Cloudflare**: DNS + cache para `dondeanime.com`. NO proxea `api.dondeanime.com` (nube gris) para que Caddy emita Let's Encrypt sin DNS-01.
- **Vercel**: build estático del frontend Astro. Llama al backend EN BUILD TIME, no desde el navegador del usuario.
- **Hetzner CX22** (Ubuntu 24.04): backend + BD + reverse proxy en Docker.

---

## Primera instalación (one-shot)

### 1. Preparar VPS

Tras comprar el VPS en Hetzner Cloud (CX22, Ubuntu 24.04, Falkenstein):

```bash
# Desde tu Mac, primera conexión con password (que Hetzner envió por email)
ssh root@IP_VPS

# Dentro del VPS:
apt update && apt upgrade -y
apt install -y docker.io docker-compose-v2 ufw git

# Usuario deploy (no usar root)
adduser --disabled-password --gecos "" deploy
usermod -aG docker deploy
usermod -aG sudo deploy

# SSH key del Mac al usuario deploy
mkdir -p /home/deploy/.ssh
cp ~/.ssh/authorized_keys /home/deploy/.ssh/
chown -R deploy:deploy /home/deploy/.ssh
chmod 700 /home/deploy/.ssh
chmod 600 /home/deploy/.ssh/authorized_keys

# Firewall: solo SSH, HTTP y HTTPS
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 443/udp        # HTTP/3
ufw --force enable

# Endurecer SSH: deshabilitar login root y password
sed -i 's/^#\?PermitRootLogin .*/PermitRootLogin no/' /etc/ssh/sshd_config
sed -i 's/^#\?PasswordAuthentication .*/PasswordAuthentication no/' /etc/ssh/sshd_config
systemctl restart ssh

# Salir y reconectar como deploy
exit
```

A partir de aquí, conexión siempre como `ssh deploy@IP_VPS` (sin password, con key).

### 2. Clonar repo y configurar secrets

```bash
ssh deploy@IP_VPS

cd /opt
sudo mkdir dondeanime && sudo chown deploy:deploy dondeanime
cd dondeanime
git clone https://github.com/diegoalegil/dondeanime.git .

# Crear .env.prod desde la plantilla
cp .env.prod.example .env.prod
nano .env.prod
# Rellenar:
#   POSTGRES_PASSWORD: openssl rand -base64 32
#   TMDB_API_KEY: el de tu .env local
#   ADMIN_PASSWORD: openssl rand -base64 32
#   VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY: npx web-push generate-vapid-keys --json
#   PUBLIC_VAPID_PUBLIC_KEY: mismo valor que VAPID_PUBLIC_KEY
#   PLAUSIBLE_API_KEY: opcional, solo si quieres top páginas en dashboard
#   VERCEL_DEPLOY_HOOK: vacío por ahora, se rellena en paso 6

chmod 600 .env.prod   # solo deploy puede leerlo
```

### 3. Levantar stack

```bash
# Build de la imagen del backend (primera vez tarda ~2-3 min)
docker compose -f docker-compose.prod.yml --env-file .env.prod build

# Arrancar todo
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Verificar logs
docker compose -f docker-compose.prod.yml logs -f
# Esperar a ver "Started BackendApplication" y Caddy emitiendo cert.
# Ctrl+C para salir del log (los contenedores siguen corriendo).
```

### 4. Configurar DNS en Cloudflare

Solo cuando Caddy esté listo (ver logs):

- En Cloudflare → `dondeanime.com` → DNS → Records → Add record:
  - Type: `A`
  - Name: `api`
  - IPv4: `IP_VPS`
  - Proxy status: **DNS only** (nube gris) ← importante para Let's Encrypt
  - TTL: Auto
- Esperar 1-2 min a que propague.
- Caddy detectará el dominio y emitirá certificado Let's Encrypt en ~30s.
- Verificar:
  ```bash
  curl -i https://api.dondeanime.com/api/anime
  # 200 OK con JSON
  ```

### 5. Primer sync de datos

Los endpoints de sync son de administración y requieren un JWT admin.
Obtener el token con el login del panel (si el 2FA está activo, añadir
`"totpCode":"123456"` al body):

```bash
ssh deploy@IP_VPS
cd /opt/dondeanime

ADMIN_TOKEN=$(curl -s -X POST https://api.dondeanime.com/api/admin/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"TU_ADMIN_PASSWORD"}' | jq -r '.token')

# Disparar sync inicial (catálogo + matching + providers).
# El scheduler también lo haría en su próximo cron, pero queremos
# datos YA para que Vercel pueda hacer build.
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" https://api.dondeanime.com/api/anime/sync
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" https://api.dondeanime.com/api/anime/match
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" https://api.dondeanime.com/api/anime/sync-providers

# Verificar (el catálogo actual ronda los ~929 anime):
curl https://api.dondeanime.com/api/anime | jq 'length'   # ~929
```

### 6. Configurar Vercel

> **Host canónico:** el sitio canónico es `https://www.dondeanime.com`
> (con `www`). `PUBLIC_SITE_URL` en Vercel DEBE ser
> `https://www.dondeanime.com`; si no, canonicals, sitemap y JSON-LD
> apuntan al host equivocado y Google reparte autoridad entre dos hosts.

- Vercel → Add New → Project → Import `dondeanime` de GitHub.
- Framework Preset: Astro (autodetectado).
- Root Directory: `frontend` (donde está el proyecto Astro).
- Environment Variables:
  - `PUBLIC_API_URL` = `https://api.dondeanime.com`
  - `PUBLIC_SITE_URL` = `https://www.dondeanime.com`
  - `PUBLIC_PLAUSIBLE_ENABLED` = `false` al principio
  - `PUBLIC_PLAUSIBLE_DOMAIN` = `dondeanime.com`
  - `ADSENSE_ENABLED` = `false`
  - `PUBLIC_ADSENSE_ENABLED` = `false`
  - `PUBLIC_ADSENSE_CLIENT_ID` vacío hasta aprobación
- Click Deploy. Primer build tarda ~2 min.
- Cuando termine, abrir la URL `dondeanime-xxx.vercel.app` y verificar que carga con datos reales.

### 7. Apuntar dondeanime.com a Vercel

- Vercel → Project → Settings → Domains → Add `dondeanime.com`.
- Vercel da instrucciones DNS específicas:
  - CNAME para `www` → `cname.vercel-dns.com`
  - A o ALIAS para `@` (apex) → IP que da Vercel
- En Cloudflare → DNS → Add records según lo que diga Vercel:
  - Proxy status: **DNS only** (nube gris) ← Vercel maneja su SSL
- Esperar 1-2 min.
- Verificar `https://dondeanime.com` carga.
- En Vercel → Settings → Domains: marcar `www.dondeanime.com` como
  dominio principal y configurar la redirección apex → www como
  **permanente (308)**, no temporal (307). Un 307 no transfiere señal
  SEO al host canónico. Verificar:
  ```bash
  curl -sI https://dondeanime.com | head -3   # debe devolver 308 → www
  ```

### 8. (Opcional) Webhook auto-rebuild

- Vercel → Project → Settings → Git → Deploy Hooks → Create Hook
  - Name: `backend-scheduler`
  - Branch: `main`
- Copiar la URL del hook (formato `https://api.vercel.com/v1/integrations/deploy/...`)
- SSH al VPS:
  ```bash
  cd /opt/dondeanime
  nano .env.prod
  # Rellenar VERCEL_DEPLOY_HOOK con la URL
  docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
  ```
- A partir de aquí, cada vez que `ProviderSyncService.syncAll()` termine, llamará al hook y Vercel rebuildea.

### 9. Monetización y analítica

#### Links afiliados

- Entrar en `https://dondeanime.com/admin/affiliate-links`.
- Usar el Basic Auth configurado con `ADMIN_USERNAME`/`ADMIN_PASSWORD`.
- Crear un link por provider y país:
  - `providerSlug=crunchyroll`, `country=ES`
  - `providerSlug=amazon-prime-video`, `country=ES`
- No inventar tags. Cada programa de afiliados puede variar por país.

#### Plausible

Para solo cargar el script en el frontend:

- En Vercel:
  - `PUBLIC_PLAUSIBLE_ENABLED=true`
  - `PUBLIC_PLAUSIBLE_DOMAIN=dondeanime.com`

Para que `/admin/dashboard` muestre top páginas desde Plausible API:

- En el VPS, editar `/opt/dondeanime/.env.prod`:
  ```env
  PLAUSIBLE_ENABLED=true
  PLAUSIBLE_API_KEY=TU_TOKEN
  PLAUSIBLE_SITE_ID=dondeanime.com
  ```
- Recrear backend:
  ```bash
  docker compose -f docker-compose.prod.yml --env-file .env.prod up -d backend
  ```

Si `PLAUSIBLE_API_KEY` está vacío, el dashboard sigue funcionando pero la sección de páginas visitadas queda vacía.

#### AdSense

No activar hasta tener aprobación y tráfico suficiente. Cuando toque:

- En Vercel:
  ```env
  ADSENSE_ENABLED=true
  PUBLIC_ADSENSE_ENABLED=true
  PUBLIC_ADSENSE_CLIENT_ID=ca-pub-...
  PUBLIC_ADSENSE_SIDEBAR_SLOT=...
  PUBLIC_ADSENSE_INLINE_SLOT=...
  ```

Sin esos valores, `AdSlot` no renderiza nada.

---

## Operación diaria

Más guías:

- Cloudflare Email Routing y Cache Rules: `docs/cloudflare-operacion.md`.
- Backups Postgres + R2: sección "Backup de BD" de este documento.

### Ver logs

```bash
ssh deploy@IP_VPS
cd /opt/dondeanime
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f caddy
```

### Reiniciar tras un cambio en la config

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
# Solo recrea los servicios cuya config cambió.
```

### Deploy de nueva versión del código

```bash
ssh deploy@IP_VPS
cd /opt/dondeanime
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod build backend
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d backend
# Postgres y Caddy no se recrean si no cambiaron.
```

### Primer deploy con Flyway

Solo la primera vez que producción pase de Hibernate `ddl-auto=update` a
Flyway:

```bash
ssh deploy@IP_VPS
cd /opt/dondeanime
git pull
set -a
. ./.env.prod
set +a

docker run --rm --network dondeanime_internal \
  flyway/flyway:11-alpine \
  -url="jdbc:postgresql://postgres:5432/${POSTGRES_DB}" \
  -user="${POSTGRES_USER}" \
  -password="${POSTGRES_PASSWORD}" \
  -baselineVersion=1 \
  -baselineDescription="schema actual producción" \
  baseline

docker compose -f docker-compose.prod.yml --env-file .env.prod build backend
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d backend
docker compose -f docker-compose.prod.yml logs -f backend
```

Ese `baseline` marca `V1__baseline.sql` como ya aplicado en la BD viva.
Después el backend arranca con `ddl-auto=validate` y Flyway aplica las
migraciones posteriores, empezando por `V2__add_indexes.sql`.

### Backup de BD

```bash
# Dentro del VPS, desde /opt/dondeanime
scripts/backup-postgres-r2.sh --check-config
scripts/backup-postgres-r2.sh
```

El script:

- Lee `/opt/dondeanime/.env.prod`.
- `--check-config` valida variables y R2 sin tocar Docker ni subir nada.
- Crea `/opt/dondeanime/backups/dondeanime-postgres-YYYYMMDDTHHMMSSZ.sql.gz`.
- Crea checksum `.sha256`.
- Sube a Cloudflare R2 si `R2_*` está configurado.
- Borra backups locales de más de `BACKUP_RETENTION_DAYS` días.

Variables en `.env.prod`:

```env
BACKUP_DIR=/opt/dondeanime/backups
BACKUP_RETENTION_DAYS=30
R2_BUCKET=TU_BUCKET
R2_ACCOUNT_ID=TU_ACCOUNT_ID
R2_ACCESS_KEY_ID=TU_ACCESS_KEY
R2_SECRET_ACCESS_KEY=TU_SECRET_KEY
R2_PREFIX=postgres
```

Para automatizar cada 6 horas:

```bash
mkdir -p /opt/dondeanime/logs
crontab -e
```

Añadir:

```cron
17 */6 * * * cd /opt/dondeanime && scripts/backup-postgres-r2.sh >> /opt/dondeanime/logs/backup.log 2>&1
```

En Cloudflare R2, configurar lifecycle del bucket para borrar objetos con prefijo `postgres/` tras 30 días. Así la retención remota no depende del cron.

Restore manual desde un backup local:

```bash
# CUIDADO: el dump lleva --clean --if-exists y puede sobrescribir datos.
gunzip -c /opt/dondeanime/backups/dondeanime-postgres-YYYYMMDDTHHMMSSZ.sql.gz \
  | docker exec -i dondeanime_postgres_prod \
      psql -v ON_ERROR_STOP=1 -U dondeanime_user -d dondeanime
```

Verificación semanal de backups:

```bash
# Ejecutar manualmente
cd /opt/dondeanime
scripts/vps/verify-backup.sh --check-config
scripts/vps/verify-backup.sh

# Instalar cron semanal, domingos a las 04:00 UTC
sudo bash scripts/vps/verify-backup.sh --install-cron
```

El verificador descarga el último backup de R2, lo restaura en un Postgres
temporal (`dondeanime_backup_verify`) ligado a `127.0.0.1:5444` y compara filas
en `anime`, `watch_provider` y `affiliate_link`. La restauración nunca se hace
sobre `dondeanime_postgres_prod`; producción solo se lee con `count(*)`.

Variables opcionales:

```env
BACKUP_VERIFY_DIR=/opt/dondeanime/backup-verification
BACKUP_VERIFY_MIN_RATIO_PERCENT=95
```

Si `TELEGRAM_ENABLED=true`, `TELEGRAM_BOT_TOKEN` y `TELEGRAM_CHAT_ID` están
configurados, un fallo en la verificación manda alerta al mismo bot operativo.

### Rotación de secretos

La rotación operativa vive en `SECRETS-ROTATION.md`. Antes de cambiar nada en
producción, probar que el script apunta al archivo correcto:

```bash
cd /opt/dondeanime
bash scripts/vps/rotate-secret.sh --dry-run --secret ADMIN_PASSWORD
```

El script solo acepta secretos en lista blanca y no imprime valores. Secretos
especiales como `POSTGRES_PASSWORD` tienen procedimiento manual propio porque
requieren tocar la credencial dentro de Postgres, no solo `.env.prod`.

### Hardening SSH y fail2ban

El hardening queda preparado en repo, pero se aplica manualmente desde el VPS
después de revisar el PR. No lo ejecuta CI y no se ejecuta desde una sesión
local.

Aplica:

```bash
ssh deploy@IP_VPS
cd /opt/dondeanime
git pull
bash scripts/vps/setup-hardening.sh --dry-run
sudo bash scripts/vps/setup-hardening.sh
```

El script hace tres cambios:

- Instala y activa `fail2ban` con jail `sshd`, `maxretry=3`, `findtime=10m` y `bantime=1h`.
- Ajusta `/etc/ssh/sshd_config`: `PasswordAuthentication no`, `PermitRootLogin no`, `MaxAuthTries 3`, `PubkeyAuthentication yes`.
- Activa `unattended-upgrades` para parches automáticos de seguridad.

Antes de cerrar la sesión SSH actual, abre otra terminal y verifica que el
login como `deploy` sigue funcionando:

```bash
ssh deploy@IP_VPS
```

Verifica el estado:

```bash
cd /opt/dondeanime
sudo bash scripts/vps/setup-hardening.sh --check
sudo fail2ban-client status sshd
sudo sshd -T | grep -E '^(passwordauthentication|permitrootlogin|maxauthtries)'
systemctl status unattended-upgrades --no-pager
```

Si `sshd -t` falla, el script no reinicia SSH. Deja backup automático en
`/etc/ssh/sshd_config.dondeanime.<timestamp>.bak`.

### Monitoreo externo UptimeRobot

Sprint 11 usa UptimeRobot en free tier para vigilar desde fuera del VPS. Los
monitores esperados son:

| Nombre | URL | Intervalo |
|---|---|---|
| DondeAnime frontend | `https://dondeanime.com` | 5 min |
| DondeAnime API anime | `https://api.dondeanime.com/api/anime` | 5 min |
| DondeAnime API health | `https://api.dondeanime.com/actuator/health` | 5 min |

La alerta Telegram se configura en UptimeRobot como alert contact. Para
localizar el ID:

```bash
cd /opt/dondeanime
UPTIMEROBOT_API_KEY=... bash scripts/vps/setup-uptimerobot-monitors.sh --list-contacts
```

Crear los monitores:

```bash
cd /opt/dondeanime
UPTIMEROBOT_API_KEY=... \
UPTIMEROBOT_ALERT_CONTACT_IDS=123456 \
bash scripts/vps/setup-uptimerobot-monitors.sh
```

Antes de ejecutar contra la API real:

```bash
UPTIMEROBOT_API_KEY=dummy \
UPTIMEROBOT_ALERT_CONTACT_IDS=123456 \
bash scripts/vps/setup-uptimerobot-monitors.sh --dry-run
```

Notas:

- UptimeRobot `type=1` es monitor HTTP(S).
- `interval=300` equivale a check cada 5 minutos.
- `alert_contacts` usa el formato `contactId_0_0`; en free tier el threshold
  y la recurrencia se mantienen en `0`.
- Si Diego prefiere hacerlo por UI, debe crear los mismos 3 monitores HTTP(S),
  intervalo 5 min, y asignar el contacto Telegram monitor por monitor.

### Disparar sync manual

Requiere JWT admin (ver "Primer sync de datos"; con 2FA activo, añadir
`"totpCode"` al body del login):

```bash
ADMIN_TOKEN=$(curl -s -X POST https://api.dondeanime.com/api/admin/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"TU_ADMIN_PASSWORD"}' | jq -r '.token')

curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" https://api.dondeanime.com/api/anime/sync
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" https://api.dondeanime.com/api/anime/match
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" https://api.dondeanime.com/api/anime/sync-providers
```

### Parar todo (mantenimiento)

```bash
docker compose -f docker-compose.prod.yml down
# Volúmenes (datos) se mantienen. Solo se paran los contenedores.
```

---

## Troubleshooting

### Caddy no emite el certificado

Lo más probable: DNS aún no propagado o el registro A está con proxy ON.

```bash
# Verificar DNS desde el VPS
dig api.dondeanime.com +short
# Debe devolver la IP del VPS, no una de Cloudflare (104.x.x.x).

# Ver logs de Caddy
docker compose -f docker-compose.prod.yml logs caddy
# Buscar errores tipo "acme: error" o "DNS challenge".
```

### Backend no conecta a Postgres

```bash
docker compose -f docker-compose.prod.yml logs backend
# Buscar "Connection refused" o "FATAL".

# Verificar que Postgres está healthy
docker compose -f docker-compose.prod.yml ps
```

### TMDB_API_KEY no funciona

```bash
docker exec dondeanime_backend env | grep TMDB
# Debe mostrar el token (no vacío).
# Si está vacío: .env.prod no se cargó. Revisar el --env-file.
```

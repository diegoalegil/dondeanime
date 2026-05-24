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

```bash
ssh deploy@IP_VPS
cd /opt/dondeanime

# Disparar sync inicial (los 100 anime + matching + providers).
# El scheduler también lo haría en su próximo cron, pero queremos
# datos YA para que Vercel pueda hacer build.
curl -X POST https://api.dondeanime.com/api/anime/sync
curl -X POST https://api.dondeanime.com/api/anime/match
curl -X POST https://api.dondeanime.com/api/anime/sync-providers

# Verificar:
curl https://api.dondeanime.com/api/anime | jq 'length'   # 100
```

### 6. Configurar Vercel

- Vercel → Add New → Project → Import `dondeanime` de GitHub.
- Framework Preset: Astro (autodetectado).
- Root Directory: `frontend` (donde está el proyecto Astro).
- Environment Variables:
  - `PUBLIC_API_URL` = `https://api.dondeanime.com`
  - `PUBLIC_SITE_URL` = `https://dondeanime.com`
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

### Backup de BD

```bash
# Dentro del VPS
docker exec dondeanime_postgres_prod \
  pg_dump -U dondeanime_user dondeanime | gzip > /opt/dondeanime/backups/dondeanime-$(date +%Y%m%d).sql.gz
```

TODO Codex Sprint 1: script cron + envío a Cloudflare R2.

### Disparar sync manual

```bash
curl -X POST https://api.dondeanime.com/api/anime/sync
curl -X POST https://api.dondeanime.com/api/anime/match
curl -X POST https://api.dondeanime.com/api/anime/sync-providers
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

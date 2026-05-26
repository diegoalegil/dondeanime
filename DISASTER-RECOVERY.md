# Disaster Recovery - DondeAnime

Runbook operativo para recuperar DondeAnime ante caídas graves. No sustituye a
los backups verificados: asume que `scripts/vps/verify-backup.sh` se ejecuta
cada semana y que existe al menos un backup R2 validado.

## Objetivos

| Métrica | Objetivo |
|---|---|
| RPO | 24h máximo si se restaura desde el último backup diario verificado. |
| RTO backend/API | 2-4h si Hetzner y Cloudflare están operativos. |
| RTO frontend | 30-60 min si Vercel está operativo. |
| Prioridad | Restaurar lectura pública antes que panel admin, métricas o newsletters. |

## Checklist Inicial

1. Confirmar incidente y alcance: frontend, API, BD, DNS/CDN o proveedor.
2. Abrir una nota local con hora UTC, síntomas, comandos ejecutados y resultado.
3. No borrar volúmenes Docker ni backups locales.
4. No rotar secretos durante la recuperación salvo fuga confirmada.
5. Avisar a Diego antes de cualquier restore destructivo sobre producción.

## VPS Muerto

Usar este flujo si `ssh deploy@IP_VPS` no responde, Hetzner marca el servidor
como irrecuperable o el disco está perdido.

### 1. Provisionar servidor nuevo

1. Crear un Hetzner CX22 nuevo con Ubuntu 24.04 en región EU.
2. Apuntar temporalmente un nombre interno en la nota del incidente:
   `dondeanime-recovery-YYYYMMDD`.
3. Conectar como root con la credencial inicial de Hetzner.
4. Subir y ejecutar el bootstrap base:

```bash
scp scripts/setup-vps.sh root@IP_NUEVA:/tmp/
ssh root@IP_NUEVA "bash /tmp/setup-vps.sh"
```

5. Reconectar como `deploy` y aplicar hardening tras confirmar login por key:

```bash
ssh deploy@IP_NUEVA
cd /opt
sudo mkdir -p dondeanime
sudo chown deploy:deploy dondeanime
cd dondeanime
git clone https://github.com/diegoalegil/dondeanime.git .
sudo bash scripts/vps/setup-hardening.sh
```

### 2. Restaurar secretos

La copia cifrada de `.env.prod` debe vivir fuera del repo, por ejemplo en el
password manager de Diego o en un export cifrado tipo `.env.prod.age`.

```bash
cd /opt/dondeanime
# Opción A: pegar desde password manager
nano .env.prod
chmod 600 .env.prod

# Opción B: si existe export cifrado
# age -d -i ~/.ssh/id_ed25519 /ruta/segura/dondeanime.env.prod.age > .env.prod
# chmod 600 .env.prod
```

Nunca copiar `.env.prod` por chats, screenshots o commits.

### 3. Levantar stack vacío

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod build backend
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
docker compose -f docker-compose.prod.yml ps
```

### 4. Restaurar último backup verificado

1. Localizar en R2 el último backup que pasó `verify-backup`.
2. Descargarlo al VPS nuevo.
3. Restaurarlo sobre el Postgres nuevo, todavía sin tráfico público:

```bash
gunzip -c /opt/dondeanime/backups/dondeanime-postgres-YYYYMMDDTHHMMSSZ.sql.gz \
  | docker exec -i dondeanime_postgres_prod \
      psql -v ON_ERROR_STOP=1 -U dondeanime_user -d dondeanime
```

4. Verificar conteos:

```bash
docker exec dondeanime_postgres_prod psql -At -U dondeanime_user -d dondeanime \
  -c "select count(*) from anime;"
docker exec dondeanime_postgres_prod psql -At -U dondeanime_user -d dondeanime \
  -c "select count(*) from watch_provider;"
docker exec dondeanime_postgres_prod psql -At -U dondeanime_user -d dondeanime \
  -c "select count(*) from affiliate_link;"
```

### 5. Cambiar tráfico

1. En Cloudflare DNS, cambiar el registro `A api` a `IP_NUEVA`.
2. Mantener proxy DNS only para `api`.
3. Esperar propagación y probar:

```bash
curl -i https://api.dondeanime.com/api/anime
curl -i https://api.dondeanime.com/actuator/health
```

4. Disparar un rebuild de Vercel si el catálogo restaurado es más antiguo:

```bash
curl -X POST "$VERCEL_DEPLOY_HOOK"
```

## BD Corrupta

Usar este flujo si el VPS está sano pero Postgres devuelve errores de datos,
migración rota o tablas dañadas.

1. Parar backend para cortar escrituras:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod stop backend
```

2. Ejecutar backup de emergencia del estado corrupto, solo para análisis:

```bash
BACKUP_DIR=/opt/dondeanime/backups-corrupt scripts/backup-postgres-r2.sh || true
```

3. Elegir el último backup verificado, no simplemente el más reciente.
4. Restaurar:

```bash
gunzip -c /opt/dondeanime/backups/dondeanime-postgres-YYYYMMDDTHHMMSSZ.sql.gz \
  | docker exec -i dondeanime_postgres_prod \
      psql -v ON_ERROR_STOP=1 -U dondeanime_user -d dondeanime
```

5. Validar conteos y endpoints:

```bash
scripts/vps/verify-backup.sh
curl -i https://api.dondeanime.com/api/anime
curl -i https://api.dondeanime.com/api/providers
```

6. Levantar backend:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d backend
```

## Cloudflare Caído

Hay dos escenarios.

### Panel Cloudflare funciona pero edge/proxy falla

1. Verificar que `api.dondeanime.com` está en DNS only.
2. Si algún registro está proxied, pasarlo a DNS only.
3. Bajar TTL si el panel lo permite.
4. Probar resolución directa:

```bash
dig api.dondeanime.com +short
curl -i https://api.dondeanime.com/api/anime
```

### Cloudflare completo no funciona

1. Mantener operativo el backend en Hetzner.
2. En Namecheap, preparar cambio temporal de nameservers fuera de Cloudflare.
3. Para frontend, usar dominio temporal de Vercel hasta recuperar DNS.
4. Para API, crear DNS temporal en el proveedor alternativo apuntando a la IP
   de Hetzner.
5. Documentar el cambio y revertir a Cloudflare cuando vuelva.

Este plan no es instantáneo: cambiar nameservers puede tardar horas. Mientras
tanto, comunicar la URL temporal de Vercel si hace falta.

## Vercel Caído

Si Vercel no sirve el frontend pero la API y el VPS funcionan, servir build
estático desde el VPS como plan B.

1. En el VPS:

```bash
cd /opt/dondeanime/frontend
npm ci
PUBLIC_API_URL=https://api.dondeanime.com \
PUBLIC_SITE_URL=https://dondeanime.com \
npm run build
```

2. Copiar `frontend/dist` a una ruta servida por Caddy:

```bash
sudo mkdir -p /var/www/dondeanime
sudo rsync -a --delete dist/ /var/www/dondeanime/
sudo chown -R caddy:caddy /var/www/dondeanime || true
```

3. Añadir temporalmente un site block de Caddy para servir `/var/www/dondeanime`
   desde el dominio público.
4. Recargar Caddy:

```bash
docker compose -f /opt/dondeanime/docker-compose.prod.yml exec caddy caddy reload --config /etc/caddy/Caddyfile
```

5. Cuando Vercel vuelva, revertir DNS/config y reconstruir en Vercel.

## Drill Trimestral

Una vez por trimestre, Diego prueba este runbook en un VPS barato temporal.

Checklist del drill:

1. Provisionar VPS de prueba.
2. Restaurar `.env.prod` con valores de prueba o secretos rotados para staging.
3. Descargar un backup R2 verificado.
4. Restaurarlo en el Postgres temporal.
5. Levantar backend y validar `/api/anime`.
6. Medir tiempos reales de RTO/RPO.
7. Destruir el VPS de prueba.
8. Actualizar este documento con lo que haya fallado.

No hacer drills destructivos en producción.

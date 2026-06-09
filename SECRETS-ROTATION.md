# Secrets Rotation - DondeAnime

Playbook para rotar secretos de producción sin commitear valores reales. Todos
los cambios se hacen en `/opt/dondeanime/.env.prod` dentro del VPS y después se
reinician los servicios que leen ese secreto.

## Reglas

1. Nunca pegar secretos en PRs, issues, logs, screenshots o chats.
2. Mantener una sesión SSH abierta mientras se reinicia backend.
3. Cambiar un secreto cada vez y verificar antes de pasar al siguiente.
4. Guardar el nuevo valor en el password manager de Diego.
5. Si hubo fuga, revocar el secreto viejo en el proveedor externo después de
   confirmar que el nuevo funciona.

## Script De Apoyo

El script actualiza `.env.prod` sin imprimir el valor:

```bash
cd /opt/dondeanime
bash scripts/vps/rotate-secret.sh --secret ADMIN_PASSWORD
```

Opciones:

```bash
bash scripts/vps/rotate-secret.sh --secret ADMIN_PASSWORD --restart-backend
bash scripts/vps/rotate-secret.sh --env-file /opt/dondeanime/.env.prod --secret JWT_SECRET
bash scripts/vps/rotate-secret.sh --secret ADMIN_PASSWORD --dry-run
```

Secretos permitidos por el script:

- `ADMIN_PASSWORD`
- `EMBEDDING_API_KEY`
- `JWT_SECRET`
- `PLAUSIBLE_API_KEY`
- `PREMIUM_ACCESS_TOKEN_SECRET`
- `RESEND_API_KEY`
- `R2_SECRET_ACCESS_KEY`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `TELEGRAM_BOT_TOKEN`
- `TMDB_API_KEY`
- `TRAKT_CLIENT_SECRET`
- `TRAKT_TOKEN_ENCRYPTION_SECRET`
- `VAPID_PRIVATE_KEY`
- `VERCEL_DEPLOY_HOOK`

`--dry-run` valida el secreto, el archivo destino y si habría reinicio, pero no
pide valor nuevo ni escribe en `.env.prod`.

## ADMIN_PASSWORD

Cadencia: cada 90 días o inmediatamente si alguien no autorizado pudo verlo.

Generar valor:

```bash
openssl rand -base64 32
```

Rotar:

```bash
cd /opt/dondeanime
bash scripts/vps/rotate-secret.sh --secret ADMIN_PASSWORD --restart-backend
```

Verificar:

```bash
curl -i -u "admin:NUEVO_PASSWORD" https://api.dondeanime.com/api/admin/dashboard
```

Impacto: solo cambia el login admin. No afecta usuarios públicos, alertas,
backups ni frontend estático.

## JWT_SECRET

Cadencia: cuando haya sospecha de fuga, al cambiar personas con acceso, o como
rotación preventiva anual.

Generar valor:

```bash
openssl rand -base64 64
```

Rotar:

```bash
cd /opt/dondeanime
bash scripts/vps/rotate-secret.sh --secret JWT_SECRET --restart-backend
```

Impacto:

- Invalida tokens de confirmación y baja todavía no usados.
- En Sprint 12 también invalidará sesiones admin JWT existentes.
- Antes de rotar por mantenimiento, evitar hacerlo justo después de mandar un
  lote grande de emails de confirmación.

Verificar:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod logs --tail=100 backend
```

## RESEND_API_KEY

Rotar si Resend avisa de compromiso, si aparece en logs, o si se cambia la
cuenta/propietario.

Pasos:

1. Crear una nueva API key en Resend.
2. Actualizar el secreto:

```bash
cd /opt/dondeanime
bash scripts/vps/rotate-secret.sh --secret RESEND_API_KEY --restart-backend
```

3. Enviar una prueba funcional desde el flujo de alertas.
4. Revocar la API key vieja en Resend.

Impacto: durante el reinicio puede fallar un envío puntual, pero el sitio
público sigue funcionando.

## R2_SECRET_ACCESS_KEY

Rotar si Cloudflare R2 avisa de compromiso, si alguien deja el proyecto, o cada
180 días como higiene operativa.

Pasos:

1. En Cloudflare R2, crear un token nuevo con permisos sobre el bucket de
   backups.
2. Actualizar solo `R2_SECRET_ACCESS_KEY`:

```bash
cd /opt/dondeanime
bash scripts/vps/rotate-secret.sh --secret R2_SECRET_ACCESS_KEY
```

3. Probar backup y verificación:

```bash
scripts/backup-postgres-r2.sh
scripts/vps/verify-backup.sh
```

4. Revocar el secreto viejo en Cloudflare.

Impacto: no requiere reiniciar backend porque los scripts leen `.env.prod` en
cada ejecución.

## Secretos Con Rotacion Simple

Estos secretos se pueden rotar con `scripts/vps/rotate-secret.sh` y después
verificar el servicio asociado. Mantener flags opcionales apagados si no hay
cuenta o clave real.

| Secreto | Reinicio | Verificacion |
|---|---|---|
| `TMDB_API_KEY` | backend | `curl -i https://api.dondeanime.com/api/anime` |
| `TRAKT_CLIENT_SECRET` | backend | login Trakt devuelve URL o `503` controlado si falta otra credencial |
| `TRAKT_TOKEN_ENCRYPTION_SECRET` | backend | solo rotar antes de activar Trakt o tras desconectar cuentas |
| `STRIPE_SECRET_KEY` | backend | Checkout sigue `503` si Premium no está configurado completo |
| `STRIPE_WEBHOOK_SECRET` | backend | webhook rechaza firmas inválidas y acepta prueba de Stripe CLI |
| `TELEGRAM_BOT_TOKEN` | backend | fallo simulado de scheduler o mensaje de prueba desde UptimeRobot |
| `EMBEDDING_API_KEY` | backend | cliente sigue desactivado si `EMBEDDINGS_ENABLED=false` |
| `VAPID_PRIVATE_KEY` | backend + frontend env | generar par nuevo y actualizar también `VAPID_PUBLIC_KEY`/`PUBLIC_VAPID_PUBLIC_KEY` |
| `PLAUSIBLE_API_KEY` | backend | dashboard admin no rompe si Plausible está apagado |
| `PREMIUM_ACCESS_TOKEN_SECRET` | backend | invalida enlaces premium firmados anteriores |
| `VERCEL_DEPLOY_HOOK` | ninguno | disparar solo si hay hook real y aprobado |

Ejemplo:

```bash
cd /opt/dondeanime
bash scripts/vps/rotate-secret.sh --secret STRIPE_WEBHOOK_SECRET --dry-run
bash scripts/vps/rotate-secret.sh --secret STRIPE_WEBHOOK_SECRET --restart-backend
```

## POSTGRES_PASSWORD

No está permitido en `rotate-secret.sh`: cambiar solo `.env.prod` rompería la
conexión porque la contraseña también vive dentro del rol de Postgres.

Rotarlo solo en ventana de mantenimiento:

1. Abrir y mantener una sesión SSH como `deploy`.
2. Parar backend para cortar escrituras:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod stop backend
```

3. Cambiar la contraseña dentro de Postgres sin dejarla en shell history:

```bash
docker exec -it dondeanime_postgres_prod psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
\password dondeanime_user
\q
```

4. Actualizar `POSTGRES_PASSWORD` en `/opt/dondeanime/.env.prod`, `chmod 600 .env.prod`.
5. Levantar backend y verificar:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d backend
curl -i https://api.dondeanime.com/actuator/health
scripts/backup-postgres-r2.sh --check-config
scripts/vps/verify-backup.sh --check-config
```

6. Revocar/eliminar el valor anterior del password manager.

## Incidente De Fuga

Si un secreto se filtró:

1. Abrir nota local con hora UTC y origen de la fuga.
2. Rotar el secreto afectado.
3. Verificar servicio.
4. Revocar el secreto anterior en el proveedor.
5. Buscar el valor filtrado en git y logs locales sin volver a imprimirlo.
6. Si llegó al repo, pausar y pedir revisión humana antes de reescribir historia.

No usar `git reset --hard`, `git filter-repo` ni force-push de emergencia sin
aprobación explícita de Diego.

# Deploy after cleanup

Run this on the VPS only. Do not run these commands from local.

## 1. SSH and pull main

```bash
ssh deploy@46.224.162.174
cd /opt/dondeanime
git pull origin main
```

## 2. Update `/opt/dondeanime/.env.prod`

Add or update these variables before starting the new backend.

```bash
# Sprint 14 - Web Push
# Generate with:
#   npx web-push generate-vapid-keys --json
# Copy publicKey to both VAPID_PUBLIC_KEY and PUBLIC_VAPID_PUBLIC_KEY.
VAPID_PUBLIC_KEY=
VAPID_PRIVATE_KEY=
VAPID_SUBJECT=mailto:contacto@dondeanime.com
PUBLIC_VAPID_PUBLIC_KEY=

# Sprint 15 - Stripe Premium
# Keep test keys until Premium is intentionally enabled.
STRIPE_SECRET_KEY=sk_test_...
PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_PRICE_ID=price_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_SUCCESS_URL=https://dondeanime.com/premium?success=1
STRIPE_CANCEL_URL=https://dondeanime.com/premium?canceled=1
STRIPE_PORTAL_RETURN_URL=https://dondeanime.com/premium

# Sprint 6 - critical Telegram alerts
TELEGRAM_ENABLED=false
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
```

Also mirror frontend public variables in Vercel project settings when the feature should be visible:

```text
PUBLIC_VAPID_PUBLIC_KEY
PUBLIC_STRIPE_PUBLISHABLE_KEY
```

## 3. Critical Flyway baseline

Sprint 6 changed production from Hibernate `ddl-auto=update` to `ddl-auto=validate`.
The first start with the new code will fail unless the existing production schema is baselined first.

Use only one of the two options below.

### Option A - baseline with `docker compose exec`

This option does not start the backend. It creates the Flyway history table and marks `V1__baseline.sql` as already applied, so Flyway will run `V2` through the latest migration on the next backend start.

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod stop backend
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d postgres

mkdir -p backups
docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  | gzip > "backups/pre-cleanup-$(date +%Y%m%d-%H%M%S).sql.gz"

docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' <<'SQL'
CREATE TABLE IF NOT EXISTS public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version varchar(50),
    description varchar(200) NOT NULL,
    type varchar(20) NOT NULL,
    script varchar(1000) NOT NULL,
    checksum integer,
    installed_by varchar(100) NOT NULL,
    installed_on timestamp without time zone NOT NULL DEFAULT now(),
    execution_time integer NOT NULL,
    success boolean NOT NULL,
    CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
);

CREATE INDEX IF NOT EXISTS flyway_schema_history_s_idx
    ON public.flyway_schema_history (success);

INSERT INTO public.flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    execution_time,
    success
)
SELECT
    1,
    '1',
    'Baseline existing production schema before cleanup deploy',
    'BASELINE',
    '<< Flyway Baseline >>',
    NULL,
    current_user,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM public.flyway_schema_history
);
SQL

docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select installed_rank, version, description, type, success from flyway_schema_history order by installed_rank;"'
```

If `flyway_schema_history` already contains failed rows, stop and inspect before deleting anything.

### Option B - baseline through Spring Boot Flyway

This option builds the new backend image first, then runs the app once with Flyway baseline flags. It should exit after the non-web context finishes startup.

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod stop backend
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d postgres
docker compose -f docker-compose.prod.yml --env-file .env.prod build backend

mkdir -p backups
docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  | gzip > "backups/pre-cleanup-$(date +%Y%m%d-%H%M%S).sql.gz"

docker compose -f docker-compose.prod.yml --env-file .env.prod run --rm --no-deps --entrypoint sh backend -c 'java $JAVA_OPTS -jar app.jar --spring.flyway.baseline-on-migrate=true --spring.flyway.baseline-version=1 --spring.flyway.baseline-description="Existing production schema before cleanup deploy" --spring.main.web-application-type=none --scheduling.enabled=false'
```

If this command stays attached after printing that the application started, press `Ctrl+C` once. The baseline and migrations run before startup completes.

## 4. Rebuild and restart backend

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod build backend
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d backend
docker compose -f docker-compose.prod.yml logs -f backend
```

Watch the logs for:

```text
Successfully applied
Started BackendApplication
```

## 5. Verify

```bash
curl -s https://api.dondeanime.com/actuator/health   # expect 200
curl -s "https://api.dondeanime.com/api/search?q=naruto"  # expect JSON
```

Also check container status:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod ps
```

## 6. Rollback plan

If the backend does not start or health checks fail:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod stop backend
git log --oneline -5
git checkout <last-known-good-main-commit>
docker compose -f docker-compose.prod.yml --env-file .env.prod build backend
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d backend
docker compose -f docker-compose.prod.yml logs -f backend
```

If a migration partially ran and the database is inconsistent, keep the backend stopped and restore the pre-cleanup backup:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod stop backend
gunzip -c backups/pre-cleanup-YYYYMMDD-HHMMSS.sql.gz \
  | docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d backend
```

Do not run `docker compose down -v` in production. It deletes the Postgres volume.

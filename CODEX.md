# CODEX.md — Plan operativo para el agente Codex

> Este documento es tu **fuente de verdad** para trabajar en DondeAnime.
> Léelo **completo** antes de tocar una sola línea de código.
> Si una instrucción de Diego entra en conflicto con este documento, **para y pregunta** antes de actuar.

---

## 0. Antes de empezar — orden de lectura

Lee estos archivos del repo en este orden exacto:

1. `CODEX.md` (este archivo) — qué tienes que hacer y bajo qué reglas.
2. `CLAUDE.md` — contexto general del proyecto, stack, decisiones técnicas hasta hoy, URLs de producción.
3. `DEPLOY.md` — operación del VPS y troubleshooting.
4. `docs/roadmap.md` — visión a 12 semanas (referencia histórica, no ejecutar de aquí).

Después de leer, **no resumas para Diego** salvo que te lo pida. Si dudas de algo, **pregunta concretamente**.

---

## 1. Tu rol y modo de trabajo

### Quién eres aquí

- Eres un agente Codex (OpenAI) ejecutando trabajo de mantenimiento y nuevas features en DondeAnime.
- El proyecto fue inicializado y desplegado a producción por Diego junto a Claude (Anthropic). Esta documentación viene de esa fase.
- Tienes acceso completo al repo (lectura + escritura + git push).
- Tienes acceso SSH al VPS de producción (`ssh deploy@46.224.162.174`) si Diego te lo concede.

### Cómo trabajas

1. **Sprints de ~2 semanas.** Tres sprints definidos en este documento. Ejecuta uno completo antes de pedir el siguiente.
2. **Branches + Pull Requests.** Nunca pushes directo a `main`. Crea branch `sprint-N/feature-corta`, abre PR, espera review de Diego antes de mergear. Si Diego te dice "mergea tú", entonces mergea.
3. **Commits granulares**, no commits de 800 líneas. Mismo formato que ya hay en el repo (`git log --oneline -20` para ver el estilo).
4. **Sin Co-Authored-By** en los commits. Diego ya decidió que no quiere coautoría de IA en commits.
5. **Idioma**: código en inglés, **comentarios y commits en español**.
6. **Cuando dudes, para y pregunta.** No inventes decisiones técnicas que no estén en este documento o en CLAUDE.md.

### Coordinación con Diego (y con Claude vía Diego)

- **Diego** es tu interlocutor. Reportas a él al cierre de cada sprint.
- **Claude** (sesiones anteriores) no está activo continuamente. Diego puede llamarlo a verificar tu trabajo cuando termine cada sprint. Si Claude detecta deviations, te las harán llegar como feedback.
- Si necesitas algo que no está en este documento (decisión de producto, nuevo dominio, nueva API key), **NO procedas**: pide a Diego.

### Cómo facilitar la verificación posterior

- Cada PR debe tener:
  - Descripción clara de qué hace.
  - Lista de tests añadidos/modificados.
  - Instrucciones de cómo probarlo en local.
  - Si hay cambios de esquema BD: snippet del DDL ejecutado y rollback.
- Mantén el formato existente de commits (`git log` para referencia).
- Si tocas un componente que ya tenía tests, **mantén o añade tests**, no los borres.

---

## 2. Convenciones inviolables

Estas reglas vienen de decisiones tomadas durante el desarrollo. **Romperlas requiere justificación explícita a Diego.**

### Backend (Spring Boot 4 + Java 21)

| Regla | Por qué |
|---|---|
| **Endpoints REST devuelven DTOs (records), nunca entidades JPA crudas.** | Esconde id interno, syncedAt, tmdbId, updatedAt, etc. La entidad es para BD, el DTO para el exterior. |
| **Inyección por constructor**, no por field con `@Autowired`. | Permite `final`, facilita tests sin Spring, hace dependencias explícitas. |
| **Sin Lombok en entidades** (al menos por ahora). | Diego prefiere el boilerplate explícito de getters/setters mientras aprende. |
| **`RestClient` para HTTP** (no `WebClient`, no `RestTemplate`). | Recomendado en Spring 6.1+, síncrono, fluent. |
| **Tests para features nuevas**: mínimo happy path + edge case. | Tests existentes (`SlugifyTest`, `AnimeMatchingServiceTest`, `AnimeControllerTest`) son el patrón. |
| **`@WebMvcTest` está en `org.springframework.boot.webmvc.test.autoconfigure`** (NO en `.servlet`). | Spring Boot 4 reorganizó paquetes. |
| **`@MockitoBean`** (de `org.springframework.test.context.bean.override.mockito`), no `@MockBean`. | `@MockBean` deprecado en Spring Boot 4. |
| **Jackson 3.x: `@JsonNaming` y `PropertyNamingStrategies` viven en `tools.jackson.databind.*`** (no `com.fasterxml.jackson.databind.*`). | Spring Boot 4 usa Jackson 3. `@JsonProperty` sigue en `com.fasterxml.jackson.annotation`. |
| **`@Modifying @Query` JPQL para deletes** que vayan dentro de la misma transacción que inserts. | Derived queries diferian el DELETE hasta el commit, los INSERTs salían primero y chocaban con uniques (bug real ya arreglado). |
| **`TransactionTemplate` en vez de `@Transactional` cuando el método se auto-invoca** desde la misma clase. | Proxies de Spring no interceptan self-invocation. |

### Frontend (Astro 6 + Tailwind 4)

| Regla | Por qué |
|---|---|
| **Astro estático puro** (`output: 'static'`, sin adapter SSR). | SEO long-tail: el HTML completo llega ya renderizado. |
| **`vercel.json` con `cleanUrls: true` y `trailingSlash: false`**. | Astro genera `.html` (`build.format: 'file'`); sin cleanUrls Vercel da 404 en URLs sin extensión. |
| **`PUBLIC_API_URL` y `PUBLIC_SITE_URL` son obligatorias** en runtime y build. | Sin `PUBLIC_SITE_URL`, `new URL()` en SEOHead lanza "Invalid URL" y rompe el build. |
| **Filtrado de provider variantes** (Crunchyroll Amazon Channel, Netflix with Ads, etc.) **se hace en `lib/platforms.ts`**, no en backend. | Backend expone fuente neutra. Frontend decide qué mostrar. |

### Producción

| Regla | Por qué |
|---|---|
| **Cloudflare proxy DESACTIVADO (nube gris) para `api.dondeanime.com`**. | Si lo activas, Let's Encrypt de Caddy no puede emitir cert por HTTP-01. Si quieres proxy ON, hay que migrar a DNS-01 challenge con plugin Caddy + API token Cloudflare. |
| **DNS de `dondeanime.com` y `www.dondeanime.com` apuntan a Vercel con proxy GRIS**. | Vercel maneja su propio SSL. Proxy naranja rompe el cert de Vercel. |
| **Secrets en `/opt/dondeanime/.env.prod` del VPS, NO en repo**. | `.env.prod` está en `.gitignore`. |
| **`ddl-auto=update`** en prod (deuda técnica conocida). | DEUDA TÉCNICA: migrar a Flyway/Liquibase es Sprint 4+ opcional. |

### Git

- Convención de commits: prefijo `Sprint N:` o `Sprint N (week X):`. Mensaje en español, cuerpo con el "por qué", no solo el "qué".
- Sin emojis en commits ni en código (Diego no los usa).
- PR description en español, con sección "Cómo probar" + "Tests añadidos".

---

## 3. Decisiones intocables (con justificación)

Estas decisiones **no se cambian** sin que Diego lo apruebe explícitamente. Si crees que están mal, antes de tocarlas, abre issue/PR de **discusión** (no de cambio) explicando por qué.

### Stack

- **Spring Boot 4 + Java 21**. Backend.
- **PostgreSQL 16** (no 17, no Mongo, no nada más). BD.
- **Astro 6** (no Next.js, no SvelteKit). Frontend.
- **Hetzner CX22 + Docker**. Hosting backend.
- **Vercel + Cloudflare**. Hosting frontend + DNS.

### Modelo de datos

- **Schema actual está congelado** salvo para añadir tablas/columnas nuevas (NUNCA borrar/renombrar columnas existentes sin migración Flyway/Liquibase planeada).
- **Slugs**: lowercase + espacios→guiones para providers y géneros (`ProviderSummaryDto.slugify`, `GenreSummaryDto.slugify`). El slug de anime es más elaborado (normalize en `AnimeSyncService`). No cambiar la regla sin migrar todos los slugs existentes.
- **`tmdbId` puede ser null** (16/100 anime sin match). No asumas que siempre está rellenado.

### Algoritmos

- **Heurística de matching TMDb en 3 pasadas** (JP+año, JP cualquier año, cualquier resultado por popularidad). Cubre regresiones reales (caso MHA Vigilantes 2025). No la simplifiques.
- **Sync de AniList con paginación interna** (AniList cappa `perPage` a 50). No quites la paginación.
- **`ProviderSyncService.syncOne` con `TransactionTemplate` + `deleteByAnimeId` con `@Modifying @Query`**. Es la combinación que arregla un bug real de prod (duplicate key). No la cambies a `@Transactional` simple ni a derived delete.
- **Rate limit 300ms entre llamadas a TMDb** en `AnimeMatchingService` y `ProviderSyncService`. TMDb permite 40 req/10s; con 300ms quedamos en ~33 req/10s, margen para no entrar en throttle.

### Endpoints existentes

- **Contratos REST existentes son estables**. NO renombres `providerSlug`, NO quites `watchProvidersByCountry`, NO cambies el shape de `AnimeDetailResponse`. El frontend depende de esto y rompería sin avisar.
- **Si necesitas exponer datos nuevos**, añade campos al DTO (los clientes existentes los ignoran) o crea un endpoint nuevo. NUNCA breaking changes.

### Scheduler

- **3 jobs con cron 6-campos de Spring** (segundo minuto hora día mes diasem). Cron de Unix es 5 campos, no confundir.
- **`@ConditionalOnProperty("scheduling.enabled")`** sobre el bean. En local OFF, en prod ON. No cambies esto, evita syncs accidentales.
- **Webhook Vercel se dispara al final de `syncProviders`** (último paso del pipeline). NO añadirlo a `syncAniList` ni `matchTmdb` o rebuildearíamos 3 veces al día sin necesidad.

---

## 4. Backlog detallado de 3 sprints

Cada sprint tiene **objetivo claro, entregables verificables, esquema BD, endpoints, errores comunes**. Ejecútalos en orden.

---

### Sprint 1 (semanas 1-2): Enriquecimiento manual top 50 + panel admin

**Objetivo**: que Diego pueda reescribir título/descripción de cualquier anime en español propio, distinto al de AniList, para diferenciación SEO (contenido único = ranking superior).

**Por qué primero**: Las descripciones de AniList son las mismas que tienen 50 webs anime más. Google los detecta como contenido duplicado y reparte la equidad. Reescribir top 50 en voz de Diego = SEO real.

#### Entregable A: esquema BD

Nueva tabla `anime_override`:

```sql
CREATE TABLE anime_override (
  id BIGSERIAL PRIMARY KEY,
  anime_id BIGINT NOT NULL REFERENCES anime(id) ON DELETE CASCADE,
  field_name VARCHAR(50) NOT NULL,          -- "description", "title_english", etc.
  field_value TEXT NOT NULL,                 -- el override real
  locale VARCHAR(5) NOT NULL DEFAULT 'es',   -- por si añadimos otros idiomas
  updated_at TIMESTAMPTZ NOT NULL,
  updated_by VARCHAR(50),                    -- "diego", "admin", quien lo editó
  UNIQUE (anime_id, field_name, locale)
);

CREATE INDEX idx_anime_override_anime ON anime_override (anime_id);
```

Entidad JPA `AnimeOverride`. Repository `AnimeOverrideRepository`.

#### Entregable B: lógica de prioridad en DTOs de salida

`AnimeDetailDto.from(Anime a, List<AnimeOverride> overrides)`:

- Si hay `override` para `description` con `locale='es'` → usa ese valor.
- Si no, usa `anime.description` (el de AniList).
- Mismo para `title_english`, `title_romaji` y cualquier otro campo overrideable.

NO modifiques `AnimeSummaryDto` para soportar overrides (sería romper el contrato y la home cargaría 100 queries de overrides en cada build). Solo el `AnimeDetailDto`.

#### Entregable C: auth básica HTTP

Spring Security con HTTP Basic, NO formularios. Variable `ADMIN_PASSWORD` en `.env.prod`. Usuario único `admin`.

```properties
# application-prod.properties (nuevo bloque)
admin.username=admin
admin.password=${ADMIN_PASSWORD}
```

Solo proteger paths `/api/admin/**`. El resto público.

#### Entregable D: endpoints admin

```
POST   /api/admin/anime/{slug}/override
  Body: { "fieldName": "description", "fieldValue": "...", "locale": "es" }
  Crea o actualiza el override. Devuelve 200 con AnimeDetailDto refrescado.

DELETE /api/admin/anime/{slug}/override?field=description&locale=es
  Borra el override (vuelve a usar el valor de AniList).

GET    /api/admin/anime/{slug}/overrides
  Lista todos los overrides activos para ese anime.
```

#### Entregable E: panel admin en frontend

Página `/admin` (NO protegida por Astro, está en frontend público). Formulario simple para login (envía Basic Auth al backend al hacer requests).

Página `/admin/anime/[slug]` con:
- Lectura del estado actual (campo de AniList + override si existe).
- Textareas para editar título y descripción.
- Botón Guardar (POST al endpoint admin).
- Botón "Resetear al original" (DELETE).

NO uses framework JS pesado. Astro + un poco de JS vanilla en `<script>` es suficiente.

#### Entregable F: tests

- `AnimeOverrideRepositoryTest`: persistencia básica.
- `AnimeDetailDtoTest`: la lógica de "override gana sobre AniList".
- `AnimeAdminControllerTest` (`@WebMvcTest`): auth con/sin credentials, POST/DELETE.

#### Errores comunes que te vas a encontrar

1. **`@ManyToOne` con cascade mal puesto**: si haces `Anime` con `@OneToMany List<AnimeOverride>` y eliminas un anime, no quieres que el cascade se cargue otros datos. Usa solo la FK explícita en `AnimeOverride.anime_id`, sin relación bidireccional.
2. **HTTP Basic con Spring Security 6.x**: la API ha cambiado vs versiones anteriores. Necesitas `SecurityFilterChain` bean con `httpBasic(Customizer.withDefaults())` y `authorizeHttpRequests`. Documenta tu config en el PR.
3. **El admin no puede saltarse CORS**: las requests del frontend admin al backend van con credentials. Configurar CORS adecuadamente en Caddy o en Spring para que el origin de Vercel pueda hacer requests con `Authorization` header.

#### Commits sugeridos

```
Sprint 1: entidad AnimeOverride + repository + migración manual
Sprint 1: endpoints admin POST/DELETE/GET overrides
Sprint 1: HTTP Basic auth en /api/admin/**
Sprint 1: AnimeDetailDto.from prefiere overrides locales
Sprint 1: tests AnimeOverrideRepository + AnimeDetailDto
Sprint 1: tests AnimeAdminController con Basic Auth
Sprint 1: frontend /admin con login Basic
Sprint 1: frontend /admin/anime/[slug] formulario edit
Sprint 1: actualizar CLAUDE.md con admin endpoints
```

---

### Sprint 2 (semanas 3-4): Sistema de alertas email

**Objetivo**: usuario suscribe su email a "avisarme cuando Attack on Titan llegue a Crunchyroll España". Cuando el scheduler detecta ese cambio, envía email.

**Por qué segundo**: retención. La gente vuelve. Vuelven = más probabilidad de click en afiliado.

#### Entregable A: esquema BD

```sql
CREATE TABLE app_user (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  confirmed_at TIMESTAMPTZ,                  -- NULL = pending confirmación
  unsubscribed_at TIMESTAMPTZ                -- NULL = activo
);

CREATE TABLE subscription (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  anime_id BIGINT NOT NULL REFERENCES anime(id) ON DELETE CASCADE,
  country_code VARCHAR(2) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  notified_at TIMESTAMPTZ,                   -- NULL = aún no notificado del cambio
  UNIQUE (user_id, anime_id, country_code)
);

CREATE INDEX idx_subscription_anime ON subscription (anime_id, country_code);
CREATE INDEX idx_subscription_user ON subscription (user_id);
```

#### Entregable B: integración con Resend

Resend tiene 3.000 emails/mes gratis (https://resend.com). API HTTP simple, no SMTP. Token vía `RESEND_API_KEY` en `.env.prod`.

Servicio `EmailService` con:
- `sendConfirmationEmail(email, confirmToken)`
- `sendAlertEmail(email, animeTitle, country, providers)`

Plantillas en HTML simple (Resend acepta HTML directo, no necesita templating).

#### Entregable C: flow doble opt-in

```
POST /api/subscriptions
  Body: { "email": "...", "animeSlug": "...", "country": "es" }
  - Si el email no existe en app_user: crea entrada con confirmed_at=NULL, manda email de confirmación con token JWT corto (15 min).
  - Si existe pero NO confirmado: reenvía email de confirmación.
  - Si existe Y confirmado: crea subscription directamente.
  Respuesta: 202 Accepted con mensaje genérico.

GET /api/subscriptions/confirm?token=...
  Decodifica token, marca app_user.confirmed_at=now(), crea la subscription pending.
  Responde con HTML simple (no JSON) confirmando.

POST /api/subscriptions/unsubscribe?token=...
  Token específico de cada subscription, llega en cada email. Marca app_user.unsubscribed_at.
```

#### Entregable D: detector de cambios en scheduler

Nuevo método en `ProviderSyncService` o en un service nuevo `AlertService`:

```
Después de syncProviders():
  - Detecta deltas: ¿hay subscriptions con (anime_id, country_code) que ahora tienen providers nuevos vs hace 24h?
  - Para cada match: marca subscription.notified_at, envía email.
  - Rate limit: máx 1 email por usuario por anime por día.
```

Estrategia simple: tabla `watch_provider_snapshot` que guarda el estado de hace 24h y compara. O delta query:

```sql
-- Subscriptions cuyo anime tiene providers NUEVOS desde la última notificación
SELECT s.* FROM subscription s
JOIN watch_provider wp ON wp.anime_id = s.anime_id AND wp.country_code = s.country_code
WHERE wp.updated_at > COALESCE(s.notified_at, s.created_at)
  AND s.notified_at IS NULL OR s.notified_at < NOW() - INTERVAL '24 hours';
```

#### Entregable E: frontend

Página de detalle (`/anime/[slug]/[pais]`): botón "Avisarme cuando llegue a [país]".

Modal/form mínimo: email + checkbox "Acepto la política de privacidad" → POST a `/api/subscriptions`.

Mensaje "Te enviamos un correo de confirmación, revisa tu bandeja" tras submit.

#### Entregable F: GDPR mínimo

- Página `/legal/privacidad` con política básica (qué datos guardamos, retención, derecho de borrado).
- Link a `/legal/privacidad` en footer.
- Link "darse de baja" en cada email enviado (token al endpoint de unsubscribe).
- Endpoint `DELETE /api/users/{email}/erase` accesible solo con token de unsubscribe del usuario, borra de BD.

#### Entregable G: tests

- `EmailServiceTest` con mock de Resend (no llamar a la API real en tests).
- `SubscriptionControllerTest` con MockMvc: POST sin auth, POST con email inválido, doble suscripción.
- `AlertServiceTest`: simulación de delta, verifica que se envía email solo para los nuevos.

#### Errores comunes

1. **JWT con clave fija en código**: NO. Generar `JWT_SECRET` en `.env.prod` con `openssl rand -base64 64`. Usar para firmar tokens de confirmación.
2. **Token de confirmación reutilizable**: el token debe expirar (15 min) y ser one-shot (consumido una vez no vale para otra). Guardar `used_at` en tabla `email_token`.
3. **Spam blocking**: Resend desde dominio no verificado va a spam de los usuarios. Configurar SPF + DKIM en Cloudflare para `dondeanime.com` siguiendo las instrucciones de Resend.

#### Commits sugeridos

```
Sprint 2: entidades app_user + subscription + email_token
Sprint 2: EmailService con cliente Resend
Sprint 2: POST /api/subscriptions con doble opt-in JWT
Sprint 2: GET /api/subscriptions/confirm
Sprint 2: scheduler AlertService detecta deltas y envía
Sprint 2: configurar SPF + DKIM Cloudflare para Resend
Sprint 2: frontend modal suscripción en página detalle
Sprint 2: política de privacidad + unsubscribe
Sprint 2: tests EmailService + SubscriptionController + AlertService
```

---

### Sprint 3 (semanas 5-6): Monetización + analítica

**Objetivo**: empezar a generar ingresos vía afiliados, instalar analítica privacy-friendly, dejar slot AdSense preparado para cuando haya tráfico aprobado.

#### Entregable A: tabla affiliate_link

```sql
CREATE TABLE affiliate_link (
  id BIGSERIAL PRIMARY KEY,
  provider_slug VARCHAR(100) NOT NULL,
  country_code VARCHAR(2) NOT NULL,
  affiliate_url TEXT NOT NULL,
  click_count INTEGER NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  UNIQUE (provider_slug, country_code)
);
```

Datos iniciales (los rellena Diego en panel admin):
- `crunchyroll / es` → link de afiliado real de Crunchyroll España.
- `amazon-prime-video / es` → tag Amazon afiliados.
- `crunchyroll / mx` → tag específico LATAM si existe.
- etc.

#### Entregable B: endpoint admin

```
POST   /api/admin/affiliate-links
DELETE /api/admin/affiliate-links/{id}
GET    /api/admin/affiliate-links
```

Solo bajo Basic Auth (mismo que Sprint 1).

#### Entregable C: lógica de override en el `ProviderDto`

Cuando el frontend pide `GET /api/anime/{slug}`, los providers vienen con `logoUrl` y `providerName`. Añadir `affiliateUrl` opcional:

- Si existe `affiliate_link` activo para (provider_slug, country_code): devolverlo.
- Si no: `affiliateUrl=null` y el frontend usa el link genérico de la plataforma.

#### Entregable D: tracking de clicks

Endpoint público:

```
POST /api/track/affiliate
  Body: { "providerSlug": "...", "country": "es", "animeSlug": "..." }
  Incrementa affiliate_link.click_count atómicamente.
  Responde 204 No Content (no devuelve nada, es fire-and-forget desde el browser).
```

Frontend: cuando usuario clica un link de afiliado, dispara fetch a este endpoint en paralelo y abre el link en nueva pestaña con `rel="noopener noreferrer sponsored"`.

#### Entregable E: Plausible Analytics

Plausible es privacy-friendly, sin cookies, no necesita banner GDPR. Self-hosted o servicio gratuito de 30 días trial luego de pago. Para empezar gratis: usar `umami.is` self-hosted o `plausible.io` con su free trial.

Decisión Diego: elegir cuál. Por defecto: **Plausible.io** trial 30 días, luego decisión.

Frontend: añadir el script de Plausible en `<head>` con `data-domain="dondeanime.com"`. Una línea.

Tracking de eventos: dispara `plausible('Affiliate Click', { props: { provider: 'crunchyroll', country: 'es' } })` cuando alguien clica un link de afiliado.

#### Entregable F: slot AdSense preparado

NO activar AdSense aún (requiere 3+ meses de tráfico aprobado). Pero dejar:

- Componente `<AdSlot position="sidebar">` que en `process.env.NODE_ENV === 'production' && ADSENSE_ENABLED === 'true'` renderiza el código AdSense.
- En `<head>`: condicional para incluir el script de AdSense solo si `ADSENSE_ENABLED`.
- Variable `PUBLIC_ADSENSE_CLIENT_ID` y `ADSENSE_ENABLED` en Vercel env vars (vacía por ahora).

Cuando Diego active AdSense (mes 4+), solo cambia `ADSENSE_ENABLED=true` y `PUBLIC_ADSENSE_CLIENT_ID=ca-pub-xxx` y los anuncios aparecen sin más cambios.

#### Entregable G: dashboard métricas mínimo

Página `/admin/dashboard` que muestra:
- Total clicks de afiliado en últimos 7/30 días.
- Top 10 anime más visitados (de Plausible API).
- Anime que generan más clicks de afiliado.

Útil para que Diego vea qué funciona.

#### Errores comunes

1. **AdSense rechaza el site**: requiere contenido original (Sprint 1 ayuda), política privacidad clara (Sprint 2 ayuda), volumen de tráfico mínimo. NO solicitar AdSense hasta tener 3+ meses publicado.
2. **Tag Amazon Afiliados varía por país**: cada país tiene su programa. España es `dondeanime-21`, México sería `dondeanime-21-mx`, etc. Verificar en cada panel de Amazon Associates por país.
3. **Disclosure obligatoria**: si pones links de afiliado tienes que decirlo. Añadir párrafo en footer "Esta web contiene enlaces de afiliado. Si compras a través de ellos podemos recibir una comisión sin coste adicional para ti."

#### Commits sugeridos

```
Sprint 3: entidad AffiliateLink + repository + endpoints admin
Sprint 3: ProviderDto incluye affiliateUrl opcional
Sprint 3: endpoint POST /api/track/affiliate con contador atómico
Sprint 3: frontend cards Ver en X usan affiliateUrl con tracking
Sprint 3: integración Plausible script + eventos custom
Sprint 3: AdSlot component condicional preparado
Sprint 3: footer disclosure afiliados + /legal/afiliados
Sprint 3: dashboard /admin con métricas básicas
Sprint 3: tests AffiliateLink + tracking + dashboard
```

---

## 5. Mejora continua paralela

Tareas que puedes ir haciendo si terminas sprints antes de tiempo, o si Diego te lo pide explícitamente. No bloquean los sprints.

### Tests E2E con Playwright (alta prioridad)

- Setup Playwright en `frontend/`.
- Tests críticos:
  - Home carga y muestra 100+ cards.
  - Click en card lleva a detalle.
  - Suscripción email (Sprint 2): formulario → email confirmación → click confirma → DB tiene la subscription.
  - Click en affiliate (Sprint 3): incrementa contador.
- CI: GitHub Actions que corra Playwright en cada PR contra preview de Vercel.

### Migración a Cloudflare Email Routing

Sustituye los MX de Namecheap por Cloudflare Email Routing (gratis). `contacto@dondeanime.com` → forward a Gmail personal de Diego. Más simple que Namecheap forwarding.

### Backups BD automáticos

Script en `/opt/dondeanime/scripts/backup.sh` que hace `pg_dump` y sube a Cloudflare R2 (10 GB gratis). Cron en VPS cada 6h. Retención: últimos 30 días. Restore manual: documentar en `DEPLOY.md`.

### Page Rules Cloudflare

Aprovechar el plan free de Cloudflare (3 page rules):
- `dondeanime.com/anime/*` → cache aggressive (Edge TTL 1 día).
- `dondeanime.com/sitemap*` → cache 1 hora.
- `api.dondeanime.com/api/anime` → bypass cache (no cachear la API).

### CI/CD

GitHub Actions:
- Lint + test backend en cada push.
- Lint + build frontend en cada PR.
- (Opcional) auto-deploy backend a Hetzner cuando merge a main (ssh + git pull + docker compose up).

### Migración Flyway/Liquibase

DEUDA TÉCNICA: cambiar `ddl-auto=update` por migraciones versionadas. No urgente mientras el schema es estable.

### Switching a output: 'directory' en Astro

Hoy usamos `format: 'file'` + `vercel.json cleanUrls`. Alternativa más portable es cambiar a `format: 'directory'` (genera `/anime/foo/index.html`). Funcionaría en cualquier hosting sin vercel.json. Considerar cuando llegue el momento.

---

## 6. Vetos explícitos

**NO hagas estas cosas sin discutirlo con Diego primero:**

1. **NO sistema de comentarios/foros**. Diego decidió que no.
2. **NO soporte manga, juegos, series JP no-anime**. Scope congelado.
3. **NO cambiar de Spring Boot a Node/Python/Go/etc**.
4. **NO cambiar de Astro a Next.js/SvelteKit/etc**.
5. **NO cambiar Postgres por Mongo/MySQL/SQLite/etc**.
6. **NO añadir Redis/Kafka/RabbitMQ/etc** sin necesidad demostrada.
7. **NO añadir OAuth (Google/GitHub login)** — el sistema de alertas usa email + double opt-in, no necesita auth federada.
8. **NO añadir app móvil nativa** — la web mobile-first ya cumple.
9. **NO internacionalización a otros idiomas** — el target es hispanohablante únicamente.
10. **NO machine learning, recomendaciones avanzadas, o features "inteligentes"** — la propuesta de valor es datos limpios y SEO, no algoritmos.
11. **NO subir el tier de free a paid en Vercel, Cloudflare, Hetzner** sin avisar a Diego con justificación de costes.
12. **NO commits con `Co-Authored-By` de IA**. Diego ya rechazó coautoría.
13. **NO modificar `application.properties`/`application-prod.properties`** para cosas que pueden ir en `.env.prod`. Secrets en env vars, no en repo.
14. **NO desactivar tests existentes** porque "no pasan". Investiga y arregla, o documenta por qué no aplican y borra el test (en PR separado).
15. **NO usar `git push --force` a main**. Nunca.

---

## 7. Cómo verificar que terminaste un sprint

Antes de cerrar el sprint y pedir review a Diego, asegúrate de:

- [ ] Todos los entregables del sprint están en `main` (o en PR pendiente de merge).
- [ ] Tests nuevos pasan en local.
- [ ] El build de Vercel pasa (espera 2-3 min tras último push).
- [ ] El backend en VPS arrancó OK tras el último deploy (`docker compose logs backend --tail 50`).
- [ ] No has roto endpoints existentes (curl rápido a los principales).
- [ ] CLAUDE.md actualizado con el progreso del sprint (sección "Estado actual del proyecto").
- [ ] Has hecho commit final con mensaje "Sprint N: cerrar sprint, ..." que liste qué se hizo.

Cuando creas que está cerrado, dile a Diego: "Sprint N cerrado. Resumen: [3 bullets]. ¿Pasamos al siguiente o revisamos antes?".

---

## 8. Cómo facilitar verificación de Claude

Diego puede llamar a Claude (Anthropic) para verificar tu trabajo al cierre de cada sprint. Para que esa verificación sea rápida y útil:

1. **Commits granulares con buenos mensajes**. Claude leerá `git log -20` y entenderá lo que hiciste si los mensajes son claros.
2. **PR descriptions completas**. No "fix bug". Explica el porqué.
3. **Tests automatizados**. Claude correrá `./mvnw test` y `npm test`. Si pasan, gran indicador de calidad.
4. **CLAUDE.md actualizado**. Si Claude lee el documento y ve que no menciona tus cambios, asume que algo está mal.
5. **No cambies decisiones de la sección "Decisiones intocables"** sin documentar en el PR el porqué. Si Claude ve que cambiaste algo de esa lista sin justificación, va a pedir rollback.

Si Diego te dice "Claude reportó que X está mal", **NO te pongas a la defensiva**. Analiza el feedback, corrige, hazlo bien. Claude tiene contexto histórico de decisiones que tú no tienes.

---

## 9. Glosario rápido

- **DTO** (Data Transfer Object): record de Java para mover datos entre capas sin exponer entidad cruda.
- **Slug**: parte URL-friendly de un nombre (ej. "Crunchyroll" → "crunchyroll", "Attack on Titan" → "attack-on-titan").
- **Override**: valor sustitutivo del dato de AniList, editorial, escrito por Diego (Sprint 1).
- **Doble opt-in**: usuario da email → recibe email confirmación → click confirma. Estándar GDPR para newsletters.
- **Affiliate disclosure**: aviso legal obligatorio cuando un sitio usa links de afiliado.
- **`@Modifying @Query`**: anotación Spring Data para queries que modifican (no SELECT). Necesario para DELETE/UPDATE custom.
- **`TransactionTemplate`**: API programática de transacciones, alternativa a `@Transactional`. Necesaria para self-invocation.

---

Última actualización: cierre del deploy (mes 2 del roadmap). Si encuentras información obsoleta, **avisa a Diego antes de actualizar el documento**.

# CODEX BACKLOG V2 — Sprints 5 al 12

> Extensión operativa de `CODEX.md`. Cubre ~2-3 meses de trabajo continuo.
> Codex sigue las MISMAS convenciones inviolables de `CODEX.md` (DTOs no entidades,
> heurística de matching intocable, `deleteByAnimeId` con `@Modifying @Query`,
> sin Lombok en código nuevo, sin Co-Authored-By, PRs no push directo a main,
> commit format `Sprint N: ...`, 4-6 tests verdes por sprint).

## Reglas adicionales globales

- **Cada sprint = 1 rama** (`sprint-5`, `sprint-6`, etc.). PRs individuales mergean a la rama del sprint. Cuando el sprint cierra, se hace un PR final sprint-N → main.
- **NUNCA tocar `BackendApplication`, `application.properties`, `docker-compose.prod.yml` salvo que el sprint lo requiera explícitamente.** Si hace falta una variable nueva, va a `.env.prod.example` con default seguro Y se documenta en CLAUDE.md.
- **Ningún PR sin tests.** Mínimo 1 test nuevo por archivo nuevo de lógica. Lectura: si tocas `AnimeMatchingService`, hay test antes de commit.
- **Refactors solo si están en el sprint.** Codex NO refactoriza código tocado por otro PR aunque "esté feo". Eso es scope creep.
- **Cero dependencias nuevas sin justificar.** Cualquier nueva lib (Spring o npm) lleva 2 líneas en el PR explicando por qué no se puede con lo que ya hay.
- **Performance: nada que añada > 50ms al endpoint p95.** Si un cambio mete latencia, lleva caché o se descarta.

---

## Sprint 5 — Testing masivo + cobertura

**Objetivo:** subir de 31 tests a 80+. Activar Testcontainers para tests con Postgres real. Frontend con Playwright E2E mínimo viable. Lighthouse CI en GitHub Actions.

**Branch:** `sprint-5`
**PRs esperados:** 5

### PR 5.1 — Testcontainers backend
- Añadir `org.testcontainers:postgresql:1.20.x` a `pom.xml` (test scope).
- Clase `AbstractIntegrationTest` con `@Testcontainers` + `@DynamicPropertySource` que arranca Postgres 16-alpine.
- Migrar `AnimeOverrideRepositoryTest` y `AffiliateLinkServiceTest` a usar Testcontainers (ahora usan H2 implícito).
- Test nuevo: `ProviderSyncServiceIntegrationTest` que valida que `deleteByAnimeId` + `insert` no genera duplicate key.
- **Veto:** no tocar la heurística de `AnimeMatchingService`.

### PR 5.2 — Tests de controllers REST con MockMvc
- 6 tests nuevos repartidos en: `GenreControllerTest`, `SeasonControllerTest`, `ProviderControllerTest`, `SitemapControllerTest`.
- Cubrir: respuesta 200 con cuerpo esperado, 404 cuando aplica, 400 cuando aplica (`/api/seasons/{year}/INVALID`).
- Usar `@WebMvcTest` + `@MockitoBean` para el service. NO `@SpringBootTest` (lento).

### PR 5.3 — Playwright E2E (frontend)
- `frontend/tests/e2e/` con Playwright 1.4x.
- 3 specs mínimos:
  - `home.spec.ts`: home carga, buscador filtra, click en card abre detalle.
  - `country.spec.ts`: `/pais/es` lista anime con providers de ES.
  - `availability-alert.spec.ts`: en una página país sin providers, el form de alerta es visible y al rellenar muestra confirmación.
- Workflow `.github/workflows/e2e.yml` que arranca backend + frontend + tira Playwright en headless.
- **Veto:** no usar fixtures de mock. Si la suite es lenta, se paraleliza con `--workers=2`, no se simplifica.

### PR 5.4 — Lighthouse CI
- `.github/workflows/lighthouse.yml` que corre Lighthouse contra `https://dondeanime.com` en cada PR.
- Umbrales mínimos (fallar PR si no los cumple): Performance 85, Accessibility 95, SEO 95, Best Practices 90.
- Comentario automático en el PR con scores.

### PR 5.5 — Cobertura backend con JaCoCo
- Plugin JaCoCo en `pom.xml`.
- Configurar mínimo 70% line coverage en `mvn verify`. Si baja, falla.
- Excluir DTOs, entidades JPA, `BackendApplication`, configs.
- README backend con cómo ver el report HTML local.

---

## Sprint 6 — Flyway + Observabilidad

**Objetivo:** salir de `ddl-auto=update` (deuda técnica reconocida). Métricas Prometheus para monitorizar. Logs estructurados en JSON. Health checks robustos.

**Branch:** `sprint-6`
**PRs esperados:** 5

### PR 6.1 — Migración a Flyway
- `org.flywaydb:flyway-core` + `flyway-database-postgresql` en `pom.xml`.
- `src/main/resources/db/migration/V1__baseline.sql` con el schema actual EXACTO (generado con `pg_dump --schema-only` del Postgres prod).
- `V2__add_indexes.sql` con índices que faltan (ver PR 6.2).
- Cambiar `spring.jpa.hibernate.ddl-auto=update` → `validate` en `application-prod.properties`.
- `application.properties` local sigue en `update` para no romper dev.
- **Veto:** NO migrar datos en el script V1. Solo schema. Los datos viven y se regeneran del scheduler.
- **Plan de despliegue documentado en el PR:** primero `flyway baseline` en prod manualmente para marcar V1 como aplicada, luego deploy.

### PR 6.2 — Índices Postgres
- Identificar queries lentas con `EXPLAIN ANALYZE` y `spring.jpa.show-sql=true` en local.
- Añadir índices en V2__add_indexes.sql:
  - `watch_provider(country_code)` para `/api/providers?country=ES`
  - `watch_provider(anime_id, country_code)` para detalle
  - `anime(season, season_year)` para `/api/seasons/{year}/{season}`
  - `anime(popularity DESC)` para listados ordenados
  - `anime_genre(genre)` para `/api/genres/{slug}`
- Test que mide `count(*)` antes/después de un endpoint para detectar N+1.

### PR 6.3 — Actuator + Prometheus
- `spring-boot-starter-actuator` + `io.micrometer:micrometer-registry-prometheus`.
- Exponer `/actuator/health`, `/actuator/info`, `/actuator/prometheus`.
- Caddy front: ruta `/actuator/**` SOLO accesible desde la red Docker interna (no expuesta a internet). Editar `Caddyfile`.
- Métricas custom: `dondeanime.scheduler.anilist.duration`, `dondeanime.scheduler.anilist.success.count`, `dondeanime.scheduler.anilist.error.count` (y equivalentes para match + providers).

### PR 6.4 — Logs estructurados JSON
- `logback-spring.xml` con encoder JSON (`net.logstash.logback:logstash-logback-encoder`).
- Solo activo en profile `prod`. En dev sigue formato humano legible.
- Incluir: timestamp, level, logger, message, MDC (trace_id, anime_slug si aplica).
- Filter en `WebMvcConfigurer` que mete `X-Request-Id` (UUID generado si no llega) al MDC.

### PR 6.5 — Telegram bot para alertas críticas
- Servicio `TelegramAlertService` con bot token + chat ID en `.env.prod`.
- Listener `@EventListener` para errores del scheduler: si un job falla, manda mensaje al chat.
- `TELEGRAM_ENABLED=false` por defecto. Si está OFF, el service no se crea (`@ConditionalOnProperty`).
- Test del listener con mock del bot.

---

## Sprint 7 — Frontend performance + PWA

**Objetivo:** Core Web Vitals en verde 100%. Frontend funciona offline para vistas cacheadas. Imágenes optimizadas WebP/AVIF.

**Branch:** `sprint-7`
**PRs esperados:** 5

### PR 7.1 — PWA básica
- `astro-pwa` o `vite-plugin-pwa` integrado.
- `manifest.json` con icons (192, 512, maskable), theme color, display standalone.
- Service worker que cachea: home, `/anime/{slug}`, `/pais/{slug}`, assets críticos. Network-first para API, cache-first para HTML estático.
- Test E2E (Playwright): abre home offline tras primera visita, verifica que renderiza.

### PR 7.2 — Imágenes responsive WebP/AVIF
- Componente `<AnimePicture>` que recibe `coverImage`, genera `<picture>` con sources WebP + AVIF + fallback original.
- Si AniList sirve URL directa, proxiear por Vercel Image Optimization (o `@astrojs/image`).
- `loading="lazy"` + `decoding="async"` por defecto. `eager` solo para LCP image (cover de la primera card visible).
- Audit: LCP < 2.0s en home con red 4G simulada.

### PR 7.3 — Skeleton screens + View Transitions
- Skeleton component reutilizable (`<SkeletonCard>`, `<SkeletonRow>`).
- Astro View Transitions API en navegación home → detalle → país. Suaviza CLS.
- Test visual: capturas con Playwright antes/después para validar.

### PR 7.4 — Bundle optimization
- Auditar bundle con `npm run build && du -sh dist/`.
- Code splitting de search-index.json: lazy load solo cuando el buscador se abre (no en cada página).
- Preload de fuentes Geist críticas. Resto en lazy.
- Tree-shake icons no usados.
- **Objetivo:** página inicial < 100 KB transferidos (HTML + CSS + JS crítico).

### PR 7.5 — Modo claro decente
- Auditar contraste en modo claro (actualmente prioriza dark).
- Paleta light con mismos accents pero fondos cálidos, no blanco puro.
- Test de accesibilidad con axe-core en ambos modos. WCAG AA mínimo, AAA donde se pueda.

---

## Sprint 8 — Expansión de catálogo (100 → 500 anime)

**Objetivo:** subir el catálogo a 500 anime sin romper performance. Añadir datos derivados (studios, characters, trailers) sin tocar la heurística core.

**Branch:** `sprint-8`
**PRs esperados:** 5

### PR 8.1 — Sync 500 anime
- `AnimeSyncService.syncPopular(500)` (ya soporta paginación interna).
- Endpoint `POST /api/anime/sync` acepta `?count=500`.
- Verificar que 500 anime + matching TMDb tarda < 5 minutos.
- Documentar en CLAUDE.md el comando de migración de prod: `curl -X POST .../api/anime/sync?count=500`.
- **Veto:** no subir más de 500 en este sprint. Más adelante se evalúa.

### PR 8.2 — Studios
- Entidad `Studio` (id, name, slug, isAnimationStudio).
- `@ManyToMany` anime ↔ studio (tabla `anime_studio`).
- AniList query: añadir `studios { nodes { id name isAnimationStudio } }`.
- Endpoint `GET /api/studios` y `GET /api/studios/{slug}` (anime del estudio).
- Frontend: páginas `/estudio/{slug}` con SSG.

### PR 8.3 — Trailers desde TMDb
- `TmdbClient.getTrailers(tmdbId, "es")` → endpoint `/tv/{id}/videos?language=es-ES`.
- Filtrar `type=Trailer` + `site=YouTube`.
- Guardar URL del primer trailer en `anime.trailerYoutubeId`.
- Frontend: componente `<TrailerEmbed>` en `[slug].astro` que carga iframe de YouTube con `loading=lazy`. Si no hay trailer, no se renderiza.

### PR 8.4 — Personajes principales
- Entidad `Character` (id, name, image, anilistId).
- `@ManyToMany` anime ↔ character (relación con role: MAIN, SUPPORTING).
- AniList query: `characters(perPage: 6, role: MAIN) { edges { role node { id name image } } }`.
- Solo 6 principales por anime (no inflar BD).
- Frontend: sección "Personajes" en página detalle.

### PR 8.5 — Re-match TMDb con catálogo ampliado
- Script `AnimeMatchingService.rematch(animeSlug)` para re-procesar matchings problemáticos.
- Endpoint `POST /api/admin/anime/{slug}/rematch` (auth basic) que fuerza re-evaluación.
- UI en `/admin/anime/[slug]` con botón "Re-matchear con TMDb".
- **Veto:** la heurística sigue igual. Esto solo permite re-disparar el algoritmo existente sobre un anime concreto.

---

## Sprint 9 — SEO masivo + content layer

**Objetivo:** subir de 720 a 1500+ páginas indexables. Estructurar el sitio para long-tail. Preparar terreno para blog editorial.

**Branch:** `sprint-9`
**PRs esperados:** 6

### PR 9.1 — Páginas "Mejores de [año]"
- Ruta `/mejores/[year].astro` (2010-2026).
- Top 30 por popularidad de cada año con providers actuales.
- Schema.org `ItemList` con 30 items.
- Sitemap incluye estas 17 URLs.

### PR 9.2 — Páginas combinatoria género × plataforma
- Ruta `/anime/[genero]/en/[plataforma].astro`.
- Generar para top 7 géneros × top 5 plataformas = 35 URLs.
- H1 dinámico: "Anime de [género] en [plataforma]".
- Lista de anime que cumplen ambos criterios (filtro por género + tienen ese provider en cualquier país hispano).

### PR 9.3 — Páginas "Estrenos próximos"
- `/estrenos/proxima-semana.astro` y `/estrenos/proximo-mes.astro`.
- Endpoint backend `GET /api/anime/upcoming?days=7` y `?days=30` ordenado por `startDate` ascendente.
- Página estática regenerada por webhook al sincronizar AniList (modificar `CatalogScheduler` para disparar deploy hook tras sync de anilist también, no solo tras providers).

### PR 9.4 — Schema.org expansion
- Añadir `FAQPage` en home con 5 preguntas frecuentes (¿dónde ver anime gratis? ¿qué plataforma tiene más anime? etc.).
- `Review` schema en cada anime con `averageScore` de AniList como rating.
- `Organization` schema en footer con logo, sameAs (RRSS futuras).
- Validador Schema.org en CI: workflow que parsea HTML del build y valida JSON-LD.

### PR 9.5 — Sitemap index
- Partir `/sitemap.xml` en:
  - `/sitemap-anime.xml` (~500 URLs)
  - `/sitemap-paises.xml` (~500 URLs)
  - `/sitemap-plataformas.xml` (~40 URLs)
  - `/sitemap-generos.xml` (~17 URLs)
  - `/sitemap-temporadas.xml` (~60 URLs)
  - `/sitemap-mejores.xml` (~17 URLs)
  - `/sitemap-combinatoria.xml` (~35 URLs)
- `/sitemap.xml` raíz = sitemap index referenciando los anteriores.

### PR 9.6 — Blog editorial (estructura, sin contenido)
- `frontend/src/pages/blog/[slug].astro` con MDX.
- `frontend/src/content/blog/` con 2 artículos placeholder reales que Diego puede editar.
- Schema `BlogPosting` por artículo.
- `/blog/` index con últimos artículos.
- RSS feed en `/blog/rss.xml`.
- **Veto:** Codex NO genera contenido editorial. Solo la infraestructura. Los 2 artículos placeholder son lorem ipsum + estructura.

---

## Sprint 10 — Búsqueda fulltext + autocomplete + filtros

**Objetivo:** búsqueda real con autocomplete instantáneo. Filtros combinados en `/pais/{slug}` y `/plataforma/{slug}`. Soporte tipográfico (búsqueda tolera typos).

**Branch:** `sprint-10`
**PRs esperados:** 5

### PR 10.1 — Postgres tsvector
- Columna `anime.search_vector tsvector` generada (`GENERATED ALWAYS AS (to_tsvector(...))`).
- Migración Flyway V3.
- Índice GIN sobre `search_vector`.
- Repositorio: `findBySearchVectorMatching(String query)` con `@Query(value="... @@ plainto_tsquery(:q)", nativeQuery=true)`.

### PR 10.2 — Endpoint /api/search
- `GET /api/search?q=...&limit=10`.
- Retorna `AnimeSummaryDto[]` ordenados por relevancia (ts_rank) + popularidad como tiebreak.
- Rate limit: 30 req/min por IP (ver PR 10.3 abajo).
- Test: busca "ataque", retorna Attack on Titan.

### PR 10.3 — Bucket4j rate limiting
- `com.bucket4j:bucket4j-core` + `bucket4j-spring-boot-starter`.
- Filter Spring que aplica:
  - `/api/search` → 30 req/min/IP
  - `/api/track/affiliate` → 60 req/min/IP
  - `/api/admin/**` → 10 req/min/IP (anti-bruteforce login)
- 429 con header `Retry-After`.
- Tests con MockMvc disparando 31 requests, esperar 429 en la 31.

### PR 10.4 — Autocomplete frontend
- Componente `<SearchBox>` (Astro + island Svelte/Solid mínima).
- Debounce 200ms.
- Llama `/api/search?q=...&limit=5`.
- Dropdown con resultados, click navega a `/anime/{slug}`.
- Atajo `/` en cualquier página enfoca el buscador.
- Funciona sin JS: si JS está OFF, el form hace submit a `/buscar?q=...` (página estática con search-index.json como ya existe).

### PR 10.5 — Filtros combinados
- En `/pais/[slug]` y `/plataforma/[slug]/[pais]`: filtros por género, año, score mínimo.
- Implementación: query string + filtrado client-side sobre el dataset SSG (no llamar API extra). 
- URLs canónicas: `?genero=action&year=2024`. Tras 2 filtros activos, `<meta name="robots" content="noindex">` para evitar canibalización.
- Test E2E Playwright: aplica filtro, verifica que la lista se reduce.

---

## Sprint 11 — Hardening + DevOps

**Objetivo:** VPS hardenizado. Monitoreo externo. Backups verificados (no solo crear, también validar que se pueden restaurar). Disaster recovery documentado y probado.

**Branch:** `sprint-11`
**PRs esperados:** 5

### PR 11.1 — Fail2ban + SSH hardening
- Script `setup-hardening.sh` en `scripts/vps/` que instala y configura fail2ban (jail para SSH con maxretry=3, bantime=1h).
- Configurar `/etc/ssh/sshd_config`: `PasswordAuthentication no`, `PermitRootLogin no`, `MaxAuthTries 3`.
- `unattended-upgrades` activado para parches de seguridad automáticos.
- Documentar en DEPLOY.md cómo aplicar y verificar.
- **Veto:** NO ejecutar el script en prod desde el PR. Solo lo deja preparado. Diego lo corre manualmente cuando lo revise.

### PR 11.2 — Monitoreo externo
- Integrar UptimeRobot o BetterUptime (free tier).
- 3 monitors: `https://dondeanime.com`, `https://api.dondeanime.com/api/anime`, `https://api.dondeanime.com/actuator/health`.
- Check cada 5 min. Alerta a Telegram (reutilizar bot del Sprint 6) si cae > 1 check.
- Documentar en DEPLOY.md cómo añadir monitors.

### PR 11.3 — Backup verification
- Script `scripts/vps/verify-backup.sh` que:
  - Baja el backup más reciente de R2.
  - Lo restaura en un Postgres temporal en Docker (puerto 5444, throwaway).
  - Cuenta filas en `anime`, `watch_provider`, `affiliate_link`.
  - Compara con el Postgres prod (en proporción razonable; debe haber al menos 95% de las filas de prod).
  - Si todo OK, exit 0. Si no, alerta a Telegram.
- Cron semanal (domingos a las 4:00 UTC).
- **Veto:** este script NUNCA toca el Postgres prod. Solo lee.

### PR 11.4 — Disaster recovery doc
- `DISASTER-RECOVERY.md` con procedimientos:
  - VPS muerto: cómo provisionar Hetzner nuevo, copiar SSH keys, clonar repo, restaurar `.env.prod` desde un export cifrado, restaurar último backup de R2.
  - BD corrupta: cómo bajar último backup verificado y restaurar.
  - Cloudflare caído: plan B (sin CDN, DNS directo a IP).
  - Vercel caído: plan B (servir frontend desde el VPS con Caddy).
- Drill: el doc lo prueba Diego una vez al trimestre en VPS de pruebas.

### PR 11.5 — Secrets rotation playbook
- Doc `SECRETS-ROTATION.md` con:
  - Rotar `ADMIN_PASSWORD` cada 90 días (comando + restart backend).
  - Rotar `JWT_SECRET` (anula sesiones admin existentes, plan: notificar y rotar).
  - Rotar `RESEND_API_KEY` si se filtra.
  - Rotar `R2_SECRET_ACCESS_KEY` (crear nuevo token, sustituir, borrar viejo).
- Script `scripts/vps/rotate-secret.sh` que recibe `--secret ADMIN_PASSWORD` y actualiza interactivamente.

---

## Sprint 12 — Admin V2 + monetización avanzada

**Objetivo:** panel admin profesional con JWT. Métricas más profundas. Gestión bulk de afiliados. Newsletter setup.

**Branch:** `sprint-12`
**PRs esperados:** 5

### PR 12.1 — JWT auth para admin
- Reemplazar HTTP Basic en `/api/admin/**` por JWT.
- Endpoint `POST /api/admin/login` que valida usuario/password y devuelve JWT (firmado con `JWT_SECRET` ya existente).
- Frontend `/admin/login` con formulario, guarda token en `localStorage`. Interceptor que añade `Authorization: Bearer ...` a fetches.
- Logout = borrar localStorage.
- Token TTL: 8 horas. Refresh: no por simplicidad, vuelve a loguear.

### PR 12.2 — 2FA TOTP opcional
- Columna `admin_user.totp_secret` (nullable).
- Endpoint `POST /api/admin/2fa/setup` genera secret + devuelve QR code data.
- Endpoint `POST /api/admin/2fa/verify` valida código de Google Authenticator.
- Login flow: si `totp_secret IS NOT NULL`, requiere código TOTP además de password.
- UI en admin para activar/desactivar.

### PR 12.3 — Bulk import afiliados
- Endpoint `POST /api/admin/affiliate-links/bulk` que acepta CSV.
- Formato CSV: `provider_slug,country_code,url,active`.
- Validación: provider+country debe existir, URL bien formada.
- UI: drag-drop CSV con preview antes de confirmar.
- Útil para cuando Diego apruebe 10 programas afiliados a la vez.

### PR 12.4 — Dashboard métricas extendido
- Añadir al `/api/admin/dashboard`:
  - Clicks por día últimos 30 días (timeseries para graficar).
  - Conversión por plataforma (clicks / vistas de detalle).
  - Top 10 países por clicks.
  - Anime con más cambios de availability en últimos 30 días.
- Frontend: charts con `chart.js` lazy-loaded.

### PR 12.5 — Newsletter signup
- Endpoint `POST /api/newsletter/subscribe` con doble opt-in (reutilizar el flow de JWT del Sprint 2 de alertas).
- Diferencia con alertas: el newsletter es global (todos los suscriptores reciben los mismos mails), las alertas son por-anime.
- Tabla `newsletter_subscriber(id, email, confirmed_at, unsubscribed_at)`.
- Componente Astro `<NewsletterForm>` para incluir en home y blog.
- Veto: NO mandar newsletter automática. Solo el infra. Diego mandará el primero a mano cuando tenga algo que decir.

---

## Notas finales para Codex

### Cómo arrancar cada sprint
```bash
git checkout main
git pull origin main
git checkout -b sprint-N
# trabajar PR 1
git checkout -b sprint-N-pr1
# commits, push, PR a sprint-N
```

### Antes de cada PR
- `./mvnw test` verde local.
- `npm run build` verde local.
- Cambios en `.env.prod.example` reflejados.
- Cambios en CLAUDE.md (sección "Estado actual" y "Decisiones tomadas") si aplica.
- Commit format: `Sprint N: <descripción imperativa breve>`.

### Cuándo escalar a Diego
- Si una decisión rompe alguna convención de CODEX.md original → PR en draft + mención al issue.
- Si un sprint requiere coste mensual nuevo (Cloudflare paid, BetterUptime paid, etc.) → preguntar antes de implementar.
- Si Codex detecta un bug real en código existente fuera del scope → abrir issue, no fixearlo en el PR del sprint.

### Sprints futuros (esbozados, no detallados aquí)
- Sprint 13: i18n preparación (sin activarlo, solo extraer strings a JSON para futuras versiones EN/PT).
- Sprint 14: WebSockets para "alerts en tiempo real" cuando una plataforma añade un anime.
- Sprint 15: Versión Premium (Stripe Checkout, ad-free, alerts ilimitadas).
- Sprint 16: API pública con rate limiting + docs OpenAPI para terceros.

Estos se detallarán en `CODEX-BACKLOG-V3.md` cuando Sprint 12 esté cerrado.

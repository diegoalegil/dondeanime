# DondeAnime

> Archivo de contexto para Claude Code. Se carga automáticamente al ejecutar `claude` dentro de este repo. Mantener actualizado conforme avanza el proyecto.

---

## Qué es este proyecto

Web pública en español para descubrir **cuándo y dónde se estrena cada anime** en plataformas hispanoamericanas (Crunchyroll, Netflix, Prime Video, HBO Max, etc.). Estrategia: aggregator + SEO long-tail con miles de páginas indexables. Monetización vía afiliados (Crunchyroll, Amazon), AdSense y futuro Premium.

- URL final: https://dondeanime.com
- Repo: https://github.com/diegoalegil/dondeanime (privado)
- Estado: **EN PRODUCCIÓN**. Frontend en `https://dondeanime.com` (Vercel), backend en `https://api.dondeanime.com` (Hetzner VPS CX22 con Docker + Caddy + Let's Encrypt). Scheduler activo: AniList cada 12h, TMDb match cada 24h, providers cada 24h, webhook auto-rebuild de Vercel tras cada sync de providers. A partir de aquí el trabajo lo lleva Codex en sprints (ver `CODEX.md`).

---

## Sobre Diego (el usuario)

Estudiante de 1º DAM en España. Terminando curso de mayo 2026, va a programar todo el verano. Prioridad académica: dominar Java + bases de datos + redes/sistemas + tener proyectos reales que demuestren nivel.

### Estilo de comunicación esperado
- **Directo, sin rodeos, anti-IA.** Nada de "es fundamental que", "hoy en día", "sin duda alguna", "en este sentido", "cabe destacar que".
- Frases cortas, lenguaje simple pero preciso.
- Paso a paso con código real, no pseudocódigo.
- Diego quiere **entender el porqué**, no solo copiar.
- Si Diego se equivoca o una decisión es mala, **decírselo y proponer alternativa**. No darle la razón gratis.
- Sin emojis salvo que él los use primero.
- Humor inteligente cuando encaje, tono motivador. Empatía sin pasarse a melodrama.

### Lo que NO quiere
- Explicaciones largas antes del meollo.
- Listas perfectas tipo IA sin valor real.
- Conclusiones que repiten lo dicho.
- Preguntas retóricas para rellenar.
- Asumir que ya sabe cosas que no se han explicado antes.

---

## Stack técnico

| Capa | Tecnología | Versión |
|---|---|---|
| Backend | Spring Boot | **4.0.6** |
| Lenguaje | Java | **21** |
| Build | Maven | wrapper incluido en repo (`./mvnw`) |
| BD | PostgreSQL | 16-alpine en Docker |
| ORM | Hibernate | 7.2.12 (Spring Data JPA) |
| Frontend | Astro | **6** + Tailwind 4 |
| Deploy backend | Hetzner Cloud VPS CX22 | producción |
| Deploy frontend | Vercel free tier | producción |
| CDN/DNS | Cloudflare free | producción |

### Dependencias Spring Boot actuales
Web, Data JPA, PostgreSQL Driver, Validation, Lombok, DevTools.

### APIs externas
- **AniList** (GraphQL, sin auth, https://graphql.anilist.co) → fuente primaria de datos de anime.
- **TMDb** (REST, requiere API key v4, https://api.themoviedb.org/3) → providers de streaming por país.

---

## Estructura del repo

```
DondeAnime/
├── CLAUDE.md                    # este archivo
├── README.md
├── docker-compose.yml           # Postgres 16 mapeado a host:5433
├── .env                         # NO commitear (TMDB_API_KEY real)
├── .env.example
├── .gitignore
├── docs/
│   ├── arquitectura.md          # diagramas, modelo BD, decisiones
│   ├── apis.md                  # docs AniList + TMDb con ejemplos curl
│   └── roadmap.md               # plan 12 semanas, entregables semanales
├── scripts/
│   ├── test-apis.js             # validación end-to-end AniList + TMDb
│   ├── package.json
│   └── package-lock.json
└── backend/                     # Spring Boot
    ├── pom.xml
    ├── mvnw, mvnw.cmd, .mvn/
    └── src/
        ├── main/java/com/dondeanime/backend/
        │   ├── BackendApplication.java        # @EnableScheduling
        │   ├── config/
        │   │   ├── HttpClientConfig.java
        │   │   └── SecurityConfig.java        # HTTP Basic + CORS para /api/**
        │   ├── admin/
        │   │   ├── AnimeAdminController.java  # /api/admin/anime/{slug}/override(s)
        │   │   ├── AnimeOverrideRequest.java
        │   │   └── AnimeOverrideDto.java
        │   ├── scheduling/
        │   │   └── CatalogScheduler.java      # 3 jobs @Scheduled (anilist, match, providers)
        │   ├── sitemap/
        │   │   ├── SitemapController.java     # GET /api/sitemap
        │   │   └── SitemapDto.java
        │   ├── provider/                      # watch providers (TMDb)
        │   │   ├── WatchProvider.java         # entidad JPA
        │   │   ├── WatchProviderRepository.java
        │   │   ├── ProviderSyncService.java
        │   │   ├── ProviderController.java    # GET /api/providers, /providers/{slug}/{country}
        │   │   ├── ProviderDto.java           # DTO público
        │   │   └── ProviderSummaryDto.java    # DTO agregado con count
        │   └── anime/
        │       ├── Anime.java                 # entidad JPA (22 campos: +genres, season, seasonYear)
        │       ├── AnimeController.java       # GET, GET /{slug}, POST /sync, /match, /sync-providers
        │       ├── AnimeRepository.java       # + findByProviderSlugAndCountry, findByGenreSlug, etc.
        │       ├── AnimeSyncService.java
        │       ├── AnimeMatchingService.java
        │       ├── AnimeSummaryDto.java       # vista de listados (sin id/tmdbId/syncedAt)
        │       ├── AnimeDetailDto.java        # vista de detalle (sin id/tmdbId/syncedAt)
        │       ├── AnimeDetailResponse.java   # AnimeDetailDto + Map<country, List<ProviderDto>>
        │       ├── AnimeOverride.java         # overrides editoriales por campo y locale
        │       ├── AnimeOverrideRepository.java
        │       ├── AnimeOverrideService.java
        │       ├── GenreController.java       # GET /api/genres, /genres/{slug}
        │       ├── GenreSummaryDto.java
        │       ├── SeasonController.java      # GET /api/seasons, /seasons/{year}/{season}
        │       ├── SeasonSummaryDto.java
        │       ├── anilist/                   # cliente + DTOs de AniList
        │       │   ├── AniListClient.java
        │       │   ├── AniListResponse.java
        │       │   ├── AniListData.java
        │       │   ├── AniListPage.java
        │       │   ├── AniListMedia.java
        │       │   ├── AniListTitle.java
        │       │   ├── AniListFuzzyDate.java
        │       │   └── AniListCoverImage.java
        │       └── tmdb/                      # cliente + DTOs de TMDb
        │           ├── TmdbClient.java
        │           ├── TmdbSearchResponse.java
        │           ├── TmdbSearchResult.java
        │           ├── TmdbProvidersResponse.java
        │           ├── TmdbCountryProviders.java
        │           └── TmdbProvider.java
        ├── main/resources/
        │   └── application.properties
        └── test/java/com/dondeanime/backend/
            └── BackendApplicationTests.java
```

---

## Configuración local (crítica)

### Postgres en puerto 5433 (NO 5432)

Diego tiene **PostgreSQL 17 nativo** instalado vía Homebrew (`postgresql@17`) corriendo como LaunchAgent en `localhost:5432`. Para no colisionar, el Postgres del Docker mapea **host:5433 → contenedor:5432**.

```
host:5432  →  Postgres 17 nativo de Diego (NO TOCAR, otras prácticas)
host:5433  →  Postgres 16 del Docker (el que usa este proyecto)
```

Por eso `application.properties` apunta a `jdbc:postgresql://localhost:5433/dondeanime`.

### Variables de entorno (`.env`, NO commiteado)

```
TMDB_API_KEY=eyJ...                    # token v4 de TMDb (regenerado tras leak inicial)
ANILIST_API_BASE=https://graphql.anilist.co
POSTGRES_HOST=localhost
POSTGRES_PORT=5433                     # ← 5433, no 5432
POSTGRES_DB=dondeanime
POSTGRES_USER=dondeanime_user
POSTGRES_PASSWORD=cambiar_en_local
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin
ADMIN_CORS_ALLOWED_ORIGINS=http://localhost:4321,http://127.0.0.1:4321,https://dondeanime.com,https://www.dondeanime.com
PUBLIC_API_URL=http://localhost:8080
PUBLIC_SITE_URL=https://dondeanime.com
CATALOG_REFRESH_HOURS=12
```

### application.properties (resumen)

```properties
spring.application.name=dondeanime-backend
server.port=8080

admin.username=${ADMIN_USERNAME:admin}
admin.password=${ADMIN_PASSWORD:admin}
admin.cors.allowed-origins=${ADMIN_CORS_ALLOWED_ORIGINS:http://localhost:4321,http://127.0.0.1:4321,https://dondeanime.com,https://www.dondeanime.com}

spring.datasource.url=jdbc:postgresql://localhost:5433/dondeanime
spring.datasource.username=dondeanime_user
spring.datasource.password=cambiar_en_local
spring.datasource.driver-class-name=org.postgresql.Driver

spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.initialization-fail-timeout=-1

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

logging.level.org.hibernate.orm.deprecation=ERROR
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.springframework.web=INFO
```

---

## Comandos cotidianos

### Arrancar todo (sesión nueva)
```bash
cd ~/Desktop/Repos-Github/DondeAnime
docker compose up -d              # levanta Postgres
cd backend
./mvnw spring-boot:run            # arranca el backend (puerto 8080)
# en otra terminal:
cd ~/Desktop/Repos-Github/DondeAnime/frontend
npm run dev                       # arranca Astro dev (puerto 4321)
```

### Bajar todo
```bash
# Ctrl+C en la terminal del backend
docker compose down               # opcional; los datos persisten en volumen
```

### Validar APIs externas
```bash
cd scripts
node test-apis.js
```

### Tests
```bash
cd backend
./mvnw test
```

### Conectarse a Postgres del Docker
```bash
docker exec -it dondeanime_postgres psql -U dondeanime_user -d dondeanime
```

Útiles dentro de psql: `\dt` (listar tablas), `\d nombre_tabla` (ver columnas), `\du` (listar roles), `\q` (salir).

### Reset total de la BD (cuidado, borra datos)
```bash
cd ~/Desktop/Repos-Github/DondeAnime
docker compose down -v            # -v BORRA el volumen
docker compose up -d
```

---

## Producción (URLs y comandos)

### URLs vivas
- **Frontend**: https://dondeanime.com (apex 307 → www.dondeanime.com, Astro estático en Vercel)
- **Backend API**: https://api.dondeanime.com (Spring Boot en Hetzner VPS, Caddy reverse proxy, Let's Encrypt)
- **Dashboards**:
  - Cloudflare DNS: https://dash.cloudflare.com → `dondeanime.com`
  - Vercel: https://vercel.com/diegoalegil/dondeanime
  - Hetzner: https://console.hetzner.cloud → proyecto `dondeanime` → server `dondeanime-prod` (IP `46.224.162.174`)

### Acceso al VPS
```bash
# SSH como deploy (sin password, con SSH key en ~/.ssh/id_ed25519)
ssh deploy@46.224.162.174

# Directorio del proyecto en el VPS
cd /opt/dondeanime
```

### Operación habitual en el VPS
```bash
# Logs en vivo
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f caddy

# Estado de contenedores
docker compose -f docker-compose.prod.yml ps

# Reiniciar backend (tras cambio de config en .env.prod)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d backend

# Deploy de nueva versión del código
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod build backend
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d backend

# Disparar sync manual (también lo hace el scheduler en cron)
curl -X POST https://api.dondeanime.com/api/anime/sync
curl -X POST https://api.dondeanime.com/api/anime/match
curl -X POST https://api.dondeanime.com/api/anime/sync-providers
```

### Variables de entorno producción
Están en `/opt/dondeanime/.env.prod` (NO en repo). Plantilla en `.env.prod.example`.
Claves: `POSTGRES_PASSWORD` (autogenerada), `TMDB_API_KEY` (la misma que en .env local), `VERCEL_DEPLOY_HOOK` (URL del Deploy Hook configurado en Vercel), `SCHEDULING_ENABLED=true`, `ADMIN_USERNAME=admin`, `ADMIN_PASSWORD` fuerte.

### Más detalle operativo
Ver `DEPLOY.md` en la raíz del repo: troubleshooting, deploy desde cero a un VPS nuevo, backups manuales.

---

## Estado actual del proyecto

- [x] APIs externas validadas (AniList + TMDb devuelven datos reales)
- [x] Dominio dondeanime.com comprado en Namecheap
- [x] Repo GitHub privado creado, push inicial con docs
- [x] Brief, arquitectura, APIs y roadmap documentados
- [x] PostgreSQL 16 corriendo en Docker (puerto 5433 del host)
- [x] Spring Boot 4 arranca, conecta a BD, responde 404 en localhost:8080
- [x] Endpoint `GET /api/anime` devuelve `[]` (semana 1 cerrada, día 3)
- [x] Entidad `Anime` ampliada a 19 campos (description, format, status, episodes, fechas year/month/day, coverImage, bannerImage, averageScore, popularity, syncedAt)
- [x] DTOs de AniList: 7 records públicos en `anime/anilist/` (uno por archivo)
- [x] `AniListClient` con `RestClient` apuntando a `https://graphql.anilist.co`, **pagina internamente** (AniList limita perPage a 50)
- [x] `HttpClientConfig` con `@Bean RestClient.Builder` reutilizable
- [x] `AnimeSyncService` con mapeo DTO→entidad, upsert por anilistId, slug normalizado con dedupe
- [x] Endpoint `POST /api/anime/sync?count=N` que dispara el sync manual
- [x] Probado end-to-end: sync de 100 anime en ~1.5s, GET devuelve 100, datos completos en Postgres (semana 2 cerrada, día 5)
- [x] Spring Boot lee `.env` vía `spring.config.import=optional:file:./.env[.properties],optional:file:../.env[.properties]`
- [x] Entidad `Anime` con campo `tmdbId` (nullable, se rellena con el matching)
- [x] Entidad `WatchProvider` (anime_id, country_code, provider_name, provider_type, logo_url, updated_at) con unique en (anime, country, provider)
- [x] DTOs de TMDb: 5 records públicos en `anime/tmdb/` (con `@JsonNaming(SnakeCaseStrategy)` Jackson 3.x)
- [x] `TmdbClient` con auth Bearer, dos endpoints (`/search/tv`, `/tv/{id}/watch/providers`)
- [x] `AnimeMatchingService` con heurística JP + año (±1) + popularidad descendente, en 3 pasadas
- [x] `ProviderSyncService` con delete+insert por anime via `TransactionTemplate`
- [x] Endpoints `POST /api/anime/match`, `POST /api/anime/sync-providers`, `GET /api/anime/{slug}`
- [x] Probado end-to-end: 84 matches de 100, 949 providers en BD, Attack on Titan en España devuelve Crunchyroll + Netflix + Prime Video (semana 3 cerrada, día 6)
- [x] Scheduler `@Scheduled` con 3 jobs (sync AniList 12h, match 24h, providers 24h), toggle `scheduling.enabled` para activar/desactivar en local vs prod (semana 4 cerrada, día 7)
- [x] Entidad `Anime` ampliada con `genres` (@ElementCollection → tabla anime_genre), `season` y `seasonYear`. Re-sync rellenó los 100 anime.
- [x] DTOs públicos `AnimeSummaryDto`, `AnimeDetailDto`, `ProviderDto` que esconden id interno, syncedAt, tmdbId, updatedAt, etc.
- [x] Endpoints frontend: `/api/providers`, `/api/providers/{slug}/{country}`, `/api/genres`, `/api/genres/{slug}`, `/api/seasons`, `/api/seasons/{year}/{season}`, `/api/sitemap`
- [x] Tests básicos: 31 verdes (SlugifyTest, AnimeMatchingServiceTest, AnimeControllerTest, AnimeOverrideRepositoryTest, AnimeDetailDtoTest, AnimeAdminControllerTest, AffiliateLinkServiceTest, AffiliateLinkAdminControllerTest)
- [x] **Frontend Astro 6 + Tailwind 4 cerrado (semana 5):** 720 páginas estáticas (100 fichas + 500 país + 5 país-hub + 8 plataforma-hub + 31 plataforma-país + 17 género + 58 temporada + home). Build en 3.4s. Paleta dark modern con gradiente morado→rosa. Geist auto-hospedada. SEO técnico completo (TVSeries/BreadcrumbList/WebSite+SearchAction/ItemList, hreflang regional, sitemap, robots, OG/Twitter). Tema oscuro/claro persistente. Buscador in-memory con search-index.json.
- [x] **Deploy producción (mes 2):** VPS Hetzner CX22 con Docker (Postgres + backend + Caddy reverse proxy), Vercel para frontend Astro estático, Cloudflare gestionando DNS de `dondeanime.com` y `api.dondeanime.com`. Cert Let's Encrypt automático en ambos. Bug `delete+insert duplicate key` en `ProviderSyncService` arreglado con `@Modifying @Query` JPQL. Webhook backend → Vercel disparado al final de `syncProviders` para auto-rebuild.
- [x] **Sprint 1 mergeado:** tabla `anime_override`, overrides editoriales en `AnimeDetailDto`, HTTP Basic para `/api/admin/**`, CORS cerrado para frontend, endpoints admin POST/DELETE/GET y panel Astro `/admin` + `/admin/anime/[slug]`.
- [x] **Sprint 3 mergeado:** tabla `affiliate_link`, tracking público de clicks, eventos para métricas 7/30 días, dashboard admin, links externos con `affiliateUrl`, Plausible preparado, AdSlot condicional y disclosure legal.
- [x] **CI básico (Sprint 4):** GitHub Actions para `./mvnw test` con Postgres 16 de servicio y `npm run build` del frontend con variables públicas de producción.
- [x] **Sprint 9 mergeado (25 mayo 2026):** SEO masivo. PRs 9.1-9.6: rankings anuales (`/mejores/[year]`), combinatoria género×plataforma (`/anime/[gen]/en/[plat]`), estrenos próximos, JSON-LD expandido (FAQPage, Review, Organization, ItemList), sitemap index particionado (7 sitemaps temáticos), blog editorial estructura (MDX + RSS, sin contenido).
- [x] **Hotfix HF mergeado:** temporada actual dinámica (HF-1) + descripciones es-ES vía TMDb con fallback 3 capas (HF-2). HF-3 (frontend redesign) sigue pendiente de aprobación humana, solo está la auditoría en `frontend/REDESIGN-NOTES.md`.
- [ ] **Sprints 5, 6, 7, 8 con PRs apilados:** todos los PRs individuales (5+5+5+5 = 20) están abiertos esperando merge a sus respectivas ramas de sprint. Diego no ha podido revisar uno a uno. Codex tiene autorización a partir de la siguiente sesión para auto-mergear con squash si CI está verde, y abrir el PR final `sprint-N → main` esperando review humano.
- [ ] **Sprint 10 en curso:** PR 10.1 (Postgres tsvector) mergeado a `sprint-10`. PR 10.2 (search endpoint) abierto. Faltan 10.3 (rate limiting), 10.4 (autocomplete), 10.5 (filtros combinados).
- [ ] **Sprints 11, 12 pendientes** (ver `CODEX-BACKLOG-V2.md`).
- [ ] **Sprints 13-20 detallados en `CODEX-BACKLOG-V3.md`** para sesiones largas de Codex.

---

## Próxima tarea concreta

**El trabajo grande lo lleva Codex en 3 sprints (~6 semanas).** Ver `CODEX.md` en la raíz del repo para el backlog detallado, convenciones inviolables y decisiones intocables.

Resumen sprints:
- **Sprint 1**: enriquecimiento manual top 50 (overrides editorial) + panel admin con auth básica.
- **Sprint 2**: sistema de alertas email (Resend, doble opt-in, GDPR mínimo).
- **Sprint 3**: monetización (links de afiliado curados) + Plausible Analytics + slot AdSense preparado.

Mientras tanto, mejora continua paralela: tests E2E con Playwright, Cloudflare Email Routing, backups BD automáticos a R2, page rules de cache, CI/CD básico.

### Cómo coordina Diego con Codex (y con Claude)

- **Codex** ejecuta sprints siguiendo `CODEX.md`. Hace PRs, no merge directo a main.
- **Diego** revisa código de Codex antes de mergear. Aprueba decisiones técnicas que se salgan del backlog.
- **Claude** (esta sesión o nuevas) interviene a petición de Diego para verificar bloques de Codex (`"verifica el bloque X"`) o resolver dudas estratégicas que excedan la autonomía de Codex.

### Lo que sigue necesitando decisión humana

1. **Revisar los 16 anime sin match TMDb** y los matches del top 50 manualmente. Es contenido editorial, no auto-generable. Codex puede preparar el panel admin pero las decisiones de qué texto y qué afiliados son de Diego.
2. **Configurar cuenta Resend** (sprint 2) o equivalente para email.
3. **Rellenar enlaces afiliados reales** en `/admin/affiliate-links` tras aprobar programas de cada plataforma/país.
4. **Activar Plausible** si Diego decide usar trial/plan de pago y quiere top páginas en dashboard.
5. **Aprobar configuración AdSense** cuando haya 3+ meses de tráfico. El slot queda preparado pero apagado.

### Endpoints listos para el frontend

| Método | Path | Para |
|---|---|---|
| GET | `/api/anime` | Home / catálogo global |
| GET | `/api/anime/{slug}` | Página de detalle de cada anime |
| GET | `/api/providers` | Listado global de plataformas |
| GET | `/api/providers?country=ES` | Plataformas filtradas por país |
| GET | `/api/providers/{slug}/{country}` | "Crunchyroll en España" → lista de anime |
| GET | `/api/genres` | Listado de géneros con count |
| GET | `/api/genres/{slug}` | "Anime de acción" → lista |
| GET | `/api/seasons` | Listado de temporadas con count |
| GET | `/api/seasons/{year}/{season}` | "Estrenos primavera 2024" |
| GET | `/api/sitemap` | Una sola request: todos los ids/slugs para generar sitemap.xml |
| POST | `/api/admin/anime/{slug}/override` | Guardar override editorial de un campo (Basic Auth) |
| DELETE | `/api/admin/anime/{slug}/override?field=description&locale=es` | Resetear override de un campo (Basic Auth) |
| GET | `/api/admin/anime/{slug}/overrides` | Listar overrides activos de una ficha (Basic Auth) |
| GET | `/api/admin/affiliate-links` | Listar links afiliados (Basic Auth) |
| POST | `/api/admin/affiliate-links` | Crear/actualizar link afiliado por provider+país (Basic Auth) |
| DELETE | `/api/admin/affiliate-links/{id}` | Borrar link afiliado (Basic Auth) |
| GET | `/api/admin/dashboard` | Métricas de clicks y Plausible (Basic Auth) |
| POST | `/api/track/affiliate` | Incrementar click afiliado y registrar evento |

---

## Decisiones tomadas (no cambiar sin justificar)

### Stack
- **Spring Boot 4** porque start.spring.io dio esa versión como estable por defecto en mayo 2026.
- **PostgreSQL 16** (no 17) porque la imagen alpine es ligera y madura; el VPS de producción usará la misma.
- **Astro 6** (no Next.js, no React puro) porque genera HTML estático puro → Google indexa al 100% sin esperar JS. La batalla SEO se gana o se pierde aquí.
- **Hetzner CX22** elegido por relación precio/recursos en EU. Datacenter europeo = baja latencia para España y LatAm vía Cloudflare.

### Configuración Hibernate
- **Dialect explícito** (`org.hibernate.dialect.PostgreSQLDialect`) aunque Hibernate suelte WARN HHH90000025 diciendo que no hace falta. SIN él, la app explota si Hikari no consigue leer metadata al primer intento. El WARN se silencia con `logging.level.org.hibernate.orm.deprecation=ERROR`.
- **`initialization-fail-timeout=-1`** para que el backend no muera si Postgres tarda en estar `healthy`.
- **`open-in-view=false`** por buena práctica (evita sesiones JPA abiertas durante render).

### Stack HTTP
- **`RestClient`** (no `WebClient` ni `RestTemplate`) porque es el cliente síncrono recomendado desde Spring 6.1. API fluent, suficiente para llamadas REST/GraphQL no-reactivas.
- **Bean `RestClient.Builder` centralizado** en `config/HttpClientConfig.java`. Spring Boot 4 con `starter-webmvc` **no lo autoconfigura** (en 3.x con `starter-web` sí). Lo declaramos a mano para que `AniListClient`, futuro `TmdbClient`, etc. lo inyecten y especialicen con su propia `baseUrl`. Beneficio: cuando queramos timeouts globales o interceptors de logging, se añaden en un solo sitio.
- **AniList `perPage` máx = 50**: la API cappa silenciosamente. Si pides más, devuelve 50 sin error. Por eso `AniListClient.fetchPopular(N)` pagina internamente en bloques de 50 hasta acumular `N`.

### Sync de AniList
- **Sin `@Transactional`** en `AnimeSyncService.syncPopular`: cada `save` es su propia tx auto-commit. Un anime que falle no arrastra al resto.
- **Slug**: normalizado a partir del título inglés (fallback romaji, fallback `"anime-{id}"`). Quita acentos, lowercase, espacios→guiones, solo `[a-z0-9-]`. Si colisiona con otro anime distinto, sufija `-{anilistId}`. Determinista entre runs gracias al orden POPULARITY_DESC: el más popular se queda el slug bonito.
- **Upsert por `anilistId`**: `findByAnilistId` + `orElseGet(Anime::new)` decide insert vs update. La unique constraint en BD es la red de seguridad.

### Integración TMDb
- **Jackson 3.x en Spring Boot 4**: los paquetes de databind y annotations se separaron. `@JsonProperty` sigue en `com.fasterxml.jackson.annotation`, pero `@JsonNaming` y `PropertyNamingStrategies` ahora viven en **`tools.jackson.databind.*`** (no `com.fasterxml.jackson.databind.*`). Romper este import es el primer tropiezo al añadir cualquier DTO.
- **`tmdb.api-key` vía `.env`**: `spring.config.import=optional:file:./.env[.properties],optional:file:../.env[.properties]` carga el .env como properties (Spring Boot 2.4+). El sufijo `[.properties]` es obligatorio. Sin default en `${TMDB_API_KEY}` para que el arranque falle si no está, en vez de petar luego al primer request.
- **Auth TMDb**: token v4 (JWT que empieza con `eyJ...`) en header `Authorization: Bearer ...`. Se inyecta como `defaultHeader` del `RestClient` en el constructor del `TmdbClient`, así no hay que repetirlo en cada llamada.
- **Heurística de matching en 3 pasadas**:
  1. Origen JP + año de TMDb dentro de ±1 año del `startYear` de AniList.
  2. Origen JP cualquier año.
  3. Cualquier resultado.
  Dentro de cada pasada, el más popular. El año es clave porque la popularity de TMDb es muy volátil (spin-offs recién estrenados ganan a la serie original por boost de novedad).
- **Rate limit TMDb**: 40 req/10s. `Thread.sleep(300)` entre cada llamada deja margen y nos mantiene en ~33 req/10s.
- **`TransactionTemplate` en vez de `@Transactional`** en `ProviderSyncService.syncOne`: la auto-invocación desde la misma clase NO pasa por el proxy de Spring (Spring usa CGLIB o JDK proxies, ambos solo interceptan llamadas externas). `TransactionTemplate` es programático, funciona siempre.
- **Estrategia "delete + insert" para providers**: por cada anime, borramos todos sus WatchProvider y reinsertamos los actuales. Más simple que hacer upsert por composite key. Para 100 anime × ~5 providers el coste es trivial.
- **Solo FLATRATE y FREE**: ignoramos RENT y BUY porque el objetivo es "dónde verlo incluido en suscripción".

### Modelo extendido (semana 4)
- **`genres` como `@ElementCollection<String>`**: tabla aparte `anime_genre` con PK compuesto `(anime_id, genre)`. EAGER para que Jackson lo serialice fuera de sesión sin petar. Sin entidad Genre dedicada porque no aporta valor (no hay metadata por género, solo el nombre).
- **`season` como String, no enum**: AniList puede meter valores nuevos. Mismo razonamiento que con `format` y `status`.
- **`seasonYear` separado de `startYear`**: AniList los distingue (un anime puede anunciarse para una temporada y estrenar después). Guardamos los dos.

### Scheduler (semana 4)
- **`@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true")`** sobre el bean entero. Si la property no está o es `false`, el bean ni se crea: en local nunca dispara syncs accidentales. En producción se activa con `scheduling.enabled=true`.
- **Cron expressions de Spring**: 6 campos (segundo, minuto, hora, día, mes, día semana). Distinto de cron Unix de 5. Defaults espaciados: AniList 3am/3pm, match 4am, providers 5am, para no solapar.
- **Cron override vía properties**: `${dondeanime.cron.sync-anilist:default}` permite cambiar el cron en `.env` sin recompilar.
- **Try/catch dentro de cada job**: un error en uno NO impide que el siguiente cron del mismo job se ejecute más tarde, ni afecta a los otros jobs.

### Monetización y analítica (sprint 3)
- **`ProviderDto.affiliateUrl` es opcional**: si hay link activo para `(provider_slug, country_code)`, el frontend lo usa; si no, enlaza al sitio genérico de la plataforma.
- **Tracking público fire-and-forget**: `POST /api/track/affiliate` responde 204, no devuelve datos y solo incrementa si existe link activo. También guarda `affiliate_click_event` para métricas por fecha/anime.
- **Dashboard admin**: `/api/admin/dashboard` combina clicks propios 7/30 días, top links, top anime por clicks y top páginas de Plausible si `PLAUSIBLE_ENABLED=true`.
- **Plausible**: el script público se activa con `PUBLIC_PLAUSIBLE_ENABLED=true`. La API de stats necesita `PLAUSIBLE_API_KEY`; si falta, esa sección queda vacía sin romper.
- **AdSense**: preparado con `AdSlot`, pero apagado por defecto. No activar hasta tener aprobación y tráfico suficiente.
- **Disclosure afiliados**: footer y `/legal/afiliados` explican la comisión sin coste extra.

### DTOs públicos vs entidades JPA
- **Endpoints REST NUNCA devuelven entidades crudas**. Cada respuesta pasa por un record DTO (`AnimeSummaryDto`, `AnimeDetailDto`, `ProviderDto`, etc.) que filtra los campos internos: `id` interno de BD, `syncedAt`, `tmdbId`, `tmdbProviderId`, `updatedAt`, `animeId`...
- **Factory estático `from(Entity)`** en cada DTO. Mapeo en un solo lugar, fácil de mantener.
- **`AnimeDetailDto.from(Anime, List<AnimeOverride>)` aplica overrides `locale='es'`** solo en detalle: `description`, `title_english`, `title_romaji`. `AnimeSummaryDto` no usa overrides para evitar N queries en builds/listados.
- **Slug provider/genre = lowercase + espacios→guiones** (`ProviderSummaryDto.slugify`, `GenreSummaryDto.slugify`). Convención simple porque los nombres en BD ya vienen limpios (sin chars raros). Si algún día llega un caso raro habrá que reforzar.

### Tests con Spring Boot 4
- **`@WebMvcTest` cambió de paquete**: en SB3 estaba en `org.springframework.boot.test.autoconfigure.web.servlet`, en SB4 está en **`org.springframework.boot.webmvc.test.autoconfigure`**. Las distribuciones de test se modularizaron junto con los starters (`spring-boot-starter-webmvc-test`).
- **`@MockBean` → `@MockitoBean`**: `@MockBean` (de spring-boot-test) está deprecado. Usar `@MockitoBean` (de `org.springframework.test.context.bean.override.mockito`).
- **Mocks puros sin Spring**: para `AnimeMatchingService` y similares, instanciar el service a mano con `mock(TmdbClient.class)` y `mock(AnimeRepository.class)`. Más rápido que arrancar contexto, suficiente para lógica de negocio aislada.

### Endpoints REST disponibles
| Método | Path | Descripción |
|---|---|---|
| GET | `/api/anime` | Lista plana (`AnimeSummaryDto[]`) |
| GET | `/api/anime/{slug}` | Detalle + providers agrupados por país (`AnimeDetailResponse`) |
| POST | `/api/anime/sync?count=N` | Sincroniza N anime desde AniList (default 100) |
| POST | `/api/anime/match` | Asigna `tmdbId` a cada anime sin matchear |
| POST | `/api/anime/sync-providers` | Refresca la tabla `watch_provider` desde TMDb |
| GET | `/api/providers` | Lista global de plataformas con count (`ProviderSummaryDto[]`) |
| GET | `/api/providers?country=ES` | Mismo, filtrado por país |
| GET | `/api/providers/{slug}/{country}` | Anime disponibles en esa plataforma en ese país |
| GET | `/api/genres` | Lista de géneros con count (`GenreSummaryDto[]`) |
| GET | `/api/genres/{slug}` | Anime de un género, ordenados por popularidad |
| GET | `/api/seasons` | Lista de temporadas con count (`SeasonSummaryDto[]`) |
| GET | `/api/seasons/{year}/{season}` | Anime de una temporada (400 si season inválida) |
| GET | `/api/sitemap` | Todos los slugs/ids para que el frontend genere sitemap.xml |
| POST | `/api/admin/anime/{slug}/override` | Crea/actualiza override editorial. Devuelve `AnimeDetailDto` refrescado |
| DELETE | `/api/admin/anime/{slug}/override?field=description&locale=es` | Borra override y vuelve al valor AniList |
| GET | `/api/admin/anime/{slug}/overrides` | Lista overrides activos con valor original |
| GET | `/api/admin/affiliate-links` | Lista links afiliados |
| POST | `/api/admin/affiliate-links` | Crea/actualiza link afiliado |
| DELETE | `/api/admin/affiliate-links/{id}` | Borra link afiliado |
| GET | `/api/admin/dashboard` | Dashboard monetización/analítica |
| POST | `/api/track/affiliate` | Tracking público de click afiliado |

### Modelado de datos
- **Records de Java 21** para DTOs externos (AniList): inmutables, concisos, Jackson los parsea sin config.
- **Un record por archivo**, todos `public`: convención Java, y necesario para usarlos desde paquetes hermanos (el service que está en `anime/` necesita `AniListMedia` que vive en `anime/anilist/`).
- **Fechas como `Integer` separados** (`startYear`, `startMonth`, `startDay`) y NO `LocalDate`, porque AniList puede devolver año sin mes ni día. `LocalDate` exige los tres.
- **Sin `enum` Java** para `format`/`status` (Strings simples). Si AniList añade un valor nuevo, `String` lo tolera; un enum petaría al deserializar.
- **`@Column(columnDefinition = "TEXT")`** para `description` (descripciones largas no caben en `VARCHAR(255)` por defecto).
- **`Instant`** para `syncedAt` (timestamp técnico UTC). Hibernate lo mapea a `timestamp with time zone` en Postgres.
- **Afiliados**: `affiliate_link` guarda un link activo por provider+país y `click_count` acumulado; `affiliate_click_event` guarda eventos con `clickedAt` y `animeSlug` para métricas 7/30 días y ranking por anime.

### Git
- Auth con **HTTPS + Personal Access Token** (SSH pendiente, no urgente, ver tareas).
- Convención de commits: empezar con "Día X:" o "Semana X:" + título breve + cuerpo con detalles.

---

## Cosas que NO hacer

- **Nunca commitear `.env`** (contiene TMDB_API_KEY real). Ya está en `.gitignore`.
- **No tocar Postgres 17 nativo de Diego** (instalado para clase, no es de este proyecto).
- **No reescribir Spring Boot** a otra versión sin justificar y discutir con Diego.
- **No añadir features fuera de roadmap** sin discutir: NO sistema de cuentas usuario, NO app móvil, NO sistema de comentarios, NO idiomas distintos al español, NO soporte manga/series JP/juegos. Todo eso está conscientemente pospuesto.
- **No abusar de Lombok** en entidades al principio: Diego está aprendiendo, mejor que entienda el boilerplate primero.
- **No mostrar la TMDB_API_KEY** ni ninguna credencial en outputs, screenshots, o mensajes.
- **No ejecutar `docker compose down -v`** sin avisar (borra los datos).

---

## Roadmap resumido (90 días)

- **Mes 1 (junio):** Backend funcional con catálogo de 100-200 anime y providers por país. Entregable: API REST estable.
- **Mes 2 (julio):** Frontend Astro desplegado en https://dondeanime.com con 1.000+ páginas indexables, sitemap, structured data JSON-LD.
- **Mes 3 (agosto):** 200 fichas enriquecidas a mano, sistema de alertas por email, monetización activa.

Detalle semana a semana en `docs/roadmap.md`.

---

## Tareas pendientes menores (no bloquean)

- Configurar SSH con GitHub (generar ed25519, añadir a la cuenta, configurar ssh-agent con keychain). Mientras tanto HTTPS+token funciona perfectamente.

---

## Glosario rápido

- **AniList**: API GraphQL pública con catálogo completo de anime. Sin auth, 90 req/min.
- **TMDb**: The Movie Database. REST + API key v4. Provee `watch/providers` por país.
- **Anilist ID ↔ TMDb ID**: cruce manual al principio (buscar por título + filtro animation).
- **Slug**: parte URL-friendly del título (ej. "frieren-beyond-journeys-end").
- **Enriched manual**: ficha con texto propio (no solo datos crudos de APIs).

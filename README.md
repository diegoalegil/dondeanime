# DondeAnime

> Dónde y cuándo ver cualquier anime en streaming en España y Latinoamérica, en menos de 5 segundos.

**En producción:** [https://dondeanime.com](https://dondeanime.com)
**API pública:** [https://api.dondeanime.com/api/anime](https://api.dondeanime.com/api/anime)

![Status](https://img.shields.io/badge/status-en%20producción-success)
![Backend](https://img.shields.io/badge/backend-Spring%20Boot%204-6DB33F?logo=spring)
![Frontend](https://img.shields.io/badge/frontend-Astro%206-FF5D01?logo=astro)
![Database](https://img.shields.io/badge/db-PostgreSQL%2016-4169E1?logo=postgresql)
![Java](https://img.shields.io/badge/java-21-007396?logo=openjdk)

---

## Qué es

Web pública en español que responde dos preguntas concretas para cualquier anime:

1. **¿Está disponible en mi país?**
2. **¿En qué plataforma de streaming legal puedo verlo?**

No es un wiki ni un foro. Es un buscador rápido orientado a SEO long-tail con páginas estáticas indexables por país, plataforma, género y temporada. Cubre los 5 mercados hispanohablantes principales (España, México, Argentina, Chile, Colombia) y las 8 plataformas relevantes (Crunchyroll, Netflix, Prime Video, HBO Max, Disney+, Apple TV+, Hulu, Pluto TV).

---

## Cifras actuales

| Métrica | Valor |
|---|---|
| Anime catalogados | 100 (objetivo Sprint 8: 500) |
| Páginas estáticas generadas | 720 |
| Países soportados | 5 |
| Plataformas indexadas | 8+ |
| Tiempo de build frontend | 3.4 segundos |
| Tests backend (verdes) | 31 |
| Endpoints REST públicos | 11 |
| Sync automático | AniList 12h / TMDb match 24h / Providers 24h |

---

## Stack técnico

### Backend
- **Java 21** + **Spring Boot 4.0.6**
- **Hibernate 7.2** sobre Spring Data JPA
- **PostgreSQL 16** (en Docker tanto local como producción)
- **RestClient** (síncrono, Spring 6.1+) para llamadas a APIs externas
- **Jackson 3.x** (paquetes `tools.jackson.*` propios de Spring Boot 4)
- **Spring Security** con HTTP Basic para `/api/admin/**` y JWT para flujos públicos (alertas, doble opt-in)
- **Resend** para email transaccional

### Frontend
- **Astro 6** + **Tailwind 4** (build estático puro, SEO-first)
- **Geist** auto-hospedada
- JSON-LD (`TVSeries`, `BreadcrumbList`, `WebSite+SearchAction`, `ItemList`)
- Tema dark/light persistente
- Buscador in-memory contra `search-index.json`

### Infraestructura
- **Hetzner CX22** (Ubuntu + Docker + Caddy con Let's Encrypt automático)
- **Vercel** free tier para el frontend estático
- **Cloudflare** para DNS, proxy, Email Routing
- **Cloudflare R2** para backups diarios automatizados de Postgres (cron + rotación 30 días)
- **GitHub Actions** para CI (backend tests + frontend build)

### Fuentes de datos
- **AniList** (GraphQL, sin auth, 90 req/min) — metadata de anime
- **TMDb** (REST + API key v4) — providers de streaming por país

---

## Arquitectura

```
┌─────────────────┐         ┌──────────────────┐
│   Cloudflare    │─DNS────▶│      Vercel      │
│   (proxy/CDN)   │         │  (Astro estático)│
└────────┬────────┘         └────────┬─────────┘
         │                           │
         │ api.dondeanime.com        │ fetch (build time)
         ▼                           ▼
┌─────────────────────────────────────────────┐
│           Hetzner VPS CX22                  │
│  ┌──────────┐  ┌────────────┐  ┌────────┐   │
│  │  Caddy   │─▶│  Backend   │─▶│Postgres│   │
│  │  (SSL)   │  │ Spring 4   │  │   16   │   │
│  └──────────┘  └─────┬──────┘  └────────┘   │
│                      │                      │
└──────────────────────┼──────────────────────┘
                       │
            ┌──────────┼──────────┐
            ▼          ▼          ▼
       ┌────────┐ ┌────────┐ ┌────────┐
       │AniList │ │ TMDb   │ │ Resend │
       │GraphQL │ │ REST   │ │ Email  │
       └────────┘ └────────┘ └────────┘
```

El scheduler dentro del backend dispara los syncs periódicos. Tras cada sync de providers se llama un Deploy Hook de Vercel para rebuildear el frontend con datos frescos.

---

## Estructura del repo

```
DondeAnime/
├── README.md                          ← Estás aquí
├── CLAUDE.md                          ← Contexto operativo para Claude Code
├── AGENTS.md                          ← Equivalente para Codex
├── CODEX.md                           ← Convenciones, vetos, sprints 1-4
├── CODEX-BACKLOG-V2.md                ← Sprints 5-12 (~40 PRs)
├── DEPLOY.md                          ← Operación producción
├── docker-compose.yml                 ← Postgres local (puerto 5433)
├── docker-compose.prod.yml            ← Stack producción (3 servicios + Caddyfile)
├── Caddyfile                          ← Reverse proxy + SSL
├── .env.example                       ← Plantilla variables locales
├── .env.prod.example                  ← Plantilla variables producción
├── backend/                           ← Spring Boot 4
│   ├── pom.xml
│   ├── src/main/java/com/dondeanime/backend/
│   │   ├── admin/                     ← Auth + overrides editoriales
│   │   ├── alerts/                    ← Email alerts (doble opt-in JWT)
│   │   ├── affiliate/                 ← Tracking links afiliados
│   │   ├── anime/                     ← Entidad central + clientes AniList/TMDb
│   │   ├── provider/                  ← Watch providers por país
│   │   ├── scheduling/                ← Jobs @Scheduled
│   │   └── sitemap/                   ← Datos para sitemap.xml
│   └── src/main/resources/
│       ├── application.properties     ← Config base (dev local)
│       └── application-prod.properties ← Overrides para prod
├── frontend/                          ← Astro 6 + Tailwind 4
│   └── src/
│       ├── pages/                     ← Rutas SSG (anime/[slug], pais/[slug], plataforma/...)
│       ├── components/                ← Componentes Astro
│       ├── lib/                       ← Helpers (api, countries, platforms, seo)
│       └── layouts/                   ← BaseLayout
├── scripts/                           ← Validación de APIs y utilidades
└── docs/                              ← Brief, arquitectura, APIs, roadmap original
```

---

## Arranque local

### Requisitos

- Docker Desktop
- Java 21 (Temurin recomendado)
- Node.js 20+
- Cuenta TMDb con [API key v4](https://www.themoviedb.org/settings/api)

### Setup

```bash
# Clonar
git clone https://github.com/diegoalegil/dondeanime.git
cd dondeanime

# Variables de entorno
cp .env.example .env
# Editar .env y meter TMDB_API_KEY real

# Arrancar Postgres (puerto host 5433, evita colisión con Postgres nativo si lo tienes)
docker compose up -d

# Backend (puerto 8080)
cd backend
./mvnw spring-boot:run

# En otra terminal: frontend (puerto 4321)
cd frontend
npm install
npm run dev
```

Primera ingesta de datos:

```bash
curl -X POST http://localhost:8080/api/anime/sync?count=100
curl -X POST http://localhost:8080/api/anime/match
curl -X POST http://localhost:8080/api/anime/sync-providers
curl -X POST http://localhost:8080/api/anime/sync-trailers
```

Tarda unos 4 minutos en total. Al terminar tienes 100 anime con providers reales y trailers cuando TMDb los expone.

### Tests

```bash
cd backend
./mvnw test    # 31 tests, ~10 segundos
```

---

## Endpoints REST principales

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/anime` | Lista catálogo completo |
| GET | `/api/anime/{slug}` | Detalle de anime con providers por país |
| POST | `/api/anime/sync-trailers` | Sincroniza trailers de YouTube desde TMDb |
| GET | `/api/providers` | Lista plataformas (filtro por `?country=ES`) |
| GET | `/api/providers/{slug}/{country}` | Anime disponibles en plataforma+país |
| GET | `/api/genres` y `/api/genres/{slug}` | Géneros con listado |
| GET | `/api/seasons` y `/api/seasons/{year}/{season}` | Temporadas |
| GET | `/api/sitemap` | Todos los slugs/ids para generar sitemap.xml |
| POST | `/api/track/affiliate` | Tracking público de clicks afiliados |
| POST | `/api/admin/anime/{slug}/override` | (Auth) Override editorial de un campo |
| GET | `/api/admin/dashboard` | (Auth) Métricas de monetización y analítica |

Lista completa en [`CLAUDE.md`](./CLAUDE.md#endpoints-rest-disponibles).

---

## Producción

Operación documentada en [`DEPLOY.md`](./DEPLOY.md). Comandos rápidos:

```bash
# Acceso al VPS
ssh deploy@<ip-vps>

# Logs
docker compose -f docker-compose.prod.yml logs -f backend

# Sync manual
curl -X POST https://api.dondeanime.com/api/anime/sync

# Backup manual (cron diario a las 3:00 UTC ya configurado)
/opt/dondeanime/scripts/backup-postgres.sh
```

---

## Roadmap

| Fase | Estado |
|---|---|
| Backend funcional + catálogo 100 anime | Completado |
| Frontend SEO 720 páginas | Completado |
| Deploy producción + SSL + scheduler | Completado |
| Sprint 1 — Panel admin + overrides editoriales | Mergeado |
| Sprint 2 — Alertas email con doble opt-in (Resend) | Mergeado |
| Sprint 3 — Monetización afiliados + Plausible + AdSense slot | Mergeado |
| Sprint 4 — CI + Playwright + backups + ops Cloudflare | En curso |
| Sprint 5-12 — Backlog detallado | Documentado en [`CODEX-BACKLOG-V2.md`](./CODEX-BACKLOG-V2.md) |

Backlog futuro cubre: testing al 70%+ coverage con Testcontainers, migración a Flyway, observabilidad con Prometheus, PWA + Core Web Vitals, expansión catálogo a 500-2000 anime, búsqueda fulltext con autocomplete, hardening VPS, panel admin con JWT + 2FA, newsletter.

---

## Filosofía del proyecto

1. **Velocidad sobre completitud.** Mejor 500 anime bien presentados y al día que 30000 con datos rancios.
2. **SEO-first.** Cada decisión técnica considera el impacto en posicionamiento orgánico.
3. **Datos verificados en el top.** El top 50 se enriquece con texto editorial propio. El resto se queda automático.
4. **Sin humo.** Sin cookies superfluas, sin newsletters spam, sin pop-ups, sin oscurecer la pantalla con ads.
5. **Honestidad de afiliados.** Footer + `/legal/afiliados` declaran la relación. Sin trampas.

---

## Licencia

Repositorio privado. Todos los derechos reservados.

---

## Autor

[@diegoalegil](https://github.com/diegoalegil) — Estudiante DAM, España.

Proyecto desarrollado con asistencia de IA (Claude Code para arquitectura y verificación, Codex para sprints de desarrollo). Decisiones técnicas, modelo de datos, estrategia SEO y monetización son del autor.

# CODEX BACKLOG V3 — Sprints 13 al 20

> Continuación operativa de `CODEX.md` + `CODEX-BACKLOG-V2.md`. Cubre
> trabajo equivalente a 3+ meses adicionales tras cerrar V2.
> Codex respeta las MISMAS reglas inviolables.

---

## Auto-merge interno autorizado (regla NUEVA para sesiones largas)

A partir de esta fase Codex queda autorizado a hacer `gh pr merge <N>
--squash --delete-branch` sobre PRs INDIVIDUALES dirigidos a ramas de
sprint (no a main) cuando se cumplan TODAS estas condiciones:

1. Todos los checks de CI están en estado `SUCCESS` (build + tests +
   Playwright si aplica).
2. El PR no toca archivos vetados (BackendApplication, application*.properties,
   docker-compose.prod.yml) o, si los toca, lo justifica explícitamente
   con referencia al ítem del backlog.
3. Codex es el autor del PR.
4. No hay reviewers humanos asignados con cambios pedidos.

Los PRs `sprint-N → main` SIEMPRE esperan review humano. NUNCA auto-merge
a main, sin excepciones.

---

## Fase prioritaria — Cerrar todo lo abierto antes de avanzar

Al arrancar la sesión, ANTES de tocar nuevos sprints, ejecuta este
ciclo de cierre:

1. `gh pr list --state open --json number,baseRefName,title` para listar
   PRs abiertos.
2. Por cada PR con `baseRefName` que empiece por `sprint-` (no `main`):
   - `gh pr checks <N>` — si todos verdes, merge squash + delete branch.
   - Si algún check rojo o pendiente, esperar máximo 10 min y reintentar.
   - Si rojo persistente, abrir issue con detalle y SALTAR ese PR.
3. Por cada rama `sprint-N` que tenga sus PRs internos cerrados:
   - `git checkout sprint-N && git pull && git rebase main`
   - Si conflictos, resuelve inline siguiendo regla del backlog.
   - `git push origin sprint-N --force-with-lease`
   - `gh pr create --base main --title "Sprint N: <tema>"` con resumen
     ejecutivo (lista de PRs incluidos, cambios principales, tests,
     riesgos, deploy notes).
   - **NO mergear este PR.** Queda esperando review humano.
4. Cuando todos los PRs internos estén mergeados y todos los sprint
   branches tengan PR final a main abierto, avanzar a sprints nuevos.

---

## Sprint 13 — Internacionalización (preparación + EN activo)

**Objetivo:** todo el frontend en inglés además de español, sin perder
SEO local. Catalán y portugués preparados pero no activos.

**Branch:** `sprint-13`
**PRs:** 6

### PR 13.1 — Extracción strings a JSON
- Crear `frontend/src/i18n/es.json` con TODOS los strings hardcodeados
  del frontend (headings, labels, CTAs, mensajes de error).
- Auditar manualmente cada archivo `.astro` para extraerlos.
- Componente `<T>` o helper `t(key)` que lee del JSON activo según locale.
- Test E2E que verifica que no quedan strings hardcoded en español en
  páginas críticas (home, detalle, país).

### PR 13.2 — Traducción EN completa
- `frontend/src/i18n/en.json` con TODAS las claves traducidas.
- Codex traduce manualmente, NO usa Google Translate / DeepL.
- Test que verifica que `en.json` tiene las mismas claves que `es.json`
  (script de validación en CI).

### PR 13.3 — Rutas con prefijo locale
- Rutas EN bajo `/en/` (ej: `/en/anime/[slug]`, `/en/country/[slug]`).
- Rutas ES siguen sin prefijo (default).
- Astro middleware que detecta locale del path y carga JSON correspondiente.
- `hreflang` actualizado en cada página (es + en + x-default).

### PR 13.4 — Sitemaps por locale
- Generar `sitemap-en.xml` con todas las URLs EN.
- Sitemap index referencia ambos (es + en).
- Robots.txt actualizado.

### PR 13.5 — Detección automática + selector manual
- Componente `<LanguageSwitcher>` en header con bandera/código.
- Detección por `navigator.language` en primer visit (redirect suave a
  `/en/...` si el browser está en inglés).
- Cookie de preferencia que recuerda elección manual del usuario (sobre-
  escribe detección automática).

### PR 13.6 — Cobertura SEO multilingüe
- Schema.org `Organization` con `availableLanguage: ["es", "en"]`.
- OpenGraph `og:locale` y `og:locale:alternate` por página.
- Test que valida JSON-LD presencia de campos i18n.

---

## Sprint 14 — Sistema de notificaciones tiempo real

**Objetivo:** cuando una plataforma añade un anime, los usuarios
suscritos a alertas reciben email Y opcionalmente notificación push del
navegador.

**Branch:** `sprint-14`
**PRs:** 5

### PR 14.1 — Event-driven backend
- Spring `@EventListener` para eventos `ProviderAddedEvent`.
- `ProviderSyncService` emite evento cuando detecta nuevo provider para
  un anime que tiene alerts activas.
- Test integration que dispara evento y verifica que `EmailAlertService`
  lo procesa.

### PR 14.2 — Web Push notifications
- Entidad `PushSubscription` (id, user_email, endpoint, p256dh, auth,
  countryIso, createdAt).
- Endpoint `POST /api/push/subscribe` que recibe la subscription del
  browser y la guarda.
- Servicio `WebPushService` con librería `nl.martijndwars:web-push:5.x`.
- Frontend: `<PushSubscribeButton>` que pide permiso y suscribe.

### PR 14.3 — Notificaciones bidireccionales
- Cuando se dispara `ProviderAddedEvent`, además de email se manda push
  a las subscriptions del país relevante.
- VAPID keys en `.env.prod` (`VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`).
- Service worker frontend que muestra notificación con click→ficha.

### PR 14.4 — Dashboard de notificaciones
- `/admin/notifications` lista todas las push subscriptions activas,
  alertas enviadas últimas 24h, tasa entrega.
- Endpoint `GET /api/admin/notifications/stats` que agrega métricas.
- Botón "test push" en admin para verificar setup en vivo.

### PR 14.5 — Auto-unsubscribe en bouncing endpoints
- Si la API de push devuelve 410 Gone o 404 Not Found, borrar la
  subscription automáticamente.
- Cron diario que verifica subscripciones inactivas > 60 días y las
  purga.

---

## Sprint 15 — Premium con Stripe

**Objetivo:** plan Premium pago (3€/mes) con ad-free, alertas ilimitadas,
notificaciones push prioritarias, badge de patron. Stripe Checkout +
webhooks.

**Branch:** `sprint-15`
**PRs:** 6

### PR 15.1 — Modelo de datos Premium
- Entidad `Subscriber` (id, email, stripeCustomerId, planTier,
  subscribedAt, expiresAt, lastPaymentAt).
- Migración Flyway V<siguiente>__premium.sql.
- Repositorio + service `SubscriberService` con `isPremium(email)`.

### PR 15.2 — Stripe integration
- Librería `com.stripe:stripe-java:25.x` en pom.xml.
- Servicio `StripeService` con métodos `createCheckoutSession(email)`,
  `verifyWebhookSignature`.
- Endpoint `POST /api/premium/checkout` que devuelve URL de Checkout.
- Endpoint `POST /api/premium/webhook` que procesa eventos Stripe
  (`customer.subscription.created`, `.updated`, `.deleted`,
  `invoice.payment_succeeded`, `invoice.payment_failed`).

### PR 15.3 — Frontend Premium
- Página `/premium` con pricing + comparativa free vs premium + CTA
  Checkout.
- Componente `<PremiumBadge>` en perfil/admin si usuario es premium.
- Modo ad-free: si user es premium, AdSlot no renderiza.
- Test E2E con Stripe test mode (clave `pk_test_...`).

### PR 15.4 — Customer portal
- Endpoint `POST /api/premium/portal` que genera URL de Stripe Customer
  Portal (cancelación, cambio de tarjeta, historial facturas).
- Botón en `/cuenta` o sidebar admin.

### PR 15.5 — Beneficios Premium activos
- Alertas ilimitadas: free tiene cap 10 simultáneas, premium ilimitadas.
- Push prioritario: si user es premium, push se manda antes en la cola.
- Acceso a `/api/admin/dashboard` para Patrons que paguen tier alto
  (futuro, marcar campo `tier`).

### PR 15.6 — Email confirmación + recibos
- Email transaccional vía Resend al confirmar suscripción.
- Email mensual con recibo (Stripe lo manda nativo pero personalizamos).
- Email de cortesía 30 días tras cancelación: "vuelve cuando quieras".

---

## Sprint 16 — API pública con OpenAPI

**Objetivo:** exponer `/api/v1/*` como API pública con docs Swagger,
rate limiting por API key, plan free + paid.

**Branch:** `sprint-16`
**PRs:** 5

### PR 16.1 — Versionado y namespace v1
- Refactor: endpoints públicos pasan a `/api/v1/anime`, `/api/v1/providers`,
  etc. (manteniendo `/api/anime` con redirect 301 + deprecation header
  durante 6 meses).
- Documentar política de versionado en `docs/api-versioning.md`.

### PR 16.2 — OpenAPI + Swagger UI
- Springdoc OpenAPI 2.x en pom.
- Anotaciones `@Operation`, `@Schema` en todos los DTOs y controllers
  públicos.
- Swagger UI en `/api/v1/docs` (público).
- Test que valida que `openapi.yaml` se genera sin warnings.

### PR 16.3 — API keys + rate limiting
- Entidad `ApiKey` (id, key, ownerEmail, tier, createdAt, lastUsedAt,
  monthlyQuota, monthlyUsage).
- Header `X-API-Key` requerido para `/api/v1/*` (excepto `/docs`).
- Bucket4j con configuración por tier (free: 1000 req/mes, paid: 100000).
- Endpoint `POST /api/admin/api-keys` (Basic auth, solo admin) que crea
  keys nuevas.

### PR 16.4 — Página landing API
- `/api` página pública explicando la API, cómo conseguir key, ejemplos
  curl/JS, link a Swagger UI.
- Schema `Service` JSON-LD.

### PR 16.5 — Stats consumo API
- `/admin/api-keys` dashboard con tabla de keys, uso mensual, top
  endpoints consumidos.
- Endpoint `GET /api/admin/api-keys/stats`.

---

## Sprint 17 — Programmatic SEO masivo

**Objetivo:** generar 5000+ páginas adicionales mediante combinatorias
útiles. Sin sacrificar calidad. Todas con texto autogenerado pero leíble.

**Branch:** `sprint-17`
**PRs:** 5

### PR 17.1 — Páginas "Anime de X minutos por capítulo"
- Rutas `/anime/duracion/[mins].astro` para duraciones populares (12, 22,
  24, 25, 45, 60 min).
- Filtro backend por `episodeDuration`.
- Schema ItemList + texto introductorio dinámico generado con templates.

### PR 17.2 — Páginas "Anime con X o menos capítulos"
- Rutas `/anime/episodios/menos-de-[N].astro` (12, 24, 50, 100, 200).
- Útil para usuarios buscando anime cortos.

### PR 17.3 — Páginas "Anime para principiantes en [género]"
- Rutas `/empezar/[genero].astro` con curaduría: top 10 más populares
  del género + texto orientativo.
- Manual override en `anime_override.beginner_recommendation` para
  permitir editorial.

### PR 17.4 — Páginas "Mejor anime de [estudio]"
- Rutas `/estudio/[slug]/mejores.astro` con top del estudio.
- Schema CreativeWorkSeries + texto sobre el estudio.

### PR 17.5 — Auto-generación texto descriptivo (templates)
- Sistema `AutoTextGenerator` con templates Markdown que rellenan
  variables (nombre anime, año, score, género, etc.).
- Aplicado a páginas combinatoria género×plataforma para dar texto único
  por página (no duplicado).
- Test que verifica que el texto generado tiene mínimo 150 palabras y
  máximo 500.

---

## Sprint 18 — Performance audit deep + CDN tuning

**Objetivo:** Core Web Vitals perfectos en todas las páginas. Cache
agresivo. Edge functions Vercel para personalización ligera sin perder
SSG.

**Branch:** `sprint-18`
**PRs:** 5

### PR 18.1 — Audit Lighthouse de TODAS las plantillas
- Script que lanza Lighthouse contra una URL de cada tipo de plantilla
  (home, detalle, país, plataforma, género, temporada, mejores, etc.).
- Reporte agregado en `frontend/lighthouse-report.json`.
- CI falla si alguna baja de 90 en cualquier categoría.

### PR 18.2 — Cloudflare page rules cache
- Documentar reglas Cloudflare:
  - HTML estático: cache 1h en edge
  - Assets versionados (CSS/JS con hash): cache 1 año
  - Imágenes: cache 30 días
  - API responses: bypass cache
- Script de configuración vía API Cloudflare en `scripts/cloudflare/`.

### PR 18.3 — Critical CSS inline
- Extraer above-the-fold CSS de cada plantilla principal e inlinear en
  `<head>`.
- Resto del CSS con `media="print" onload="this.media='all'"` (truco
  para no-bloqueante).
- Verificar LCP mejora con Lighthouse.

### PR 18.4 — Service Worker estratégico
- Reforzar SW del Sprint 7 con:
  - Background sync para alertas pendientes.
  - Cache de imágenes de cover con stale-while-revalidate.
  - Pre-fetch de páginas vinculadas en home.

### PR 18.5 — Vercel Edge Function ligero
- Edge function que personaliza el `<head>` añadiendo `<link
  rel="preconnect">` al CDN de imágenes (CDN diferente por país detectado
  vía Geo).
- Edge function NO bloquea SSG; solo wrappea response.

---

## Sprint 19 — PWA completa

**Objetivo:** experiencia app instalable real. Offline funcional.
Sincronización en background. Splash screen.

**Branch:** `sprint-19`
**PRs:** 4

### PR 19.1 — Manifest completo + icons
- `manifest.json` con todos los icons (16, 32, 192, 512, 1024,
  maskable_icon).
- `screenshots` array para que el "install" dialog muestre previsualización.
- `shortcuts` array (3 atajos: Buscar, Mi país, Alertas).

### PR 19.2 — Offline first para páginas vistas
- Service Worker que cache cada página visitada para acceso offline.
- Página `/offline.astro` que se muestra si SW no tiene cache de la URL
  pedida y no hay red.

### PR 19.3 — Background sync de alertas
- Cuando user crea alerta offline, queue en IndexedDB.
- SW background sync que dispara `POST /api/alerts` cuando hay red.
- Notificación push al user cuando se completa.

### PR 19.4 — Install promotion
- Banner sutil tras 3 visitas: "Instala DondeAnime como app".
- Botón cierra para no molestar (cookie 30 días).
- Tracking en Plausible: `install_prompt_shown`, `install_completed`.

---

## Sprint 20 — Recomendaciones personalizadas

**Objetivo:** "Otros anime que te pueden gustar" en cada ficha de detalle.
Recomendaciones por género + score + país del visitante. Sin requerir
cuenta de usuario.

**Branch:** `sprint-20`
**PRs:** 4

### PR 20.1 — Algoritmo de similitud
- Servicio `RecommendationService` con método
  `findSimilar(animeId, limit)`.
- Heurística simple:
  1. Mismo género primario + score > 70 → top 10.
  2. Mismo estudio + score > 70 → top 5.
  3. Mismas tags AniList (cuando se sincronicen, ver PR 20.2).
- Cachear resultado con Caffeine (TTL 24h).

### PR 20.2 — Sync de tags AniList
- AniList expone `tags { name rank }`. Sincronizar al pedir media.
- Tabla `anime_tag` (anime_id, tag_name, rank).
- Recomendaciones usan tags con `rank > 70` como similarity.

### PR 20.3 — Endpoint y componente frontend
- `GET /api/anime/{slug}/similar` retorna 10 recomendaciones.
- Componente `<SimilarAnime>` en página detalle.
- Schema ItemRelated.

### PR 20.4 — Tracking de clicks en recomendaciones
- Event `recommendation_clicked` con anime origen + anime destino.
- Tabla `recommendation_event` para análisis offline.
- Dashboard admin con top "el del anime A clickea anime B".

---

## Sprint 21+ (esbozos)

Cuando llegues al final del Sprint 20 sin esperar review humano,
documenta esbozos para los siguientes sprints en `CODEX-BACKLOG-V4.md`:

- **Sprint 21**: chatbot de búsqueda con embeddings (OpenAI/Anthropic).
- **Sprint 22**: integración con Trakt.tv (sincronizar "lo que ya he visto").
- **Sprint 23**: app móvil nativa (React Native o Capacitor).
- **Sprint 24**: marketplace de listas curadas (users premium publican
  listas, otros las siguen).

No empieces ninguno de estos sin que Diego apruebe el v4.

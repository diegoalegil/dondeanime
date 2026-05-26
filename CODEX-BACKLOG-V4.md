# CODEX BACKLOG V4 — Sprints 21 al 24

> Continuación operativa de `CODEX-BACKLOG-V3.md`. Este documento es
> planificación, no autorización para ejecutar. No empezar ningún sprint
> de V4 hasta que Diego revise y apruebe este PR.

---

## Reglas heredadas

Se mantienen las reglas inviolables de `CODEX.md`, `CODEX-BACKLOG-V2.md`
y `CODEX-BACKLOG-V3.md`:

1. Nada se mergea a `main` sin review humano.
2. PRs internos a ramas `sprint-N` pueden auto-mergearse solo con CI verde.
3. Sin dependencias nuevas sin justificación explícita en PR.
4. Sin servicios de pago activados sin aprobación de Diego.
5. No exponer entidades JPA en REST.
6. No tocar archivos vetados salvo que el PR lo pida explícitamente.
7. Cualquier integración externa debe tener tests con mocks; nada de red en tests.

---

## Sprint 21 — Chatbot de búsqueda con embeddings

**Objetivo:** añadir un asistente de búsqueda que responda preguntas tipo
"quiero algo corto, oscuro y en Crunchyroll España" usando el catálogo de
DondeAnime. Debe recomendar anime y enlazar a fichas existentes, no inventar
datos.

**Branch:** `sprint-21`
**PRs:** 5

### PR 21.1 — Modelo de documentos para embeddings
- Crear DTO interno `AnimeSearchDocument` con título, sinopsis española,
  géneros, temporada, score, país/plataformas y URL canónica.
- Endpoint admin `POST /api/admin/embeddings/rebuild` preparado pero protegido
  por Basic Auth.
- Servicio `EmbeddingDocumentBuilder` que genera documentos deterministas desde
  `Anime`.
- Tests unitarios para asegurar que no incluye campos internos (`id`, `tmdbId`,
  `syncedAt`).

### PR 21.2 — Tabla vectorial y almacenamiento
- Añadir tabla `anime_embedding` con `anime_id`, `model`, `content_hash`,
  `embedding`, `updated_at`.
- Decidir implementación tras revisar coste:
  - Opción preferida inicial: PostgreSQL + `pgvector` si producción lo soporta.
  - Fallback permitido: almacenar JSON `float[]` y hacer reranking in-memory
    para catálogo pequeño.
- Documentar decisión en `docs/embeddings.md`.
- Tests de repositorio con mocks o perfil local sin depender de API externa.

### PR 21.3 — Cliente de embeddings
- Crear interfaz `EmbeddingClient` y una implementación real configurable por
  env.
- Variables nuevas solo en `.env.prod.example` y documentadas en `CLAUDE.md`.
- No commitear claves. No activar llamadas pagadas en CI.
- Tests con cliente fake que devuelve vectores deterministas.

### PR 21.4 — Endpoint de chatbot
- `POST /api/chat/search` recibe pregunta y país opcional.
- Devuelve DTO con:
  - respuesta breve en español,
  - lista de recomendaciones,
  - explicación corta por recomendación,
  - enlaces a fichas.
- El backend debe rechazar prompts vacíos, demasiado largos o con intento de
  inyección obvia.
- Tests para validación, ranking y no alucinación: solo se devuelven anime
  existentes.

### PR 21.5 — UI del chatbot
- Componente `<AnimeChatSearch>` accesible desde home y búsqueda.
- UI simple: input, estado cargando, resultados enlazables.
- No usar chat flotante invasivo.
- E2E: pregunta mockeada devuelve recomendaciones y navega a una ficha.

---

## Sprint 22 — Integración con Trakt.tv

**Objetivo:** permitir que un usuario conecte Trakt.tv para marcar anime
vistos y mejorar recomendaciones. La integración debe ser opcional y revocable.

**Branch:** `sprint-22`
**PRs:** 5

### PR 22.1 — OAuth Trakt
- Crear endpoints:
  - `GET /api/trakt/oauth/start`
  - `GET /api/trakt/oauth/callback`
- Guardar tokens cifrados o, si no hay cifrado aprobado, no guardar refresh
  token y documentar limitación.
- Variables `TRAKT_CLIENT_ID`, `TRAKT_CLIENT_SECRET`, `TRAKT_REDIRECT_URI`.
- Tests de callback con Trakt mockeado.

### PR 22.2 — Modelo de usuario externo
- Entidad `ExternalAccount` con provider, externalUserId, email opcional,
  tokens, scopes y timestamps.
- Entidad `UserWatchedAnime` con user/account, animeSlug, watchedAt, source.
- No crear sistema de cuentas general fuera del alcance; usar sesión/token de
  integración mínimo.
- Tests de normalización y unicidad.

### PR 22.3 — Sync de historial Trakt
- Servicio `TraktSyncService` que importa watch history y ratings.
- Matching por título + año contra catálogo local, sin tocar
  `AnimeMatchingService`.
- Endpoint admin/user `POST /api/trakt/sync`.
- Tests con respuestas Trakt mockeadas y casos sin match.

### PR 22.4 — Filtros "no vistos"
- Frontend permite ocultar anime ya vistos en listados compatibles.
- Estado persistido en localStorage si no hay sesión backend.
- Backend expone endpoint `GET /api/trakt/watched` con slugs.
- E2E: conectar cuenta fake, ocultar visto, limpiar filtro.

### PR 22.5 — Recomendaciones personalizadas con vistos
- `RecommendationService` acepta contexto opcional de slugs vistos.
- Excluye vistos y prioriza géneros/plataformas que el usuario suele ver.
- Dashboard admin muestra métricas agregadas anónimas: cuentas conectadas,
  syncs 7/30 días, matches fallidos.
- Tests de ranking y privacidad: ningún endpoint público filtra emails/tokens.

---

## Sprint 23 — App móvil nativa

**Objetivo:** empaquetar DondeAnime como app móvil instalable con UX nativa
básica, reutilizando el frontend cuando sea razonable. Decisión inicial
recomendada: Capacitor, porque el sitio ya es Astro/PWA.

**Branch:** `sprint-23`
**PRs:** 5

### PR 23.1 — Decisión técnica y scaffold
- Crear `mobile/DECISION.md` comparando Capacitor vs React Native.
- Si Diego aprueba Capacitor, crear scaffold mínimo `mobile/` sin publicar en
  stores.
- No añadir cuentas Apple/Google ni costes.
- CI solo valida lint/build del wrapper si no requiere SDK móvil local.

### PR 23.2 — Deep links y navegación
- Configurar deep links:
  - `dondeanime://anime/{slug}`
  - `dondeanime://buscar`
- El frontend debe resolver deep links a rutas existentes.
- Tests unitarios de parsing de enlaces.

### PR 23.3 — Push móvil
- Reutilizar infraestructura de push del Sprint 14 cuando exista.
- Adaptador móvil para registrar device token y país preferido.
- No enviar newsletters ni campañas; solo alertas solicitadas.
- Tests con proveedor push mockeado.

### PR 23.4 — Offline móvil
- Cache de últimas fichas visitadas.
- Pantalla offline coherente con `/offline`.
- Sin cachear datos sensibles ni tokens de terceros en claro.
- E2E o test de integración del service worker cuando sea viable.

### PR 23.5 — Documentación de release
- `mobile/RELEASE.md` con pasos para generar builds internos.
- Checklist de privacidad para App Store / Play Store.
- Capturas requeridas, iconos, permisos y textos legales.
- No publicar en stores desde Codex.

---

## Sprint 24 — Marketplace de listas curadas

**Objetivo:** permitir listas públicas de anime creadas por usuarios premium
o curadores aprobados. Ejemplos: "anime corto para empezar", "mechas clásicos",
"romance sin relleno".

**Branch:** `sprint-24`
**PRs:** 6

### PR 24.1 — Modelo de listas
- Entidades:
  - `CuratedList` (slug, title, description, owner, visibility, status).
  - `CuratedListItem` (listId, animeSlug, position, note).
- Estados: `DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `REJECTED`.
- DTOs públicos sin emails ni ids internos.
- Tests de orden, slug único y validaciones.

### PR 24.2 — API pública de listas
- `GET /api/lists` lista publicadas.
- `GET /api/lists/{slug}` detalle con anime ordenados.
- Schema.org `ItemList`.
- Tests MVC para DTO y 404.

### PR 24.3 — Editor admin
- `/admin/lists` para crear, ordenar y publicar listas.
- Reutilizar Basic Auth actual.
- Drag/drop permitido solo si no añade dependencia pesada; si no, controles
  subir/bajar.
- E2E del flujo crear lista -> añadir anime -> publicar.

### PR 24.4 — Marketplace público
- Página `/listas` con filtros por género, país/plataforma y duración.
- Página `/listas/[slug]` SEO indexable.
- Cards compactas, no landing marketing.
- Sitemap `sitemap-listas.xml`.

### PR 24.5 — Curadores y permisos
- Preparar modelo `CuratorProfile` para usuarios aprobados.
- No abrir registro público todavía.
- Admin puede aprobar/revocar curadores.
- Tests de permisos: solo admin o propietario puede editar borrador.

### PR 24.6 — Monetización Premium
- Permitir marcar listas como `premiumOnly`.
- Usuarios no premium ven preview limitado y CTA a Premium.
- No bloquear listas públicas existentes.
- Métricas en dashboard: vistas por lista, clicks a anime desde lista,
  conversiones a Premium si Stripe ya está activo.
- Tests de acceso free/premium con mocks.

---

## Bloqueos previstos antes de ejecutar V4

1. Diego debe aprobar proveedor de embeddings y coste máximo mensual.
2. Diego debe decidir si Trakt.tv se activa antes o después de cuentas propias.
3. Diego debe aprobar Capacitor vs React Native antes de Sprint 23.
4. Diego debe aprobar si las listas curadas son solo admin o también usuarios
   premium desde el primer lanzamiento.

# Roadmap DondeAnime — 90 días

> Plan realista semana a semana. Cada semana tiene 1 entregable visible y mesurable. Si una semana se va a peor, NO arrastrar la siguiente: cortar el alcance, no el ritmo.

**Regla de oro:** mejor un MVP feo desplegado en mes 1 que una obra de arte sin lanzar en mes 6.

---

## MES 1 — Fundaciones técnicas (junio)

Objetivo del mes: backend funcional que ingiere datos de las APIs y los sirve por REST.

### Semana 1 (días 1-7) — Validación y setup
- [ ] **Día 1:** Validar las 3 APIs con `scripts/test-apis.js` (ya están AniList y TMDb)
- [ ] **Día 1:** Comprar dominio dondeanime.com
- [ ] **Día 2:** Crear repo privado en GitHub, push inicial con README, .gitignore y docs
- [ ] **Día 3-4:** Inicializar proyecto Spring Boot 3 con dependencias: Web, JPA, PostgreSQL, Validation, Lombok
- [ ] **Día 5-6:** Instalar PostgreSQL local, crear BD `dondeanime_dev`, conectar Spring Boot
- [ ] **Día 7:** Definir entidades JPA: `Anime`, `AnimePlatform`. Migrar con Flyway o similar.

**Entregable semana 1:** `mvn spring-boot:run` arranca, conecta a BD, devuelve `[]` en `GET /api/anime`.

### Semana 2 — Cliente AniList
- [ ] Servicio `AniListClient` con WebClient o RestTemplate
- [ ] Mapper de respuesta GraphQL → entidad `Anime`
- [ ] Endpoint `POST /api/admin/sync/anilist` que importa los 100 anime más populares
- [ ] Comprobar en BD que se insertan correctamente con todos los campos
- [ ] Manejo de duplicados (UPSERT por `anilist_id`)

**Entregable semana 2:** `GET /api/anime` devuelve 100 anime reales desde tu BD.

### Semana 3 — Cliente TMDb y cruce
- [ ] Servicio `TmdbClient`
- [ ] Endpoint `POST /api/admin/match-tmdb` que busca cada anime en TMDb por título y guarda el `tmdb_id`
- [ ] Servicio que obtiene `watch/providers` para los países hispanos
- [ ] Persistir en tabla `anime_platform`

**Entregable semana 3:** Para cada anime se puede ver en qué plataformas está disponible en España, México y Argentina al menos.

### Semana 4 — Programación automática y limpieza
- [ ] `@Scheduled` para refresco cada 12h del catálogo top
- [ ] Logging serio (logback con rotación)
- [ ] Endpoint `GET /api/anime/{slug}` con todos los campos
- [ ] Validaciones y manejo de errores en endpoints públicos
- [ ] Refactor de lo aprendido en estas 4 semanas

**Entregable mes 1:** API REST estable con catálogo poblado de 100-200 anime con providers por país.

---

## MES 2 — Frontend y SEO (julio)

Objetivo del mes: web pública desplegada, indexable, con 1.000+ páginas en Google.

### Semana 5 — Setup frontend Astro
- [ ] Inicializar proyecto Astro 4
- [ ] Diseñar layout base (header, footer, contenedor)
- [ ] Tema oscuro / claro (CSS variables, no librería)
- [ ] Página home con hero y "estrenos esta semana"
- [ ] Fetch al backend en build time (datos estáticos)

**Entregable semana 5:** Home en local mostrando datos reales del backend.

### Semana 6 — Páginas dinámicas y SEO técnico
- [ ] Páginas `/anime/[slug]` generadas estáticamente para los 100 anime
- [ ] Meta tags por página: `title`, `description`, Open Graph, Twitter Card
- [ ] Sitemap.xml automático
- [ ] robots.txt
- [ ] Structured data JSON-LD (`TVSeries` schema)
- [ ] Página de error 404 decente

**Entregable semana 6:** Lighthouse SEO 100/100 en al menos la home y una página de detalle.

### Semana 7 — Deploy a producción
- [ ] Comprar VPS en Hetzner (CX22, ~6€/mes)
- [ ] Provisionar: instalar Java 21, PostgreSQL, nginx
- [ ] Migrar BD local a VPS (datos + esquema)
- [ ] Desplegar backend Spring Boot como servicio systemd
- [ ] Configurar Cloudflare como proxy delante de la IP del VPS
- [ ] Conectar dondeanime.com → Vercel (frontend) y `api.dondeanime.com` → VPS (backend)
- [ ] HTTPS con Cloudflare (Full Strict)
- [ ] Vercel deploy automático desde main

**Entregable semana 7:** https://dondeanime.com cargando datos reales desde la API en producción.

### Semana 8 — Indexación y primeras 1.000 páginas
- [ ] Aumentar catálogo importado a 1.000-2.000 anime
- [ ] Verificar todas las páginas son indexables
- [ ] Enviar sitemap a Google Search Console y Bing Webmaster
- [ ] Compartir la web en r/anime, foros españoles, Twitter (sin spam, presentación honesta)
- [ ] Empezar a monitorizar impresiones y CTR en Search Console

**Entregable mes 2:** Web pública, 1.000+ páginas enviadas a indexar, primeras impresiones en Search Console.

---

## MES 3 — Contenido manual y monetización (agosto)

Objetivo del mes: top 200 títulos enriquecidos a mano + monetización activa + primeros euros.

### Semana 9 — Enriquecimiento manual top 50
- [ ] Identificar los 50 anime más buscados ahora mismo (Search Console + AniList trending)
- [ ] Para cada uno: verificar fecha estreno ES/LatAm, doblaje, plataformas, escribir 2-3 párrafos personales
- [ ] Sistema en backend para marcar `enriched_manual = true`
- [ ] Mostrar badge visual en las fichas enriquecidas
- [ ] Página `/calendario` con próximos estrenos por mes

**Entregable semana 9:** 50 fichas premium publicadas.

### Semana 10 — Sistema de alertas por email
- [ ] Entidades `Subscriber` y `Alert`
- [ ] Endpoint público para suscribirse a un anime concreto (sin login, solo email + confirmación)
- [ ] Servicio de email transaccional (Resend o SMTP de Cloudflare, ambos free tier)
- [ ] Cron diario que detecta anime que estrenan en 24h y envía aviso a los suscriptores
- [ ] Plantilla de email decente (HTML simple, sin imágenes pesadas)

**Entregable semana 10:** Usuario puede pulsar "Avísame cuando salga" y recibe el email el día previo.

### Semana 11 — Monetización fase 1
- [ ] Aplicar al programa de afiliados de Amazon España (cuentas con menos requisitos)
- [ ] Aplicar al programa de afiliados de Crunchyroll
- [ ] Implementar Google AdSense (anuncio modesto, no invasivo)
- [ ] Reemplazar enlaces a Crunchyroll, Amazon, Netflix por links de afiliado donde se pueda
- [ ] Página `/contacto` y `/privacidad` (legales mínimas para AdSense)
- [ ] Banner de cookies acorde con RGPD (puede ser propio, no necesitas Cookiebot pagado)

**Entregable semana 11:** Primer click de afiliado registrado, AdSense aprobado o en revisión.

### Semana 12 — Pulido y retrospectiva
- [ ] Subir a 200 fichas enriquecidas manualmente
- [ ] Subir a 5.000-10.000 anime en catálogo total
- [ ] Página "ranking de la temporada" con top 30 anime
- [ ] Revisar Search Console: top queries, páginas con CTR bajo, mejoras
- [ ] Documentar aprendizajes en `docs/retrospectiva-mes-3.md`
- [ ] Decidir prioridades mes 4 según datos reales

**Entregable mes 3:** Web con 10.000 páginas indexadas, 200 enriquecidas, monetización funcionando, primeros ingresos pequeños.

---

## Métricas que vamos a vigilar cada semana

| Métrica | Herramienta | Objetivo mes 3 |
|---|---|---|
| Páginas indexadas | Google Search Console | 2.000+ |
| Impresiones/mes | Google Search Console | 10.000+ |
| Clicks/mes | Google Search Console | 500+ |
| Visitas únicas/mes | Plausible o Umami (privacy-friendly) | 1.000+ |
| Suscriptores email | BD propia | 50+ |
| Ingresos | Manual | €30+ |

---

## Lo que NO entra en estos 90 días (aunque te tienten)

- App móvil
- Logo profesional caro (usa algo simple en Figma free)
- Rediseño grande del frontend
- Sistema de cuentas de usuario (más allá de email para alertas)
- Sistema de comentarios
- Versión Premium de pago (esperar a tener tráfico real)
- Idiomas distintos al español
- Soporte para manga
- Soporte para series asiáticas/juegos JP

---

## Si algo va peor de lo previsto

**Si en mes 1 no tienes la API funcionando:** simplifica el modelo. Quita `anime_platform` y haz solo `anime`. Avanza.

**Si en mes 2 el deploy te bloquea:** usa Railway o Render aunque sea más caro, no pierdas semanas con DevOps.

**Si en mes 3 no llegan visitas:** revisa Search Console, verifica que las páginas estén indexadas, comparte en más sitios. El SEO tarda. No abandones.

**Si todo se rompe:** lee la retrospectiva, identifica la decisión equivocada concreta, no te flageles. Pivotar es mejor que abandonar.

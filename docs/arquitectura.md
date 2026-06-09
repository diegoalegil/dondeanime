# Arquitectura técnica de DondeAnime

> Documento vivo. Cada decisión técnica importante se documenta aquí con su razón.

---

## Diagrama de flujo de datos

```
                                ┌──────────────────────┐
                                │   Usuario final      │
                                │  (navegador web)     │
                                └──────────┬───────────┘
                                           │
                                           ▼
                                ┌──────────────────────┐
                                │   Cloudflare CDN     │
                                │   (caché + DNS)      │
                                └──────────┬───────────┘
                                           │
                                           ▼
                                ┌──────────────────────┐
                                │   Frontend Astro     │
                                │   (Vercel, estático) │
                                └──────────┬───────────┘
                                           │ API REST
                                           ▼
                                ┌──────────────────────┐
                                │  Backend Spring Boot │
                                │  (Hetzner VPS)       │
                                └──────────┬───────────┘
                                           │
                                           ▼
                                ┌──────────────────────┐
                                │   PostgreSQL         │
                                │   (Hetzner VPS)      │
                                └──────────▲───────────┘
                                           │
                          ┌────────────────┴────────────────┐
                          │                                 │
                ┌─────────┴─────────┐               ┌───────┴────────┐
                │  Cron 12h         │               │  Cron diario   │
                │  Refresca anime   │               │  Refresca      │
                │  populares        │               │  catálogo full │
                └─────────┬─────────┘               └────────┬───────┘
                          │                                  │
                          ▼                                  ▼
                ┌───────────────────┐               ┌─────────────────┐
                │   AniList API     │               │   TMDb API      │
                │   (GraphQL)       │               │   (REST)        │
                └───────────────────┘               └─────────────────┘
```

---

## Stack final y justificación

### Backend: Java 21 + Spring Boot 3

**Por qué:**
- Diego está estudiando DAM y Java es su prioridad académica. Trabajar en un proyecto real refuerza lo de clase.
- Spring Boot tiene ecosistema maduro para REST, JPA, scheduled tasks (cron), validation.
- Tipado fuerte = menos bugs en runtime.

**Alternativas descartadas:**
- Node.js: más rápido de prototipar pero no aporta al aprendizaje DAM.
- Python (FastAPI): mismo motivo que Node.
- Go: curva de aprendizaje extra que no toca ahora.

### Base de datos: PostgreSQL 16

**Por qué:**
- Open source, gratis, robusto.
- JSON nativo (útil para guardar respuestas crudas de APIs sin diseñar 40 tablas).
- Full-text search nativo (útil para el buscador interno sin meter Elasticsearch).

**Alternativas descartadas:**
- MySQL: válido pero PostgreSQL es mejor para JSON y full-text.
- SQLite: tentador por simplicidad pero no escala bien con concurrencia.
- MongoDB: sin necesidad de schema-less, los datos son tabulares.

### Frontend: Astro 4

**Por qué (esto es crítico para el éxito SEO):**
- Genera HTML estático en build → Google indexa todo perfectamente, sin esperar a JavaScript.
- Permite islas de interactividad (React/Vue/Svelte) solo donde se necesita.
- Tiempos de carga sub-segundo de fábrica.
- Perfecto para webs con miles de páginas tipo "una ficha por título".

**Alternativas descartadas:**
- Next.js: válido pero más complejo, y SSR no aporta nada cuando los datos cambian cada 12h.
- React puro (CSR): MAL. Google indexa peor, pierde la batalla SEO desde el día 1.
- Vue/Nuxt: mismo nivel que Next pero menos comunidad para SEO técnico.

### Hosting backend: Hetzner Cloud (VPS CX22)

**Por qué:**
- ~6€/mes por 2 vCPU, 4 GB RAM, 40 GB SSD, 20 TB tráfico.
- Datacenter europeo (Falkenstein/Helsinki) = baja latencia para España y LatAm vía Cloudflare.
- Permite instalar PostgreSQL en el mismo VPS al principio.

**Alternativas descartadas:**
- Railway/Render: cómodos pero caros para BD persistente.
- AWS/GCP/Azure: free tier engaña, sale más caro y más complejo.
- DigitalOcean: similar a Hetzner pero el doble de precio en Europa.

### Hosting frontend: Vercel (free tier)

**Por qué:**
- Deploy automático desde GitHub con cada push.
- CDN global incluido.
- Free tier sobra para 100 GB de tráfico/mes (cubre los primeros 6 meses tranquilamente).

**Alternativas descartadas:**
- Cloudflare Pages: también válido, considerar si Vercel cambia condiciones.
- GitHub Pages: limitado, sin build server.

### CDN + DNS: Cloudflare (free)

**Por qué:**
- Cachea recursos estáticos = velocidad.
- Protección DDoS y bots gratis.
- DNS rápido y editable desde dashboard.
- Email forwarding gratis (contacto@dondeanime.com → tu Gmail) cuando lo necesites.

---

## Modelo de datos (borrador inicial)

```
┌─────────────────────────────────────────┐
│ anime                                   │
├─────────────────────────────────────────┤
│ id                BIGSERIAL PK          │
│ anilist_id        BIGINT UNIQUE         │
│ tmdb_id           BIGINT NULL           │
│ slug              VARCHAR UNIQUE        │
│ title_es          VARCHAR               │
│ title_en          VARCHAR               │
│ title_jp          VARCHAR               │
│ synopsis_es       TEXT                  │
│ release_date_jp   DATE                  │
│ release_date_es   DATE NULL  (manual)   │
│ release_date_us   DATE NULL             │
│ status            VARCHAR (airing, etc) │
│ format            VARCHAR (TV, movie)   │
│ episodes          INT NULL              │
│ image_cover       VARCHAR (URL)         │
│ image_banner      VARCHAR (URL)         │
│ created_at        TIMESTAMP             │
│ updated_at        TIMESTAMP             │
│ enriched_manual   BOOLEAN DEFAULT false │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ anime_platform                          │
├─────────────────────────────────────────┤
│ id                BIGSERIAL PK          │
│ anime_id          BIGINT FK             │
│ platform_name     VARCHAR (Netflix..)   │
│ country_code      CHAR(2) (ES, MX..)    │
│ language          VARCHAR (sub, dub)    │
│ link              VARCHAR (URL)         │
│ verified_manual   BOOLEAN DEFAULT false │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ subscriber                              │
├─────────────────────────────────────────┤
│ id                BIGSERIAL PK          │
│ email             VARCHAR UNIQUE        │
│ created_at        TIMESTAMP             │
│ confirmed         BOOLEAN               │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ alert                                   │
├─────────────────────────────────────────┤
│ id                BIGSERIAL PK          │
│ subscriber_id     BIGINT FK             │
│ anime_id          BIGINT FK             │
│ country_code      CHAR(2)               │
│ notified          BOOLEAN DEFAULT false │
└─────────────────────────────────────────┘
```

---

## Decisiones que se posponen conscientemente

| Decisión | Cuándo retomar |
|---|---|
| Tests unitarios completos | Cuando haya endpoints estables, no antes |
| CI/CD GitHub Actions | Cuando haya algo desplegado en VPS |
| Docker para backend | Solo si en mes 3 el deploy manual es coñazo |
| Sistema de comentarios | Mes 6 mínimo (modera distrae) |
| Cuentas de usuario | Mes 4-5, ligado a sistema de alertas premium |
| App móvil | El wrapper Capacitor ya existe en `mobile/`; la publicación en stores queda pendiente de decisión |
| Internacionalización fuera de español | Mes 12 mínimo |

---

## Riesgos técnicos identificados

1. **Astro requiere build completo si cambia el catálogo.** Mitigación: build incremental (Astro 4 lo soporta) o pasar las páginas dinámicas a SSR con caché agresiva.
2. **PostgreSQL en el mismo VPS que backend = single point of failure.** Aceptable a esta escala. Migrar a BD gestionada (Hetzner Managed o Supabase) en mes 6 si hay tracción.
3. **AniList puede capar requests si abusamos.** Mitigación: rate limit propio (1 req/s), cachear todo en BD, refresco programado.

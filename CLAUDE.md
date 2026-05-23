# DondeAnime

> Archivo de contexto para Claude Code. Se carga automáticamente al ejecutar `claude` dentro de este repo. Mantener actualizado conforme avanza el proyecto.

---

## Qué es este proyecto

Web pública en español para descubrir **cuándo y dónde se estrena cada anime** en plataformas hispanoamericanas (Crunchyroll, Netflix, Prime Video, HBO Max, etc.). Estrategia: aggregator + SEO long-tail con miles de páginas indexables. Monetización vía afiliados (Crunchyroll, Amazon), AdSense y futuro Premium.

- URL final: https://dondeanime.com
- Repo: https://github.com/diegoalegil/dondeanime (privado)
- Estado: desarrollo activo. Día 4 cerrado (endpoint `GET /api/anime` funcional, sync de AniList preparado: entidad ampliada + DTOs + cliente HTTP + config compartida).

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
| Frontend | Astro | 4 (pendiente, mes 2) |
| Deploy backend | Hetzner Cloud VPS CX22 | mes 2 |
| Deploy frontend | Vercel free tier | mes 2 |
| CDN/DNS | Cloudflare free | mes 2 |

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
        │   ├── BackendApplication.java
        │   ├── config/
        │   │   └── HttpClientConfig.java
        │   └── anime/
        │       ├── Anime.java                # entidad JPA (19 campos)
        │       ├── AnimeController.java      # GET /api/anime
        │       ├── AnimeRepository.java      # JpaRepository<Anime, Long>
        │       └── anilist/                  # cliente + DTOs de AniList
        │           ├── AniListClient.java
        │           ├── AniListResponse.java
        │           ├── AniListData.java
        │           ├── AniListPage.java
        │           ├── AniListMedia.java
        │           ├── AniListTitle.java
        │           ├── AniListFuzzyDate.java
        │           └── AniListCoverImage.java
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
PUBLIC_API_URL=http://localhost:8080
PUBLIC_SITE_URL=https://dondeanime.com
CATALOG_REFRESH_HOURS=12
```

### application.properties (resumen)

```properties
spring.application.name=dondeanime-backend
server.port=8080

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
./mvnw spring-boot:run            # arranca el backend
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
- [x] `AniListClient` con `RestClient` apuntando a `https://graphql.anilist.co`
- [x] `HttpClientConfig` con `@Bean RestClient.Builder` reutilizable
- [ ] **PRÓXIMO:** `AnimeSyncService` + endpoint `POST /api/anime/sync` (sub-paso 4 de semana 2)
- [ ] TMDbClient + cruce AniList↔TMDb + providers (semana 3)
- [ ] Refresco programado (@Scheduled cada 12h) (semana 4)
- [ ] Frontend Astro 4 (mes 2)
- [ ] Deploy en Hetzner + Vercel + Cloudflare (mes 2)
- [ ] Enriquecimiento manual top 50 (mes 3)
- [ ] Sistema de alertas por email (mes 3)
- [ ] Monetización: AdSense + afiliados (mes 3)

---

## Próxima tarea concreta

**Crear `AnimeSyncService` + endpoint `POST /api/anime/sync`.** Sub-paso 4 de la semana 2.

### Estado de la semana 2

- [x] Entidad `Anime` ampliada (19 campos)
- [x] DTOs de AniList (records en `anime/anilist/`)
- [x] `AniListClient` listo y compilando
- [x] `HttpClientConfig` con `RestClient.Builder` compartido
- [ ] `AnimeSyncService` con lógica de mapeo + upsert + slug
- [ ] Endpoint `POST /api/anime/sync` que dispara el sync manual
- [ ] Probar end-to-end: dispara sync → 100 anime en BD → `GET /api/anime` los devuelve

### Plan concreto del sub-paso 4

1. **Ampliar `AnimeRepository`** con `Optional<Anime> findByAnilistId(Long anilistId)`. Spring Data lo implementa solo al parsear el nombre del método.
2. **Crear `AnimeSyncService`** en `com.dondeanime.backend.anime`:
   - Inyectar `AniListClient` y `AnimeRepository` por constructor.
   - Método público `int syncPopular(int count)` que:
     - Llama a `client.fetchPopular(count)` → recibe `List<AniListMedia>`.
     - Por cada `AniListMedia`: busca por `anilistId` en BD (`findByAnilistId`). Si existe, actualiza esa entidad; si no, crea una nueva.
     - Mapea cada campo del DTO al campo correspondiente de `Anime` (cuidado con nulls en `title`, `coverImage`, `startDate`, `endDate`).
     - Genera el `slug` a partir del título inglés (fallback romaji): minúsculas, sin acentos, espacios→guiones, sin chars especiales.
     - `setSyncedAt(Instant.now())`.
     - `repository.save(anime)`.
     - Devuelve el número de animes procesados.
3. **Añadir endpoint `POST /sync`** en `AnimeController`:
   - Inyectar `AnimeSyncService` por constructor (junto al `AnimeRepository` que ya tiene).
   - `@PostMapping("/sync")` que llama a `service.syncPopular(100)` y devuelve `Map.of("synced", n)`.
4. **Probar**:
   - `curl -X POST http://localhost:8080/api/anime/sync` → debería tardar 1-2s y devolver `{"synced": 100}`.
   - `curl http://localhost:8080/api/anime | jq 'length'` → debería decir 100.
   - `docker exec dondeanime_postgres psql -U dondeanime_user -d dondeanime -c "SELECT count(*) FROM anime;"` → 100.
5. **Commit**: `git commit -m "Día 5: sync de AniList con 100 anime populares"`.

### Detalles a tener en cuenta

- **Nulls de AniList**: `media.title()` puede tener `romaji` o `english` en null, `media.startDate()` puede tener año pero no mes ni día. El mapeo tiene que ser defensivo (`if (media.title() != null) anime.setTitleRomaji(media.title().romaji())`).
- **Slug duplicado**: si dos anime tienen el mismo título normalizado, el segundo `save` violaría el UNIQUE. Estrategia simple inicial: añadir el `anilistId` al final del slug si choca. Más adelante mejoramos.
- **Description con HTML**: la query GraphQL pide `description(asHtml: false)`, así llega como markdown sin HTML. Aún así puede tener algún `<br>` residual, pero por ahora se guarda tal cual.

---

## Decisiones tomadas (no cambiar sin justificar)

### Stack
- **Spring Boot 4** porque start.spring.io dio esa versión como estable por defecto en mayo 2026.
- **PostgreSQL 16** (no 17) porque la imagen alpine es ligera y madura; el VPS de producción usará la misma.
- **Astro 4** (no Next.js, no React puro) porque genera HTML estático puro → Google indexa al 100% sin esperar JS. La batalla SEO se gana o se pierde aquí.
- **Hetzner CX22** elegido por relación precio/recursos en EU. Datacenter europeo = baja latencia para España y LatAm vía Cloudflare.

### Configuración Hibernate
- **Dialect explícito** (`org.hibernate.dialect.PostgreSQLDialect`) aunque Hibernate suelte WARN HHH90000025 diciendo que no hace falta. SIN él, la app explota si Hikari no consigue leer metadata al primer intento. El WARN se silencia con `logging.level.org.hibernate.orm.deprecation=ERROR`.
- **`initialization-fail-timeout=-1`** para que el backend no muera si Postgres tarda en estar `healthy`.
- **`open-in-view=false`** por buena práctica (evita sesiones JPA abiertas durante render).

### Stack HTTP
- **`RestClient`** (no `WebClient` ni `RestTemplate`) porque es el cliente síncrono recomendado desde Spring 6.1. API fluent, suficiente para llamadas REST/GraphQL no-reactivas.
- **Bean `RestClient.Builder` centralizado** en `config/HttpClientConfig.java`. Spring Boot 4 con `starter-webmvc` **no lo autoconfigura** (en 3.x con `starter-web` sí). Lo declaramos a mano para que `AniListClient`, futuro `TmdbClient`, etc. lo inyecten y especialicen con su propia `baseUrl`. Beneficio: cuando queramos timeouts globales o interceptors de logging, se añaden en un solo sitio.

### Modelado de datos
- **Records de Java 21** para DTOs externos (AniList): inmutables, concisos, Jackson los parsea sin config.
- **Un record por archivo**, todos `public`: convención Java, y necesario para usarlos desde paquetes hermanos (el service que está en `anime/` necesita `AniListMedia` que vive en `anime/anilist/`).
- **Fechas como `Integer` separados** (`startYear`, `startMonth`, `startDay`) y NO `LocalDate`, porque AniList puede devolver año sin mes ni día. `LocalDate` exige los tres.
- **Sin `enum` Java** para `format`/`status` (Strings simples). Si AniList añade un valor nuevo, `String` lo tolera; un enum petaría al deserializar.
- **`@Column(columnDefinition = "TEXT")`** para `description` (descripciones largas no caben en `VARCHAR(255)` por defecto).
- **`Instant`** para `syncedAt` (timestamp técnico UTC). Hibernate lo mapea a `timestamp with time zone` en Postgres.

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

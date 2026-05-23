# APIs externas usadas por DondeAnime

> Documentación práctica de las APIs que usamos. Incluye autenticación, ejemplos de queries reales y ejemplos de respuesta. Sirve para no tener que volver a leer la doc oficial cada vez.

---

## 1. AniList API (primaria)

### Resumen rápido
- **Tipo:** GraphQL
- **URL base:** `https://graphql.anilist.co`
- **Autenticación:** Ninguna para queries públicas
- **Rate limit:** 90 requests/minuto (sin auth), 200/min (con auth)
- **Documentación oficial:** https://docs.anilist.co/

### Por qué la usamos
Es la fuente más completa y actualizada de anime existente. Comunidad activa, datos limpios, API gratuita y bien diseñada (GraphQL permite pedir solo los campos que necesitamos).

### Cosas importantes que saber
1. GraphQL = una sola URL, las queries se envían como POST con un body JSON que contiene la query.
2. Si pides demasiados campos, la respuesta se hace lenta. Pide solo lo que vas a guardar.
3. El campo `startDate` puede tener año/mes/día como null si no se ha anunciado todavía.
4. Las imágenes (`coverImage`) vienen en varias resoluciones: `large`, `medium`, `extraLarge`.

### Ejemplo de query: obtener los 10 anime más populares de la temporada actual

```graphql
query {
  Page(page: 1, perPage: 10) {
    pageInfo {
      total
      currentPage
      lastPage
      hasNextPage
    }
    media(season: SPRING, seasonYear: 2026, type: ANIME, sort: POPULARITY_DESC) {
      id
      title {
        romaji
        english
        native
      }
      description(asHtml: false)
      startDate {
        year
        month
        day
      }
      episodes
      duration
      format
      status
      genres
      averageScore
      coverImage {
        large
        extraLarge
      }
      bannerImage
      externalLinks {
        site
        url
      }
      studios(isMain: true) {
        nodes {
          name
        }
      }
    }
  }
}
```

### Cómo enviarla (curl real)

```bash
curl -X POST https://graphql.anilist.co \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{"query":"query { Page(page: 1, perPage: 5) { media(season: SPRING, seasonYear: 2026, type: ANIME, sort: POPULARITY_DESC) { id title { romaji english } startDate { year month day } } } }"}'
```

### Ejemplo de respuesta (recortada)

```json
{
  "data": {
    "Page": {
      "media": [
        {
          "id": 162804,
          "title": {
            "romaji": "Sousou no Frieren",
            "english": "Frieren: Beyond Journey's End"
          },
          "startDate": {
            "year": 2026,
            "month": 4,
            "day": 5
          }
        }
      ]
    }
  }
}
```

### Campos que vamos a guardar de cada anime

| Campo AniList | Campo BD | Notas |
|---|---|---|
| `id` | `anilist_id` | Clave única externa |
| `title.romaji` | `title_jp` | Título original romanizado |
| `title.english` | `title_en` | Para SEO en mercados anglo |
| `description(asHtml: false)` | `synopsis_es` | Traducción manual o automática |
| `startDate` | `release_date_jp` | Fecha original Japón |
| `format` | `format` | TV, MOVIE, OVA, etc. |
| `episodes` | `episodes` | Total de episodios |
| `coverImage.extraLarge` | `image_cover` | URL del póster |
| `bannerImage` | `image_banner` | URL del banner horizontal |
| `externalLinks` | (procesar) | Links a Crunchyroll, Netflix, etc. |

---

## 2. TMDb API (complementaria)

### Resumen rápido
- **Tipo:** REST
- **URL base:** `https://api.themoviedb.org/3`
- **Autenticación:** API key (header `Authorization: Bearer <token>` o query param `api_key`)
- **Rate limit:** 50 requests/segundo (suficientemente generoso)
- **Documentación oficial:** https://developer.themoviedb.org/

### Por qué la usamos
Información de **dónde ver** cada título por país (Netflix, Prime Video, Crunchyroll, etc.). AniList tiene `externalLinks` pero TMDb tiene un endpoint específico (`/watch/providers`) que es mucho más completo y se actualiza más a menudo.

### Cómo obtener la clave
1. Crear cuenta en https://www.themoviedb.org/signup
2. Ir a Settings → API
3. Solicitar clave de tipo "Developer" (instantánea, sin justificación elaborada)
4. Guardar el "API Read Access Token" (v4 auth) — es el largo que empieza por `eyJ...`

### Endpoint clave: providers de streaming por título y país

```bash
GET https://api.themoviedb.org/3/tv/{tv_id}/watch/providers
```

### Ejemplo con curl

```bash
curl -X GET \
  'https://api.themoviedb.org/3/tv/95479/watch/providers' \
  -H 'Authorization: Bearer TU_TOKEN_AQUI' \
  -H 'accept: application/json'
```

### Ejemplo de respuesta (recortada)

```json
{
  "id": 95479,
  "results": {
    "ES": {
      "link": "https://www.themoviedb.org/tv/95479/watch?locale=ES",
      "flatrate": [
        {
          "logo_path": "/8Gt1iClBlzTeQs8WQm8UrCoIxnQ.jpg",
          "provider_id": 283,
          "provider_name": "Crunchyroll",
          "display_priority": 4
        }
      ]
    },
    "MX": {
      "flatrate": [
        {
          "provider_id": 8,
          "provider_name": "Netflix"
        }
      ]
    }
  }
}
```

### Países que nos interesan (filtrar en backend)

`ES`, `MX`, `AR`, `CO`, `CL`, `PE`, `VE`, `UY`, `EC`, `BO`, `PY`, `CR`, `PA`, `DO`, `GT`, `HN`, `NI`, `SV`, `US` (hispanohablantes en USA).

### El cruce AniList ↔ TMDb es manual al principio

TMDb usa sus propios IDs, no los de AniList. Para cruzar:
1. Buscar el título en TMDb por nombre: `GET /search/tv?query=...`
2. Filtrar por `original_language: ja` y `genre_ids: [16]` (animation)
3. Guardar el `tmdb_id` en nuestra tabla `anime`

Para los 500 títulos top hacer este cruce a mano (o semi-automático con script + revisión). Para el resto, dejar `tmdb_id` en null y mostrar solo info de AniList.

---

## 3. Estrategia de cacheo y refresco

| Tipo de dato | Frecuencia de refresco | Justificación |
|---|---|---|
| Catálogo nuevo (anime que aún no han salido) | Cada 12 horas | Las fechas cambian rara vez pero hay que captar nuevos anuncios |
| Anime emitiéndose ahora | Cada 6 horas | Próximo episodio, plataformas pueden cambiar |
| Anime ya terminados | Cada 30 días | Datos prácticamente estáticos |
| Providers TMDb (top 500) | Cada 7 días | Las plataformas añaden/quitan títulos con esta frecuencia aproximadamente |

Implementar con `@Scheduled` de Spring Boot.

---

## 4. Errores comunes y cómo manejarlos

| Error | Causa | Solución |
|---|---|---|
| AniList 429 Too Many Requests | Pasaste el rate limit | Implementar exponential backoff, máximo 1 req/seg |
| TMDb 401 Unauthorized | Token mal o expirado | Verificar `.env`, regenerar token en panel |
| AniList responde con `data: null` | Query GraphQL mal formada | Probar en https://anilist.co/graphiql |
| TMDb `results: {}` en watch/providers | Título no tiene info de providers ese país | Mostrar "Pendiente de confirmar plataforma" |
| Caracteres raros en respuesta | Encoding | Asegurar UTF-8 en cabeceras HTTP y BD |

---

## 5. Recursos útiles

- **AniList GraphiQL explorer:** https://anilist.co/graphiql (probar queries antes de meterlas en código)
- **TMDb test endpoint:** https://developer.themoviedb.org/reference/intro/getting-started
- **Lista de providers TMDb:** https://api.themoviedb.org/3/watch/providers/tv (devuelve todos los providers que TMDb conoce)

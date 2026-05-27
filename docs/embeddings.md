# Embeddings

## Decision inicial

Sprint 21 usa almacenamiento JSON para los vectores en PostgreSQL:

```text
anime_embedding.embedding = JSON float[]
```

No se usa `pgvector` de momento.

## Por que no pgvector ahora

Produccion corre `postgres:16-alpine` en Docker. Esa imagen no trae la
extension `pgvector` lista para activar. Meterla ahora implicaria cambiar la
imagen de Postgres o compilar extensiones en produccion, justo en una fase de
limpieza con Flyway y despliegue pendiente.

Para el catalogo actual, reranking en memoria es suficiente y evita tocar la
operacion de base de datos. Cuando el catalogo crezca de forma clara, se puede
migrar a `pgvector` con una PR dedicada y plan de deploy propio.

## Tabla

`anime_embedding` guarda un vector por anime y modelo:

- `anime_id`: FK a `anime(id)`.
- `model`: nombre del modelo de embeddings.
- `content_hash`: SHA-256 del documento usado para generar el vector.
- `embedding`: array JSON de numeros.
- `updated_at`: fecha de escritura.

La clave unica es `(anime_id, model)`. Si cambia el documento o el modelo, el
rebuild actualiza la fila.

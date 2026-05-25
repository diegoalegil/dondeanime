# Politica de versionado de la API publica

La API publica de DondeAnime vive bajo `/api/v1/*`.

## Version activa

- `v1` es la version estable actual.
- La documentacion y los ejemplos nuevos deben usar siempre `/api/v1`.
- Las rutas historicas `/api/anime`, `/api/providers`, `/api/genres`, `/api/seasons` y `/api/sitemap` redirigen con `301` a su equivalente versionado.

## Compatibilidad hacia atras

Dentro de una version estable no se eliminan campos, no se renombran campos y no se cambia el significado de un campo existente.

Cambios permitidos sin nueva version:

- Anadir campos opcionales en respuestas JSON.
- Anadir filtros o parametros opcionales.
- Anadir endpoints nuevos.
- Corregir errores cuando la respuesta anterior era claramente incorrecta.

Cambios que requieren una nueva version mayor:

- Eliminar o renombrar campos.
- Cambiar tipos de datos publicados.
- Cambiar codigos HTTP esperados por clientes existentes.
- Convertir parametros opcionales en obligatorios.
- Cambiar reglas de autenticacion o cuota de forma incompatible.

## Deprecacion

Una ruta publica obsoleta debe mantener compatibilidad durante 6 meses.

Durante ese periodo devuelve:

- `Deprecation: true`
- `Sunset: <fecha RFC 1123>`
- `Link: </docs/api-versioning.md>; rel="deprecation"; type="text/markdown"`

Las rutas sin version actuales tienen fecha de retirada prevista el 25 de noviembre de 2026.

## Politica de errores

Los errores publicos deben conservar una estructura estable dentro de la version activa. Si un endpoint necesita un formato de error distinto, se introduce en una version nueva o en un endpoint nuevo.

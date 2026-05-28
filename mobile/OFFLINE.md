# Offline movil

Sprint 23.4 prepara el wrapper movil para abrir fichas recientes sin red.

## Politica de cache

- El service worker solo cachea navegaciones publicas de ficha:
  `/anime/{slug}` y `/en/anime/{slug}`.
- Se guardan como maximo 12 fichas. Al visitar una ficha ya cacheada, vuelve
  al principio de la lista.
- Si una ruta no esta cacheada, la app cae en `/offline`, la misma pantalla que
  la PWA web.
- No se cachean rutas `/admin`, `/api`, busqueda, tokens, emails, passwords,
  device tokens ni credenciales de terceros.

## Cache nativa

`mobile/src/offlineCache.mjs` guarda una version sanitizada de las ultimas fichas
visitadas en `localStorage`. El modulo acepta un `storage` inyectado para poder
moverlo mas tarde a Capacitor Preferences sin cambiar la politica.

Campos guardados:

- `slug`, titulos publicos, descripcion recortada, formato, estado y episodios.
- Imagenes `http/https`, generos, estudios y trailers publicos.
- `cachedAt` para ordenar por visita reciente.

Campos descartados:

- IDs internos, `tmdbId`, emails, passwords, tokens, secretos y cualquier campo
  fuera de la lista publica.

## Validacion

```bash
cd mobile
npm run lint
npm test
```

El test cubre sanitizacion, orden LRU, limite de fichas, storage corrupto y rutas
cacheables.

# DondeAnime mobile

Wrapper movil de DondeAnime con Capacitor.

## Comandos

```bash
cd mobile
npm ci
npm run lint
npm test
npm run build
```

`npm run build` valida el scaffold. No genera binarios nativos ni requiere SDK de
Android/iOS en este PR.

## Offline

`OFFLINE.md` documenta la cache de ultimas fichas visitadas. La politica evita
guardar rutas admin, llamadas API y tokens; solo persiste datos publicos
sanitizados de fichas de anime.

## Flujo futuro

1. Construir el frontend desde `frontend/`.
2. Sincronizar Capacitor cuando existan proyectos nativos.
3. Generar builds internos siguiendo `mobile/RELEASE.md` cuando Sprint 23.5 lo
   documente.

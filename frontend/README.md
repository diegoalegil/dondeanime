# Frontend DondeAnime

Frontend Astro estático de DondeAnime. Genera las páginas en build time leyendo la API pública.

## Comandos

```sh
npm install
npm run dev
npm run build
npm run preview
npm run test:e2e
```

## Variables necesarias

```env
PUBLIC_API_URL=https://api.dondeanime.com
PUBLIC_SITE_URL=https://dondeanime.com
```

`playwright.config.ts` usa esos valores por defecto si no están definidos, para que `npm run test:e2e` funcione en local y en CI sin depender de un `.env` dentro de `frontend/`.

## Tests E2E

Los tests viven en `e2e/` y arrancan solos un build estático con preview:

```sh
npm run test:e2e
```

Cubren:

- Home con catálogo estático y navegación a ficha de anime.
- Rutas estáticas de país, plataforma, género y temporada.
- `search-index.json`, `robots.txt` y `sitemap-index.xml`.

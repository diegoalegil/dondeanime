export const DEFAULT_ANIME_IMAGE_WIDTHS = [160, 240, 320, 480, 640];
export const DEFAULT_ANIME_IMAGE_SIZES =
  '(min-width: 1280px) 16vw, (min-width: 768px) 25vw, (min-width: 640px) 33vw, 50vw';
export const DEFAULT_ANIME_IMAGE_QUALITY = 75;

// `sizes` de la portada del hero de la ficha. Constante compartida por
// AnimeHero (el <img>) y el preload del <head> de la ficha (PERF-3): si el
// preload no usa EXACTAMENTE el mismo sizes/srcset que el <img>, el navegador
// descarga dos candidatos distintos (doble descarga). Aquí está el único sitio.
export const HERO_COVER_SIZES = '(min-width: 768px) 14rem, 10rem';

/**
 * Variantes nativas que el CDN de AniList ya sirve para una misma portada,
 * cambiando solo el segmento /cover/<size>/ de la ruta. Sus anchos son
 * aproximados (AniList no los publica exactos), suficientes como descriptor
 * `w` para que el navegador elija; el byte real lo decide el CDN.
 *
 * Servimos estas variantes directamente (JPG ya optimizado por AniList) en
 * lugar de pasar por el optimizador de Vercel: coste 0 y sin tope de cupo,
 * que con 12.853 páginas y tráfico de crawlers se agotaba siempre.
 */
// AniList NO genera 'extraLarge' (700w) para la mayoría de portadas: ese
// segmento devuelve 404 SIEMPRE, y en pantallas Retina el navegador elegía ese
// candidato del srcset → 404 → toda la rejilla caía al placeholder. Servimos
// solo las variantes que existen de verdad: 'medium' (230w, en el 100% de las
// portadas — es la que guarda el backend) y 'large' (460w, ~96%; el 4% sin
// large cae con gracia a medium vía window.__coverError). Nada de extraLarge.
const ANILIST_COVER_VARIANTS = [
  { segment: 'medium', width: 230 },
  { segment: 'large', width: 460 },
] as const;

const ANILIST_COVER_SEGMENT = /\/cover\/(medium|large|extraLarge)\//;

/**
 * Proxy opcional para las portadas. Vacío (por defecto) = se sirven directas
 * de AniList. Su CDN estrangula cuando se piden muchas a la vez (la home pide
 * ~90), así que para que carguen TODAS conviene proxearlas/cachearlas. Valores:
 *   - 'wsrv'        → proxy gratuito wsrv.nl (images.weserv.nl).
 *   - 'https://...' → un proxy propio (p.ej. un Cloudflare Worker) que acepte
 *                     ?url=<url> y devuelva la imagen cacheada.
 * Ver infra/cloudflare-image-worker.js y DEPLOY.md ("Proxy de portadas").
 */
const IMAGE_PROXY = (import.meta.env.PUBLIC_IMAGE_PROXY ?? '').trim();

function proxiedUrl(anilistUrl: string, width: number): string {
  if (!IMAGE_PROXY) {
    return anilistUrl;
  }
  const base = IMAGE_PROXY === 'wsrv' ? 'https://wsrv.nl/' : IMAGE_PROXY;
  let url: URL;
  try {
    url = new URL(base);
  } catch {
    return anilistUrl;
  }
  url.searchParams.set('url', anilistUrl);
  if (IMAGE_PROXY === 'wsrv') {
    url.searchParams.set('w', String(width));
    url.searchParams.set('output', 'webp');
    url.searchParams.set('q', String(DEFAULT_ANIME_IMAGE_QUALITY));
  }
  return url.toString();
}

export function canOptimizeAnimeImage(src: string) {
  return isAniListCover(src);
}

export function normalizeImageWidths(widths: number[]) {
  return [...new Set(widths)].sort((a, b) => a - b);
}

export function buildAnimeImageUrl(src: string, width: number, _quality = DEFAULT_ANIME_IMAGE_QUALITY) {
  if (!isAniListCover(src)) {
    return src;
  }

  // Elige la variante nativa más pequeña que cubra el ancho pedido.
  const variant =
    ANILIST_COVER_VARIANTS.find((candidate) => candidate.width >= width) ??
    ANILIST_COVER_VARIANTS[ANILIST_COVER_VARIANTS.length - 1];
  return withCoverVariant(src, variant.segment, variant.width);
}

export function buildAnimeImageSrcset(
  src: string,
  _widths = DEFAULT_ANIME_IMAGE_WIDTHS,
  _quality = DEFAULT_ANIME_IMAGE_QUALITY,
) {
  if (!isAniListCover(src)) {
    return undefined;
  }

  return ANILIST_COVER_VARIANTS.map(
    (variant) => `${withCoverVariant(src, variant.segment, variant.width)} ${variant.width}w`,
  ).join(', ');
}

function withCoverVariant(src: string, segment: string, width: number) {
  const anilist = src.replace(ANILIST_COVER_SEGMENT, `/cover/${segment}/`);
  return proxiedUrl(anilist, width);
}

function isAniListCover(src: string) {
  try {
    const url = new URL(src);
    return (
      url.protocol === 'https:' &&
      url.hostname === 's4.anilist.co' &&
      ANILIST_COVER_SEGMENT.test(url.pathname)
    );
  } catch {
    return false;
  }
}

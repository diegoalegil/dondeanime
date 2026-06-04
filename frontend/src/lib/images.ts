export const DEFAULT_ANIME_IMAGE_WIDTHS = [160, 240, 320, 480, 640];
export const DEFAULT_ANIME_IMAGE_SIZES =
  '(min-width: 1280px) 16vw, (min-width: 768px) 25vw, (min-width: 640px) 33vw, 50vw';
export const DEFAULT_ANIME_IMAGE_QUALITY = 75;

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
const ANILIST_COVER_VARIANTS = [
  { segment: 'medium', width: 230 },
  { segment: 'large', width: 460 },
  { segment: 'extraLarge', width: 700 },
] as const;

const ANILIST_COVER_SEGMENT = /\/cover\/(medium|large|extraLarge)\//;

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
  return withCoverVariant(src, variant.segment);
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
    (variant) => `${withCoverVariant(src, variant.segment)} ${variant.width}w`,
  ).join(', ');
}

function withCoverVariant(src: string, segment: string) {
  return src.replace(ANILIST_COVER_SEGMENT, `/cover/${segment}/`);
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

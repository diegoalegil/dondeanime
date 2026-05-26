export const DEFAULT_ANIME_IMAGE_WIDTHS = [160, 240, 320, 480, 640];
export const DEFAULT_ANIME_IMAGE_SIZES =
  '(min-width: 1280px) 16vw, (min-width: 768px) 25vw, (min-width: 640px) 33vw, 50vw';
export const DEFAULT_ANIME_IMAGE_QUALITY = 75;

export function canOptimizeAnimeImage(src: string) {
  return import.meta.env.VERCEL === '1' && isAniListImage(src);
}

export function normalizeImageWidths(widths: number[]) {
  return [...new Set(widths)].sort((a, b) => a - b);
}

export function buildAnimeImageUrl(src: string, width: number, quality = DEFAULT_ANIME_IMAGE_QUALITY) {
  if (!canOptimizeAnimeImage(src)) {
    return src;
  }

  const params = new URLSearchParams({
    url: src,
    w: String(width),
    q: String(quality),
  });
  return `/_vercel/image?${params.toString()}`;
}

export function buildAnimeImageSrcset(
  src: string,
  widths = DEFAULT_ANIME_IMAGE_WIDTHS,
  quality = DEFAULT_ANIME_IMAGE_QUALITY,
) {
  if (!canOptimizeAnimeImage(src)) {
    return undefined;
  }

  return normalizeImageWidths(widths)
    .map((width) => `${buildAnimeImageUrl(src, width, quality)} ${width}w`)
    .join(', ');
}

function isAniListImage(src: string) {
  try {
    const url = new URL(src);
    return (
      url.protocol === 'https:' &&
      url.hostname === 's4.anilist.co' &&
      url.pathname.startsWith('/file/anilistcdn/media/anime/')
    );
  } catch {
    return false;
  }
}

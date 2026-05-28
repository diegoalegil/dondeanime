const STORAGE_KEY = 'dondeanime.mobile.offline.anime.v1';
const DEFAULT_LIMIT = 12;
const SLUG_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const ANIME_ROUTE_PATTERN = /^\/(?:en\/)?anime\/[a-z0-9]+(?:-[a-z0-9]+)*\/?$/;

export function createOfflineAnimeCache({
  storage = globalThis.localStorage,
  limit = DEFAULT_LIMIT,
  now = () => new Date().toISOString(),
} = {}) {
  if (!storage || typeof storage.getItem !== 'function' || typeof storage.setItem !== 'function') {
    throw new Error('storage debe implementar getItem y setItem');
  }
  if (!Number.isInteger(limit) || limit < 1 || limit > 50) {
    throw new Error('limit debe estar entre 1 y 50');
  }

  const readEntries = () => {
    try {
      const parsed = JSON.parse(storage.getItem(STORAGE_KEY) ?? '[]');
      return Array.isArray(parsed) ? parsed.filter(isCachedAnime) : [];
    } catch {
      return [];
    }
  };

  const writeEntries = (entries) => {
    storage.setItem(STORAGE_KEY, JSON.stringify(entries.slice(0, limit)));
  };

  return {
    saveVisitedAnime(anime) {
      const snapshot = sanitizeAnimeForOffline(anime);
      const cachedAt = now();
      const entries = readEntries().filter((entry) => entry.slug !== snapshot.slug);
      const next = [{ ...snapshot, cachedAt }, ...entries].slice(0, limit);
      writeEntries(next);
      return next[0];
    },

    listVisitedAnime() {
      return readEntries();
    },

    getVisitedAnime(slug) {
      if (!SLUG_PATTERN.test(slug ?? '')) return null;
      return readEntries().find((entry) => entry.slug === slug) ?? null;
    },

    clear() {
      if (typeof storage.removeItem === 'function') {
        storage.removeItem(STORAGE_KEY);
        return;
      }
      storage.setItem(STORAGE_KEY, '[]');
    },
  };
}

export function sanitizeAnimeForOffline(anime) {
  if (!anime || typeof anime !== 'object') {
    throw new Error('anime debe ser un objeto');
  }

  const slug = cleanSlug(anime.slug);
  if (!slug) {
    throw new Error('anime.slug no es valido');
  }

  const title = firstString(
    anime.titleEnglish,
    anime.titleRomaji,
    anime.titleNative,
    anime.title,
    slug,
  );

  return removeEmpty({
    slug,
    title,
    titleEnglish: cleanString(anime.titleEnglish, 160),
    titleRomaji: cleanString(anime.titleRomaji, 160),
    titleNative: cleanString(anime.titleNative, 160),
    description: cleanString(anime.description, 2200),
    format: cleanString(anime.format, 40),
    status: cleanString(anime.status, 40),
    episodes: cleanInteger(anime.episodes),
    startYear: cleanInteger(anime.startYear),
    season: cleanString(anime.season, 40),
    seasonYear: cleanInteger(anime.seasonYear),
    coverImage: cleanHttpUrl(anime.coverImage),
    bannerImage: cleanHttpUrl(anime.bannerImage),
    averageScore: cleanInteger(anime.averageScore),
    popularity: cleanInteger(anime.popularity),
    genres: cleanStringArray(anime.genres, 16),
    studios: cleanNamedArray(anime.studios, 12),
    trailers: cleanTrailerArray(anime.trailers, 3),
  });
}

export function isOfflineAnimeRoute(value) {
  if (typeof value !== 'string' || value.trim() === '') {
    return false;
  }

  try {
    const url = value.startsWith('/') ? new URL(value, 'https://dondeanime.com') : new URL(value);
    return ANIME_ROUTE_PATTERN.test(url.pathname);
  } catch {
    return false;
  }
}

function isCachedAnime(entry) {
  return entry && typeof entry === 'object' && SLUG_PATTERN.test(entry.slug ?? '');
}

function cleanSlug(value) {
  return typeof value === 'string' && SLUG_PATTERN.test(value) ? value : null;
}

function firstString(...values) {
  return values.find((value) => typeof value === 'string' && value.trim() !== '')?.trim() ?? null;
}

function cleanString(value, maxLength) {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  if (trimmed === '') return null;
  return trimmed.slice(0, maxLength);
}

function cleanInteger(value) {
  if (!Number.isFinite(value)) return null;
  return Math.trunc(value);
}

function cleanHttpUrl(value) {
  if (typeof value !== 'string') return null;

  try {
    const url = new URL(value);
    return url.protocol === 'https:' || url.protocol === 'http:' ? url.toString() : null;
  } catch {
    return null;
  }
}

function cleanStringArray(value, limit) {
  if (!Array.isArray(value)) return [];
  return [...new Set(value
    .map((item) => cleanString(item, 80))
    .filter(Boolean))]
    .slice(0, limit);
}

function cleanNamedArray(value, limit) {
  if (!Array.isArray(value)) return [];
  return value
    .map((item) => {
      if (typeof item === 'string') return cleanString(item, 100);
      if (!item || typeof item !== 'object') return null;
      return cleanString(item.name, 100);
    })
    .filter(Boolean)
    .slice(0, limit);
}

function cleanTrailerArray(value, limit) {
  if (!Array.isArray(value)) return [];
  return value
    .map((item) => {
      if (!item || typeof item !== 'object') return null;
      const trailer = removeEmpty({
        site: cleanString(item.site, 40),
        thumbnail: cleanHttpUrl(item.thumbnail),
        url: cleanHttpUrl(item.url),
      });
      return trailer.url || trailer.thumbnail ? trailer : null;
    })
    .filter(Boolean)
    .slice(0, limit);
}

function removeEmpty(value) {
  return Object.fromEntries(
    Object.entries(value).filter(([, entry]) => {
      if (entry === null || entry === undefined) return false;
      if (Array.isArray(entry) && entry.length === 0) return false;
      return true;
    }),
  );
}

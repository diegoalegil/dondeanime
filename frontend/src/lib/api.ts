import { formatStudioName, studioSlug } from './programmaticSeo';

const API_URL = import.meta.env.PUBLIC_DATA_API_URL ?? import.meta.env.PUBLIC_API_URL;
const JSON_CACHE = new Map<string, Promise<unknown>>();
const FETCH_ATTEMPTS = 3;
const FETCH_TIMEOUT_MS = 15_000;
const DETAIL_BUILD_CONCURRENCY = 12;
let BUILDABLE_ANIME_DETAILS: Promise<AnimeBuildDetail[]> | null = null;

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export interface AnimeSummary {
  anilistId: number;
  slug: string;
  titleEnglish: string;
  titleRomaji: string;
  format: string;
  status: string;
  episodes: number | null;
  episodeDuration: number | null;
  studio: string | null;
  year: number | null;
  averageScore: number | null;
  popularity: number | null;
  coverImage: string;
  genres: string[];
  season: string | null;
  seasonYear: number | null;
}

export interface ChatRecommendation {
  anime: AnimeSummary;
  canonicalUrl: string;
  explanation: string;
}

export interface ChatSearchResponse {
  answer: string;
  recommendations: ChatRecommendation[];
}

export interface TraktWatchedResponse {
  slugs: string[];
}

export interface BeginnerAnime {
  anime: AnimeSummary;
  beginnerRecommendation: string | null;
}

export interface UpcomingAnime extends AnimeSummary {
  startYear: number;
  startMonth: number;
  startDay: number;
}

export interface Studio {
  slug: string;
  name: string;
  animationStudio: boolean;
}

export interface StudioSummary extends Studio {
  animeCount: number;
}

export interface WatchProvider {
  countryCode: string;
  providerSlug: string;
  providerName: string;
  providerType: string;
  logoUrl: string;
  affiliateUrl: string | null;
}

export interface AnimeCharacter {
  anilistId: number;
  name: string;
  image: string | null;
  role: string;
}

export interface AnimeDetail {
  anime: {
    anilistId: number;
    slug: string;
    titleEnglish: string;
    titleRomaji: string;
    trailerYoutubeId?: string | null;
    description: string | null;
    descriptionTranslationPending: boolean;
    format: string;
    status: string;
    episodes: number | null;
    averageScore: number | null;
    popularity: number | null;
    coverImage: string;
    bannerImage: string | null;
    startYear: number | null;
    startMonth: number | null;
    startDay: number | null;
    endYear: number | null;
    endMonth: number | null;
    endDay: number | null;
    genres: string[];
    studios?: Studio[];
    season: string | null;
    seasonYear: number | null;
    characters?: AnimeCharacter[];
  };
  watchProvidersByCountry: Record<string, WatchProvider[]>;
}

export interface AnimeBuildDetail {
  summary: AnimeSummary;
  detail: AnimeDetail;
}

export interface ProviderSummary {
  slug: string;
  providerName: string;
  logoUrl: string;
  animeCount: number;
}

export interface CuratedListSummary {
  slug: string;
  title: string;
  description: string | null;
  owner: string | null;
  visibility: string;
  status: string;
  premiumOnly: boolean;
  itemCount: number;
}

export interface CuratedListItem {
  animeSlug: string;
  position: number;
  note: string | null;
  anime: AnimeSummary | null;
}

export interface CuratedListDetail extends CuratedListSummary {
  premiumPreview: boolean;
  premiumCtaUrl: string | null;
  items: CuratedListItem[];
  schema: unknown;
}

const FIXTURE_DIR = import.meta.env.PUBLIC_FIXTURE_DIR;

/**
 * Modo fixture (CI): con PUBLIC_FIXTURE_DIR definido, cada GET del build se
 * resuelve contra un JSON en disco en vez de la API real. El mapeo ruta ->
 * fichero replica el de scripts/generate-fixtures.mjs ([?&=] -> '_').
 * Fichero ausente = 404, así aplican los mismos fallbacks que con la API.
 */
async function fixtureResponse(path: string): Promise<Response> {
  const [{ readFile }, { resolve }] = await Promise.all([
    import('node:fs/promises'),
    import('node:path'),
  ]);
  const fileRelative = `${path.replace(/[?&=]/g, '_')}.json`;
  const filePath = resolve(FIXTURE_DIR, `.${fileRelative}`);
  try {
    const body = await readFile(filePath, 'utf-8');
    return new Response(body, {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    });
  } catch {
    return new Response('null', { status: 404 });
  }
}

async function fetchWithRetry(path: string): Promise<Response> {
  if (FIXTURE_DIR) {
    return fixtureResponse(path);
  }

  let lastError: unknown;

  for (let attempt = 1; attempt <= FETCH_ATTEMPTS; attempt += 1) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), FETCH_TIMEOUT_MS);

    try {
      const res = await fetch(`${API_URL}${path}`, { signal: controller.signal });
      if (res.status < 500 || attempt === FETCH_ATTEMPTS) {
        return res;
      }
    } catch (error) {
      lastError = error;
      if (attempt === FETCH_ATTEMPTS) {
        throw error;
      }
    } finally {
      clearTimeout(timeoutId);
    }

    await sleep(400 * attempt);
  }

  throw lastError;
}

async function fetchJson<T>(path: string): Promise<T> {
  const cached = JSON_CACHE.get(path);
  if (cached) {
    return cached as Promise<T>;
  }

  const request = (async () => {
    const res = await fetchWithRetry(path);
    if (!res.ok) {
      throw new Error(`API ${path} failed: ${res.status} ${res.statusText}`);
    }
    return res.json() as Promise<T>;
  })();

  JSON_CACHE.set(path, request);

  try {
    return await request;
  } catch (error) {
    JSON_CACHE.delete(path);
    throw error;
  }
}

async function fetchJsonAllowing404<T>(path: string, fallback: () => Promise<T> | T): Promise<T> {
  try {
    const res = await fetchWithRetry(path);
    if (res.status === 404) {
      return fallback();
    }
    if (!res.ok) {
      throw new Error(`API ${path} failed: ${res.status} ${res.statusText}`);
    }
    return res.json() as Promise<T>;
  } catch {
    return fallback();
  }
}

/**
 * Como fetchJson pero NUNCA lanza: ante cualquier fallo (5xx, timeout, red,
 * 404) devuelve el fallback. Para datos NO críticos cuya ausencia no debe
 * tumbar el build estático (p.ej. noticias, decoración de la ficha). Un hipo
 * transitorio de la API —incluido un 502 mientras el backend reinicia— no
 * puede abortar la generación de 13k páginas.
 */
async function fetchJsonSafe<T>(path: string, fallback: T): Promise<T> {
  const cached = JSON_CACHE.get(path);
  if (cached) {
    try {
      return (await cached) as T;
    } catch {
      return fallback;
    }
  }

  const request = (async () => {
    const res = await fetchWithRetry(path);
    if (!res.ok) {
      throw new Error(`API ${path} failed: ${res.status} ${res.statusText}`);
    }
    return res.json() as Promise<T>;
  })();

  JSON_CACHE.set(path, request);

  try {
    return await request;
  } catch {
    JSON_CACHE.delete(path);
    return fallback;
  }
}

/**
 * Guard de build para datos ESTRUCTURALES (géneros, temporadas, providers):
 * con ~1000 anime en catálogo nunca pueden estar legítimamente vacíos. Si la
 * API responde vacío durante el build (degradada, 5xx convertido en fallback),
 * generar 0 páginas desindexaría miles de URLs en silencio: mejor abortar.
 * Las noticias NO usan este guard: vacías es un estado válido.
 */
export const requireNonEmpty = <T>(name: string, items: T[]): T[] => {
  if (items.length === 0) {
    throw new Error(
      `Build abortado: la API devolvió 0 ${name}. Con el catálogo poblado esto solo ocurre con la API degradada; generar 0 páginas desindexaría esas URLs.`,
    );
  }
  return items;
};

export const getAllAnime = () => fetchJson<AnimeSummary[]>('/api/anime');

export const getUpcomingAnime = async (days: number) => {
  return fetchJsonAllowing404(`/api/anime/upcoming?days=${days}`, () => [] as UpcomingAnime[]);
};

export const getAnimeBySlug = (slug: string) =>
  fetchJson<AnimeDetail>(`/api/anime/${slug}`);

export const getAnimeBySlugOrNull = (slug: string) =>
  fetchJsonAllowing404<AnimeDetail | null>(`/api/anime/${slug}`, () => null);

async function mapWithConcurrency<T, R>(
  items: T[],
  limit: number,
  mapper: (item: T, index: number) => Promise<R>,
): Promise<R[]> {
  const results = new Array<R>(items.length);
  let nextIndex = 0;

  const workers = Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (nextIndex < items.length) {
      const index = nextIndex;
      nextIndex += 1;
      results[index] = await mapper(items[index], index);
    }
  });

  await Promise.all(workers);
  return results;
}

export const getBuildableAnimeDetails = async (): Promise<AnimeBuildDetail[]> => {
  if (!BUILDABLE_ANIME_DETAILS) {
    BUILDABLE_ANIME_DETAILS = (async () => {
      const allAnime = await getAllAnime();
      const entries = await mapWithConcurrency(allAnime, DETAIL_BUILD_CONCURRENCY, async (summary) => {
        const detail = await getAnimeBySlugOrNull(summary.slug);
        return detail ? { summary, detail } : null;
      });
      return entries.filter((entry): entry is AnimeBuildDetail => entry !== null);
    })();
  }

  return BUILDABLE_ANIME_DETAILS;
};

export const getSimilarAnime = async (slug: string, watchedSlugs: string[] = []) => {
  const params = new URLSearchParams();
  watchedSlugs.forEach((watched) => params.append('watched', watched));
  const suffix = params.toString() ? `?${params.toString()}` : '';
  return fetchJsonAllowing404(`/api/anime/${slug}/similar${suffix}`, () => [] as AnimeSummary[]);
};

export const getProviders = () => fetchJson<ProviderSummary[]>('/api/providers');

export const getProvidersByCountry = (countryIso: string) =>
  fetchJsonSafe<ProviderSummary[]>(`/api/providers?country=${countryIso}`, []);

export const getAnimeByProvider = (providerSlug: string, countryIso: string) =>
  fetchJsonSafe<AnimeSummary[]>(`/api/providers/${providerSlug}/${countryIso}`, []);

export const getAnimeByCountry = async (countryIso: string) => {
  try {
    return await fetchJson<AnimeSummary[]>(`/api/providers/country/${countryIso}/anime`);
  } catch {
    const providers = await getProvidersByCountry(countryIso);
    const results = await Promise.allSettled(
      providers.map((provider) => getAnimeByProvider(provider.slug, countryIso)),
    );
    const animeBySlug = new Map<string, AnimeSummary>();

    for (const result of results) {
      if (result.status !== 'fulfilled') continue;
      for (const item of result.value) {
        animeBySlug.set(item.slug, item);
      }
    }

    return [...animeBySlug.values()].sort((a, b) => {
      const popularityA = a.popularity ?? 0;
      const popularityB = b.popularity ?? 0;
      if (popularityA !== popularityB) return popularityB - popularityA;
      return (a.titleEnglish || a.titleRomaji).localeCompare(b.titleEnglish || b.titleRomaji, 'es');
    });
  }
};

export const getCuratedLists = async () => {
  return fetchJsonAllowing404('/api/lists', () => [] as CuratedListSummary[]);
};

export const getCuratedListBySlug = (slug: string) =>
  fetchJson<CuratedListDetail>(`/api/lists/${slug}`);

export interface GenreSummary {
  name: string;
  slug: string;
  animeCount: number;
}

export interface SeasonSummary {
  year: number;
  season: string;
  animeCount: number;
}

export const getGenres = () => fetchJsonSafe<GenreSummary[]>('/api/genres', []);

export const getAnimeByGenre = (genreSlug: string) =>
  fetchJsonSafe<AnimeSummary[]>(`/api/genres/${genreSlug}`, []);

export const getBeginnerAnimeByGenre = async (genreSlug: string) => {
  return fetchJsonAllowing404(`/api/genres/${genreSlug}/beginner`, async () => {
    const anime = await getAnimeByGenre(genreSlug);
    return anime.slice(0, 10).map((item) => ({
      anime: item,
      beginnerRecommendation: null,
    })) satisfies BeginnerAnime[];
  });
};

export const getSeasons = () => fetchJsonSafe<SeasonSummary[]>('/api/seasons', []);

export const getAnimeBySeason = (year: number, season: string) =>
  fetchJsonSafe<AnimeSummary[]>(`/api/seasons/${year}/${season}`, []);

export const getAnimeByDuration = async (minutes: number) => {
  return fetchJsonAllowing404(`/api/anime/duration/${minutes}`, async () => {
    const allAnime = await getAllAnime();
    return allAnime.filter((anime) => anime.episodeDuration === minutes);
  });
};

export const getAnimeByMaxEpisodes = async (maxEpisodes: number) => {
  return fetchJsonAllowing404(`/api/anime/episodes/less-than/${maxEpisodes}`, async () => {
    const allAnime = await getAllAnime();
    return allAnime.filter((anime) => anime.episodes !== null && anime.episodes <= maxEpisodes);
  });
};

const summarizeStudiosFromAnime = (anime: AnimeSummary[]): StudioSummary[] => {
  const counts = new Map<string, { name: string; animeCount: number }>();

  anime.forEach((item) => {
    if (!item.studio) return;
    const slug = studioSlug(item.studio);
    if (!slug) return;
    const current = counts.get(slug);
    counts.set(slug, {
      name: current?.name ?? item.studio,
      animeCount: (current?.animeCount ?? 0) + 1,
    });
  });

  const summaries = Array.from(counts.entries())
    .map(([slug, value]) => ({ slug, animationStudio: false, ...value }))
    .sort((a, b) => b.animeCount - a.animeCount || a.name.localeCompare(b.name, 'es'));

  return summaries;
};

export const getStudios = async (): Promise<StudioSummary[]> => {
  const studios = await fetchJsonAllowing404<StudioSummary[]>('/api/studios', async () => {
    return summarizeStudiosFromAnime(await getAllAnime());
  });
  // Normalizamos el casing del nombre para mostrar; el slug se mantiene del backend.
  return studios.map((studio) => ({ ...studio, name: formatStudioName(studio.name) }));
};

export const getAnimeByStudio = async (slug: string) => {
  return fetchJsonAllowing404(`/api/studios/${slug}`, async () => {
    const allAnime = await getAllAnime();
    return allAnime
      .filter((anime) => anime.studio && studioSlug(anime.studio) === slug)
      .sort((a, b) => (b.popularity ?? 0) - (a.popularity ?? 0));
  });
};

export const getBestAnimeByStudio = async (slug: string) => {
  return fetchJsonAllowing404(`/api/studios/${slug}/best`, async () => {
    const allAnime = await getAllAnime();
    return allAnime
      .filter((anime) => anime.studio && studioSlug(anime.studio) === slug)
      .sort((a, b) => (b.popularity ?? 0) - (a.popularity ?? 0));
  });
};

export interface NewsSummary {
  slug: string;
  title: string;
  summary: string | null;
  imageUrl: string | null;
  sourceName: string;
  animeId: number | null;
  animeSlug: string | null;
  publishedAt: string;
}

export interface NewsDetail {
  slug: string;
  title: string;
  summary: string | null;
  body: string | null;
  imageUrl: string | null;
  sourceName: string;
  sourceUrl: string;
  animeId: number | null;
  metaTitle: string | null;
  metaDescription: string | null;
  publishedAt: string;
}

// Noticias = contenido NO crítico (la ficha y la home las muestran como
// decoración; el índice degrada a vacío). Usamos fetchJsonSafe para que NINGÚN
// fallo de la API —vacío, 404, 5xx, timeout, o un 502 mientras el backend
// reinicia— pueda tumbar el build estático de 13k páginas.
export const getNews = (limit = 30) =>
  fetchJsonSafe<NewsSummary[]>(`/api/news?limit=${limit}`, []);

// TODOS los slugs publicados, para getStaticPaths: /api/news capa a 100 y
// dejaría artículos antiguos indexados en 404 cuando el catálogo crezca.
// Fallback a getNews(100) mientras la API desplegada no exponga /slugs.
export const getNewsSlugs = async (): Promise<string[]> => {
  const slugs = await fetchJsonSafe<string[] | null>('/api/news/slugs', null);
  if (Array.isArray(slugs)) {
    return slugs;
  }
  return (await getNews(100)).map((item) => item.slug);
};

export const getNewsBySlug = (slug: string) =>
  fetchJsonSafe<NewsDetail | null>(`/api/news/${slug}`, null);

export const getAnimeNews = (slug: string) =>
  fetchJsonSafe<NewsSummary[]>(`/api/news/anime/${slug}`, []);

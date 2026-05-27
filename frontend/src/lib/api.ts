const API_URL = import.meta.env.PUBLIC_DATA_API_URL ?? import.meta.env.PUBLIC_API_URL;
const JSON_CACHE = new Map<string, Promise<unknown>>();
const FETCH_ATTEMPTS = 3;
const FETCH_TIMEOUT_MS = 15_000;

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export interface AnimeSummary {
  anilistId: number;
  slug: string;
  titleEnglish: string;
  titleRomaji: string;
  format: string;
  status: string;
  episodes: number | null;
  year: number | null;
  averageScore: number | null;
  popularity: number | null;
  coverImage: string;
  genres: string[];
  season: string | null;
  seasonYear: number | null;
}

export interface UpcomingAnime extends AnimeSummary {
  startYear: number;
  startMonth: number;
  startDay: number;
}

export interface WatchProvider {
  countryCode: string;
  providerSlug: string;
  providerName: string;
  providerType: string;
  logoUrl: string;
  affiliateUrl: string | null;
}

export interface AnimeDetail {
  anime: {
    anilistId: number;
    slug: string;
    titleEnglish: string;
    titleRomaji: string;
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
    season: string | null;
    seasonYear: number | null;
  };
  watchProvidersByCountry: Record<string, WatchProvider[]>;
}

export interface ProviderSummary {
  slug: string;
  providerName: string;
  logoUrl: string;
  animeCount: number;
}

async function fetchWithRetry(path: string): Promise<Response> {
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
  const res = await fetchWithRetry(path);
  if (res.status === 404) {
    return fallback();
  }
  if (!res.ok) {
    throw new Error(`API ${path} failed: ${res.status} ${res.statusText}`);
  }
  return res.json() as Promise<T>;
}

export const getAllAnime = () => fetchJson<AnimeSummary[]>('/api/anime');

export const getUpcomingAnime = async (days: number) => {
  return fetchJsonAllowing404(`/api/anime/upcoming?days=${days}`, () => [] as UpcomingAnime[]);
};

export const getAnimeBySlug = (slug: string) =>
  fetchJson<AnimeDetail>(`/api/anime/${slug}`);

export const getSimilarAnime = async (slug: string) => {
  return fetchJsonAllowing404(`/api/anime/${slug}/similar`, () => [] as AnimeSummary[]);
};

export const getProviders = () => fetchJson<ProviderSummary[]>('/api/providers');

export const getProvidersByCountry = (countryIso: string) =>
  fetchJson<ProviderSummary[]>(`/api/providers?country=${countryIso}`);

export const getAnimeByProvider = (providerSlug: string, countryIso: string) =>
  fetchJson<AnimeSummary[]>(`/api/providers/${providerSlug}/${countryIso}`);

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

export const getGenres = () => fetchJson<GenreSummary[]>('/api/genres');

export const getAnimeByGenre = (genreSlug: string) =>
  fetchJson<AnimeSummary[]>(`/api/genres/${genreSlug}`);

export const getSeasons = () => fetchJson<SeasonSummary[]>('/api/seasons');

export const getAnimeBySeason = (year: number, season: string) =>
  fetchJson<AnimeSummary[]>(`/api/seasons/${year}/${season}`);

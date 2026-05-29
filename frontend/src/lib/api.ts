import { FALLBACK_TOP_STUDIOS, studioSlug } from './programmaticSeo';

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

export const getSimilarAnime = async (slug: string, watchedSlugs: string[] = []) => {
  const params = new URLSearchParams();
  watchedSlugs.forEach((watched) => params.append('watched', watched));
  const suffix = params.toString() ? `?${params.toString()}` : '';
  return fetchJsonAllowing404(`/api/anime/${slug}/similar${suffix}`, () => [] as AnimeSummary[]);
};

export const getProviders = () => fetchJson<ProviderSummary[]>('/api/providers');

export const getProvidersByCountry = (countryIso: string) =>
  fetchJson<ProviderSummary[]>(`/api/providers?country=${countryIso}`);

export const getAnimeByProvider = (providerSlug: string, countryIso: string) =>
  fetchJson<AnimeSummary[]>(`/api/providers/${providerSlug}/${countryIso}`);

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

export const getGenres = () => fetchJson<GenreSummary[]>('/api/genres');

export const getAnimeByGenre = (genreSlug: string) =>
  fetchJson<AnimeSummary[]>(`/api/genres/${genreSlug}`);

export const getBeginnerAnimeByGenre = async (genreSlug: string) => {
  return fetchJsonAllowing404(`/api/genres/${genreSlug}/beginner`, async () => {
    const anime = await getAnimeByGenre(genreSlug);
    return anime.slice(0, 10).map((item) => ({
      anime: item,
      beginnerRecommendation: null,
    })) satisfies BeginnerAnime[];
  });
};

export const getSeasons = () => fetchJson<SeasonSummary[]>('/api/seasons');

export const getAnimeBySeason = (year: number, season: string) =>
  fetchJson<AnimeSummary[]>(`/api/seasons/${year}/${season}`);

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

  if (summaries.length > 0) return summaries;

  return FALLBACK_TOP_STUDIOS.map((studio) => ({
    ...studio,
    animationStudio: false,
    animeCount: 0,
  }));
};

export const getStudios = async (): Promise<StudioSummary[]> => {
  return fetchJsonAllowing404('/api/studios', async () => {
    return summarizeStudiosFromAnime(await getAllAnime());
  });
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

import { FALLBACK_TOP_STUDIOS, studioSlug } from './programmaticSeo';

const API_URL = import.meta.env.PUBLIC_DATA_API_URL ?? import.meta.env.PUBLIC_API_URL;

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

export interface BeginnerAnime {
  anime: AnimeSummary;
  beginnerRecommendation: string | null;
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

async function fetchJson<T>(path: string): Promise<T> {
  const res = await fetch(`${API_URL}${path}`);
  if (!res.ok) {
    throw new Error(`API ${path} failed: ${res.status} ${res.statusText}`);
  }
  return res.json() as Promise<T>;
}

export const getAllAnime = () => fetchJson<AnimeSummary[]>('/api/anime');

export const getUpcomingAnime = async (days: number) => {
  const res = await fetch(`${API_URL}/api/anime/upcoming?days=${days}`);
  if (res.status === 404) {
    return [] as UpcomingAnime[];
  }
  if (!res.ok) {
    throw new Error(`API /api/anime/upcoming failed: ${res.status} ${res.statusText}`);
  }
  return res.json() as Promise<UpcomingAnime[]>;
};

export const getAnimeBySlug = (slug: string) =>
  fetchJson<AnimeDetail>(`/api/anime/${slug}`);

export const getSimilarAnime = async (slug: string) => {
  const res = await fetch(`${API_URL}/api/anime/${slug}/similar`);
  if (res.status === 404) {
    return [] as AnimeSummary[];
  }
  if (!res.ok) {
    throw new Error(`API /api/anime/${slug}/similar failed: ${res.status} ${res.statusText}`);
  }
  return res.json() as Promise<AnimeSummary[]>;
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

export interface StudioSummary {
  name: string;
  slug: string;
  animeCount: number;
}

export const getGenres = () => fetchJson<GenreSummary[]>('/api/genres');

export const getAnimeByGenre = (genreSlug: string) =>
  fetchJson<AnimeSummary[]>(`/api/genres/${genreSlug}`);

export const getBeginnerAnimeByGenre = async (genreSlug: string) => {
  const res = await fetch(`${API_URL}/api/genres/${genreSlug}/beginner`);
  if (res.status === 404) {
    const anime = await getAnimeByGenre(genreSlug);
    return anime.slice(0, 10).map((item) => ({
      anime: item,
      beginnerRecommendation: null,
    })) satisfies BeginnerAnime[];
  }
  if (!res.ok) {
    throw new Error(`API /api/genres/${genreSlug}/beginner failed: ${res.status} ${res.statusText}`);
  }
  return res.json() as Promise<BeginnerAnime[]>;
};

export const getSeasons = () => fetchJson<SeasonSummary[]>('/api/seasons');

export const getAnimeBySeason = (year: number, season: string) =>
  fetchJson<AnimeSummary[]>(`/api/seasons/${year}/${season}`);

export const getAnimeByDuration = async (minutes: number) => {
  const res = await fetch(`${API_URL}/api/anime/duration/${minutes}`);
  if (res.status === 404) {
    const allAnime = await getAllAnime();
    return allAnime.filter((anime) => anime.episodeDuration === minutes);
  }
  if (!res.ok) {
    throw new Error(`API /api/anime/duration/${minutes} failed: ${res.status} ${res.statusText}`);
  }
  return res.json() as Promise<AnimeSummary[]>;
};

export const getAnimeByMaxEpisodes = async (maxEpisodes: number) => {
  const res = await fetch(`${API_URL}/api/anime/episodes/less-than/${maxEpisodes}`);
  if (res.status === 404) {
    const allAnime = await getAllAnime();
    return allAnime.filter((anime) => anime.episodes !== null && anime.episodes <= maxEpisodes);
  }
  if (!res.ok) {
    throw new Error(`API /api/anime/episodes/less-than/${maxEpisodes} failed: ${res.status} ${res.statusText}`);
  }
  return res.json() as Promise<AnimeSummary[]>;
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
    .map(([slug, value]) => ({ slug, ...value }))
    .sort((a, b) => b.animeCount - a.animeCount || a.name.localeCompare(b.name, 'es'));

  if (summaries.length > 0) return summaries;

  return FALLBACK_TOP_STUDIOS.map((studio) => ({
    ...studio,
    animeCount: 0,
  }));
};

export const getStudios = async () => {
  const res = await fetch(`${API_URL}/api/studios`);
  if (res.status === 404) {
    return summarizeStudiosFromAnime(await getAllAnime());
  }
  if (!res.ok) {
    throw new Error(`API /api/studios failed: ${res.status} ${res.statusText}`);
  }
  return res.json() as Promise<StudioSummary[]>;
};

export const getBestAnimeByStudio = async (slug: string) => {
  const res = await fetch(`${API_URL}/api/studios/${slug}/best`);
  if (res.status === 404) {
    const allAnime = await getAllAnime();
    return allAnime
      .filter((anime) => anime.studio && studioSlug(anime.studio) === slug)
      .sort((a, b) => (b.popularity ?? 0) - (a.popularity ?? 0));
  }
  if (!res.ok) {
    throw new Error(`API /api/studios/${slug}/best failed: ${res.status} ${res.statusText}`);
  }
  return res.json() as Promise<AnimeSummary[]>;
};

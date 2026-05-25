const API_URL = import.meta.env.PUBLIC_API_URL;

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
    studios?: Studio[];
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

export async function getStudios(): Promise<StudioSummary[]> {
  const res = await fetch(`${API_URL}/api/studios`);
  if (res.status === 404) {
    return [];
  }
  if (!res.ok) {
    throw new Error(`API /api/studios failed: ${res.status} ${res.statusText}`);
  }
  return res.json() as Promise<StudioSummary[]>;
}

export const getAnimeByStudio = (studioSlug: string) =>
  fetchJson<AnimeSummary[]>(`/api/studios/${studioSlug}`);

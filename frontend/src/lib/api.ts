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

export interface WatchProvider {
  countryCode: string;
  providerSlug: string;
  providerName: string;
  providerType: string;
  logoUrl: string;
}

export interface AnimeDetail {
  anime: {
    anilistId: number;
    slug: string;
    titleEnglish: string;
    titleRomaji: string;
    description: string | null;
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

export const getAnimeBySlug = (slug: string) =>
  fetchJson<AnimeDetail>(`/api/anime/${slug}`);

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

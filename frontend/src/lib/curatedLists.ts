import type { AnimeDetail, CuratedListDetail } from './api';
import { COUNTRIES, COUNTRY_SLUGS, isoToSlug, type CountrySlug } from './countries';
import { filterVisibleProviders, getPlatform, PLATFORM_SLUGS } from './platforms';

export const DURATION_FILTERS = [
  { slug: 'corta', label: 'Hasta 13 episodios' },
  { slug: 'media', label: '14 a 26 episodios' },
  { slug: 'larga', label: 'Más de 26 episodios' },
  { slug: 'pelicula', label: 'Películas' },
] as const;

export type DurationFilterSlug = typeof DURATION_FILTERS[number]['slug'];

export interface FilterOption {
  slug: string;
  label: string;
}

export interface CuratedListMarketplaceItem {
  list: CuratedListDetail;
  coverImages: string[];
  genreOptions: FilterOption[];
  countryOptions: FilterOption[];
  platformOptions: FilterOption[];
  durationOptions: FilterOption[];
  genreTokens: string[];
  countryTokens: string[];
  platformTokens: string[];
  countryPlatformTokens: string[];
  durationTokens: string[];
}

const slugifyToken = (value: string): string =>
  value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/&/g, 'and')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');

const titleFor = (item: CuratedListDetail['items'][number]): string =>
  item.anime?.titleEnglish || item.anime?.titleRomaji || item.animeSlug;

export const curatedListTitlePreview = (list: CuratedListDetail): string =>
  list.items.slice(0, 3).map(titleFor).join(', ');

const durationFor = (item: CuratedListDetail['items'][number]): DurationFilterSlug | null => {
  const anime = item.anime;
  if (!anime) return null;
  if (anime.format === 'MOVIE') return 'pelicula';
  if (anime.episodes === null) return null;
  if (anime.episodes <= 13) return 'corta';
  if (anime.episodes <= 26) return 'media';
  return 'larga';
};

const sortedOptions = (options: Map<string, string>): FilterOption[] =>
  Array.from(options.entries())
    .map(([slug, label]) => ({ slug, label }))
    .sort((a, b) => a.label.localeCompare(b.label, 'es'));

export const buildCuratedListMarketplaceItem = (
  list: CuratedListDetail,
  detailBySlug: Map<string, AnimeDetail>,
): CuratedListMarketplaceItem => {
  const genres = new Map<string, string>();
  const countries = new Map<string, string>();
  const platforms = new Map<string, string>();
  const durations = new Map<string, string>();
  const countryPlatformTokens = new Set<string>();

  for (const item of list.items) {
    for (const genre of item.anime?.genres ?? []) {
      const slug = slugifyToken(genre);
      if (slug) genres.set(slug, genre);
    }

    const duration = durationFor(item);
    if (duration) {
      durations.set(
        duration,
        DURATION_FILTERS.find((filter) => filter.slug === duration)?.label ?? duration,
      );
    }

    const detail = detailBySlug.get(item.animeSlug);
    if (!detail) continue;

    for (const [countryIso, providers] of Object.entries(detail.watchProvidersByCountry)) {
      const countrySlug = isoToSlug(countryIso);
      if (!countrySlug) continue;
      countries.set(countrySlug, COUNTRIES[countrySlug].name);

      for (const provider of filterVisibleProviders(providers)) {
        const platformName = getPlatform(provider.providerSlug)?.name ?? provider.providerName;
        platforms.set(provider.providerSlug, platformName);
        countryPlatformTokens.add(`${countrySlug}:${provider.providerSlug}`);
      }
    }
  }

  return {
    list,
    coverImages: list.items
      .map((item) => item.anime?.coverImage)
      .filter((cover): cover is string => Boolean(cover))
      .slice(0, 4),
    genreOptions: sortedOptions(genres),
    countryOptions: sortedOptions(countries),
    platformOptions: sortedOptions(platforms),
    durationOptions: sortedOptions(durations),
    genreTokens: Array.from(genres.keys()).sort(),
    countryTokens: Array.from(countries.keys()).sort(),
    platformTokens: Array.from(platforms.keys()).sort(),
    countryPlatformTokens: Array.from(countryPlatformTokens).sort(),
    durationTokens: Array.from(durations.keys()).sort(),
  };
};

export const mergeFilterOptions = (
  lists: CuratedListMarketplaceItem[],
  field: keyof Pick<
    CuratedListMarketplaceItem,
    'genreOptions' | 'countryOptions' | 'platformOptions' | 'durationOptions'
  >,
): FilterOption[] => {
  const merged = new Map<string, string>();
  for (const item of lists) {
    for (const option of item[field]) {
      merged.set(option.slug, option.label);
    }
  }
  return sortedOptions(merged);
};

export const defaultCountryOptions = (): FilterOption[] =>
  COUNTRY_SLUGS.map((slug) => ({ slug, label: COUNTRIES[slug].name }));

export const defaultPlatformOptions = (): FilterOption[] =>
  PLATFORM_SLUGS.map((slug) => ({ slug, label: getPlatform(slug)?.name ?? slug }));

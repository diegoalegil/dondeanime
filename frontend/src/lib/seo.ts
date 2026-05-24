import type { AnimeDetail, WatchProvider } from './api';

const SITE_URL = import.meta.env.PUBLIC_SITE_URL;

export interface BreadcrumbItem {
  name: string;
  url: string;
}

export const absoluteUrl = (path: string): string => `${SITE_URL}${path}`;

const stripHtml = (s: string): string => s.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();

const isoDate = (year: number | null, month: number | null, day: number | null): string | undefined => {
  if (year === null) return undefined;
  const m = String(month ?? 1).padStart(2, '0');
  const d = String(day ?? 1).padStart(2, '0');
  return `${year}-${m}-${d}`;
};

export const buildTVSeriesSchema = (
  anime: AnimeDetail['anime'],
  providers: WatchProvider[],
  pageUrl: string,
) => ({
  '@context': 'https://schema.org',
  '@type': 'TVSeries',
  name: anime.titleEnglish || anime.titleRomaji,
  alternateName: anime.titleRomaji || undefined,
  description: anime.description ? stripHtml(anime.description) : undefined,
  image: anime.coverImage,
  url: pageUrl,
  inLanguage: 'ja',
  datePublished: isoDate(anime.startYear, anime.startMonth, anime.startDay),
  numberOfEpisodes: anime.episodes ?? undefined,
  genre: anime.genres.length > 0 ? anime.genres : undefined,
  aggregateRating: anime.averageScore !== null ? {
    '@type': 'AggregateRating',
    ratingValue: (anime.averageScore / 10).toFixed(1),
    bestRating: '10',
    worstRating: '0',
    ratingCount: anime.popularity ?? 1,
  } : undefined,
  ...(providers.length > 0 && {
    offers: providers.map((p) => ({
      '@type': 'Offer',
      url: pageUrl,
      seller: { '@type': 'Organization', name: p.providerName },
      availability: 'https://schema.org/InStock',
    })),
  }),
});

export const buildBreadcrumbSchema = (items: BreadcrumbItem[]) => ({
  '@context': 'https://schema.org',
  '@type': 'BreadcrumbList',
  itemListElement: items.map((item, i) => ({
    '@type': 'ListItem',
    position: i + 1,
    name: item.name,
    item: item.url,
  })),
});

export const buildWebSiteSchema = () => ({
  '@context': 'https://schema.org',
  '@type': 'WebSite',
  name: 'DondeAnime',
  url: SITE_URL,
  potentialAction: {
    '@type': 'SearchAction',
    target: {
      '@type': 'EntryPoint',
      urlTemplate: `${SITE_URL}/?q={search_term_string}`,
    },
    'query-input': 'required name=search_term_string',
  },
});

export const buildItemListSchema = (
  items: Array<{ name: string; url: string }>,
  pageUrl: string,
  listName: string,
) => ({
  '@context': 'https://schema.org',
  '@type': 'ItemList',
  name: listName,
  url: pageUrl,
  numberOfItems: items.length,
  itemListElement: items.map((item, i) => ({
    '@type': 'ListItem',
    position: i + 1,
    name: item.name,
    url: item.url,
  })),
});

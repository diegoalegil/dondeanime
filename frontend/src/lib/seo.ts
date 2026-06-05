import { getLocale, t } from '@/i18n';
import type { AnimeDetail, AnimeSummary, NewsDetail, WatchProvider } from './api';
import { localizedPath } from './localizedRoutes';

const SITE_URL = import.meta.env.PUBLIC_SITE_URL;
const DEFAULT_ORGANIZATION_SAME_AS = 'https://github.com/diegoalegil';

export interface BreadcrumbItem {
  name: string;
  url: string;
}

export interface FAQItem {
  question: string;
  answer: string;
}

export const getHomeFaqs = (): FAQItem[] => [
  { question: t('faq.legal.question'), answer: t('faq.legal.answer') },
  { question: t('faq.free.question'), answer: t('faq.free.answer') },
  { question: t('faq.platform.question'), answer: t('faq.platform.answer') },
  { question: t('faq.update.question'), answer: t('faq.update.answer') },
  { question: t('faq.unavailable.question'), answer: t('faq.unavailable.answer') },
];

const isAssetPath = (path: string): boolean => /\.[a-z0-9]+$/i.test(path.split('?')[0] ?? path);

export const absoluteUrl = (path: string): string =>
  `${SITE_URL}${isAssetPath(path) ? path : localizedPath(path)}`;

const stripHtml = (s: string): string => s.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();

const isoDate = (year: number | null, month: number | null, day: number | null): string | undefined => {
  if (year === null) return undefined;
  const m = String(month ?? 1).padStart(2, '0');
  const d = String(day ?? 1).padStart(2, '0');
  return `${year}-${m}-${d}`;
};

const titleFor = (anime: AnimeDetail['anime']): string => anime.titleEnglish || anime.titleRomaji;

const ratingValue = (averageScore: number): string => (averageScore / 10).toFixed(1);

const organizationSameAs = (): string[] => {
  const raw = import.meta.env.PUBLIC_ORGANIZATION_SAME_AS ?? DEFAULT_ORGANIZATION_SAME_AS;
  const urls = raw.split(',').map((url) => url.trim()).filter(Boolean);
  return urls.length > 0 ? urls : [DEFAULT_ORGANIZATION_SAME_AS];
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
    ratingValue: ratingValue(anime.averageScore),
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

export const buildAnimeReviewSchema = (
  anime: AnimeDetail['anime'],
  pageUrl: string,
) => {
  if (anime.averageScore === null) return undefined;

  const name = titleFor(anime);

  return {
    '@context': 'https://schema.org',
    '@type': 'Review',
    name: t('seo.review.name', { name }),
    itemReviewed: {
      '@type': 'TVSeries',
      name,
      url: pageUrl,
      image: anime.coverImage,
    },
    author: {
      '@type': 'Organization',
      name: 'AniList',
      url: 'https://anilist.co',
    },
    publisher: {
      '@type': 'Organization',
      name: t('brand.name'),
      url: SITE_URL,
    },
    reviewBody: t('seo.review.body', { name }),
    reviewRating: {
      '@type': 'Rating',
      ratingValue: ratingValue(anime.averageScore),
      bestRating: '10',
      worstRating: '0',
    },
  };
};

export const buildNewsArticleSchema = (news: NewsDetail, pageUrl: string) => ({
  '@context': 'https://schema.org',
  '@type': 'NewsArticle',
  headline: news.title,
  description: news.summary ?? undefined,
  image: news.imageUrl ?? undefined,
  datePublished: news.publishedAt,
  dateModified: news.publishedAt,
  inLanguage: getLocale(),
  mainEntityOfPage: {
    '@type': 'WebPage',
    '@id': pageUrl,
  },
  author: {
    '@type': 'Organization',
    name: news.sourceName,
  },
  publisher: {
    '@type': 'Organization',
    name: t('brand.name'),
    logo: {
      '@type': 'ImageObject',
      url: absoluteUrl('/og-default.png'),
    },
  },
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
  name: t('brand.name'),
  url: SITE_URL,
  potentialAction: {
    '@type': 'SearchAction',
    target: {
      '@type': 'EntryPoint',
      urlTemplate: `${SITE_URL}${localizedPath('/')}?q={search_term_string}`,
    },
    'query-input': 'required name=search_term_string',
  },
});

export const buildOrganizationSchema = () => ({
  '@context': 'https://schema.org',
  '@type': 'Organization',
  name: t('brand.name'),
  url: `${SITE_URL}${localizedPath('/')}`,
  availableLanguage: ['es', 'en'],
  logo: {
    '@type': 'ImageObject',
    url: absoluteUrl('/og-default.png'),
  },
  sameAs: organizationSameAs(),
});

export const buildFAQPageSchema = (faqs: FAQItem[]) => ({
  '@context': 'https://schema.org',
  '@type': 'FAQPage',
  mainEntity: faqs.map((faq) => ({
    '@type': 'Question',
    name: faq.question,
    acceptedAnswer: {
      '@type': 'Answer',
      text: faq.answer,
    },
  })),
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

export const buildRelatedAnimeSchema = (
  source: AnimeDetail['anime'],
  related: AnimeSummary[],
  pageUrl: string,
) => {
  if (related.length === 0) return undefined;

  const sourceName = titleFor(source);

  return {
    '@context': 'https://schema.org',
    '@type': 'ItemList',
    name: `Anime similares a ${sourceName}`,
    url: pageUrl,
    about: {
      '@type': 'TVSeries',
      name: sourceName,
      url: pageUrl,
    },
    numberOfItems: related.length,
    itemListElement: related.map((item, i) => ({
      '@type': 'ListItem',
      position: i + 1,
      item: {
        '@type': 'TVSeries',
        name: item.titleEnglish || item.titleRomaji,
        url: absoluteUrl(`/anime/${item.slug}`),
        image: item.coverImage,
      },
    })),
  };
};

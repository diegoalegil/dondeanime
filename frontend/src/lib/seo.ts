import type { AnimeDetail, AnimeSummary, WatchProvider } from './api';

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

export const HOME_FAQS: FAQItem[] = [
  {
    question: '¿Dónde puedo ver anime online de forma legal?',
    answer: 'Puedes revisar plataformas con licencia como Crunchyroll, Netflix, Prime Video, Disney+ y HBO Max. DondeAnime cruza cada anime con las plataformas detectadas por país.',
  },
  {
    question: '¿Dónde ver anime gratis legalmente?',
    answer: 'Algunas plataformas publican episodios gratis o catálogos con anuncios según el país. Cuando el proveedor aparece como gratuito o incluido en streaming, DondeAnime lo muestra en la ficha.',
  },
  {
    question: '¿Qué plataforma tiene más anime?',
    answer: 'Depende del país y del catálogo vigente. Crunchyroll suele concentrar mucho anime, pero Netflix, Prime Video y otras plataformas cambian sus licencias con frecuencia.',
  },
  {
    question: '¿Cada cuánto se actualiza el catálogo?',
    answer: 'El catálogo se actualiza automáticamente con datos de AniList y TMDb. Las páginas públicas se regeneran cuando hay cambios relevantes en anime o disponibilidad.',
  },
  {
    question: '¿Por qué un anime no aparece disponible en mi país?',
    answer: 'Las licencias de streaming cambian por región. Un anime puede estar disponible en otro país, llegar más tarde o no tener proveedor detectado todavía.',
  },
];

export const absoluteUrl = (path: string): string => `${SITE_URL}${path}`;

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
    name: `Puntuación media de ${name}`,
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
      name: 'DondeAnime',
      url: SITE_URL,
    },
    reviewBody: `Puntuación media de ${name} en AniList, convertida a escala 0-10.`,
    reviewRating: {
      '@type': 'Rating',
      ratingValue: ratingValue(anime.averageScore),
      bestRating: '10',
      worstRating: '0',
    },
  };
};

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

export const buildOrganizationSchema = () => ({
  '@context': 'https://schema.org',
  '@type': 'Organization',
  name: 'DondeAnime',
  url: SITE_URL,
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

import {
  getAllAnime,
  getGenres,
  getProviders,
  getProvidersByCountry,
  getSeasons,
} from './api';
import { getCollection } from 'astro:content';
import { BEST_ANIME_YEARS } from './bestYears';
import { COUNTRIES, COUNTRY_SLUGS } from './countries';
import { localizedPath } from './localizedRoutes';
import { isHiddenVariant } from './platforms';
import { t } from '@/i18n';
import {
  DURATION_MINUTES,
  EPISODE_LIMITS,
  beginnerGenrePath,
  durationPath,
  episodeLimitPath,
} from './programmaticSeo';

export const LANGUAGE_SITEMAP_ENTRIES = [
  { name: 'Spanish', path: '/sitemap-es.xml' },
  { name: 'English', path: '/sitemap-en.xml' },
] as const;

export const PARTITION_SITEMAP_ENTRIES = [
  { name: t('sitemap.anime'), path: '/sitemap-anime.xml' },
  { name: t('sitemap.countries'), path: '/sitemap-paises.xml' },
  { name: t('sitemap.platforms'), path: '/sitemap-plataformas.xml' },
  { name: t('sitemap.genres'), path: '/sitemap-generos.xml' },
  { name: t('sitemap.seasons'), path: '/sitemap-temporadas.xml' },
  { name: t('sitemap.best'), path: '/sitemap-mejores.xml' },
  { name: t('sitemap.combinations'), path: '/sitemap-combinatoria.xml' },
] as const;

export const SITEMAP_ENTRIES = [
  ...LANGUAGE_SITEMAP_ENTRIES,
  ...PARTITION_SITEMAP_ENTRIES,
] as const;

const SITE_URL = import.meta.env.PUBLIC_SITE_URL.replace(/\/$/, '');
const UPCOMING_RELEASE_PATHS = [
  '/estrenos/proxima-semana',
  '/estrenos/proximo-mes',
];

const xmlEscape = (value: string): string =>
  value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');

const toAbsoluteUrl = (path: string): string => `${SITE_URL}${path}`;

const uniqueSorted = (paths: string[]): string[] =>
  Array.from(new Set(paths)).sort((a, b) => a.localeCompare(b, 'es'));

export const sitemapResponse = (body: string): Response =>
  new Response(body, {
    status: 200,
    headers: {
      'Content-Type': 'application/xml; charset=utf-8',
    },
  });

export const renderSitemapIndex = (): string => {
  const sitemaps = SITEMAP_ENTRIES
    .map((entry) => `  <sitemap><loc>${xmlEscape(toAbsoluteUrl(entry.path))}</loc></sitemap>`)
    .join('\n');

  return `<?xml version="1.0" encoding="UTF-8"?>
<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${sitemaps}
</sitemapindex>
`;
};

export const renderUrlSet = (paths: string[]): string => {
  const urls = uniqueSorted(paths)
    .map((path) => `  <url><loc>${xmlEscape(toAbsoluteUrl(path))}</loc></url>`)
    .join('\n');

  return `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${urls}
</urlset>
`;
};

export const animeSitemapPaths = async (): Promise<string[]> => {
  const anime = await getAllAnime();
  return anime.map((item) => `/anime/${item.slug}`);
};

export const staticSitemapPaths = async (): Promise<string[]> => {
  const posts = await getCollection('blog', ({ data }) => !data.draft);

  return [
    '/',
    '/blog',
    ...posts.map((post) => `/blog/${post.id}`),
    '/legal/privacidad',
    '/legal/afiliados',
  ];
};

export const countrySitemapPaths = async (): Promise<string[]> => {
  const anime = await getAllAnime();
  const countryHubs = COUNTRY_SLUGS.map((countrySlug) => `/pais/${countrySlug}`);
  const animeCountryPages = anime.flatMap((item) =>
    COUNTRY_SLUGS.map((countrySlug) => `/anime/${item.slug}/${countrySlug}`),
  );

  return [...countryHubs, ...animeCountryPages];
};

export const platformSitemapPaths = async (): Promise<string[]> => {
  const providerByCountry = await Promise.all(
    COUNTRY_SLUGS.map(async (countrySlug) => {
      const providers = await getProvidersByCountry(COUNTRIES[countrySlug].iso);
      return {
        countrySlug,
        providers: providers.filter((provider) => !isHiddenVariant(provider.slug)),
      };
    }),
  );

  const providerHubs = Array.from(
    new Set(providerByCountry.flatMap(({ providers }) => providers.map((provider) => provider.slug))),
  ).map((providerSlug) => `/plataforma/${providerSlug}`);

  const providerCountryPages = providerByCountry.flatMap(({ countrySlug, providers }) =>
    providers.map((provider) => `/plataforma/${provider.slug}/${countrySlug}`),
  );

  return [...providerHubs, ...providerCountryPages];
};

export const genreSitemapPaths = async (): Promise<string[]> => {
  const genres = await getGenres();
  return genres.map((genre) => `/genero/${genre.slug}`);
};

export const seasonSitemapPaths = async (): Promise<string[]> => {
  const seasons = await getSeasons();
  const seasonPaths = seasons.map((season) => `/temporada/${season.year}/${season.season.toLowerCase()}`);
  return [...seasonPaths, ...UPCOMING_RELEASE_PATHS];
};

export const bestYearSitemapPaths = (): string[] =>
  BEST_ANIME_YEARS.map((year) => `/mejores/${year}`);

export const combinationSitemapPaths = async (): Promise<string[]> => {
  const topGenreLimit = 7;
  const topProviderLimit = 5;
  const [genres, providers] = await Promise.all([
    getGenres(),
    getProviders(),
  ]);

  const topGenres = genres.slice(0, topGenreLimit);
  const topProviders = providers
    .filter((provider) => !isHiddenVariant(provider.slug))
    .slice(0, topProviderLimit);

  const genreProviderPaths = topGenres.flatMap((genre) =>
    topProviders.map((provider) => `/anime/${genre.slug}/en/${provider.slug}`),
  );
  const durationPaths = DURATION_MINUTES.map((minutes) => durationPath(minutes));
  const episodeLimitPaths = EPISODE_LIMITS.map((maxEpisodes) => episodeLimitPath(maxEpisodes));
  const beginnerGenrePaths = genres.map((genre) => beginnerGenrePath(genre.slug));

  return [...genreProviderPaths, ...durationPaths, ...episodeLimitPaths, ...beginnerGenrePaths];
};

export const spanishSitemapPaths = async (): Promise<string[]> => {
  const [
    staticPaths,
    animePaths,
    countryPaths,
    platformPaths,
    genrePaths,
    seasonPaths,
    combinationPaths,
  ] = await Promise.all([
    staticSitemapPaths(),
    animeSitemapPaths(),
    countrySitemapPaths(),
    platformSitemapPaths(),
    genreSitemapPaths(),
    seasonSitemapPaths(),
    combinationSitemapPaths(),
  ]);

  return [
    ...staticPaths,
    ...animePaths,
    ...countryPaths,
    ...platformPaths,
    ...genrePaths,
    ...seasonPaths,
    ...bestYearSitemapPaths(),
    ...combinationPaths,
  ];
};

export const englishSitemapPaths = async (): Promise<string[]> =>
  (await spanishSitemapPaths()).map((path) => localizedPath(path, 'en'));

import { getLocale, type Locale } from '@/i18n';
import { COUNTRIES, type CountrySlug } from '@/lib/countries';

const EN_COUNTRY_SLUGS = {
  espana: 'spain',
  mexico: 'mexico',
  argentina: 'argentina',
  colombia: 'colombia',
  chile: 'chile',
} as const satisfies Record<CountrySlug, string>;

const ES_COUNTRY_SLUGS = Object.fromEntries(
  Object.entries(EN_COUNTRY_SLUGS).map(([es, en]) => [en, es]),
) as Record<string, CountrySlug>;

const trimPath = (path: string): string => {
  const withoutOrigin = path.startsWith('http') ? new URL(path).pathname : path;
  const cleaned = withoutOrigin
    .replace(/\/index\.html$/, '/')
    .replace(/\.html$/, '')
    .replace(/\/+$/, '');
  return cleaned === '' ? '/' : cleaned;
};

const segmentsOf = (path: string): string[] =>
  trimPath(path).split('/').filter(Boolean);

export const countryParamForLocale = (
  slug: CountrySlug,
  locale: Locale = getLocale(),
): string => (locale === 'en' ? EN_COUNTRY_SLUGS[slug] : slug);

export const canonicalCountrySlug = (slug: string | undefined): CountrySlug | null => {
  if (!slug) return null;
  if (slug in COUNTRIES) return slug as CountrySlug;
  return ES_COUNTRY_SLUGS[slug] ?? null;
};

export const localizedCountryName = (
  slug: CountrySlug,
  locale: Locale = getLocale(),
): string => (locale === 'en' ? COUNTRIES[slug].nameEn : COUNTRIES[slug].name);

export const spanishPathFromLocalized = (path: string): string => {
  const segments = segmentsOf(path);
  if (segments[0] !== 'en') return trimPath(path);

  const [, section, first, second, third] = segments;
  if (!section) return '/';

  if (section === 'country' && first) {
    const country = canonicalCountrySlug(first);
    return country ? `/pais/${country}` : `/pais/${first}`;
  }

  if (section === 'platform' && first) {
    const country = canonicalCountrySlug(second);
    return country ? `/plataforma/${first}/${country}` : `/plataforma/${first}`;
  }

  if (section === 'genre' && first) return `/genero/${first}`;
  if (section === 'season' && first && second) return `/temporada/${first}/${second}`;
  if (section === 'best' && first) return `/mejores/${first}`;

  if (section === 'upcoming') {
    if (first === 'next-week') return '/estrenos/proxima-semana';
    if (first === 'next-month') return '/estrenos/proximo-mes';
  }

  if (section === 'blog') return first ? `/blog/${first}` : '/blog';

  if (section === 'legal') {
    if (first === 'privacy') return '/legal/privacidad';
    if (first === 'affiliates') return '/legal/afiliados';
  }

  if (section === 'anime' && first) {
    if (second === 'on' && third) return `/anime/${first}/en/${third}`;
    const country = canonicalCountrySlug(second);
    return country ? `/anime/${first}/${country}` : `/anime/${first}`;
  }

  return trimPath(path.replace(/^\/en/, '') || '/');
};

export const localizedPath = (
  path: string,
  locale: Locale = getLocale(),
): string => {
  const spanishPath = spanishPathFromLocalized(path);
  if (locale === 'es') return spanishPath;

  const segments = segmentsOf(spanishPath);
  const [section, first, second, third] = segments;
  if (!section) return '/en';

  if (section === 'pais' && first) {
    const country = canonicalCountrySlug(first);
    return `/en/country/${country ? countryParamForLocale(country, 'en') : first}`;
  }

  if (section === 'plataforma' && first) {
    const country = canonicalCountrySlug(second);
    return country
      ? `/en/platform/${first}/${countryParamForLocale(country, 'en')}`
      : `/en/platform/${first}`;
  }

  if (section === 'genero' && first) return `/en/genre/${first}`;
  if (section === 'temporada' && first && second) return `/en/season/${first}/${second}`;
  if (section === 'mejores' && first) return `/en/best/${first}`;

  if (section === 'estrenos') {
    if (first === 'proxima-semana') return '/en/upcoming/next-week';
    if (first === 'proximo-mes') return '/en/upcoming/next-month';
  }

  if (section === 'blog') return first ? `/en/blog/${first}` : '/en/blog';

  if (section === 'legal') {
    if (first === 'privacidad') return '/en/legal/privacy';
    if (first === 'afiliados') return '/en/legal/affiliates';
  }

  if (section === 'anime' && first) {
    if (second === 'en' && third) return `/en/anime/${first}/on/${third}`;
    const country = canonicalCountrySlug(second);
    return country
      ? `/en/anime/${first}/${countryParamForLocale(country, 'en')}`
      : `/en/anime/${first}`;
  }

  return `/en/${segments.join('/')}`;
};

export const localeAlternates = (path: string) => {
  const spanishPath = spanishPathFromLocalized(path);
  return [
    { hreflang: 'es', path: localizedPath(spanishPath, 'es') },
    { hreflang: 'en', path: localizedPath(spanishPath, 'en') },
    { hreflang: 'x-default', path: localizedPath(spanishPath, 'es') },
  ];
};

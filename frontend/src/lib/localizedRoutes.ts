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
  if (section === 'genres') return '/generos';
  if (section === 'platforms') return '/plataformas';
  if (section === 'season' && first && second) return `/temporada/${first}/${second}`;
  if (section === 'seasons') return '/temporadas';
  if (section === 'airing') return '/en-emision';
  if (section === 'best' && first) return `/mejores/${first}`;

  if (section === 'upcoming') {
    if (first === 'next-week') return '/estrenos/proxima-semana';
    if (first === 'next-month') return '/estrenos/proximo-mes';
  }

  if (section === 'blog') return first ? `/blog/${first}` : '/blog';

  if (section === 'news') return first ? `/noticias/${first}` : '/noticias';

  if (section === 'contact') return '/contacto';

  if (section === 'about') return '/sobre';

  if (section === 'stickers') return '/stickers';

  if (section === 'my-list') return '/mi-lista';

  if (section === 'legal') {
    if (first === 'privacy') return '/legal/privacidad';
    if (first === 'cookies') return '/legal/cookies';
    if (first === 'terms') return '/legal/terminos';
    if (first === 'affiliates') return '/legal/afiliados';
  }

  if (section === 'anime' && first) {
    if (second === 'news') return `/anime/${first}/noticias`;
    if (second === 'on' && third) return `/anime/${first}/en/${third}`;
    const country = canonicalCountrySlug(second);
    return country ? `/anime/${first}/${country}` : `/anime/${first}`;
  }

  return trimPath(path.replace(/^\/en/, '') || '/');
};

/**
 * Mapea una ruta española a su equivalente inglesa REAL, o devuelve null
 * si esa página inglesa no existe (buscar, api, empezar, estudio,
 * anime/duracion, anime/episodios, índices sin traducir...). Usar null en
 * lugar de inventar una /en/... que daría 404.
 */
export const localizedEnPath = (spanishPath: string): string | null => {
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
  if (section === 'generos') return '/en/genres';
  if (section === 'plataformas') return '/en/platforms';
  if (section === 'temporada' && first && second) return `/en/season/${first}/${second}`;
  if (section === 'temporadas') return '/en/seasons';
  if (section === 'en-emision') return '/en/airing';
  if (section === 'mejores' && first) return `/en/best/${first}`;

  if (section === 'estrenos') {
    if (first === 'proxima-semana') return '/en/upcoming/next-week';
    if (first === 'proximo-mes') return '/en/upcoming/next-month';
    return null;
  }

  if (section === 'blog') return first ? `/en/blog/${first}` : '/en/blog';

  if (section === 'noticias') return first ? `/en/news/${first}` : '/en/news';

  if (section === 'contacto') return '/en/contact';

  if (section === 'sobre') return '/en/about';

  if (section === 'stickers') return '/en/stickers';

  if (section === 'mi-lista') return '/en/my-list';

  if (section === 'legal') {
    if (first === 'privacidad') return '/en/legal/privacy';
    if (first === 'cookies') return '/en/legal/cookies';
    if (first === 'terminos') return '/en/legal/terms';
    if (first === 'afiliados') return '/en/legal/affiliates';
    return null;
  }

  if (section === 'premium') return '/en/premium';

  if (section === 'anime' && first) {
    // /anime/duracion/* y /anime/episodios/* son solo-español.
    if (first === 'duracion' || first === 'episodios') return null;
    if (second === 'noticias') return `/en/anime/${first}/news`;
    if (second === 'en' && third) return `/en/anime/${first}/on/${third}`;
    const country = canonicalCountrySlug(second);
    return country
      ? `/en/anime/${first}/${countryParamForLocale(country, 'en')}`
      : `/en/anime/${first}`;
  }

  return null;
};

export const localizedPath = (
  path: string,
  locale: Locale = getLocale(),
): string => {
  const spanishPath = spanishPathFromLocalized(path);
  if (locale === 'es') return spanishPath;

  // Para CONSTRUIR enlaces mantenemos un fallback aunque la página inglesa
  // no exista, para no romper la navegación ya existente.
  const segments = segmentsOf(spanishPath);
  return localizedEnPath(spanishPath) ?? `/en/${segments.join('/')}`;
};

export const localeAlternates = (path: string): Array<{ hreflang: string; path: string }> => {
  const spanishPath = spanishPathFromLocalized(path);
  const enPath = localizedEnPath(spanishPath);
  // Solo emitimos hreflang cuando la versión inglesa EXISTE. Para páginas
  // solo-español no emitimos nada: un hreflang apuntando a un 404 hace que
  // Google ignore (o penalice) el clúster de idiomas.
  if (!enPath) {
    return [];
  }
  return [
    { hreflang: 'es', path: spanishPath },
    { hreflang: 'en', path: enPath },
    { hreflang: 'x-default', path: spanishPath },
  ];
};

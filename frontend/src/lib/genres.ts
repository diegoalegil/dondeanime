import { getLocale, type Locale } from '@/i18n';

// Los nombres de genero llegan del API en ingles (AniList). El long-tail hispano
// busca "anime de accion", "anime de terror", "anime de deportes" — no "Action".
// Mapa por slug (estable) -> nombre en espanol para todo el texto y meta ES.
const ES_GENRE_NAMES: Record<string, string> = {
  action: 'Acción',
  adventure: 'Aventura',
  comedy: 'Comedia',
  drama: 'Drama',
  ecchi: 'Ecchi',
  fantasy: 'Fantasía',
  horror: 'Terror',
  'mahou-shoujo': 'Mahou Shoujo',
  mecha: 'Mecha',
  music: 'Música',
  mystery: 'Misterio',
  psychological: 'Psicológico',
  romance: 'Romance',
  'sci-fi': 'Ciencia ficción',
  'slice-of-life': 'Recuentos de la vida',
  sports: 'Deportes',
  supernatural: 'Sobrenatural',
  thriller: 'Suspense',
};

/**
 * Nombre del genero localizado. En ES devuelve el nombre espanol por slug (con
 * fallback al nombre del API si el slug no esta mapeado); en EN devuelve el
 * nombre del API tal cual (ingles).
 */
export const localizedGenreName = (
  slug: string,
  fallbackName: string,
  locale: Locale = getLocale(),
): string => (locale === 'es' ? ES_GENRE_NAMES[slug] ?? fallbackName : fallbackName);

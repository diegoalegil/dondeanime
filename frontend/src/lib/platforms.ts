import { getLocale, type Locale } from '@/i18n';

export interface PlatformInfo {
  name: string;
  color: string;
  url: string;
}

export const PLATFORMS = {
  'crunchyroll': { name: 'Crunchyroll', color: '#F47521', url: 'https://www.crunchyroll.com/' },
  'netflix': { name: 'Netflix', color: '#E50914', url: 'https://www.netflix.com/' },
  'amazon-prime-video': { name: 'Prime Video', color: '#1FA2FF', url: 'https://www.primevideo.com/' },
  'hbo-max': { name: 'HBO Max', color: '#B47AFD', url: 'https://www.max.com/' },
  'disney-plus': { name: 'Disney+', color: '#0F3057', url: 'https://www.disneyplus.com/' },
  'claro-video': { name: 'Claro video', color: '#DA291C', url: 'https://www.clarovideo.com/' },
  'movistartv': { name: 'MovistarTV', color: '#019DF4', url: 'https://www.movistarplus.es/' },
  '3cat': { name: '3Cat', color: '#1A1A1A', url: 'https://www.3cat.cat/' },
} as const satisfies Record<string, PlatformInfo>;

export type PlatformSlug = keyof typeof PLATFORMS;

export const PLATFORM_SLUGS = Object.keys(PLATFORMS) as PlatformSlug[];

const HIDDEN_VARIANT_SLUGS = new Set<string>([
  'crunchyroll-amazon-channel',
  'netflix-standard-with-ads',
  'amazon-prime-video-with-ads',
  'hbo-max-amazon-channel',
]);

export const isHiddenVariant = (slug: string): boolean =>
  HIDDEN_VARIANT_SLUGS.has(slug);

export const filterVisibleProviders = <T extends { providerSlug: string }>(
  providers: T[]
): T[] => providers.filter((p) => !isHiddenVariant(p.providerSlug));

export const getPlatform = (slug: string): PlatformInfo | null =>
  PLATFORMS[slug as PlatformSlug] ?? null;

// Descripcion editorial por plataforma: prosa lider para /plataforma/[slug] y su
// espejo /en/platform/[slug] (antes solo H1 + contador). Texto general y honesto
// —la disponibilidad real varia por pais y se ve en la rejilla de abajo—, sin
// afirmar catalogos concretos que cambian. Slugs no mapeados: sin parrafo.
const ES_PLATFORM_DESCRIPTIONS: Record<string, string> = {
  crunchyroll: 'Crunchyroll es la plataforma de referencia para el anime: el mayor catálogo del género y simulcast de los estrenos de cada temporada casi a la vez que en Japón. Aquí ves qué anime de tu lista están en Crunchyroll en cada país.',
  netflix: 'Netflix apuesta por el anime con producciones propias y licencias exclusivas, normalmente con doblaje y subtítulos en español. Consulta qué series de anime están disponibles en Netflix según tu país.',
  'amazon-prime-video': 'Prime Video suma anime a su catálogo general, con exclusivas puntuales y títulos que no encuentras en otras plataformas. Mira qué anime puedes ver con Prime Video en España y Latinoamérica.',
  'hbo-max': 'Max (antes HBO Max) ha ido ampliando su oferta de anime, con una disponibilidad que cambia bastante según la región. Revisa qué anime están en Max en tu país.',
  'disney-plus': 'Disney+ incorpora anime de forma selectiva, con estrenos cuidados y doblaje en español. Aquí tienes qué anime puedes ver en Disney+ en cada país.',
  'claro-video': 'Claro video ofrece anime dentro de su catálogo para Latinoamérica, con una disponibilidad que varía de un país a otro. Comprueba qué anime están en Claro video donde vives.',
  movistartv: 'Movistar Plus+ incluye anime en su catálogo en España, entre estrenos y títulos de fondo. Mira qué anime puedes ver con Movistar.',
  '3cat': '3Cat, la plataforma de la CCMA, mantiene viva la tradición del anime doblado al catalán. Descubre qué anime están disponibles en 3Cat.',
};

const EN_PLATFORM_DESCRIPTIONS: Record<string, string> = {
  crunchyroll: 'Crunchyroll is the go-to platform for anime: the largest catalog in the genre and simulcasts of each season\'s premieres almost at the same time as Japan. See which anime from your list are on Crunchyroll in each country.',
  netflix: 'Netflix backs anime with its own productions and exclusive licenses, usually with Spanish dub and subtitles. Check which anime series are available on Netflix in your country.',
  'amazon-prime-video': 'Prime Video adds anime to its general catalog, with occasional exclusives and titles you won\'t find elsewhere. See which anime you can watch with Prime Video across Spain and Latin America.',
  'hbo-max': 'Max (formerly HBO Max) has been expanding its anime line-up, with availability that varies a lot by region. Check which anime are on Max in your country.',
  'disney-plus': 'Disney+ adds anime selectively, with polished premieres and Spanish dubbing. Here\'s which anime you can watch on Disney+ in each country.',
  'claro-video': 'Claro video offers anime within its Latin America catalog, with availability that varies from country to country. Check which anime are on Claro video where you live.',
  movistartv: 'Movistar Plus+ includes anime in its catalog in Spain, between new premieres and back catalog. See which anime you can watch with Movistar.',
  '3cat': '3Cat, the CCMA\'s platform, keeps the tradition of anime dubbed into Catalan alive. Discover which anime are available on 3Cat.',
};

/**
 * Descripcion editorial de la plataforma por locale (ES/EN). null si el slug no
 * esta mapeado (providers menores), para no renderizar un parrafo vacio.
 */
export const platformDescription = (
  slug: string,
  locale: Locale = getLocale(),
): string | null =>
  (locale === 'en' ? EN_PLATFORM_DESCRIPTIONS[slug] : ES_PLATFORM_DESCRIPTIONS[slug]) ?? null;

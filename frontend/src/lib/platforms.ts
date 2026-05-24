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

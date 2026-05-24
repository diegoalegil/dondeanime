export interface PlatformInfo {
  name: string;
  color: string;
}

export const PLATFORMS = {
  'crunchyroll': { name: 'Crunchyroll', color: '#F47521' },
  'netflix': { name: 'Netflix', color: '#E50914' },
  'amazon-prime-video': { name: 'Prime Video', color: '#1FA2FF' },
  'hbo-max': { name: 'HBO Max', color: '#B47AFD' },
  'disney-plus': { name: 'Disney+', color: '#0F3057' },
  'claro-video': { name: 'Claro video', color: '#DA291C' },
  'movistartv': { name: 'MovistarTV', color: '#019DF4' },
  '3cat': { name: '3Cat', color: '#1A1A1A' },
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

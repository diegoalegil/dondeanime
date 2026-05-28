export const DURATION_MINUTES = [12, 22, 24, 25, 45, 60] as const;
export const EPISODE_LIMITS = [12, 24, 50, 100, 200] as const;
export const TOP_STUDIO_LIMIT = 10;

export const FALLBACK_TOP_STUDIOS = [
  { name: 'Madhouse', slug: 'madhouse' },
  { name: 'Bones', slug: 'bones' },
  { name: 'MAPPA', slug: 'mappa' },
  { name: 'WIT Studio', slug: 'wit-studio' },
  { name: 'Ufotable', slug: 'ufotable' },
  { name: 'Kyoto Animation', slug: 'kyoto-animation' },
  { name: 'A-1 Pictures', slug: 'a-1-pictures' },
  { name: 'Toei Animation', slug: 'toei-animation' },
  { name: 'Pierrot', slug: 'pierrot' },
  { name: 'Trigger', slug: 'trigger' },
] as const;

export const durationPath = (minutes: number): string => `/anime/duracion/${minutes}`;
export const episodeLimitPath = (maxEpisodes: number): string => `/anime/episodios/menos-de-${maxEpisodes}`;
export const beginnerGenrePath = (genreSlug: string): string => `/empezar/${genreSlug}`;
export const studioPath = (studioSlug: string): string => `/estudio/${studioSlug}/mejores`;

export const studioSlug = (value: string): string =>
  value
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, '')
    .trim()
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-');

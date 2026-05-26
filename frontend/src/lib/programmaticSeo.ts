export const DURATION_MINUTES = [12, 22, 24, 25, 45, 60] as const;
export const EPISODE_LIMITS = [12, 24, 50, 100, 200] as const;

export const durationPath = (minutes: number): string => `/anime/duracion/${minutes}`;
export const episodeLimitPath = (maxEpisodes: number): string => `/anime/episodios/menos-de-${maxEpisodes}`;

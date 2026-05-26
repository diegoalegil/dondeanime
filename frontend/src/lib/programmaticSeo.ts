export const DURATION_MINUTES = [12, 22, 24, 25, 45, 60] as const;

export const durationPath = (minutes: number): string => `/anime/duracion/${minutes}`;

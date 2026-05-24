const FORMAT_LABELS: Record<string, string> = {
  TV: 'Serie TV',
  TV_SHORT: 'Serie corta',
  MOVIE: 'Película',
  SPECIAL: 'Especial',
  OVA: 'OVA',
  ONA: 'ONA',
  MUSIC: 'Música',
};

const STATUS_LABELS: Record<string, string> = {
  FINISHED: 'Finalizado',
  RELEASING: 'En emisión',
  NOT_YET_RELEASED: 'Próximamente',
  CANCELLED: 'Cancelado',
  HIATUS: 'En pausa',
};

const SEASON_LABELS: Record<string, string> = {
  WINTER: 'Invierno',
  SPRING: 'Primavera',
  SUMMER: 'Verano',
  FALL: 'Otoño',
};

const PROVIDER_TYPE_LABELS: Record<string, string> = {
  FLATRATE: 'Suscripción',
  FREE: 'Gratis',
  RENT: 'Alquiler',
  BUY: 'Compra',
  ADS: 'Con anuncios',
};

const MONTHS_SHORT = [
  'ene', 'feb', 'mar', 'abr', 'may', 'jun',
  'jul', 'ago', 'sep', 'oct', 'nov', 'dic',
];

export const formatType = (value: string): string => FORMAT_LABELS[value] ?? value;
export const formatStatus = (value: string): string => STATUS_LABELS[value] ?? value;
export const formatSeason = (value: string): string => SEASON_LABELS[value] ?? value;
export const formatProviderType = (value: string): string =>
  PROVIDER_TYPE_LABELS[value] ?? value;

export const formatDate = (
  year: number | null,
  month: number | null,
  day: number | null,
): string => {
  if (year === null) return '—';
  if (month === null) return String(year);
  const monthLabel = MONTHS_SHORT[month - 1] ?? '';
  if (day === null) return `${monthLabel} ${year}`;
  return `${day} ${monthLabel} ${year}`;
};

export const formatNumber = (value: number | null): string => {
  if (value === null) return '—';
  return new Intl.NumberFormat('es-ES').format(value);
};

export const formatSeasonYear = (
  season: string | null,
  year: number | null,
): string => {
  if (!season || year === null) return '—';
  return `${formatSeason(season)} ${year}`;
};

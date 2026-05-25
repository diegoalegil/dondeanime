import { t, type I18nKey } from '@/i18n';

const FORMAT_LABEL_KEYS: Record<string, I18nKey> = {
  TV: 'format.type.TV',
  TV_SHORT: 'format.type.TV_SHORT',
  MOVIE: 'format.type.MOVIE',
  SPECIAL: 'format.type.SPECIAL',
  OVA: 'format.type.OVA',
  ONA: 'format.type.ONA',
  MUSIC: 'format.type.MUSIC',
};

const STATUS_LABEL_KEYS: Record<string, I18nKey> = {
  FINISHED: 'format.status.FINISHED',
  RELEASING: 'format.status.RELEASING',
  NOT_YET_RELEASED: 'format.status.NOT_YET_RELEASED',
  CANCELLED: 'format.status.CANCELLED',
  HIATUS: 'format.status.HIATUS',
};

const SEASON_LABEL_KEYS: Record<string, I18nKey> = {
  WINTER: 'format.season.WINTER',
  SPRING: 'format.season.SPRING',
  SUMMER: 'format.season.SUMMER',
  FALL: 'format.season.FALL',
};

const PROVIDER_TYPE_LABEL_KEYS: Record<string, I18nKey> = {
  FLATRATE: 'format.providerType.FLATRATE',
  FREE: 'format.providerType.FREE',
  RENT: 'format.providerType.RENT',
  BUY: 'format.providerType.BUY',
  ADS: 'format.providerType.ADS',
};

const MONTH_KEYS: Record<number, I18nKey> = {
  1: 'format.month.1',
  2: 'format.month.2',
  3: 'format.month.3',
  4: 'format.month.4',
  5: 'format.month.5',
  6: 'format.month.6',
  7: 'format.month.7',
  8: 'format.month.8',
  9: 'format.month.9',
  10: 'format.month.10',
  11: 'format.month.11',
  12: 'format.month.12',
};

export const formatType = (value: string): string => {
  const key = FORMAT_LABEL_KEYS[value];
  return key ? t(key) : value;
};

export const formatStatus = (value: string): string => {
  const key = STATUS_LABEL_KEYS[value];
  return key ? t(key) : value;
};

export const formatSeason = (value: string): string => {
  const key = SEASON_LABEL_KEYS[value];
  return key ? t(key) : value;
};

export const formatProviderType = (value: string): string => {
  const key = PROVIDER_TYPE_LABEL_KEYS[value];
  return key ? t(key) : value;
};

export const formatDate = (
  year: number | null,
  month: number | null,
  day: number | null,
): string => {
  if (year === null) return '—';
  if (month === null) return String(year);
  const monthLabel = MONTH_KEYS[month] ? t(MONTH_KEYS[month]) : '';
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

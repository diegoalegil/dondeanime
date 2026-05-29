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

// AniList manda los nombres de estudio con casing inconsistente: gritados
// (MADHOUSE, WHITE FOX), minusculas (bones, ufotable) y acronimos legitimos que
// DEBEN ir en mayusculas (AIC, APPP, A.C.G.T., P.I.C.S., TNK, MAPPA, TRIGGER).
// No hay regla automatica que distinga "MADHOUSE" (gritado) de "AIC" (acronimo),
// asi que: title-case solo a los que vienen en minusculas (seguro) y un
// diccionario curado para los gritados que queremos suavizar. Todo lo no listado
// se respeta tal cual viene del backend (cero regresion sobre acronimos).
const STUDIO_NAME_OVERRIDES: Record<string, string> = {
  // Estilizados: su casing oficial NO es title-case.
  mappa: 'MAPPA',
  olm: 'OLM',
  'feel.': 'feel.',
  ufotable: 'ufotable',
  // Con puntos / formato propio.
  'j.c.staff': 'J.C.Staff',
  'p.a.works': 'P.A. Works',
  lidenfilms: 'Liden Films',
  'project no.9': 'Project No.9',
  // Gritados (todo mayusculas en AniList) que se ven mejor en su casing real.
  madhouse: 'Madhouse',
  gonzo: 'Gonzo',
  arms: 'Arms',
  blade: 'Blade',
  'bug films': 'Bug Films',
  connect: 'Connect',
  'egg firm': 'Egg Firm',
  hornets: 'Hornets',
  nut: 'Nut',
  'pierrot films': 'Pierrot Films',
  'pine jam': 'Pine Jam',
  'polygon pictures': 'Polygon Pictures',
  revoroot: 'Revoroot',
  sanzigen: 'Sanzigen',
  'typhoon graphics': 'Typhoon Graphics',
  'white fox': 'White Fox',
  'wit studio': 'Wit Studio',
  zexcs: 'Zexcs',
  'aqua aris': 'Aqua Aris',
  'silver link.': 'Silver Link.',
};

const titleCaseStudioWord = (word: string): string =>
  word.length === 0 ? word : word.charAt(0).toUpperCase() + word.slice(1).toLowerCase();

export const formatStudioName = (raw: string): string => {
  const name = (raw ?? '').trim();
  if (!name) return name;
  const override = STUDIO_NAME_OVERRIDES[name.toLowerCase()];
  if (override) return override;
  const hasLower = /[a-z]/.test(name);
  const hasUpper = /[A-Z]/.test(name);
  // Solo title-case a los que vienen TODO en minusculas (bones, david production).
  // Los acronimos viven en mayusculas (AIC, APPP); NO los tocamos para no romper
  // "AIC" -> "Aic". Los gritados que queremos suavizar van por el diccionario.
  if (hasLower && !hasUpper) {
    return name.split(/\s+/).map(titleCaseStudioWord).join(' ');
  }
  return name;
};

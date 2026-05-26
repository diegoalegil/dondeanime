export interface CountryInfo {
  iso: string;
  name: string;
  nameEn: string;
  flagCode: string;
  hreflang: string;
}

export const COUNTRIES = {
  espana: { iso: 'ES', name: 'Espa\u00f1a', nameEn: 'Spain', flagCode: 'es', hreflang: 'es-ES' },
  mexico: { iso: 'MX', name: 'M\u00e9xico', nameEn: 'Mexico', flagCode: 'mx', hreflang: 'es-MX' },
  argentina: { iso: 'AR', name: 'Argentina', nameEn: 'Argentina', flagCode: 'ar', hreflang: 'es-AR' },
  colombia: { iso: 'CO', name: 'Colombia', nameEn: 'Colombia', flagCode: 'co', hreflang: 'es-CO' },
  chile: { iso: 'CL', name: 'Chile', nameEn: 'Chile', flagCode: 'cl', hreflang: 'es-CL' },
} as const satisfies Record<string, CountryInfo>;

export type CountrySlug = keyof typeof COUNTRIES;

export const COUNTRY_SLUGS = Object.keys(COUNTRIES) as CountrySlug[];

export const isoToSlug = (iso: string): CountrySlug | null => {
  const entry = (Object.entries(COUNTRIES) as Array<[CountrySlug, CountryInfo]>)
    .find(([, v]) => v.iso === iso);
  return entry ? entry[0] : null;
};

export const slugToIso = (slug: string): string | null =>
  COUNTRIES[slug as CountrySlug]?.iso ?? null;

export const getCountry = (slug: string): CountryInfo | null =>
  COUNTRIES[slug as CountrySlug] ?? null;

export interface CountryInfo {
  iso: string;
  name: string;
  hreflang: string;
}

export const COUNTRIES = {
  espana: { iso: 'ES', name: 'España', hreflang: 'es-ES' },
  mexico: { iso: 'MX', name: 'México', hreflang: 'es-MX' },
  argentina: { iso: 'AR', name: 'Argentina', hreflang: 'es-AR' },
  colombia: { iso: 'CO', name: 'Colombia', hreflang: 'es-CO' },
  chile: { iso: 'CL', name: 'Chile', hreflang: 'es-CL' },
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

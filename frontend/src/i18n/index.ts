import { AsyncLocalStorage } from 'node:async_hooks';
import es from './es.json';
import en from './en.json';

export type Locale = 'es' | 'en';
export type I18nKey = keyof typeof es;

const dictionaries = {
  es,
  en,
} satisfies Record<Locale, Record<string, string>>;

const localeStore = new AsyncLocalStorage<Locale>();
let activeLocale: Locale = 'es';

export const isLocale = (value: string | undefined): value is Locale =>
  value === 'es' || value === 'en';

export const localeFromPath = (pathname: string): Locale => {
  const cleanPathname = pathname
    .replace(/\/index\.html$/, '/')
    .replace(/\.html$/, '');
  return cleanPathname === '/en' || cleanPathname.startsWith('/en/') ? 'en' : 'es';
};

export const runWithLocale = <T>(locale: Locale, callback: () => T): T =>
  localeStore.run(locale, callback);

export const setLocale = (locale: Locale) => {
  activeLocale = locale;
};

export const getLocale = (): Locale => localeStore.getStore() ?? activeLocale;

export const t = (
  key: I18nKey,
  values: Record<string, string | number> = {},
  locale: Locale = activeLocale,
): string => {
  const dictionary = dictionaries[locale] ?? dictionaries.es;
  const template = dictionary[key] ?? dictionaries.es[key] ?? key;
  return Object.entries(values).reduce(
    (text, [name, value]) => text.replaceAll(`{${name}}`, String(value)),
    template,
  );
};

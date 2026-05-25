import es from './es.json';
import en from './en.json';

export type Locale = 'es' | 'en';
export type I18nKey = keyof typeof es;

const dictionaries = {
  es,
  en,
} satisfies Record<Locale, Record<string, string>>;

let activeLocale: Locale = 'es';

export const setLocale = (locale: Locale) => {
  activeLocale = locale;
};

export const getLocale = (): Locale => activeLocale;

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

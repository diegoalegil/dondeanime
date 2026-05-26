import { defineMiddleware } from 'astro:middleware';
import { localeFromPath, runWithLocale, setLocale } from '@/i18n';

export const onRequest = defineMiddleware((context, next) => {
  const locale = localeFromPath(context.url.pathname);
  setLocale(locale);
  return runWithLocale(locale, () => next());
});

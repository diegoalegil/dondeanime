import type { CollectionEntry } from 'astro:content';

export type BlogPost = CollectionEntry<'blog'>;
export type BlogLocale = 'es' | 'en';

export const blogSlug = (post: BlogPost): string => {
  const segments = post.id.split('/');
  return segments[segments.length - 1] ?? post.id;
};

export const blogLocale = (post: BlogPost): BlogLocale => post.data.locale ?? 'es';

export const isPublishedBlogPost = (post: BlogPost, locale: BlogLocale): boolean =>
  !post.data.draft && blogLocale(post) === locale;

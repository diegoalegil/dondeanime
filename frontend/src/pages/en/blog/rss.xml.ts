import type { APIRoute } from 'astro';
import { getCollection } from 'astro:content';
import { blogSlug, isPublishedBlogPost } from '@/lib/blog';
import { setLocale, t } from '@/i18n';

const SITE_URL = import.meta.env.PUBLIC_SITE_URL.replace(/\/$/, '');

const xmlEscape = (value: string): string =>
  value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');

export const GET: APIRoute = async () => {
  setLocale('en');
  const posts = (await getCollection('blog', (post) => isPublishedBlogPost(post, 'en')))
    .sort((a, b) => b.data.pubDate.getTime() - a.data.pubDate.getTime());

  const items = posts.map((post) => {
    const url = `${SITE_URL}/en/blog/${blogSlug(post)}`;
    return `    <item>
      <title>${xmlEscape(post.data.title)}</title>
      <description>${xmlEscape(post.data.description)}</description>
      <link>${xmlEscape(url)}</link>
      <guid>${xmlEscape(url)}</guid>
      <pubDate>${post.data.pubDate.toUTCString()}</pubDate>
    </item>`;
  }).join('\n');

  const body = `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>${xmlEscape(t('blog.rss.title'))}</title>
    <description>${xmlEscape(t('blog.rss.description'))}</description>
    <link>${xmlEscape(`${SITE_URL}/en/blog`)}</link>
${items}
  </channel>
</rss>
`;

  return new Response(body, {
    status: 200,
    headers: {
      'Content-Type': 'application/rss+xml; charset=utf-8',
    },
  });
};

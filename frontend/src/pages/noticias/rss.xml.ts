import type { APIRoute } from 'astro';
import { getNews } from '@/lib/api';
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
  setLocale('es');
  const news = await getNews(50);

  const items = news
    .map((n) => {
      const url = `${SITE_URL}/noticias/${n.slug}`;
      return `    <item>
      <title>${xmlEscape(n.title)}</title>
      <description>${xmlEscape(n.summary ?? '')}</description>
      <link>${xmlEscape(url)}</link>
      <guid>${xmlEscape(url)}</guid>
      <pubDate>${new Date(n.publishedAt).toUTCString()}</pubDate>
    </item>`;
    })
    .join('\n');

  const body = `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>${xmlEscape(t('news.rss.title'))}</title>
    <description>${xmlEscape(t('news.rss.description'))}</description>
    <link>${xmlEscape(`${SITE_URL}/noticias`)}</link>
${items}
  </channel>
</rss>`;

  return new Response(body, {
    status: 200,
    headers: {
      'Content-Type': 'application/rss+xml; charset=utf-8',
    },
  });
};

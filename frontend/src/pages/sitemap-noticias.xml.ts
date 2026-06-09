import type { APIRoute } from 'astro';
import { getNews } from '@/lib/api';
import { sitemapResponse } from '@/lib/sitemaps';

const SITE_URL = import.meta.env.PUBLIC_SITE_URL.replace(/\/$/, '');
// Google News solo considera artículos de las últimas 48h; los más antiguos
// siguen listados como <url> normales para no perder descubribilidad.
const NEWS_FRESHNESS_MS = 48 * 60 * 60 * 1000;

const xmlEscape = (value: string): string =>
  value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');

export const GET: APIRoute = async () => {
  const news = await getNews(100);
  const now = Date.now();

  const urls = news
    .map((item) => {
      const loc = xmlEscape(`${SITE_URL}/noticias/${item.slug}`);
      const publishedAt = new Date(item.publishedAt);
      const isFresh = Number.isFinite(publishedAt.getTime())
        && now - publishedAt.getTime() <= NEWS_FRESHNESS_MS;

      if (!isFresh) {
        return `  <url><loc>${loc}</loc></url>`;
      }

      return `  <url>
    <loc>${loc}</loc>
    <news:news>
      <news:publication>
        <news:name>DondeAnime</news:name>
        <news:language>es</news:language>
      </news:publication>
      <news:publication_date>${xmlEscape(publishedAt.toISOString())}</news:publication_date>
      <news:title>${xmlEscape(item.title)}</news:title>
    </news:news>
  </url>`;
    })
    .join('\n');

  const body = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:news="http://www.google.com/schemas/sitemap-news/0.9">
${urls}
</urlset>
`;

  return sitemapResponse(body);
};

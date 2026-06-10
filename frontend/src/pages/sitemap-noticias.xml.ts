import type { APIRoute } from 'astro';
import { getNews, getNewsSlugs } from '@/lib/api';
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
  // Las frescas (48h) necesitan título y fecha → getNews(100) llega de sobra.
  // El resto solo necesita <loc>, y /slugs no está capado a 100 artículos.
  const [news, allSlugs] = await Promise.all([getNews(100), getNewsSlugs()]);
  const now = Date.now();
  const summaryBySlug = new Map(news.map((item) => [item.slug, item]));

  const urls = allSlugs
    .map((slug) => {
      const loc = xmlEscape(`${SITE_URL}/noticias/${slug}`);
      const item = summaryBySlug.get(slug);
      const publishedAt = item ? new Date(item.publishedAt) : null;
      const isFresh = publishedAt !== null
        && Number.isFinite(publishedAt.getTime())
        && now - publishedAt.getTime() <= NEWS_FRESHNESS_MS;

      if (!item || !isFresh) {
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

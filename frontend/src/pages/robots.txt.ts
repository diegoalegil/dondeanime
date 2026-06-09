import type { APIRoute } from 'astro';

export const GET: APIRoute = ({ site }) => {
  const base = site?.toString() ?? 'https://www.dondeanime.com/';
  const sitemapUrl = new URL('sitemap-index.xml', base).toString();
  const spanishSitemapUrl = new URL('sitemap-es.xml', base).toString();
  const englishSitemapUrl = new URL('sitemap-en.xml', base).toString();
  const body = `User-agent: *
Allow: /
Disallow: /api/
Disallow: /admin/

Sitemap: ${sitemapUrl}
Sitemap: ${spanishSitemapUrl}
Sitemap: ${englishSitemapUrl}
`;
  return new Response(body, {
    status: 200,
    headers: { 'Content-Type': 'text/plain; charset=utf-8' },
  });
};

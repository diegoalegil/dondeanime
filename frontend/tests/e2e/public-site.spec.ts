import { expect, test } from '@playwright/test';

test('static routes expose search index, robots and sitemap', async ({ request }) => {
  const searchIndex = await request.get('/search-index.json');
  expect(searchIndex.ok()).toBe(true);
  expect(searchIndex.headers()['content-type']).toContain('application/json');

  const index = await searchIndex.json();
  expect(index.length).toBeGreaterThanOrEqual(100);
  expect(index[0]).toEqual(expect.objectContaining({
    slug: expect.any(String),
    cover: expect.any(String),
  }));

  const robots = await request.get('/robots.txt');
  expect(robots.ok()).toBe(true);
  expect(await robots.text()).toContain('Sitemap: https://dondeanime.com/sitemap-index.xml');

  const sitemap = await request.get('/sitemap-index.xml');
  expect(sitemap.ok()).toBe(true);
  expect(await sitemap.text()).toContain('<sitemapindex');
});

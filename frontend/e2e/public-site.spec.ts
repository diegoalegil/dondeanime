import { expect, test } from '@playwright/test';

test('home renders the static catalog and links to an anime detail page', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveTitle(/DondeAnime/);
  await expect(page.getByRole('heading', { name: /Encuentra dónde ver/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute('href', 'https://dondeanime.com/');

  const animeCards = page.locator('article a[href^="/anime/"]');
  expect(await animeCards.count()).toBeGreaterThanOrEqual(100);

  const firstHref = await animeCards.first().getAttribute('href');
  expect(firstHref).toMatch(/^\/anime\/[^/]+$/);

  await animeCards.first().click();

  await expect(page).toHaveURL(new RegExp(`${escapeRegExp(firstHref!)}$`));
  await expect(page.getByRole('heading', { name: /Dónde verlo/i })).toBeVisible();
  await expect(page.locator('script[type="application/ld+json"]').first()).toBeAttached();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    /https:\/\/dondeanime\.com\/anime\/[^/]+$/,
  );
});

test('country, platform, genre and season hubs have working static routes', async ({ page }) => {
  await page.goto('/');

  await page.locator('main a[href="/pais/espana"]').click();
  await expect(page.getByRole('heading', { name: /Anime en streaming en España/i })).toBeVisible();

  const platformCountryLink = page.locator('a[href^="/plataforma/"][href$="/espana"]').first();
  await expect(platformCountryLink).toBeVisible();

  const platformCountryHref = await platformCountryLink.getAttribute('href');
  expect(platformCountryHref).toMatch(/^\/plataforma\/[^/]+\/espana$/);

  await platformCountryLink.click();
  await expect(page.getByRole('heading', { name: /Anime en .*España/i })).toBeVisible();

  await page.goto('/');
  const seasonLink = page.locator('a[href^="/temporada/"]').first();
  await expect(seasonLink).toBeVisible();
  await seasonLink.click();
  await expect(page.getByRole('heading', { name: /Anime de /i })).toBeVisible();

  await page.goto('/');
  const genreLink = page.locator('a[href^="/genero/"]').first();
  await expect(genreLink).toBeVisible();
  await genreLink.click();
  await expect(page.getByRole('heading', { name: /Anime de /i })).toBeVisible();
});

test('search index, robots and sitemap are generated', async ({ request }) => {
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

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

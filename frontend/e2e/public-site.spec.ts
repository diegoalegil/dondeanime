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

  expect(new URL(page.url()).pathname).toBe(firstHref);
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

test('genre and platform combination pages filter anime and are indexed', async ({ page, request }) => {
  await page.goto('/anime/action/en/crunchyroll');

  await expect(page.getByRole('heading', { name: /Anime de Action en Crunchyroll/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://dondeanime.com/anime/action/en/crunchyroll',
  );

  const animeCards = page.locator('article a[href^="/anime/"]');
  const resultCount = await animeCards.count();
  expect(resultCount).toBeGreaterThan(0);

  const jsonLdBlocks = await page.locator('script[type="application/ld+json"]').allTextContents();
  const itemList = jsonLdBlocks
    .map((block) => JSON.parse(block))
    .find((schema) => schema['@type'] === 'ItemList');

  expect(itemList).toEqual(expect.objectContaining({
    numberOfItems: Math.min(resultCount, 30),
    itemListElement: expect.any(Array),
  }));
  expect(itemList.itemListElement).toHaveLength(Math.min(resultCount, 30));

  const sitemapIndex = await request.get('/sitemap-index.xml');
  expect(sitemapIndex.ok()).toBe(true);
  const sitemapIndexText = await sitemapIndex.text();
  const sitemapPath = sitemapIndexText.match(/https:\/\/dondeanime\.com(\/sitemap-[^<]+\.xml)/)?.[1];
  expect(sitemapPath).toBeTruthy();

  const sitemap = await request.get(sitemapPath!);
  expect(sitemap.ok()).toBe(true);
  const sitemapText = await sitemap.text();
  const comboUrls = new Set(
    [...sitemapText.matchAll(/https:\/\/dondeanime\.com\/anime\/[^/]+\/en\/[^<]+/g)].map((match) => match[0]),
  );

  expect(comboUrls.size).toBe(35);
  expect(comboUrls).toContain('https://dondeanime.com/anime/action/en/crunchyroll');
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

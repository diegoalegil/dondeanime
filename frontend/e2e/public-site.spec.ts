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

test('upcoming release pages render and are indexed', async ({ page, request }) => {
  await page.goto('/estrenos/proxima-semana');
  await expect(page.getByRole('heading', { name: /Estrenos de anime de la próxima semana/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://dondeanime.com/estrenos/proxima-semana',
  );
  await expect(page.locator('script[type="application/ld+json"]').first()).toBeAttached();

  await page.goto('/estrenos/proximo-mes');
  await expect(page.getByRole('heading', { name: /Estrenos de anime del próximo mes/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://dondeanime.com/estrenos/proximo-mes',
  );

  const sitemapIndex = await request.get('/sitemap-index.xml');
  expect(sitemapIndex.ok()).toBe(true);
  const sitemapIndexText = await sitemapIndex.text();
  const sitemapPath = sitemapIndexText.match(/https:\/\/dondeanime\.com(\/sitemap-[^<]+\.xml)/)?.[1];
  expect(sitemapPath).toBeTruthy();

  const sitemap = await request.get(sitemapPath!);
  expect(sitemap.ok()).toBe(true);
  const sitemapText = await sitemap.text();
  expect(sitemapText).toContain('https://dondeanime.com/estrenos/proxima-semana');
  expect(sitemapText).toContain('https://dondeanime.com/estrenos/proximo-mes');
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
  const sitemapPaths = [...sitemapIndexText.matchAll(/https:\/\/dondeanime\.com(\/sitemap-[^<]+\.xml)/g)].map((m) => m[1]);
  expect(sitemapPaths.length).toBeGreaterThan(0);

  const allSitemapText = (
    await Promise.all(
      sitemapPaths.map(async (path) => {
        const sitemap = await request.get(path);
        expect(sitemap.ok()).toBe(true);
        return sitemap.text();
      }),
    )
  ).join('\n');

  const comboUrls = new Set(
    [...allSitemapText.matchAll(/https:\/\/dondeanime\.com\/anime\/[^/]+\/en\/[^<]+/g)].map((match) => match[0]),
  );

  expect(comboUrls.size).toBe(35);
  expect(comboUrls).toContain('https://dondeanime.com/anime/action/en/crunchyroll');
});

test('best anime by year pages render ranking, providers and schema', async ({ page, request }) => {
  await page.goto('/mejores/2024');

  await expect(page.getByRole('heading', { name: /Mejores anime de 2024/i })).toBeVisible();
  const resultCount = await page.locator('[data-best-year-result]').count();
  expect(resultCount).toBeGreaterThan(0);
  expect(resultCount).toBeLessThanOrEqual(30);
  await expect(page.locator('[data-provider-chip]').first()).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://dondeanime.com/mejores/2024',
  );

  const jsonLdBlocks = await page.locator('script[type="application/ld+json"]').allTextContents();
  const itemList = jsonLdBlocks
    .map((block) => JSON.parse(block))
    .find((schema) => schema['@type'] === 'ItemList');

  expect(itemList).toEqual(expect.objectContaining({
    numberOfItems: resultCount,
    itemListElement: expect.any(Array),
  }));
  expect(itemList.itemListElement).toHaveLength(resultCount);

  const sitemapIndex = await request.get('/sitemap-index.xml');
  expect(sitemapIndex.ok()).toBe(true);
  const sitemapIndexText = await sitemapIndex.text();
  const sitemapPaths = [...sitemapIndexText.matchAll(/https:\/\/dondeanime\.com(\/sitemap-[^<]+\.xml)/g)].map((m) => m[1]);
  expect(sitemapPaths.length).toBeGreaterThan(0);

  const allSitemapText = (
    await Promise.all(
      sitemapPaths.map(async (path) => {
        const sitemap = await request.get(path);
        expect(sitemap.ok()).toBe(true);
        return sitemap.text();
      }),
    )
  ).join('\n');

  for (let year = 2010; year <= 2026; year += 1) {
    expect(allSitemapText).toContain(`https://dondeanime.com/mejores/${year}`);
  }
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

test('structured data includes FAQ, organization and anime review schemas', async ({ page }) => {
  await page.goto('/');

  const homeSchemas = await page.locator('script[type="application/ld+json"]').allTextContents();
  const homeJson = homeSchemas.map((schema) => JSON.parse(schema));
  const homeTypes = homeJson.map((schema) => schema['@type']);
  expect(homeTypes).toContain('FAQPage');
  expect(homeTypes).toContain('Organization');

  const faq = homeJson.find((schema) => schema['@type'] === 'FAQPage');
  expect(faq.mainEntity).toHaveLength(5);

  const organization = homeJson.find((schema) => schema['@type'] === 'Organization');
  expect(organization.logo.url).toBe('https://dondeanime.com/og-default.png');
  expect(organization.sameAs.length).toBeGreaterThan(0);

  await page.locator('article a[href^="/anime/"]').first().click();

  const detailSchemas = await page.locator('script[type="application/ld+json"]').allTextContents();
  const detailJson = detailSchemas.map((schema) => JSON.parse(schema));
  const detailTypes = detailJson.map((schema) => schema['@type']);
  expect(detailTypes).toContain('TVSeries');
  expect(detailTypes).toContain('Review');

  const review = detailJson.find((schema) => schema['@type'] === 'Review');
  expect(Number(review.reviewRating.ratingValue)).toBeGreaterThan(0);
  expect(review.author.name).toBe('AniList');
});

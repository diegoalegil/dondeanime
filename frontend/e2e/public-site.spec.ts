import { expect, test, type APIRequestContext } from '@playwright/test';

const expectedPartitionSitemaps = [
  '/sitemap-anime.xml',
  '/sitemap-paises.xml',
  '/sitemap-plataformas.xml',
  '/sitemap-generos.xml',
  '/sitemap-temporadas.xml',
  '/sitemap-mejores.xml',
  '/sitemap-combinatoria.xml',
];

const seasonOrder: Record<string, number> = { winter: 0, spring: 1, summer: 2, fall: 3 };
const seasonLabelPattern: Record<string, string> = {
  winter: 'Invierno',
  spring: 'Primavera',
  summer: 'Verano',
  fall: 'Oto.o',
};

const currentSeasonPath = (date: Date) => {
  const month = date.getUTCMonth() + 1;
  const season = month <= 3 ? 'winter' : month <= 6 ? 'spring' : month <= 9 ? 'summer' : 'fall';
  return `/temporada/${date.getUTCFullYear()}/${season}`;
};

const expectedHomeSeasonPath = async (request: APIRequestContext) => {
  const sitemap = await request.get('/sitemap-temporadas.xml');
  expect(sitemap.ok()).toBe(true);
  const paths = [...(await sitemap.text()).matchAll(/https:\/\/dondeanime\.com(\/temporada\/(\d{4})\/(winter|spring|summer|fall))/g)]
    .map((match) => ({
      path: match[1],
      year: Number(match[2]),
      season: match[3],
    }));

  const current = currentSeasonPath(new Date());
  if (paths.some((item) => item.path === current)) return current;

  return [...paths].sort((a, b) => {
    if (a.year !== b.year) return b.year - a.year;
    return seasonOrder[b.season] - seasonOrder[a.season];
  })[0].path;
};

const sitemapPathsFromIndex = async (request: APIRequestContext, path = '/sitemap-index.xml') => {
  const sitemapIndex = await request.get(path);
  expect(sitemapIndex.ok()).toBe(true);
  const sitemapIndexText = await sitemapIndex.text();

  expect(sitemapIndexText).toContain('<sitemapindex');
  expect(expectedPartitionSitemaps.every((sitemapPath) =>
    sitemapIndexText.includes(`https://dondeanime.com${sitemapPath}`),
  )).toBe(true);

  return [...sitemapIndexText.matchAll(/https:\/\/dondeanime\.com(\/sitemap-[^<]+\.xml)/g)]
    .map((m) => m[1]);
};

const allPartitionedSitemapText = async (request: APIRequestContext) => {
  const sitemapPaths = await sitemapPathsFromIndex(request);
  expect(sitemapPaths).toEqual(expect.arrayContaining(expectedPartitionSitemaps));

  return (
    await Promise.all(
      sitemapPaths.map(async (path) => {
        const sitemap = await request.get(path);
        expect(sitemap.ok()).toBe(true);
        const text = await sitemap.text();
        expect(text).toContain('<urlset');
        return text;
      }),
    )
  ).join('\n');
};

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

test('home highlights the current season or falls back to latest populated season', async ({ page, request }) => {
  const expectedPath = await expectedHomeSeasonPath(request);
  const [, year, season] = expectedPath.match(/^\/temporada\/(\d{4})\/([a-z]+)$/)!;

  await page.goto('/');

  await expect(page.getByRole('heading', {
    name: new RegExp(`Estrenos de ${seasonLabelPattern[season]} ${year}`),
  })).toBeVisible();
  await expect(page.locator(`main a[href="${expectedPath}"]`)).toBeVisible();
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

  const sitemapText = await allPartitionedSitemapText(request);
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

  const allSitemapText = await allPartitionedSitemapText(request);

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

  const allSitemapText = await allPartitionedSitemapText(request);

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

  const sitemapPaths = await sitemapPathsFromIndex(request);
  expect(sitemapPaths).toEqual(expectedPartitionSitemaps);

  const sitemapAlias = await request.get('/sitemap.xml');
  expect(sitemapAlias.ok()).toBe(true);
  expect(await sitemapAlias.text()).toContain('<sitemapindex');

  const animeSitemap = await request.get('/sitemap-anime.xml');
  const animeUrls = [...(await animeSitemap.text()).matchAll(/<url>/g)];
  expect(animeUrls.length).toBeGreaterThanOrEqual(100);

  const countrySitemap = await request.get('/sitemap-paises.xml');
  expect(await countrySitemap.text()).toContain('https://dondeanime.com/pais/espana');

  const platformSitemap = await request.get('/sitemap-plataformas.xml');
  expect(await platformSitemap.text()).toContain('https://dondeanime.com/plataforma/crunchyroll');

  const genreSitemap = await request.get('/sitemap-generos.xml');
  expect(await genreSitemap.text()).toContain('https://dondeanime.com/genero/action');

  const seasonSitemap = await request.get('/sitemap-temporadas.xml');
  expect(await seasonSitemap.text()).toMatch(/https:\/\/dondeanime\.com\/temporada\/\d{4}\/[a-z]+/);
});

test('blog index, article schema and RSS are generated', async ({ page, request }) => {
  await page.goto('/blog');

  await expect(page.getByRole('heading', { name: /Blog DondeAnime/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://dondeanime.com/blog',
  );

  const articleLinks = page.locator('article h2 a[href^="/blog/"]');
  await expect(articleLinks).toHaveCount(2);

  const firstHref = await articleLinks.first().getAttribute('href');
  expect(firstHref).toBe('/blog/placeholder-guia-editorial');

  await articleLinks.first().click();
  await expect(page.getByRole('heading', { name: 'Placeholder editorial 1' })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://dondeanime.com/blog/placeholder-guia-editorial',
  );

  const jsonLdBlocks = await page.locator('script[type="application/ld+json"]').allTextContents();
  const blogPosting = jsonLdBlocks
    .map((block) => JSON.parse(block))
    .find((schema) => schema['@type'] === 'BlogPosting');

  expect(blogPosting).toEqual(expect.objectContaining({
    headline: 'Placeholder editorial 1',
    inLanguage: 'es',
    mainEntityOfPage: expect.objectContaining({
      '@id': 'https://dondeanime.com/blog/placeholder-guia-editorial',
    }),
  }));

  const rss = await request.get('/blog/rss.xml');
  expect(rss.ok()).toBe(true);
  expect(rss.headers()['content-type']).toMatch(/application\/rss\+xml|application\/xml|text\/xml/);
  const rssText = await rss.text();
  expect(rssText).toContain('<rss version="2.0">');
  expect(rssText).toContain('https://dondeanime.com/blog/placeholder-guia-editorial');
  expect(rssText).toContain('https://dondeanime.com/blog/placeholder-lista-editorial');
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

test('search index is loaded lazily when the user searches', async ({ page }) => {
  const searchIndexRequests: string[] = [];
  page.on('request', (request) => {
    if (new URL(request.url()).pathname === '/search-index.json') {
      searchIndexRequests.push(request.url());
    }
  });

  await page.goto('/');
  expect(searchIndexRequests).toHaveLength(0);

  await page.getByPlaceholder('Buscar anime...').fill('naruto');

  await expect(page.locator('[data-search-results] a[href^="/anime/"]').first()).toBeVisible();
  expect(searchIndexRequests).toHaveLength(1);
});

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

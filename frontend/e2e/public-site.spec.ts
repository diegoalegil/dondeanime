import { expect, test, type APIRequestContext } from '@playwright/test';

const expectedPartitionSitemaps = [
  '/sitemap-anime.xml',
  '/sitemap-paises.xml',
  '/sitemap-plataformas.xml',
  '/sitemap-generos.xml',
  '/sitemap-temporadas.xml',
  '/sitemap-mejores.xml',
  '/sitemap-combinatoria.xml',
  '/sitemap-listas.xml',
];

const expectedLanguageSitemaps = [
  '/sitemap-es.xml',
  '/sitemap-en.xml',
];

const expectedSitemapIndexPaths = [
  ...expectedLanguageSitemaps,
  ...expectedPartitionSitemaps,
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
  expect(expectedSitemapIndexPaths.every((sitemapPath) =>
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
  const autoText = page.locator('[data-auto-text]');
  await expect(autoText).toBeVisible();
  expect((await autoText.innerText()).split(/\s+/).length).toBeGreaterThanOrEqual(150);

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

test('duration pages render and are indexed', async ({ page, request }) => {
  await page.goto('/anime/duracion/24');

  await expect(page.getByRole('heading', { name: /Anime de 24 minutos/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://dondeanime.com/anime/duracion/24',
  );

  const jsonLdBlocks = await page.locator('script[type="application/ld+json"]').allTextContents();
  const itemList = jsonLdBlocks
    .map((block) => JSON.parse(block))
    .find((schema) => schema['@type'] === 'ItemList');

  expect(itemList).toEqual(expect.objectContaining({
    name: 'Anime de 24 minutos por capitulo',
    itemListElement: expect.any(Array),
  }));

  const allSitemapText = await allPartitionedSitemapText(request);
  for (const minutes of [12, 22, 24, 25, 45, 60]) {
    expect(allSitemapText).toContain(`https://dondeanime.com/anime/duracion/${minutes}`);
  }
});

test('episode count pages render and are indexed', async ({ page, request }) => {
  await page.goto('/anime/episodios/menos-de-12');

  await expect(page.getByRole('heading', { name: /Anime con 12 episodios o menos/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://dondeanime.com/anime/episodios/menos-de-12',
  );

  const jsonLdBlocks = await page.locator('script[type="application/ld+json"]').allTextContents();
  const itemList = jsonLdBlocks
    .map((block) => JSON.parse(block))
    .find((schema) => schema['@type'] === 'ItemList');

  expect(itemList).toEqual(expect.objectContaining({
    name: 'Anime con 12 episodios o menos',
    itemListElement: expect.any(Array),
  }));

  const allSitemapText = await allPartitionedSitemapText(request);
  for (const maxEpisodes of [12, 24, 50, 100, 200]) {
    expect(allSitemapText).toContain(`https://dondeanime.com/anime/episodios/menos-de-${maxEpisodes}`);
  }
});

test('beginner genre pages render curated recommendations and are indexed', async ({ page, request }) => {
  await page.goto('/empezar/action');

  await expect(page.getByRole('heading', { name: /Anime para principiantes en Action/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://dondeanime.com/empezar/action',
  );

  const resultCount = await page.locator('[data-beginner-result]').count();
  expect(resultCount).toBeGreaterThan(0);
  expect(resultCount).toBeLessThanOrEqual(10);

  const jsonLdBlocks = await page.locator('script[type="application/ld+json"]').allTextContents();
  const itemList = jsonLdBlocks
    .map((block) => JSON.parse(block))
    .find((schema) => schema['@type'] === 'ItemList');

  expect(itemList).toEqual(expect.objectContaining({
    name: 'Anime para principiantes en Action',
    numberOfItems: resultCount,
    itemListElement: expect.any(Array),
  }));
  expect(itemList.itemListElement).toHaveLength(resultCount);

  const allSitemapText = await allPartitionedSitemapText(request);
  expect(allSitemapText).toContain('https://dondeanime.com/empezar/action');
});

test('studio ranking pages render schema and are indexed', async ({ page, request }) => {
  await page.goto('/estudio/madhouse/mejores');

  await expect(page.getByRole('heading', { name: /Mejor anime de Madhouse/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://dondeanime.com/estudio/madhouse/mejores',
  );

  const jsonLdBlocks = await page.locator('script[type="application/ld+json"]').allTextContents();
  const schemas = jsonLdBlocks.map((block) => JSON.parse(block));
  const creativeWorkSeries = schemas.find((schema) => schema['@type'] === 'CreativeWorkSeries');
  const itemList = schemas.find((schema) => schema['@type'] === 'ItemList');

  expect(creativeWorkSeries).toEqual(expect.objectContaining({
    name: 'Mejor anime de Madhouse',
    creator: expect.objectContaining({ name: 'Madhouse' }),
  }));
  expect(itemList).toEqual(expect.objectContaining({
    name: 'Mejor anime de Madhouse',
    itemListElement: expect.any(Array),
  }));

  const allSitemapText = await allPartitionedSitemapText(request);
  expect(allSitemapText).toContain('https://dondeanime.com/estudio/madhouse/mejores');
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
  const robotsText = await robots.text();
  expect(robotsText).toContain('Sitemap: https://dondeanime.com/sitemap-index.xml');
  expect(robotsText).toContain('Sitemap: https://dondeanime.com/sitemap-es.xml');
  expect(robotsText).toContain('Sitemap: https://dondeanime.com/sitemap-en.xml');

  const sitemapPaths = await sitemapPathsFromIndex(request);
  // Comparamos como conjunto (orden indiferente): el orden en el indice no es
  // significativo y cambia al anadir sitemaps nuevos (p.ej. sitemap-listas del Sprint 24).
  expect([...sitemapPaths].sort()).toEqual([...expectedSitemapIndexPaths].sort());

  const sitemapAlias = await request.get('/sitemap.xml');
  expect(sitemapAlias.ok()).toBe(true);
  expect(await sitemapAlias.text()).toContain('<sitemapindex');

  const spanishSitemap = await request.get('/sitemap-es.xml');
  const spanishSitemapText = await spanishSitemap.text();
  expect(spanishSitemap.ok()).toBe(true);
  expect(spanishSitemapText).toContain('https://dondeanime.com/');
  expect(spanishSitemapText).toContain('https://dondeanime.com/blog/placeholder-guia-editorial');
  expect(spanishSitemapText).toContain('https://dondeanime.com/legal/privacidad');

  const englishSitemap = await request.get('/sitemap-en.xml');
  const englishSitemapText = await englishSitemap.text();
  expect(englishSitemap.ok()).toBe(true);
  expect(englishSitemapText).toContain('https://dondeanime.com/en');
  expect(englishSitemapText).toContain('https://dondeanime.com/en/country/spain');
  expect(englishSitemapText).toContain('https://dondeanime.com/en/upcoming/next-week');
  expect(englishSitemapText).toContain('https://dondeanime.com/en/blog/placeholder-guia-editorial');
  expect(englishSitemapText).not.toContain('https://dondeanime.com/en/pais/espana');

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

test('PWA manifest links icons, screenshots and shortcuts', async ({ page, request }) => {
  await page.goto('/');
  await expect(page.locator('link[rel="manifest"]')).toHaveAttribute('href', '/manifest.json');
  await expect(page.locator('meta[name="theme-color"]')).toHaveAttribute('content', '#0A0A0F');

  const manifestResponse = await request.get('/manifest.json');
  expect(manifestResponse.ok()).toBe(true);
  expect(manifestResponse.headers()['content-type']).toContain('application/json');

  const manifest = await manifestResponse.json();
  expect(manifest).toEqual(expect.objectContaining({
    name: 'DondeAnime',
    short_name: 'DondeAnime',
    display: 'standalone',
    start_url: '/',
    scope: '/',
  }));
  expect(manifest.icons.map((icon: { sizes: string }) => icon.sizes)).toEqual(
    expect.arrayContaining(['16x16', '32x32', '192x192', '512x512', '1024x1024']),
  );
  expect(manifest.icons.some((icon: { purpose?: string }) => icon.purpose === 'maskable')).toBe(true);
  expect(manifest.screenshots).toHaveLength(2);
  expect(manifest.shortcuts.map((shortcut: { name: string }) => shortcut.name)).toEqual([
    'Buscar',
    'Mi pais',
    'Alertas',
  ]);

  for (const asset of [...manifest.icons, ...manifest.screenshots]) {
    const assetResponse = await request.get(asset.src);
    expect(assetResponse.ok()).toBe(true);
    expect(assetResponse.headers()['content-type']).toContain('image/svg+xml');
  }
});

test('offline page and service worker are generated', async ({ request }) => {
  const home = await request.get('/');
  expect(home.ok()).toBe(true);
  expect(await home.text()).toContain("navigator.serviceWorker.register('/sw.js')");

  const offline = await request.get('/offline');
  expect(offline.ok()).toBe(true);
  expect(await offline.text()).toContain('No hay conexion disponible');

  const serviceWorker = await request.get('/sw.js');
  expect(serviceWorker.ok()).toBe(true);
  const serviceWorkerText = await serviceWorker.text();
  expect(serviceWorkerText).toContain("const PAGE_CACHE = 'dondeanime-pages-v1'");
  expect(serviceWorkerText).toContain("const OFFLINE_URL = '/offline'");
  expect(serviceWorkerText).toContain('const MAX_CACHED_ANIME_PAGES = 12');
  expect(serviceWorkerText).toContain('const ANIME_PAGE_PATTERN');
  expect(serviceWorkerText).toContain("request.mode === 'navigate'");
});

test('alert background sync is generated in the service worker', async ({ request }) => {
  const serviceWorker = await request.get('/sw.js');
  expect(serviceWorker.ok()).toBe(true);
  const serviceWorkerText = await serviceWorker.text();

  expect(serviceWorkerText).toContain("const ALERT_SYNC_TAG = 'dondeanime-alerts-sync'");
  expect(serviceWorkerText).toContain("const ALERT_DB_NAME = 'dondeanime-alerts'");
  expect(serviceWorkerText).toContain("self.addEventListener('sync'");
  expect(serviceWorkerText).toContain("fetch(alert.endpoint");
  expect(serviceWorkerText).toContain("self.registration.showNotification('Alerta enviada'");
});

test('install promotion banner is generated with visit gate and tracking', async ({ request }) => {
  const home = await request.get('/');
  expect(home.ok()).toBe(true);
  const homeText = await home.text();

  expect(homeText).toContain('data-install-promotion');
  expect(homeText).toContain('Instala DondeAnime como app');
  expect(homeText).toContain('dondeanime-install-visits');
  expect(homeText).toContain('dondeanime-install-dismissed');
  expect(homeText).toContain('install_prompt_shown');
  expect(homeText).toContain('install_completed');
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

test('premium page creates Stripe checkout and customer portal sessions in test mode', async ({ page }) => {
  await page.route('**/api/premium/checkout', async (route) => {
    const corsHeaders = {
      'access-control-allow-origin': '*',
      'access-control-allow-methods': 'POST, OPTIONS',
      'access-control-allow-headers': 'content-type',
    };

    if (route.request().method() === 'OPTIONS') {
      await route.fulfill({ status: 204, headers: corsHeaders });
      return;
    }

    const payload = route.request().postDataJSON();
    expect(payload).toEqual({ email: 'premium@dondeanime.test' });

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      headers: corsHeaders,
      body: JSON.stringify({ url: '/premium?success=1' }),
    });
  });
  await page.route('**/api/premium/portal', async (route) => {
    const corsHeaders = {
      'access-control-allow-origin': '*',
      'access-control-allow-methods': 'POST, OPTIONS',
      'access-control-allow-headers': 'content-type',
    };

    if (route.request().method() === 'OPTIONS') {
      await route.fulfill({ status: 204, headers: corsHeaders });
      return;
    }

    const payload = route.request().postDataJSON();
    expect(payload).toEqual({ email: 'premium@dondeanime.test' });

    // Por seguridad el portal ya no devuelve URL: el backend envia el enlace
    // por email y responde con un acuse generico.
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      headers: corsHeaders,
      body: JSON.stringify({ status: 'sent' }),
    });
  });

  await page.goto('/premium');

  // Si el build no trae la clave publicable de Stripe (pk_test_), Premium no esta
  // configurado en este entorno: omitimos el flujo de checkout en vez de fallar el CI.
  const stripeKey = await page.locator('[data-premium-page]').getAttribute('data-stripe-key');
  test.skip(
    !stripeKey?.startsWith('pk_test_'),
    'Stripe no configurado en el build (sin clave publicable pk_test_).',
  );

  await expect(page.getByRole('heading', { name: 'Premium DondeAnime' })).toBeVisible();
  await expect(page.locator('[data-premium-page]')).toHaveAttribute('data-stripe-key', /^pk_test_/);
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://dondeanime.com/premium',
  );

  await page.getByLabel('Email').fill('premium@dondeanime.test');
  await page.getByRole('button', { name: 'Ir a checkout' }).click();
  await page.waitForURL('**/premium?success=1');
  await expect(page.locator('[data-premium-status]')).toContainText('Premium activo');
  expect(await page.evaluate(() => localStorage.getItem('dondeanime-premium'))).toBe('true');

  await page.getByLabel('Email').fill('premium@dondeanime.test');
  await page.getByRole('button', { name: 'Gestionar suscripción' }).click();
  await expect(page.locator('[data-premium-status]')).toContainText('te hemos enviado un enlace a tu email');
});

test('structured data includes FAQ, organization and anime review schemas', async ({ page }) => {
  await page.goto('/');

  const homeSchemas = await page.locator('script[type="application/ld+json"]').allTextContents();
  const homeJson = homeSchemas.map((schema) => JSON.parse(schema));
  const homeTypes = homeJson.map((schema) => schema['@type']);
  expect(homeTypes).toContain('FAQPage');
  expect(homeTypes).toContain('Organization');
  await expect(page.locator('meta[property="og:locale"]')).toHaveAttribute('content', 'es_ES');
  await expect(page.locator('meta[property="og:locale:alternate"]')).toHaveAttribute('content', 'en_US');

  const faq = homeJson.find((schema) => schema['@type'] === 'FAQPage');
  expect(faq.mainEntity).toHaveLength(5);

  const organization = homeJson.find((schema) => schema['@type'] === 'Organization');
  expect(organization.availableLanguage).toEqual(['es', 'en']);
  expect(organization.logo.url).toBe('https://dondeanime.com/og-default.png');
  expect(organization.sameAs.length).toBeGreaterThan(0);

  await page.locator('article a[href^="/anime/"]').first().click();
  await page.waitForURL(/\/anime\/[^/]+$/);
  await expect(page.getByRole('heading', { name: /Dónde verlo/i })).toBeVisible();
  await expect(page.locator('script[type="application/ld+json"]').first()).toBeAttached();

  const detailSchemas = await page.locator('script[type="application/ld+json"]').allTextContents();
  const detailJson = detailSchemas.map((schema) => JSON.parse(schema));
  const detailTypes = detailJson.map((schema) => schema['@type']);
  expect(detailTypes).toContain('TVSeries');
  expect(detailTypes).toContain('Review');

  const review = detailJson.find((schema) => schema['@type'] === 'Review');
  expect(Number(review.reviewRating.ratingValue)).toBeGreaterThan(0);
  expect(review.author.name).toBe('AniList');
});

test('search results load lazily from the search API when the user searches', async ({ page }) => {
  const searchRequests: string[] = [];
  page.on('request', (request) => {
    if (new URL(request.url()).pathname === '/api/search') {
      searchRequests.push(request.url());
    }
  });

  await page.route('**/api/search**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          slug: 'attack-on-titan',
          titleEnglish: 'Attack on Titan',
          titleRomaji: 'Shingeki no Kyojin',
          format: 'TV',
          year: 2013,
          coverImage: 'https://example.com/cover.jpg',
        },
      ]),
    });
  });

  await page.goto('/');
  expect(searchRequests).toHaveLength(0);

  await page.getByPlaceholder('Buscar anime...').fill('ataque');

  await expect(page.locator('[data-search-results] a[href^="/anime/"]').first()).toBeVisible();
  expect(searchRequests).toHaveLength(1);
});

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

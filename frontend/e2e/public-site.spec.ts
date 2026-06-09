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
  '/sitemap-noticias.xml',
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
  const paths = [...(await sitemap.text()).matchAll(/https:\/\/www\.dondeanime\.com(\/temporada\/(\d{4})\/(winter|spring|summer|fall))/g)]
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
    sitemapIndexText.includes(`https://www.dondeanime.com${sitemapPath}`),
  )).toBe(true);

  return [...sitemapIndexText.matchAll(/https:\/\/www\.dondeanime\.com(\/sitemap-[^<]+\.xml)/g)]
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
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute('href', 'https://www.dondeanime.com/');

  // La home ya no vuelca el catálogo entero (DOM gigante): muestra un teaser
  // de 60 cards más las secciones de tendencia/temporada/género.
  const animeCards = page.locator('article a[href^="/anime/"]');
  expect(await animeCards.count()).toBeGreaterThanOrEqual(60);

  const firstHref = await animeCards.first().getAttribute('href');
  expect(firstHref).toMatch(/^\/anime\/[^/]+$/);

  await animeCards.first().click();

  await expect(page).toHaveURL(new RegExp(`${escapeRegExp(firstHref!)}$`));
  await expect(page.getByRole('heading', { name: /Dónde verlo/i })).toBeVisible();
  await expect(page.locator('script[type="application/ld+json"]').first()).toBeAttached();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    /https:\/\/www\.dondeanime\.com\/anime\/[^/]+$/,
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
    'https://www.dondeanime.com/estrenos/proxima-semana',
  );
  await expect(page.locator('script[type="application/ld+json"]').first()).toBeAttached();

  await page.goto('/estrenos/proximo-mes');
  await expect(page.getByRole('heading', { name: /Estrenos de anime del próximo mes/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://www.dondeanime.com/estrenos/proximo-mes',
  );

  const sitemapText = await allPartitionedSitemapText(request);
  expect(sitemapText).toContain('https://www.dondeanime.com/estrenos/proxima-semana');
  expect(sitemapText).toContain('https://www.dondeanime.com/estrenos/proximo-mes');
});

test('genre and platform combination pages filter anime and are indexed', async ({ page, request }) => {
  await page.goto('/anime/action/en/crunchyroll');

  await expect(page.getByRole('heading', { name: /Anime de Action en Crunchyroll/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://www.dondeanime.com/anime/action/en/crunchyroll',
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
    [...allSitemapText.matchAll(/https:\/\/www\.dondeanime\.com\/anime\/[^/]+\/en\/[^<]+/g)].map((match) => match[0]),
  );

  expect(comboUrls.size).toBe(35);
  expect(comboUrls).toContain('https://www.dondeanime.com/anime/action/en/crunchyroll');
});

test('duration pages render and are indexed', async ({ page, request }) => {
  await page.goto('/anime/duracion/24');

  await expect(page.getByRole('heading', { name: /Anime de 24 minutos/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://www.dondeanime.com/anime/duracion/24',
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
    expect(allSitemapText).toContain(`https://www.dondeanime.com/anime/duracion/${minutes}`);
  }
});

test('episode count pages render and are indexed', async ({ page, request }) => {
  await page.goto('/anime/episodios/menos-de-12');

  await expect(page.getByRole('heading', { name: /Anime con 12 episodios o menos/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://www.dondeanime.com/anime/episodios/menos-de-12',
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
    expect(allSitemapText).toContain(`https://www.dondeanime.com/anime/episodios/menos-de-${maxEpisodes}`);
  }
});

test('beginner genre pages render curated recommendations and are indexed', async ({ page, request }) => {
  await page.goto('/empezar/action');

  await expect(page.getByRole('heading', { name: /Anime para principiantes en Action/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://www.dondeanime.com/empezar/action',
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
  expect(allSitemapText).toContain('https://www.dondeanime.com/empezar/action');
});

test('studio ranking pages render schema and are indexed', async ({ page, request }) => {
  await page.goto('/estudio/madhouse/mejores');

  await expect(page.getByRole('heading', { name: /Mejor anime de Madhouse/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://www.dondeanime.com/estudio/madhouse/mejores',
  );

  const jsonLdBlocks = await page.locator('script[type="application/ld+json"]').allTextContents();
  const schemas = jsonLdBlocks.map((block) => JSON.parse(block));
  const creativeWorkSeries = schemas.find((schema) => schema['@type'] === 'CreativeWorkSeries');
  const itemList = schemas.find((schema) => schema['@type'] === 'ItemList');

  // El nombre del estudio viene de AniList con su casing original (p.ej. "MADHOUSE"),
  // asi que validamos la estructura del schema derivando el nombre, no hardcodeandolo.
  const studioName = creativeWorkSeries?.creator?.name;
  expect(studioName).toBeTruthy();
  expect(creativeWorkSeries).toEqual(expect.objectContaining({
    name: `Mejor anime de ${studioName}`,
    creator: expect.objectContaining({ name: studioName }),
  }));
  expect(itemList).toEqual(expect.objectContaining({
    name: `Mejor anime de ${studioName}`,
    itemListElement: expect.any(Array),
  }));

  const allSitemapText = await allPartitionedSitemapText(request);
  expect(allSitemapText).toContain('https://www.dondeanime.com/estudio/madhouse/mejores');
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
    'https://www.dondeanime.com/mejores/2024',
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
    expect(allSitemapText).toContain(`https://www.dondeanime.com/mejores/${year}`);
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
  expect(robotsText).toContain('Sitemap: https://www.dondeanime.com/sitemap-index.xml');
  expect(robotsText).toContain('Sitemap: https://www.dondeanime.com/sitemap-es.xml');
  expect(robotsText).toContain('Sitemap: https://www.dondeanime.com/sitemap-en.xml');

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
  expect(spanishSitemapText).toContain('https://www.dondeanime.com/');
  expect(spanishSitemapText).toContain('https://www.dondeanime.com/blog/guia-estrenos-anime-streaming');
  expect(spanishSitemapText).toContain('https://www.dondeanime.com/contacto');
  expect(spanishSitemapText).toContain('https://www.dondeanime.com/legal/privacidad');
  expect(spanishSitemapText).toContain('https://www.dondeanime.com/legal/cookies');
  expect(spanishSitemapText).toContain('https://www.dondeanime.com/legal/terminos');

  const englishSitemap = await request.get('/sitemap-en.xml');
  const englishSitemapText = await englishSitemap.text();
  expect(englishSitemap.ok()).toBe(true);
  expect(englishSitemapText).toContain('https://www.dondeanime.com/en');
  expect(englishSitemapText).toContain('https://www.dondeanime.com/en/country/spain');
  expect(englishSitemapText).toContain('https://www.dondeanime.com/en/upcoming/next-week');
  expect(englishSitemapText).toContain('https://www.dondeanime.com/en/blog/guia-estrenos-anime-streaming');
  expect(englishSitemapText).toContain('https://www.dondeanime.com/en/contact');
  expect(englishSitemapText).toContain('https://www.dondeanime.com/en/legal/cookies');
  expect(englishSitemapText).toContain('https://www.dondeanime.com/en/legal/terms');
  expect(englishSitemapText).not.toContain('https://www.dondeanime.com/en/pais/espana');

  const animeSitemap = await request.get('/sitemap-anime.xml');
  const animeUrls = [...(await animeSitemap.text()).matchAll(/<url>/g)];
  expect(animeUrls.length).toBeGreaterThanOrEqual(100);

  const countrySitemap = await request.get('/sitemap-paises.xml');
  expect(await countrySitemap.text()).toContain('https://www.dondeanime.com/pais/espana');

  const platformSitemap = await request.get('/sitemap-plataformas.xml');
  expect(await platformSitemap.text()).toContain('https://www.dondeanime.com/plataforma/crunchyroll');

  const genreSitemap = await request.get('/sitemap-generos.xml');
  expect(await genreSitemap.text()).toContain('https://www.dondeanime.com/genero/action');

  const seasonSitemap = await request.get('/sitemap-temporadas.xml');
  expect(await seasonSitemap.text()).toMatch(/https:\/\/www\.dondeanime\.com\/temporada\/\d{4}\/[a-z]+/);
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
  expect(serviceWorkerText).toContain("const PAGE_CACHE = 'dondeanime-pages-v2'");
  expect(serviceWorkerText).toContain("const OFFLINE_URL = '/offline'");
  expect(serviceWorkerText).toContain('const MAX_CACHED_ANIME_PAGES = 12');
  expect(serviceWorkerText).toContain("const ASSET_CACHE = 'dondeanime-assets-v2'");
  expect(serviceWorkerText).toContain("const IMAGE_CACHE = 'dondeanime-images-v2'");
  expect(serviceWorkerText).toContain('const MAX_CACHED_IMAGES = 120');
  expect(serviceWorkerText).toContain('const ANIME_PAGE_PATTERN');
  expect(serviceWorkerText).toContain("request.destination === 'image'");
  expect(serviceWorkerText).toContain('allowOpaque: true');
  expect(serviceWorkerText).toContain("request.mode === 'navigate'");
  // HTML en navegación suave (ClientRouter) = network-first, no cache-first.
  expect(serviceWorkerText).toContain("accept.includes('text/html')");
  // El SW unificado también gestiona push (antes vivía en push-worker.js).
  expect(serviceWorkerText).toContain("self.addEventListener('push'");
  expect(serviceWorkerText).toContain("self.addEventListener('notificationclick'");
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
  expect(serviceWorkerText).toContain("badge: '/pwa/icons/maskable-icon.svg'");
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
    'https://www.dondeanime.com/blog',
  );

  const articleLinks = page.locator('article h2 a[href^="/blog/"]');
  await expect(articleLinks).toHaveCount(2);

  const firstHref = await articleLinks.first().getAttribute('href');
  expect(firstHref).toBe('/blog/guia-estrenos-anime-streaming');

  await articleLinks.first().click();
  await expect(page.getByRole('heading', { name: 'Guía para seguir estrenos de anime en streaming' })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://www.dondeanime.com/blog/guia-estrenos-anime-streaming',
  );
  await expect(page.getByText('Lorem ipsum')).toHaveCount(0);

  const jsonLdBlocks = await page.locator('script[type="application/ld+json"]').allTextContents();
  const blogPosting = jsonLdBlocks
    .map((block) => JSON.parse(block))
    .find((schema) => schema['@type'] === 'BlogPosting');

  expect(blogPosting).toEqual(expect.objectContaining({
    headline: 'Guía para seguir estrenos de anime en streaming',
    inLanguage: 'es',
    mainEntityOfPage: expect.objectContaining({
      '@id': 'https://www.dondeanime.com/blog/guia-estrenos-anime-streaming',
    }),
  }));

  const rss = await request.get('/blog/rss.xml');
  expect(rss.ok()).toBe(true);
  expect(rss.headers()['content-type']).toMatch(/application\/rss\+xml|application\/xml|text\/xml/);
  const rssText = await rss.text();
  expect(rssText).toContain('<rss version="2.0">');
  expect(rssText).toContain('https://www.dondeanime.com/blog/guia-estrenos-anime-streaming');
  expect(rssText).toContain('https://www.dondeanime.com/blog/como-elegir-plataforma-anime');
  expect(rssText).not.toContain('placeholder');

  await page.goto('/en/blog');
  await expect(page.getByRole('heading', { name: /Blog DondeAnime/i })).toBeVisible();

  const englishArticleLinks = page.locator('article h2 a[href^="/en/blog/"]');
  await expect(englishArticleLinks).toHaveCount(2);
  expect(await englishArticleLinks.first().getAttribute('href')).toBe('/en/blog/guia-estrenos-anime-streaming');

  await englishArticleLinks.first().click();
  await expect(page.getByRole('heading', { name: 'How to track new anime premieres on streaming' })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://www.dondeanime.com/en/blog/guia-estrenos-anime-streaming',
  );

  const englishJsonLdBlocks = await page.locator('script[type="application/ld+json"]').allTextContents();
  const englishBlogPosting = englishJsonLdBlocks
    .map((block) => JSON.parse(block))
    .find((schema) => schema['@type'] === 'BlogPosting');

  expect(englishBlogPosting).toEqual(expect.objectContaining({
    headline: 'How to track new anime premieres on streaming',
    inLanguage: 'en',
    mainEntityOfPage: expect.objectContaining({
      '@id': 'https://www.dondeanime.com/en/blog/guia-estrenos-anime-streaming',
    }),
  }));

  const englishRss = await request.get('/en/blog/rss.xml');
  expect(englishRss.ok()).toBe(true);
  const englishRssText = await englishRss.text();
  expect(englishRssText).toContain('https://www.dondeanime.com/en/blog/guia-estrenos-anime-streaming');
  expect(englishRssText).toContain('How to track new anime premieres on streaming');
});

test('contact and legal pages render with localized alternates and sitemap entries', async ({ page, request }) => {
  const pages = [
    {
      path: '/contacto',
      heading: /Contacto/i,
      canonical: 'https://www.dondeanime.com/contacto',
      alternate: 'https://www.dondeanime.com/en/contact',
    },
    {
      path: '/legal/cookies',
      heading: /Cookies/i,
      canonical: 'https://www.dondeanime.com/legal/cookies',
      alternate: 'https://www.dondeanime.com/en/legal/cookies',
    },
    {
      path: '/legal/terminos',
      heading: /Términos de uso/i,
      canonical: 'https://www.dondeanime.com/legal/terminos',
      alternate: 'https://www.dondeanime.com/en/legal/terms',
    },
  ];

  for (const item of pages) {
    await page.goto(item.path);
    await expect(page.getByRole('heading', { name: item.heading })).toBeVisible();
    await expect(page.locator('link[rel="canonical"]')).toHaveAttribute('href', item.canonical);
    await expect(page.locator('link[hreflang="en"]')).toHaveAttribute('href', item.alternate);
  }

  await page.goto('/en/contact');
  await expect(page.getByRole('heading', { name: /Contact/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://www.dondeanime.com/en/contact',
  );
  await expect(page.locator('link[hreflang="es"]')).toHaveAttribute('href', 'https://www.dondeanime.com/contacto');

  const spanishSitemapText = await (await request.get('/sitemap-es.xml')).text();
  expect(spanishSitemapText).toContain('https://www.dondeanime.com/contacto');
  expect(spanishSitemapText).toContain('https://www.dondeanime.com/legal/cookies');
  expect(spanishSitemapText).toContain('https://www.dondeanime.com/legal/terminos');

  const englishSitemapText = await (await request.get('/sitemap-en.xml')).text();
  expect(englishSitemapText).toContain('https://www.dondeanime.com/en/contact');
  expect(englishSitemapText).toContain('https://www.dondeanime.com/en/legal/cookies');
  expect(englishSitemapText).toContain('https://www.dondeanime.com/en/legal/terms');
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
    // El checkout incluye sourceListSlug (Sprint 24): vacio cuando no se viene de una lista.
    expect(payload).toEqual({ email: 'premium@dondeanime.test', sourceListSlug: '' });

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
  await page.route('**/api/premium/access-link', async (route) => {
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
      body: JSON.stringify({ status: 'email_sent' }),
    });
  });
  await page.route('**/api/premium/status', async (route) => {
    const corsHeaders = {
      'access-control-allow-origin': '*',
      'access-control-allow-methods': 'GET, OPTIONS',
      'access-control-allow-headers': 'authorization, content-type',
    };

    if (route.request().method() === 'OPTIONS') {
      await route.fulfill({ status: 204, headers: corsHeaders });
      return;
    }

    expect(route.request().headers().authorization).toBe('Bearer premium-token');

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      headers: corsHeaders,
      body: JSON.stringify({ premium: true, planTier: 'PREMIUM', expiresAt: '2026-06-30T00:00:00Z' }),
    });
  });

  await page.goto('/premium');

  const stripeKey = await page.locator('[data-premium-page]').getAttribute('data-stripe-key');
  test.skip(
    Boolean(stripeKey) && !stripeKey?.startsWith('pk_test_') && !stripeKey?.startsWith('pk_live_'),
    'Stripe trae una clave publicable con formato no valido.',
  );

  await expect(page.getByRole('heading', { name: 'Premium DondeAnime' })).toBeVisible();
  await expect(page.locator('[data-premium-page]')).toHaveAttribute('data-stripe-key', /^$|^pk_(test|live)_/);
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://www.dondeanime.com/premium',
  );

  await page.getByLabel('Email').fill('premium@dondeanime.test');
  await page.getByRole('button', { name: 'Ir a checkout' }).click();
  await page.waitForURL('**/premium?success=1');
  await expect(page.locator('[data-premium-status]')).toContainText('webhook confirme');
  expect(await page.evaluate(() => localStorage.getItem('dondeanime-premium'))).toBeNull();

  await page.getByLabel('Email').fill('premium@dondeanime.test');
  await page.getByRole('button', { name: 'Gestionar suscripción' }).click();
  await expect(page.locator('[data-premium-status]')).toContainText('te hemos enviado un enlace a tu email');

  await page.getByLabel('Email').fill('premium@dondeanime.test');
  await page.getByRole('button', { name: 'Activar en este navegador' }).click();
  await expect(page.locator('[data-premium-status]')).toContainText('enlace de acceso a tu email');

  await page.goto('/premium?access_token=premium-token');
  await expect(page.locator('[data-premium-status]')).toContainText('Premium activo');
  expect(await page.evaluate(() => localStorage.getItem('dondeanime-premium-token'))).toBe('premium-token');
});

test('premium page has a real English route', async ({ browser }) => {
  const context = await browser.newContext({ locale: 'en-US' });
  const page = await context.newPage();

  // Ya no hay redirección automática por navigator.language; la ruta inglesa
  // existe y se llega a ella por el selector de idioma o enlace directo.
  await page.goto('/en/premium');

  await expect(page).toHaveURL(/\/en\/premium$/);
  await expect(page.getByRole('heading', { name: 'DondeAnime Premium' })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://www.dondeanime.com/en/premium',
  );

  await context.close();
});

test('structured data includes FAQ, organization and TVSeries schemas', async ({ page }) => {
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
  expect(organization.logo.url).toBe('https://www.dondeanime.com/og-default.png');
  expect(organization.sameAs.length).toBeGreaterThan(0);

  await page.locator('article a[href^="/anime/"]').first().click();
  await page.waitForURL(/\/anime\/[^/]+$/);
  await expect(page.getByRole('heading', { name: /Dónde verlo/i })).toBeVisible();
  await expect(page.locator('script[type="application/ld+json"]').first()).toBeAttached();

  const detailSchemas = await page.locator('script[type="application/ld+json"]').allTextContents();
  const detailJson = detailSchemas.map((schema) => JSON.parse(schema));
  const detailTypes = detailJson.map((schema) => schema['@type']);
  expect(detailTypes).toContain('TVSeries');
  // Sin Review templated de "AniList" (spam de datos estructurados) ni
  // aggregateRating sin ratingCount real: Google los penaliza.
  expect(detailTypes).not.toContain('Review');

  const tvSeries = detailJson.find((schema) => schema['@type'] === 'TVSeries');
  expect(tvSeries.aggregateRating).toBeUndefined();
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

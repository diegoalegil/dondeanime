import { expect, test } from '@playwright/test';

const testBaseURL = `http://127.0.0.1:${Number(process.env.PLAYWRIGHT_PORT ?? 4321)}`;

const expectLocaleAlternates = async (
  page: import('@playwright/test').Page,
  expected: { es: string; en: string; default: string },
) => {
  await expect(page.locator('link[rel="alternate"][hreflang="es"]')).toHaveAttribute('href', expected.es);
  await expect(page.locator('link[rel="alternate"][hreflang="en"]')).toHaveAttribute('href', expected.en);
  await expect(page.locator('link[rel="alternate"][hreflang="x-default"]')).toHaveAttribute('href', expected.default);
};

test('English routes render translated UI under /en with locale alternates', async ({ page }) => {
  await page.goto('/en');

  await expect(page.locator('html')).toHaveAttribute('lang', 'en');
  await expect(page.getByRole('heading', { name: /Find where to watch/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute('href', 'https://www.dondeanime.com/en');
  await expect(page.locator('meta[property="og:locale"]')).toHaveAttribute('content', 'en_US');
  await expect(page.locator('meta[property="og:locale:alternate"]')).toHaveAttribute('content', 'es_ES');
  await expectLocaleAlternates(page, {
    es: 'https://www.dondeanime.com/',
    en: 'https://www.dondeanime.com/en',
    default: 'https://www.dondeanime.com/',
  });

  await expect(page.locator('main a[href="/en/country/spain"]')).toBeVisible();
  await expect(page.locator('article a[href^="/en/anime/"]').first()).toBeVisible();
});

test('English country, platform and combo routes map back to Spanish canonicals', async ({ page }) => {
  await page.goto('/en/country/spain');

  await expect(page.getByRole('heading', { name: /Anime streaming in Spain/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://www.dondeanime.com/en/country/spain',
  );
  await expectLocaleAlternates(page, {
    es: 'https://www.dondeanime.com/pais/espana',
    en: 'https://www.dondeanime.com/en/country/spain',
    default: 'https://www.dondeanime.com/pais/espana',
  });

  await page.goto('/en/platform/crunchyroll');
  await expect(page.getByRole('heading', { name: /Anime on Crunchyroll/i })).toBeVisible();
  await expect(page.locator('a[href^="/en/platform/crunchyroll/"]').first()).toBeVisible();

  await page.goto('/en/anime/action/on/crunchyroll');
  await expect(page.getByRole('heading', { name: /Anime in Action on Crunchyroll/i })).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://www.dondeanime.com/en/anime/action/on/crunchyroll',
  );
  await expectLocaleAlternates(page, {
    es: 'https://www.dondeanime.com/anime/action/en/crunchyroll',
    en: 'https://www.dondeanime.com/en/anime/action/on/crunchyroll',
    default: 'https://www.dondeanime.com/anime/action/en/crunchyroll',
  });
});

test('language switcher stores a manual preference cookie', async ({ page, context }) => {
  await context.clearCookies();
  await page.goto('/en');

  await expect(page.getByRole('link', { name: 'English' })).toHaveAttribute('aria-current', 'true');
  await expect(page.getByRole('link', { name: 'Spanish' })).toHaveAttribute('href', '/');

  await page.getByRole('link', { name: 'Spanish' }).click();
  await page.waitForURL(`${testBaseURL}/`);

  const cookies = await context.cookies();
  expect(cookies).toEqual(expect.arrayContaining([
    expect.objectContaining({ name: 'dondeanime-locale', value: 'es' }),
  ]));

  await page.goto('/en');
  await page.waitForURL(`${testBaseURL}/`);
  await expect(page.locator('html')).toHaveAttribute('lang', 'es');
});

test('browser locale alone does not redirect (Googlebot renders with en-US)', async ({ browser }) => {
  const context = await browser.newContext({ locale: 'en-US' });
  const page = await context.newPage();

  // Sin cookie de preferencia explícita NO hay redirección automática: la
  // home ES se queda en ES aunque el navegador anuncie inglés.
  await page.goto(`${testBaseURL}/`);
  await expect(page.locator('html')).toHaveAttribute('lang', 'es');
  await expect(page).toHaveURL(`${testBaseURL}/`);

  await context.close();
});

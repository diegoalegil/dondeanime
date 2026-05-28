import { expect, test } from '@playwright/test';

test.afterEach(async ({ context }) => {
  await context.setOffline(false);
});

test('anime detail renders offline after the service worker caches it', async ({ context, page }) => {
  await page.goto('/');

  await expect(page.getByRole('heading', { name: /Encuentra dónde ver/i })).toBeVisible();
  await expect(page.locator('link[rel="manifest"]')).toHaveAttribute('href', /manifest\.json/);

  await page.evaluate(async () => {
    if (!('serviceWorker' in navigator)) {
      throw new Error('Service workers no están disponibles');
    }
    await navigator.serviceWorker.ready;
  });

  await page.reload({ waitUntil: 'domcontentloaded' });
  await page.waitForFunction(() => navigator.serviceWorker.controller !== null);

  const detailLink = await page.locator('article a[href^="/anime/"]').first().getAttribute('href');
  expect(detailLink).toMatch(/^\/anime\/[a-z0-9-]+$/);
  await page.goto(detailLink!);
  const detailHeading = page.locator('h1').first();
  await expect(detailHeading).toBeVisible();
  const title = await detailHeading.textContent();

  await context.setOffline(true);
  await page.reload({ waitUntil: 'domcontentloaded' });

  await expect(page.locator('h1').first()).toContainText(title?.trim() ?? '');
});

import { expect, test } from '@playwright/test';

test.afterEach(async ({ context }) => {
  await context.setOffline(false);
});

test('home renders offline after the service worker is installed', async ({ context, page }) => {
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

  await context.setOffline(true);
  await page.reload({ waitUntil: 'domcontentloaded' });

  await expect(page.getByRole('heading', { name: /Encuentra dónde ver/i })).toBeVisible();
  await expect(page.locator('article a[href^="/anime/"]').first()).toBeVisible();
});

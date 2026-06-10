import { expect, test } from '@playwright/test';

test('conecta una cuenta fake de Trakt, oculta vistos y limpia el filtro', async ({ page }) => {
  await page.route('**/api/trakt/watched', async (route) => {
    expect(route.request().headers()['authorization']).toBe('Bearer fake-signed-token');
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ slugs: ['attack-on-titan'] }),
    });
  });

  await page.goto('/buscar');

  const watchedCard = page.locator('[data-anime-filter-item][data-anime-slug="attack-on-titan"]').first();
  await expect(watchedCard).toBeVisible();

  // La isla Svelte pierde el valor si el fill llega antes de hidratar
  // (Astro quita el atributo ssr al completar la hidratación).
  await page.waitForSelector('astro-island[component-url*="WatchedFilter"]:not([ssr])');
  await page.getByLabel('Token de Trakt').fill('fake-signed-token');
  await page.getByRole('button', { name: 'Conectar' }).click();
  await expect(page.locator('[data-trakt-filter-status]')).toContainText('1 vistos');

  await page.getByLabel('Ocultar vistos').check();
  await expect(watchedCard).toBeHidden();

  await page.getByRole('button', { name: 'Limpiar' }).click();
  await expect(watchedCard).toBeVisible();
});

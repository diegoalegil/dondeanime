import { expect, test } from '@playwright/test';

test('header search autocompletes via API and navigates on result click', async ({ page }) => {
  await page.route('**/api/search**', async (route) => {
    const url = new URL(route.request().url());
    expect(url.searchParams.get('q')).toBe('ataque');
    expect(url.searchParams.get('limit')).toBe('5');

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

  const form = page.locator('form[action="/buscar"][method="get"]').first();
  await expect(form).toBeVisible();

  await page.keyboard.press('/');
  const input = page.getByRole('combobox', { name: /Buscar anime/i });
  await expect(input).toBeFocused();

  await input.fill('ataque');

  const result = page.getByRole('option', { name: /Attack on Titan/i });
  await expect(result).toBeVisible();

  await result.click();
  await expect(page).toHaveURL(/\/anime\/attack-on-titan$/);
});

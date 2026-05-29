import { expect, test } from '@playwright/test';

test('curated lists marketplace renders filter shell', async ({ page }) => {
  await page.goto('/listas');

  await expect(page.getByRole('heading', { name: 'Listas curadas de anime' })).toBeVisible();
  await expect(page.getByLabel('Género')).toBeVisible();
  await expect(page.getByLabel('País')).toBeVisible();
  await expect(page.getByLabel('Plataforma')).toBeVisible();
  await expect(page.getByLabel('Duración')).toBeVisible();

  await page.getByLabel('Duración').selectOption('corta');
  await expect(page).toHaveURL(/duracion=corta/);
  await expect(page.locator('[data-list-count]')).toBeVisible();

  await page.getByRole('button', { name: 'Limpiar' }).click();
  await expect(page).toHaveURL(/\/listas$/);
});

test('curated list sitemap is exposed', async ({ page }) => {
  const response = await page.goto('/sitemap-listas.xml');
  expect(response?.ok()).toBeTruthy();
  await expect(page.locator('body')).toContainText('/listas');
});

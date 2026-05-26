import { expect, test } from '@playwright/test';

test('España lista plataformas y permite abrir un catálogo de provider con anime', async ({ page }) => {
  await page.goto('/pais/espana');

  await expect(page.getByRole('heading', { name: /Anime en streaming en España/i })).toBeVisible();

  const providerLinks = page.locator('a[href^="/plataforma/"][href$="/espana"]');
  expect(await providerLinks.count()).toBeGreaterThan(0);

  const firstHref = await providerLinks.first().getAttribute('href');
  expect(firstHref).toMatch(/^\/plataforma\/[^/]+\/espana$/);

  await providerLinks.first().click();

  await expect(page).toHaveURL(new RegExp(`${escapeRegExp(firstHref!)}$`));
  await expect(page.getByRole('heading', { name: /Anime en .*España/i })).toBeVisible();
  await expect(page.locator('article a[href^="/anime/"]').first()).toBeVisible();
});

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

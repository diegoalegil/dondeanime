import { expect, test } from '@playwright/test';

test('anime covers use responsive picture markup and first mobile row is eager', async ({ page }) => {
  await page.goto('/');

  const cards = page.locator('article a[href^="/anime/"]');
  expect(await cards.count()).toBeGreaterThanOrEqual(100);

  const firstHref = await cards.first().getAttribute('href');
  expect(firstHref).toMatch(/^\/anime\/[^/]+$/);

  const firstImage = cards.first().locator('picture img');
  await expect(firstImage).toHaveAttribute('loading', 'eager');
  await expect(firstImage).toHaveAttribute('decoding', 'async');
  await expect(firstImage).toHaveAttribute('fetchpriority', 'high');
  await expect(firstImage).toHaveAttribute('width', '320');
  await expect(firstImage).toHaveAttribute('height', '480');

  const secondImage = cards.nth(1).locator('picture img');
  await expect(secondImage).toHaveAttribute('loading', 'eager');
  await expect(secondImage).toHaveAttribute('decoding', 'async');
  await expect(secondImage).toHaveAttribute('fetchpriority', 'high');

  const thirdImage = cards.nth(2).locator('picture img');
  await expect(thirdImage).toHaveAttribute('loading', 'lazy');
  await expect(thirdImage).toHaveAttribute('decoding', 'async');
  await expect(thirdImage).not.toHaveAttribute('fetchpriority', 'high');

  await page.goto(firstHref!);

  const heroCover = page.locator('picture img[fetchpriority="high"]').first();
  await expect(heroCover).toBeVisible();
  await expect(heroCover).toHaveAttribute('loading', 'eager');
  await expect(heroCover).toHaveAttribute('decoding', 'async');
});

import { expect, test } from '@playwright/test';

test('view transitions show skeletons between home, detail and country pages', async ({ page }, testInfo) => {
  await page.addInitScript(() => {
    const state = window as typeof window & { __astroTransitionEvents?: string[] };
    state.__astroTransitionEvents = [];

    for (const eventName of ['astro:before-preparation', 'astro:after-swap', 'astro:page-load']) {
      document.addEventListener(eventName, () => {
        state.__astroTransitionEvents?.push(eventName);
      });
    }
  });

  await page.goto('/');

  await expect(page.locator('meta[name="astro-view-transitions-enabled"]')).toHaveCount(1);
  await expect(page.locator('[data-navigation-skeleton]')).toBeHidden();
  await page.screenshot({ path: testInfo.outputPath('home-before-transition.png'), fullPage: false });

  const firstCard = page.locator('article a[href^="/anime/"]').first();
  const firstHref = await firstCard.getAttribute('href');
  expect(firstHref).toMatch(/^\/anime\/[^/]+$/);

  await page.route(`**${firstHref}`, async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 250));
    await route.continue();
  }, { times: 1 });

  const clickPromise = firstCard.click();
  await expect(page.locator('[data-navigation-skeleton]')).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath('skeleton-during-transition.png'), fullPage: false });
  await clickPromise;

  await expect(page).toHaveURL(new RegExp(`${escapeRegExp(firstHref!)}$`));
  await expect(page.locator('h1').first()).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath('detail-after-transition.png'), fullPage: false });

  await page.getByRole('link', { name: 'Países' }).click();
  await expect(page.getByRole('heading', { name: /Anime en streaming en España/i })).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath('country-after-transition.png'), fullPage: false });

  const transitionEvents = await page.evaluate(() => {
    const state = window as typeof window & { __astroTransitionEvents?: string[] };
    return state.__astroTransitionEvents ?? [];
  });
  expect(transitionEvents.filter((eventName) => eventName === 'astro:before-preparation').length)
    .toBeGreaterThanOrEqual(2);
  expect(transitionEvents).toContain('astro:after-swap');
});

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

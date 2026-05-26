import { expect, test, type Page } from '@playwright/test';

const visibleCards = (page: Page) => page.locator('[data-filter-card]:not(.hidden)');

const pickReducingGenre = async (page: Page) =>
  page.locator('[data-filter-card]').evaluateAll((cards) => {
    const total = cards.length;
    const counts = new Map<string, number>();

    for (const card of cards) {
      const genres = ((card as HTMLElement).dataset.genres ?? '').split(' ').filter(Boolean);
      for (const genre of genres) {
        counts.set(genre, (counts.get(genre) ?? 0) + 1);
      }
    }

    return [...counts.entries()].find(([, count]) => count > 0 && count < total)?.[0] ?? '';
  });

const pickReducingScore = async (page: Page) =>
  page.locator('[data-filter-card]').evaluateAll((cards) => {
    const total = cards.length;
    const scores = cards.map((card) => Number((card as HTMLElement).dataset.score || 0));

    return [80, 70, 60]
      .map((threshold) => ({
        threshold,
        count: scores.filter((score) => score >= threshold).length,
      }))
      .find(({ count }) => count < total)?.threshold.toString() ?? '80';
  });

const firstYear = async (page: Page) =>
  page.locator('[data-filter-year] option').evaluateAll((options) =>
    options
      .map((option) => (option as HTMLOptionElement).value)
      .find((value) => value.length > 0) ?? '',
  );

test('country page filters by genre and adds noindex after two active filters', async ({ page }) => {
  await page.goto('/pais/espana');

  const total = await page.locator('[data-filter-card]').count();
  expect(total).toBeGreaterThan(0);

  const genre = await pickReducingGenre(page);
  expect(genre).not.toBe('');

  await page.locator('[data-filter-genre]').selectOption(genre);
  expect(new URL(page.url()).searchParams.get('genero')).toBe(genre);
  await expect.poll(async () => visibleCards(page).count()).toBeLessThan(total);

  const year = await firstYear(page);
  expect(year).not.toBe('');

  await page.locator('[data-filter-year]').selectOption(year);
  expect(new URL(page.url()).searchParams.get('year')).toBe(year);
  await expect(page.locator('meta[name="robots"][content="noindex"]')).toHaveCount(1);
});

test('platform country page filters by minimum score', async ({ page }) => {
  await page.goto('/plataforma/crunchyroll/espana');

  const total = await page.locator('[data-filter-card]').count();
  expect(total).toBeGreaterThan(0);

  const score = await pickReducingScore(page);
  await page.locator('[data-filter-score]').selectOption(score);

  expect(new URL(page.url()).searchParams.get('score')).toBe(score);
  await expect.poll(async () => visibleCards(page).count()).toBeLessThan(total);
});

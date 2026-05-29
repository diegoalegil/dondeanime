import { expect, test } from '@playwright/test';

interface SearchEntry {
  slug: string;
  en: string | null;
  jp: string | null;
}

test('home carga, el buscador filtra y un resultado abre la ficha', async ({ page, request }) => {
  const indexResponse = await request.get('/search-index.json');
  expect(indexResponse.ok()).toBe(true);
  const index = await indexResponse.json() as SearchEntry[];
  const target = index.find((entry) => entry.slug === 'attack-on-titan')
    ?? index.find((entry) => entry.slug && (entry.en || entry.jp || entry.slug).length >= 4);
  expect(target).toBeTruthy();
  const targetTitle = target!.en || target!.jp || target!.slug;

  await page.goto('/');

  await expect(page).toHaveTitle(/DondeAnime/);
  await expect(page.getByRole('heading', { name: /Encuentra dónde ver/i })).toBeVisible();
  await expect(page.locator('article a[href^="/anime/"]').first()).toBeVisible();

  await page.getByLabel('Buscar anime').fill(target!.slug);

  const result = page.locator(`[data-search-results] a[href="/anime/${target!.slug}"]`).first();
  await expect(result).toBeVisible();
  await result.click();

  await expect(page).toHaveURL(new RegExp(`/anime/${escapeRegExp(target!.slug)}$`));
  // .first(): la ficha lista anime relacionados cuyo título contiene el del anime
  // (p.ej. "Attack on Titan: Final Season"), así que getByRole por nombre encuentra
  // varios headings. El H1 principal es el primero en el DOM.
  await expect(page.getByRole('heading', { name: targetTitle }).first()).toBeVisible();
  await expect(page.getByRole('heading', { name: /Dónde verlo/i })).toBeVisible();
});

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

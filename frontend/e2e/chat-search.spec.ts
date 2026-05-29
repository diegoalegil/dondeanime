import { expect, test } from '@playwright/test';

const chatResponse = {
  answer: 'He encontrado una recomendacion que encaja con tu busqueda.',
  recommendations: [
    {
      anime: {
        anilistId: 16498,
        slug: 'attack-on-titan',
        titleEnglish: 'Attack on Titan',
        titleRomaji: 'Shingeki no Kyojin',
        format: 'TV',
        status: 'FINISHED',
        episodes: 25,
        episodeDuration: 24,
        studio: 'Wit Studio',
        year: 2013,
        averageScore: 85,
        popularity: 999999,
        coverImage: 'https://example.com/attack-on-titan.jpg',
        genres: ['Action', 'Drama'],
        season: 'SPRING',
        seasonYear: 2013,
      },
      canonicalUrl: 'https://dondeanime.com/anime/attack-on-titan',
      explanation: 'Coincide por tono oscuro, ritmo alto y disponibilidad filtrada.',
    },
  ],
};

test('el chatbot de la home devuelve recomendaciones enlazables', async ({ page }) => {
  await page.route('**/api/chat/search', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(chatResponse),
    });
  });

  await page.goto('/');

  await page.getByLabel('Pregunta para el buscador de anime').fill('Quiero algo oscuro y corto');
  await page.getByRole('button', { name: /Buscar recomendaciones/i }).click();

  await expect(page.getByText(chatResponse.answer)).toBeVisible();

  const recommendation = page.locator('[data-chat-recommendation]').first();
  await expect(recommendation.getByText('Attack on Titan')).toBeVisible();
  await expect(recommendation.getByText(/tono oscuro/i)).toBeVisible();
  await expect(recommendation).toHaveAttribute('href', '/anime/attack-on-titan');

  await recommendation.click();
  await expect(page).toHaveURL(/\/anime\/attack-on-titan$/);
});

test('la pagina de busqueda tambien expone el chatbot', async ({ page }) => {
  await page.goto('/buscar');

  await expect(page.getByRole('heading', { name: 'Buscar anime' })).toBeVisible();
  await expect(page.getByRole('heading', { name: /Pide una recomendacion concreta/i })).toBeVisible();
  await expect(page.getByLabel('Pregunta para el buscador de anime')).toBeVisible();
});

import { expect, test } from '@playwright/test';

test('admin creates, adds anime and publishes a curated list', async ({ page }) => {
  const apiBase = (process.env.PUBLIC_API_URL ?? 'https://api.dondeanime.com').replace(/\/$/, '');
  let lists: any[] = [];

  await page.route(`${apiBase}/api/admin/login`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        token: 'jwt-token-for-lists',
        tokenType: 'Bearer',
        expiresAt: '2026-05-27T20:00:00Z',
      }),
    });
  });

  await page.route(`${apiBase}/api/admin/lists`, async (route) => {
    const request = route.request();
    if (request.method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(lists),
      });
      return;
    }

    const body = request.postDataJSON();
    const saved = {
      slug: body.slug || 'anime-para-empezar',
      title: body.title,
      description: body.description,
      owner: body.owner,
      visibility: body.visibility,
      status: body.status,
      items: [],
    };
    lists = [saved];
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(saved),
    });
  });

  await page.route(`${apiBase}/api/admin/lists/anime-para-empezar/items`, async (route) => {
    const body = route.request().postDataJSON();
    lists[0] = {
      ...lists[0],
      items: [{
        animeSlug: body.animeSlug,
        position: 1,
        note: body.note,
        anime: {
          slug: body.animeSlug,
          titleEnglish: 'Frieren',
          titleRomaji: 'Sousou no Frieren',
        },
      }],
    };
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(lists[0]),
    });
  });

  await page.route(`${apiBase}/api/admin/lists/anime-para-empezar/items/frieren-beyond-journeys-end/up`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(lists[0]),
    });
  });

  await page.route(`${apiBase}/api/admin/lists/anime-para-empezar/publish`, async (route) => {
    lists[0] = { ...lists[0], status: 'PUBLISHED', visibility: 'PUBLIC' };
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(lists[0]),
    });
  });

  await page.goto('/admin/lists');
  await page.getByLabel('Contraseña').fill('secret');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.locator('[data-status]')).toHaveText('Listas cargadas.');

  await page.getByLabel('Titulo').fill('Anime para empezar');
  await page.getByLabel('Descripcion').fill('Lista curada para nuevos lectores.');
  await page.getByRole('button', { name: 'Guardar lista' }).click();
  await expect(page.getByRole('button', { name: /Anime para empezar/ })).toBeVisible();

  await page.getByLabel('Anime slug').fill('frieren-beyond-journeys-end');
  await page.getByLabel('Nota').fill('Fantasia moderna.');
  await page.getByRole('button', { name: 'Añadir' }).click();
  await expect(page.getByText('1. Frieren')).toBeVisible();

  await page.getByRole('button', { name: 'Subir' }).click();
  await page.getByRole('button', { name: 'Publicar' }).click();
  await expect(page.getByText('PUBLISHED · PUBLIC')).toBeVisible();
});

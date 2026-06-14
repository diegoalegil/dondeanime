import { expect, test } from '@playwright/test';

test('home prioritizes the hero image; catalog covers use responsive markup and defer', async ({ page }) => {
  await page.goto('/');

  // El LCP de la home es la imagen del hero (mascota), que va arriba del
  // pliegue. Las portadas del catálogo quedan debajo del hero, así que se
  // cargan en diferido para no robarle prioridad al recurso LCP real.
  const hero = page.locator('img[fetchpriority="high"]').first();
  await expect(hero).toBeVisible();
  await expect(hero).toHaveAttribute('loading', 'eager');
  await expect(hero).toHaveAttribute('decoding', 'async');

  // La home muestra un teaser del catálogo (60 cards) más las secciones
  // de tendencia/temporada/género; el catálogo completo vive en /buscar.
  const cards = page.locator('article a[href^="/anime/"]');
  expect(await cards.count()).toBeGreaterThanOrEqual(60);

  const firstHref = await cards.first().getAttribute('href');
  expect(firstHref).toMatch(/^\/anime\/[^/]+$/);

  // Las portadas conservan markup responsive (srcset/sizes + dimensiones para
  // evitar CLS) pero NO son prioritarias en la home: cargan en diferido.
  const firstImage = cards.first().locator('picture img');
  await expect(firstImage).toHaveAttribute('loading', 'lazy');
  await expect(firstImage).toHaveAttribute('decoding', 'async');
  await expect(firstImage).toHaveAttribute('sizes', /vw/);
  await expect(firstImage).toHaveAttribute('width', '320');
  await expect(firstImage).toHaveAttribute('height', '480');
  await expect(firstImage).not.toHaveAttribute('fetchpriority', 'high');

  await page.goto(firstHref!);

  // En la ficha del anime, su portada principal SÍ es el LCP: eager + alta
  // prioridad (ahí no hay hero por encima que compita).
  const heroCover = page.locator('picture img[fetchpriority="high"]').first();
  await expect(heroCover).toBeVisible();
  await expect(heroCover).toHaveAttribute('loading', 'eager');
  await expect(heroCover).toHaveAttribute('decoding', 'async');
});

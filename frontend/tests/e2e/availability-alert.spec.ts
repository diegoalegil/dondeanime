import { expect, test, type APIRequestContext } from '@playwright/test';

interface SearchEntry {
  slug: string;
}

const countrySlugs = ['chile', 'colombia', 'argentina', 'mexico', 'espana'];

test('una página sin providers muestra alerta y confirma el alta', async ({ page, request }) => {
  test.skip(
    !isLocalActionApi(),
    'El alta de alertas solo se prueba contra backend local para no tocar producción.',
  );

  const alertPath = await findUnavailableCountryPage(request);

  await page.goto(alertPath);
  await expect(page.getByRole('heading', { name: /Aviso de disponibilidad/i })).toBeVisible();

  await page.getByRole('button', { name: 'Avisarme' }).click();
  await page.getByLabel('Email').fill(`playwright-${Date.now()}@example.com`);
  await page.getByLabel(/Acepto la política de privacidad/i).check();
  await page.getByRole('button', { name: 'Crear alerta' }).click();

  await expect(page.locator('[data-alert-status]'))
    .toHaveText(/correo de confirmación/i);
});

async function findUnavailableCountryPage(request: APIRequestContext) {
  const indexResponse = await request.get('/search-index.json');
  expect(indexResponse.ok()).toBe(true);
  const index = await indexResponse.json() as SearchEntry[];

  for (const entry of index) {
    for (const countrySlug of countrySlugs) {
      const path = `/anime/${entry.slug}/${countrySlug}`;
      const response = await request.get(path);
      if (!response.ok()) {
        continue;
      }
      const html = await response.text();
      if (html.includes('data-alert-form') && html.includes('Aviso de disponibilidad')) {
        return path;
      }
    }
  }

  throw new Error('No se encontró ninguna página país sin providers para probar alertas.');
}

function isLocalActionApi() {
  const apiUrl = process.env.PUBLIC_API_URL ?? '';
  return apiUrl.startsWith('http://127.0.0.1') || apiUrl.startsWith('http://localhost');
}

import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';

const routes = ['/', '/anime/attack-on-titan'];
const themes = ['dark', 'light'] as const;

for (const theme of themes) {
  test.describe(`${theme} theme accessibility`, () => {
    for (const route of routes) {
      test(`${route} has no WCAG A/AA violations`, async ({ page }) => {
        await page.addInitScript((selectedTheme) => {
          localStorage.setItem('dondeanime-theme', selectedTheme);
        }, theme);

        await page.goto(route);

        const results = await new AxeBuilder({ page })
          .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
          // El iframe del trailer carga el reproductor de YouTube (terceros): sus
          // violaciones WCAG son de Google, no nuestras, y no podemos corregirlas.
          // WCAG aplica a nuestro contenido; el iframe en sí ya lleva su `title`.
          .exclude('iframe[src*="youtube"]')
          .analyze();

        expect(results.violations).toEqual([]);
      });
    }
  });
}

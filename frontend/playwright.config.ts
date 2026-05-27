import { defineConfig, devices } from '@playwright/test';

const port = Number(process.env.PLAYWRIGHT_PORT ?? 4321);
const baseURL = `http://127.0.0.1:${port}`;
const publicApiUrl = process.env.PUBLIC_API_URL ?? 'https://api.dondeanime.com';
const publicDataApiUrl = process.env.PUBLIC_DATA_API_URL ?? publicApiUrl;
const publicSiteUrl = process.env.PUBLIC_SITE_URL ?? 'https://dondeanime.com';
const publicStripePublishableKey = process.env.PUBLIC_STRIPE_PUBLISHABLE_KEY ?? 'pk_test_REEMPLAZAR';

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 30_000,
  expect: {
    timeout: 7_000,
  },
  fullyParallel: true,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: process.env.CI
    ? [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]]
    : [['list']],
  use: {
    baseURL,
    locale: 'es-ES',
    screenshot: 'only-on-failure',
    trace: 'on-first-retry',
  },
  webServer: {
    command: `npm run build && npm run preview -- --host 127.0.0.1 --port ${port}`,
    url: baseURL,
    reuseExistingServer: !process.env.CI && !process.env.PUBLIC_API_URL && !process.env.PUBLIC_DATA_API_URL,
    timeout: 600_000,
    env: {
      ...process.env,
      PUBLIC_API_URL: publicApiUrl,
      PUBLIC_DATA_API_URL: publicDataApiUrl,
      PUBLIC_SITE_URL: publicSiteUrl,
      PUBLIC_STRIPE_PUBLISHABLE_KEY: publicStripePublishableKey,
    },
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});

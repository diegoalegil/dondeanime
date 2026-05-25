import { expect, test } from '@playwright/test';

test('admin login stores JWT token in localStorage and redirects', async ({ page }) => {
  await page.route('https://api.dondeanime.com/api/admin/login', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        token: 'jwt-token-for-e2e',
        tokenType: 'Bearer',
        expiresAt: '2026-05-25T20:00:00Z',
      }),
    });
  });
  await page.route('https://api.dondeanime.com/api/admin/dashboard', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        clicksLast7Days: 0,
        clicksLast30Days: 0,
        topAffiliateAnime: [],
        topAffiliateLinks: [],
        topVisitedAnime: [],
      }),
    });
  });

  await page.goto('/admin/login?next=/admin/dashboard');
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'secret');
  await Promise.all([
    page.waitForURL(/\/admin\/dashboard$/),
    page.click('button[type="submit"]'),
  ]);

  await page.waitForFunction(() => localStorage.getItem('dondeanime-admin-token') === 'jwt-token-for-e2e');
});

import { expect, test } from '@playwright/test';

async function mockAdminShell(page) {
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
  await page.route('https://api.dondeanime.com/api/admin/2fa', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ enabled: false }),
    });
  });
}

test('admin login stores JWT token in localStorage and redirects', async ({ page }) => {
  await mockAdminShell(page);
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

  await page.goto('/admin/login?next=/admin/dashboard');
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'secret');
  await Promise.all([
    page.waitForURL(/\/admin\/dashboard$/),
    page.click('button[type="submit"]'),
  ]);

  await page.waitForFunction(() => localStorage.getItem('dondeanime-admin-token') === 'jwt-token-for-e2e');
});

test('admin login asks for TOTP code when enabled', async ({ page }) => {
  await mockAdminShell(page);
  await page.route('https://api.dondeanime.com/api/admin/login', async (route) => {
    const body = route.request().postDataJSON();
    if (!body.totpCode) {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'totp_required' }),
      });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        token: 'jwt-token-with-totp',
        tokenType: 'Bearer',
        expiresAt: '2026-05-25T20:00:00Z',
      }),
    });
  });

  await page.goto('/admin/login?next=/admin/dashboard');
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'secret');
  await page.click('button[type="submit"]');
  await expect(page.locator('[data-totp-field]')).toBeVisible();

  await page.fill('input[name="totpCode"]', '123456');
  await Promise.all([
    page.waitForURL(/\/admin\/dashboard$/),
    page.click('button[type="submit"]'),
  ]);

  await page.waitForFunction(() => localStorage.getItem('dondeanime-admin-token') === 'jwt-token-with-totp');
});

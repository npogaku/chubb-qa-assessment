import { test, expect } from './fixtures/test-fixtures';

/**
 * E2E: Admin Dashboard — statistics and Redis cache behaviour
 */

test.describe('Admin Dashboard', () => {
  test('dashboard loads and shows statistics cards', async ({
    adminDashboardPage,
  }) => {
    await adminDashboardPage.goto();

    await expect(
      adminDashboardPage.page.getByText('Total Users')
    ).toBeVisible();
    await expect(
      adminDashboardPage.page.getByText('Total Claims')
    ).toBeVisible();
    await expect(
      adminDashboardPage.page.getByText('Claims by Status')
    ).toBeVisible();
  });

  test('first cache load is a Cache Miss (cold start)', async ({
    adminDashboardPage,
  }) => {
    await adminDashboardPage.goto();

    // Flush first to guarantee cold start
    await adminDashboardPage.flushCache();

    const result = await adminDashboardPage.loadFromCache();
    expect(result).toMatch(/Cache Miss/i);
  });

  test('second cache load is a Cache Hit', async ({ adminDashboardPage }) => {
    await adminDashboardPage.goto();
    await adminDashboardPage.flushCache();

    // First load populates cache
    await adminDashboardPage.loadFromCache();
    // Second load should hit cache
    const result = await adminDashboardPage.loadFromCache();
    expect(result).toMatch(/Cache Hit/i);
  });

  test('flush cache resets to Cache Miss on next load', async ({
    adminDashboardPage,
  }) => {
    await adminDashboardPage.goto();

    // Warm up
    await adminDashboardPage.loadFromCache();
    await adminDashboardPage.loadFromCache();

    // Flush and re-check
    await adminDashboardPage.flushCache();
    const result = await adminDashboardPage.loadFromCache();
    expect(result).toMatch(/Cache Miss/i);
  });

  test('WebSocket live indicator is shown for admin', async ({
    adminDashboardPage,
  }) => {
    await adminDashboardPage.goto();
    await expect(
      adminDashboardPage.page.getByText('Live')
    ).toBeVisible();
  });
});

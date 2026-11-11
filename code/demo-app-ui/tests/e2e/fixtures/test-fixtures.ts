import { test as base } from '@playwright/test';
import { ClaimsPage }        from '../page-objects/claims.page';
import { AdminClaimsPage }   from '../page-objects/admin-claims.page';
import { AdminDashboardPage } from '../page-objects/admin-dashboard.page';

/**
 * Extended Playwright test fixture that provides typed page objects
 * and pre-authenticated sessions for both roles.
 *
 * Usage:
 *   import { test } from '../fixtures/test-fixtures';
 *   test('...', async ({ claimsPage }) => { ... });
 */
interface TestFixtures {
  claimsPage: ClaimsPage;
  adminClaimsPage: AdminClaimsPage;
  adminDashboardPage: AdminDashboardPage;
}

export const test = base.extend<TestFixtures>({
  claimsPage: async ({ browser }, use) => {
    const context = await browser.newContext({
      storageState: 'tests/e2e/.auth/claimant.json',
    });
    const page = await context.newPage();
    await use(new ClaimsPage(page));
    await context.close();
  },

  adminClaimsPage: async ({ browser }, use) => {
    const context = await browser.newContext({
      storageState: 'tests/e2e/.auth/admin.json',
    });
    const page = await context.newPage();
    await use(new AdminClaimsPage(page));
    await context.close();
  },

  adminDashboardPage: async ({ browser }, use) => {
    const context = await browser.newContext({
      storageState: 'tests/e2e/.auth/admin.json',
    });
    const page = await context.newPage();
    await use(new AdminDashboardPage(page));
    await context.close();
  },
});

export { expect } from '@playwright/test';

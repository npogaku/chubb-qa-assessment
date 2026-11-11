import { chromium, FullConfig } from '@playwright/test';

/**
 * Global E2E setup — authenticates both test users once and saves their
 * storage state to disk so individual tests don't need to repeat login.
 *
 * Sessions are stored under tests/e2e/.auth/ and referenced by the
 * storageState option in page-object fixtures.
 */
export default async function globalSetup(_config: FullConfig) {
  const browser = await chromium.launch();

  // ---- Claimant session ------------------------------------------------
  const claimantPage = await browser.newPage();
  await loginAs(claimantPage, 'claimant@demo.com', 'Claimant123!');
  await claimantPage.context().storageState({ path: 'tests/e2e/.auth/claimant.json' });
  await claimantPage.close();

  // ---- Admin session ----------------------------------------------------
  const adminPage = await browser.newPage();
  await loginAs(adminPage, 'admin@demo.com', 'Admin123!');
  await adminPage.context().storageState({ path: 'tests/e2e/.auth/admin.json' });
  await adminPage.close();

  await browser.close();
}

async function loginAs(page: any, email: string, password: string) {
  await page.goto('http://localhost:3001');
  // The app redirects to Keycloak — wait for the login form
  await page.waitForURL(/localhost:8180/, { timeout: 15_000 });
  await page.fill('#username', email);
  await page.fill('#password', password);
  await page.click('[type="submit"]');
  // Wait until redirected back to the app
  await page.waitForURL(/localhost:3001/, { timeout: 15_000 });
}

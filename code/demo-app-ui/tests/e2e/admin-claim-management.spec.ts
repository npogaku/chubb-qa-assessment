import { test, expect } from './fixtures/test-fixtures';
import { ClaimsPage } from './page-objects/claims.page';
import { LoginPage }  from './page-objects/login.page';

/**
 * E2E: Admin claim status management
 *
 * Tests the admin status-change workflow and validates:
 * - Status transition state machine (happy paths and illegal transitions)
 * - Real-time WebSocket push — claimant sees update without refreshing
 * - RBAC: claimant cannot access admin routes
 */

const VALID_CLAIM = {
  incidentDate: '2026-09-20',
  location: 'Hitech City, Hyderabad, Telangana',
  amount: 25000,
  description:
    'Office property damage caused by an unexpected water pipe burst. ' +
    'Server room equipment affected.',
};

test.describe('Admin Claim Status Management', () => {
  test('admin can move claim from Submitted to Under Review', async ({
    adminClaimsPage,
    browser,
  }) => {
    // First submit a claim as claimant
    const claimantCtx = await browser.newContext({
      storageState: 'tests/e2e/.auth/claimant.json',
    });
    const claimantPage = new ClaimsPage(await claimantCtx.newPage());
    await claimantPage.goto();
    const claimId = await claimantPage.submitClaim(VALID_CLAIM);
    await claimantCtx.close();

    // Admin changes status
    await adminClaimsPage.goto();
    await adminClaimsPage.changeClaimStatus(claimId, 'Under Review');

    const row = await adminClaimsPage.getClaimRow(claimId);
    await expect(row.getByRole('status')).toHaveText('Under Review');
  });

  test('admin can follow full approval lifecycle: Submitted → Under Review → Approved → Closed', async ({
    adminClaimsPage,
    browser,
  }) => {
    const claimantCtx = await browser.newContext({
      storageState: 'tests/e2e/.auth/claimant.json',
    });
    const claimantPage = new ClaimsPage(await claimantCtx.newPage());
    await claimantPage.goto();
    const claimId = await claimantPage.submitClaim(VALID_CLAIM);
    await claimantCtx.close();

    await adminClaimsPage.goto();

    await adminClaimsPage.changeClaimStatus(claimId, 'Under Review');
    await adminClaimsPage.changeClaimStatus(claimId, 'Approved');
    await adminClaimsPage.changeClaimStatus(claimId, 'Closed');

    const row = await adminClaimsPage.getClaimRow(claimId);
    await expect(row.getByRole('status')).toHaveText('Closed');
  });

  test('admin can reject a claim from Under Review', async ({
    adminClaimsPage,
    browser,
  }) => {
    const claimantCtx = await browser.newContext({
      storageState: 'tests/e2e/.auth/claimant.json',
    });
    const claimantPage = new ClaimsPage(await claimantCtx.newPage());
    await claimantPage.goto();
    const claimId = await claimantPage.submitClaim(VALID_CLAIM);
    await claimantCtx.close();

    await adminClaimsPage.goto();
    await adminClaimsPage.changeClaimStatus(claimId, 'Under Review');
    await adminClaimsPage.changeClaimStatus(claimId, 'Rejected');

    const row = await adminClaimsPage.getClaimRow(claimId);
    await expect(row.getByRole('status')).toHaveText('Rejected');
  });

  test('closed claim status dropdown is disabled (terminal state)', async ({
    adminClaimsPage,
    browser,
  }) => {
    const claimantCtx = await browser.newContext({
      storageState: 'tests/e2e/.auth/claimant.json',
    });
    const claimantPage = new ClaimsPage(await claimantCtx.newPage());
    await claimantPage.goto();
    const claimId = await claimantPage.submitClaim(VALID_CLAIM);
    await claimantCtx.close();

    await adminClaimsPage.goto();
    await adminClaimsPage.changeClaimStatus(claimId, 'Under Review');
    await adminClaimsPage.changeClaimStatus(claimId, 'Approved');
    await adminClaimsPage.changeClaimStatus(claimId, 'Closed');

    const row = await adminClaimsPage.getClaimRow(claimId);
    // No transitions from CLOSED — dropdown or select should be disabled/absent
    await expect(row.getByRole('combobox')).toBeDisabled();
  });
});

test.describe('Real-Time Updates via WebSocket', () => {
  test('claimant sees status update in real time when admin changes it', async ({
    browser,
  }) => {
    // Open claimant tab
    const claimantCtx = await browser.newContext({
      storageState: 'tests/e2e/.auth/claimant.json',
    });
    const claimantPage = new ClaimsPage(await claimantCtx.newPage());
    await claimantPage.goto();
    const claimId = await claimantPage.submitClaim(VALID_CLAIM);

    // Open admin tab
    const adminCtx = await browser.newContext({
      storageState: 'tests/e2e/.auth/admin.json',
    });
    const { AdminClaimsPage } = await import('./page-objects/admin-claims.page');
    const adminClaims = new AdminClaimsPage(await adminCtx.newPage());
    await adminClaims.goto();
    await adminClaims.changeClaimStatus(claimId, 'Under Review');

    // Claimant's tab should update WITHOUT a page refresh (WebSocket push)
    await expect(
      claimantPage.page.getByText('Under Review', { exact: false })
    ).toBeVisible({ timeout: 15_000 });

    await claimantCtx.close();
    await adminCtx.close();
  });
});

test.describe('RBAC: Claimant cannot access admin routes', () => {
  test('claimant is redirected away from /admin/dashboard', async ({
    browser,
  }) => {
    const ctx = await browser.newContext({
      storageState: 'tests/e2e/.auth/claimant.json',
    });
    const page = await ctx.newPage();
    await page.goto('/admin/dashboard');

    // Should be redirected to /claims or / — not remain on /admin/
    await expect(page).not.toHaveURL(/\/admin\//);
    await ctx.close();
  });
});

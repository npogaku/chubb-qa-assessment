import { test, expect } from './fixtures/test-fixtures';

/**
 * E2E: Claim submission wizard (claimant journey)
 *
 * Tests the core claimant happy path end-to-end:
 * UI → BFF → Claims Service → PostgreSQL → WebSocket notification
 *
 * Requires the full Docker Compose stack to be running:
 *   ./start-infra.sh && ./start-app.sh
 */

const VALID_CLAIM = {
  incidentDate: '2026-10-15',
  location: '45 Marine Drive, Mumbai, India',
  amount: 50000,
  description:
    'Vehicle collision on the highway during evening rush hour. ' +
    'Front bumper and bonnet damage. Third party involved.',
};

test.describe('Claim Submission Wizard', () => {
  test('claimant can submit a new claim and see it in their list', async ({
    claimsPage,
  }) => {
    await claimsPage.goto();

    const claimId = await claimsPage.submitClaim(VALID_CLAIM);

    // Claim ID returned from success toast
    expect(claimId).toBeTruthy();

    // Claim appears in the list immediately (store prepends it)
    await expect(
      claimsPage.page.getByText(claimId.substring(0, 8), { exact: false })
    ).toBeVisible({ timeout: 10_000 });
  });

  test('submitted claim starts with SUBMITTED status', async ({ claimsPage }) => {
    await claimsPage.goto();
    const claimId = await claimsPage.submitClaim(VALID_CLAIM);

    // Open claim detail modal
    await claimsPage.openClaimDetails(claimId);

    const statusText = await claimsPage.getStatusBadgeText();
    expect(statusText).toBe('Submitted');
  });

  test('wizard step 1 rejects a future incident date', async ({ claimsPage }) => {
    await claimsPage.goto();
    await claimsPage.submitButton.click();

    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = tomorrow.toISOString().split('T')[0];

    await claimsPage.page.fill('[aria-label*="When did the incident occur"]', tomorrowStr);
    await claimsPage.page.keyboard.press('Tab');

    await expect(
      claimsPage.page.getByText(/Incident date cannot be in the future/i)
    ).toBeVisible();

    // Next button should remain disabled
    await expect(
      claimsPage.page.getByRole('button', { name: /Next/i })
    ).toBeDisabled();
  });

  test('wizard step 1 rejects claim amount above $1,000,000', async ({
    claimsPage,
  }) => {
    await claimsPage.goto();
    await claimsPage.submitButton.click();

    await claimsPage.page.fill('[aria-label*="Claim Amount"]', '1500000');
    await claimsPage.page.keyboard.press('Tab');

    await expect(
      claimsPage.page.getByText(/Claim amount cannot exceed \$1,000,000/i)
    ).toBeVisible();
  });

  test('wizard step 2 rejects a description shorter than 10 characters', async ({
    claimsPage,
  }) => {
    await claimsPage.goto();
    await claimsPage.submitButton.click();

    // Fill step 1 to advance
    await claimsPage.page.fill('[aria-label*="When did the incident occur"]', '2026-10-01');
    await claimsPage.page.fill('[aria-label*="Where did the incident occur"]', 'Test Location Here');
    await claimsPage.page.fill('[aria-label*="Claim Amount"]', '5000');
    await claimsPage.page.getByRole('button', { name: /Next/i }).click();

    // Now on step 2
    await claimsPage.page.fill('textarea', 'Too short');
    await claimsPage.page.keyboard.press('Tab');

    await expect(
      claimsPage.page.getByText(/Description must be at least 10 characters/i)
    ).toBeVisible();
  });

  test('BUG-009: wizard accepts year 1111 as incident date', async ({
    claimsPage,
  }) => {
    /**
     * KNOWN BUG: The domain validates incidentDate is not in the FUTURE,
     * but has no minimum date — so year 1111 is accepted.
     * This test documents the behaviour. In a real insurance system,
     * dates older than the policy window should be rejected.
     */
    await claimsPage.goto();
    await claimsPage.submitButton.click();

    await claimsPage.page.fill('[aria-label*="When did the incident occur"]', '1111-11-11');
    await claimsPage.page.keyboard.press('Tab');

    // No validation error appears — this is the bug
    await expect(
      claimsPage.page.getByText(/date cannot be in the future/i)
    ).not.toBeVisible();
  });
});

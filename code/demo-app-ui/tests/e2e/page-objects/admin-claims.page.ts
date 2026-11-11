import { Page, Locator, expect } from '@playwright/test';

/**
 * Page object for the admin claims management view.
 */
export class AdminClaimsPage {
  constructor(private readonly page: Page) {}

  async goto(): Promise<void> {
    await this.page.goto('/admin/claims');
    await this.page.waitForLoadState('networkidle');
  }

  async changeClaimStatus(claimId: string, newStatus: string): Promise<void> {
    // Find the row containing the claim ID and click the status dropdown
    const row = this.page.getByRole('row').filter({ hasText: claimId.substring(0, 8) });
    await row.getByRole('combobox').click();
    await this.page.getByRole('option', { name: newStatus }).click();

    // Confirm in the dialog
    const dialog = this.page.getByRole('dialog');
    await expect(dialog).toBeVisible({ timeout: 5_000 });
    await dialog.getByRole('button', { name: /confirm/i }).click();

    // Wait for the status badge to update
    await expect(
      row.getByRole('status').filter({ hasText: newStatus })
    ).toBeVisible({ timeout: 10_000 });
  }

  async getClaimRow(claimId: string): Promise<Locator> {
    return this.page.getByRole('row').filter({ hasText: claimId.substring(0, 8) });
  }
}

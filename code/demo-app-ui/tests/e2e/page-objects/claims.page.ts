import { Page, Locator, expect } from '@playwright/test';

export interface ClaimFormData {
  incidentDate: string;   // YYYY-MM-DD
  location: string;
  amount: number;
  description: string;
}

/**
 * Page object for the claimant-facing claims pages.
 * Encapsulates the 3-step submission wizard and claim list interactions.
 */
export class ClaimsPage {
  readonly submitButton: Locator;
  readonly claimList: Locator;

  constructor(private readonly page: Page) {
    this.submitButton = page.getByRole('button', { name: /Submit New Claim/i });
    this.claimList    = page.getByRole('row');
  }

  async goto(): Promise<void> {
    await this.page.goto('/claims');
    await this.page.waitForLoadState('networkidle');
  }

  /** Runs through all three wizard steps and submits the claim. */
  async submitClaim(data: ClaimFormData): Promise<string> {
    await this.submitButton.click();

    // ── Step 1: Incident Details ──────────────────────────────────────────
    await this.page.fill('[aria-label*="When did the incident occur"]', data.incidentDate);
    await this.page.fill('[aria-label*="Where did the incident occur"]', data.location);
    await this.page.fill('[aria-label*="Claim Amount"]', String(data.amount));
    await this.page.getByRole('button', { name: /Next/i }).click();

    // ── Step 2: Supporting Details ────────────────────────────────────────
    await this.page.fill('textarea', data.description);
    await this.page.getByRole('button', { name: /Next/i }).click();

    // ── Step 3: Review & Submit ───────────────────────────────────────────
    await expect(this.page.getByRole('heading', { name: /Review Your Claim/i })).toBeVisible();
    await this.page.getByRole('button', { name: /Submit Claim/i }).click();

    // Wait for success toast and capture the new claim ID
    const toast = this.page.getByText(/Claim submitted successfully/i);
    await expect(toast).toBeVisible({ timeout: 10_000 });

    const toastText = await toast.textContent() ?? '';
    const match = toastText.match(/Claim ID:\s*(\S+)/);
    return match?.[1] ?? '';
  }

  async openClaimDetails(claimId: string): Promise<void> {
    await this.page.getByText(claimId.substring(0, 8), { exact: false }).click();
  }

  async getStatusBadgeText(): Promise<string> {
    const badge = this.page.getByRole('status').first();
    return (await badge.textContent()) ?? '';
  }
}

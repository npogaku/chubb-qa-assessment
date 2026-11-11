import { Page, expect } from '@playwright/test';

/**
 * Page object for the admin dashboard (/admin/dashboard).
 */
export class AdminDashboardPage {
  constructor(private readonly page: Page) {}

  async goto(): Promise<void> {
    await this.page.goto('/admin/dashboard');
    await this.page.waitForLoadState('networkidle');
  }

  async getTotalClaims(): Promise<number> {
    const text = await this.page.getByTestId('total-claims').textContent();
    return parseInt(text ?? '0', 10);
  }

  async loadFromCache(): Promise<string> {
    await this.page.getByRole('button', { name: /Load from Cache/i }).click();
    await this.page.waitForLoadState('networkidle');
    const badge = this.page.getByText(/Cache (Hit|Miss)/i);
    return (await badge.textContent()) ?? '';
  }

  async flushCache(): Promise<void> {
    await this.page.getByRole('button', { name: /Flush Cache/i }).click();
    await this.page.waitForLoadState('networkidle');
  }
}

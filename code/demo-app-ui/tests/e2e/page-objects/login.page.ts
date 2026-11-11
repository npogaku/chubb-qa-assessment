import { Page } from '@playwright/test';

/**
 * Page object for the Keycloak login screen.
 * The app delegates authentication to Keycloak (OAuth2/OIDC redirect).
 */
export class LoginPage {
  constructor(private readonly page: Page) {}

  async loginAs(email: string, password: string): Promise<void> {
    await this.page.goto('/');
    await this.page.waitForURL(/localhost:8180/, { timeout: 15_000 });
    await this.page.fill('#username', email);
    await this.page.fill('#password', password);
    await this.page.click('[type="submit"]');
    await this.page.waitForURL(/localhost:3001/, { timeout: 15_000 });
  }
}

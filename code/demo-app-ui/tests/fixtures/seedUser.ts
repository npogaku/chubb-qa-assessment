import { test as base } from '@playwright/test';
import { createUser, UserTestData } from './userFactory';

/**
 * Playwright fixture for seeding and auto-cleaning test users
 *
 * Usage in tests:
 * @example
 * test('user can view profile', async ({ seedUser }) => {
 *   const user = await seedUser.create();
 *   // user is available for test
 *   // automatically cleaned up after test via afterEach hook
 * });
 */

interface SeedUserFixture {
  create: (overrides?: Partial<UserTestData>) => Promise<UserTestData>;
  createdUsers: UserTestData[];
}

export const test = base.extend<{ seedUser: SeedUserFixture }>({
  seedUser: async ({}, use) => {
    const createdUsers: UserTestData[] = [];

    const seedUser: SeedUserFixture = {
      async create(overrides?: Partial<UserTestData>) {
        const user = createUser(overrides);
        createdUsers.push(user);
        // In a real scenario, you would call API to create user in backend:
        // await request.post('/api/users', { data: user });
        return user;
      },
      createdUsers,
    };

    await use(seedUser);

    // Cleanup: called after each test
    // In a real scenario, you would delete created users:
    // for (const user of createdUsers) {
    //   await request.delete(`/api/users/${user.id}`);
    // }
    createdUsers.length = 0;
  },
});

export { expect } from '@playwright/test';

import { faker } from '@faker-js/faker';

/**
 * Test data factory for User objects
 * Uses Faker to generate realistic, randomized data for parallel-safe tests
 */

export interface UserTestData {
  id: string;
  email: string;
  name: string;
  role: 'user' | 'admin';
}

/**
 * Create a User with randomly generated email, name, and UUID
 * Optionally pass overrides to customize specific fields
 *
 * @example
 * const user = createUser()
 * const admin = createUser({ email: 'test@example.com', role: 'admin' })
 */
export function createUser(overrides?: Partial<UserTestData>): UserTestData {
  return {
    id: faker.string.uuid(),
    email: faker.internet.email(),
    name: faker.person.fullName(),
    role: 'user',
    ...overrides,
  };
}

/**
 * Create an admin user (convenience method)
 */
export function createAdminUser(): UserTestData {
  return createUser({ role: 'admin' });
}

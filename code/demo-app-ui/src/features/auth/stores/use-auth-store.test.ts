import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore, type User } from './use-auth-store';

const testUser: User = {
  userId: 'user-123',
  email: 'test@example.com',
  name: 'Test User',
  role: 'CLAIMANT',
};

describe('useAuthStore', () => {
  beforeEach(() => {
    useAuthStore.setState({ user: null, isAuthenticated: false });
  });

  it('starts with no user and not authenticated', () => {
    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
  });

  it('login sets user and isAuthenticated', () => {
    useAuthStore.getState().login(testUser);
    const state = useAuthStore.getState();
    expect(state.user).toEqual(testUser);
    expect(state.isAuthenticated).toBe(true);
  });

  it('logout clears user and isAuthenticated', () => {
    useAuthStore.getState().login(testUser);
    useAuthStore.getState().logout();
    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
  });

  it('supports admin role', () => {
    const admin: User = { ...testUser, role: 'ADMIN' };
    useAuthStore.getState().login(admin);
    expect(useAuthStore.getState().user?.role).toBe('ADMIN');
  });
});

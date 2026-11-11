import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useUsersStore } from './use-users-store';
import { adminDashboardApi } from '@/src/lib/api/bff-client';
import { ResponseError } from '@/src/lib/api/generated/runtime';

vi.mock('@/src/lib/api/bff-client', () => ({
  adminDashboardApi: {
    listAllUsers: vi.fn(),
  },
}));

describe('useUsersStore', () => {
  beforeEach(() => {
    useUsersStore.setState({
      users: [],
      isLoading: false,
      error: null,
      searchQuery: '',
    });
    vi.clearAllMocks();
  });

  describe('Initial State', () => {
    it('should have correct initial state', () => {
      const { result } = renderHook(() => useUsersStore());
      expect(result.current.users).toEqual([]);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
      expect(result.current.searchQuery).toBe('');
    });
  });

  describe('fetchUsers', () => {
    it('should fetch and sort users by createdAt descending', async () => {
      const older = { id: '1', createdAt: '2025-01-01T00:00:00Z' };
      const newer = { id: '2', createdAt: '2025-06-01T00:00:00Z' };
      vi.mocked(adminDashboardApi.listAllUsers).mockResolvedValue([older, newer] as any);

      const { result } = renderHook(() => useUsersStore());
      await act(async () => {
        await result.current.fetchUsers();
      });

      expect(result.current.users[0].id).toBe('2');
      expect(result.current.users[1].id).toBe('1');
      expect(result.current.isLoading).toBe(false);
    });

    it('should handle 403 ACCESS_DENIED error', async () => {
      const error = new ResponseError(
        new Response(null, { status: 403 }),
        'Forbidden'
      );
      vi.mocked(adminDashboardApi.listAllUsers).mockRejectedValue(error);

      const { result } = renderHook(() => useUsersStore());
      await act(async () => {
        await result.current.fetchUsers();
      });

      expect(result.current.error).toBe('ACCESS_DENIED');
      expect(result.current.isLoading).toBe(false);
    });

    it('should handle generic errors', async () => {
      vi.mocked(adminDashboardApi.listAllUsers).mockRejectedValue(new Error('Network'));

      const { result } = renderHook(() => useUsersStore());
      await act(async () => {
        await result.current.fetchUsers();
      });

      expect(result.current.error).toBe('Failed to load users');
      expect(result.current.isLoading).toBe(false);
    });
  });

  describe('setSearchQuery', () => {
    it('should update search query', () => {
      const { result } = renderHook(() => useUsersStore());
      act(() => {
        result.current.setSearchQuery('admin');
      });

      expect(result.current.searchQuery).toBe('admin');
    });
  });

  describe('clearError', () => {
    it('should clear the error', () => {
      useUsersStore.setState({ error: 'some error' });

      const { result } = renderHook(() => useUsersStore());
      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });
  });
});

import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { useDashboardStore } from './use-dashboard-store';
import { adminDashboardApi } from '@/src/lib/api/bff-client';
import { ResponseError } from '@/src/lib/api/generated/runtime';

vi.mock('@/src/lib/api/bff-client', () => ({
  adminDashboardApi: {
    getDashboardStats: vi.fn(),
    flushDashboardCache: vi.fn(),
  },
}));

const mockStatsResponse = {
  totalUsers: 42,
  totalClaims: 127,
  claimsByStatus: {
    SUBMITTED: 15,
    UNDER_REVIEW: 8,
    APPROVED: 3,
    REJECTED: 2,
    CLOSED: 1,
  },
  cacheHit: true,
  queryTimeMs: 95,
};

describe('useDashboardStore', () => {
  beforeEach(() => {
    useDashboardStore.setState({
      stats: null,
      isLoading: false,
      error: null,
      lastLoadInfo: null,
    });
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('Initial State', () => {
    it('should have null stats initially', () => {
      const { result } = renderHook(() => useDashboardStore());
      expect(result.current.stats).toBeNull();
    });

    it('should not be loading initially', () => {
      const { result } = renderHook(() => useDashboardStore());
      expect(result.current.isLoading).toBe(false);
    });

    it('should have no error initially', () => {
      const { result } = renderHook(() => useDashboardStore());
      expect(result.current.error).toBeNull();
    });

    it('should have no load info initially', () => {
      const { result } = renderHook(() => useDashboardStore());
      expect(result.current.lastLoadInfo).toBeNull();
    });
  });

  describe('fetchStats', () => {
    it('should fetch stats and update store on success', async () => {
      vi.mocked(adminDashboardApi.getDashboardStats).mockResolvedValue(mockStatsResponse);

      const { result } = renderHook(() => useDashboardStore());

      await act(async () => {
        await result.current.fetchStats(false);
      });

      expect(adminDashboardApi.getDashboardStats).toHaveBeenCalledWith({ bypassCache: false });
      expect(result.current.stats).toEqual(mockStatsResponse);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.lastLoadInfo).not.toBeNull();
      expect(result.current.lastLoadInfo?.cacheHit).toBe(true);
      expect(result.current.lastLoadInfo?.queryTimeMs).toBe(95);
    });

    it('should call with bypassCache=true when specified', async () => {
      vi.mocked(adminDashboardApi.getDashboardStats).mockResolvedValue({
        ...mockStatsResponse,
        cacheHit: false,
        queryTimeMs: 823,
      });

      const { result } = renderHook(() => useDashboardStore());

      await act(async () => {
        await result.current.fetchStats(true);
      });

      expect(adminDashboardApi.getDashboardStats).toHaveBeenCalledWith({ bypassCache: true });
      expect(result.current.lastLoadInfo?.cacheHit).toBe(false);
      expect(result.current.lastLoadInfo?.queryTimeMs).toBe(823);
    });

    it('should set ACCESS_DENIED error on 403', async () => {
      const mockResponse = new Response(null, { status: 403 });
      vi.mocked(adminDashboardApi.getDashboardStats).mockRejectedValue(
        new ResponseError(mockResponse, 'Forbidden')
      );

      const { result } = renderHook(() => useDashboardStore());

      await act(async () => {
        await result.current.fetchStats(false);
      });

      expect(result.current.error).toBe('ACCESS_DENIED');
      expect(result.current.isLoading).toBe(false);
    });

    it('should set generic error on other failures', async () => {
      vi.mocked(adminDashboardApi.getDashboardStats).mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useDashboardStore());

      await act(async () => {
        await result.current.fetchStats(false);
      });

      expect(result.current.error).toBe('Failed to load dashboard stats');
      expect(result.current.isLoading).toBe(false);
    });

    it('should set isLoading to true during fetch', async () => {
      let resolvePromise: (value: typeof mockStatsResponse) => void;
      const promise = new Promise<typeof mockStatsResponse>((resolve) => {
        resolvePromise = resolve;
      });

      vi.mocked(adminDashboardApi.getDashboardStats).mockReturnValue(promise);

      const { result } = renderHook(() => useDashboardStore());

      act(() => {
        result.current.fetchStats(false);
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(true);
      });

      await act(async () => {
        resolvePromise!(mockStatsResponse);
      });

      expect(result.current.isLoading).toBe(false);
    });
  });

  describe('flushCache', () => {
    it('should flush cache then fetch with bypassCache=true', async () => {
      vi.mocked(adminDashboardApi.flushDashboardCache).mockResolvedValue(undefined);
      vi.mocked(adminDashboardApi.getDashboardStats).mockResolvedValue({
        ...mockStatsResponse,
        cacheHit: false,
        queryTimeMs: 823,
      });

      const { result } = renderHook(() => useDashboardStore());

      await act(async () => {
        await result.current.flushCache();
      });

      expect(adminDashboardApi.flushDashboardCache).toHaveBeenCalled();
      expect(adminDashboardApi.getDashboardStats).toHaveBeenCalledWith({ bypassCache: true });
      expect(result.current.lastLoadInfo?.cacheHit).toBe(false);
    });

    it('should set error if flush fails', async () => {
      vi.mocked(adminDashboardApi.flushDashboardCache).mockRejectedValue(new Error('Flush failed'));

      const { result } = renderHook(() => useDashboardStore());

      await act(async () => {
        await result.current.flushCache();
      });

      expect(result.current.error).toBe('Failed to flush cache');
      expect(result.current.isLoading).toBe(false);
    });

    it('should set ACCESS_DENIED on 403 from flush', async () => {
      const mockResponse = new Response(null, { status: 403 });
      vi.mocked(adminDashboardApi.flushDashboardCache).mockRejectedValue(
        new ResponseError(mockResponse, 'Forbidden')
      );

      const { result } = renderHook(() => useDashboardStore());

      await act(async () => {
        await result.current.flushCache();
      });

      expect(result.current.error).toBe('ACCESS_DENIED');
    });
  });

  describe('clearError', () => {
    it('should clear the error', () => {
      useDashboardStore.setState({ error: 'some error' });

      const { result } = renderHook(() => useDashboardStore());

      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });
  });
});

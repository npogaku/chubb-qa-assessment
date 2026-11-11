import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useClaimsStore } from './use-claims-store';
import { claimsApi } from '@/src/lib/api/bff-client';
import { ResponseError } from '@/src/lib/api/generated/runtime';

vi.mock('@/src/lib/api/bff-client', () => ({
  claimsApi: {
    listClaims: vi.fn(),
  },
}));

describe('useClaimsStore', () => {
  beforeEach(() => {
    useClaimsStore.setState({
      claims: [],
      isLoading: false,
      error: null,
    });
    vi.clearAllMocks();
  });

  describe('Initial State', () => {
    it('should have empty claims list', () => {
      const { result } = renderHook(() => useClaimsStore());
      expect(result.current.claims).toEqual([]);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
    });
  });

  describe('fetchClaims', () => {
    it('should fetch and sort claims by createdAt descending', async () => {
      const older = { claimId: '1', createdAt: new Date('2025-01-01') };
      const newer = { claimId: '2', createdAt: new Date('2025-06-01') };
      vi.mocked(claimsApi.listClaims).mockResolvedValue([older, newer] as any);

      const { result } = renderHook(() => useClaimsStore());
      await act(async () => {
        await result.current.fetchClaims();
      });

      expect(result.current.claims[0].claimId).toBe('2');
      expect(result.current.claims[1].claimId).toBe('1');
      expect(result.current.isLoading).toBe(false);
    });

    it('should set loading state during fetch', async () => {
      let resolvePromise: (value: any) => void;
      vi.mocked(claimsApi.listClaims).mockReturnValue(
        new Promise((resolve) => { resolvePromise = resolve; })
      );

      const { result } = renderHook(() => useClaimsStore());
      const fetchPromise = act(async () => {
        result.current.fetchClaims();
      });

      // Loading should be true during fetch
      expect(useClaimsStore.getState().isLoading).toBe(true);

      resolvePromise!([]);
      await fetchPromise;
    });

    it('should handle 401 UNAUTHORIZED error', async () => {
      const error = new ResponseError(
        new Response(null, { status: 401 }),
        'Unauthorized'
      );
      vi.mocked(claimsApi.listClaims).mockRejectedValue(error);

      const { result } = renderHook(() => useClaimsStore());
      await act(async () => {
        await result.current.fetchClaims();
      });

      expect(result.current.error).toBe('UNAUTHORIZED');
      expect(result.current.isLoading).toBe(false);
    });

    it('should handle generic errors', async () => {
      vi.mocked(claimsApi.listClaims).mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useClaimsStore());
      await act(async () => {
        await result.current.fetchClaims();
      });

      expect(result.current.error).toBe('Failed to fetch claims');
      expect(result.current.isLoading).toBe(false);
    });
  });

  describe('addClaim', () => {
    it('should prepend new claim to list', () => {
      const existing = { claimId: '1', createdAt: new Date() } as any;
      const newClaim = { claimId: '2', createdAt: new Date() } as any;
      useClaimsStore.setState({ claims: [existing] });

      const { result } = renderHook(() => useClaimsStore());
      act(() => {
        result.current.addClaim(newClaim);
      });

      expect(result.current.claims).toHaveLength(2);
      expect(result.current.claims[0].claimId).toBe('2');
    });
  });

  describe('updateClaimStatus', () => {
    it('should update status for matching claim', () => {
      const claim = { claimId: 'abc', status: 'SUBMITTED' } as any;
      useClaimsStore.setState({ claims: [claim] });

      const { result } = renderHook(() => useClaimsStore());
      act(() => {
        result.current.updateClaimStatus('abc', 'APPROVED' as any);
      });

      expect(result.current.claims[0].status).toBe('APPROVED');
    });

    it('should not modify non-matching claims', () => {
      const claim = { claimId: 'abc', status: 'SUBMITTED' } as any;
      useClaimsStore.setState({ claims: [claim] });

      const { result } = renderHook(() => useClaimsStore());
      act(() => {
        result.current.updateClaimStatus('xyz', 'APPROVED' as any);
      });

      expect(result.current.claims[0].status).toBe('SUBMITTED');
    });
  });

  describe('clearError', () => {
    it('should clear the error', () => {
      useClaimsStore.setState({ error: 'some error' });

      const { result } = renderHook(() => useClaimsStore());
      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });
  });
});

import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { useAdminClaimsStore } from './use-admin-claims-store';
import { adminDashboardApi, ClaimStatus } from '@/src/lib/api/bff-client';
import { ResponseError } from '@/src/lib/api/generated/runtime';

vi.mock('@/src/lib/api/bff-client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/src/lib/api/bff-client')>();
  return {
    ...actual,
    adminDashboardApi: {
      listAllClaims: vi.fn(),
      updateClaimStatus: vi.fn(),
    },
  };
});

const mockClaim = (overrides: Partial<{ claimId: string; status: ClaimStatus; userId: string; createdAt: Date }> = {}) => ({
  claimId: overrides.claimId ?? 'claim-001',
  userId: overrides.userId ?? 'user-001',
  incidentDate: new Date('2026-01-15'),
  incidentLocation: 'Office',
  description: 'Test claim description',
  claimAmount: 1500.0,
  status: overrides.status ?? ClaimStatus.Submitted,
  createdAt: overrides.createdAt ?? new Date('2026-01-15'),
  updatedAt: new Date('2026-01-15'),
});

const mockClaims = [
  mockClaim({ claimId: 'claim-001', status: ClaimStatus.Submitted }),
  mockClaim({ claimId: 'claim-002', status: ClaimStatus.UnderReview, userId: 'user-002' }),
  mockClaim({ claimId: 'claim-003', status: ClaimStatus.Approved, userId: 'user-003' }),
];

describe('useAdminClaimsStore', () => {
  beforeEach(() => {
    useAdminClaimsStore.setState({
      claims: [],
      isLoading: false,
      error: null,
      statusFilter: null,
      searchQuery: '',
    });
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('Initial State', () => {
    it('should have empty claims initially', () => {
      const { result } = renderHook(() => useAdminClaimsStore());
      expect(result.current.claims).toEqual([]);
    });

    it('should not be loading initially', () => {
      const { result } = renderHook(() => useAdminClaimsStore());
      expect(result.current.isLoading).toBe(false);
    });

    it('should have no error initially', () => {
      const { result } = renderHook(() => useAdminClaimsStore());
      expect(result.current.error).toBeNull();
    });

    it('should have null statusFilter initially', () => {
      const { result } = renderHook(() => useAdminClaimsStore());
      expect(result.current.statusFilter).toBeNull();
    });

    it('should have empty searchQuery initially', () => {
      const { result } = renderHook(() => useAdminClaimsStore());
      expect(result.current.searchQuery).toBe('');
    });
  });

  describe('fetchClaims', () => {
    it('should fetch all claims and update store on success', async () => {
      vi.mocked(adminDashboardApi.listAllClaims).mockResolvedValue(mockClaims);

      const { result } = renderHook(() => useAdminClaimsStore());

      await act(async () => {
        await result.current.fetchClaims();
      });

      expect(adminDashboardApi.listAllClaims).toHaveBeenCalledWith({});
      expect(result.current.claims).toEqual(mockClaims);
      expect(result.current.isLoading).toBe(false);
    });

    it('should fetch claims filtered by status', async () => {
      vi.mocked(adminDashboardApi.listAllClaims).mockResolvedValue([mockClaims[0]]);

      const { result } = renderHook(() => useAdminClaimsStore());

      await act(async () => {
        await result.current.fetchClaims(ClaimStatus.Submitted);
      });

      expect(adminDashboardApi.listAllClaims).toHaveBeenCalledWith({ status: ClaimStatus.Submitted });
    });

    it('should sort claims by createdAt DESC (newest first)', async () => {
      const unsortedClaims = [
        mockClaim({ claimId: 'claim-old', createdAt: new Date('2026-01-01') }),
        mockClaim({ claimId: 'claim-newest', createdAt: new Date('2026-01-20') }),
        mockClaim({ claimId: 'claim-middle', createdAt: new Date('2026-01-10') }),
      ];
      vi.mocked(adminDashboardApi.listAllClaims).mockResolvedValue(unsortedClaims);

      const { result } = renderHook(() => useAdminClaimsStore());

      await act(async () => {
        await result.current.fetchClaims();
      });

      expect(result.current.claims[0].claimId).toBe('claim-newest');
      expect(result.current.claims[1].claimId).toBe('claim-middle');
      expect(result.current.claims[2].claimId).toBe('claim-old');
    });

    it('should set ACCESS_DENIED error on 403', async () => {
      const mockResponse = new Response(null, { status: 403 });
      vi.mocked(adminDashboardApi.listAllClaims).mockRejectedValue(
        new ResponseError(mockResponse, 'Forbidden')
      );

      const { result } = renderHook(() => useAdminClaimsStore());

      await act(async () => {
        await result.current.fetchClaims();
      });

      expect(result.current.error).toBe('ACCESS_DENIED');
      expect(result.current.isLoading).toBe(false);
    });

    it('should set generic error on other failures', async () => {
      vi.mocked(adminDashboardApi.listAllClaims).mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useAdminClaimsStore());

      await act(async () => {
        await result.current.fetchClaims();
      });

      expect(result.current.error).toBe('Failed to load claims');
      expect(result.current.isLoading).toBe(false);
    });

    it('should set isLoading to true during fetch', async () => {
      let resolvePromise: (value: typeof mockClaims) => void;
      const promise = new Promise<typeof mockClaims>((resolve) => {
        resolvePromise = resolve;
      });

      vi.mocked(adminDashboardApi.listAllClaims).mockReturnValue(promise);

      const { result } = renderHook(() => useAdminClaimsStore());

      act(() => {
        result.current.fetchClaims();
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(true);
      });

      await act(async () => {
        resolvePromise!(mockClaims);
      });

      expect(result.current.isLoading).toBe(false);
    });
  });

  describe('updateClaimStatus', () => {
    it('should optimistically update status and confirm with server response', async () => {
      const updatedClaim = mockClaim({ claimId: 'claim-001', status: 'UNDER_REVIEW' });
      vi.mocked(adminDashboardApi.updateClaimStatus).mockResolvedValue(updatedClaim);

      useAdminClaimsStore.setState({ claims: [...mockClaims] });

      const { result } = renderHook(() => useAdminClaimsStore());

      await act(async () => {
        await result.current.updateClaimStatus('claim-001', ClaimStatus.UnderReview);
      });

      expect(adminDashboardApi.updateClaimStatus).toHaveBeenCalledWith({
        claimId: 'claim-001',
        updateClaimStatusRequest: { newStatus: ClaimStatus.UnderReview },
      });

      const updatedClaimInStore = result.current.claims.find((c) => c.claimId === 'claim-001');
      expect(updatedClaimInStore?.status).toBe(ClaimStatus.UnderReview);
    });

    it('should rollback on error', async () => {
      const mockResponse = new Response(JSON.stringify({ detail: 'Invalid transition' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' },
      });
      vi.mocked(adminDashboardApi.updateClaimStatus).mockRejectedValue(
        new ResponseError(mockResponse, 'Bad Request')
      );

      useAdminClaimsStore.setState({ claims: [...mockClaims] });

      const { result } = renderHook(() => useAdminClaimsStore());

      await act(async () => {
        try {
          await result.current.updateClaimStatus('claim-001', ClaimStatus.Closed);
        } catch {
          // Expected
        }
      });

      // Should rollback to original status
      const claimInStore = result.current.claims.find((c) => c.claimId === 'claim-001');
      expect(claimInStore?.status).toBe(ClaimStatus.Submitted);
      expect(result.current.error).toBe('Invalid transition');
    });

    it('should rollback on network error', async () => {
      vi.mocked(adminDashboardApi.updateClaimStatus).mockRejectedValue(new Error('Network error'));

      useAdminClaimsStore.setState({ claims: [...mockClaims] });

      const { result } = renderHook(() => useAdminClaimsStore());

      await act(async () => {
        try {
          await result.current.updateClaimStatus('claim-001', ClaimStatus.UnderReview);
        } catch {
          // Expected
        }
      });

      const claimInStore = result.current.claims.find((c) => c.claimId === 'claim-001');
      expect(claimInStore?.status).toBe(ClaimStatus.Submitted);
      expect(result.current.error).toBe('Failed to update claim status');
    });
  });

  describe('setStatusFilter', () => {
    it('should set filter and trigger fetch', async () => {
      vi.mocked(adminDashboardApi.listAllClaims).mockResolvedValue([]);

      const { result } = renderHook(() => useAdminClaimsStore());

      await act(async () => {
        result.current.setStatusFilter(ClaimStatus.Approved);
      });

      expect(result.current.statusFilter).toBe(ClaimStatus.Approved);
      expect(adminDashboardApi.listAllClaims).toHaveBeenCalledWith({ status: ClaimStatus.Approved });
    });
  });

  describe('setSearchQuery', () => {
    it('should update search query without API call', () => {
      const { result } = renderHook(() => useAdminClaimsStore());

      act(() => {
        result.current.setSearchQuery('user-001');
      });

      expect(result.current.searchQuery).toBe('user-001');
      expect(adminDashboardApi.listAllClaims).not.toHaveBeenCalled();
    });
  });

  describe('clearFilters', () => {
    it('should reset filters and fetch all claims', async () => {
      vi.mocked(adminDashboardApi.listAllClaims).mockResolvedValue(mockClaims);

      useAdminClaimsStore.setState({
        statusFilter: ClaimStatus.Approved,
        searchQuery: 'test',
      });

      const { result } = renderHook(() => useAdminClaimsStore());

      await act(async () => {
        result.current.clearFilters();
      });

      expect(result.current.statusFilter).toBeNull();
      expect(result.current.searchQuery).toBe('');
      expect(adminDashboardApi.listAllClaims).toHaveBeenCalledWith({});
    });
  });

  describe('clearError', () => {
    it('should clear the error', () => {
      useAdminClaimsStore.setState({ error: 'some error' });

      const { result } = renderHook(() => useAdminClaimsStore());

      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });
  });
});

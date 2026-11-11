import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { useClaimWizardStore } from './use-claim-wizard-store';
import { claimsApi } from '@/src/lib/api/bff-client';
import { ResponseError } from '@/src/lib/api/generated/runtime';

vi.mock('@/src/lib/api/bff-client', () => ({
  claimsApi: {
    submitClaim: vi.fn(),
  },
}));

describe('useClaimWizardStore', () => {
  beforeEach(() => {
    // Reset store to initial state
    useClaimWizardStore.setState({
      step: 1,
      formData: {},
      isSubmitting: false,
      error: null,
      fieldErrors: {},
    });
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('Initial State', () => {
    it('should have initial step as 1', () => {
      const { result } = renderHook(() => useClaimWizardStore());
      expect(result.current.step).toBe(1);
    });

    it('should have empty form data initially', () => {
      const { result } = renderHook(() => useClaimWizardStore());
      expect(result.current.formData).toEqual({});
    });

    it('should not be submitting initially', () => {
      const { result } = renderHook(() => useClaimWizardStore());
      expect(result.current.isSubmitting).toBe(false);
    });

    it('should have no error initially', () => {
      const { result } = renderHook(() => useClaimWizardStore());
      expect(result.current.error).toBeNull();
    });

    it('should have no field errors initially', () => {
      const { result } = renderHook(() => useClaimWizardStore());
      expect(result.current.fieldErrors).toEqual({});
    });
  });

  describe('Navigation Actions', () => {
    it('should set step directly with setStep', () => {
      const { result } = renderHook(() => useClaimWizardStore());

      act(() => {
        result.current.setStep(3);
      });

      expect(result.current.step).toBe(3);
    });

    it('should increment step with nextStep', () => {
      const { result } = renderHook(() => useClaimWizardStore());

      act(() => {
        result.current.nextStep();
      });

      expect(result.current.step).toBe(2);
    });

    it('should not go beyond step 3 with nextStep', () => {
      const { result } = renderHook(() => useClaimWizardStore());

      act(() => {
        result.current.setStep(3);
        result.current.nextStep();
      });

      expect(result.current.step).toBe(3);
    });

    it('should decrement step with previousStep', () => {
      const { result } = renderHook(() => useClaimWizardStore());

      act(() => {
        result.current.setStep(2);
        result.current.previousStep();
      });

      expect(result.current.step).toBe(1);
    });

    it('should not go below step 1 with previousStep', () => {
      const { result } = renderHook(() => useClaimWizardStore());

      act(() => {
        result.current.previousStep();
      });

      expect(result.current.step).toBe(1);
    });
  });

  describe('Form Data Management', () => {
    it('should update form data with updateFormData', () => {
      const { result } = renderHook(() => useClaimWizardStore());
      const testDate = new Date('2026-02-15');

      act(() => {
        result.current.updateFormData({
          incidentDate: testDate,
          incidentLocation: 'Test Location',
        });
      });

      expect(result.current.formData).toEqual({
        incidentDate: testDate,
        incidentLocation: 'Test Location',
      });
    });

    it('should merge form data on multiple updates', () => {
      const { result } = renderHook(() => useClaimWizardStore());
      const testDate = new Date('2026-02-15');

      act(() => {
        result.current.updateFormData({
          incidentDate: testDate,
        });
        result.current.updateFormData({
          incidentLocation: 'Test Location',
        });
      });

      expect(result.current.formData).toEqual({
        incidentDate: testDate,
        incidentLocation: 'Test Location',
      });
    });
  });

  describe('Submit Claim - Success', () => {
    it('should successfully submit claim and return response', async () => {
      const mockResponse = {
        claimId: 'claim-123',
        userId: 'user-456',
        incidentDate: '2026-02-15',
        incidentLocation: 'Test Location',
        description: 'Test description',
        claimAmount: 1000,
        status: 'SUBMITTED' as const,
        createdAt: '2026-02-17T10:00:00Z',
        updatedAt: '2026-02-17T10:00:00Z',
      };

      vi.mocked(claimsApi.submitClaim).mockResolvedValue(mockResponse);

      const { result } = renderHook(() => useClaimWizardStore());
      const testDate = new Date('2026-02-15');

      act(() => {
        result.current.updateFormData({
          incidentDate: testDate,
          incidentLocation: 'Test Location',
          claimAmount: 1000,
          description: 'Test description',
        });
      });

      let response;
      await act(async () => {
        response = await result.current.submitClaim();
      });

      expect(response).toEqual(mockResponse);
      expect(result.current.isSubmitting).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should clear errors before submission', async () => {
      const mockResponse = {
        claimId: 'claim-123',
        userId: 'user-456',
        incidentDate: '2026-02-15',
        incidentLocation: 'Test Location',
        description: 'Test description',
        claimAmount: 1000,
        status: 'SUBMITTED' as const,
        createdAt: '2026-02-17T10:00:00Z',
        updatedAt: '2026-02-17T10:00:00Z',
      };

      vi.mocked(claimsApi.submitClaim).mockResolvedValue(mockResponse);

      const { result } = renderHook(() => useClaimWizardStore());
      const testDate = new Date('2026-02-15');

      // Set an error first
      act(() => {
        useClaimWizardStore.setState({
          error: 'Previous error',
          fieldErrors: { test: 'error' },
        });
      });

      act(() => {
        result.current.updateFormData({
          incidentDate: testDate,
          incidentLocation: 'Test Location',
          claimAmount: 1000,
          description: 'Test description',
        });
      });

      await act(async () => {
        await result.current.submitClaim();
      });

      expect(result.current.error).toBeNull();
      expect(result.current.fieldErrors).toEqual({});
    });
  });

  describe('Submit Claim - 400 Validation Error', () => {
    it('should handle 400 error with field errors', async () => {
      const mockErrorResponse = {
        detail: 'Validation failed',
        fieldErrors: {
          claimAmount: 'Amount exceeds policy limit',
          incidentDate: 'Date is outside coverage period',
        },
      };

      const mockError = new ResponseError(
        new Response(JSON.stringify(mockErrorResponse), {
          status: 400,
          headers: { 'Content-Type': 'application/json' },
        })
      );

      vi.mocked(claimsApi.submitClaim).mockRejectedValue(mockError);

      const { result } = renderHook(() => useClaimWizardStore());
      const testDate = new Date('2026-02-15');

      act(() => {
        result.current.updateFormData({
          incidentDate: testDate,
          incidentLocation: 'Test Location',
          claimAmount: 1000,
          description: 'Test description',
        });
      });

      let response;
      await act(async () => {
        response = await result.current.submitClaim();
      });

      expect(response).toBeNull();
      expect(result.current.error).toBe('Validation failed');
      expect(result.current.fieldErrors).toEqual({
        claimAmount: 'Amount exceeds policy limit',
        incidentDate: 'Date is outside coverage period',
      });
      expect(result.current.isSubmitting).toBe(false);
    });

    it('should handle 400 error without field errors', async () => {
      const mockErrorResponse = {
        detail: 'Generic validation error',
      };

      const mockError = new ResponseError(
        new Response(JSON.stringify(mockErrorResponse), {
          status: 400,
          headers: { 'Content-Type': 'application/json' },
        })
      );

      vi.mocked(claimsApi.submitClaim).mockRejectedValue(mockError);

      const { result } = renderHook(() => useClaimWizardStore());
      const testDate = new Date('2026-02-15');

      act(() => {
        result.current.updateFormData({
          incidentDate: testDate,
          incidentLocation: 'Test Location',
          claimAmount: 1000,
          description: 'Test description',
        });
      });

      await act(async () => {
        await result.current.submitClaim();
      });

      expect(result.current.error).toBe('Generic validation error');
      expect(result.current.fieldErrors).toEqual({});
    });

    it('should handle 400 error with invalid JSON response', async () => {
      const mockError = new ResponseError(
        new Response('Invalid JSON', {
          status: 400,
          headers: { 'Content-Type': 'text/plain' },
        })
      );

      vi.mocked(claimsApi.submitClaim).mockRejectedValue(mockError);

      const { result } = renderHook(() => useClaimWizardStore());
      const testDate = new Date('2026-02-15');

      act(() => {
        result.current.updateFormData({
          incidentDate: testDate,
          incidentLocation: 'Test Location',
          claimAmount: 1000,
          description: 'Test description',
        });
      });

      await act(async () => {
        await result.current.submitClaim();
      });

      expect(result.current.error).toBe('Validation failed');
      expect(result.current.isSubmitting).toBe(false);
    });
  });

  describe('Submit Claim - 500 Server Error', () => {
    it('should handle 500 error with generic message', async () => {
      const mockError = new Error('Network error');

      vi.mocked(claimsApi.submitClaim).mockRejectedValue(mockError);

      const { result } = renderHook(() => useClaimWizardStore());
      const testDate = new Date('2026-02-15');

      act(() => {
        result.current.updateFormData({
          incidentDate: testDate,
          incidentLocation: 'Test Location',
          claimAmount: 1000,
          description: 'Test description',
        });
      });

      await act(async () => {
        await result.current.submitClaim();
      });

      expect(result.current.error).toBe(
        'Failed to submit claim. Please try again.'
      );
      expect(result.current.isSubmitting).toBe(false);
    });
  });

  describe('Reset Wizard', () => {
    it('should reset all state to initial values', () => {
      const { result } = renderHook(() => useClaimWizardStore());
      const testDate = new Date('2026-02-15');

      act(() => {
        result.current.setStep(3);
        result.current.updateFormData({
          incidentDate: testDate,
          incidentLocation: 'Test Location',
        });
        useClaimWizardStore.setState({
          error: 'Test error',
          fieldErrors: { field: 'error' },
        });
      });

      act(() => {
        result.current.resetWizard();
      });

      expect(result.current.step).toBe(1);
      expect(result.current.formData).toEqual({});
      expect(result.current.isSubmitting).toBe(false);
      expect(result.current.error).toBeNull();
      expect(result.current.fieldErrors).toEqual({});
    });
  });
});

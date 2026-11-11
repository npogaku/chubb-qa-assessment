import { describe, it, expect } from 'vitest';
import { vi } from 'vitest';

// Mock the generated API module before importing
vi.mock('@/src/lib/api/bff-client', () => ({
  ClaimStatus: {
    SUBMITTED: 'SUBMITTED',
    UNDER_REVIEW: 'UNDER_REVIEW',
    APPROVED: 'APPROVED',
    REJECTED: 'REJECTED',
    CLOSED: 'CLOSED',
  },
}));

import {
  VALID_TRANSITIONS,
  getValidTransitions,
  STATUS_LABELS,
} from './claim-status-transitions';

describe('VALID_TRANSITIONS', () => {
  it('SUBMITTED can transition to UNDER_REVIEW or REJECTED', () => {
    expect(VALID_TRANSITIONS.SUBMITTED).toEqual(['UNDER_REVIEW', 'REJECTED']);
  });

  it('UNDER_REVIEW can transition to APPROVED, REJECTED, or SUBMITTED', () => {
    expect(VALID_TRANSITIONS.UNDER_REVIEW).toEqual([
      'APPROVED',
      'REJECTED',
      'SUBMITTED',
    ]);
  });

  it('APPROVED can only transition to CLOSED', () => {
    expect(VALID_TRANSITIONS.APPROVED).toEqual(['CLOSED']);
  });

  it('REJECTED can only transition to CLOSED', () => {
    expect(VALID_TRANSITIONS.REJECTED).toEqual(['CLOSED']);
  });

  it('CLOSED has no valid transitions', () => {
    expect(VALID_TRANSITIONS.CLOSED).toEqual([]);
  });
});

describe('getValidTransitions', () => {
  it('returns valid transitions for known status', () => {
    expect(getValidTransitions('SUBMITTED' as any)).toEqual([
      'UNDER_REVIEW',
      'REJECTED',
    ]);
  });

  it('returns empty array for unknown status', () => {
    expect(getValidTransitions('UNKNOWN' as any)).toEqual([]);
  });
});

describe('STATUS_LABELS', () => {
  it('has labels for all five statuses', () => {
    expect(STATUS_LABELS.SUBMITTED).toBe('Submitted');
    expect(STATUS_LABELS.UNDER_REVIEW).toBe('Under Review');
    expect(STATUS_LABELS.APPROVED).toBe('Approved');
    expect(STATUS_LABELS.REJECTED).toBe('Rejected');
    expect(STATUS_LABELS.CLOSED).toBe('Closed');
  });
});

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';

vi.mock('@/src/lib/api/bff-client', () => ({
  ClaimStatus: {
    SUBMITTED: 'SUBMITTED',
    UNDER_REVIEW: 'UNDER_REVIEW',
    APPROVED: 'APPROVED',
    REJECTED: 'REJECTED',
    CLOSED: 'CLOSED',
  },
}));

import { ClaimStatusBadge } from './claim-status-badge';

describe('ClaimStatusBadge', () => {
  it.each([
    ['SUBMITTED', 'Submitted'],
    ['UNDER_REVIEW', 'Under Review'],
    ['APPROVED', 'Approved'],
    ['REJECTED', 'Rejected'],
    ['CLOSED', 'Closed'],
  ] as const)('renders %s as "%s"', (status, label) => {
    render(<ClaimStatusBadge status={status as any} />);
    expect(screen.getByRole('status')).toHaveTextContent(label);
    expect(screen.getByRole('status')).toHaveAttribute(
      'aria-label',
      `Claim status: ${label}`
    );
  });

  it('renders Unknown for invalid status', () => {
    render(<ClaimStatusBadge status={'INVALID' as any} />);
    expect(screen.getByRole('status')).toHaveTextContent('Unknown');
  });
});

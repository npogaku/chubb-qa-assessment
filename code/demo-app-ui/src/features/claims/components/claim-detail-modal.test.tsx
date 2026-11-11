import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ClaimDetailModal } from './claim-detail-modal';

const mockGetClaim = vi.fn();

vi.mock('@/src/lib/api/bff-client', () => ({
  claimsApi: {
    getClaim: (...args: unknown[]) => mockGetClaim(...args),
  },
  ClaimStatus: {
    SUBMITTED: 'SUBMITTED',
    UNDER_REVIEW: 'UNDER_REVIEW',
    APPROVED: 'APPROVED',
    REJECTED: 'REJECTED',
    CLOSED: 'CLOSED',
  },
}));

const mockClaim = {
  claimId: '123e4567-e89b-12d3-a456-426614174000',
  userId: 'user-1',
  incidentDate: new Date('2026-01-15'),
  incidentLocation: '123 Main St, Springfield',
  description: 'Water damage to basement after pipe burst during winter storm.',
  claimAmount: 15000.5,
  status: 'SUBMITTED' as const,
  createdAt: new Date('2026-02-01T10:00:00Z'),
  updatedAt: new Date('2026-02-05T14:30:00Z'),
};

describe('ClaimDetailModal', () => {
  const defaultProps = {
    isOpen: true,
    claimId: '123e4567-e89b-12d3-a456-426614174000',
    onClose: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockGetClaim.mockResolvedValue(mockClaim);
  });

  it('renders all claim fields correctly', async () => {
    render(<ClaimDetailModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('123e4567-e89b-12d3-a456-426614174000')).toBeInTheDocument();
    });

    expect(screen.getByText('123 Main St, Springfield')).toBeInTheDocument();
    expect(screen.getByText('Water damage to basement after pipe burst during winter storm.')).toBeInTheDocument();
    expect(screen.getByText('$15,000.50')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Submitted');
    // Date formatting - en-US short format
    expect(screen.getByText('Jan 15, 2026')).toBeInTheDocument();
    expect(screen.getByText('Feb 1, 2026')).toBeInTheDocument();
    expect(screen.getByText('Feb 5, 2026')).toBeInTheDocument();
  });

  it('shows loading state while fetching', () => {
    mockGetClaim.mockReturnValue(new Promise(() => {})); // never resolves
    render(<ClaimDetailModal {...defaultProps} />);

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText('Loading claim details...')).toBeInTheDocument();
  });

  it('shows error state on API failure', async () => {
    mockGetClaim.mockRejectedValue(new Error('Network error'));
    render(<ClaimDetailModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    expect(screen.getByText('Failed to load claim details. Please try again.')).toBeInTheDocument();
  });

  it('shows unauthorized error for 401', async () => {
    mockGetClaim.mockRejectedValue({ response: { status: 401 } });
    render(<ClaimDetailModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('You are not authorized to view this claim.')).toBeInTheDocument();
    });
  });

  it('shows not found error for 404', async () => {
    mockGetClaim.mockRejectedValue({ response: { status: 404 } });
    render(<ClaimDetailModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Claim not found.')).toBeInTheDocument();
    });
  });

  it('closes on close button click', async () => {
    const onClose = vi.fn();
    render(<ClaimDetailModal {...defaultProps} onClose={onClose} />);

    await userEvent.click(screen.getByRole('button', { name: 'Close' }));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('closes on backdrop click', async () => {
    const onClose = vi.fn();
    render(<ClaimDetailModal {...defaultProps} onClose={onClose} />);

    await userEvent.click(screen.getByTestId('claim-detail-modal'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('closes on Escape key', async () => {
    const onClose = vi.fn();
    render(<ClaimDetailModal {...defaultProps} onClose={onClose} />);

    await userEvent.keyboard('{Escape}');
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('renders ClaimStatusBadge with correct status', async () => {
    render(<ClaimDetailModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByRole('status')).toHaveAttribute(
        'aria-label',
        'Claim status: Submitted'
      );
    });
  });

  it('does not render when closed', () => {
    const { container } = render(
      <ClaimDetailModal {...defaultProps} isOpen={false} />
    );

    expect(container.innerHTML).toBe('');
  });

  it('has correct ARIA attributes', async () => {
    render(<ClaimDetailModal {...defaultProps} />);

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(dialog).toHaveAttribute('aria-labelledby', 'claim-detail-title');
  });

  it('calls getClaim with correct claimId', async () => {
    render(<ClaimDetailModal {...defaultProps} />);

    await waitFor(() => {
      expect(mockGetClaim).toHaveBeenCalledWith({ claimId: '123e4567-e89b-12d3-a456-426614174000' });
    });
  });
});

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';

const mockConnectionState = vi.fn().mockReturnValue('disconnected');
const mockConnectionError = vi.fn().mockReturnValue(null);

vi.mock('@/src/features/claims/stores/use-realtime-store', () => ({
  useRealtimeStore: (selector: (s: any) => any) => {
    const state = {
      connectionState: mockConnectionState(),
      connectionError: mockConnectionError(),
    };
    return selector(state);
  },
}));

import { ConnectionStatusIndicator } from './connection-status-indicator';

describe('ConnectionStatusIndicator', () => {
  beforeEach(() => {
    mockConnectionState.mockReturnValue('disconnected');
    mockConnectionError.mockReturnValue(null);
  });

  it('shows "Offline" when disconnected', () => {
    render(<ConnectionStatusIndicator />);
    expect(screen.getByRole('status')).toHaveTextContent('Offline');
  });

  it('shows "Live" when connected', () => {
    mockConnectionState.mockReturnValue('connected');
    render(<ConnectionStatusIndicator />);
    expect(screen.getByRole('status')).toHaveTextContent('Live');
  });

  it('shows "Reconnecting..." when reconnecting', () => {
    mockConnectionState.mockReturnValue('reconnecting');
    render(<ConnectionStatusIndicator />);
    expect(screen.getByRole('status')).toHaveTextContent('Reconnecting...');
  });

  it('shows "Offline" with error message when connection error exists', () => {
    mockConnectionError.mockReturnValue('Max retries reached');
    render(<ConnectionStatusIndicator />);
    expect(screen.getByRole('status')).toHaveTextContent('Offline');
    expect(screen.getByRole('status')).toHaveAttribute(
      'title',
      'Real-time updates unavailable. Max reconnection attempts reached.'
    );
  });
});

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { useRealtimeStore } from '@/src/features/claims/stores/use-realtime-store';

describe('useRealtimeStore', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    useRealtimeStore.setState({
      connectionState: 'disconnected',
      connectionError: false,
      lastMessage: null,
      recentlyUpdatedClaimIds: new Set(),
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('has correct default state', () => {
    const state = useRealtimeStore.getState();
    expect(state.connectionState).toBe('disconnected');
    expect(state.connectionError).toBe(false);
    expect(state.lastMessage).toBeNull();
    expect(state.recentlyUpdatedClaimIds.size).toBe(0);
  });

  it('setConnectionState updates connection state', () => {
    useRealtimeStore.getState().setConnectionState('connected');
    expect(useRealtimeStore.getState().connectionState).toBe('connected');
  });

  it('setConnectionState transitions through states', () => {
    useRealtimeStore.getState().setConnectionState('reconnecting');
    expect(useRealtimeStore.getState().connectionState).toBe('reconnecting');

    useRealtimeStore.getState().setConnectionState('connected');
    expect(useRealtimeStore.getState().connectionState).toBe('connected');
  });

  it('setConnectionError updates error state', () => {
    useRealtimeStore.getState().setConnectionError(true);
    expect(useRealtimeStore.getState().connectionError).toBe(true);
  });

  it('setLastMessage stores the message', () => {
    const message = {
      type: 'CLAIM_STATUS_CHANGED' as const,
      claimId: 'test-123',
      newStatus: 'APPROVED',
      oldStatus: 'UNDER_REVIEW',
      timestamp: '2026-02-17T12:00:00Z',
      correlationId: 'corr-1',
    };

    useRealtimeStore.getState().setLastMessage(message);
    expect(useRealtimeStore.getState().lastMessage).toEqual(message);
  });

  it('markClaimUpdated adds claim ID to recently updated set', () => {
    useRealtimeStore.getState().markClaimUpdated('claim-1');
    expect(useRealtimeStore.getState().recentlyUpdatedClaimIds.has('claim-1')).toBe(true);
  });

  it('markClaimUpdated auto-clears after 2 seconds', () => {
    useRealtimeStore.getState().markClaimUpdated('claim-1');
    expect(useRealtimeStore.getState().recentlyUpdatedClaimIds.has('claim-1')).toBe(true);

    vi.advanceTimersByTime(2000);
    expect(useRealtimeStore.getState().recentlyUpdatedClaimIds.has('claim-1')).toBe(false);
  });

  it('markClaimUpdated tracks multiple claims independently', () => {
    useRealtimeStore.getState().markClaimUpdated('claim-1');
    vi.advanceTimersByTime(1000);
    useRealtimeStore.getState().markClaimUpdated('claim-2');

    // claim-1 still has 1s left, claim-2 has 2s
    expect(useRealtimeStore.getState().recentlyUpdatedClaimIds.has('claim-1')).toBe(true);
    expect(useRealtimeStore.getState().recentlyUpdatedClaimIds.has('claim-2')).toBe(true);

    vi.advanceTimersByTime(1000);
    // claim-1 cleared, claim-2 still has 1s
    expect(useRealtimeStore.getState().recentlyUpdatedClaimIds.has('claim-1')).toBe(false);
    expect(useRealtimeStore.getState().recentlyUpdatedClaimIds.has('claim-2')).toBe(true);

    vi.advanceTimersByTime(1000);
    expect(useRealtimeStore.getState().recentlyUpdatedClaimIds.has('claim-2')).toBe(false);
  });

  it('clearClaimUpdated removes specific claim', () => {
    useRealtimeStore.getState().markClaimUpdated('claim-1');
    useRealtimeStore.getState().markClaimUpdated('claim-2');

    useRealtimeStore.getState().clearClaimUpdated('claim-1');
    expect(useRealtimeStore.getState().recentlyUpdatedClaimIds.has('claim-1')).toBe(false);
    expect(useRealtimeStore.getState().recentlyUpdatedClaimIds.has('claim-2')).toBe(true);
  });

  it('clearAllTimeouts clears all pending timeouts and claim IDs', () => {
    useRealtimeStore.getState().markClaimUpdated('claim-1');
    useRealtimeStore.getState().markClaimUpdated('claim-2');
    useRealtimeStore.getState().markClaimUpdated('claim-3');

    expect(useRealtimeStore.getState().recentlyUpdatedClaimIds.size).toBe(3);
    expect(useRealtimeStore.getState().highlightTimeouts.size).toBe(3);

    useRealtimeStore.getState().clearAllTimeouts();

    expect(useRealtimeStore.getState().recentlyUpdatedClaimIds.size).toBe(0);
    expect(useRealtimeStore.getState().highlightTimeouts.size).toBe(0);
  });

  it('replaces timeout when same claim marked updated twice', () => {
    useRealtimeStore.getState().markClaimUpdated('claim-1');
    vi.advanceTimersByTime(1000);

    // Mark again before first timeout expires
    useRealtimeStore.getState().markClaimUpdated('claim-1');

    // Should still have only 1 timeout
    expect(useRealtimeStore.getState().highlightTimeouts.size).toBe(1);

    // After original 2s from first call, claim should still be highlighted
    vi.advanceTimersByTime(1000);
    expect(useRealtimeStore.getState().recentlyUpdatedClaimIds.has('claim-1')).toBe(true);

    // After 2s from second call, claim should be cleared
    vi.advanceTimersByTime(1000);
    expect(useRealtimeStore.getState().recentlyUpdatedClaimIds.has('claim-1')).toBe(false);
  });
});

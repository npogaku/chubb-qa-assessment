import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useRealtimeClaimUpdates, type WebSocketMessage } from '@/src/features/claims/hooks/use-realtime-claim-updates';
import { useAuthStore } from '@/src/features/auth/stores/use-auth-store';

// Mock WebSocket
class MockWebSocket {
  static instances: MockWebSocket[] = [];
  url: string;
  onopen: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  readyState = 0;

  constructor(url: string) {
    this.url = url;
    MockWebSocket.instances.push(this);
  }

  close() {
    this.readyState = 3;
    this.onclose?.(new CloseEvent('close'));
  }

  simulateOpen() {
    this.readyState = 1;
    this.onopen?.(new Event('open'));
  }

  simulateMessage(data: WebSocketMessage) {
    this.onmessage?.(new MessageEvent('message', { data: JSON.stringify(data) }));
  }

  simulateError() {
    this.onerror?.(new Event('error'));
  }

  static reset() {
    MockWebSocket.instances = [];
  }
}

describe('useRealtimeClaimUpdates', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    MockWebSocket.reset();
    vi.stubGlobal('WebSocket', MockWebSocket);
    useAuthStore.setState({ user: null, isAuthenticated: false });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('does not connect when user is not authenticated', () => {
    renderHook(() => useRealtimeClaimUpdates());
    expect(MockWebSocket.instances).toHaveLength(0);
  });

  it('connects when user is authenticated', () => {
    useAuthStore.setState({
      user: { userId: '1', email: 'test@demo.com', name: 'Test', role: 'CLAIMANT' },
      isAuthenticated: true,
    });

    renderHook(() => useRealtimeClaimUpdates());
    expect(MockWebSocket.instances).toHaveLength(1);
    expect(MockWebSocket.instances[0].url).toContain('/api/ws/claims');
  });

  it('returns isConnected=true after successful connection', async () => {
    useAuthStore.setState({
      user: { userId: '1', email: 'test@demo.com', name: 'Test', role: 'CLAIMANT' },
      isAuthenticated: true,
    });

    const { result } = renderHook(() => useRealtimeClaimUpdates());

    act(() => {
      MockWebSocket.instances[0].simulateOpen();
    });

    expect(result.current.isConnected).toBe(true);
    expect(result.current.connectionState).toBe('connected');
    expect(result.current.connectionError).toBe(false);
  });

  it('calls onMessage callback when message received', async () => {
    useAuthStore.setState({
      user: { userId: '1', email: 'test@demo.com', name: 'Test', role: 'CLAIMANT' },
      isAuthenticated: true,
    });

    const onMessage = vi.fn();
    const { result } = renderHook(() => useRealtimeClaimUpdates(onMessage));

    act(() => {
      MockWebSocket.instances[0].simulateOpen();
    });

    const message: WebSocketMessage = {
      type: 'CLAIM_STATUS_CHANGED',
      claimId: 'claim-123',
      newStatus: 'APPROVED',
      oldStatus: 'UNDER_REVIEW',
      timestamp: '2026-02-17T12:00:00Z',
      correlationId: 'corr-1',
    };

    act(() => {
      MockWebSocket.instances[0].simulateMessage(message);
    });

    expect(onMessage).toHaveBeenCalledWith(message);
    expect(result.current.lastMessage).toEqual(message);
  });

  it('attempts reconnection with exponential backoff on disconnect', async () => {
    useAuthStore.setState({
      user: { userId: '1', email: 'test@demo.com', name: 'Test', role: 'CLAIMANT' },
      isAuthenticated: true,
    });

    const { result } = renderHook(() => useRealtimeClaimUpdates());

    act(() => {
      MockWebSocket.instances[0].simulateOpen();
    });

    // Simulate disconnect
    act(() => {
      MockWebSocket.instances[0].close();
    });

    expect(result.current.connectionState).toBe('reconnecting');

    // After 1s delay, should attempt reconnect (1st retry)
    act(() => {
      vi.advanceTimersByTime(1000);
    });

    expect(MockWebSocket.instances).toHaveLength(2);
  });

  it('sets connectionError after max retries exhausted', () => {
    useAuthStore.setState({
      user: { userId: '1', email: 'test@demo.com', name: 'Test', role: 'CLAIMANT' },
      isAuthenticated: true,
    });

    const { result } = renderHook(() => useRealtimeClaimUpdates());

    // Exhaust all 5 retries
    for (let i = 0; i < 6; i++) {
      const instance = MockWebSocket.instances[MockWebSocket.instances.length - 1];
      act(() => {
        instance.close();
      });

      if (i < 5) {
        const delay = Math.min(1000 * Math.pow(2, i), 16000);
        act(() => {
          vi.advanceTimersByTime(delay);
        });
      }
    }

    expect(result.current.connectionError).toBe(true);
    expect(result.current.connectionState).toBe('disconnected');
  });

  it('resets retry count on successful reconnection', () => {
    useAuthStore.setState({
      user: { userId: '1', email: 'test@demo.com', name: 'Test', role: 'CLAIMANT' },
      isAuthenticated: true,
    });

    const { result } = renderHook(() => useRealtimeClaimUpdates());

    // Connect, disconnect, reconnect
    act(() => {
      MockWebSocket.instances[0].simulateOpen();
    });
    act(() => {
      MockWebSocket.instances[0].close();
    });
    act(() => {
      vi.advanceTimersByTime(1000);
    });
    act(() => {
      MockWebSocket.instances[1].simulateOpen();
    });

    expect(result.current.isConnected).toBe(true);
    expect(result.current.connectionError).toBe(false);
  });

  it('disconnects on unmount', () => {
    useAuthStore.setState({
      user: { userId: '1', email: 'test@demo.com', name: 'Test', role: 'CLAIMANT' },
      isAuthenticated: true,
    });

    const { unmount } = renderHook(() => useRealtimeClaimUpdates());

    act(() => {
      MockWebSocket.instances[0].simulateOpen();
    });

    unmount();

    expect(MockWebSocket.instances[0].readyState).toBe(3); // CLOSED
  });

  it('logs error when receiving invalid JSON message', () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    useAuthStore.setState({
      user: { userId: '1', email: 'test@demo.com', name: 'Test', role: 'CLAIMANT' },
      isAuthenticated: true,
    });

    renderHook(() => useRealtimeClaimUpdates());

    act(() => {
      MockWebSocket.instances[0].simulateOpen();
    });

    // Send invalid JSON (not a ping)
    act(() => {
      const invalidMessage = new MessageEvent('message', { data: 'invalid-json-data' });
      MockWebSocket.instances[0].onmessage?.(invalidMessage);
    });

    expect(consoleErrorSpy).toHaveBeenCalledWith(
      expect.stringContaining('WebSocket message parse error'),
      expect.any(Error),
      expect.any(String),
      'invalid-json-data'
    );

    consoleErrorSpy.mockRestore();
  });

  it('logs error on WebSocket error event', () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    useAuthStore.setState({
      user: { userId: '1', email: 'test@demo.com', name: 'Test', role: 'CLAIMANT' },
      isAuthenticated: true,
    });

    renderHook(() => useRealtimeClaimUpdates());

    act(() => {
      MockWebSocket.instances[0].simulateOpen();
    });

    act(() => {
      MockWebSocket.instances[0].simulateError();
    });

    expect(consoleErrorSpy).toHaveBeenCalledWith(
      expect.stringContaining('WebSocket error occurred'),
      expect.any(Event)
    );

    consoleErrorSpy.mockRestore();
  });
});

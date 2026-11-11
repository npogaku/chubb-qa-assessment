import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useCopyClipboard } from './use-copy-clipboard';
import toast from 'react-hot-toast';

vi.mock('react-hot-toast', () => ({
  default: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('useCopyClipboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should copy text to clipboard and show success toast', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, {
      clipboard: { writeText },
    });

    const { result } = renderHook(() => useCopyClipboard());
    await act(async () => {
      await result.current.copyToClipboard('hello');
    });

    expect(writeText).toHaveBeenCalledWith('hello');
    expect(toast.success).toHaveBeenCalledWith('Copied to clipboard');
  });

  it('should use custom success message', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, {
      clipboard: { writeText },
    });

    const { result } = renderHook(() => useCopyClipboard());
    await act(async () => {
      await result.current.copyToClipboard('id-123', 'ID copied!');
    });

    expect(toast.success).toHaveBeenCalledWith('ID copied!');
  });

  it('should show error toast on clipboard failure', async () => {
    const writeText = vi.fn().mockRejectedValue(new Error('denied'));
    Object.assign(navigator, {
      clipboard: { writeText },
    });

    const { result } = renderHook(() => useCopyClipboard());
    await act(async () => {
      await result.current.copyToClipboard('hello');
    });

    expect(toast.error).toHaveBeenCalledWith('Failed to copy');
  });
});

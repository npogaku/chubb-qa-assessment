import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render } from '@testing-library/react';
import { ThemeProvider } from '@/src/features/theme/components/theme-provider';
import { useThemeStore } from '@/src/features/theme/stores/use-theme-store';

describe('ThemeProvider', () => {
  beforeEach(() => {
    // Reset store and clear document classes
    useThemeStore.setState({ theme: 'default', isDark: false });
    document.documentElement.className = '';
  });

  it('applies theme-default class to html element on mount', () => {
    useThemeStore.setState({ theme: 'default', isDark: false });
    render(<ThemeProvider />);

    expect(document.documentElement.classList.contains('theme-default')).toBe(true);
  });

  it('applies theme-corporate class when theme is corporate', () => {
    useThemeStore.setState({ theme: 'corporate', isDark: false });
    render(<ThemeProvider />);

    expect(document.documentElement.classList.contains('theme-corporate')).toBe(true);
    expect(document.documentElement.classList.contains('theme-default')).toBe(false);
  });

  it('applies theme-warm class when theme is warm', () => {
    useThemeStore.setState({ theme: 'warm', isDark: false });
    render(<ThemeProvider />);

    expect(document.documentElement.classList.contains('theme-warm')).toBe(true);
    expect(document.documentElement.classList.contains('theme-default')).toBe(false);
  });

  it('applies dark class when isDark is true', () => {
    useThemeStore.setState({ theme: 'default', isDark: true });
    render(<ThemeProvider />);

    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(document.documentElement.classList.contains('theme-default')).toBe(true);
  });

  it('removes dark class when isDark is false', () => {
    document.documentElement.classList.add('dark');
    useThemeStore.setState({ theme: 'default', isDark: false });
    render(<ThemeProvider />);

    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  it('removes old theme class when theme changes', () => {
    const { rerender } = render(<ThemeProvider />);
    useThemeStore.setState({ theme: 'default', isDark: false });
    rerender(<ThemeProvider />);

    expect(document.documentElement.classList.contains('theme-default')).toBe(true);

    useThemeStore.setState({ theme: 'corporate', isDark: false });
    rerender(<ThemeProvider />);

    expect(document.documentElement.classList.contains('theme-corporate')).toBe(true);
    expect(document.documentElement.classList.contains('theme-default')).toBe(false);
  });

  it('handles theme and dark mode changes together', () => {
    const { rerender } = render(<ThemeProvider />);
    useThemeStore.setState({ theme: 'default', isDark: false });
    rerender(<ThemeProvider />);

    useThemeStore.setState({ theme: 'warm', isDark: true });
    rerender(<ThemeProvider />);

    expect(document.documentElement.classList.contains('theme-warm')).toBe(true);
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(document.documentElement.classList.contains('theme-default')).toBe(false);
  });

  it('renders nothing (returns null)', () => {
    const { container } = render(<ThemeProvider />);
    expect(container.firstChild).toBeNull();
  });
});

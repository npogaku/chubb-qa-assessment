import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ThemePicker } from '@/src/features/theme/components/theme-picker';
import { useThemeStore } from '@/src/features/theme/stores/use-theme-store';

describe('ThemePicker', () => {
  beforeEach(() => {
    useThemeStore.setState({ theme: 'default', isDark: false });
  });

  it('renders all three theme buttons', () => {
    render(<ThemePicker />);

    expect(screen.getByLabelText('Switch to Blue theme')).toBeInTheDocument();
    expect(screen.getByLabelText('Switch to Teal theme')).toBeInTheDocument();
    expect(screen.getByLabelText('Switch to Amber theme')).toBeInTheDocument();
  });

  it('renders dark mode toggle button', () => {
    render(<ThemePicker />);

    expect(screen.getByLabelText('Switch to dark mode')).toBeInTheDocument();
  });

  it('marks current theme button as pressed', () => {
    useThemeStore.setState({ theme: 'corporate' });
    render(<ThemePicker />);

    const corporateButton = screen.getByLabelText('Switch to Teal theme');
    expect(corporateButton).toHaveAttribute('aria-pressed', 'true');

    const defaultButton = screen.getByLabelText('Switch to Blue theme');
    expect(defaultButton).toHaveAttribute('aria-pressed', 'false');
  });

  it('changes theme when theme button is clicked', () => {
    render(<ThemePicker />);

    const warmButton = screen.getByLabelText('Switch to Amber theme');
    fireEvent.click(warmButton);

    expect(useThemeStore.getState().theme).toBe('warm');
  });

  it('toggles dark mode when dark mode button is clicked', () => {
    render(<ThemePicker />);

    const darkModeButton = screen.getByLabelText('Switch to dark mode');
    fireEvent.click(darkModeButton);

    expect(useThemeStore.getState().isDark).toBe(true);
  });

  it('updates dark mode button label when toggled', () => {
    const { rerender } = render(<ThemePicker />);

    const darkModeButton = screen.getByLabelText('Switch to dark mode');
    fireEvent.click(darkModeButton);

    rerender(<ThemePicker />);

    expect(screen.getByLabelText('Switch to light mode')).toBeInTheDocument();
  });

  it('displays checkmark on selected theme button', () => {
    useThemeStore.setState({ theme: 'warm' });
    const { container } = render(<ThemePicker />);

    const warmButton = screen.getByLabelText('Switch to Amber theme');
    const checkmark = warmButton.querySelector('span:last-child');

    expect(checkmark).toBeInTheDocument();
    expect(checkmark?.textContent).toBe('✓');
  });

  it('has group role with appropriate label', () => {
    const { container } = render(<ThemePicker />);

    const group = container.querySelector('[role="group"]');
    expect(group).toBeInTheDocument();
    expect(group).toHaveAttribute('aria-label', 'Theme and appearance settings');
  });

  it('theme buttons have focus ring styles', () => {
    render(<ThemePicker />);

    const defaultButton = screen.getByLabelText('Switch to Blue theme');
    expect(defaultButton.className).toContain('focus:ring-2');
    expect(defaultButton.className).toContain('focus:ring-primary');
  });
});

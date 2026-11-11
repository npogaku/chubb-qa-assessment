import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useForm } from 'react-hook-form';
import { PasswordField } from './password-field';

function TestWrapper({
  error,
  label,
}: {
  error?: string;
  label?: string;
}) {
  const { register } = useForm({ defaultValues: { password: '' } });
  return (
    <PasswordField
      register={register}
      fieldName="password"
      error={error}
      label={label}
    />
  );
}

describe('PasswordField', () => {
  it('renders with default label', () => {
    render(<TestWrapper />);
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
  });

  it('renders with custom label', () => {
    render(<TestWrapper label="Confirm Password" />);
    expect(screen.getByLabelText('Confirm Password')).toBeInTheDocument();
  });

  it('starts with password hidden', () => {
    render(<TestWrapper />);
    expect(screen.getByLabelText('Password')).toHaveAttribute('type', 'password');
  });

  it('toggles password visibility', async () => {
    const user = userEvent.setup();
    render(<TestWrapper />);

    const input = screen.getByLabelText('Password');
    const toggle = screen.getByRole('button', { name: /show password/i });

    await user.click(toggle);
    expect(input).toHaveAttribute('type', 'text');

    await user.click(screen.getByRole('button', { name: /hide password/i }));
    expect(input).toHaveAttribute('type', 'password');
  });

  it('displays error message', () => {
    render(<TestWrapper error="Password is required" />);
    expect(screen.getByRole('alert')).toHaveTextContent('Password is required');
  });

  it('does not display error when none provided', () => {
    render(<TestWrapper />);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

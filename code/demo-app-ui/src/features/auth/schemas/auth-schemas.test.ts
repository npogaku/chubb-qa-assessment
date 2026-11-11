import { describe, it, expect } from 'vitest';
import { loginSchema, signupSchema } from './auth-schemas';

describe('loginSchema', () => {
  it('accepts valid credentials', () => {
    const result = loginSchema.safeParse({
      email: 'user@example.com',
      password: 'password',
    });
    expect(result.success).toBe(true);
  });

  it('rejects invalid email', () => {
    const result = loginSchema.safeParse({
      email: 'not-an-email',
      password: 'password',
    });
    expect(result.success).toBe(false);
  });

  it('rejects empty password', () => {
    const result = loginSchema.safeParse({
      email: 'user@example.com',
      password: '',
    });
    expect(result.success).toBe(false);
  });

  it('rejects missing email', () => {
    const result = loginSchema.safeParse({ password: 'password' });
    expect(result.success).toBe(false);
  });
});

describe('signupSchema', () => {
  it('accepts valid signup data', () => {
    const result = signupSchema.safeParse({
      name: 'John Doe',
      email: 'john@example.com',
      password: 'Password1',
    });
    expect(result.success).toBe(true);
  });

  it('rejects password without uppercase', () => {
    const result = signupSchema.safeParse({
      name: 'John',
      email: 'john@example.com',
      password: 'password1',
    });
    expect(result.success).toBe(false);
  });

  it('rejects password without number', () => {
    const result = signupSchema.safeParse({
      name: 'John',
      email: 'john@example.com',
      password: 'Password',
    });
    expect(result.success).toBe(false);
  });

  it('rejects short password', () => {
    const result = signupSchema.safeParse({
      name: 'John',
      email: 'john@example.com',
      password: 'Pass1',
    });
    expect(result.success).toBe(false);
  });

  it('rejects empty name', () => {
    const result = signupSchema.safeParse({
      name: '',
      email: 'john@example.com',
      password: 'Password1',
    });
    expect(result.success).toBe(false);
  });

  it('rejects name exceeding 255 characters', () => {
    const result = signupSchema.safeParse({
      name: 'A'.repeat(256),
      email: 'john@example.com',
      password: 'Password1',
    });
    expect(result.success).toBe(false);
  });
});

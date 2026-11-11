import { describe, it, expect } from 'vitest';
import {
  formatDate,
  formatCurrency,
  truncateDescription,
  MAX_DESCRIPTION_LENGTH,
} from './formatting';

describe('formatDate', () => {
  it('formats a date in US short format', () => {
    const date = new Date('2026-02-15T00:00:00.000Z');
    const result = formatDate(date);
    expect(result).toContain('Feb');
    expect(result).toContain('15');
    expect(result).toContain('2026');
  });

  it('formats January 1st correctly', () => {
    const date = new Date('2026-01-01T00:00:00.000Z');
    const result = formatDate(date);
    expect(result).toContain('Jan');
    expect(result).toContain('1');
  });
});

describe('formatCurrency', () => {
  it('formats whole number as USD', () => {
    expect(formatCurrency(5000)).toBe('$5,000.00');
  });

  it('formats decimal amount', () => {
    expect(formatCurrency(5000.5)).toBe('$5,000.50');
  });

  it('formats zero', () => {
    expect(formatCurrency(0)).toBe('$0.00');
  });

  it('formats small amount', () => {
    expect(formatCurrency(0.01)).toBe('$0.01');
  });
});

describe('truncateDescription', () => {
  it('returns short description unchanged', () => {
    const short = 'Short description';
    expect(truncateDescription(short)).toBe(short);
  });

  it('truncates long description at default length', () => {
    const long = 'A'.repeat(100);
    const result = truncateDescription(long);
    expect(result).toHaveLength(MAX_DESCRIPTION_LENGTH + 3); // +3 for "..."
    expect(result.endsWith('...')).toBe(true);
  });

  it('truncates at custom length', () => {
    const text = 'Hello World';
    expect(truncateDescription(text, 5)).toBe('Hello...');
  });

  it('returns exact-length string unchanged', () => {
    const exact = 'A'.repeat(MAX_DESCRIPTION_LENGTH);
    expect(truncateDescription(exact)).toBe(exact);
  });
});

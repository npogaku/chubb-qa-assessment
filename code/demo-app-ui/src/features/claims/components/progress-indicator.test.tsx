import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { ProgressIndicator } from './progress-indicator';

describe('ProgressIndicator', () => {
  describe('Step Labels', () => {
    it('should display all three step labels', () => {
      render(<ProgressIndicator currentStep={1} />);

      expect(screen.getByText(/Step 1: Incident Details/i)).toBeInTheDocument();
      expect(
        screen.getByText(/Step 2: Supporting Details/i)
      ).toBeInTheDocument();
      expect(screen.getByText(/Step 3: Review/i)).toBeInTheDocument();
    });

    it('should highlight current step with primary color', () => {
      render(<ProgressIndicator currentStep={2} />);

      const step2 = screen.getByText(/Step 2: Supporting Details/i);
      expect(step2).toHaveClass('text-primary');
    });

    it('should show completed steps with success color', () => {
      render(<ProgressIndicator currentStep={3} />);

      const step1 = screen.getByText(/Step 1: Incident Details/i);
      const step2 = screen.getByText(/Step 2: Supporting Details/i);

      expect(step1).toHaveClass('text-success');
      expect(step2).toHaveClass('text-success');
    });

    it('should show future steps with muted color', () => {
      render(<ProgressIndicator currentStep={1} />);

      const step2 = screen.getByText(/Step 2: Supporting Details/i);
      const step3 = screen.getByText(/Step 3: Review/i);

      expect(step2).toHaveClass('text-text-muted');
      expect(step3).toHaveClass('text-text-muted');
    });
  });

  describe('Accessibility', () => {
    it('should have navigation role', () => {
      render(<ProgressIndicator currentStep={1} />);

      const nav = screen.getByRole('navigation', { name: /wizard progress/i });
      expect(nav).toBeInTheDocument();
    });

    it('should mark current step with aria-current', () => {
      render(<ProgressIndicator currentStep={2} />);

      const step2 = screen.getByText(/Step 2: Supporting Details/i);
      expect(step2).toHaveAttribute('aria-current', 'step');
    });

    it('should have progressbar role with correct attributes', () => {
      render(<ProgressIndicator currentStep={2} />);

      const progressbar = screen.getByRole('progressbar');
      expect(progressbar).toHaveAttribute('aria-valuenow', '2');
      expect(progressbar).toHaveAttribute('aria-valuemin', '1');
      expect(progressbar).toHaveAttribute('aria-valuemax', '3');
      expect(progressbar).toHaveAttribute('aria-label', 'Step 2 of 3');
    });
  });

  describe('Progress Bar Width', () => {
    it('should show 33% width for step 1', () => {
      render(<ProgressIndicator currentStep={1} />);

      const progressbar = screen.getByRole('progressbar');
      expect(progressbar).toHaveStyle({ width: '33.33333333333333%' });
    });

    it('should show 66% width for step 2', () => {
      render(<ProgressIndicator currentStep={2} />);

      const progressbar = screen.getByRole('progressbar');
      expect(progressbar).toHaveStyle({ width: '66.66666666666666%' });
    });

    it('should show 100% width for step 3', () => {
      render(<ProgressIndicator currentStep={3} />);

      const progressbar = screen.getByRole('progressbar');
      expect(progressbar).toHaveStyle({ width: '100%' });
    });
  });

  describe('Step Transitions', () => {
    it('should update when step changes from 1 to 2', () => {
      const { rerender } = render(<ProgressIndicator currentStep={1} />);

      const step1Before = screen.getByText(/Step 1: Incident Details/i);
      expect(step1Before).toHaveClass('text-primary');

      rerender(<ProgressIndicator currentStep={2} />);

      const step1After = screen.getByText(/Step 1: Incident Details/i);
      const step2After = screen.getByText(/Step 2: Supporting Details/i);

      expect(step1After).toHaveClass('text-success');
      expect(step2After).toHaveClass('text-primary');
    });

    it('should update progressbar width when step changes', () => {
      const { rerender } = render(<ProgressIndicator currentStep={1} />);

      const progressbar = screen.getByRole('progressbar');
      expect(progressbar).toHaveStyle({ width: '33.33333333333333%' });

      rerender(<ProgressIndicator currentStep={3} />);
      expect(progressbar).toHaveStyle({ width: '100%' });
    });
  });
});

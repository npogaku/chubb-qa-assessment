import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { WizardStep3 } from './wizard-step-3';
import { useClaimWizardStore } from '../stores/use-claim-wizard-store';
import toast from 'react-hot-toast';

vi.mock('../stores/use-claim-wizard-store', () => ({
  useClaimWizardStore: vi.fn(),
}));

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
}));

vi.mock('react-hot-toast', () => ({
  default: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('WizardStep3', () => {
  const mockSubmitClaim = vi.fn();
  const mockPreviousStep = vi.fn();
  const mockResetWizard = vi.fn();

  const sampleFormData = {
    incidentDate: new Date('2026-02-15'),
    incidentLocation: '123 Main Street, Springfield',
    claimAmount: 5000.5,
    description: 'Test description of the incident',
  };

  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(useClaimWizardStore).mockReturnValue({
      formData: sampleFormData,
      fieldErrors: {},
      isSubmitting: false,
      error: null,
      submitClaim: mockSubmitClaim,
      previousStep: mockPreviousStep,
      resetWizard: mockResetWizard,
      step: 3,
      updateFormData: vi.fn(),
      nextStep: vi.fn(),
      setStep: vi.fn(),
    });

    // Mock getState for wizard step 3's error retrieval
    vi.mocked(useClaimWizardStore).getState = vi.fn().mockReturnValue({
      error: null,
    });
  });

  describe('Review Display', () => {
    it('should render heading "Review Your Claim"', () => {
      render(<WizardStep3 />);

      expect(
        screen.getByRole('heading', { name: /Review Your Claim/i })
      ).toBeInTheDocument();
    });

    it('should display formatted incident date', () => {
      render(<WizardStep3 />);

      expect(screen.getByText('February 15, 2026')).toBeInTheDocument();
    });

    it('should display incident location', () => {
      render(<WizardStep3 />);

      expect(
        screen.getByText('123 Main Street, Springfield')
      ).toBeInTheDocument();
    });

    it('should display formatted claim amount as currency', () => {
      render(<WizardStep3 />);

      expect(screen.getByText('$5,000.50')).toBeInTheDocument();
    });

    it('should display description', () => {
      render(<WizardStep3 />);

      expect(
        screen.getByText('Test description of the incident')
      ).toBeInTheDocument();
    });

    it('should show N/A when form data is missing', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: {},
        fieldErrors: {},
        isSubmitting: false,
        error: null,
        submitClaim: mockSubmitClaim,
        previousStep: mockPreviousStep,
        resetWizard: mockResetWizard,
        step: 3,
        updateFormData: vi.fn(),
        nextStep: vi.fn(),
        setStep: vi.fn(),
      });

      render(<WizardStep3 />);

      const naElements = screen.getAllByText('N/A');
      expect(naElements.length).toBeGreaterThan(0);
    });
  });

  describe('Navigation Buttons', () => {
    it('should display Back and Submit Claim buttons', () => {
      render(<WizardStep3 />);

      expect(
        screen.getByRole('button', { name: /Back/i })
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: /Submit Claim/i })
      ).toBeInTheDocument();
    });

    it('should call previousStep when Back is clicked', async () => {
      const user = userEvent.setup();
      render(<WizardStep3 />);

      const backButton = screen.getByRole('button', { name: /Back/i });
      await user.click(backButton);

      expect(mockPreviousStep).toHaveBeenCalled();
    });

    it('should disable Back button when submitting', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: sampleFormData,
        fieldErrors: {},
        isSubmitting: true,
        error: null,
        submitClaim: mockSubmitClaim,
        previousStep: mockPreviousStep,
        resetWizard: mockResetWizard,
        step: 3,
        updateFormData: vi.fn(),
        nextStep: vi.fn(),
        setStep: vi.fn(),
      });

      render(<WizardStep3 />);

      const backButton = screen.getByRole('button', { name: /Back/i });
      expect(backButton).toBeDisabled();
    });

    it('should disable Submit button when submitting', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: sampleFormData,
        fieldErrors: {},
        isSubmitting: true,
        error: null,
        submitClaim: mockSubmitClaim,
        previousStep: mockPreviousStep,
        resetWizard: mockResetWizard,
        step: 3,
        updateFormData: vi.fn(),
        nextStep: vi.fn(),
        setStep: vi.fn(),
      });

      render(<WizardStep3 />);

      const submitButton = screen.getByRole('button', {
        name: /Submitting.../i,
      });
      expect(submitButton).toBeDisabled();
    });
  });

  describe('Loading State', () => {
    it('should show loading spinner when submitting', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: sampleFormData,
        fieldErrors: {},
        isSubmitting: true,
        error: null,
        submitClaim: mockSubmitClaim,
        previousStep: mockPreviousStep,
        resetWizard: mockResetWizard,
        step: 3,
        updateFormData: vi.fn(),
        nextStep: vi.fn(),
        setStep: vi.fn(),
      });

      render(<WizardStep3 />);

      expect(
        screen.getByRole('button', { name: /Submitting.../i })
      ).toBeInTheDocument();
    });

    it('should show "Submit Claim" text when not submitting', () => {
      render(<WizardStep3 />);

      expect(
        screen.getByRole('button', { name: /^Submit Claim$/i })
      ).toBeInTheDocument();
    });
  });

  describe('Submit Success', () => {
    it('should show success toast on successful submission', async () => {
      const user = userEvent.setup();
      const mockResponse = {
        claimId: 'claim-123',
        userId: 'user-456',
        incidentDate: '2026-02-15',
        incidentLocation: '123 Main Street',
        description: 'Test',
        claimAmount: 5000,
        status: 'SUBMITTED' as const,
        createdAt: '2026-02-17T10:00:00Z',
        updatedAt: '2026-02-17T10:00:00Z',
      };

      mockSubmitClaim.mockResolvedValue(mockResponse);

      render(<WizardStep3 />);

      const submitButton = screen.getByRole('button', {
        name: /Submit Claim/i,
      });
      await user.click(submitButton);

      await waitFor(() => {
        expect(toast.success).toHaveBeenCalledWith(
          'Claim submitted successfully! Claim ID: claim-123'
        );
      });
    });

    it('should reset wizard on successful submission', async () => {
      const user = userEvent.setup();
      const mockResponse = {
        claimId: 'claim-123',
        userId: 'user-456',
        incidentDate: '2026-02-15',
        incidentLocation: '123 Main Street',
        description: 'Test',
        claimAmount: 5000,
        status: 'SUBMITTED' as const,
        createdAt: '2026-02-17T10:00:00Z',
        updatedAt: '2026-02-17T10:00:00Z',
      };

      mockSubmitClaim.mockResolvedValue(mockResponse);

      render(<WizardStep3 />);

      const submitButton = screen.getByRole('button', {
        name: /Submit Claim/i,
      });
      await user.click(submitButton);

      await waitFor(() => {
        expect(mockResetWizard).toHaveBeenCalled();
      });
    });
  });

  describe('Submit Failure', () => {
    it('should show error toast on submission failure', async () => {
      const user = userEvent.setup();

      mockSubmitClaim.mockResolvedValue(null);
      vi.mocked(useClaimWizardStore).getState = vi.fn().mockReturnValue({
        error: 'Failed to submit claim. Please try again.',
      });

      render(<WizardStep3 />);

      const submitButton = screen.getByRole('button', {
        name: /Submit Claim/i,
      });
      await user.click(submitButton);

      await waitFor(() => {
        expect(toast.error).toHaveBeenCalledWith(
          'Failed to submit claim. Please try again.'
        );
      });
    });

    it('should display error message from store', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: sampleFormData,
        fieldErrors: {},
        isSubmitting: false,
        error: 'Validation error: Invalid claim data',
        submitClaim: mockSubmitClaim,
        previousStep: mockPreviousStep,
        resetWizard: mockResetWizard,
        step: 3,
        updateFormData: vi.fn(),
        nextStep: vi.fn(),
        setStep: vi.fn(),
      });

      render(<WizardStep3 />);

      expect(
        screen.getByText('Validation error: Invalid claim data')
      ).toBeInTheDocument();
    });

    it('should mark error message with role="alert"', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: sampleFormData,
        fieldErrors: {},
        isSubmitting: false,
        error: 'Server error',
        submitClaim: mockSubmitClaim,
        previousStep: mockPreviousStep,
        resetWizard: mockResetWizard,
        step: 3,
        updateFormData: vi.fn(),
        nextStep: vi.fn(),
        setStep: vi.fn(),
      });

      render(<WizardStep3 />);

      const errorMessage = screen.getByText('Server error');
      expect(errorMessage).toHaveAttribute('role', 'alert');
    });
  });

  describe('Currency Formatting', () => {
    it('should format large amounts with commas', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: {
          ...sampleFormData,
          claimAmount: 123456.78,
        },
        fieldErrors: {},
        isSubmitting: false,
        error: null,
        submitClaim: mockSubmitClaim,
        previousStep: mockPreviousStep,
        resetWizard: mockResetWizard,
        step: 3,
        updateFormData: vi.fn(),
        nextStep: vi.fn(),
        setStep: vi.fn(),
      });

      render(<WizardStep3 />);

      expect(screen.getByText('$123,456.78')).toBeInTheDocument();
    });

    it('should format amounts with two decimal places', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: {
          ...sampleFormData,
          claimAmount: 100,
        },
        fieldErrors: {},
        isSubmitting: false,
        error: null,
        submitClaim: mockSubmitClaim,
        previousStep: mockPreviousStep,
        resetWizard: mockResetWizard,
        step: 3,
        updateFormData: vi.fn(),
        nextStep: vi.fn(),
        setStep: vi.fn(),
      });

      render(<WizardStep3 />);

      expect(screen.getByText('$100.00')).toBeInTheDocument();
    });
  });

  describe('Accessibility', () => {
    it('should have larger button for primary CTA', () => {
      render(<WizardStep3 />);

      const submitButton = screen.getByRole('button', {
        name: /Submit Claim/i,
      });

      expect(submitButton).toHaveClass('px-8', 'py-3', 'text-lg');
    });

    it('should hide spinner SVG from screen readers', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: sampleFormData,
        fieldErrors: {},
        isSubmitting: true,
        error: null,
        submitClaim: mockSubmitClaim,
        previousStep: mockPreviousStep,
        resetWizard: mockResetWizard,
        step: 3,
        updateFormData: vi.fn(),
        nextStep: vi.fn(),
        setStep: vi.fn(),
      });

      const { container } = render(<WizardStep3 />);

      const spinner = container.querySelector('svg');
      expect(spinner).toHaveAttribute('aria-hidden', 'true');
    });
  });
});

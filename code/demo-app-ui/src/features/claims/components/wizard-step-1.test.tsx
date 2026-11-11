import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { WizardStep1 } from './wizard-step-1';
import { useClaimWizardStore } from '../stores/use-claim-wizard-store';

vi.mock('../stores/use-claim-wizard-store', () => ({
  useClaimWizardStore: vi.fn(),
}));

describe('WizardStep1', () => {
  const mockUpdateFormData = vi.fn();
  const mockNextStep = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(useClaimWizardStore).mockReturnValue({
      formData: {},
      fieldErrors: {},
      updateFormData: mockUpdateFormData,
      nextStep: mockNextStep,
      step: 1,
      isSubmitting: false,
      error: null,
      setStep: vi.fn(),
      previousStep: vi.fn(),
      submitClaim: vi.fn(),
      resetWizard: vi.fn(),
    });
  });

  describe('Form Fields Rendering', () => {
    it('should render incident date field', () => {
      render(<WizardStep1 />);

      expect(
        screen.getByLabelText(/When did the incident occur?/i)
      ).toBeInTheDocument();
    });

    it('should render incident location field', () => {
      render(<WizardStep1 />);

      expect(
        screen.getByLabelText(/Where did the incident occur?/i)
      ).toBeInTheDocument();
    });

    it('should render claim amount field', () => {
      render(<WizardStep1 />);

      expect(screen.getByLabelText(/Claim Amount/i)).toBeInTheDocument();
    });

    it('should display Next button', () => {
      render(<WizardStep1 />);

      expect(
        screen.getByRole('button', { name: /Next/i })
      ).toBeInTheDocument();
    });
  });

  describe('Field Validation', () => {
    it('should disable Next button when form is invalid', () => {
      render(<WizardStep1 />);

      const nextButton = screen.getByRole('button', { name: /Next/i });
      expect(nextButton).toBeDisabled();
    });

    it('should show error for future incident date', async () => {
      const user = userEvent.setup();
      render(<WizardStep1 />);

      const dateInput = screen.getByLabelText(/When did the incident occur?/i);
      const futureDate = new Date();
      futureDate.setDate(futureDate.getDate() + 1);
      const futureDateString = futureDate.toISOString().split('T')[0];

      await user.type(dateInput, futureDateString);
      await user.tab(); // Trigger validation

      await waitFor(() => {
        expect(
          screen.getByText(/Incident date cannot be in the future/i)
        ).toBeInTheDocument();
      });
    });

    it('should show error for location shorter than 5 characters', async () => {
      const user = userEvent.setup();
      render(<WizardStep1 />);

      const locationInput = screen.getByLabelText(
        /Where did the incident occur?/i
      );

      await user.type(locationInput, 'ABC');
      await user.tab();

      await waitFor(() => {
        expect(
          screen.getByText(/Location must be at least 5 characters/i)
        ).toBeInTheDocument();
      });
    });

    it('should show error for claim amount below minimum', async () => {
      const user = userEvent.setup();
      render(<WizardStep1 />);

      const amountInput = screen.getByLabelText(/Claim Amount/i);

      await user.type(amountInput, '0');
      await user.tab();

      await waitFor(() => {
        expect(
          screen.getByText(/Claim amount must be at least \$0.01/i)
        ).toBeInTheDocument();
      });
    });

    it('should show error for claim amount above maximum', async () => {
      const user = userEvent.setup();
      render(<WizardStep1 />);

      const amountInput = screen.getByLabelText(/Claim Amount/i);

      await user.type(amountInput, '2000000');
      await user.tab();

      await waitFor(() => {
        expect(
          screen.getByText(/Claim amount cannot exceed \$1,000,000/i)
        ).toBeInTheDocument();
      });
    });
  });

  describe('Location Character Counter', () => {
    it('should display character counter for location field', () => {
      render(<WizardStep1 />);

      expect(screen.getByText('0/200')).toBeInTheDocument();
    });

    it('should update character counter when typing', async () => {
      const user = userEvent.setup();
      render(<WizardStep1 />);

      const locationInput = screen.getByLabelText(
        /Where did the incident occur?/i
      );

      await user.type(locationInput, 'Test location');

      await waitFor(() => {
        expect(screen.getByText('13/200')).toBeInTheDocument();
      });
    });
  });

  describe('Backend Field Errors', () => {
    it('should display backend error for incident date', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: {},
        fieldErrors: {
          incidentDate: 'Date is outside coverage period',
        },
        updateFormData: mockUpdateFormData,
        nextStep: mockNextStep,
        step: 1,
        isSubmitting: false,
        error: null,
        setStep: vi.fn(),
        previousStep: vi.fn(),
        submitClaim: vi.fn(),
        resetWizard: vi.fn(),
      });

      render(<WizardStep1 />);

      expect(
        screen.getByText('Date is outside coverage period')
      ).toBeInTheDocument();
    });

    it('should display backend error for location', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: {},
        fieldErrors: {
          incidentLocation: 'Location not in covered area',
        },
        updateFormData: mockUpdateFormData,
        nextStep: mockNextStep,
        step: 1,
        isSubmitting: false,
        error: null,
        setStep: vi.fn(),
        previousStep: vi.fn(),
        submitClaim: vi.fn(),
        resetWizard: vi.fn(),
      });

      render(<WizardStep1 />);

      expect(
        screen.getByText('Location not in covered area')
      ).toBeInTheDocument();
    });

    it('should display backend error for claim amount', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: {},
        fieldErrors: {
          claimAmount: 'Amount exceeds policy limit',
        },
        updateFormData: mockUpdateFormData,
        nextStep: mockNextStep,
        step: 1,
        isSubmitting: false,
        error: null,
        setStep: vi.fn(),
        previousStep: vi.fn(),
        submitClaim: vi.fn(),
        resetWizard: vi.fn(),
      });

      render(<WizardStep1 />);

      expect(screen.getByText('Amount exceeds policy limit')).toBeInTheDocument();
    });
  });

  describe('Form Submission', () => {
    it('should call updateFormData and nextStep with valid data', async () => {
      const user = userEvent.setup();
      render(<WizardStep1 />);

      const dateInput = screen.getByLabelText(/When did the incident occur?/i);
      const locationInput = screen.getByLabelText(
        /Where did the incident occur?/i
      );
      const amountInput = screen.getByLabelText(/Claim Amount/i);

      await user.type(dateInput, '2026-02-15');
      await user.type(locationInput, '123 Main Street, Springfield');
      await user.type(amountInput, '5000');

      await waitFor(() => {
        const nextButton = screen.getByRole('button', { name: /Next/i });
        expect(nextButton).toBeEnabled();
      });

      const nextButton = screen.getByRole('button', { name: /Next/i });
      await user.click(nextButton);

      await waitFor(() => {
        expect(mockUpdateFormData).toHaveBeenCalledWith(
          expect.objectContaining({
            incidentDate: expect.any(Date),
            incidentLocation: '123 Main Street, Springfield',
            claimAmount: 5000,
          })
        );
        expect(mockNextStep).toHaveBeenCalled();
      });
    });
  });

  describe('Accessibility', () => {
    it('should have proper aria-invalid attributes when errors exist', async () => {
      const user = userEvent.setup();
      render(<WizardStep1 />);

      const locationInput = screen.getByLabelText(
        /Where did the incident occur?/i
      );

      await user.type(locationInput, 'ABC');
      await user.tab();

      await waitFor(() => {
        expect(locationInput).toHaveAttribute('aria-invalid', 'true');
      });
    });

    it('should have proper aria-describedby linking errors', async () => {
      const user = userEvent.setup();
      render(<WizardStep1 />);

      const locationInput = screen.getByLabelText(
        /Where did the incident occur?/i
      );

      await user.type(locationInput, 'ABC');
      await user.tab();

      await waitFor(() => {
        expect(locationInput).toHaveAttribute(
          'aria-describedby',
          expect.stringContaining('incidentLocation-error')
        );
      });
    });

    it('should mark error messages with role="alert"', async () => {
      const user = userEvent.setup();
      render(<WizardStep1 />);

      const locationInput = screen.getByLabelText(
        /Where did the incident occur?/i
      );

      await user.type(locationInput, 'ABC');
      await user.tab();

      await waitFor(() => {
        const errorMessage = screen.getByText(
          /Location must be at least 5 characters/i
        );
        expect(errorMessage).toHaveAttribute('role', 'alert');
      });
    });
  });

  describe('Prepopulated Data', () => {
    it('should use pre-filled form data from store as defaults', () => {
      const testDate = new Date('2026-02-15');

      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: {
          incidentDate: testDate,
          incidentLocation: 'Saved Location',
          claimAmount: 1234.56,
        },
        fieldErrors: {},
        updateFormData: mockUpdateFormData,
        nextStep: mockNextStep,
        step: 1,
        isSubmitting: false,
        error: null,
        setStep: vi.fn(),
        previousStep: vi.fn(),
        submitClaim: vi.fn(),
        resetWizard: vi.fn(),
      });

      render(<WizardStep1 />);

      // Verify that form uses store data as defaultValues
      // Note: react-hook-form with defaultValues may not immediately populate controlled inputs
      // The key test is that the form will submit the correct values
      const locationInput = screen.getByLabelText(
        /Where did the incident occur?/i
      ) as HTMLInputElement;

      expect(locationInput.value).toBe('Saved Location');
    });
  });
});

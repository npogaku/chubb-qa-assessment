import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { WizardStep2 } from './wizard-step-2';
import { useClaimWizardStore } from '../stores/use-claim-wizard-store';

vi.mock('../stores/use-claim-wizard-store', () => ({
  useClaimWizardStore: vi.fn(),
}));

describe('WizardStep2', () => {
  const mockUpdateFormData = vi.fn();
  const mockNextStep = vi.fn();
  const mockPreviousStep = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(useClaimWizardStore).mockReturnValue({
      formData: {},
      fieldErrors: {},
      updateFormData: mockUpdateFormData,
      nextStep: mockNextStep,
      previousStep: mockPreviousStep,
      step: 2,
      isSubmitting: false,
      error: null,
      setStep: vi.fn(),
      submitClaim: vi.fn(),
      resetWizard: vi.fn(),
    });
  });

  describe('Form Fields Rendering', () => {
    it('should render heading "Supporting Details"', () => {
      render(<WizardStep2 />);

      expect(
        screen.getByRole('heading', { name: /Supporting Details/i })
      ).toBeInTheDocument();
    });

    it('should render description textarea', () => {
      render(<WizardStep2 />);

      expect(
        screen.getByLabelText(/Describe what happened/i)
      ).toBeInTheDocument();
    });

    it('should display helpful prompt text', () => {
      render(<WizardStep2 />);

      expect(
        screen.getByText(
          /Be as detailed as possible. Include what happened, when, and any relevant circumstances./i
        )
      ).toBeInTheDocument();
    });

    it('should display Back and Next buttons', () => {
      render(<WizardStep2 />);

      expect(
        screen.getByRole('button', { name: /Back/i })
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: /Next/i })
      ).toBeInTheDocument();
    });
  });

  describe('Character Counter', () => {
    it('should display initial character counter 0/1000', () => {
      render(<WizardStep2 />);

      expect(screen.getByText('0/1000')).toBeInTheDocument();
    });

    it('should update character counter when typing', async () => {
      const user = userEvent.setup();
      render(<WizardStep2 />);

      const textarea = screen.getByLabelText(/Describe what happened/i);

      await user.type(textarea, 'Test description text');

      await waitFor(() => {
        expect(screen.getByText('21/1000')).toBeInTheDocument();
      });
    });

    it('should show error color when character count is below minimum', async () => {
      const user = userEvent.setup();
      render(<WizardStep2 />);

      const textarea = screen.getByLabelText(/Describe what happened/i);

      await user.type(textarea, 'Short');

      await waitFor(() => {
        const counter = screen.getByText('5/1000');
        expect(counter).toHaveClass('text-error');
      });
    });

    it('should show primary color when approaching maximum', async () => {
      const user = userEvent.setup();
      render(<WizardStep2 />);

      const textarea = screen.getByLabelText(/Describe what happened/i);

      const longText = 'a'.repeat(950);
      await user.type(textarea, longText);

      await waitFor(() => {
        const counter = screen.getByText('950/1000');
        expect(counter).toHaveClass('text-primary');
      });
    });

    it('should show muted color when character count is valid', async () => {
      const user = userEvent.setup();
      render(<WizardStep2 />);

      const textarea = screen.getByLabelText(/Describe what happened/i);

      await user.type(textarea, 'This is a valid description with sufficient length');

      await waitFor(() => {
        const counter = screen.getByText('50/1000');
        expect(counter).toHaveClass('text-text-muted');
      });
    });
  });

  describe('Field Validation', () => {
    it('should disable Next button when description is too short', () => {
      render(<WizardStep2 />);

      const nextButton = screen.getByRole('button', { name: /Next/i });
      expect(nextButton).toBeDisabled();
    });

    it('should show error for description shorter than 10 characters', async () => {
      const user = userEvent.setup();
      render(<WizardStep2 />);

      const textarea = screen.getByLabelText(/Describe what happened/i);

      await user.type(textarea, 'Short');
      await user.tab();

      await waitFor(() => {
        expect(
          screen.getByText(/Description must be at least 10 characters/i)
        ).toBeInTheDocument();
      });
    });

    it('should prevent description longer than 1000 characters via maxLength attribute', () => {
      render(<WizardStep2 />);

      const textarea = screen.getByLabelText(/Describe what happened/i);

      // Verify textarea has maxLength attribute to prevent typing beyond limit
      expect(textarea).toHaveAttribute('maxLength', '1000');
    });

    it('should enable Next button when description is valid', async () => {
      const user = userEvent.setup();
      render(<WizardStep2 />);

      const textarea = screen.getByLabelText(/Describe what happened/i);

      await user.type(
        textarea,
        'This is a valid description with more than 10 characters'
      );

      await waitFor(() => {
        const nextButton = screen.getByRole('button', { name: /Next/i });
        expect(nextButton).toBeEnabled();
      });
    });
  });

  describe('Backend Field Errors', () => {
    it('should display backend error for description', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: {},
        fieldErrors: {
          description: 'Description contains inappropriate content',
        },
        updateFormData: mockUpdateFormData,
        nextStep: mockNextStep,
        previousStep: mockPreviousStep,
        step: 2,
        isSubmitting: false,
        error: null,
        setStep: vi.fn(),
        submitClaim: vi.fn(),
        resetWizard: vi.fn(),
      });

      render(<WizardStep2 />);

      expect(
        screen.getByText('Description contains inappropriate content')
      ).toBeInTheDocument();
    });

    it('should mark textarea as invalid when backend error exists', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: {},
        fieldErrors: {
          description: 'Server validation error',
        },
        updateFormData: mockUpdateFormData,
        nextStep: mockNextStep,
        previousStep: mockPreviousStep,
        step: 2,
        isSubmitting: false,
        error: null,
        setStep: vi.fn(),
        submitClaim: vi.fn(),
        resetWizard: vi.fn(),
      });

      render(<WizardStep2 />);

      const textarea = screen.getByLabelText(/Describe what happened/i);
      expect(textarea).toHaveAttribute('aria-invalid', 'true');
    });
  });

  describe('Navigation', () => {
    it('should call previousStep when Back button is clicked', async () => {
      const user = userEvent.setup();
      render(<WizardStep2 />);

      const backButton = screen.getByRole('button', { name: /Back/i });
      await user.click(backButton);

      expect(mockPreviousStep).toHaveBeenCalled();
    });

    it('should call updateFormData and nextStep when form is valid and Next is clicked', async () => {
      const user = userEvent.setup();
      render(<WizardStep2 />);

      const textarea = screen.getByLabelText(/Describe what happened/i);

      await user.type(
        textarea,
        'This is a valid and detailed description of the incident'
      );

      await waitFor(() => {
        const nextButton = screen.getByRole('button', { name: /Next/i });
        expect(nextButton).toBeEnabled();
      });

      const nextButton = screen.getByRole('button', { name: /Next/i });
      await user.click(nextButton);

      await waitFor(() => {
        expect(mockUpdateFormData).toHaveBeenCalledWith({
          description: 'This is a valid and detailed description of the incident',
        });
        expect(mockNextStep).toHaveBeenCalled();
      });
    });
  });

  describe('Accessibility', () => {
    it('should have proper aria-invalid when error exists', async () => {
      const user = userEvent.setup();
      render(<WizardStep2 />);

      const textarea = screen.getByLabelText(/Describe what happened/i);

      await user.type(textarea, 'Short');
      await user.tab();

      await waitFor(() => {
        expect(textarea).toHaveAttribute('aria-invalid', 'true');
      });
    });

    it('should have aria-describedby linking to error and counter', async () => {
      const user = userEvent.setup();
      render(<WizardStep2 />);

      const textarea = screen.getByLabelText(/Describe what happened/i);

      await user.type(textarea, 'Short');
      await user.tab();

      await waitFor(() => {
        const ariaDescribedBy = textarea.getAttribute('aria-describedby');
        expect(ariaDescribedBy).toContain('description-error');
        expect(ariaDescribedBy).toContain('description-counter');
      });
    });

    it('should mark error messages with role="alert"', async () => {
      const user = userEvent.setup();
      render(<WizardStep2 />);

      const textarea = screen.getByLabelText(/Describe what happened/i);

      await user.type(textarea, 'Short');
      await user.tab();

      await waitFor(() => {
        const errorMessage = screen.getByText(
          /Description must be at least 10 characters/i
        );
        expect(errorMessage).toHaveAttribute('role', 'alert');
      });
    });
  });

  describe('Prepopulated Data', () => {
    it('should display pre-filled description from store', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: {
          description: 'Previously saved description text',
        },
        fieldErrors: {},
        updateFormData: mockUpdateFormData,
        nextStep: mockNextStep,
        previousStep: mockPreviousStep,
        step: 2,
        isSubmitting: false,
        error: null,
        setStep: vi.fn(),
        submitClaim: vi.fn(),
        resetWizard: vi.fn(),
      });

      render(<WizardStep2 />);

      const textarea = screen.getByLabelText(
        /Describe what happened/i
      ) as HTMLTextAreaElement;
      expect(textarea.value).toBe('Previously saved description text');
    });

    it('should display correct character count for pre-filled data', () => {
      vi.mocked(useClaimWizardStore).mockReturnValue({
        formData: {
          description: 'Previously saved description',
        },
        fieldErrors: {},
        updateFormData: mockUpdateFormData,
        nextStep: mockNextStep,
        previousStep: mockPreviousStep,
        step: 2,
        isSubmitting: false,
        error: null,
        setStep: vi.fn(),
        submitClaim: vi.fn(),
        resetWizard: vi.fn(),
      });

      render(<WizardStep2 />);

      expect(screen.getByText('28/1000')).toBeInTheDocument();
    });
  });
});

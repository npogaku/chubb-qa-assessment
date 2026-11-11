package com.example.demo.domain.claim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClaimStatusTest {

    // SUBMITTED transitions
    @Test
    void should_AllowTransition_When_SubmittedToUnderReview() {
        assertTrue(ClaimStatus.SUBMITTED.canTransitionTo(ClaimStatus.UNDER_REVIEW));
    }

    @Test
    void should_AllowTransition_When_SubmittedToRejected() {
        assertTrue(ClaimStatus.SUBMITTED.canTransitionTo(ClaimStatus.REJECTED));
    }

    @Test
    void should_BlockTransition_When_SubmittedToApproved() {
        assertFalse(ClaimStatus.SUBMITTED.canTransitionTo(ClaimStatus.APPROVED));
    }

    @Test
    void should_BlockTransition_When_SubmittedToClosed() {
        assertFalse(ClaimStatus.SUBMITTED.canTransitionTo(ClaimStatus.CLOSED));
    }

    // UNDER_REVIEW transitions
    @Test
    void should_AllowTransition_When_UnderReviewToApproved() {
        assertTrue(ClaimStatus.UNDER_REVIEW.canTransitionTo(ClaimStatus.APPROVED));
    }

    @Test
    void should_AllowTransition_When_UnderReviewToRejected() {
        assertTrue(ClaimStatus.UNDER_REVIEW.canTransitionTo(ClaimStatus.REJECTED));
    }

    @Test
    void should_AllowTransition_When_UnderReviewToSubmitted() {
        assertTrue(ClaimStatus.UNDER_REVIEW.canTransitionTo(ClaimStatus.SUBMITTED));
    }

    @Test
    void should_BlockTransition_When_UnderReviewToClosed() {
        assertFalse(ClaimStatus.UNDER_REVIEW.canTransitionTo(ClaimStatus.CLOSED));
    }

    // APPROVED transitions
    @Test
    void should_AllowTransition_When_ApprovedToClosed() {
        assertTrue(ClaimStatus.APPROVED.canTransitionTo(ClaimStatus.CLOSED));
    }

    @Test
    void should_BlockTransition_When_ApprovedToSubmitted() {
        assertFalse(ClaimStatus.APPROVED.canTransitionTo(ClaimStatus.SUBMITTED));
    }

    // REJECTED transitions
    @Test
    void should_AllowTransition_When_RejectedToClosed() {
        assertTrue(ClaimStatus.REJECTED.canTransitionTo(ClaimStatus.CLOSED));
    }

    @Test
    void should_BlockTransition_When_RejectedToSubmitted() {
        assertFalse(ClaimStatus.REJECTED.canTransitionTo(ClaimStatus.SUBMITTED));
    }

    // CLOSED transitions (terminal state - no transitions allowed)
    @Test
    void should_BlockAllTransitions_When_ClosedIsTerminalState() {
        assertFalse(ClaimStatus.CLOSED.canTransitionTo(ClaimStatus.SUBMITTED),
            "CLOSED is terminal - cannot transition to SUBMITTED");
        assertFalse(ClaimStatus.CLOSED.canTransitionTo(ClaimStatus.UNDER_REVIEW),
            "CLOSED is terminal - cannot transition to UNDER_REVIEW");
        assertFalse(ClaimStatus.CLOSED.canTransitionTo(ClaimStatus.APPROVED),
            "CLOSED is terminal - cannot transition to APPROVED");
        assertFalse(ClaimStatus.CLOSED.canTransitionTo(ClaimStatus.REJECTED),
            "CLOSED is terminal - cannot transition to REJECTED");
        assertFalse(ClaimStatus.CLOSED.canTransitionTo(ClaimStatus.CLOSED),
            "CLOSED is terminal - cannot transition to itself");
    }
}

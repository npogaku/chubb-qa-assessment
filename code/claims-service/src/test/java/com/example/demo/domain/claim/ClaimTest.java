package com.example.demo.domain.claim;

import com.example.demo.domain.DomainEvent;
import com.example.demo.domain.claim.events.ClaimStatusChanged;
import com.example.demo.domain.claim.events.ClaimSubmitted;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClaimTest {

    private static final UUID CLAIM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate VALID_DATE = LocalDate.now().minusDays(1);
    private static final String VALID_LOCATION = "123 Main Street, City";
    private static final String VALID_DESCRIPTION = "This is a valid claim description with enough characters";
    private static final BigDecimal VALID_AMOUNT = new BigDecimal("5000.00");

    // TC-016
    @Test
    void should_ThrowInvalidClaimException_When_IncidentDateInFuture() {
        LocalDate futureDate = LocalDate.now().plusDays(1);

        assertThrows(InvalidClaimException.class, () ->
            new Claim(CLAIM_ID, USER_ID, futureDate, VALID_LOCATION, VALID_DESCRIPTION, VALID_AMOUNT)
        );
    }

    // TC-040
    @Test
    void should_ThrowInvalidClaimException_When_ClaimAmountNegative() {
        assertThrows(InvalidClaimException.class, () ->
            new Claim(CLAIM_ID, USER_ID, VALID_DATE, VALID_LOCATION, VALID_DESCRIPTION, new BigDecimal("-100"))
        );
    }

    // TC-041
    @Test
    void should_ThrowInvalidClaimException_When_DescriptionTooShort() {
        assertThrows(InvalidClaimException.class, () ->
            new Claim(CLAIM_ID, USER_ID, VALID_DATE, VALID_LOCATION, "Short", VALID_AMOUNT)
        );
    }

    @Test
    void should_ThrowInvalidClaimException_When_ClaimAmountExceedsMaximum() {
        BigDecimal overMaxAmount = Claim.MAX_CLAIM_AMOUNT.add(BigDecimal.ONE);
        assertThrows(InvalidClaimException.class, () ->
            new Claim(CLAIM_ID, USER_ID, VALID_DATE, VALID_LOCATION, VALID_DESCRIPTION, overMaxAmount)
        );
    }

    @Test
    void should_ThrowInvalidClaimException_When_IncidentLocationTooShort() {
        assertThrows(InvalidClaimException.class, () ->
            new Claim(CLAIM_ID, USER_ID, VALID_DATE, "AB", VALID_DESCRIPTION, VALID_AMOUNT)
        );
    }

    @Test
    void should_CreateClaim_When_AllFieldsValid() {
        Claim claim = new Claim(CLAIM_ID, USER_ID, VALID_DATE, VALID_LOCATION, VALID_DESCRIPTION, VALID_AMOUNT);

        assertEquals(CLAIM_ID, claim.getClaimId());
        assertEquals(USER_ID, claim.getUserId());
        assertEquals(VALID_DATE, claim.getIncidentDate());
        assertEquals(VALID_LOCATION, claim.getIncidentLocation());
        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());
        assertNotNull(claim.getCreatedAt());
        assertNotNull(claim.getUpdatedAt());
    }

    @Test
    void should_TrimWhitespace_When_CreatingClaim() {
        String paddedDescription = "  This is a padded description with spaces  ";
        String paddedLocation = "  123 Main St  ";

        Claim claim = new Claim(CLAIM_ID, USER_ID, VALID_DATE, paddedLocation, paddedDescription, VALID_AMOUNT);

        assertEquals("This is a padded description with spaces", claim.getDescription());
        assertEquals("123 Main St", claim.getIncidentLocation());
    }

    @Test
    void should_RaiseClaimSubmittedEvent_When_ClaimCreated() {
        Claim claim = new Claim(CLAIM_ID, USER_ID, VALID_DATE, VALID_LOCATION, VALID_DESCRIPTION, VALID_AMOUNT);

        List<DomainEvent> events = claim.getDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(ClaimSubmitted.class, events.getFirst());

        ClaimSubmitted event = (ClaimSubmitted) events.getFirst();
        assertEquals(CLAIM_ID, event.claimId());
        assertEquals(USER_ID, event.userId());
    }

    // TC-017
    @Test
    void should_UpdateStatus_When_TransitionValid() {
        Claim claim = new Claim(CLAIM_ID, USER_ID, VALID_DATE, VALID_LOCATION, VALID_DESCRIPTION, VALID_AMOUNT);
        claim.clearEvents();

        claim.updateStatus(ClaimStatus.UNDER_REVIEW);

        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());
    }

    @Test
    void should_ThrowInvalidStatusTransitionException_When_TransitionInvalid() {
        Claim claim = new Claim(CLAIM_ID, USER_ID, VALID_DATE, VALID_LOCATION, VALID_DESCRIPTION, VALID_AMOUNT);

        assertThrows(InvalidStatusTransitionException.class, () ->
            claim.updateStatus(ClaimStatus.APPROVED)
        );
    }

    @Test
    void should_RaiseClaimStatusChangedEvent_When_StatusUpdated() {
        Claim claim = new Claim(CLAIM_ID, USER_ID, VALID_DATE, VALID_LOCATION, VALID_DESCRIPTION, VALID_AMOUNT);
        claim.clearEvents();

        claim.updateStatus(ClaimStatus.UNDER_REVIEW);

        List<DomainEvent> events = claim.getDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(ClaimStatusChanged.class, events.getFirst());

        ClaimStatusChanged event = (ClaimStatusChanged) events.getFirst();
        assertEquals(CLAIM_ID, event.claimId());
        assertEquals(ClaimStatus.SUBMITTED, event.oldStatus());
        assertEquals(ClaimStatus.UNDER_REVIEW, event.newStatus());
    }
}

package com.example.demo.adapter.in.web;

import com.example.demo.application.exceptions.UnauthorizedClaimAccessException;
import com.example.demo.application.exceptions.UserNotFoundException;
import com.example.demo.domain.claim.ClaimNotFoundException;
import com.example.demo.domain.claim.InvalidClaimException;
import com.example.demo.domain.claim.InvalidStatusTransitionException;
import com.example.demo.user.domain.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClaimExceptionHandlerTest {

    private ErrorTypeConstants errorTypes;
    private ClaimExceptionHandler handler;

    @BeforeEach
    void setUp() {
        errorTypes = mock(ErrorTypeConstants.class);
        when(errorTypes.validationError()).thenReturn(URI.create("https://example.com/validation-error"));
        when(errorTypes.notFound()).thenReturn(URI.create("https://example.com/not-found"));
        when(errorTypes.forbidden()).thenReturn(URI.create("https://example.com/forbidden"));
        handler = new ClaimExceptionHandler(errorTypes);
    }

    @Test
    void should_Return400_When_UserNotFound() {
        UUID userId = UUID.randomUUID();
        var ex = new UserNotFoundException(userId);
        ProblemDetail detail = handler.handleUserNotFound(ex);
        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getTitle()).isEqualTo("Validation Error");
        assertThat(detail.getType()).isEqualTo(URI.create("https://example.com/validation-error"));
    }

    @Test
    void should_Return400_When_InvalidClaim() {
        var ex = new InvalidClaimException("Claim amount exceeds maximum");
        ProblemDetail detail = handler.handleInvalidClaim(ex);
        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getDetail()).isEqualTo("Claim amount exceeds maximum");
        assertThat(detail.getTitle()).isEqualTo("Validation Error");
    }

    @Test
    void should_Return404_When_ClaimNotFound() {
        UUID claimId = UUID.randomUUID();
        var ex = new ClaimNotFoundException(claimId);
        ProblemDetail detail = handler.handleClaimNotFound(ex);
        assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(detail.getTitle()).isEqualTo("Not Found");
        assertThat(detail.getType()).isEqualTo(URI.create("https://example.com/not-found"));
    }

    @Test
    void should_Return403_When_UnauthorizedClaimAccess() {
        var ex = new UnauthorizedClaimAccessException(UUID.randomUUID(), UUID.randomUUID());
        ProblemDetail detail = handler.handleUnauthorizedClaimAccess(ex);
        assertThat(detail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(detail.getTitle()).isEqualTo("Forbidden");
        assertThat(detail.getType()).isEqualTo(URI.create("https://example.com/forbidden"));
    }

    // found this bug — trying to do CLOSED -> APPROVED was returning 500 not 422
    @Test
    void should_Return422_When_InvalidStatusTransition() {
        var ex = new InvalidStatusTransitionException("Cannot transition from CLOSED to APPROVED");
        ProblemDetail result = handler.handleInvalidStatusTransition(ex);
        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(result.getTitle()).isEqualTo("Invalid Status Transition");
        assertThat(result.getDetail()).isEqualTo("Cannot transition from CLOSED to APPROVED");
    }

    // non-admin calling update status endpoint was returning 500 not 403
    // UnauthorizedException (domain one) != UnauthorizedClaimAccessException
    @Test
    void should_Return403_When_NonAdminUpdatesClaimStatus() {
        var ex = new UnauthorizedException("Only admin users can update claim status");
        ProblemDetail result = handler.handleUnauthorized(ex);
        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(result.getDetail()).isEqualTo("Only admin users can update claim status");
        assertThat(result.getTitle()).isEqualTo("Forbidden");
    }
}

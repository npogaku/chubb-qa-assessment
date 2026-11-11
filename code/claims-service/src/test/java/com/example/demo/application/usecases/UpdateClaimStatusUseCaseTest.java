package com.example.demo.application.usecases;

import com.example.demo.domain.claim.Claim;
import com.example.demo.domain.claim.ClaimNotFoundException;
import com.example.demo.domain.claim.ClaimRepository;
import com.example.demo.domain.claim.ClaimStatus;
import com.example.demo.domain.claim.InvalidStatusTransitionException;
import com.example.demo.user.domain.Email;
import com.example.demo.user.domain.UnauthorizedException;
import com.example.demo.user.domain.User;
import com.example.demo.user.domain.UserId;
import com.example.demo.user.domain.UserRepository;
import com.example.demo.user.domain.UserRole;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateClaimStatusUseCaseTest {

    private ClaimRepository claimRepository;
    private UserRepository userRepository;
    private MeterRegistry meterRegistry;
    private UpdateClaimStatusUseCase useCase;

    private static final UUID CLAIM_ID = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();
    private static final UUID NON_ADMIN_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        claimRepository = mock(ClaimRepository.class);
        userRepository = mock(UserRepository.class);
        meterRegistry = new SimpleMeterRegistry();
        useCase = new UpdateClaimStatusUseCase(claimRepository, userRepository, meterRegistry);
    }

    // TC-031: Use case orchestration logic tested with mocked repositories
    @Test
    void should_UpdateClaimStatus_When_AdminUserAndValidTransition() {
        // Arrange
        User adminUser = createAdminUser(ADMIN_USER_ID);
        Claim claim = createClaim(ClaimStatus.SUBMITTED);

        when(userRepository.findById(UserId.of(ADMIN_USER_ID))).thenReturn(Optional.of(adminUser));
        when(claimRepository.findById(CLAIM_ID)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenReturn(claim);

        var command = new UpdateClaimStatusCommand(CLAIM_ID, ClaimStatus.UNDER_REVIEW, ADMIN_USER_ID);

        // Act
        Claim result = useCase.execute(command);

        // Assert
        assertNotNull(result);
        assertEquals(ClaimStatus.UNDER_REVIEW, result.getStatus());
        verify(claimRepository).save(claim);
    }

    // TC-058: Non-admin user cannot update status (throws UnauthorizedException)
    @Test
    void should_ThrowUnauthorizedException_When_UserNotAdmin() {
        // Arrange
        User nonAdminUser = createClaimantUser(NON_ADMIN_USER_ID);
        when(userRepository.findById(UserId.of(NON_ADMIN_USER_ID))).thenReturn(Optional.of(nonAdminUser));

        var command = new UpdateClaimStatusCommand(CLAIM_ID, ClaimStatus.UNDER_REVIEW, NON_ADMIN_USER_ID);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> useCase.execute(command));
        verify(claimRepository, never()).save(any());
    }

    // TC-059: Claim not found throws ClaimNotFoundException
    @Test
    void should_ThrowClaimNotFoundException_When_ClaimNotFound() {
        // Arrange
        User adminUser = createAdminUser(ADMIN_USER_ID);
        when(userRepository.findById(UserId.of(ADMIN_USER_ID))).thenReturn(Optional.of(adminUser));
        when(claimRepository.findById(CLAIM_ID)).thenReturn(Optional.empty());

        var command = new UpdateClaimStatusCommand(CLAIM_ID, ClaimStatus.UNDER_REVIEW, ADMIN_USER_ID);

        // Act & Assert
        assertThrows(ClaimNotFoundException.class, () -> useCase.execute(command));
        verify(claimRepository, never()).save(any());
    }

    @Test
    void should_ThrowInvalidStatusTransitionException_When_TransitionInvalid() {
        // Arrange
        User adminUser = createAdminUser(ADMIN_USER_ID);
        Claim claim = createClaim(ClaimStatus.CLOSED);

        when(userRepository.findById(UserId.of(ADMIN_USER_ID))).thenReturn(Optional.of(adminUser));
        when(claimRepository.findById(CLAIM_ID)).thenReturn(Optional.of(claim));

        var command = new UpdateClaimStatusCommand(CLAIM_ID, ClaimStatus.APPROVED, ADMIN_USER_ID);

        // Act & Assert
        assertThrows(InvalidStatusTransitionException.class, () -> useCase.execute(command));
        verify(claimRepository, never()).save(any());
    }

    @Test
    void should_ThrowUnauthorizedException_When_UserNotFound() {
        // Arrange
        when(userRepository.findById(UserId.of(ADMIN_USER_ID))).thenReturn(Optional.empty());

        var command = new UpdateClaimStatusCommand(CLAIM_ID, ClaimStatus.UNDER_REVIEW, ADMIN_USER_ID);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> useCase.execute(command));
    }

    private User createAdminUser(UUID userId) {
        return new User(UserId.of(userId), "Admin User", Email.of("admin@test.com"), UserRole.ADMIN, Instant.now());
    }

    private User createClaimantUser(UUID userId) {
        return new User(UserId.of(userId), "Claimant User", Email.of("claimant@test.com"), UserRole.CLAIMANT, Instant.now());
    }

    private Claim createClaim(ClaimStatus status) {
        return Claim.reconstitute(
            CLAIM_ID,
            UUID.randomUUID(),
            LocalDate.now().minusDays(1),
            "123 Main Street, City",
            "Test claim description for unit testing",
            new BigDecimal("5000.00"),
            status,
            Instant.now(),
            Instant.now()
        );
    }
}

package com.example.demo.application.usecases;

import com.example.demo.domain.claim.ClaimNotFoundException;
import com.example.demo.application.exceptions.UnauthorizedClaimAccessException;
import com.example.demo.domain.claim.Claim;
import com.example.demo.domain.claim.ClaimRepository;
import com.example.demo.fixtures.ClaimTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetClaimUseCaseTest {

    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private GetClaimUseCase useCase;

    @Test
    void should_ReturnClaim_When_UserOwnsClaim() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Claim claim = ClaimTestBuilder.aClaim()
                .withClaimId(claimId)
                .withUserId(userId)
                .build();

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));

        Claim result = useCase.execute(new GetClaimQuery(claimId, userId));

        assertThat(result).isNotNull();
        assertThat(result.getClaimId()).isEqualTo(claimId);
        assertThat(result.getUserId()).isEqualTo(userId);
        verify(claimRepository).findById(claimId);
    }

    @Test
    void should_ThrowClaimNotFoundException_When_ClaimDoesNotExist() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(claimRepository.findById(claimId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new GetClaimQuery(claimId, userId)))
                .isInstanceOf(ClaimNotFoundException.class)
                .hasMessageContaining(claimId.toString());

        verify(claimRepository).findById(claimId);
    }

    @Test
    void should_ThrowUnauthorizedClaimAccessException_When_UserDoesNotOwnClaim() {
        UUID claimId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID requestingUserId = UUID.randomUUID();

        Claim claim = ClaimTestBuilder.aClaim()
                .withClaimId(claimId)
                .withUserId(ownerId)
                .build();

        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> useCase.execute(new GetClaimQuery(claimId, requestingUserId)))
                .isInstanceOf(UnauthorizedClaimAccessException.class)
                .hasMessageContaining(requestingUserId.toString())
                .hasMessageContaining(claimId.toString());

        verify(claimRepository).findById(claimId);
    }
}

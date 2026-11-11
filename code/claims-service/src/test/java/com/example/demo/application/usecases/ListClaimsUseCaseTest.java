package com.example.demo.application.usecases;

import com.example.demo.domain.claim.Claim;
import com.example.demo.domain.claim.ClaimRepository;
import com.example.demo.fixtures.ClaimTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListClaimsUseCaseTest {

    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private ListClaimsUseCase useCase;

    @Test
    void should_ReturnUserClaims_When_ClaimsExist() {
        UUID userId = UUID.randomUUID();

        Claim claim1 = ClaimTestBuilder.aClaim()
                .withUserId(userId)
                .withClaimAmount(new BigDecimal("1000.00"))
                .build();
        Claim claim2 = ClaimTestBuilder.aClaim()
                .withUserId(userId)
                .withClaimAmount(new BigDecimal("2000.00"))
                .build();

        when(claimRepository.findByUserId(userId)).thenReturn(List.of(claim2, claim1));

        List<Claim> result = useCase.execute(new ListClaimsQuery(userId));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getClaimAmount()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(result.get(1).getClaimAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        verify(claimRepository).findByUserId(userId);
    }

    @Test
    void should_ReturnEmptyList_When_UserHasNoClaims() {
        UUID userId = UUID.randomUUID();

        when(claimRepository.findByUserId(userId)).thenReturn(List.of());

        List<Claim> result = useCase.execute(new ListClaimsQuery(userId));

        assertThat(result).isEmpty();
        verify(claimRepository).findByUserId(userId);
    }
}

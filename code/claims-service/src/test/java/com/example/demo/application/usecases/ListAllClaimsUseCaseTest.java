package com.example.demo.application.usecases;

import com.example.demo.domain.claim.Claim;
import com.example.demo.domain.claim.ClaimRepository;
import com.example.demo.domain.claim.ClaimStatus;
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
class ListAllClaimsUseCaseTest {

    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private ListAllClaimsUseCase useCase;

    @Test
    void should_ReturnAllClaims_When_NoFiltersProvided() {
        // Arrange
        Claim claim1 = ClaimTestBuilder.aClaim().withClaimAmount(new BigDecimal("1000.00")).build();
        Claim claim2 = ClaimTestBuilder.aClaim().withClaimAmount(new BigDecimal("2000.00")).build();
        when(claimRepository.findAll()).thenReturn(List.of(claim1, claim2));

        // Act
        List<Claim> result = useCase.execute(ListAllClaimsQuery.all());

        // Assert
        assertThat(result).hasSize(2);
        verify(claimRepository).findAll();
    }

    @Test
    void should_FilterByStatus_When_StatusFilterProvided() {
        // Arrange
        Claim claim = ClaimTestBuilder.aClaim().build();
        when(claimRepository.findAllByStatus(ClaimStatus.SUBMITTED)).thenReturn(List.of(claim));

        // Act
        List<Claim> result = useCase.execute(ListAllClaimsQuery.byStatus(ClaimStatus.SUBMITTED));

        // Assert
        assertThat(result).hasSize(1);
        verify(claimRepository).findAllByStatus(ClaimStatus.SUBMITTED);
    }

    @Test
    void should_FilterByUserId_When_UserIdFilterProvided() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Claim claim = ClaimTestBuilder.aClaim().withUserId(userId).build();
        when(claimRepository.findByUserId(userId)).thenReturn(List.of(claim));

        // Act
        List<Claim> result = useCase.execute(ListAllClaimsQuery.byUserId(userId));

        // Assert
        assertThat(result).hasSize(1);
        verify(claimRepository).findByUserId(userId);
    }

    @Test
    void should_FilterByBothStatusAndUserId_When_BothFiltersProvided() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Claim claim = ClaimTestBuilder.aClaim().withUserId(userId).build();
        when(claimRepository.findAllByStatusAndUserId(ClaimStatus.APPROVED, userId))
                .thenReturn(List.of(claim));

        // Act
        List<Claim> result = useCase.execute(
                ListAllClaimsQuery.byStatusAndUserId(ClaimStatus.APPROVED, userId));

        // Assert
        assertThat(result).hasSize(1);
        verify(claimRepository).findAllByStatusAndUserId(ClaimStatus.APPROVED, userId);
    }

    @Test
    void should_ReturnEmptyList_When_NoClaimsMatchFilter() {
        // Arrange
        when(claimRepository.findAllByStatus(ClaimStatus.CLOSED)).thenReturn(List.of());

        // Act
        List<Claim> result = useCase.execute(ListAllClaimsQuery.byStatus(ClaimStatus.CLOSED));

        // Assert
        assertThat(result).isEmpty();
        verify(claimRepository).findAllByStatus(ClaimStatus.CLOSED);
    }

    @Test
    void should_HandleNullFilters_When_ConstructedWithNulls() {
        // Covers the null-guard branches in ListAllClaimsQuery compact constructor
        when(claimRepository.findAll()).thenReturn(List.of());

        List<Claim> result = useCase.execute(new ListAllClaimsQuery(null, null));

        assertThat(result).isEmpty();
        verify(claimRepository).findAll();
    }

    @Test
    void should_ReturnEmptyList_When_NoClaimsExist() {
        // Arrange
        when(claimRepository.findAll()).thenReturn(List.of());

        // Act
        List<Claim> result = useCase.execute(ListAllClaimsQuery.all());

        // Assert
        assertThat(result).isEmpty();
        verify(claimRepository).findAll();
    }
}

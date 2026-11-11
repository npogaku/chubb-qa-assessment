package com.example.demo.application.usecases;

import com.example.demo.domain.claim.ClaimRepository;
import com.example.demo.domain.claim.ClaimStatus;
import com.example.demo.domain.dashboard.DashboardStats;
import com.example.demo.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetDashboardStatsUseCaseTest {

    private UserRepository userRepository;
    private ClaimRepository claimRepository;
    private GetDashboardStatsUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        claimRepository = mock(ClaimRepository.class);
        useCase = new GetDashboardStatsUseCase(userRepository, claimRepository);
    }

    @Test
    void should_AggregateUserAndClaimCounts_When_Executed() {
        // Arrange
        when(userRepository.count()).thenReturn(50L);
        when(claimRepository.count()).thenReturn(18L);
        when(claimRepository.countGroupedByStatus()).thenReturn(Map.of(
                ClaimStatus.SUBMITTED, 10L,
                ClaimStatus.UNDER_REVIEW, 5L,
                ClaimStatus.APPROVED, 3L
        ));

        var query = new GetDashboardStatsQuery(false);

        // Act
        DashboardStats result = useCase.execute(query);

        // Assert
        assertNotNull(result);
        assertEquals(50, result.totalUsers());
        assertEquals(18, result.totalClaims());
        assertEquals(10, result.claimsByStatus().get(ClaimStatus.SUBMITTED));
        assertEquals(5, result.claimsByStatus().get(ClaimStatus.UNDER_REVIEW));
        assertEquals(3, result.claimsByStatus().get(ClaimStatus.APPROVED));
    }

    @Test
    void should_CallCountGroupedByStatus_When_Executed() {
        // Arrange
        when(userRepository.count()).thenReturn(0L);
        when(claimRepository.count()).thenReturn(0L);
        when(claimRepository.countGroupedByStatus()).thenReturn(Map.of());

        // Act
        useCase.execute(new GetDashboardStatsQuery(false));

        // Assert
        verify(claimRepository).countGroupedByStatus();
    }

    @Test
    void should_ReturnEmptyStatusMap_When_NoDataExists() {
        // Arrange
        when(userRepository.count()).thenReturn(0L);
        when(claimRepository.count()).thenReturn(0L);
        when(claimRepository.countGroupedByStatus()).thenReturn(Map.of());

        // Act
        DashboardStats result = useCase.execute(new GetDashboardStatsQuery(true));

        // Assert
        assertEquals(0, result.totalUsers());
        assertEquals(0, result.totalClaims());
        assertEquals(0, result.claimsByStatus().size());
    }
}

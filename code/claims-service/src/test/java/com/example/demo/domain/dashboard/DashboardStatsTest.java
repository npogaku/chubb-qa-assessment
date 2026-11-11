package com.example.demo.domain.dashboard;

import com.example.demo.domain.claim.ClaimStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DashboardStatsTest {

    @Test
    void should_CreateDashboardStats_When_ValidParameters() {
        // Arrange
        Map<ClaimStatus, Integer> claimsByStatus = Map.of(
                ClaimStatus.SUBMITTED, 10,
                ClaimStatus.UNDER_REVIEW, 5,
                ClaimStatus.APPROVED, 3
        );

        // Act
        DashboardStats stats = new DashboardStats(50, 18, claimsByStatus);

        // Assert
        assertEquals(50, stats.totalUsers());
        assertEquals(18, stats.totalClaims());
        assertEquals(3, stats.claimsByStatus().size());
        assertEquals(10, stats.claimsByStatus().get(ClaimStatus.SUBMITTED));
    }

    @Test
    void should_ThrowException_When_ClaimsByStatusIsNull() {
        assertThrows(NullPointerException.class, () -> new DashboardStats(0, 0, null));
    }

    @Test
    void should_ThrowException_When_TotalUsersIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new DashboardStats(-1, 0, Map.of()));
    }

    @Test
    void should_ThrowException_When_TotalClaimsIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new DashboardStats(0, -1, Map.of()));
    }

    @Test
    void should_CreateImmutableMap_When_Constructed() {
        // Arrange
        var mutableMap = new java.util.HashMap<ClaimStatus, Integer>();
        mutableMap.put(ClaimStatus.SUBMITTED, 5);

        // Act
        DashboardStats stats = new DashboardStats(1, 5, mutableMap);

        // Assert - should not be affected by original map changes
        mutableMap.put(ClaimStatus.APPROVED, 3);
        assertNotNull(stats.claimsByStatus());
        assertEquals(1, stats.claimsByStatus().size());
    }
}

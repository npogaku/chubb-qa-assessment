package com.example.demo.adapter.in.web;

import com.example.demo.adapter.out.cache.DashboardStatsCache;
import com.example.demo.application.usecases.GetDashboardStatsQuery;
import com.example.demo.application.usecases.GetDashboardStatsUseCase;
import com.example.demo.domain.claim.ClaimStatus;
import com.example.demo.domain.dashboard.DashboardStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardControllerTest {

    private GetDashboardStatsUseCase getDashboardStatsUseCase;
    private DashboardStatsCache dashboardStatsCache;
    private DashboardController controller;

    private static final DashboardStats SAMPLE_STATS = new DashboardStats(
            50, 18, Map.of(
                    ClaimStatus.SUBMITTED, 10,
                    ClaimStatus.APPROVED, 8
            )
    );

    @BeforeEach
    void setUp() {
        getDashboardStatsUseCase = mock(GetDashboardStatsUseCase.class);
        dashboardStatsCache = mock(DashboardStatsCache.class);
        controller = new DashboardController(getDashboardStatsUseCase, dashboardStatsCache);
    }

    @Test
    void should_ReturnCachedStats_When_CacheHitAndBypassFalse() {
        // Arrange
        when(dashboardStatsCache.get()).thenReturn(Optional.of(SAMPLE_STATS));

        // Act
        var response = controller.getDashboardStats(Optional.of(false));

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getCacheHit());
        assertEquals(50, response.getBody().getTotalUsers());
        assertTrue(response.getBody().getQueryTimeMs() >= 0);
        verify(getDashboardStatsUseCase, never()).execute(any());
    }

    @Test
    void should_QueryDatabaseAndCache_When_CacheMiss() {
        // Arrange
        when(dashboardStatsCache.get()).thenReturn(Optional.empty());
        when(getDashboardStatsUseCase.execute(any())).thenReturn(SAMPLE_STATS);

        // Act
        var response = controller.getDashboardStats(Optional.of(false));

        // Assert
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getCacheHit());
        verify(dashboardStatsCache).put(any(), anyLong());
    }

    @Test
    void should_BypassCacheAndQueryDatabase_When_BypassTrue() {
        // Arrange
        when(getDashboardStatsUseCase.execute(any())).thenReturn(SAMPLE_STATS);

        // Act
        var response = controller.getDashboardStats(Optional.of(true));

        // Assert
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getCacheHit());
        verify(dashboardStatsCache, never()).get();
        verify(dashboardStatsCache, never()).put(any(), anyLong());
    }

    @Test
    void should_ReturnNoContent_When_FlushCacheCalled() {
        // Act
        var response = controller.flushDashboardCache();

        // Assert
        assertEquals(204, response.getStatusCode().value());
        verify(dashboardStatsCache).evict();
    }

    @Test
    void should_MapClaimStatusToString_When_BuildingResponse() {
        // Arrange
        when(dashboardStatsCache.get()).thenReturn(Optional.of(SAMPLE_STATS));

        // Act
        var response = controller.getDashboardStats(Optional.empty());

        // Assert
        assertNotNull(response.getBody());
        var claimsByStatus = response.getBody().getClaimsByStatus();
        assertEquals(10, claimsByStatus.get("SUBMITTED"));
        assertEquals(8, claimsByStatus.get("APPROVED"));
    }
}

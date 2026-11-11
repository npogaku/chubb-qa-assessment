package com.example.demo.adapter.out.cache;

import com.example.demo.domain.claim.ClaimStatus;
import com.example.demo.domain.dashboard.DashboardStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class DashboardStatsCacheTest {

    private RedisTemplate<String, DashboardStats> redisTemplate;
    private ValueOperations<String, DashboardStats> valueOperations;
    private DashboardStatsCache cache;

    private static final DashboardStats SAMPLE_STATS = new DashboardStats(
            50, 18, Map.of(ClaimStatus.SUBMITTED, 10, ClaimStatus.APPROVED, 8)
    );

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cache = new DashboardStatsCache(redisTemplate);
    }

    @Test
    void should_ReturnCachedStats_When_CacheHit() {
        // Arrange
        when(valueOperations.get("dashboard:stats")).thenReturn(SAMPLE_STATS);

        // Act
        Optional<DashboardStats> result = cache.get();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(50, result.get().totalUsers());
    }

    @Test
    void should_ReturnEmpty_When_CacheMiss() {
        // Arrange
        when(valueOperations.get("dashboard:stats")).thenReturn(null);

        // Act
        Optional<DashboardStats> result = cache.get();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void should_ReturnEmpty_When_RedisUnavailable() {
        // Arrange (TC-031)
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection refused"));

        // Act
        Optional<DashboardStats> result = cache.get();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void should_StoreStatsWithTtl_When_PutCalled() {
        // Arrange (TC-062)

        // Act
        cache.put(SAMPLE_STATS, 300);

        // Assert
        verify(valueOperations).set(
                eq("dashboard:stats"),
                eq(SAMPLE_STATS),
                eq(Duration.ofSeconds(300))
        );
    }

    @Test
    void should_NotThrow_When_PutFailsDueToRedisUnavailable() {
        // Arrange
        doThrow(new RuntimeException("Redis connection refused"))
                .when(valueOperations).set(anyString(), any(), any(Duration.class));

        // Act — should not throw
        cache.put(SAMPLE_STATS, 300);
    }

    @Test
    void should_DeleteCacheKey_When_EvictCalled() {
        // Arrange (TC-063)
        when(redisTemplate.delete("dashboard:stats")).thenReturn(true);

        // Act
        cache.evict();

        // Assert
        verify(redisTemplate).delete("dashboard:stats");
    }

    @Test
    void should_NotThrow_When_EvictFailsDueToRedisUnavailable() {
        // Arrange
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis connection refused"));

        // Act — should not throw
        cache.evict();
    }

    @Test
    void should_NotLog_When_EvictReturnsFalse() {
        // Arrange — delete returns false (key was not present)
        when(redisTemplate.delete("dashboard:stats")).thenReturn(false);

        // Act — should not throw
        cache.evict();

        verify(redisTemplate).delete("dashboard:stats");
    }
}

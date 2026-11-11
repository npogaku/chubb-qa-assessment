package com.example.demo.adapter.out.persistence;

import com.example.demo.domain.claim.Claim;
import com.example.demo.domain.claim.ClaimStatus;
import com.example.demo.fixtures.ClaimTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClaimRepositoryAdapterTest {

    private ClaimJpaRepository jpaRepository;
    private ClaimPersistenceMapper mapper;
    private ClaimRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        jpaRepository = mock(ClaimJpaRepository.class);
        mapper = mock(ClaimPersistenceMapper.class);
        adapter = new ClaimRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    void should_SaveAndReturnClaim() {
        var claim = ClaimTestBuilder.aClaim().build();
        var entity = new ClaimEntity();
        var savedEntity = new ClaimEntity();
        when(mapper.toEntity(claim)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(claim);

        Claim result = adapter.save(claim);

        assertThat(result).isEqualTo(claim);
        verify(jpaRepository).save(entity);
    }

    @Test
    void should_ReturnClaim_When_FindByIdFound() {
        UUID claimId = UUID.randomUUID();
        var entity = new ClaimEntity();
        var claim = ClaimTestBuilder.aClaim().withClaimId(claimId).build();
        when(jpaRepository.findById(claimId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(claim);

        Optional<Claim> result = adapter.findById(claimId);

        assertThat(result).contains(claim);
    }

    @Test
    void should_ReturnEmpty_When_FindByIdNotFound() {
        UUID claimId = UUID.randomUUID();
        when(jpaRepository.findById(claimId)).thenReturn(Optional.empty());

        Optional<Claim> result = adapter.findById(claimId);

        assertThat(result).isEmpty();
    }

    @Test
    void should_ReturnClaims_When_FindByUserId() {
        UUID userId = UUID.randomUUID();
        var entity = new ClaimEntity();
        var claim = ClaimTestBuilder.aClaim().withUserId(userId).build();
        when(jpaRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(claim);

        List<Claim> result = adapter.findByUserId(userId);

        assertThat(result).containsExactly(claim);
    }

    @Test
    void should_ReturnAllClaims_When_FindAll() {
        var entity = new ClaimEntity();
        var claim = ClaimTestBuilder.aClaim().build();
        when(jpaRepository.findAll()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(claim);

        List<Claim> result = adapter.findAll();

        assertThat(result).containsExactly(claim);
    }

    @Test
    void should_ReturnClaimsByStatus_When_FindAllByStatus() {
        var entity = new ClaimEntity();
        var claim = ClaimTestBuilder.aClaim().build();
        when(jpaRepository.findByStatus(ClaimStatus.SUBMITTED)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(claim);

        List<Claim> result = adapter.findAllByStatus(ClaimStatus.SUBMITTED);

        assertThat(result).containsExactly(claim);
    }

    @Test
    void should_ReturnClaimsByStatusAndUserId() {
        UUID userId = UUID.randomUUID();
        var entity = new ClaimEntity();
        var claim = ClaimTestBuilder.aClaim().withUserId(userId).build();
        when(jpaRepository.findByStatusAndUserId(ClaimStatus.APPROVED, userId)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(claim);

        List<Claim> result = adapter.findAllByStatusAndUserId(ClaimStatus.APPROVED, userId);

        assertThat(result).containsExactly(claim);
    }

    @Test
    void should_ReturnTrue_When_ClaimExists() {
        UUID claimId = UUID.randomUUID();
        when(jpaRepository.existsById(claimId)).thenReturn(true);

        assertThat(adapter.existsById(claimId)).isTrue();
    }

    @Test
    void should_ReturnCount() {
        when(jpaRepository.count()).thenReturn(42L);

        assertThat(adapter.count()).isEqualTo(42L);
    }

    @Test
    void should_ReturnCountByStatus() {
        when(jpaRepository.countByStatus(ClaimStatus.SUBMITTED)).thenReturn(5L);

        assertThat(adapter.countByStatus(ClaimStatus.SUBMITTED)).isEqualTo(5L);
    }

    @Test
    void should_ReturnCountGroupedByStatus() {
        Object[] row = new Object[]{ClaimStatus.SUBMITTED, 10L};
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(row);
        when(jpaRepository.countGroupedByStatus()).thenReturn(rows);

        Map<ClaimStatus, Long> result = adapter.countGroupedByStatus();

        assertThat(result).containsEntry(ClaimStatus.SUBMITTED, 10L);
    }
}

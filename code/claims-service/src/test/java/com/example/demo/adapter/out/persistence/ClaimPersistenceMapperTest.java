package com.example.demo.adapter.out.persistence;

import com.example.demo.domain.claim.Claim;
import com.example.demo.domain.claim.ClaimStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimPersistenceMapperTest {

    private final ClaimPersistenceMapper mapper = Mappers.getMapper(ClaimPersistenceMapper.class);

    @Test
    @DisplayName("M4: toDomain returns null when entity is null")
    void should_ReturnNull_When_EntityIsNull() {
        // Act
        Claim result = mapper.toDomain(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("M4: toEntity converts valid Claim to ClaimEntity")
    void should_ConvertToEntity_When_ValidClaim() {
        // Arrange
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Claim claim = new Claim(claimId, userId, LocalDate.now().minusDays(1),
                "Test Location", "Valid description for testing mapper", new BigDecimal("5000.00"));

        // Act
        ClaimEntity entity = mapper.toEntity(claim);

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getClaimId()).isEqualTo(claimId);
        assertThat(entity.getUserId()).isEqualTo(userId);
        assertThat(entity.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(entity.getClaimAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("M4: toDomain converts valid ClaimEntity to Claim using reconstitute")
    void should_ConvertToDomain_When_ValidEntity() {
        // Arrange
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        ClaimEntity entity = new ClaimEntity();
        entity.setClaimId(claimId);
        entity.setUserId(userId);
        entity.setIncidentDate(LocalDate.now().minusDays(1));
        entity.setIncidentLocation("Test Location");
        entity.setDescription("Valid description for testing mapper");
        entity.setClaimAmount(new BigDecimal("5000.00"));
        entity.setStatus(ClaimStatus.UNDER_REVIEW);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        // Act
        Claim claim = mapper.toDomain(entity);

        // Assert
        assertThat(claim).isNotNull();
        assertThat(claim.getClaimId()).isEqualTo(claimId);
        assertThat(claim.getUserId()).isEqualTo(userId);
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(claim.getClaimAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(claim.getCreatedAt()).isEqualTo(now);
        assertThat(claim.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("M4: Bidirectional mapping preserves all field values")
    void should_PreserveAllFields_When_RoundTripMapping() {
        // Arrange
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Claim originalClaim = new Claim(claimId, userId, LocalDate.now().minusDays(5),
                "Original Location", "Original description for round trip test", new BigDecimal("7500.00"));

        // Act
        ClaimEntity entity = mapper.toEntity(originalClaim);
        Claim reconstitutedClaim = mapper.toDomain(entity);

        // Assert
        assertThat(reconstitutedClaim.getClaimId()).isEqualTo(originalClaim.getClaimId());
        assertThat(reconstitutedClaim.getUserId()).isEqualTo(originalClaim.getUserId());
        assertThat(reconstitutedClaim.getIncidentDate()).isEqualTo(originalClaim.getIncidentDate());
        assertThat(reconstitutedClaim.getIncidentLocation()).isEqualTo(originalClaim.getIncidentLocation());
        assertThat(reconstitutedClaim.getDescription()).isEqualTo(originalClaim.getDescription());
        assertThat(reconstitutedClaim.getClaimAmount()).isEqualByComparingTo(originalClaim.getClaimAmount());
        assertThat(reconstitutedClaim.getStatus()).isEqualTo(originalClaim.getStatus());
    }
}

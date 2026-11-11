package com.example.demo.adapter.out.persistence;

import com.example.demo.domain.claim.Claim;
import com.example.demo.domain.claim.ClaimRepository;
import com.example.demo.domain.claim.ClaimStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@Tag("integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ClaimRepositoryAdapter.class, ClaimPersistenceMapperImpl.class})
class ClaimRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ClaimJpaRepository claimJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private void insertTestUser(UUID userId) {
        jdbcTemplate.update(
                "INSERT INTO users (user_id, email, name, role, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING",
                userId, "test-" + userId + "@example.com", "Test User", "CLAIMANT"
        );
    }

    @Test
    @DisplayName("TC-018: Full claim submission with Postgres persistence validates data is saved correctly")
    void should_SaveAndRetrieveClaim_When_ValidClaimSubmitted() {
        // Arrange
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        insertTestUser(userId);

        Claim claim = new Claim(
                claimId,
                userId,
                LocalDate.now().minusDays(1),
                "123 Main St, City",
                "Vehicle collision on highway",
                new BigDecimal("5000.00")
        );

        // Act
        Claim saved = claimRepository.save(claim);
        Optional<Claim> retrieved = claimRepository.findById(claimId);

        // Assert
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getClaimId()).isEqualTo(claimId);
        assertThat(retrieved.get().getUserId()).isEqualTo(userId);
        assertThat(retrieved.get().getIncidentLocation()).isEqualTo("123 Main St, City");
        assertThat(retrieved.get().getDescription()).isEqualTo("Vehicle collision on highway");
        assertThat(retrieved.get().getClaimAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(retrieved.get().getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
    }

    @Test
    @DisplayName("Retrieve claims by userId, verify ordering by created_at DESC")
    void should_ReturnClaimsOrderedByCreatedAtDesc_When_FindByUserId() {
        // Arrange - Use JPA repository directly to have full control over timestamps
        UUID userId = UUID.randomUUID();
        insertTestUser(userId);

        UUID claim1Id = UUID.randomUUID();
        UUID claim2Id = UUID.randomUUID();

        // Create entities with explicit timestamps to ensure deterministic ordering
        Instant earlierTime = Instant.now().minusSeconds(60);
        Instant laterTime = Instant.now();

        ClaimEntity entity1 = new ClaimEntity();
        entity1.setClaimId(claim1Id);
        entity1.setUserId(userId);
        entity1.setIncidentDate(LocalDate.now().minusDays(3));
        entity1.setIncidentLocation("Location One");
        entity1.setDescription("Description for first claim");
        entity1.setClaimAmount(new BigDecimal("1000.00"));
        entity1.setStatus(ClaimStatus.SUBMITTED);
        entity1.setCreatedAt(earlierTime);
        entity1.setUpdatedAt(earlierTime);

        ClaimEntity entity2 = new ClaimEntity();
        entity2.setClaimId(claim2Id);
        entity2.setUserId(userId);
        entity2.setIncidentDate(LocalDate.now().minusDays(1));
        entity2.setIncidentLocation("Location Two");
        entity2.setDescription("Description for second claim");
        entity2.setClaimAmount(new BigDecimal("2000.00"));
        entity2.setStatus(ClaimStatus.SUBMITTED);
        entity2.setCreatedAt(laterTime);
        entity2.setUpdatedAt(laterTime);

        claimJpaRepository.save(entity1);
        claimJpaRepository.save(entity2);

        // Act
        List<Claim> claims = claimRepository.findByUserId(userId);

        // Assert - Most recent (claim2) should be first
        assertThat(claims).hasSize(2);
        assertThat(claims.get(0).getClaimId()).isEqualTo(claim2Id);
        assertThat(claims.get(1).getClaimId()).isEqualTo(claim1Id);
    }

    @Test
    @DisplayName("Update claim status, verify updated_at changes")
    void should_UpdateUpdatedAtTimestamp_When_StatusChanged() {
        // Arrange
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        insertTestUser(userId);

        Claim claim = new Claim(claimId, userId, LocalDate.now().minusDays(1),
                "Test Location", "Description for status test", new BigDecimal("1000.00"));
        Claim saved = claimRepository.save(claim);
        Instant originalUpdatedAt = saved.getUpdatedAt();

        // Act - updateStatus sets updatedAt to Instant.now() which will be after originalUpdatedAt
        saved.updateStatus(ClaimStatus.UNDER_REVIEW);
        Claim updated = claimRepository.save(saved);

        // Assert
        assertThat(updated.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("H2: Saving claim with non-existent userId throws constraint exception")
    void should_ThrowException_When_ForeignKeyConstraintViolated() {
        // Arrange
        UUID claimId = UUID.randomUUID();
        UUID nonExistentUserId = UUID.randomUUID(); // User not inserted

        ClaimEntity entity = new ClaimEntity();
        entity.setClaimId(claimId);
        entity.setUserId(nonExistentUserId);
        entity.setIncidentDate(LocalDate.now().minusDays(1));
        entity.setIncidentLocation("Test Location");
        entity.setDescription("Description for FK test");
        entity.setClaimAmount(new BigDecimal("1000.00"));
        entity.setStatus(ClaimStatus.SUBMITTED);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        // Act & Assert - Should throw exception due to FK constraint when flushed to DB
        // Note: Can be either Spring DataIntegrityViolationException or Hibernate ConstraintViolationException
        assertThatThrownBy(() -> {
            claimJpaRepository.save(entity);
            entityManager.flush(); // Force constraint check
        }).satisfiesAnyOf(
            ex -> assertThat(ex).isInstanceOf(DataIntegrityViolationException.class),
            ex -> assertThat(ex).isInstanceOf(org.hibernate.exception.ConstraintViolationException.class)
        ).hasMessageContaining("fk_claims_user_id");
    }

    @Test
    @DisplayName("M3: Verify all performance indexes exist in database")
    void should_HaveAllIndexes_When_MigrationRuns() {
        // Act
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
            "SELECT indexname FROM pg_indexes WHERE tablename = 'claims' AND schemaname = 'public'"
        );

        List<String> indexNames = indexes.stream()
            .map(row -> (String) row.get("indexname"))
            .toList();

        // Assert
        assertThat(indexNames)
            .contains(
                "pk_claims",  // Primary key index
                "idx_claims_user_id",
                "idx_claims_status",
                "idx_claims_user_id_created_at"
            );
    }

    @Test
    @DisplayName("H1: Find claims by status returns only matching claims")
    void should_ReturnClaimsWithMatchingStatus_When_FindByStatus() {
        // Arrange
        UUID userId = UUID.randomUUID();
        insertTestUser(userId);

        Claim submittedClaim = new Claim(UUID.randomUUID(), userId, LocalDate.now().minusDays(1),
                "Location A", "Submitted claim description", new BigDecimal("1000.00"));
        claimRepository.save(submittedClaim);

        Claim reviewClaim = new Claim(UUID.randomUUID(), userId, LocalDate.now().minusDays(2),
                "Location B", "Under review claim description", new BigDecimal("2000.00"));
        reviewClaim.updateStatus(ClaimStatus.UNDER_REVIEW);
        claimRepository.save(reviewClaim);

        // Act
        List<ClaimEntity> submittedClaims = claimJpaRepository.findByStatus(ClaimStatus.SUBMITTED);

        // Assert
        assertThat(submittedClaims).hasSize(1);
        assertThat(submittedClaims.get(0).getClaimId()).isEqualTo(submittedClaim.getClaimId());
    }
}

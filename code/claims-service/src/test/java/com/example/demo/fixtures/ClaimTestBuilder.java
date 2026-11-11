package com.example.demo.fixtures;

import com.example.demo.application.usecases.CreateClaimCommand;
import com.example.demo.domain.claim.Claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Test data builder for Claim domain objects.
 * Provides sensible defaults for unit tests.
 *
 * Usage:
 * <pre>
 *   Claim claim = ClaimTestBuilder.aClaim()
 *       .withUserId(userId)
 *       .withClaimAmount(new BigDecimal("10000"))
 *       .build();
 * </pre>
 */
public final class ClaimTestBuilder {

    private UUID claimId = UUID.randomUUID();
    private UUID userId = UUID.randomUUID();
    private LocalDate incidentDate = LocalDate.now().minusDays(1);
    private String incidentLocation = "123 Main St, Springfield, IL";
    private String description = "Water damage to basement due to pipe burst";
    private BigDecimal claimAmount = new BigDecimal("5000.00");

    private ClaimTestBuilder() {}

    public static ClaimTestBuilder aClaim() {
        return new ClaimTestBuilder();
    }

    public ClaimTestBuilder withClaimId(UUID claimId) {
        this.claimId = claimId;
        return this;
    }

    public ClaimTestBuilder withUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public ClaimTestBuilder withIncidentDate(LocalDate incidentDate) {
        this.incidentDate = incidentDate;
        return this;
    }

    public ClaimTestBuilder withIncidentLocation(String incidentLocation) {
        this.incidentLocation = incidentLocation;
        return this;
    }

    public ClaimTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ClaimTestBuilder withClaimAmount(BigDecimal claimAmount) {
        this.claimAmount = claimAmount;
        return this;
    }

    /**
     * Builds a new Claim domain object with validation and domain events.
     */
    public Claim build() {
        return new Claim(claimId, userId, incidentDate, incidentLocation, description, claimAmount);
    }

    /**
     * Converts builder state to CreateClaimCommand (for use case tests).
     */
    public CreateClaimCommand toCommand() {
        return new CreateClaimCommand(userId, incidentDate, incidentLocation, description, claimAmount);
    }
}

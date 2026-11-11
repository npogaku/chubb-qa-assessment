package com.example.demo.fixtures;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Factory for generating realistic test Claim objects.
 * Uses Faker for randomized data to ensure parallel-safe, collision-free tests.
 */
public class ClaimFactory {

    private static final Faker faker = new Faker();

    /**
     * Create a Claim with randomly generated incident date, description, and UUID
     * Status defaults to SUBMITTED
     */
    public static ClaimTestData createClaim() {
        return new ClaimTestData()
                .withId(UUID.randomUUID().toString())
                .withIncidentDate(LocalDate.now().minusDays(faker.number().numberBetween(1, 90)))
                .withDescription(faker.lorem().sentence(10))
                .withStatus("SUBMITTED");
    }

    /**
     * Create a Claim with custom field overrides
     * @param overrides Map of field names to values
     */
    public static ClaimTestData createClaim(Map<String, Object> overrides) {
        ClaimTestData claim = createClaim();
        if (overrides != null) {
            if (overrides.containsKey("id")) {
                claim.withId((String) overrides.get("id"));
            }
            if (overrides.containsKey("incidentDate")) {
                claim.withIncidentDate((LocalDate) overrides.get("incidentDate"));
            }
            if (overrides.containsKey("description")) {
                claim.withDescription((String) overrides.get("description"));
            }
            if (overrides.containsKey("status")) {
                claim.withStatus((String) overrides.get("status"));
            }
        }
        return claim;
    }

    /**
     * Test data class for Claim
     */
    public static class ClaimTestData {
        private String id;
        private LocalDate incidentDate;
        private String description;
        private String status;

        public ClaimTestData withId(String id) {
            this.id = id;
            return this;
        }

        public ClaimTestData withIncidentDate(LocalDate incidentDate) {
            this.incidentDate = incidentDate;
            return this;
        }

        public ClaimTestData withDescription(String description) {
            this.description = description;
            return this;
        }

        public ClaimTestData withStatus(String status) {
            this.status = status;
            return this;
        }

        public String getId() {
            return id;
        }

        public LocalDate getIncidentDate() {
            return incidentDate;
        }

        public String getDescription() {
            return description;
        }

        public String getStatus() {
            return status;
        }

        @Override
        public String toString() {
            return "ClaimTestData{" +
                    "id='" + id + '\'' +
                    ", incidentDate=" + incidentDate +
                    ", description='" + description + '\'' +
                    ", status='" + status + '\'' +
                    '}';
        }
    }
}

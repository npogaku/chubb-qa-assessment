package com.example.demo.fixtures;

/**
 * Single source of truth for Claim-related JSON test data.
 * Used by contract validation tests in BFF Service.
 *
 * When schema changes, update ONLY these builders — not 20+ test files.
 */
public final class ClaimJsonFixtures {

    private ClaimJsonFixtures() {}

    // ========================================================================
    // SUBMIT CLAIM REQUEST (BFF endpoint)
    // ========================================================================

    public static SubmitClaimRequestBuilder submitClaimRequest() {
        return new SubmitClaimRequestBuilder();
    }

    public static final class SubmitClaimRequestBuilder {
        private String incidentDate = "2026-02-15";
        private String incidentLocation = "123 Main St, Springfield, IL";
        private String description = "Water damage to basement due to pipe burst";
        private double claimAmount = 5000.00;

        public SubmitClaimRequestBuilder withIncidentDate(String date) { this.incidentDate = date; return this; }
        public SubmitClaimRequestBuilder withIncidentLocation(String location) { this.incidentLocation = location; return this; }
        public SubmitClaimRequestBuilder withDescription(String description) { this.description = description; return this; }
        public SubmitClaimRequestBuilder withClaimAmount(double amount) { this.claimAmount = amount; return this; }

        public String toJson() {
            return """
                {
                    "incidentDate": "%s",
                    "incidentLocation": "%s",
                    "description": "%s",
                    "claimAmount": %.2f
                }
                """.formatted(incidentDate, incidentLocation, description, claimAmount);
        }

        /**
         * Returns JSON missing the specified field (for negative tests).
         */
        public String toJsonWithoutField(String field) {
            return switch (field) {
                case "incidentDate" -> """
                    {
                        "incidentLocation": "%s",
                        "description": "%s",
                        "claimAmount": %.2f
                    }
                    """.formatted(incidentLocation, description, claimAmount);
                case "incidentLocation" -> """
                    {
                        "incidentDate": "%s",
                        "description": "%s",
                        "claimAmount": %.2f
                    }
                    """.formatted(incidentDate, description, claimAmount);
                case "description" -> """
                    {
                        "incidentDate": "%s",
                        "incidentLocation": "%s",
                        "claimAmount": %.2f
                    }
                    """.formatted(incidentDate, incidentLocation, claimAmount);
                case "claimAmount" -> """
                    {
                        "incidentDate": "%s",
                        "incidentLocation": "%s",
                        "description": "%s"
                    }
                    """.formatted(incidentDate, incidentLocation, description);
                default -> throw new IllegalArgumentException("Unknown field: " + field);
            };
        }
    }

    // ========================================================================
    // CLAIM RESPONSE
    // ========================================================================

    public static ClaimResponseBuilder claimResponse() {
        return new ClaimResponseBuilder();
    }

    public static final class ClaimResponseBuilder {
        private String claimId = "660e8400-e29b-41d4-a716-446655440001";
        private String userId = "550e8400-e29b-41d4-a716-446655440000";
        private String incidentDate = "2026-02-15";
        private String incidentLocation = "123 Main St, Springfield, IL";
        private String description = "Water damage to basement due to pipe burst";
        private double claimAmount = 5000.00;
        private String status = "SUBMITTED";
        private String createdAt = "2026-02-17T10:30:00.000Z";
        private String updatedAt = "2026-02-17T10:30:00.000Z";

        public ClaimResponseBuilder withClaimId(String id) { this.claimId = id; return this; }
        public ClaimResponseBuilder withUserId(String id) { this.userId = id; return this; }
        public ClaimResponseBuilder withIncidentDate(String date) { this.incidentDate = date; return this; }
        public ClaimResponseBuilder withIncidentLocation(String location) { this.incidentLocation = location; return this; }
        public ClaimResponseBuilder withDescription(String description) { this.description = description; return this; }
        public ClaimResponseBuilder withClaimAmount(double amount) { this.claimAmount = amount; return this; }
        public ClaimResponseBuilder withStatus(String status) { this.status = status; return this; }
        public ClaimResponseBuilder withCreatedAt(String createdAt) { this.createdAt = createdAt; return this; }
        public ClaimResponseBuilder withUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; return this; }

        public String toJson() {
            return """
                {
                    "claimId": "%s",
                    "userId": "%s",
                    "incidentDate": "%s",
                    "incidentLocation": "%s",
                    "description": "%s",
                    "claimAmount": %.2f,
                    "status": "%s",
                    "createdAt": "%s",
                    "updatedAt": "%s"
                }
                """.formatted(claimId, userId, incidentDate, incidentLocation,
                              description, claimAmount, status, createdAt, updatedAt);
        }

        /**
         * Returns JSON missing required fields (for negative tests).
         */
        public String toJsonWithoutRequiredFields() {
            return """
                {
                    "claimId": "%s",
                    "status": "%s"
                }
                """.formatted(claimId, status);
        }

        /**
         * Wraps this response in a JSON array (for list endpoint tests).
         */
        public String toJsonArray() {
            return "[" + toJson() + "]";
        }
    }

    // ========================================================================
    // CLAIM SUMMARY RESPONSE (for list endpoint)
    // ========================================================================

    public static ClaimSummaryResponseBuilder claimSummaryResponse() {
        return new ClaimSummaryResponseBuilder();
    }

    public static final class ClaimSummaryResponseBuilder {
        private String claimId = "660e8400-e29b-41d4-a716-446655440001";
        private String incidentDate = "2026-02-15";
        private String description = "Water damage to basement due to pipe burst";
        private double claimAmount = 5000.00;
        private String status = "SUBMITTED";
        private String createdAt = "2026-02-17T10:30:00.000Z";

        public ClaimSummaryResponseBuilder withClaimId(String id) { this.claimId = id; return this; }
        public ClaimSummaryResponseBuilder withIncidentDate(String date) { this.incidentDate = date; return this; }
        public ClaimSummaryResponseBuilder withDescription(String description) { this.description = description; return this; }
        public ClaimSummaryResponseBuilder withClaimAmount(double amount) { this.claimAmount = amount; return this; }
        public ClaimSummaryResponseBuilder withStatus(String status) { this.status = status; return this; }
        public ClaimSummaryResponseBuilder withCreatedAt(String createdAt) { this.createdAt = createdAt; return this; }

        public String toJson() {
            return """
                {
                    "claimId": "%s",
                    "incidentDate": "%s",
                    "description": "%s",
                    "claimAmount": %.2f,
                    "status": "%s",
                    "createdAt": "%s"
                }
                """.formatted(claimId, incidentDate, description, claimAmount, status, createdAt);
        }

        public String toJsonArray() {
            return "[" + toJson() + "]";
        }
    }

    // ========================================================================
    // UPDATE CLAIM STATUS REQUEST
    // ========================================================================

    public static UpdateClaimStatusRequestBuilder updateClaimStatusRequest() {
        return new UpdateClaimStatusRequestBuilder();
    }

    public static final class UpdateClaimStatusRequestBuilder {
        private String newStatus = "APPROVED";

        public UpdateClaimStatusRequestBuilder withNewStatus(String newStatus) { this.newStatus = newStatus; return this; }

        public String toJson() {
            return """
                {
                    "newStatus": "%s"
                }
                """.formatted(newStatus);
        }
    }

    // ========================================================================
    // CLAIM STATUS ENUM CONSTANTS
    // ========================================================================

    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_INVALID = "INVALID_STATUS";
}

package com.example.demo.fixtures;

/**
 * Single source of truth for Dashboard-related JSON test data.
 * Used by contract validation tests in BFF Service.
 */
public final class DashboardJsonFixtures {

    private DashboardJsonFixtures() {}

    public static DashboardStatsResponseBuilder dashboardStatsResponse() {
        return new DashboardStatsResponseBuilder();
    }

    public static final class DashboardStatsResponseBuilder {
        private int totalUsers = 50;
        private int totalClaims = 142;
        private int submitted = 45;
        private int underReview = 32;
        private int approved = 50;
        private int rejected = 15;
        private boolean cacheHit = true;
        private int queryTimeMs = 12;

        public DashboardStatsResponseBuilder withTotalUsers(int totalUsers) { this.totalUsers = totalUsers; return this; }
        public DashboardStatsResponseBuilder withTotalClaims(int totalClaims) { this.totalClaims = totalClaims; return this; }
        public DashboardStatsResponseBuilder withSubmitted(int submitted) { this.submitted = submitted; return this; }
        public DashboardStatsResponseBuilder withUnderReview(int underReview) { this.underReview = underReview; return this; }
        public DashboardStatsResponseBuilder withApproved(int approved) { this.approved = approved; return this; }
        public DashboardStatsResponseBuilder withRejected(int rejected) { this.rejected = rejected; return this; }
        public DashboardStatsResponseBuilder withCacheHit(boolean cacheHit) { this.cacheHit = cacheHit; return this; }
        public DashboardStatsResponseBuilder withQueryTimeMs(int queryTimeMs) { this.queryTimeMs = queryTimeMs; return this; }

        public String toJson() {
            return """
                {
                    "totalUsers": %d,
                    "totalClaims": %d,
                    "claimsByStatus": {
                        "SUBMITTED": %d,
                        "UNDER_REVIEW": %d,
                        "APPROVED": %d,
                        "REJECTED": %d
                    },
                    "cacheHit": %b,
                    "queryTimeMs": %d
                }
                """.formatted(totalUsers, totalClaims, submitted, underReview, approved, rejected, cacheHit, queryTimeMs);
        }

        /**
         * Simplified response with minimal status breakdown (for bypassCache test).
         */
        public String toJsonMinimal() {
            return """
                {
                    "totalUsers": %d,
                    "totalClaims": %d,
                    "claimsByStatus": {
                        "SUBMITTED": %d
                    },
                    "cacheHit": %b,
                    "queryTimeMs": %d
                }
                """.formatted(totalUsers, totalClaims, submitted, cacheHit, queryTimeMs);
        }
    }
}

package com.example.demo.fixtures;

/**
 * Single source of truth for User-related JSON test data.
 * Used by contract validation tests in Claims Service.
 */
public final class UserJsonFixtures {

    private UserJsonFixtures() {}

    // ========================================================================
    // CREATE USER REQUEST
    // ========================================================================

    public static CreateUserRequestBuilder createUserRequest() {
        return new CreateUserRequestBuilder();
    }

    public static final class CreateUserRequestBuilder {
        private String userId = "550e8400-e29b-41d4-a716-446655440000";
        private String email = "jane.doe@example.com";
        private String name = "Jane Doe";
        private String role = "CLAIMANT";

        public CreateUserRequestBuilder withUserId(String userId) { this.userId = userId; return this; }
        public CreateUserRequestBuilder withEmail(String email) { this.email = email; return this; }
        public CreateUserRequestBuilder withName(String name) { this.name = name; return this; }
        public CreateUserRequestBuilder withRole(String role) { this.role = role; return this; }

        public String toJson() {
            return """
                {
                    "userId": "%s",
                    "email": "%s",
                    "name": "%s",
                    "role": "%s"
                }
                """.formatted(userId, email, name, role);
        }

        /**
         * Returns JSON missing required fields (for negative tests).
         */
        public String toJsonWithoutRequiredFields() {
            return """
                {
                    "email": "%s"
                }
                """.formatted(email);
        }
    }

    // ========================================================================
    // USER RESPONSE
    // ========================================================================

    public static UserResponseBuilder userResponse() {
        return new UserResponseBuilder();
    }

    public static final class UserResponseBuilder {
        private String userId = "550e8400-e29b-41d4-a716-446655440000";
        private String email = "jane.doe@example.com";
        private String name = "Jane Doe";
        private String role = "CLAIMANT";
        private String createdAt = "2026-02-17T10:30:00.000Z";

        public UserResponseBuilder withUserId(String userId) { this.userId = userId; return this; }
        public UserResponseBuilder withEmail(String email) { this.email = email; return this; }
        public UserResponseBuilder withName(String name) { this.name = name; return this; }
        public UserResponseBuilder withRole(String role) { this.role = role; return this; }
        public UserResponseBuilder withCreatedAt(String createdAt) { this.createdAt = createdAt; return this; }

        public String toJson() {
            return """
                {
                    "userId": "%s",
                    "email": "%s",
                    "name": "%s",
                    "role": "%s",
                    "createdAt": "%s"
                }
                """.formatted(userId, email, name, role, createdAt);
        }

        /**
         * Returns JSON missing required fields (for negative tests).
         */
        public String toJsonWithoutRequiredFields() {
            return """
                {
                    "userId": "%s",
                    "email": "%s"
                }
                """.formatted(userId, email);
        }
    }

    // ========================================================================
    // USER ROLE ENUM CONSTANTS
    // ========================================================================

    public static final String ROLE_CLAIMANT = "CLAIMANT";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_INVALID = "SUPERADMIN";
}

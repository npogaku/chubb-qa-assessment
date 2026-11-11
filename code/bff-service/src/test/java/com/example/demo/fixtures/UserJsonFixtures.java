package com.example.demo.fixtures;

/**
 * Single source of truth for User-related JSON test data.
 * Used by contract validation tests in BFF Service.
 */
public final class UserJsonFixtures {

    private UserJsonFixtures() {}

    // ========================================================================
    // SIGNUP REQUEST
    // ========================================================================

    public static SignupRequestBuilder signupRequest() {
        return new SignupRequestBuilder();
    }

    public static final class SignupRequestBuilder {
        private String email = "jane.doe@example.com";
        private String password = "SecurePass123!";
        private String name = "Jane Doe";

        public SignupRequestBuilder withEmail(String email) { this.email = email; return this; }
        public SignupRequestBuilder withPassword(String password) { this.password = password; return this; }
        public SignupRequestBuilder withName(String name) { this.name = name; return this; }

        public String toJson() {
            return """
                {
                    "email": "%s",
                    "password": "%s",
                    "name": "%s"
                }
                """.formatted(email, password, name);
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
    // LOGIN REQUEST
    // ========================================================================

    public static LoginRequestBuilder loginRequest() {
        return new LoginRequestBuilder();
    }

    public static final class LoginRequestBuilder {
        private String email = "jane.doe@example.com";
        private String password = "SecurePass123!";

        public LoginRequestBuilder withEmail(String email) { this.email = email; return this; }
        public LoginRequestBuilder withPassword(String password) { this.password = password; return this; }

        public String toJson() {
            return """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);
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
    }

    // ========================================================================
    // USER ROLE ENUM CONSTANTS
    // ========================================================================

    public static final String ROLE_CLAIMANT = "CLAIMANT";
    public static final String ROLE_ADMIN = "ADMIN";
}

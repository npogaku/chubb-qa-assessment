package com.example.demo.fixtures;

/**
 * Single source of truth for RFC 7807 ProblemDetail JSON test data.
 * Used by contract validation tests in Claims Service.
 */
public final class ProblemDetailJsonFixtures {

    private ProblemDetailJsonFixtures() {}

    public static ProblemDetailBuilder problemDetail() {
        return new ProblemDetailBuilder();
    }

    public static final class ProblemDetailBuilder {
        private String type = "https://api.demo-app.com/errors/validation-error";
        private String title = "Validation Error";
        private int status = 400;
        private String detail = "Email format is invalid";
        private String instance = "/api/users";

        public ProblemDetailBuilder withType(String type) { this.type = type; return this; }
        public ProblemDetailBuilder withTitle(String title) { this.title = title; return this; }
        public ProblemDetailBuilder withStatus(int status) { this.status = status; return this; }
        public ProblemDetailBuilder withDetail(String detail) { this.detail = detail; return this; }
        public ProblemDetailBuilder withInstance(String instance) { this.instance = instance; return this; }

        public String toJson() {
            return """
                {
                    "type": "%s",
                    "title": "%s",
                    "status": %d,
                    "detail": "%s",
                    "instance": "%s"
                }
                """.formatted(type, title, status, detail, instance);
        }

        // ========================================================================
        // CONVENIENCE BUILDERS FOR COMMON ERROR TYPES
        // ========================================================================

        public static ProblemDetailBuilder unauthorized() {
            return new ProblemDetailBuilder()
                    .withType("https://api.demo-app.com/errors/unauthorized")
                    .withTitle("Unauthorized")
                    .withStatus(401)
                    .withDetail("Bearer token is missing or invalid");
        }

        public static ProblemDetailBuilder conflict() {
            return new ProblemDetailBuilder()
                    .withType("https://api.demo-app.com/errors/duplicate-entity")
                    .withTitle("Conflict")
                    .withStatus(409)
                    .withDetail("A user with this email already exists");
        }

        public static ProblemDetailBuilder notFound() {
            return new ProblemDetailBuilder()
                    .withType("https://api.demo-app.com/errors/not-found")
                    .withTitle("Not Found")
                    .withStatus(404)
                    .withDetail("User not found");
        }

        public static ProblemDetailBuilder validationError() {
            return new ProblemDetailBuilder()
                    .withType("https://api.demo-app.com/errors/validation-error")
                    .withTitle("Validation Error")
                    .withStatus(400);
        }
    }
}

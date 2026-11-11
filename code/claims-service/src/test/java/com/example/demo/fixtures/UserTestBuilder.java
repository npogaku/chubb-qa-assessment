package com.example.demo.fixtures;

import com.example.demo.user.domain.Email;
import com.example.demo.user.domain.User;
import com.example.demo.user.domain.UserId;
import com.example.demo.user.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Test data builder for User domain objects.
 * Provides sensible defaults for unit tests.
 *
 * Usage:
 * <pre>
 *   User user = UserTestBuilder.aUser()
 *       .withEmail("test@example.com")
 *       .withRole(UserRole.ADMIN)
 *       .build();
 * </pre>
 */
public final class UserTestBuilder {

    private UUID userId = UUID.randomUUID();
    private String name = "Test User";
    private String email = "test@example.com";
    private UserRole role = UserRole.CLAIMANT;
    private Instant createdAt = Instant.now();

    private UserTestBuilder() {}

    public static UserTestBuilder aUser() {
        return new UserTestBuilder();
    }

    public UserTestBuilder withUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public UserTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public UserTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserTestBuilder withRole(UserRole role) {
        this.role = role;
        return this;
    }

    public UserTestBuilder withCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public UserTestBuilder asClaimant() {
        this.role = UserRole.CLAIMANT;
        return this;
    }

    public UserTestBuilder asAdmin() {
        this.role = UserRole.ADMIN;
        return this;
    }

    /**
     * Builds a new User domain object.
     */
    public User build() {
        return new User(UserId.of(userId), name, Email.of(email), role, createdAt);
    }
}

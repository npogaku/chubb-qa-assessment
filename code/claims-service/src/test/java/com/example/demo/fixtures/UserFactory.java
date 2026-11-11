package com.example.demo.fixtures;

import com.example.demo.user.domain.Email;
import com.example.demo.user.domain.User;
import com.example.demo.user.domain.UserId;
import com.example.demo.user.domain.UserRole;
import com.github.javafaker.Faker;

import java.time.Instant;
import java.util.UUID;

/**
 * Factory for generating realistic test User domain objects.
 * Uses Faker for randomized data to ensure parallel-safe, collision-free tests.
 * Builder pattern allows overriding any field for specific test scenarios.
 */
public class UserFactory {

    private static final Faker faker = new Faker();

    public static UserBuilder aUser() {
        return new UserBuilder()
                .withUserId(UserId.generate())
                .withEmail(Email.of(faker.internet().emailAddress()))
                .withName(faker.name().fullName())
                .withRole(UserRole.CLAIMANT)
                .withCreatedAt(Instant.now());
    }

    public static UserBuilder anAdmin() {
        return aUser().withRole(UserRole.ADMIN);
    }

    public static class UserBuilder {

        private UserId userId;
        private String name;
        private Email email;
        private UserRole role;
        private Instant createdAt;

        public UserBuilder withUserId(UserId userId) {
            this.userId = userId;
            return this;
        }

        public UserBuilder withUserId(UUID value) {
            this.userId = UserId.of(value);
            return this;
        }

        public UserBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public UserBuilder withEmail(Email email) {
            this.email = email;
            return this;
        }

        public UserBuilder withEmail(String value) {
            this.email = Email.of(value);
            return this;
        }

        public UserBuilder withRole(UserRole role) {
            this.role = role;
            return this;
        }

        public UserBuilder withCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public User build() {
            return new User(userId, name, email, role, createdAt);
        }

        // Expose builder fields for tests that need raw values
        public UserId getUserId() { return userId; }
        public String getName() { return name; }
        public Email getEmail() { return email; }
        public UserRole getRole() { return role; }
        public Instant getCreatedAt() { return createdAt; }
    }
}

package com.example.demo.user.adapter.out.persistence;

import com.example.demo.user.domain.Email;
import com.example.demo.user.domain.User;
import com.example.demo.user.domain.UserId;
import com.example.demo.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserPersistenceMapperTest {

    private UserPersistenceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(UserPersistenceMapper.class);
    }

    @Test
    void should_MapToJpaEntity_When_DomainUserProvided() {
        // Arrange
        var userId = UserId.generate();
        var email = Email.of("test@example.com");
        var name = "Test User";
        var role = UserRole.CLAIMANT;
        var createdAt = Instant.now();
        var domainUser = new User(userId, name, email, role, createdAt);

        // Act
        var jpaEntity = mapper.toJpaEntity(domainUser);

        // Assert
        assertNotNull(jpaEntity);
        assertEquals(userId.value(), jpaEntity.getId());
        assertEquals(email.value(), jpaEntity.getEmail());
        assertEquals(name, jpaEntity.getName());
        assertEquals(role, jpaEntity.getRole());
        assertEquals(createdAt, jpaEntity.getCreatedAt());
    }

    @Test
    void should_MapToDomain_When_JpaEntityProvided() {
        // Arrange
        var id = UUID.randomUUID();
        var email = "test@example.com";
        var name = "Test User";
        var role = UserRole.ADMIN;
        var createdAt = Instant.now();
        var jpaEntity = new UserJpaEntity(id, email, name, role, createdAt);

        // Act
        var domainUser = mapper.toDomain(jpaEntity);

        // Assert
        assertNotNull(domainUser);
        assertEquals(id, domainUser.getUserId().value());
        assertEquals(email, domainUser.getEmail().value());
        assertEquals(name, domainUser.getName());
        assertEquals(role, domainUser.getRole());
        assertEquals(createdAt, domainUser.getCreatedAt());
    }

    @Test
    void should_ReturnNull_When_MappingNullJpaEntityToDomain() {
        // Act
        var domainUser = mapper.toDomain(null);

        // Assert
        assertNull(domainUser);
    }

    @Test
    void should_MapUserId_When_UserIdProvided() {
        // Arrange
        var uuid = UUID.randomUUID();
        var userId = UserId.of(uuid);

        // Act
        var mapped = mapper.mapUserId(userId);

        // Assert
        assertEquals(uuid, mapped);
    }

    @Test
    void should_ReturnNull_When_MappingNullUserId() {
        // Act
        var mapped = mapper.mapUserId(null);

        // Assert
        assertNull(mapped);
    }

    @Test
    void should_MapEmail_When_EmailProvided() {
        // Arrange
        var emailValue = "test@example.com";
        var email = Email.of(emailValue);

        // Act
        var mapped = mapper.mapEmail(email);

        // Assert
        assertEquals(emailValue, mapped);
    }

    @Test
    void should_ReturnNull_When_MappingNullEmail() {
        // Act
        var mapped = mapper.mapEmail(null);

        // Assert
        assertNull(mapped);
    }

    @Test
    void should_PreserveAllFields_When_RoundTripMapping() {
        // Arrange
        var userId = UserId.generate();
        var email = Email.of("roundtrip@example.com");
        var name = "Round Trip User";
        var role = UserRole.CLAIMANT;
        var createdAt = Instant.now();
        var originalUser = new User(userId, name, email, role, createdAt);

        // Act - Round trip: Domain -> JPA -> Domain
        var jpaEntity = mapper.toJpaEntity(originalUser);
        var mappedUser = mapper.toDomain(jpaEntity);

        // Assert
        assertNotNull(mappedUser);
        assertEquals(originalUser.getUserId(), mappedUser.getUserId());
        assertEquals(originalUser.getEmail(), mappedUser.getEmail());
        assertEquals(originalUser.getName(), mappedUser.getName());
        assertEquals(originalUser.getRole(), mappedUser.getRole());
        assertEquals(originalUser.getCreatedAt(), mappedUser.getCreatedAt());
    }
}

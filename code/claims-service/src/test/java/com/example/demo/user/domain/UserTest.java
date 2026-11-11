package com.example.demo.user.domain;

import com.example.demo.fixtures.UserFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    @Test
    void should_CreateUser_When_AllFieldsValid() {
        var user = UserFactory.aUser().build();

        assertNotNull(user.getUserId());
        assertNotNull(user.getName());
        assertNotNull(user.getEmail());
        assertNotNull(user.getRole());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getDomainEvents());
    }

    @Test
    void should_ThrowException_When_EmailInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> Email.of("not-an-email"));
    }

    @Test
    void should_ThrowException_When_EmailNull() {
        assertThrows(NullPointerException.class,
                () -> Email.of(null));
    }

    @Test
    void should_ThrowException_When_EmailBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> Email.of("   "));
    }

    @Test
    void should_ThrowException_When_NameBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> UserFactory.aUser().withName("").build());
    }

    @Test
    void should_ThrowException_When_NameNull() {
        assertThrows(IllegalArgumentException.class,
                () -> UserFactory.aUser().withName(null).build());
    }

    @Test
    void should_ChangeName_When_NewNameValid() {
        var user = UserFactory.aUser().build();

        user.changeName("Jane Doe");

        assertEquals("Jane Doe", user.getName());
    }

    @Test
    void should_ThrowException_When_ChangeNameBlank() {
        var user = UserFactory.aUser().build();

        assertThrows(IllegalArgumentException.class,
                () -> user.changeName(""));
    }

    @Test
    void should_UpdateEmail_When_NewEmailValid() {
        var user = UserFactory.aUser().build();
        var newEmail = Email.of("new@example.com");

        user.updateEmail(newEmail);

        assertEquals(newEmail, user.getEmail());
    }

    @Test
    void should_BeEqual_When_SameUserId() {
        var sharedId = UserId.generate();
        var user1 = UserFactory.aUser().withUserId(sharedId).withName("Name A").withEmail("a@test.com").build();
        var user2 = UserFactory.aUser().withUserId(sharedId).withName("Name B").withEmail("b@test.com").withRole(UserRole.ADMIN).build();

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    void should_NotBeEqual_When_DifferentUserId() {
        var user1 = UserFactory.aUser().withEmail("same@test.com").build();
        var user2 = UserFactory.aUser().withEmail("same@test.com").build();

        assertNotEquals(user1, user2);
    }

    @Test
    void should_CreateUserId_When_Generated() {
        var id = UserId.generate();
        assertNotNull(id);
        assertNotNull(id.value());
    }

    @Test
    void should_ThrowException_When_UserIdNull() {
        assertThrows(NullPointerException.class,
                () -> UserId.of(null));
    }

    @Test
    void should_ReturnDisplayName_ForUserRole() {
        assertEquals("Claimant", UserRole.CLAIMANT.getDisplayName());
        assertEquals("Admin", UserRole.ADMIN.getDisplayName());
    }

    @Test
    void should_ThrowException_When_NameExceedsMaxLength() {
        var longName = "a".repeat(256); // 256 characters
        assertThrows(IllegalArgumentException.class,
                () -> UserFactory.aUser().withName(longName).build());
    }

    @Test
    void should_CreateUser_When_NameIsMaxLength() {
        var maxName = "a".repeat(255); // Exactly 255 characters
        var user = UserFactory.aUser().withName(maxName).build();
        assertEquals(maxName, user.getName());
    }

    @Test
    void should_ThrowException_When_EmailLacksTLD() {
        assertThrows(IllegalArgumentException.class,
                () -> Email.of("test@localhost"));
    }

    @Test
    void should_ThrowException_When_EmailLacksDomain() {
        assertThrows(IllegalArgumentException.class,
                () -> Email.of("test@"));
    }

    @Test
    void should_CreateEmail_When_ValidWithSubdomain() {
        var email = Email.of("test@mail.example.com");
        assertEquals("test@mail.example.com", email.value());
    }
}

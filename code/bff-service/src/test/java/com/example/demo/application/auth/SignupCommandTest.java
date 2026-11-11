package com.example.demo.application.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SignupCommandTest {

    @Test
    void should_CreateCommand_When_ValidInputs() {
        var cmd = new SignupCommand("user@example.com", "Password1!", "Test User");
        assertThat(cmd.email()).isEqualTo("user@example.com");
        assertThat(cmd.name()).isEqualTo("Test User");
    }

    @Test
    void should_Throw_When_NullEmail() {
        assertThatThrownBy(() -> new SignupCommand(null, "Password1!", "Name"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_Throw_When_BlankEmail() {
        assertThatThrownBy(() -> new SignupCommand("   ", "Password1!", "Name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void should_Throw_When_InvalidEmail() {
        assertThatThrownBy(() -> new SignupCommand("not-an-email", "Password1!", "Name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email");
    }

    @Test
    void should_Throw_When_NullPassword() {
        assertThatThrownBy(() -> new SignupCommand("user@example.com", null, "Name"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_Throw_When_BlankPassword() {
        assertThatThrownBy(() -> new SignupCommand("user@example.com", "   ", "Name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void should_Throw_When_WeakPassword() {
        assertThatThrownBy(() -> new SignupCommand("user@example.com", "short", "Name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 characters");
    }

    @Test
    void should_Throw_When_NullName() {
        assertThatThrownBy(() -> new SignupCommand("user@example.com", "Password1!", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_Throw_When_BlankName() {
        assertThatThrownBy(() -> new SignupCommand("user@example.com", "Password1!", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }
}

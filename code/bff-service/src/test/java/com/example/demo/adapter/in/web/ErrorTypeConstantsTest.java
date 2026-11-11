package com.example.demo.adapter.in.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorTypeConstantsTest {

    private final ErrorTypeConstants errorTypes = new ErrorTypeConstants("https://errors.example.com");

    @Test
    void should_ReturnUserExistsUri() {
        assertThat(errorTypes.userExists().toString()).isEqualTo("https://errors.example.com/user-exists");
    }

    @Test
    void should_ReturnAuthFailedUri() {
        assertThat(errorTypes.authFailed().toString()).isEqualTo("https://errors.example.com/auth-failed");
    }

    @Test
    void should_ReturnValidationErrorUri() {
        assertThat(errorTypes.validationError().toString()).isEqualTo("https://errors.example.com/validation-error");
    }

    @Test
    void should_ReturnAccessDeniedUri() {
        assertThat(errorTypes.accessDenied().toString()).isEqualTo("https://errors.example.com/access-denied");
    }

    @Test
    void should_ReturnUnauthorizedUri() {
        assertThat(errorTypes.unauthorized().toString()).isEqualTo("https://errors.example.com/unauthorized");
    }
}

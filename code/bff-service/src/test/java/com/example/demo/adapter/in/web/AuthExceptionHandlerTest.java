package com.example.demo.adapter.in.web;

import com.example.demo.application.auth.InvalidCredentialsException;
import com.example.demo.application.auth.UserAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionHandlerTest {

    private AuthExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AuthExceptionHandler(new ErrorTypeConstants("https://errors.example.com"));
    }

    @Test
    void should_Return409_When_UserAlreadyExists() {
        var ex = new UserAlreadyExistsException("test@example.com");
        var response = handler.handleUserAlreadyExists(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("User Already Exists");
        assertThat(response.getBody().getProperties()).containsKey("email");
    }

    @Test
    void should_Return401_When_InvalidCredentials() {
        var ex = new InvalidCredentialsException();
        var response = handler.handleInvalidCredentials(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Authentication Failed");
    }
}

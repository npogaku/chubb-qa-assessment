package com.example.demo.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityExceptionHandlerTest {

    private final ErrorTypeConstants errorTypes = mock(ErrorTypeConstants.class);
    private final SecurityExceptionHandler handler = new SecurityExceptionHandler(errorTypes);

    @Test
    void should_Return403ProblemDetail_When_AccessDenied() {
        // Arrange
        var exception = new AccessDeniedException("You can only view your own claims");
        var expectedType = URI.create("https://api.demo-app.com/errors/access-denied");
        when(errorTypes.accessDenied()).thenReturn(expectedType);

        // Act
        ResponseEntity<ProblemDetail> response = handler.handleAccessDenied(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getTitle()).isEqualTo("Access Denied");
        assertThat(response.getBody().getDetail()).isEqualTo("You can only view your own claims");
        assertThat(response.getBody().getType()).isEqualTo(expectedType);
    }

    @Test
    void should_Return401ProblemDetail_When_AuthenticationFailure() {
        // Arrange
        var exception = new AuthenticationException("Invalid token") {};
        var expectedType = URI.create("https://api.demo-app.com/errors/unauthorized");
        when(errorTypes.unauthorized()).thenReturn(expectedType);

        // Act
        ResponseEntity<ProblemDetail> response = handler.handleAuthenticationFailure(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getTitle()).isEqualTo("Unauthorized");
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
        assertThat(response.getBody().getType()).isEqualTo(expectedType);
    }
}

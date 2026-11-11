package com.example.demo.application.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupUseCaseTest {

    @Mock
    private KeycloakAdminPort keycloakAdminPort;

    @Mock
    private ClaimsServicePort claimsServicePort;

    @InjectMocks
    private SignupUseCaseImpl signupUseCase;

    @Test
    void should_CreateUser_When_ValidSignupCommand() {
        var command = new SignupCommand("test@example.com", "Password123!", "Test User");
        var keycloakUserId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        when(keycloakAdminPort.createUser("test@example.com", "Password123!", "Test User", "CLAIMANT"))
                .thenReturn(keycloakUserId);
        when(claimsServicePort.createUser(keycloakUserId, "test@example.com", "Test User", "CLAIMANT"))
                .thenReturn(new ClaimsServicePort.UserRecord(keycloakUserId, "test@example.com", "Test User", "CLAIMANT", now));

        var result = signupUseCase.signup(command);

        assertThat(result.userId()).isEqualTo(keycloakUserId);
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.name()).isEqualTo("Test User");
        assertThat(result.role()).isEqualTo("CLAIMANT");
        verify(keycloakAdminPort).createUser("test@example.com", "Password123!", "Test User", "CLAIMANT");
        verify(claimsServicePort).createUser(keycloakUserId, "test@example.com", "Test User", "CLAIMANT");
    }

    @Test
    void should_ThrowException_When_EmailAlreadyExistsInKeycloak() {
        var command = new SignupCommand("existing@example.com", "Password123!", "Existing User");

        when(keycloakAdminPort.createUser(any(), any(), any(), any()))
                .thenThrow(new UserAlreadyExistsException("existing@example.com"));

        assertThatThrownBy(() -> signupUseCase.signup(command))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("existing@example.com");
    }

    @Test
    void should_ThrowException_When_EmailAlreadyExistsInClaimsService() {
        var command = new SignupCommand("existing@example.com", "Password123!", "Existing User");
        var keycloakUserId = UUID.randomUUID();

        when(keycloakAdminPort.createUser(any(), any(), any(), any()))
                .thenReturn(keycloakUserId);
        when(claimsServicePort.createUser(any(), any(), any(), any()))
                .thenThrow(new UserAlreadyExistsException("existing@example.com"));

        assertThatThrownBy(() -> signupUseCase.signup(command))
                .isInstanceOf(UserAlreadyExistsException.class);
    }
}

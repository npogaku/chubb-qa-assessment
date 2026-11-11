package com.example.demo.application.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    @Mock
    private KeycloakTokenPort keycloakTokenPort;

    @InjectMocks
    private LogoutUseCaseImpl logoutUseCase;

    @Test
    void should_InvalidateSession_When_ValidToken() {
        logoutUseCase.logout("some-access-token");

        verify(keycloakTokenPort).invalidateSession("some-access-token");
    }

    @Test
    void should_NotThrow_When_KeycloakSessionInvalidationFails() {
        doThrow(new RuntimeException("Keycloak unavailable"))
                .when(keycloakTokenPort).invalidateSession("some-access-token");

        logoutUseCase.logout("some-access-token");

        verify(keycloakTokenPort).invalidateSession("some-access-token");
    }
}

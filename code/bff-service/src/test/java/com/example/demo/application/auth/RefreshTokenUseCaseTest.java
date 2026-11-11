package com.example.demo.application.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock
    private KeycloakTokenPort keycloakTokenPort;

    private RefreshTokenUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RefreshTokenUseCaseImpl(keycloakTokenPort);
    }

    @Test
    void should_ReturnRefreshedTokens() {
        when(keycloakTokenPort.refreshTokens("old-refresh-token"))
                .thenReturn(new KeycloakTokenPort.TokenResponse("new-access", "new-refresh", 300, 1800));

        var result = useCase.refresh("old-refresh-token");

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
    }
}

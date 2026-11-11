package com.example.demo.adapter.out.keycloak;

import com.example.demo.application.auth.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeycloakTokenAdapterTest {

    @SuppressWarnings("unchecked")
    private KeycloakTokenAdapter buildAdapter(Object tokenResponseValue) {
        var restClientBuilder = mock(RestClient.Builder.class);
        var restClient = mock(RestClient.class);
        var requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        var requestBodySpec = mock(RestClient.RequestBodySpec.class);
        var responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClientBuilder.build()).thenReturn(restClient);
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        if (tokenResponseValue instanceof Throwable throwable) {
            when(responseSpec.body(Map.class)).thenThrow((Throwable) tokenResponseValue);
        } else {
            when(responseSpec.body(Map.class)).thenReturn((Map<String, Object>) tokenResponseValue);
        }
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        return new KeycloakTokenAdapter(
                restClientBuilder,
                "http://keycloak/token",
                "http://keycloak/logout",
                "demo-app-ui"
        );
    }

    @Test
    void should_ReturnTokenResponse_When_ValidCredentials() {
        var adapter = buildAdapter(Map.of("access_token", "access-123", "refresh_token", "refresh-456",
                "expires_in", 300, "refresh_expires_in", 1800));

        var result = adapter.getTokens("user@example.com", "password");

        assertThat(result.accessToken()).isEqualTo("access-123");
        assertThat(result.refreshToken()).isEqualTo("refresh-456");
        assertThat(result.expiresIn()).isEqualTo(300);
    }

    @Test
    void should_ReturnTokenResponse_When_RefreshToken() {
        var adapter = buildAdapter(Map.of("access_token", "access-123", "refresh_token", "refresh-456",
                "expires_in", 300, "refresh_expires_in", 1800));

        var result = adapter.refreshTokens("old-refresh");

        assertThat(result.accessToken()).isEqualTo("access-123");
    }

    @Test
    void should_ThrowInvalidCredentialsException_When_HttpClientError() {
        var adapter = buildAdapter(
                HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));

        assertThatThrownBy(() -> adapter.getTokens("user@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void should_ThrowInvalidCredentialsException_When_NullResponseBody() {
        var adapter = buildAdapter(null);

        assertThatThrownBy(() -> adapter.getTokens("user@example.com", "password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void should_ThrowInvalidCredentialsException_When_ResponseMissingAccessToken() {
        // Covers: response != null but !response.containsKey("access_token") → true branch
        var adapter = buildAdapter(Map.of("token_type", "Bearer")); // no access_token key

        assertThatThrownBy(() -> adapter.getTokens("user@example.com", "password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void should_InvalidateSession_Without_Exception() {
        var adapter = buildAdapter(Map.of("access_token", "token", "refresh_token", "refresh",
                "expires_in", 300, "refresh_expires_in", 1800));

        // should not throw
        adapter.invalidateSession("some-token");
    }
}

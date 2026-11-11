package com.example.demo.adapter.out.keycloak;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceAccountTokenProviderTest {

    @SuppressWarnings("unchecked")
    private ServiceAccountTokenProviderImpl buildProvider(Map<String, Object> tokenResponse) {
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
        when(responseSpec.body(Map.class)).thenReturn(tokenResponse);

        return new ServiceAccountTokenProviderImpl(
                restClientBuilder,
                "http://keycloak/token",
                "service-client",
                "secret"
        );
    }

    @Test
    void should_ReturnToken_When_ValidResponse() {
        var provider = buildProvider(Map.of("access_token", "svc-token-123", "expires_in", 300));

        var token = provider.getServiceAccountToken();

        assertThat(token).isEqualTo("svc-token-123");
    }

    @Test
    void should_ReturnCachedToken_When_CalledTwice() {
        var provider = buildProvider(Map.of("access_token", "svc-token-123", "expires_in", 300));

        var first = provider.getServiceAccountToken();
        var second = provider.getServiceAccountToken();

        assertThat(first).isEqualTo(second).isEqualTo("svc-token-123");
    }

    @Test
    void should_ForceRefresh_And_ReturnNewToken() {
        var provider = buildProvider(Map.of("access_token", "new-token", "expires_in", 300));

        // warm up cache
        provider.getServiceAccountToken();

        var refreshed = provider.forceRefreshToken();

        assertThat(refreshed).isEqualTo("new-token");
    }

    @Test
    void should_ThrowIllegalStateException_When_NullResponse() {
        var provider = buildProvider(null);

        assertThatThrownBy(provider::getServiceAccountToken)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("service account token");
    }

    @Test
    void should_RefreshAgain_When_CachedTokenExpiresWithinBuffer() {
        // expires_in = 30s < TOKEN_REFRESH_BUFFER_SECONDS (60s)
        // Second call: now.plusSeconds(60).isBefore(now+30) → false → triggers refresh
        // Covers: isBefore(tokenExpiry) false branch
        var provider = buildProvider(Map.of("access_token", "near-expiry-token", "expires_in", 30));

        var first = provider.getServiceAccountToken();
        var second = provider.getServiceAccountToken(); // should trigger refresh again

        assertThat(first).isEqualTo("near-expiry-token");
        assertThat(second).isEqualTo("near-expiry-token");
    }

    @Test
    void should_RefreshAgain_When_CachedTokenExpiryIsNull() {
        // After a failed refresh that sets cachedToken but not tokenExpiry,
        // the next call covers: cachedToken != null && tokenExpiry != null → false branch
        var provider = buildProvider(Map.of("access_token", "token-no-expiry"));

        // First call: refreshToken() sets cachedToken but throws before setting tokenExpiry
        assertThatThrownBy(provider::getServiceAccountToken)
                .isInstanceOf(IllegalStateException.class);

        // Second call: cachedToken != null, tokenExpiry == null → false → calls refreshToken() again
        assertThatThrownBy(provider::getServiceAccountToken)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expires_in");
    }

    @Test
    void should_ThrowIllegalStateException_When_MissingExpiresIn() {
        var provider = buildProvider(Map.of("access_token", "token-no-expiry"));

        assertThatThrownBy(provider::getServiceAccountToken)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expires_in");
    }
}

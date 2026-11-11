package com.example.demo.adapter.in.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler wsHandler;

    private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtDecoder);
    }

    @Test
    void should_ReturnFalse_When_NoTokenProvided() throws Exception {
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getURI()).thenReturn(URI.create("ws://localhost/ws/claims"));

        var attributes = new HashMap<String, Object>();
        var result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
        assertThat(attributes).isEmpty();
    }

    @Test
    void should_ReturnTrue_When_ValidTokenInQueryParam() throws Exception {
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getURI()).thenReturn(URI.create("ws://localhost/ws/claims?token=valid.jwt.token"));

        var jwt = buildJwt("user-123", List.of("claimant"));
        when(jwtDecoder.decode("valid.jwt.token")).thenReturn(jwt);

        var attributes = new HashMap<String, Object>();
        var result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes).containsKey("userId");
        assertThat(attributes.get("userId")).isEqualTo("user-123");
    }

    @Test
    void should_ReturnTrue_When_ValidTokenInCookie() throws Exception {
        var headers = new HttpHeaders();
        headers.add("Cookie", "access_token=cookie.jwt.token; session_id=abc");
        when(request.getHeaders()).thenReturn(headers);

        var jwt = buildJwt("user-456", List.of("claimant"));
        when(jwtDecoder.decode("cookie.jwt.token")).thenReturn(jwt);

        var attributes = new HashMap<String, Object>();
        var result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes.get("userId")).isEqualTo("user-456");
    }

    @Test
    void should_ReturnFalse_When_InvalidToken() throws Exception {
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getURI()).thenReturn(URI.create("ws://localhost/ws/claims?token=bad.token"));
        when(jwtDecoder.decode("bad.token")).thenThrow(new JwtException("Invalid token"));

        var attributes = new HashMap<String, Object>();
        var result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isFalse();
    }

    @Test
    void should_ExtractRoles_From_RealmAccess() throws Exception {
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getURI()).thenReturn(URI.create("ws://localhost/ws/claims?token=admin.jwt.token"));

        var jwt = buildJwt("admin-789", List.of("admin", "claimant"));
        when(jwtDecoder.decode("admin.jwt.token")).thenReturn(jwt);

        var attributes = new HashMap<String, Object>();
        interceptor.beforeHandshake(request, response, wsHandler, attributes);

        @SuppressWarnings("unchecked")
        var roles = (List<String>) attributes.get("userRoles");
        assertThat(roles).contains("admin");
    }

    @Test
    void should_ReturnEmptyRoles_When_NoRealmAccess() throws Exception {
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getURI()).thenReturn(URI.create("ws://localhost/ws/claims?token=norole.jwt.token"));

        var jwt = Jwt.withTokenValue("norole.jwt.token")
                .header("alg", "RS256")
                .subject("user-000")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(jwtDecoder.decode("norole.jwt.token")).thenReturn(jwt);

        var attributes = new HashMap<String, Object>();
        interceptor.beforeHandshake(request, response, wsHandler, attributes);

        @SuppressWarnings("unchecked")
        var roles = (List<String>) attributes.get("userRoles");
        assertThat(roles).isEmpty();
    }

    @Test
    void should_DoNothing_On_AfterHandshake() {
        // afterHandshake is a no-op — just verify it doesn't throw
        interceptor.afterHandshake(request, response, wsHandler, null);
        interceptor.afterHandshake(request, response, wsHandler, new RuntimeException("test"));
    }

    @Test
    void should_PreferCookieToken_Over_QueryParam() throws Exception {
        var headers = new HttpHeaders();
        headers.add("Cookie", "access_token=cookie.token");
        when(request.getHeaders()).thenReturn(headers);

        var jwt = buildJwt("cookie-user", List.of("claimant"));
        when(jwtDecoder.decode("cookie.token")).thenReturn(jwt);

        var attributes = new HashMap<String, Object>();
        var result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes.get("userId")).isEqualTo("cookie-user");
    }

    @Test
    void should_FallbackToQueryParam_When_NoCookieHeader() throws Exception {
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getURI()).thenReturn(URI.create("ws://localhost/ws?token=query.token"));

        var jwt = buildJwt("query-user", List.of());
        when(jwtDecoder.decode("query.token")).thenReturn(jwt);

        var attributes = new HashMap<String, Object>();
        interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(attributes.get("userId")).isEqualTo("query-user");
    }

    @Test
    void should_ReturnEmptyRoles_When_RealmAccessPresentButRolesKeyMissing() throws Exception {
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getURI()).thenReturn(URI.create("ws://localhost/ws/claims?token=noroles.jwt.token"));

        // realm_access present but no "roles" key inside it
        var jwt = Jwt.withTokenValue("noroles.jwt.token")
                .header("alg", "RS256")
                .subject("user-no-roles")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("realm_access", Map.of())
                .build();
        when(jwtDecoder.decode("noroles.jwt.token")).thenReturn(jwt);

        var attributes = new HashMap<String, Object>();
        interceptor.beforeHandshake(request, response, wsHandler, attributes);

        @SuppressWarnings("unchecked")
        var roles = (List<String>) attributes.get("userRoles");
        assertThat(roles).isEmpty();
    }

    @Test
    void should_FallbackToQueryParam_When_CookieHeaderHasNoAccessToken() throws Exception {
        var headers = new HttpHeaders();
        headers.add("Cookie", "session_id=abc; other_cookie=xyz");
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create("ws://localhost/ws?token=fallback.token"));

        var jwt = buildJwt("fallback-user", List.of());
        when(jwtDecoder.decode("fallback.token")).thenReturn(jwt);

        var attributes = new HashMap<String, Object>();
        var result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes.get("userId")).isEqualTo("fallback-user");
    }

    private Jwt buildJwt(String subject, List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("realm_access", Map.of("roles", roles))
                .build();
    }
}

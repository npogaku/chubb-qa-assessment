package com.example.demo.adapter.out.security;

import com.example.demo.application.auth.NoRoleException;
import com.example.demo.application.auth.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationFacadeTest {

    private final AuthenticationFacadeImpl authenticationFacade = new AuthenticationFacadeImpl();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_ExtractUserId_When_JwtAuthenticated() {
        UUID expectedUserId = UUID.randomUUID();
        setAuthentication(expectedUserId, "ROLE_CLAIMANT");

        UUID actualUserId = authenticationFacade.getCurrentUserId();

        assertThat(actualUserId).isEqualTo(expectedUserId);
    }

    @Test
    void should_ExtractRole_When_JwtAuthenticated() {
        setAuthentication(UUID.randomUUID(), "ROLE_ADMIN");

        String role = authenticationFacade.getCurrentUserRole();

        assertThat(role).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void should_ReturnTrue_When_UserHasRole() {
        setAuthentication(UUID.randomUUID(), "ROLE_ADMIN");

        assertThat(authenticationFacade.hasRole("ADMIN")).isTrue();
        assertThat(authenticationFacade.isAdmin()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_UserDoesNotHaveRole() {
        setAuthentication(UUID.randomUUID(), "ROLE_CLAIMANT");

        assertThat(authenticationFacade.hasRole("ADMIN")).isFalse();
        assertThat(authenticationFacade.isAdmin()).isFalse();
    }

    @Test
    void should_ThrowUnauthenticatedException_When_NoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authenticationFacade.getCurrentUserId())
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");
    }

    @Test
    void should_ThrowNoRoleException_When_UserHasNoRole() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        var authentication = new JwtAuthenticationToken(jwt, List.of()); // No authorities
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> authenticationFacade.getCurrentUserRole())
                .isInstanceOf(NoRoleException.class)
                .hasMessage("No role found for authenticated user");
    }

    @Test
    void should_ReturnTokenValue_When_JwtAuthenticated() {
        setAuthentication(UUID.randomUUID(), "ROLE_CLAIMANT");

        assertThat(authenticationFacade.getJwtTokenValue()).isEqualTo("test-token");
    }

    @Test
    void should_ThrowUnauthenticatedException_When_NoAuthenticationForTokenValue() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authenticationFacade.getJwtTokenValue())
                .isInstanceOf(UnauthenticatedException.class);
    }

    private void setAuthentication(UUID userId, String role) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        var authorities = List.of(new SimpleGrantedAuthority(role));
        var authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

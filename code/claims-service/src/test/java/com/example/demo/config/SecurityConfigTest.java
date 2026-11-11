package com.example.demo.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
    }

    @Test
    void should_ExtractRolesAsAuthorities_When_RealmAccessPresent() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        Jwt jwt = buildJwtWithRoles(List.of("admin", "claimant"));

        List<String> roleAuthorities = converter.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .toList();

        assertThat(roleAuthorities).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_CLAIMANT");
    }

    @Test
    void should_ReturnNoRoleAuthorities_When_NoRealmAccess() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        Jwt jwt = buildJwtWithoutRealmAccess();

        List<String> roleAuthorities = converter.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .toList();

        assertThat(roleAuthorities).isEmpty();
    }

    @Test
    void should_ReturnNoRoleAuthorities_When_RealmAccessHasNoRoles() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        Jwt jwt = buildJwtWithRealmAccessNoRoles();

        List<String> roleAuthorities = converter.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .toList();

        assertThat(roleAuthorities).isEmpty();
    }

    @Test
    void should_UppercaseRoles_When_MappingAuthorities() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        Jwt jwt = buildJwtWithRoles(List.of("some_role"));

        List<String> roleAuthorities = converter.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .toList();

        assertThat(roleAuthorities).containsExactly("ROLE_SOME_ROLE");
    }

    private Jwt buildJwtWithRoles(List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("realm_access", Map.of("roles", roles))
                .build();
    }

    private Jwt buildJwtWithoutRealmAccess() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private Jwt buildJwtWithRealmAccessNoRoles() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("realm_access", Map.of("other_key", "value"))
                .build();
    }
}

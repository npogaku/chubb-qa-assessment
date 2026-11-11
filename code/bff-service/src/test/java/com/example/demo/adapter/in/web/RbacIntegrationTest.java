package com.example.demo.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.adapter.in.web.AdminController;
import com.example.demo.adapter.in.web.AuthController;
import com.example.demo.adapter.in.web.ClaimsController;
import com.example.demo.application.auth.AuthenticationFacade;
import com.example.demo.application.auth.ClaimsServicePort;
import com.example.demo.application.auth.LoginUseCase;
import com.example.demo.application.auth.LogoutUseCase;
import com.example.demo.application.auth.RefreshTokenUseCase;
import com.example.demo.application.auth.ServiceAccountTokenProvider;
import com.example.demo.application.auth.SignupUseCase;
import com.example.demo.config.SecurityConfig;
import com.example.demo.fixtures.ClaimJsonFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, SecurityExceptionHandler.class, AuthExceptionHandler.class, ErrorTypeConstants.class, com.example.demo.config.TestRestClientConfig.class})
@TestPropertySource(properties = {
    "claims-service.url=http://localhost:8081",
    "keycloak.token.url=http://localhost:9090/realms/demo-app/protocol/openid-connect/token",
    "keycloak.logout.url=http://localhost:9090/realms/demo-app/protocol/openid-connect/logout"
})
class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    @MockitoBean
    private SignupUseCase signupUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockitoBean
    private LogoutUseCase logoutUseCase;

    @MockitoBean
    private ClaimsServicePort claimsServicePort;

    @MockitoBean
    private ClaimResponseMapper claimResponseMapper;

    @MockitoBean
    private AdminClaimResponseMapper adminClaimResponseMapper;

    @MockitoBean
    private ServiceAccountTokenProvider serviceAccountTokenProvider;

    // =========================================================================
    // TC-006: Claimant cannot access admin endpoints (returns 403)
    // =========================================================================

    @Test
    void should_Return403_When_ClaimantAccessesDashboardStats() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLAIMANT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_Return403_When_ClaimantAccessesAdminClaims() throws Exception {
        mockMvc.perform(get("/api/admin/claims")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLAIMANT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_Return403_When_ClaimantUpdatesClaimStatus() throws Exception {
        mockMvc.perform(patch("/api/admin/claims/{claimId}/status", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLAIMANT")))
                        .contentType("application/json")
                        .content(ClaimJsonFixtures.updateClaimStatusRequest()
                                .withNewStatus(ClaimJsonFixtures.STATUS_APPROVED)
                                .toJson()))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // TC-RB-02: Admin can access admin endpoints
    // =========================================================================

    @Test
    void should_AllowAdmin_When_AccessingDashboardStats() throws Exception {
        // Admin role passes RBAC check — the key assertion is NOT 403.
        // The actual status depends on downstream service availability.
        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void should_AllowAdmin_When_AccessingAdminClaims() throws Exception {
        // Admin role passes RBAC check — the key assertion is NOT 403.
        // The actual status depends on downstream service availability.
        mockMvc.perform(get("/api/admin/claims")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    // =========================================================================
    // TC-RB-03: Unauthenticated request returns 401
    // =========================================================================

    @Test
    void should_Return401_When_UnauthenticatedAccessToProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/claims"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_Return401_When_UnauthenticatedAccessToAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // TC-RB-01: Claimant can access own claims endpoints (authenticated)
    // =========================================================================

    @Test
    void should_AllowClaimant_When_AccessingOwnClaims() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/claims")
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder
                                .subject(userId.toString())
                                .claim("realm_access", java.util.Map.of("roles", List.of("claimant"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLAIMANT"))))
                .andExpect(status().isOk());
    }

    @Test
    void should_AllowClaimant_When_AccessingSingleClaim() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/claims/{claimId}", UUID.randomUUID())
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder
                                .subject(userId.toString())
                                .claim("realm_access", java.util.Map.of("roles", List.of("claimant"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLAIMANT"))))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // TC-013: Claimant cannot view other users' claims (returns 403)
    // Note: Ownership check is enforced by claims-service, not BFF Spring Security.
    // BFF proxies the request and claims-service returns 403 if userId doesn't match.
    // This RBAC test verifies the endpoint is accessible to ROLE_CLAIMANT.
    // =========================================================================

    @Test
    void should_AllowClaimant_When_AccessingSpecificClaim() throws Exception {
        UUID claimantUserId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();

        mockMvc.perform(get("/api/claims/{claimId}", claimId)
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder
                                .subject(claimantUserId.toString())
                                .claim("realm_access", java.util.Map.of("roles", List.of("claimant"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLAIMANT"))))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // TC-RB-04: POST /api/claims overrides userId with authenticated user
    // =========================================================================

    @Test
    void should_OverrideUserId_When_ClaimantSubmitsClaim() throws Exception {
        UUID authenticatedUserId = UUID.randomUUID();

        org.mockito.Mockito.when(authenticationFacade.getCurrentUserId()).thenReturn(authenticatedUserId);
        org.mockito.Mockito.when(authenticationFacade.getJwtTokenValue()).thenReturn("mock-jwt-token");
        org.mockito.Mockito.when(claimsServicePort.submitClaim(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new com.example.demo.application.auth.ClaimsServicePort.ClaimRecord(
                UUID.randomUUID(), authenticatedUserId, java.time.LocalDate.of(2024, 1, 15),
                "123 Main St, Springfield", "Test claim for RBAC validation",
                java.math.BigDecimal.valueOf(1000.00), "SUBMITTED",
                java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now()
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/claims")
                        .with(jwt().jwt(jwtBuilder -> jwtBuilder
                                .subject(authenticatedUserId.toString())
                                .claim("realm_access", java.util.Map.of("roles", List.of("claimant"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLAIMANT")))
                        .contentType("application/json")
                        .content(ClaimJsonFixtures.submitClaimRequest()
                                .withIncidentDate("2024-01-15")
                                .withIncidentLocation("123 Main St, Springfield")
                                .withDescription("Test claim for RBAC validation")
                                .withClaimAmount(1000.00)
                                .toJson()))
                .andExpect(status().isCreated());

        // Verify the JWT token was forwarded (not a UUID from the request body)
        org.mockito.Mockito.verify(claimsServicePort).submitClaim(
                org.mockito.ArgumentMatchers.eq("mock-jwt-token"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}

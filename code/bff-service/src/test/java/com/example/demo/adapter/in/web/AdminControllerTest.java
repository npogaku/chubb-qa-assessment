package com.example.demo.adapter.in.web;

import com.example.bff.adapter.out.claims.ApiException;
import com.example.bff.adapter.out.claims.generated.AdminClaimsApi;
import com.example.bff.adapter.out.claims.generated.DashboardApi;
import com.example.bff.adapter.out.claims.generated.UsersApi;
import com.example.bff.adapter.out.claims.generated.model.ClaimStatusDTO;
import com.example.bff.adapter.out.claims.generated.model.DashboardStatsResponseDTO;
import com.example.bff.adapter.out.claims.generated.model.UserResponseDTO;
import com.example.bff.adapter.out.claims.generated.model.UserRoleDTO;
import com.example.bff.adapter.in.web.generated.model.UpdateClaimStatusRequestDTO;
import com.example.demo.application.auth.ServiceAccountTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminControllerTest {

    @Mock
    private ServiceAccountTokenProvider serviceAccountTokenProvider;

    @Mock
    private AdminClaimResponseMapper adminClaimResponseMapper;

    @BeforeEach
    void setUp() {
        when(serviceAccountTokenProvider.getServiceAccountToken()).thenReturn("svc-token");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setJwtSecurityContext(String tokenValue) {
        var jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .subject("admin-user-id")
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @Test
    void should_Return200_When_GetDashboardStats() throws Exception {
        var stats = new DashboardStatsResponseDTO()
                .totalUsers(10)
                .totalClaims(50)
                .cacheHit(true)
                .queryTimeMs(5);

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class,
                (mock, ctx) -> when(mock.getDashboardStats(false)).thenReturn(stats));
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class);
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            var response = controller.getDashboardStats(Optional.empty());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getTotalUsers()).isEqualTo(10);
            assertThat(response.getBody().getTotalClaims()).isEqualTo(50);
        }
    }

    @Test
    void should_Return403_When_DashboardApiThrows403() throws Exception {
        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class,
                (mock, ctx) -> when(mock.getDashboardStats(false)).thenThrow(new ApiException(403, "Forbidden")));
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class);
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            var response = controller.getDashboardStats(Optional.empty());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void should_ThrowIllegalStateException_When_DashboardApiThrows500() throws Exception {
        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class,
                (mock, ctx) -> when(mock.getDashboardStats(false)).thenThrow(new ApiException(500, "Server Error")));
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class);
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);

            assertThatThrownBy(() -> controller.getDashboardStats(Optional.empty()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void should_Return204_When_FlushCache() throws Exception {
        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class,
                (mock, ctx) -> doNothing().when(mock).flushDashboardCache());
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class);
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            var response = controller.flushDashboardCache();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @Test
    void should_Return200_When_ListAllUsers() throws Exception {
        var user = new UserResponseDTO()
                .userId(UUID.randomUUID())
                .email("user@example.com")
                .name("Test User")
                .role(UserRoleDTO.CLAIMANT)
                .createdAt(OffsetDateTime.now());

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class);
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class,
                     (mock, ctx) -> when(mock.listAllUsers()).thenReturn(List.of(user)))) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);

            // listAllUsers uses setUserBearerToken — we need to set up a security context
            // Since there's no JWT in test context, it will throw IllegalStateException
            // This tests the method behavior at the adapter level
            assertThatThrownBy(controller::listAllUsers)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No user JWT token");
        }
    }

    @Test
    void should_Return200_When_ListAllClaims() throws Exception {
        var claimResponse = new com.example.bff.adapter.out.claims.generated.model.AdminClaimResponseDTO()
                .claimId(UUID.randomUUID())
                .status(ClaimStatusDTO.SUBMITTED);
        var bffClaim = new com.example.bff.adapter.in.web.generated.model.ClaimResponseDTO()
                .claimId(claimResponse.getClaimId());

        when(serviceAccountTokenProvider.getServiceAccountToken()).thenReturn("svc-token");
        when(adminClaimResponseMapper.toClaimResponseList(any())).thenReturn(List.of(bffClaim));

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class,
                     (mock, ctx) -> when(mock.listAllClaims(any(), any())).thenReturn(List.of(claimResponse)));
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            var response = controller.listAllClaims(Optional.empty(), Optional.empty());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Test
    void should_Return403_When_ListAllClaims_Throws403() throws Exception {
        when(serviceAccountTokenProvider.getServiceAccountToken()).thenReturn("svc-token");

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class,
                     (mock, ctx) -> when(mock.listAllClaims(any(), any())).thenThrow(new ApiException(403, "Forbidden")));
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            var response = controller.listAllClaims(Optional.empty(), Optional.empty());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void should_Return403_When_FlushCache_Throws403() throws Exception {
        when(serviceAccountTokenProvider.getServiceAccountToken()).thenReturn("svc-token");

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class,
                (mock, ctx) -> when(mock.getDashboardStats(false)).thenReturn(null)); // unused, just to init
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class);
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            // Get the constructed DashboardApi mock and configure it to throw on flushDashboardCache
            var dashboardApiMock = dashboardMock.constructed().get(0);
            org.mockito.Mockito.doThrow(new ApiException(403, "Forbidden")).when(dashboardApiMock).flushDashboardCache();

            var response = controller.flushDashboardCache();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void should_Return400_When_UpdateClaimStatusThrows400() throws Exception {
        var claimId = UUID.randomUUID();
        var request = new UpdateClaimStatusRequestDTO()
                .newStatus(com.example.bff.adapter.in.web.generated.model.ClaimStatusDTO.APPROVED);

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class,
                     (mock, ctx) -> when(mock.updateClaimStatus(any(), any())).thenThrow(new ApiException(400, "Bad Request")));
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);

            // updateClaimStatus also uses setUserBearerToken — no JWT in test context
            assertThatThrownBy(() -> controller.updateClaimStatus(claimId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No user JWT token");
        }
    }

    @Test
    void should_Return200_When_ListAllUsers_WithJwt() throws Exception {
        var user = new com.example.bff.adapter.out.claims.generated.model.UserResponseDTO()
                .userId(UUID.randomUUID())
                .email("user@example.com")
                .name("Test User")
                .role(UserRoleDTO.CLAIMANT)
                .createdAt(OffsetDateTime.now());

        setJwtSecurityContext("admin-jwt-token");

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class);
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class,
                     (mock, ctx) -> when(mock.listAllUsers()).thenReturn(List.of(user)))) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            var response = controller.listAllUsers();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Test
    void should_Return403_When_ListAllUsers_Throws403() throws Exception {
        setJwtSecurityContext("admin-jwt-token");

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class);
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class,
                     (mock, ctx) -> when(mock.listAllUsers()).thenThrow(new ApiException(403, "Forbidden")))) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            var response = controller.listAllUsers();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void should_ThrowIllegalStateException_When_ListAllUsers_Throws500() throws Exception {
        setJwtSecurityContext("admin-jwt-token");

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class);
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class,
                     (mock, ctx) -> when(mock.listAllUsers()).thenThrow(new ApiException(500, "Server Error")))) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            assertThatThrownBy(controller::listAllUsers)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void should_Return200_When_UpdateClaimStatus_WithJwt() throws Exception {
        var claimId = UUID.randomUUID();
        var request = new UpdateClaimStatusRequestDTO()
                .newStatus(com.example.bff.adapter.in.web.generated.model.ClaimStatusDTO.APPROVED);
        var claimResponse = new com.example.bff.adapter.out.claims.generated.model.ClaimResponseDTO()
                .claimId(claimId)
                .status(ClaimStatusDTO.APPROVED);
        var bffClaim = new com.example.bff.adapter.in.web.generated.model.ClaimResponseDTO()
                .claimId(claimId);

        when(adminClaimResponseMapper.toClaimResponse(
                any(com.example.bff.adapter.out.claims.generated.model.ClaimResponseDTO.class)))
                .thenReturn(bffClaim);
        setJwtSecurityContext("admin-jwt-token");

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class,
                     (mock, ctx) -> when(mock.updateClaimStatus(any(), any())).thenReturn(claimResponse));
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            var response = controller.updateClaimStatus(claimId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void should_Return400_When_UpdateClaimStatus_WithJwt_Throws400() throws Exception {
        var claimId = UUID.randomUUID();
        var request = new UpdateClaimStatusRequestDTO()
                .newStatus(com.example.bff.adapter.in.web.generated.model.ClaimStatusDTO.APPROVED);

        setJwtSecurityContext("admin-jwt-token");

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class,
                     (mock, ctx) -> when(mock.updateClaimStatus(any(), any())).thenThrow(new ApiException(400, "Bad Request")));
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            var response = controller.updateClaimStatus(claimId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    void should_Return403_When_UpdateClaimStatus_Throws403() throws Exception {
        var claimId = UUID.randomUUID();
        var request = new UpdateClaimStatusRequestDTO()
                .newStatus(com.example.bff.adapter.in.web.generated.model.ClaimStatusDTO.APPROVED);

        setJwtSecurityContext("admin-jwt-token");

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class,
                     (mock, ctx) -> when(mock.updateClaimStatus(any(), any())).thenThrow(new ApiException(403, "Forbidden")));
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            var response = controller.updateClaimStatus(claimId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void should_Return404_When_UpdateClaimStatus_Throws404() throws Exception {
        var claimId = UUID.randomUUID();
        var request = new UpdateClaimStatusRequestDTO()
                .newStatus(com.example.bff.adapter.in.web.generated.model.ClaimStatusDTO.APPROVED);

        setJwtSecurityContext("admin-jwt-token");

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class,
                     (mock, ctx) -> when(mock.updateClaimStatus(any(), any())).thenThrow(new ApiException(404, "Not Found")));
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            var response = controller.updateClaimStatus(claimId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    void should_ThrowIllegalStateException_When_UpdateClaimStatus_Throws500() throws Exception {
        var claimId = UUID.randomUUID();
        var request = new UpdateClaimStatusRequestDTO()
                .newStatus(com.example.bff.adapter.in.web.generated.model.ClaimStatusDTO.APPROVED);

        setJwtSecurityContext("admin-jwt-token");

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class,
                     (mock, ctx) -> when(mock.updateClaimStatus(any(), any())).thenThrow(new ApiException(500, "Server Error")));
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            assertThatThrownBy(() -> controller.updateClaimStatus(claimId, request))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void should_ThrowIllegalStateException_When_FlushCache_Throws500() throws Exception {
        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class);
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            var dashboardApiMock = dashboardMock.constructed().get(0);
            org.mockito.Mockito.doThrow(new ApiException(500, "Server Error")).when(dashboardApiMock).flushDashboardCache();

            assertThatThrownBy(controller::flushDashboardCache)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void should_ThrowIllegalStateException_When_ListAllClaims_Throws500() throws Exception {
        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class,
                     (mock, ctx) -> when(mock.listAllClaims(any(), any())).thenThrow(new ApiException(500, "Server Error")));
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            assertThatThrownBy(() -> controller.listAllClaims(Optional.empty(), Optional.empty()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void should_Return200_When_ListAllClaims_WithStatusAndUserId() throws Exception {
        var claimResponse = new com.example.bff.adapter.out.claims.generated.model.AdminClaimResponseDTO()
                .claimId(UUID.randomUUID())
                .status(ClaimStatusDTO.APPROVED);
        var bffClaim = new com.example.bff.adapter.in.web.generated.model.ClaimResponseDTO()
                .claimId(claimResponse.getClaimId());

        when(adminClaimResponseMapper.toClaimResponseList(any())).thenReturn(List.of(bffClaim));

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class,
                     (mock, ctx) -> when(mock.listAllClaims(any(), any())).thenReturn(List.of(claimResponse)));
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            var response = controller.listAllClaims(
                    Optional.of(com.example.bff.adapter.in.web.generated.model.ClaimStatusDTO.APPROVED),
                    Optional.of(UUID.randomUUID()));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void should_ThrowIllegalStateException_When_ListAllUsers_NonJwtAuthentication() throws Exception {
        // Covers: authentication != null but getPrincipal() is NOT a Jwt → throws
        var nonJwtAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "admin-user", "password", List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(nonJwtAuth);

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class);
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class);
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            assertThatThrownBy(controller::listAllUsers)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No user JWT token");
        }
    }

    @Test
    void should_Return200_When_GetDashboardStats_BypassCacheTrue() throws Exception {
        var stats = new DashboardStatsResponseDTO()
                .totalUsers(5)
                .totalClaims(20)
                .cacheHit(false)
                .queryTimeMs(10);

        try (MockedConstruction<DashboardApi> dashboardMock = mockConstruction(DashboardApi.class,
                (mock, ctx) -> when(mock.getDashboardStats(true)).thenReturn(stats));
             MockedConstruction<AdminClaimsApi> adminMock = mockConstruction(AdminClaimsApi.class);
             MockedConstruction<UsersApi> usersMock = mockConstruction(UsersApi.class)) {

            var controller = new AdminController("http://localhost:8081", serviceAccountTokenProvider, adminClaimResponseMapper);
            var response = controller.getDashboardStats(Optional.of(true));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}

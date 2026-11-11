package com.example.demo.adapter.out.claimsservice;

import com.example.bff.adapter.out.claims.ApiClient;
import com.example.bff.adapter.out.claims.ApiException;
import com.example.bff.adapter.out.claims.generated.ClaimsApi;
import com.example.bff.adapter.out.claims.generated.UsersApi;
import com.example.bff.adapter.out.claims.generated.model.ClaimResponseDTO;
import com.example.bff.adapter.out.claims.generated.model.ClaimStatusDTO;
import com.example.bff.adapter.out.claims.generated.model.ClaimSummaryResponseDTO;
import com.example.bff.adapter.out.claims.generated.model.UserResponseDTO;
import com.example.bff.adapter.out.claims.generated.model.UserRoleDTO;
import com.example.demo.application.auth.ServiceAccountTokenProvider;
import com.example.demo.application.auth.UserAlreadyExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaimsServiceAdapterTest {

    @Mock
    private ApiClient apiClient;

    @Mock
    private ServiceAccountTokenProvider serviceAccountTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    private ClaimsServiceAdapter adapter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CLAIM_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(serviceAccountTokenProvider.getServiceAccountToken()).thenReturn("service-token");
        when(apiClient.getBasePath()).thenReturn("http://claims-service");
    }

    @Test
    void should_CreateUser_Successfully() throws Exception {
        var userResponse = new UserResponseDTO()
                .userId(USER_ID)
                .email("test@example.com")
                .name("Test User")
                .role(UserRoleDTO.CLAIMANT)
                .createdAt(OffsetDateTime.now());

        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class,
                (mock, ctx) -> when(mock.createUser(any())).thenReturn(userResponse));
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class)) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);

            var result = adapter.createUser(USER_ID, "test@example.com", "Test User", "CLAIMANT");

            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.email()).isEqualTo("test@example.com");
            assertThat(result.role()).isEqualTo("CLAIMANT");
        }
    }

    @Test
    void should_ThrowUserAlreadyExistsException_When_409OnCreateUser() throws Exception {
        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class,
                (mock, ctx) -> when(mock.createUser(any())).thenThrow(new ApiException(409, "Conflict")));
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class)) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);

            assertThatThrownBy(() -> adapter.createUser(USER_ID, "test@example.com", "Test User", "CLAIMANT"))
                    .isInstanceOf(UserAlreadyExistsException.class);
        }
    }

    @Test
    void should_ThrowIllegalStateException_When_OtherApiExceptionOnCreateUser() throws Exception {
        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class,
                (mock, ctx) -> when(mock.createUser(any())).thenThrow(new ApiException(500, "Server Error")));
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class)) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);

            assertThatThrownBy(() -> adapter.createUser(USER_ID, "test@example.com", "Test User", "CLAIMANT"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void should_RetryWithFreshToken_When_401OnCreateUser() throws Exception {
        var userResponse = new UserResponseDTO()
                .userId(USER_ID)
                .email("test@example.com")
                .name("Test User")
                .role(UserRoleDTO.CLAIMANT)
                .createdAt(OffsetDateTime.now());

        when(serviceAccountTokenProvider.forceRefreshToken()).thenReturn("fresh-token");

        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class,
                (mock, ctx) -> {
                    when(mock.createUser(any()))
                            .thenThrow(new ApiException(401, "Unauthorized"))
                            .thenReturn(userResponse);
                });
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class)) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);

            var result = adapter.createUser(USER_ID, "test@example.com", "Test User", "CLAIMANT");

            assertThat(result.userId()).isEqualTo(USER_ID);
            verify(serviceAccountTokenProvider).forceRefreshToken();
        }
    }

    @Test
    void should_SubmitClaim_Successfully() throws Exception {
        var claimResponse = new ClaimResponseDTO()
                .claimId(CLAIM_ID)
                .userId(USER_ID)
                .incidentDate(LocalDate.of(2024, 1, 15))
                .incidentLocation("London")
                .description("Car accident")
                .claimAmount(1500.0)
                .status(ClaimStatusDTO.SUBMITTED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now());

        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class);
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class,
                     (mock, ctx) -> when(mock.createClaim(any())).thenReturn(claimResponse))) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);

            var result = adapter.submitClaim("user-jwt", LocalDate.of(2024, 1, 15), "London", "Car accident", BigDecimal.valueOf(1500.0));

            assertThat(result.claimId()).isEqualTo(CLAIM_ID);
            assertThat(result.status()).isEqualTo("SUBMITTED");
        }
    }

    @Test
    void should_ListClaims_Successfully() throws Exception {
        var summary = new ClaimSummaryResponseDTO()
                .claimId(CLAIM_ID)
                .incidentDate(LocalDate.of(2024, 1, 15))
                .description("desc")
                .claimAmount(100.0)
                .status(ClaimStatusDTO.SUBMITTED)
                .createdAt(OffsetDateTime.now());

        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class);
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class,
                     (mock, ctx) -> when(mock.listClaims()).thenReturn(List.of(summary)))) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);

            var results = adapter.listClaims("user-jwt", USER_ID);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).claimId()).isEqualTo(CLAIM_ID);
        }
    }

    @Test
    void should_GetClaim_Successfully() throws Exception {
        var claimResponse = new ClaimResponseDTO()
                .claimId(CLAIM_ID)
                .userId(USER_ID)
                .incidentDate(LocalDate.of(2024, 1, 15))
                .incidentLocation("London")
                .description("desc")
                .claimAmount(500.0)
                .status(ClaimStatusDTO.APPROVED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now());

        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class);
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class,
                     (mock, ctx) -> when(mock.getClaim(CLAIM_ID)).thenReturn(claimResponse))) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);

            var result = adapter.getClaim("user-jwt", CLAIM_ID);

            assertThat(result.claimId()).isEqualTo(CLAIM_ID);
            assertThat(result.status()).isEqualTo("APPROVED");
        }
    }

    @Test
    void should_ThrowIllegalStateException_When_403OnGetClaim() throws Exception {
        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class);
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class,
                     (mock, ctx) -> when(mock.getClaim(any())).thenThrow(new ApiException(403, "Forbidden")))) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);

            assertThatThrownBy(() -> adapter.getClaim("user-jwt", CLAIM_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Forbidden");
        }
    }

    @Test
    void should_ThrowIllegalStateException_When_404OnGetClaim() throws Exception {
        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class);
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class,
                     (mock, ctx) -> when(mock.getClaim(any())).thenThrow(new ApiException(404, "Not Found")))) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);

            assertThatThrownBy(() -> adapter.getClaim("user-jwt", CLAIM_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Test
    void should_RetryGetUser_When_401() throws Exception {
        var userResponse = new UserResponseDTO()
                .userId(USER_ID)
                .email("test@example.com")
                .name("Test User")
                .role(UserRoleDTO.CLAIMANT)
                .createdAt(OffsetDateTime.now());

        when(serviceAccountTokenProvider.forceRefreshToken()).thenReturn("fresh-token");

        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class,
                (mock, ctx) -> when(mock.getUser(USER_ID))
                        .thenThrow(new ApiException(401, "Unauthorized"))
                        .thenReturn(userResponse));
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class)) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);
            var result = adapter.getUser(USER_ID);

            assertThat(result.userId()).isEqualTo(USER_ID);
            verify(serviceAccountTokenProvider).forceRefreshToken();
        }
    }

    @Test
    void should_ThrowIllegalStateException_When_GetUserFails() throws Exception {
        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class,
                (mock, ctx) -> when(mock.getUser(USER_ID)).thenThrow(new ApiException(500, "Error")));
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class)) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);
            assertThatThrownBy(() -> adapter.getUser(USER_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void should_ThrowIllegalStateException_When_SubmitClaimFails() throws Exception {
        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class);
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class,
                     (mock, ctx) -> when(mock.createClaim(any())).thenThrow(new ApiException(400, "Bad Request")))) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);
            assertThatThrownBy(() -> adapter.submitClaim("jwt", LocalDate.now(), "London", "desc", BigDecimal.ONE))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void should_ThrowIllegalStateException_When_ListClaimsFails() throws Exception {
        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class);
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class,
                     (mock, ctx) -> when(mock.listClaims()).thenThrow(new ApiException(500, "Error")))) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);
            assertThatThrownBy(() -> adapter.listClaims("jwt", USER_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void should_ThrowIllegalStateException_When_500OnGetClaim() throws Exception {
        // Covers: if (code == 404) false branch in getClaim
        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class);
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class,
                     (mock, ctx) -> when(mock.getClaim(any())).thenThrow(new ApiException(500, "Server Error")))) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);

            assertThatThrownBy(() -> adapter.getClaim("user-jwt", CLAIM_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Claims Service call failed");
        }
    }

    @Test
    void should_MapUserRecord_When_CreatedAtIsNull() throws Exception {
        // Covers: response.getCreatedAt() != null ternary false branch
        var userResponse = new UserResponseDTO()
                .userId(USER_ID)
                .email("test@example.com")
                .name("Test User")
                .role(com.example.bff.adapter.out.claims.generated.model.UserRoleDTO.CLAIMANT);
        // createdAt is deliberately not set (null)

        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class,
                (mock, ctx) -> when(mock.getUser(USER_ID)).thenReturn(userResponse));
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class)) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);
            var result = adapter.getUser(USER_ID);

            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.createdAt()).isNotNull(); // falls back to OffsetDateTime.now()
        }
    }

    @Test
    void should_GetUser_Successfully() throws Exception {
        var userResponse = new UserResponseDTO()
                .userId(USER_ID)
                .email("test@example.com")
                .name("Test User")
                .role(UserRoleDTO.CLAIMANT)
                .createdAt(OffsetDateTime.now());

        try (MockedConstruction<UsersApi> usersApiMock = mockConstruction(UsersApi.class,
                (mock, ctx) -> when(mock.getUser(USER_ID)).thenReturn(userResponse));
             MockedConstruction<ClaimsApi> claimsApiMock = mockConstruction(ClaimsApi.class)) {

            adapter = new ClaimsServiceAdapter(apiClient, serviceAccountTokenProvider, objectMapper);

            var result = adapter.getUser(USER_ID);

            assertThat(result.userId()).isEqualTo(USER_ID);
        }
    }
}

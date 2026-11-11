package com.example.demo.application.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private KeycloakTokenPort keycloakTokenPort;

    @Mock
    private ClaimsServicePort claimsServicePort;

    private LoginUseCaseImpl loginUseCase;

    @BeforeEach
    void setUp() {
        loginUseCase = new LoginUseCaseImpl(keycloakTokenPort, claimsServicePort, new ObjectMapper());
    }

    @Test
    void should_ReturnLoginResult_When_ValidCredentials() {
        var command = new LoginCommand("test@example.com", "Password123!");
        var userId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        var fakeToken = createFakeJwt(userId.toString());
        var tokenResponse = new KeycloakTokenPort.TokenResponse(fakeToken, "fake-refresh-token", 300, 1800);

        when(keycloakTokenPort.getTokens("test@example.com", "Password123!"))
                .thenReturn(tokenResponse);
        when(claimsServicePort.getUser(userId))
                .thenReturn(new ClaimsServicePort.UserRecord(userId, "test@example.com", "Test User", "CLAIMANT", now));

        var result = loginUseCase.login(command);

        assertThat(result.accessToken()).isEqualTo(fakeToken);
        assertThat(result.refreshToken()).isEqualTo("fake-refresh-token");
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.name()).isEqualTo("Test User");
        assertThat(result.role()).isEqualTo("CLAIMANT");
    }

    @Test
    void should_ThrowException_When_InvalidCredentials() {
        var command = new LoginCommand("test@example.com", "wrongpassword");

        when(keycloakTokenPort.getTokens(any(), any()))
                .thenThrow(new InvalidCredentialsException());

        assertThatThrownBy(() -> loginUseCase.login(command))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void should_ThrowException_When_UserNotFoundInClaimsService() {
        var command = new LoginCommand("test@example.com", "Password123!");
        var userId = UUID.randomUUID();

        var fakeToken = createFakeJwt(userId.toString());
        var tokenResponse = new KeycloakTokenPort.TokenResponse(fakeToken, "fake-refresh-token", 300, 1800);

        when(keycloakTokenPort.getTokens("test@example.com", "Password123!"))
                .thenReturn(tokenResponse);
        when(claimsServicePort.getUser(userId))
                .thenThrow(new RuntimeException("User not found"));

        assertThatThrownBy(() -> loginUseCase.login(command))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void should_AutoProvisionUser_When_UserNotFoundInClaimsService() {
        var command = new LoginCommand("new@example.com", "Password123!");
        var userId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        var fakeToken = createFakeJwt(userId.toString());
        var tokenResponse = new KeycloakTokenPort.TokenResponse(fakeToken, "fake-refresh-token", 300, 1800);

        when(keycloakTokenPort.getTokens("new@example.com", "Password123!")).thenReturn(tokenResponse);
        when(claimsServicePort.getUser(userId)).thenThrow(new IllegalStateException("not found"));
        when(claimsServicePort.createUser(any(), any(), any(), any()))
                .thenReturn(new ClaimsServicePort.UserRecord(userId, "new@example.com", "New User", "CLAIMANT", now));

        var result = loginUseCase.login(command);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.role()).isEqualTo("CLAIMANT");
    }

    @Test
    void should_ExtractAdminRole_When_TokenContainsAdminRole() {
        var command = new LoginCommand("admin@example.com", "Password123!");
        var userId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        var fakeToken = createFakeJwtWithRole(userId.toString(), "admin");
        var tokenResponse = new KeycloakTokenPort.TokenResponse(fakeToken, "fake-refresh-token", 300, 1800);

        when(keycloakTokenPort.getTokens("admin@example.com", "Password123!")).thenReturn(tokenResponse);
        when(claimsServicePort.getUser(userId))
                .thenReturn(new ClaimsServicePort.UserRecord(userId, "admin@example.com", "Admin User", "ADMIN", now));

        var result = loginUseCase.login(command);

        assertThat(result.role()).isEqualTo("ADMIN");
    }

    @Test
    void should_DefaultToClaimant_When_NoRealmAccessInToken() {
        var command = new LoginCommand("user@example.com", "Password123!");
        var userId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        var fakeToken = createFakeJwtNoRealm(userId.toString());
        var tokenResponse = new KeycloakTokenPort.TokenResponse(fakeToken, "fake-refresh-token", 300, 1800);

        when(keycloakTokenPort.getTokens("user@example.com", "Password123!")).thenReturn(tokenResponse);
        when(claimsServicePort.getUser(userId))
                .thenReturn(new ClaimsServicePort.UserRecord(userId, "user@example.com", "User", "CLAIMANT", now));

        var result = loginUseCase.login(command);

        assertThat(result.userId()).isEqualTo(userId);
    }

    @Test
    void should_ExtractClaimantRole_When_TokenContainsClaimantRole() {
        var command = new LoginCommand("user@example.com", "Password123!");
        var userId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        var fakeToken = createFakeJwtWithRole(userId.toString(), "claimant");
        var tokenResponse = new KeycloakTokenPort.TokenResponse(fakeToken, "fake-refresh-token", 300, 1800);

        when(keycloakTokenPort.getTokens("user@example.com", "Password123!")).thenReturn(tokenResponse);
        when(claimsServicePort.getUser(userId))
                .thenReturn(new ClaimsServicePort.UserRecord(userId, "user@example.com", "User", "CLAIMANT", now));

        var result = loginUseCase.login(command);

        assertThat(result.role()).isEqualTo("CLAIMANT");
    }

    @Test
    void should_DefaultToClaimant_When_RealmAccessRolesIsNull() {
        var command = new LoginCommand("user@example.com", "Password123!");
        var userId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        // realm_access present but no "roles" key
        var fakeToken = createFakeJwtWithEmptyRealmAccess(userId.toString());
        var tokenResponse = new KeycloakTokenPort.TokenResponse(fakeToken, "fake-refresh-token", 300, 1800);

        when(keycloakTokenPort.getTokens("user@example.com", "Password123!")).thenReturn(tokenResponse);
        when(claimsServicePort.getUser(userId))
                .thenReturn(new ClaimsServicePort.UserRecord(userId, "user@example.com", "User", "CLAIMANT", now));

        var result = loginUseCase.login(command);

        assertThat(result.role()).isEqualTo("CLAIMANT");
    }

    @Test
    void should_DefaultToClaimant_When_RealmAccessHasUnknownRoles() {
        var command = new LoginCommand("user@example.com", "Password123!");
        var userId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        var fakeToken = createFakeJwtWithRole(userId.toString(), "unknown-role");
        var tokenResponse = new KeycloakTokenPort.TokenResponse(fakeToken, "fake-refresh-token", 300, 1800);

        when(keycloakTokenPort.getTokens("user@example.com", "Password123!")).thenReturn(tokenResponse);
        when(claimsServicePort.getUser(userId))
                .thenReturn(new ClaimsServicePort.UserRecord(userId, "user@example.com", "User", "CLAIMANT", now));

        var result = loginUseCase.login(command);

        assertThat(result.role()).isEqualTo("CLAIMANT");
    }

    private String createFakeJwtWithEmptyRealmAccess(String subject) {
        var header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
        var payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + subject + "\",\"email\":\"user@example.com\",\"name\":\"User\","
                        + "\"realm_access\":{}}").getBytes());
        var signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("fake-signature".getBytes());
        return header + "." + payload + "." + signature;
    }

    private String createFakeJwtWithRole(String subject, String role) {
        var header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
        var payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + subject + "\",\"email\":\"admin@example.com\",\"name\":\"Admin\","
                        + "\"realm_access\":{\"roles\":[\"" + role + "\"]}}").getBytes());
        var signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("fake-signature".getBytes());
        return header + "." + payload + "." + signature;
    }

    private String createFakeJwtNoRealm(String subject) {
        var header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
        var payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + subject + "\",\"email\":\"user@example.com\",\"name\":\"User\"}").getBytes());
        var signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("fake-signature".getBytes());
        return header + "." + payload + "." + signature;
    }

    private String createFakeJwt(String subject) {
        var header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
        var payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + subject + "\",\"email\":\"test@example.com\"}").getBytes());
        var signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("fake-signature".getBytes());
        return header + "." + payload + "." + signature;
    }
}

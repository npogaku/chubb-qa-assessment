package com.example.demo.adapter.in.web;

import com.example.demo.application.auth.InvalidCredentialsException;
import com.example.demo.application.auth.LoginCommand;
import com.example.demo.application.auth.LoginResult;
import com.example.demo.application.auth.LoginUseCase;
import com.example.demo.application.auth.LogoutUseCase;
import com.example.demo.application.auth.RefreshTokenUseCase;
import com.example.demo.application.auth.SignupResult;
import com.example.demo.application.auth.SignupUseCase;
import com.example.demo.application.auth.UserAlreadyExistsException;
import com.example.demo.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.demo.application.auth.InvalidCredentialsException;
import com.example.demo.application.auth.RefreshTokenResult;
import jakarta.servlet.http.Cookie;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthExceptionHandler.class, ErrorTypeConstants.class, com.example.demo.config.TestRestClientConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SignupUseCase signupUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockitoBean
    private LogoutUseCase logoutUseCase;

    @Test
    void should_Return201_When_ValidSignupRequest() throws Exception {
        var userId = UUID.randomUUID();
        var now = OffsetDateTime.now();
        var signupResult = new SignupResult(userId, "test@example.com", "Test User", "CLAIMANT", now);
        when(signupUseCase.signup(any())).thenReturn(signupResult);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "Password123!",
                                    "name": "Test User"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.role").value("CLAIMANT"));
    }

    @Test
    void should_Return400_When_InvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "not-an-email",
                                    "password": "Password123!",
                                    "name": "Test User"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_Return400_When_PasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "short",
                                    "name": "Test User"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_Return409_When_EmailAlreadyExists() throws Exception {
        when(signupUseCase.signup(any()))
                .thenThrow(new UserAlreadyExistsException("test@example.com"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "Password123!",
                                    "name": "Test User"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("User Already Exists"));
    }

    @Test
    void should_Return200_When_ValidLoginRequest() throws Exception {
        var userId = UUID.randomUUID();
        var now = OffsetDateTime.now();
        var loginResult = new LoginResult("fake-access-token", "fake-refresh-token", userId, "test@example.com", "Test User", "CLAIMANT", now);
        when(loginUseCase.login(any())).thenReturn(loginResult);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void should_Return401_When_InvalidCredentials() throws Exception {
        when(loginUseCase.login(any()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "wrongpassword"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication Failed"));
    }

    @Test
    @WithMockUser
    void should_Return204_When_Logout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void should_Return204_When_Logout_WithOtherCookiesPresent() throws Exception {
        // Covers: getCookies() != null false branch in extractAccessTokenFromCookies
        mockMvc.perform(post("/api/auth/logout")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .cookie(new Cookie("session_id", "some-session")))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void should_Return204_When_RefreshWithValidCookie() throws Exception {
        when(refreshTokenUseCase.refresh("valid-refresh-token"))
                .thenReturn(new RefreshTokenResult("new-access", "new-refresh"));

        mockMvc.perform(post("/api/auth/refresh")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .cookie(new Cookie("refresh_token", "valid-refresh-token")))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void should_Return401_When_RefreshWithNoCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void should_Return401_When_RefreshTokenInvalid() throws Exception {
        when(refreshTokenUseCase.refresh(any()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/refresh")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .cookie(new Cookie("refresh_token", "expired-token")))
                .andExpect(status().isUnauthorized());
    }

}

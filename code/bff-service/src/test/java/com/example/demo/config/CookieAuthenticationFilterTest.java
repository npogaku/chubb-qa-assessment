package com.example.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CookieAuthenticationFilterTest {

    private CookieAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new CookieAuthenticationFilter();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void should_AddAuthorizationHeader_When_AccessTokenCookiePresent() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Custom-Header", "custom-value");
        request.setCookies(new Cookie("access_token", "my-jwt-token"));
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(argThat(req -> {
            var httpReq = (jakarta.servlet.http.HttpServletRequest) req;
            // Covers Authorization branch (true) of equalsIgnoreCase check
            boolean authHeader = "Bearer my-jwt-token".equals(httpReq.getHeader("Authorization"));
            // Covers non-Authorization branch (false) — falls back to super.getHeader()
            boolean otherHeader = "custom-value".equals(httpReq.getHeader("X-Custom-Header"));
            return authHeader && otherHeader;
        }), argThat(res -> true));
    }

    @Test
    void should_PassThrough_When_NoAccessTokenCookie() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void should_SkipCookieProcessing_When_AuthorizationHeaderAlreadyPresent() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer existing-token");
        request.setCookies(new Cookie("access_token", "cookie-token"));
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(argThat(req -> {
            var httpReq = (jakarta.servlet.http.HttpServletRequest) req;
            return "Bearer existing-token".equals(httpReq.getHeader("Authorization"));
        }), argThat(res -> true));
    }

    @Test
    void should_PassThrough_When_NoCookiesAtAll() throws Exception {
        var request = new MockHttpServletRequest();
        // no cookies set, getCookies() returns null
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void should_PassThrough_When_OnlyOtherCookiesPresent() throws Exception {
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("session_id", "some-session"));
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}

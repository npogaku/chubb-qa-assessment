package com.example.demo.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ContractTestSecurityConfigurationTest {

    private final ContractTestSecurityConfiguration configuration =
            new ContractTestSecurityConfiguration();

    @Test
    void should_InjectStaticJwt_When_NoAuthenticationPresent() throws Exception {
        SecurityContextHolder.clearContext();

        OncePerRequestFilter filter = configuration.contractTestJwtInjectionFilter();
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isInstanceOf(JwtAuthenticationToken.class);

        var jwtAuth = (JwtAuthenticationToken) authentication;
        assertThat(jwtAuth.getToken().getTokenValue()).isEqualTo("test-token-for-validation");
        assertThat((String) jwtAuth.getToken().getClaim("sub"))
                .isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(jwtAuth.getAuthorities().size()).isEqualTo(2);

        SecurityContextHolder.clearContext();
    }

    @Test
    void should_NotOverrideExistingAuthentication() throws Exception {
        SecurityContextHolder.clearContext();

        OncePerRequestFilter filter = configuration.contractTestJwtInjectionFilter();
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // First call sets auth
        filter.doFilter(request, response, chain);
        var firstAuth = SecurityContextHolder.getContext().getAuthentication();

        // Second call should not override
        filter.doFilter(request, response, chain);
        var secondAuth = SecurityContextHolder.getContext().getAuthentication();

        assertThat(secondAuth).isSameAs(firstAuth);

        SecurityContextHolder.clearContext();
    }
}

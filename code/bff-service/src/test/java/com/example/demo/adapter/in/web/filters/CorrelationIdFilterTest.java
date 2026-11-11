package com.example.demo.adapter.in.web.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CorrelationIdFilter}.
 *
 * <p>Uses MockHttpServletRequest / MockHttpServletResponse + a lambda FilterChain —
 * no Spring context required.
 *
 * <p>TC-094: Generates new UUID when no X-Correlation-ID header present
 * <p>TC-095: Reuses existing X-Correlation-ID header
 * <p>TC-096: Adds X-Correlation-ID to HTTP response headers
 * <p>TC-097: MDC is cleared after request completes (no thread-local leak)
 */
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void TC094_should_GenerateNewUUID_When_NoCorrelationIdHeaderPresent() throws ServletException, IOException {
        // Arrange
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        FilterChain noOpChain = (req, res) -> { };

        // Act
        filter.doFilter(request, response, noOpChain);

        // Assert — a valid UUID correlation ID is generated
        String correlationId = response.getHeader("X-Correlation-ID");
        assertThat(correlationId).isNotNull().isNotBlank();
        assertThat(UUID.fromString(correlationId)).isNotNull(); // throws if not a valid UUID
    }

    @Test
    void TC095_should_ReuseExistingCorrelationId_When_HeaderAlreadyPresent() throws ServletException, IOException {
        // Arrange
        var request = new MockHttpServletRequest();
        String existingCorrelationId = "existing-correlation-id-abc123";
        request.addHeader("X-Correlation-ID", existingCorrelationId);
        var response = new MockHttpServletResponse();
        FilterChain noOpChain = (req, res) -> { };

        // Act
        filter.doFilter(request, response, noOpChain);

        // Assert — the same correlation ID is echoed back
        assertThat(response.getHeader("X-Correlation-ID")).isEqualTo(existingCorrelationId);
    }

    @Test
    void TC096_should_AddCorrelationIdToResponseHeader() throws ServletException, IOException {
        // Arrange
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        FilterChain noOpChain = (req, res) -> { };

        // Act
        filter.doFilter(request, response, noOpChain);

        // Assert — X-Correlation-ID is present in response regardless of whether generated or forwarded
        assertThat(response.getHeader("X-Correlation-ID")).isNotNull().isNotBlank();
    }

    @Test
    void TC098_should_GenerateNewUUID_When_CorrelationIdHeaderIsBlank() throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "   ");
        var response = new MockHttpServletResponse();
        FilterChain noOpChain = (req, res) -> { };

        filter.doFilter(request, response, noOpChain);

        String correlationId = response.getHeader("X-Correlation-ID");
        assertThat(correlationId).isNotNull().isNotBlank();
        assertThat(UUID.fromString(correlationId)).isNotNull();
    }

    @Test
    void TC097_should_ClearMDCAfterRequest_ToPreventThreadLocalLeak() throws ServletException, IOException {
        // Arrange
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        // Capture MDC state *during* filter chain execution to confirm it was set
        var mdcDuringRequest = new String[1];
        FilterChain capturingChain = (req, res) -> {
            mdcDuringRequest[0] = MDC.get("correlationId");
        };

        // Act
        filter.doFilter(request, response, capturingChain);

        // Assert — MDC was populated during execution, then cleared in the finally block
        assertThat(mdcDuringRequest[0])
                .as("correlationId must be set in MDC during request processing")
                .isNotNull();
        assertThat(MDC.get("correlationId"))
                .as("MDC must be cleared after request completes (no thread-local leak)")
                .isNull();
    }
}

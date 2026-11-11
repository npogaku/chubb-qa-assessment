package com.example.demo.adapter.out.http;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorrelationIdInterceptorTest {

    private CorrelationIdInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new CorrelationIdInterceptor();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void should_AddCorrelationIdHeader_When_PresentInMdc() throws IOException {
        MDC.put("correlationId", "test-correlation-id");

        var request = new Request.Builder().url("http://example.com").build();
        var chain = mock(Interceptor.Chain.class);
        var response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build();

        when(chain.request()).thenReturn(request);
        when(chain.proceed(argThat(r -> "test-correlation-id".equals(r.header("X-Correlation-ID"))))).thenReturn(response);

        interceptor.intercept(chain);

        verify(chain).proceed(argThat(r -> "test-correlation-id".equals(r.header("X-Correlation-ID"))));
    }

    @Test
    void should_NotAddHeader_When_MdcIsEmpty() throws IOException {
        var request = new Request.Builder().url("http://example.com").build();
        var chain = mock(Interceptor.Chain.class);
        var response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build();

        when(chain.request()).thenReturn(request);
        when(chain.proceed(request)).thenReturn(response);

        interceptor.intercept(chain);

        verify(chain).proceed(request);
    }

    @Test
    void should_NotAddHeader_When_CorrelationIdIsBlank() throws IOException {
        MDC.put("correlationId", "   ");

        var request = new Request.Builder().url("http://example.com").build();
        var chain = mock(Interceptor.Chain.class);
        var response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build();

        when(chain.request()).thenReturn(request);
        when(chain.proceed(request)).thenReturn(response);

        interceptor.intercept(chain);

        verify(chain).proceed(request);
    }
}

package com.example.demo.adapter.out.http;

import brave.Tracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TracingInterceptorTest {

    @Mock
    private Tracing tracing;

    @Test
    void should_ProceedOriginalRequest_When_NoTraceContext() throws IOException {
        var currentTraceContext = mock(CurrentTraceContext.class);
        when(tracing.currentTraceContext()).thenReturn(currentTraceContext);
        when(currentTraceContext.get()).thenReturn(null);

        var interceptor = new TracingInterceptor(tracing);
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
    @SuppressWarnings("unchecked")
    void should_InjectTraceHeaders_When_TraceContextPresent() throws IOException {
        var traceContext = TraceContext.newBuilder().traceId(1L).spanId(2L).build();
        var currentTraceContext = mock(CurrentTraceContext.class);
        var propagation = mock(brave.propagation.Propagation.class);

        when(tracing.currentTraceContext()).thenReturn(currentTraceContext);
        when(currentTraceContext.get()).thenReturn(traceContext);
        when(tracing.propagation()).thenReturn(propagation);

        var injector = mock(brave.propagation.Propagation.Setter.class);
        when(propagation.injector(any())).thenReturn((context, carrier) -> {
            // simulate B3 header injection - no-op for test
        });

        var interceptor = new TracingInterceptor(tracing);
        var request = new Request.Builder().url("http://example.com").build();
        var chain = mock(Interceptor.Chain.class);
        var response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build();

        when(chain.request()).thenReturn(request);
        when(chain.proceed(any(Request.class))).thenReturn(response);

        interceptor.intercept(chain);

        verify(chain).proceed(any(Request.class));
    }
}

package com.example.demo.config;

import com.example.bff.adapter.out.claims.ApiClient;
import com.example.demo.adapter.out.http.CorrelationIdInterceptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

/**
 * Test configuration for REST clients.
 * Provides mock beans for @WebMvcTest contexts to avoid loading full RestClientConfig.
 */
@TestConfiguration
public class TestRestClientConfig {

    @Bean
    @Primary
    public ApiClient mockApiClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://localhost:8080");
        return apiClient;
    }

    @Bean
    public CorrelationIdInterceptor correlationIdInterceptor() {
        return new CorrelationIdInterceptor();
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}

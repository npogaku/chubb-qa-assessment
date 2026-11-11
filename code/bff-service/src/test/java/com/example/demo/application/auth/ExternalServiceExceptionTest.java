package com.example.demo.application.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalServiceExceptionTest {

    @Test
    void should_IncludeServiceName_In_Message() {
        var ex = new ExternalServiceException("Keycloak", "User creation failed: HTTP 500");

        assertThat(ex.getServiceName()).isEqualTo("Keycloak");
        assertThat(ex.getMessage()).contains("Keycloak").contains("User creation failed");
    }

    @Test
    void should_IncludeCause_When_ProvidedWithThreArgConstructor() {
        var cause = new RuntimeException("root cause");
        var ex = new ExternalServiceException("Claims", "timeout", cause);

        assertThat(ex.getServiceName()).isEqualTo("Claims");
        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex.getMessage()).contains("Claims").contains("timeout");
    }
}

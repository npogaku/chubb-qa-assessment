package com.example.demo.adapter.out.messaging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaMessageValidationExceptionTest {

    @Test
    void should_CreateException_When_MessageOnly() {
        var exception = new KafkaMessageValidationException("validation failed");

        assertThat(exception.getMessage()).isEqualTo("validation failed");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void should_CreateException_When_MessageAndCause() {
        var cause = new RuntimeException("root cause");
        var exception = new KafkaMessageValidationException("validation failed", cause);

        assertThat(exception.getMessage()).isEqualTo("validation failed");
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}

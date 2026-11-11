package com.example.demo.domain;

import com.example.demo.domain.claim.InvalidClaimException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    @Test
    void should_CreateExceptionWithMessage() {
        var ex = new InvalidClaimException("bad claim");

        assertThat(ex.getMessage()).isEqualTo("bad claim");
    }

    // Covers the protected DomainException(String, Throwable) constructor via a concrete subclass
    static class TestDomainException extends DomainException {
        TestDomainException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Test
    void should_CreateExceptionWithMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");

        var ex = new TestDomainException("domain error", cause);

        assertThat(ex.getMessage()).isEqualTo("domain error");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}

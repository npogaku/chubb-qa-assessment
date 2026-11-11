package com.example.demo.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaMessageSchemaValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_PassValidation_When_ValidClaimSubmittedPayload() {
        var validator = new KafkaMessageSchemaValidator(objectMapper, true);

        String validPayload = """
                {
                    "eventId": "evt-123",
                    "claimId": "d4e5f6a7-b8c9-0123-4567-89abcdef0123",
                    "userId": "f6a7b8c9-d0e1-2345-6789-abcdef012345",
                    "incidentDate": "2026-03-01",
                    "claimAmount": 2500.00,
                    "occurredAt": "2026-03-02T10:30:00Z"
                }
                """;

        assertThatCode(() -> validator.validate(validPayload, "ClaimSubmittedMessage"))
                .doesNotThrowAnyException();
    }

    @Test
    void should_ThrowException_When_MissingRequiredField() {
        var validator = new KafkaMessageSchemaValidator(objectMapper, true);

        String missingClaimId = """
                {
                    "eventId": "evt-123",
                    "userId": "f6a7b8c9-d0e1-2345-6789-abcdef012345",
                    "incidentDate": "2026-03-01",
                    "claimAmount": 2500.00,
                    "occurredAt": "2026-03-02T10:30:00Z"
                }
                """;

        assertThatThrownBy(() -> validator.validate(missingClaimId, "ClaimSubmittedMessage"))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessageContaining("ClaimSubmittedMessage")
                .hasMessageContaining("claimId");
    }

    @Test
    void should_ThrowException_When_InvalidClaimStatusEnum() {
        var validator = new KafkaMessageSchemaValidator(objectMapper, true);

        String invalidStatus = """
                {
                    "eventId": "evt-123",
                    "claimId": "d4e5f6a7-b8c9-0123-4567-89abcdef0123",
                    "oldStatus": "INVALID_STATUS",
                    "newStatus": "APPROVED",
                    "occurredAt": "2026-03-05T14:22:00Z"
                }
                """;

        assertThatThrownBy(() -> validator.validate(invalidStatus, "ClaimStatusChangedMessage"))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessageContaining("ClaimStatusChangedMessage");
    }

    @Test
    void should_SkipValidation_When_Disabled() {
        var validator = new KafkaMessageSchemaValidator(objectMapper, false);

        assertThatCode(() -> validator.validate("invalid-json", "NonExistentSchema"))
                .doesNotThrowAnyException();
    }

    @Test
    void should_ThrowException_When_SchemaFileNotFound() {
        var validator = new KafkaMessageSchemaValidator(objectMapper, true);

        assertThatThrownBy(() -> validator.validate("{}", "NonExistentSchema"))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessageContaining("Schema file not found");
    }

    @Test
    void should_ThrowException_When_InvalidJson() {
        var validator = new KafkaMessageSchemaValidator(objectMapper, true);

        assertThatThrownBy(() -> validator.validate("not-json", "ClaimSubmittedMessage"))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessageContaining("Failed to parse JSON");
    }

    @Test
    void should_PassValidation_When_ClaimStatusChangedWithNullChangedBy() {
        var validator = new KafkaMessageSchemaValidator(objectMapper, true);

        // changedBy is optional in the spec (not in required list); omitting it must be valid
        String payloadWithoutChangedBy = """
                {
                    "eventId": "evt-123",
                    "claimId": "d4e5f6a7-b8c9-0123-4567-89abcdef0123",
                    "oldStatus": "SUBMITTED",
                    "newStatus": "APPROVED",
                    "occurredAt": "2026-03-05T14:22:00Z"
                }
                """;

        assertThatCode(() -> validator.validate(payloadWithoutChangedBy, "ClaimStatusChangedMessage"))
                .doesNotThrowAnyException();
    }

    @Test
    void should_CacheSchema_When_CalledMultipleTimes() {
        var validator = new KafkaMessageSchemaValidator(objectMapper, true);

        String validPayload = """
                {
                    "eventId": "evt-123",
                    "claimId": "d4e5f6a7-b8c9-0123-4567-89abcdef0123",
                    "userId": "f6a7b8c9-d0e1-2345-6789-abcdef012345",
                    "incidentDate": "2026-03-01",
                    "claimAmount": 2500.00,
                    "occurredAt": "2026-03-02T10:30:00Z"
                }
                """;

        assertThatCode(() -> {
            validator.validate(validPayload, "ClaimSubmittedMessage");
            validator.validate(validPayload, "ClaimSubmittedMessage");
        }).doesNotThrowAnyException();
    }
}

package com.example.demo.adapter.out.messaging;

import com.example.demo.domain.claim.ClaimStatus;
import com.example.demo.domain.claim.events.ClaimStatusChanged;
import com.example.demo.domain.claim.events.ClaimSubmitted;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaDomainEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaMessageSchemaValidator schemaValidator;

    private KafkaDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaDomainEventPublisher(kafkaTemplate, objectMapper, schemaValidator);
    }

    // TC-077: Event published to Kafka with correct topic and message key
    @Test
    void should_PublishEventToKafka_When_ClaimSubmittedEventProvided() throws JsonProcessingException {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimSubmitted(
                UUID.randomUUID().toString(),
                claimId,
                UUID.randomUUID(),
                LocalDate.now(),
                BigDecimal.TEN,
                Instant.now()
        );
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"eventId\":\"test\"}");
        doNothing().when(schemaValidator).validate(anyString(), anyString());
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(event);

        verify(kafkaTemplate).send(eq("claim-events"), eq(claimId.toString()), eq("{\"eventId\":\"test\"}"));
        verify(schemaValidator).validate("{\"eventId\":\"test\"}", "ClaimSubmittedMessage");
    }

    // TC-077b: ClaimStatusChanged published with correct key
    @Test
    void should_PublishEventToKafka_When_ClaimStatusChangedEventProvided() throws JsonProcessingException {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimStatusChanged(
                UUID.randomUUID().toString(),
                claimId,
                ClaimStatus.SUBMITTED,
                ClaimStatus.UNDER_REVIEW,
                UUID.randomUUID(),
                Instant.now()
        );
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"type\":\"status-changed\"}");
        doNothing().when(schemaValidator).validate(anyString(), anyString());
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(event);

        verify(kafkaTemplate).send(eq("claim-events"), eq(claimId.toString()), eq("{\"type\":\"status-changed\"}"));
        verify(schemaValidator).validate("{\"type\":\"status-changed\"}", "ClaimStatusChangedMessage");
    }

    // TC-078: Event payload contains expected fields when serialized
    @Test
    void should_SerializeEventFields_When_EventPublished() throws JsonProcessingException {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimSubmitted(
                UUID.randomUUID().toString(),
                claimId,
                UUID.randomUUID(),
                LocalDate.now(),
                BigDecimal.TEN,
                Instant.now()
        );

        ObjectMapper realMapper = new ObjectMapper();
        realMapper.findAndRegisterModules();
        KafkaDomainEventPublisher realPublisher = new KafkaDomainEventPublisher(kafkaTemplate, realMapper, schemaValidator);
        doNothing().when(schemaValidator).validate(anyString(), anyString());
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        realPublisher.publish(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("claim-events"), eq(claimId.toString()), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(payload).contains("\"eventId\"");
        assertThat(payload).contains("\"claimId\"");
        assertThat(payload).contains(claimId.toString());
    }

    // TC-079: Failed serialization throws IllegalStateException
    @Test
    void should_ThrowIllegalStateException_When_SerializationFails() throws JsonProcessingException {
        var event = new ClaimSubmitted(
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now(),
                BigDecimal.TEN,
                Instant.now()
        );
        when(objectMapper.writeValueAsString(event)).thenThrow(new JsonProcessingException("test") {});

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize domain event");
    }

    // TC-NEW-001: Publisher calls validator with correct payload and schema name for ClaimSubmitted
    @Test
    void should_CallValidatorWithCorrectArgs_When_ClaimSubmittedEventPublished() throws JsonProcessingException {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimSubmitted(
                UUID.randomUUID().toString(),
                claimId,
                UUID.randomUUID(),
                LocalDate.now(),
                BigDecimal.TEN,
                Instant.now()
        );

        ObjectMapper realMapper = new ObjectMapper();
        realMapper.findAndRegisterModules();
        String serialized = realMapper.writeValueAsString(event);

        when(objectMapper.writeValueAsString(event)).thenReturn(serialized);
        doNothing().when(schemaValidator).validate(serialized, "ClaimSubmittedMessage");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(event);

        verify(schemaValidator).validate(serialized, "ClaimSubmittedMessage");
        verify(kafkaTemplate).send(eq("claim-events"), eq(claimId.toString()), eq(serialized));
    }

    // TC-NEW-002: Publisher propagates KafkaMessageValidationException when validator rejects missing required field
    @Test
    void should_PropagateValidationException_When_ValidatorRejectsMissingRequiredField() throws JsonProcessingException {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimSubmitted(
                UUID.randomUUID().toString(),
                claimId,
                UUID.randomUUID(),
                LocalDate.now(),
                BigDecimal.TEN,
                Instant.now()
        );
        String payloadMissingClaimId = "{\"eventId\":\"evt-1\"}";
        when(objectMapper.writeValueAsString(event)).thenReturn(payloadMissingClaimId);
        doThrow(new KafkaMessageValidationException("claimId: is missing but it is required"))
                .when(schemaValidator).validate(payloadMissingClaimId, "ClaimSubmittedMessage");

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessageContaining("claimId");
    }

    // TC-NEW-003: Publisher propagates KafkaMessageValidationException when validator rejects invalid enum value
    @Test
    void should_PropagateValidationException_When_ValidatorRejectsInvalidClaimStatus() throws JsonProcessingException {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimStatusChanged(
                UUID.randomUUID().toString(),
                claimId,
                ClaimStatus.SUBMITTED,
                ClaimStatus.UNDER_REVIEW,
                UUID.randomUUID(),
                Instant.now()
        );
        String payloadWithInvalidStatus = "{\"eventId\":\"evt-1\",\"claimId\":\"" + claimId
                + "\",\"oldStatus\":\"INVALID_STATUS\",\"newStatus\":\"UNDER_REVIEW\",\"occurredAt\":\"2026-03-08T10:00:00Z\"}";
        when(objectMapper.writeValueAsString(event)).thenReturn(payloadWithInvalidStatus);
        doThrow(new KafkaMessageValidationException("oldStatus: does not have a value in the enumeration"))
                .when(schemaValidator).validate(payloadWithInvalidStatus, "ClaimStatusChangedMessage");

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessageContaining("oldStatus");
    }
}

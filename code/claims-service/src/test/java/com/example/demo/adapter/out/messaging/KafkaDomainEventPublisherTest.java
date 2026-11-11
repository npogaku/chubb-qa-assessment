package com.example.demo.adapter.out.messaging;

import com.example.demo.domain.claim.ClaimStatus;
import com.example.demo.domain.claim.events.ClaimStatusChanged;
import com.example.demo.domain.claim.events.ClaimSubmitted;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
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
import static org.mockito.Mockito.*;

// original tests were mocking ObjectMapper and stubbing writeValueAsString(event) directly
// but the publisher builds an envelope ObjectNode first via objectMapper.createObjectNode()
// mocked mapper returns null from createObjectNode() -> NPE on envelope.put()
// fix: use @Spy on a real mapper so createObjectNode() actually works
@ExtendWith(MockitoExtension.class)
class KafkaDomainEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    // need real mapper — publisher does createObjectNode() internally, can't mock that away
    @Spy
    private ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private KafkaMessageSchemaValidator schemaValidator;

    private KafkaDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaDomainEventPublisher(kafkaTemplate, mapper, schemaValidator);
        lenient().when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        lenient().doNothing().when(schemaValidator).validate(anyString(), anyString());
    }

    @Test
    void should_PublishToClaimEventsTopic_WithClaimIdAsKey_When_ClaimSubmittedEventProvided() {
        UUID claimId = UUID.randomUUID();
        publisher.publish(submittedEvent(claimId));
        verify(kafkaTemplate).send(eq("claim-events"), eq(claimId.toString()), anyString());
    }

    @Test
    void should_PublishToClaimEventsTopic_WithClaimIdAsKey_When_ClaimStatusChangedEventProvided() {
        UUID claimId = UUID.randomUUID();
        publisher.publish(statusChangedEvent(claimId));
        verify(kafkaTemplate).send(eq("claim-events"), eq(claimId.toString()), anyString());
    }

    @Test
    void should_IncludeClaimIdInEnvelope_When_ClaimSubmittedEventPublished() {
        UUID claimId = UUID.randomUUID();
        publisher.publish(submittedEvent(claimId));

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("claim-events"), eq(claimId.toString()), cap.capture());

        assertThat(cap.getValue()).contains("claim-submitted");
        assertThat(cap.getValue()).contains(claimId.toString());
    }

    @Test
    void should_IncludeOldAndNewStatusInEnvelope_When_ClaimStatusChangedEventPublished() {
        UUID claimId = UUID.randomUUID();
        publisher.publish(statusChangedEvent(claimId));

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), anyString(), cap.capture());

        assertThat(cap.getValue()).contains("SUBMITTED");
        assertThat(cap.getValue()).contains("UNDER_REVIEW");
    }

    @Test
    void should_ValidateAgainstClaimSubmittedSchema_When_ClaimSubmittedEventPublished() {
        publisher.publish(submittedEvent(UUID.randomUUID()));
        verify(schemaValidator).validate(anyString(), eq("ClaimSubmittedMessage"));
    }

    @Test
    void should_ValidateAgainstClaimStatusChangedSchema_When_StatusChangedEventPublished() {
        publisher.publish(statusChangedEvent(UUID.randomUUID()));
        verify(schemaValidator).validate(anyString(), eq("ClaimStatusChangedMessage"));
    }

    @Test
    void should_PropagateValidationException_When_SchemaValidatorRejectsPayload() {
        doThrow(new KafkaMessageValidationException("claimId: required"))
                .when(schemaValidator).validate(anyString(), anyString());

        assertThatThrownBy(() -> publisher.publish(submittedEvent(UUID.randomUUID())))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessageContaining("claimId");
    }

    @Test
    void should_ThrowIllegalStateException_When_EnvelopeSerializationFails() throws Exception {
        doThrow(new com.fasterxml.jackson.core.JsonProcessingException("fail") {})
                .when(mapper).writeValueAsString(any());

        assertThatThrownBy(() -> publisher.publish(submittedEvent(UUID.randomUUID())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize domain event");
    }

    // helpers
    private ClaimSubmitted submittedEvent(UUID claimId) {
        return new ClaimSubmitted(UUID.randomUUID().toString(), claimId,
                UUID.randomUUID(), LocalDate.now().minusDays(1), BigDecimal.TEN, Instant.now());
    }

    private ClaimStatusChanged statusChangedEvent(UUID claimId) {
        return new ClaimStatusChanged(UUID.randomUUID().toString(), claimId,
                ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, UUID.randomUUID(), Instant.now());
    }
}

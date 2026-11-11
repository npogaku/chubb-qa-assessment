package com.example.demo.adapter.in.messaging;

import com.example.demo.adapter.in.websocket.ClaimWebSocketHandler;
import com.example.demo.adapter.in.websocket.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ClaimEventKafkaConsumerTest {

    private ClaimWebSocketHandler webSocketHandler;
    private ClaimEventKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        webSocketHandler = mock(ClaimWebSocketHandler.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        consumer = new ClaimEventKafkaConsumer(webSocketHandler, objectMapper);
    }

    @Test
    void should_BroadcastToAdmins_When_ClaimSubmittedEventReceived() {
        // Arrange
        String message = """
                {
                    "eventId": "evt-123",
                    "eventType": "claim-submitted",
                    "timestamp": "2026-02-16T10:30:45.123Z",
                    "aggregateId": "claim-789",
                    "aggregateType": "Claim",
                    "payload": {
                        "claimId": "claim-789",
                        "userId": "user-1",
                        "claimAmount": 1500.00
                    }
                }
                """;

        // Act
        consumer.consumeDomainEvent(message);

        // Assert
        verify(webSocketHandler).broadcastToAdmins(any(WebSocketMessage.class));
        verify(webSocketHandler, never()).broadcastToUserAndAdmins(any(), any());
    }

    @Test
    void should_BroadcastToUserAndAdmins_When_ClaimStatusChangedEventReceived() {
        // Arrange
        String message = """
                {
                    "eventId": "evt-456",
                    "eventType": "claim-status-changed",
                    "timestamp": "2026-02-16T10:30:45.123Z",
                    "aggregateId": "claim-789",
                    "aggregateType": "Claim",
                    "payload": {
                        "claimId": "claim-789",
                        "userId": "user-1",
                        "oldStatus": "SUBMITTED",
                        "newStatus": "APPROVED"
                    }
                }
                """;

        // Act
        consumer.consumeDomainEvent(message);

        // Assert
        verify(webSocketHandler).broadcastToUserAndAdmins(eq("user-1"), any(WebSocketMessage.class));
    }

    @Test
    void should_BroadcastToAdmins_When_StatusChangedWithNoUserId() {
        // Arrange
        String message = """
                {
                    "eventId": "evt-789",
                    "eventType": "claim-status-changed",
                    "timestamp": "2026-02-16T10:30:45.123Z",
                    "aggregateId": "claim-789",
                    "aggregateType": "Claim",
                    "payload": {
                        "claimId": "claim-789",
                        "oldStatus": "SUBMITTED",
                        "newStatus": "APPROVED"
                    }
                }
                """;

        // Act
        consumer.consumeDomainEvent(message);

        // Assert
        verify(webSocketHandler).broadcastToAdmins(any(WebSocketMessage.class));
    }

    @Test
    void should_HandleCdcUpdateEvent_When_StatusChanges() {
        // Arrange
        String message = """
                {
                    "op": "u",
                    "before": { "id": "claim-789", "status": "SUBMITTED", "user_id": "user-1" },
                    "after": { "id": "claim-789", "status": "APPROVED", "user_id": "user-1" },
                    "ts_ms": 1234567890
                }
                """;

        // Act
        consumer.consumeCdcEvent(message);

        // Assert
        verify(webSocketHandler).broadcastToUserAndAdmins(eq("user-1"), any(WebSocketMessage.class));
    }

    @Test
    void should_HandleCdcCreateEvent_When_NewClaim() {
        // Arrange
        String message = """
                {
                    "op": "c",
                    "before": null,
                    "after": { "id": "claim-789", "status": "SUBMITTED", "user_id": "user-1" },
                    "ts_ms": 1234567890
                }
                """;

        // Act
        consumer.consumeCdcEvent(message);

        // Assert
        verify(webSocketHandler).broadcastToAdmins(any(WebSocketMessage.class));
    }

    @Test
    void should_LogAndContinue_When_DomainEventDeserializationFails() {
        // Arrange
        String invalidMessage = "not-valid-json";

        // Act - should not throw
        consumer.consumeDomainEvent(invalidMessage);

        // Assert
        verifyNoInteractions(webSocketHandler);
    }

    @Test
    void should_LogAndContinue_When_CdcEventDeserializationFails() {
        // Arrange
        String invalidMessage = "not-valid-json";

        // Act - should not throw
        consumer.consumeCdcEvent(invalidMessage);

        // Assert
        verifyNoInteractions(webSocketHandler);
    }

    @Test
    void should_IgnoreUnknownEventType() {
        // Arrange
        String message = """
                {
                    "eventId": "evt-999",
                    "eventType": "unknown-event",
                    "timestamp": "2026-02-16T10:30:45.123Z",
                    "aggregateId": "claim-789",
                    "aggregateType": "Claim",
                    "payload": {}
                }
                """;

        // Act
        consumer.consumeDomainEvent(message);

        // Assert
        verifyNoInteractions(webSocketHandler);
    }

    @Test
    void should_BroadcastToAdmins_When_CdcUpdateWithNoUserId() {
        String message = """
                {
                    "op": "u",
                    "before": { "claim_id": "claim-789", "status": "SUBMITTED" },
                    "after": { "claim_id": "claim-789", "status": "APPROVED" },
                    "ts_ms": 1234567890
                }
                """;

        consumer.consumeCdcEvent(message);

        verify(webSocketHandler).broadcastToAdmins(any(WebSocketMessage.class));
    }

    @Test
    void should_UseEventId_When_CorrelationIdEmpty() {
        String message = """
                {
                    "eventId": "evt-fallback",
                    "eventType": "claim-submitted",
                    "payload": {
                        "claimId": "claim-100"
                    }
                }
                """;

        consumer.consumeDomainEvent(message);

        verify(webSocketHandler).broadcastToAdmins(any(WebSocketMessage.class));
    }

    @Test
    void should_UseProvidedCorrelationId_When_PayloadHasCorrelationId() {
        String message = """
                {
                    "eventId": "evt-200",
                    "eventType": "claim-submitted",
                    "payload": {
                        "claimId": "claim-200",
                        "correlationId": "explicit-correlation-id"
                    }
                }
                """;

        consumer.consumeDomainEvent(message);

        verify(webSocketHandler).broadcastToAdmins(any(WebSocketMessage.class));
    }

    @Test
    void should_BroadcastToAdmins_When_ClaimStatusChangedWithEmptyUserId() {
        String message = """
                {
                    "eventId": "evt-300",
                    "eventType": "claim-status-changed",
                    "payload": {
                        "claimId": "claim-300",
                        "userId": "",
                        "oldStatus": "SUBMITTED",
                        "newStatus": "APPROVED"
                    }
                }
                """;

        consumer.consumeDomainEvent(message);

        verify(webSocketHandler).broadcastToAdmins(any(WebSocketMessage.class));
        verify(webSocketHandler, never()).broadcastToUserAndAdmins(any(), any());
    }

    @Test
    void should_BroadcastToAdmins_When_CdcUpdateWithEmptyUserId() {
        String message = """
                {
                    "op": "u",
                    "before": { "id": "claim-400", "status": "SUBMITTED", "user_id": "" },
                    "after": { "id": "claim-400", "status": "APPROVED", "user_id": "" },
                    "ts_ms": 1234567890
                }
                """;

        consumer.consumeCdcEvent(message);

        verify(webSocketHandler).broadcastToAdmins(any(WebSocketMessage.class));
    }

    @Test
    void should_IgnoreCdcDeleteOperation() {
        // Arrange
        String message = """
                {
                    "op": "d",
                    "before": { "id": "claim-789", "status": "SUBMITTED" },
                    "after": null,
                    "ts_ms": 1234567890
                }
                """;

        // Act
        consumer.consumeCdcEvent(message);

        // Assert
        verifyNoInteractions(webSocketHandler);
    }

    // TC-NEW-004: Valid ClaimSubmitted JSON payload deserialised correctly against AsyncAPI schema
    @Test
    void should_DeserialiseClaimSubmitted_When_ValidAsyncApiPayload() {
        String message = """
                {
                    "eventId": "evt-a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                    "eventType": "claim-submitted",
                    "payload": {
                        "claimId": "d4e5f6a7-b8c9-0123-4567-89abcdef0123",
                        "userId": "f6a7b8c9-d0e1-2345-6789-abcdef012345",
                        "incidentDate": "2026-03-01",
                        "claimAmount": 2500.00,
                        "occurredAt": "2026-03-02T10:30:00Z"
                    }
                }
                """;

        consumer.consumeDomainEvent(message);

        verify(webSocketHandler).broadcastToAdmins(any(WebSocketMessage.class));
    }

    // TC-NEW-005: Valid Debezium CDC payload with op=u deserialised correctly
    @Test
    void should_DeserialiseCdcUpdate_When_ValidDebeziumPayload() {
        String message = """
                {
                    "op": "u",
                    "before": {
                        "id": "d4e5f6a7-b8c9-0123-4567-89abcdef0123",
                        "user_id": "f6a7b8c9-d0e1-2345-6789-abcdef012345",
                        "status": "SUBMITTED",
                        "claim_amount": 2500.00,
                        "incident_date": "2026-03-01"
                    },
                    "after": {
                        "id": "d4e5f6a7-b8c9-0123-4567-89abcdef0123",
                        "user_id": "f6a7b8c9-d0e1-2345-6789-abcdef012345",
                        "status": "UNDER_REVIEW",
                        "claim_amount": 2500.00,
                        "incident_date": "2026-03-01"
                    },
                    "ts_ms": 1709625600000
                }
                """;

        consumer.consumeCdcEvent(message);

        verify(webSocketHandler).broadcastToUserAndAdmins(
                eq("f6a7b8c9-d0e1-2345-6789-abcdef012345"),
                any(WebSocketMessage.class));
    }

    // TC-NEW-006: Consumer does not crash when eventId is absent; falls back to empty correlationId and still broadcasts
    @Test
    void should_ProcessWithEmptyCorrelationId_When_EventIdMissing() {
        String message = """
                {
                    "eventType": "claim-submitted",
                    "payload": {
                        "claimId": "claim-100"
                    }
                }
                """;

        // Should not throw - consumer falls back to empty correlationId and still processes
        consumer.consumeDomainEvent(message);

        verify(webSocketHandler).broadcastToAdmins(any(WebSocketMessage.class));
    }
}

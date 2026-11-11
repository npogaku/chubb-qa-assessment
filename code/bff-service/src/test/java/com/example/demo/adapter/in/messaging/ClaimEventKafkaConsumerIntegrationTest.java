package com.example.demo.adapter.in.messaging;

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.adapter.in.websocket.ClaimWebSocketHandler;
import com.example.demo.adapter.in.websocket.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.profiles.active=test",
                "kafka.consumer.auto-startup=true"
        }
)
@Import(TestcontainersConfiguration.class)
@Tag("integration")
class ClaimEventKafkaConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClaimWebSocketHandler webSocketHandler;

    @Test
    void should_ConsumeKafkaMessage_And_BroadcastToAdmins_When_ClaimSubmitted() {
        // Arrange
        String message = """
                {
                    "eventId": "evt-integration-001",
                    "eventType": "claim-submitted",
                    "timestamp": "2026-02-16T10:30:45.123Z",
                    "aggregateId": "claim-integration-789",
                    "aggregateType": "Claim",
                    "payload": {
                        "claimId": "claim-integration-789",
                        "userId": "user-1",
                        "claimAmount": 1500.00
                    }
                }
                """;

        // Act
        kafkaTemplate.send("claim-events", "claim-integration-789", message);

        // Assert
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                verify(webSocketHandler, atLeastOnce()).broadcastToAdmins(any(WebSocketMessage.class))
        );
    }

    @Test
    void should_ConsumeKafkaMessage_And_BroadcastToUserAndAdmins_When_ClaimStatusChanged() {
        // Arrange
        String message = """
                {
                    "eventId": "evt-integration-002",
                    "eventType": "claim-status-changed",
                    "timestamp": "2026-02-16T10:30:45.123Z",
                    "aggregateId": "claim-integration-790",
                    "aggregateType": "Claim",
                    "payload": {
                        "claimId": "claim-integration-790",
                        "userId": "user-integration-1",
                        "oldStatus": "SUBMITTED",
                        "newStatus": "APPROVED"
                    }
                }
                """;

        // Act
        kafkaTemplate.send("claim-events", "claim-integration-790", message);

        // Assert
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                verify(webSocketHandler, atLeastOnce()).broadcastToUserAndAdmins(
                        eq("user-integration-1"), any(WebSocketMessage.class))
        );
    }

    @Test
    void should_ConsumeCdcEvent_And_Broadcast_When_StatusUpdated() {
        // Arrange
        String message = """
                {
                    "op": "u",
                    "before": { "id": "claim-cdc-789", "status": "SUBMITTED", "user_id": "user-cdc-1" },
                    "after": { "id": "claim-cdc-789", "status": "APPROVED", "user_id": "user-cdc-1" },
                    "ts_ms": 1234567890
                }
                """;

        // Act
        kafkaTemplate.send("cdc.demo-app.public.claims", "claim-cdc-789", message);

        // Assert
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                verify(webSocketHandler, atLeastOnce()).broadcastToUserAndAdmins(
                        eq("user-cdc-1"), any(WebSocketMessage.class))
        );
    }
}

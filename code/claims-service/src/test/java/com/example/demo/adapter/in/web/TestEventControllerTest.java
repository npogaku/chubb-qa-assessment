package com.example.demo.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TestEventControllerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private TestEventController testEventController;

    @Test
    void triggerCDCEvent_publishesEventToCorrectTopic() {
        var request = new MockCDCEventRequest("claim-123", "APPROVED");

        var response = testEventController.triggerCDCEvent(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("cdc.demo-app.public.claims"), eq("claim-123"), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(payload).contains("\"op\": \"u\"");
        assertThat(payload).contains("\"id\": \"claim-123\"");
        assertThat(payload).contains("\"status\": \"APPROVED\"");
    }

    @Test
    void triggerCDCEvent_includesTimestampInPayload() {
        var request = new MockCDCEventRequest("claim-456", "REJECTED");

        testEventController.triggerCDCEvent(request);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("cdc.demo-app.public.claims"), eq("claim-456"), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(payload).contains("\"ts_ms\":");
        assertThat(payload).contains("\"updated_at\":");
    }
}

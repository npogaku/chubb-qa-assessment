package com.example.demo.adapter.out.messaging;

import com.example.demo.domain.claim.events.ClaimSubmitted;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DomainEventListenerTest {

    @Mock
    private KafkaDomainEventPublisher kafkaDomainEventPublisher;

    @InjectMocks
    private DomainEventListener domainEventListener;

    @Test
    void should_DelegateToKafkaPublisher_When_DomainEventReceived() {
        var event = new ClaimSubmitted(
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now(),
                BigDecimal.TEN,
                Instant.now()
        );

        domainEventListener.handleDomainEvent(event);

        verify(kafkaDomainEventPublisher).publish(event);
    }
}

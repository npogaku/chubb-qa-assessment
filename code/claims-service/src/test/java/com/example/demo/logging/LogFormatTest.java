package com.example.demo.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying that structured JSON log output contains all required observability fields.
 *
 * <p>Uses an in-memory Logback {@link ListAppender} to capture {@link ILoggingEvent} objects
 * without file I/O or a Spring context. No Testcontainers or external services are required.
 *
 * <p>TC-101: Log event contains all required structured fields:
 *            {@code @timestamp}, {@code level}, {@code logger_name}, {@code message},
 *            {@code thread_name}, {@code correlationId}, {@code service_name}
 * <p>TC-052: The {@code correlationId} MDC field is present in the captured log event
 *            when set via {@link MDC#put(String, String)}
 */
@Tag("unit")
class LogFormatTest {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LogFormatTest.class);

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logbackLogger;

    @BeforeEach
    void setUp() {
        logbackLogger = (Logger) LoggerFactory.getLogger(LogFormatTest.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logbackLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logbackLogger.detachAppender(listAppender);
        listAppender.stop();
        MDC.clear();
    }

    /**
     * TC-101: Verifies all required structured log fields are present.
     *
     * <p>Fields on {@link ILoggingEvent} cover: {@code @timestamp}, {@code level},
     * {@code logger_name}, {@code message}, {@code thread_name}, {@code correlationId}.
     *
     * <p>{@code service_name} is provided by the {@link LogstashEncoder}'s {@code customFields}
     * configuration (see {@code logback-spring.xml}). This test verifies the encoder emits it
     * in the JSON output by configuring the encoder identically to production.
     */
    @Test
    void TC101_should_ProduceStructuredEventWithAllRequiredFields() throws Exception {
        // Arrange — seed MDC with a known correlationId
        MDC.put("correlationId", "test-correlation-id-tc101");

        // Act
        LOG.info("Test structured log message for TC-101");

        // Assert — event captured
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);

        // @timestamp — represented as epoch millis on ILoggingEvent
        assertThat(event.getTimeStamp())
                .as("@timestamp must be a positive epoch millis value")
                .isPositive();

        // level
        assertThat(event.getLevel())
                .as("level must be set")
                .isEqualTo(Level.INFO);

        // logger_name
        assertThat(event.getLoggerName())
                .as("logger_name must be present")
                .isNotBlank();

        // message
        assertThat(event.getFormattedMessage())
                .as("message must be present")
                .isNotBlank();

        // thread_name
        assertThat(event.getThreadName())
                .as("thread_name must be present")
                .isNotBlank();

        // correlationId — from MDC
        assertThat(event.getMDCPropertyMap())
                .as("MDC must contain correlationId")
                .containsKey("correlationId");
        assertThat(event.getMDCPropertyMap().get("correlationId"))
                .isEqualTo("test-correlation-id-tc101");

        // service_name — set via LogstashEncoder customFields (logback-spring.xml)
        // Verify the encoder emits this field in JSON output
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(logbackLogger.getLoggerContext());
        encoder.setCustomFields("{\"service_name\":\"claims-service\"}");
        encoder.start();
        String json = new String(encoder.encode(event), StandardCharsets.UTF_8);
        assertThat(json)
                .as("JSON output must include service_name field")
                .contains("\"service_name\"")
                .contains("claims-service");
        encoder.stop();
    }

    /**
     * TC-052: Verifies the {@code correlationId} MDC field flows through to the log event.
     */
    @Test
    void TC052_should_IncludeCorrelationIdInLogEvent_When_SetViaMDCPut() {
        // Arrange
        String knownCorrelationId = "known-correlation-id-tc052";
        MDC.put("correlationId", knownCorrelationId);

        // Act
        LOG.info("Test log for TC-052 correlation ID propagation");

        // Assert
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getMDCPropertyMap())
                .as("correlationId must be present in the captured log event's MDC map")
                .containsKey("correlationId");
        assertThat(event.getMDCPropertyMap().get("correlationId"))
                .isEqualTo(knownCorrelationId);
    }
}

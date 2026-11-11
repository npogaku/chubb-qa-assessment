package com.example.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    @Test
    void should_CreateObjectMapper_WithJavaTimeModuleAndNoTimestamps() {
        JacksonConfig config = new JacksonConfig();

        ObjectMapper mapper = config.objectMapper();

        assertThat(mapper).isNotNull();
        assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }
}

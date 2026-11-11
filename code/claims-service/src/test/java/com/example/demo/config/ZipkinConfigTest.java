package com.example.demo.config;

import org.junit.jupiter.api.Test;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.Encoding;

import static org.assertj.core.api.Assertions.assertThat;

class ZipkinConfigTest {

    @Test
    void should_ReturnJsonEncoding() {
        ZipkinConfig config = new ZipkinConfig();

        Encoding encoding = config.zipkinEncoding();

        assertThat(encoding).isEqualTo(Encoding.JSON);
    }

    @Test
    void should_CreateSender_WithGivenEndpoint() {
        ZipkinConfig config = new ZipkinConfig();

        BytesMessageSender sender = config.zipkinSender("http://localhost:9411/api/v2/spans");

        assertThat(sender).isNotNull();
    }
}

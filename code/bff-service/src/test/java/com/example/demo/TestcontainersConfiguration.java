package com.example.demo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"))
				.withReuse(true);
	}

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:latest"))
				.withExposedPorts(6379)
				.withCommand("redis-server", "--databases", "32")
				.withReuse(true);
	}

	@Bean
	DynamicPropertyRegistrar redisDatabaseIsolation(GenericContainer<?> redisContainer) {
		return registry -> {
			String forkNumber = System.getProperty("surefire.forkNumber", "0");
			registry.add("spring.data.redis.database", () -> forkNumber);
		};
	}
}

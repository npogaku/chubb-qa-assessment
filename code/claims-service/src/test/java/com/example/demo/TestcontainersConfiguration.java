package com.example.demo;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
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
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"))
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
	KeycloakContainer keycloakContainer() {
		return new KeycloakContainer("quay.io/keycloak/keycloak:26.1.4")
				.withRealmImportFile("keycloak/demo-app-realm.json")
				.withReuse(true);
	}

	@Bean
	DynamicPropertyRegistrar keycloakProperties(KeycloakContainer keycloak) {
		return registry -> {
			var issuerUri = keycloak.getAuthServerUrl() + "/realms/demo-app";
			registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuerUri);
			registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
					() -> issuerUri + "/protocol/openid-connect/certs");
		};
	}

	@Bean
	DynamicPropertyRegistrar redisDatabaseIsolation(GenericContainer<?> redisContainer) {
		return registry -> {
			String forkNumber = System.getProperty("surefire.forkNumber", "0");
			registry.add("spring.data.redis.database", () -> forkNumber);
		};
	}
}

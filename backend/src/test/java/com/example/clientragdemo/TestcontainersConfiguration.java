package com.example.clientragdemo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	OllamaContainer ollamaContainer() {
		OllamaContainer container = new OllamaContainer(DockerImageName.parse("ollama/ollama:latest"));
		// Started eagerly (rather than left to the Spring context lifecycle) so the model pulls
		// below run before any bean tries to actually call these models. nomic-embed-text is what
		// the documents feature's ingestion tests need; llama3.2:1b (small, fast to pull) is what
		// the chat feature's tests need for real chat completions.
		container.start();
		pullModel(container, "nomic-embed-text");
		pullModel(container, "llama3.2:1b");
		return container;
	}

	// spring-ai-spring-boot-testcontainers' @ServiceConnection support for OllamaContainer wires
	// Ollama's own native client (spring.ai.ollama.base-url) - this app uses the OpenAI-compatible
	// client everywhere instead (see build.gradle), pointed at Ollama's /v1 endpoint, which has no
	// dedicated @ServiceConnection support of its own, so it's wired manually here.
	@Bean
	DynamicPropertyRegistrar openAiProperties(OllamaContainer ollamaContainer) {
		return registry -> {
			registry.add("spring.ai.openai.base-url", () -> ollamaContainer.getEndpoint() + "/v1");
			// Explicit empty string, not omitted - Spring AI's OpenAI client treats "" as a
			// deliberate no-auth signal (see OpenAiAutoConfigurationUtil); Ollama doesn't require
			// a real key.
			registry.add("spring.ai.openai.api-key", () -> "");
		};
	}

	private static void pullModel(OllamaContainer container, String model) {
		try {
			ExecResult result = container.execInContainer("ollama", "pull", model);
			if (result.getExitCode() != 0) {
				throw new IllegalStateException(
						"ollama pull " + model + " exited with code " + result.getExitCode()
								+ "\nstdout: " + result.getStdout() + "\nstderr: " + result.getStderr());
			}
		}
		catch (IOException | InterruptedException ex) {
			throw new IllegalStateException("Failed to pull " + model + " into the Ollama test container", ex);
		}
	}

	@Bean
	@ServiceConnection
	PostgreSQLContainer pgvectorContainer() {
		return new PostgreSQLContainer(
				DockerImageName.parse("pgvector/pgvector:pg18").asCompatibleSubstituteFor("postgres")
		);
	}

}

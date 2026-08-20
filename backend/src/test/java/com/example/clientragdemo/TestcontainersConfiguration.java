package com.example.clientragdemo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	OllamaContainer ollamaContainer() {
		OllamaContainer container = new OllamaContainer(DockerImageName.parse("ollama/ollama:latest"));
		// Started eagerly (rather than left to the Spring context lifecycle) so the model pull
		// below runs before any bean tries to actually call the embedding model. nomic-embed-text
		// is the only model pulled here - it's what the documents feature's ingestion tests need;
		// add a chat model pull too once a feature exercises the chat model against this container.
		container.start();
		try {
			ExecResult result = container.execInContainer("ollama", "pull", "nomic-embed-text");
			if (result.getExitCode() != 0) {
				throw new IllegalStateException(
						"ollama pull nomic-embed-text exited with code " + result.getExitCode()
								+ "\nstdout: " + result.getStdout() + "\nstderr: " + result.getStderr());
			}
		}
		catch (IOException | InterruptedException ex) {
			throw new IllegalStateException("Failed to pull nomic-embed-text into the Ollama test container", ex);
		}
		return container;
	}

	@Bean
	@ServiceConnection
	PostgreSQLContainer pgvectorContainer() {
		return new PostgreSQLContainer(
				DockerImageName.parse("pgvector/pgvector:pg18").asCompatibleSubstituteFor("postgres")
		);
	}

}

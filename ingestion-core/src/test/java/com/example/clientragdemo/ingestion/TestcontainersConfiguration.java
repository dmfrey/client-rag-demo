package com.example.clientragdemo.ingestion;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

// Same containers/wiring as backend's TestcontainersConfiguration (see CLAUDE.md) - duplicated
// rather than shared, since it's test-only code and this module has no dependency backend could
// pull it from without an awkward test-fixtures wiring for two lines' worth of savings.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    OllamaContainer ollamaContainer() {
        OllamaContainer container = new OllamaContainer(DockerImageName.parse("ollama/ollama:latest"));
        container.start();
        pullModel(container, "nomic-embed-text");
        return container;
    }

    @Bean
    DynamicPropertyRegistrar openAiProperties(OllamaContainer ollamaContainer) {
        return registry -> {
            registry.add("spring.ai.openai.base-url", () -> ollamaContainer.getEndpoint() + "/v1");
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

package com.example.clientragdemo.ingestion.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.pivotal.cfenv.boot.genai.GenaiLocator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

// Shared by backend and sharepoint-batch: the embedding model comes from a dedicated, normally
// single-model GenAI service, looked up by name.
class GenaiBindingsConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GenaiBindingsConfiguration.class)
            .withPropertyValues("app.genai.embedding-service=embed-svc");

    @Test
    void takesTheEmbeddingModelFromTheEmbeddingService() {
        EmbeddingModel embedding = mock(EmbeddingModel.class);
        GenaiLocator locator = mock(GenaiLocator.class);
        when(locator.getModelNamesByCapability("EMBEDDING")).thenReturn(List.of("nomic-ai/nomic-embed-text-v2-moe"));
        when(locator.getEmbeddingModelByName("nomic-ai/nomic-embed-text-v2-moe")).thenReturn(embedding);
        GenaiLocatorFactory factory = name -> name.equals("embed-svc") ? locator : null;

        runner.withBean(GenaiLocatorFactory.class, () -> factory)
                .run(context -> assertThat(context.getBean(EmbeddingModel.class)).isSameAs(embedding));
    }

    @Test
    void anExplicitModelNameResolvesAnAllModelsService() {
        EmbeddingModel embedding = mock(EmbeddingModel.class);
        GenaiLocator locator = mock(GenaiLocator.class);
        when(locator.getModelNamesByCapability("EMBEDDING")).thenReturn(List.of("a", "b"));
        when(locator.getEmbeddingModelByName("b")).thenReturn(embedding);

        runner.withBean(GenaiLocatorFactory.class, () -> name -> locator)
                .withPropertyValues("app.genai.embedding-model=b")
                .run(context -> assertThat(context.getBean(EmbeddingModel.class)).isSameAs(embedding));
    }

    @Test
    void failsStartupWhenTheServiceIsAmbiguous() {
        GenaiLocator allModels = mock(GenaiLocator.class);
        when(allModels.getModelNamesByCapability("EMBEDDING")).thenReturn(List.of("a", "b"));

        runner.withBean(GenaiLocatorFactory.class, () -> name -> allModels).run(context ->
                assertThat(context).hasFailed().getFailure().rootCause().hasMessageContaining("2 EMBEDDING models"));
    }

    @Test
    void inactiveWithoutTheServiceName() {
        new ApplicationContextRunner().withUserConfiguration(GenaiBindingsConfiguration.class).run(context ->
                assertThat(context).doesNotHaveBean(EmbeddingModel.class));
    }
}

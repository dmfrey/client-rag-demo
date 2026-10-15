package com.example.clientragdemo.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.pivotal.cfenv.boot.genai.GenaiLocator;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

// The bound GenAI service can grant several models; the point of GenaiModelConfiguration is that
// the app picks its chat/embedding model by the configured name, never "first available" (the
// embedding model's dimension is baked into vector_store's schema).
class GenaiModelConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GenaiModelConfiguration.class)
            .withPropertyValues(
                    "spring.ai.openai.chat.model=openai/gpt-oss-120b",
                    "spring.ai.openai.embedding.model=nomic-ai/nomic-embed-text-v2-moe");

    @Test
    void selectsConfiguredModelsByName() {
        ChatModel chat = mock(ChatModel.class);
        EmbeddingModel embedding = mock(EmbeddingModel.class);
        GenaiLocator locator = mock(GenaiLocator.class);
        when(locator.getChatModelByName("openai/gpt-oss-120b")).thenReturn(chat);
        when(locator.getEmbeddingModelByName("nomic-ai/nomic-embed-text-v2-moe")).thenReturn(embedding);

        runner.withBean(GenaiLocator.class, () -> locator)
                .withPropertyValues("genai.locator.config-url=http://unused")
                .run(context -> {
                    assertThat(context.getBean(ChatModel.class)).isSameAs(chat);
                    assertThat(context.getBean(EmbeddingModel.class)).isSameAs(embedding);
                });
    }

    @Test
    void inactiveWithoutABoundGenaiService() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(ChatModel.class);
            assertThat(context).doesNotHaveBean(EmbeddingModel.class);
        });
    }
}

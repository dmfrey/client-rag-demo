package com.example.clientragdemo.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.pivotal.cfenv.boot.genai.GenaiLocator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

// Chat and embedding models come from two separately bound GenAI services, each looked up by
// name; the model used is whichever one that service grants.
class GenaiModelConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GenaiModelConfiguration.class)
            .withPropertyValues("app.genai.chat-service=chat-svc", "app.genai.embedding-service=embed-svc");

    @Test
    void takesTheChatModelFromTheChatServiceAndTheEmbeddingModelFromTheEmbeddingService() {
        ChatModel chat = mock(ChatModel.class);
        EmbeddingModel embedding = mock(EmbeddingModel.class);
        GenaiLocator chatLocator = mock(GenaiLocator.class);
        GenaiLocator embedLocator = mock(GenaiLocator.class);
        when(chatLocator.getModelNamesByCapability("CHAT")).thenReturn(List.of("openai/gpt-oss-120b"));
        when(chatLocator.getChatModelByName("openai/gpt-oss-120b")).thenReturn(chat);
        when(embedLocator.getModelNamesByCapability("EMBEDDING")).thenReturn(List.of("nomic-ai/nomic-embed-text-v2-moe"));
        when(embedLocator.getEmbeddingModelByName("nomic-ai/nomic-embed-text-v2-moe")).thenReturn(embedding);
        GenaiLocatorFactory factory = name -> name.equals("chat-svc") ? chatLocator : embedLocator;

        runner.withBean(GenaiLocatorFactory.class, () -> factory).run(context -> {
            assertThat(context.getBean(ChatModel.class)).isSameAs(chat);
            assertThat(context.getBean(EmbeddingModel.class)).isSameAs(embedding);
        });
    }

    @Test
    void failsStartupWhenAServiceIsAmbiguous() {
        GenaiLocator allModels = mock(GenaiLocator.class);
        when(allModels.getModelNamesByCapability("CHAT")).thenReturn(List.of("a", "b"));
        when(allModels.getModelNamesByCapability("EMBEDDING")).thenReturn(List.of("e"));

        runner.withBean(GenaiLocatorFactory.class, () -> name -> allModels).run(context ->
                assertThat(context).hasFailed().getFailure().rootCause().hasMessageContaining("2 CHAT models"));
    }

    @Test
    void inactiveWithoutTheServiceNames() {
        new ApplicationContextRunner().withUserConfiguration(GenaiModelConfiguration.class).run(context -> {
            assertThat(context).doesNotHaveBean(ChatModel.class);
            assertThat(context).doesNotHaveBean(EmbeddingModel.class);
        });
    }
}

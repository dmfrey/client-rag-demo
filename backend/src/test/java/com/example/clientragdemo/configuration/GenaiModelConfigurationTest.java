package com.example.clientragdemo.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.clientragdemo.ingestion.configuration.GenaiLocatorFactory;
import io.pivotal.cfenv.boot.genai.GenaiLocator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

// The chat model comes from its own separately bound GenAI service, looked up by name; the model
// used is whichever one that service grants. (The embedding side is covered by ingestion-core's
// GenaiBindingsConfigurationTest.)
class GenaiModelConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GenaiModelConfiguration.class)
            .withPropertyValues("app.genai.chat-service=chat-svc");

    @Test
    void takesTheChatModelFromTheChatService() {
        ChatModel chat = mock(ChatModel.class);
        GenaiLocator chatLocator = mock(GenaiLocator.class);
        when(chatLocator.getModelNamesByCapability("CHAT")).thenReturn(List.of("openai/gpt-oss-120b"));
        when(chatLocator.getChatModelByName("openai/gpt-oss-120b")).thenReturn(chat);
        GenaiLocatorFactory factory = name -> name.equals("chat-svc") ? chatLocator : null;

        runner.withBean(GenaiLocatorFactory.class, () -> factory)
                .run(context -> assertThat(context.getBean(ChatModel.class)).isSameAs(chat));
    }

    @Test
    void failsStartupWhenTheServiceIsAmbiguous() {
        GenaiLocator allModels = mock(GenaiLocator.class);
        when(allModels.getModelNamesByCapability("CHAT")).thenReturn(List.of("a", "b"));

        runner.withBean(GenaiLocatorFactory.class, () -> name -> allModels).run(context ->
                assertThat(context).hasFailed().getFailure().rootCause().hasMessageContaining("2 CHAT models"));
    }

    @Test
    void inactiveWithoutTheServiceName() {
        new ApplicationContextRunner().withUserConfiguration(GenaiModelConfiguration.class).run(context ->
                assertThat(context).doesNotHaveBean(ChatModel.class));
    }
}

package com.example.clientragdemo.ingestion.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.clientragdemo.ingestion.application.port.out.TranscribeImagePort;
import io.pivotal.cfenv.boot.genai.GenaiLocator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

// The vision model is optional: it exists only when app.genai.vision-service names a bound service.
class GenaiVisionConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GenaiVisionConfiguration.class);

    @Test
    void buildsTheTranscriberFromTheVisionService() {
        GenaiLocator locator = mock(GenaiLocator.class);
        when(locator.getModelNamesByCapability("VISION")).thenReturn(List.of("google/gemma"));
        when(locator.getChatModelByName("google/gemma")).thenReturn(mock(ChatModel.class));

        runner.withPropertyValues("app.genai.vision-service=vision-svc")
                .withBean(GenaiLocatorFactory.class, () -> name -> name.equals("vision-svc") ? locator : null)
                .run(context -> {
                    assertThat(context).hasSingleBean(TranscribeImagePort.class);
                    // kept private to the adapter so backend's single chat model stays unambiguous
                    assertThat(context).doesNotHaveBean(ChatModel.class);
                });
    }

    @Test
    void failsStartupWhenTheServiceGrantsNoVisionModel() {
        GenaiLocator textOnly = mock(GenaiLocator.class);
        when(textOnly.getModelNamesByCapability("VISION")).thenReturn(List.of());

        runner.withPropertyValues("app.genai.vision-service=chat-only")
                .withBean(GenaiLocatorFactory.class, () -> name -> textOnly)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void inactiveWithoutTheServiceName() {
        runner.run(context -> assertThat(context).doesNotHaveBean(TranscribeImagePort.class));
    }
}

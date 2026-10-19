package com.example.clientragdemo.ingestion.configuration;

import com.example.clientragdemo.ingestion.adapter.out.vision.VisionTranscriptionAdapter;
import com.example.clientragdemo.ingestion.application.port.out.TranscribeImagePort;
import io.pivotal.cfenv.boot.genai.GenaiLocator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Optional: active only when app.genai.vision-service names a bound GenAI service (set
// APP_GENAI_VISION_SERVICE after `cf bind-service`). Without it no TranscribeImagePort exists and
// image files / scanned PDFs fail ingestion with a clear message.
//
// The vision model is deliberately NOT exposed as a ChatModel bean: backend has exactly one chat
// model (the chat service's) that everything else injects, and a second ChatModel would make that
// ambiguous. It stays private to the adapter. Uses ingestion-core's GenaiLocatorFactory, so the
// service is looked up in VCAP_SERVICES the same way as the embedding service.
@Configuration
@ConditionalOnProperty("app.genai.vision-service")
@EnableConfigurationProperties(GenaiVisionProperties.class)
public class GenaiVisionConfiguration {

    @Bean
    TranscribeImagePort visionTranscriber(GenaiLocatorFactory locators, GenaiVisionProperties properties) {
        GenaiLocator locator = locators.forService(properties.visionService());
        String name = GenaiModelSelector.select(properties.visionService(), "VISION",
                locator.getModelNamesByCapability("VISION"), properties.visionModel());
        return new VisionTranscriptionAdapter(locator.getChatModelByName(name));
    }
}

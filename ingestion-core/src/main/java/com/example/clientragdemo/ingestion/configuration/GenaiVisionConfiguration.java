package com.example.clientragdemo.ingestion.configuration;

import com.example.clientragdemo.ingestion.adapter.out.vision.VisionTranscriptionAdapter;
import com.example.clientragdemo.ingestion.application.port.out.TranscribeImagePort;
import io.pivotal.cfenv.boot.genai.GenaiLocator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.List;
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

    private static final Log logger = LogFactory.getLog(GenaiVisionConfiguration.class);

    @Bean
    TranscribeImagePort visionTranscriber(GenaiLocatorFactory locators, GenaiVisionProperties properties) {
        GenaiLocator locator = locators.forService(properties.visionService());
        List<String> available = locator.getModelNamesByCapability("VISION");
        if (available.isEmpty()) {
            // Vision is optional, so a service without a VISION model must not take the whole app down
            // (this is typically a plan picked before anyone knew whether it can see). Documents that
            // need vision fail with this message instead.
            String message = "Vision service '" + properties.visionService() + "' grants no VISION model, so images"
                    + " and scanned PDFs cannot be read. Bind a service whose plan includes one.";
            logger.warn(message);
            return (image, mimeType) -> {
                throw new IllegalStateException(message);
            };
        }
        String name = GenaiModelSelector.select(properties.visionService(), "VISION", available,
                properties.visionModel());
        return new VisionTranscriptionAdapter(locator.getChatModelByName(name));
    }
}

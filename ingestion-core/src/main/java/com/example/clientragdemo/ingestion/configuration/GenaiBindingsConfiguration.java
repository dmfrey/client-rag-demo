package com.example.clientragdemo.ingestion.configuration;

import java.util.Map;

import io.pivotal.cfenv.boot.genai.DefaultGenaiLocator;
import io.pivotal.cfenv.boot.genai.GenaiLocator;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

// Cloud Foundry only: active when app.genai.embedding-service is set (each app's
// application-cloud.yaml). Everywhere else (local Ollama, tests) Spring AI's own
// spring.ai.openai.* autoconfiguration supplies the EmbeddingModel unchanged.
//
// The embedding model comes from a dedicated, normally single-model GenAI service, so which model
// is used is decided by which service you bind - not by a model name in a manifest. Its endpoint
// and API key come from that service's VCAP_SERVICES binding (java-cfenv). Shared by backend and
// sharepoint-batch so both embed with exactly the same model, selected the same way.
@Configuration
@ConditionalOnProperty("app.genai.embedding-service")
@EnableConfigurationProperties(GenaiEmbeddingProperties.class)
public class GenaiBindingsConfiguration {

    @Bean
    @ConditionalOnMissingBean
    GenaiLocatorFactory genaiLocatorFactory(ObjectProvider<RestClient.Builder> restClientBuilder) {
        return serviceName -> {
            CfService service;
            try {
                service = new CfEnv().findServiceByName(serviceName);
            } catch (RuntimeException e) {
                throw new IllegalStateException("No bound service named '" + serviceName + "' - check the app's"
                        + " services: in the manifest and the app.genai.*-service settings.", e);
            }
            Object endpoint = service.getCredentials().getMap().get("endpoint");
            if (!(endpoint instanceof Map<?, ?> credentials)) {
                throw new IllegalStateException("Service '" + serviceName + "' has no credentials.endpoint - is it a"
                        + " Tanzu GenAI (ai-models) service?");
            }
            return new DefaultGenaiLocator(restClientBuilder.getIfAvailable(RestClient::builder),
                    (String) credentials.get("config_url"), (String) credentials.get("api_key"),
                    (String) credentials.get("api_base"));
        };
    }

    @Bean
    @Primary
    EmbeddingModel genaiEmbeddingModel(GenaiLocatorFactory locators, GenaiEmbeddingProperties properties) {
        GenaiLocator locator = locators.forService(properties.embeddingService());
        String name = GenaiModelSelector.select(properties.embeddingService(), "EMBEDDING",
                locator.getModelNamesByCapability("EMBEDDING"), properties.embeddingModel());
        return locator.getEmbeddingModelByName(name);
    }
}

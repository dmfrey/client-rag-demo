package com.example.clientragdemo.configuration;

import java.util.Map;

import io.pivotal.cfenv.boot.genai.DefaultGenaiLocator;
import io.pivotal.cfenv.boot.genai.GenaiLocator;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

// Cloud Foundry only: active when app.genai.chat-service and app.genai.embedding-service are set
// (application-cloud.yaml). Everywhere else (local Ollama, tests) Spring AI's own
// spring.ai.openai.* autoconfiguration is used unchanged.
//
// The chat and embedding models come from two separately bound GenAI services, each normally on a
// single-model plan, so which model is used is decided by which service you bind - not by a model
// name in a manifest. Each service's credentials come from its VCAP_SERVICES binding (java-cfenv).
@Configuration
@ConditionalOnProperty({"app.genai.chat-service", "app.genai.embedding-service"})
@EnableConfigurationProperties(GenaiProperties.class)
class GenaiModelConfiguration {

    @Bean
    @ConditionalOnMissingBean
    GenaiLocatorFactory genaiLocatorFactory(RestClient.Builder restClientBuilder) {
        CfEnv cfEnv = new CfEnv();
        return serviceName -> {
            CfService service;
            try {
                service = cfEnv.findServiceByName(serviceName);
            } catch (RuntimeException e) {
                throw new IllegalStateException("No bound service named '" + serviceName + "' - check the app's"
                        + " services: in the manifest and app.genai.chat-service / app.genai.embedding-service.", e);
            }
            Object endpoint = service.getCredentials().getMap().get("endpoint");
            if (!(endpoint instanceof Map<?, ?> credentials)) {
                throw new IllegalStateException("Service '" + serviceName + "' has no credentials.endpoint - is it a"
                        + " Tanzu GenAI (ai-models) service?");
            }
            return new DefaultGenaiLocator(restClientBuilder, (String) credentials.get("config_url"),
                    (String) credentials.get("api_key"), (String) credentials.get("api_base"));
        };
    }

    @Bean
    @Primary
    ChatModel genaiChatModel(GenaiLocatorFactory locators, GenaiProperties properties) {
        GenaiLocator locator = locators.forService(properties.chatService());
        String name = GenaiModelSelector.select(properties.chatService(), "CHAT",
                locator.getModelNamesByCapability("CHAT"), properties.chatModel());
        return locator.getChatModelByName(name);
    }

    @Bean
    @Primary
    EmbeddingModel genaiEmbeddingModel(GenaiLocatorFactory locators, GenaiProperties properties) {
        GenaiLocator locator = locators.forService(properties.embeddingService());
        String name = GenaiModelSelector.select(properties.embeddingService(), "EMBEDDING",
                locator.getModelNamesByCapability("EMBEDDING"), properties.embeddingModel());
        return locator.getEmbeddingModelByName(name);
    }
}

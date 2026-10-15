package com.example.clientragdemo.configuration;

import io.pivotal.cfenv.boot.genai.GenaiLocator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

// Active only when java-cfenv-boot-tanzu-genai found a bound GenAI service (it sets
// genai.locator.config-url from VCAP_SERVICES) - i.e. on Cloud Foundry. Everywhere else
// (local Ollama, tests) Spring AI's own spring.ai.openai.* autoconfiguration is used unchanged.
//
// The bound ai-models instance can grant several models; the locator lists them all, and which
// ones this app uses is decided here, by the same SPRING_AI_OPENAI_CHAT_MODEL /
// SPRING_AI_OPENAI_EMBEDDING_MODEL settings as before - not "first available". The embedding
// model in particular must be the 768-dimension one vector_store's column was created for.
@Configuration
@ConditionalOnProperty("genai.locator.config-url")
class GenaiModelConfiguration {

    @Bean
    @Primary
    ChatModel genaiChatModel(GenaiLocator locator, @Value("${spring.ai.openai.chat.model}") String name) {
        return locator.getChatModelByName(name);
    }

    @Bean
    @Primary
    EmbeddingModel genaiEmbeddingModel(GenaiLocator locator, @Value("${spring.ai.openai.embedding.model}") String name) {
        return locator.getEmbeddingModelByName(name);
    }
}

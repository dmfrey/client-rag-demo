package com.example.clientragdemo.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Which bound Cloud Foundry services supply the chat and embedding models (see
// GenaiModelConfiguration). Defaults live in application-cloud.yaml, so these only matter on CF.
// chatModel/embeddingModel are optional: with a single-model service plan (the intended setup)
// the one model that service grants is used and these stay blank; they only exist to disambiguate
// when a service is bound to an all-models plan.
@ConfigurationProperties(prefix = "app.genai")
public record GenaiProperties(String chatService, String embeddingService, String chatModel, String embeddingModel) {
}

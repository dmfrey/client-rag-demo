package com.example.clientragdemo.ingestion.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Which bound Cloud Foundry GenAI service supplies the embedding model (see
// GenaiBindingsConfiguration). The service name is set in each app's application-cloud.yaml.
// embeddingModel is optional: with a single-model plan (the intended setup) the one model the
// service grants is used and this stays blank; it only disambiguates an all-models plan.
// (backend binds its own chat-side properties from the same app.genai prefix.)
@ConfigurationProperties(prefix = "app.genai")
public record GenaiEmbeddingProperties(String embeddingService, String embeddingModel) {
}

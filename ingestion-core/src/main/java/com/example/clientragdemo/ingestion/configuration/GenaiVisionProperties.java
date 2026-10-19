package com.example.clientragdemo.ingestion.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Which bound Cloud Foundry GenAI service supplies the optional vision model (see
// GenaiVisionConfiguration). visionService is unset by default, which turns image/scanned-PDF
// ingestion off; visionModel only disambiguates a service that grants several VISION models.
@ConfigurationProperties(prefix = "app.genai")
public record GenaiVisionProperties(String visionService, String visionModel) {
}

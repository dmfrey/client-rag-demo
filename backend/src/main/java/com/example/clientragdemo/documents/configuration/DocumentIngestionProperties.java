package com.example.clientragdemo.documents.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// 800 matches TokenTextSplitter's own built-in default (DEFAULT_CHUNK_SIZE) - same default,
// just externalized. Needs lowering per deployment when the embedding model's own context
// window is smaller than the chunk size: Tanzu Platform's genai-service proxy serves
// nomic-embed-text-v2-moe with just a 512-token limit (see manifest.yml), well under this
// default, discovered only by a real embedding call failing in production - Ollama's
// nomic-embed-text (v1) never enforced anything this tight locally/in tests.
@ConfigurationProperties(prefix = "app.documents.ingestion")
public record DocumentIngestionProperties(@DefaultValue("800") int chunkSize) {}

package com.example.clientragdemo.ingestion.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// 800 matches TokenTextSplitter's own built-in default (DEFAULT_CHUNK_SIZE) - same default, just
// externalized. Needs lowering per deployment when the embedding model's own context window is
// smaller than the chunk size - see backend's application.yaml/manifest.yml for the concrete
// example (Tanzu Platform's genai-service proxy). Prefix kept as app.documents.ingestion (not
// app.ingestion) across this class's move/rename into ingestion-core so backend's already-deployed
// APP_DOCUMENTS_INGESTION_CHUNK_SIZE env var keeps working unchanged.
@ConfigurationProperties(prefix = "app.documents.ingestion")
public record IngestionProperties(@DefaultValue("800") int chunkSize,
        // Scanned PDFs are read page by page through the vision model (one call per page), so this
        // bounds the cost/time of a single document. Larger scans fail with a clear message.
        @DefaultValue("100") int visionMaxPages) {}

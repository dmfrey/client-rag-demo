package com.example.clientragdemo.ingestion.application.domain.model;

import java.time.Instant;

/**
 * source/externalId/sourceVersion track where a document came from - "UPLOAD" (externalId/
 * sourceVersion null) for the web-upload flow, "SHAREPOINT" (externalId = the Graph driveItem id,
 * sourceVersion = its eTag) for sharepoint-batch's polling ingestion. externalId, not filename, is
 * the real identity for a non-upload source - see LoadDocumentBySourceAndExternalIdPort.
 */
public record Document(
        Long id,
        String filename,
        ContentType contentType,
        DocumentStatus status,
        String errorMessage,
        Integer chunkCount,
        String uploadedBy,
        String source,
        String externalId,
        String sourceVersion,
        Instant createdAt,
        Instant updatedAt
) {

    public static final String SOURCE_UPLOAD = "UPLOAD";
    public static final String SOURCE_SHAREPOINT = "SHAREPOINT";
}

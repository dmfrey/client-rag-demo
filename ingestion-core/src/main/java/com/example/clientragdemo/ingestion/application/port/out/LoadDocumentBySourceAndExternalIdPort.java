package com.example.clientragdemo.ingestion.application.port.out;

import com.example.clientragdemo.ingestion.application.domain.model.Document;

import java.util.Optional;

// The dedup/change-detection lookup a non-upload source (sharepoint-batch) uses instead of
// LoadDocumentByFilenamePort - a Graph driveItem id is a stable, globally unique identity within
// its source, unlike filename (which is only unique per source, see documents-003's unique
// constraint).
public interface LoadDocumentBySourceAndExternalIdPort {

    Optional<Document> loadBySourceAndExternalId(String source, String externalId);
}

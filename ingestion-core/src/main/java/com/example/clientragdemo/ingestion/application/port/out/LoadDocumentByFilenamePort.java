package com.example.clientragdemo.ingestion.application.port.out;

import com.example.clientragdemo.ingestion.application.domain.model.Document;

import java.util.Optional;

public interface LoadDocumentByFilenamePort {

    Optional<Document> loadByFilename(String filename);
}

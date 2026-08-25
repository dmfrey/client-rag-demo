package com.example.clientragdemo.ingestion.application.port.in;

import com.example.clientragdemo.ingestion.application.domain.model.ContentType;

public interface IngestDocumentUseCase {

    void execute(IngestDocumentCommand command);

    record IngestDocumentCommand(Long documentId, String filename, byte[] content, ContentType contentType) {}
}

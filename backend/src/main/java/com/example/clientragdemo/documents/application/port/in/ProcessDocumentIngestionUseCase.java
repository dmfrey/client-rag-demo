package com.example.clientragdemo.documents.application.port.in;

import com.example.clientragdemo.documents.application.domain.model.ContentType;

public interface ProcessDocumentIngestionUseCase {

    void execute(ProcessDocumentIngestionCommand command);

    record ProcessDocumentIngestionCommand(Long documentId, String filename, byte[] content, ContentType contentType) {}
}

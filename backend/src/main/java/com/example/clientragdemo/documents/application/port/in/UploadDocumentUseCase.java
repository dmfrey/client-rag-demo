package com.example.clientragdemo.documents.application.port.in;

import com.example.clientragdemo.ingestion.application.domain.model.Document;

public interface UploadDocumentUseCase {

    Document execute(UploadDocumentCommand command);

    record UploadDocumentCommand(String filename, byte[] content, String uploadedBy) {}
}

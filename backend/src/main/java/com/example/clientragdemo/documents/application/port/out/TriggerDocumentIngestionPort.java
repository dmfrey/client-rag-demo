package com.example.clientragdemo.documents.application.port.out;

import com.example.clientragdemo.ingestion.application.domain.model.ContentType;

public interface TriggerDocumentIngestionPort {

    void trigger(Long documentId, String filename, byte[] content, ContentType contentType);
}

package com.example.clientragdemo.ingestion.application.port.out;

import com.example.clientragdemo.ingestion.application.domain.model.ContentType;

public interface IndexDocumentChunksPort {

    int index(Long documentId, String filename, byte[] content, ContentType contentType);
}

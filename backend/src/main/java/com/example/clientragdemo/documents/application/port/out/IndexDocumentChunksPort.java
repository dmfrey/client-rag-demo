package com.example.clientragdemo.documents.application.port.out;

import com.example.clientragdemo.documents.application.domain.model.ContentType;

public interface IndexDocumentChunksPort {

    int index(Long documentId, String filename, byte[] content, ContentType contentType);
}

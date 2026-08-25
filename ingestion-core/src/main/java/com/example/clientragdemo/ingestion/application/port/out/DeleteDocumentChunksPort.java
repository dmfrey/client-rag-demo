package com.example.clientragdemo.ingestion.application.port.out;

public interface DeleteDocumentChunksPort {

    void deleteByDocumentId(Long documentId);
}

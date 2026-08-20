package com.example.clientragdemo.documents.application.domain.service;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(Long id) {
        super("Document not found: " + id);
    }
}

package com.example.clientragdemo.documents.application.domain.service;

import com.example.clientragdemo.shared.exception.NotFoundException;

public class DocumentNotFoundException extends NotFoundException {

    public DocumentNotFoundException(Long id) {
        super("Document not found: " + id);
    }
}

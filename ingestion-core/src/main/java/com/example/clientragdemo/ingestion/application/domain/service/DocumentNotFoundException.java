package com.example.clientragdemo.ingestion.application.domain.service;

// Deliberately a plain RuntimeException, not backend's shared.exception.NotFoundException -
// ingestion-core has no HTTP concern of its own (sharepoint-batch has no REST API at all), so it
// doesn't depend on backend's status-code-mapping exception hierarchy. Backend's
// GlobalExceptionHandler adds its own @ExceptionHandler for this type instead.
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(Long id) {
        super("Document not found: " + id);
    }
}

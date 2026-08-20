package com.example.clientragdemo.documents.application.port.out;

import com.example.clientragdemo.documents.application.domain.model.Document;

import java.util.Optional;

public interface LoadDocumentByIdPort {

    Optional<Document> loadById(Long id);
}

package com.example.clientragdemo.documents.application.port.in;

import com.example.clientragdemo.ingestion.application.domain.model.Document;

import java.util.List;

public interface ListDocumentsUseCase {

    List<Document> execute();
}

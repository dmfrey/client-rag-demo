package com.example.clientragdemo.ingestion.application.port.out;

import com.example.clientragdemo.ingestion.application.domain.model.Document;

import java.util.List;

public interface LoadAllDocumentsPort {

    List<Document> loadAll();
}

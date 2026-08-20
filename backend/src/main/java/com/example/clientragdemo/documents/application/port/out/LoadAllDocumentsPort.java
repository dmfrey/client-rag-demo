package com.example.clientragdemo.documents.application.port.out;

import com.example.clientragdemo.documents.application.domain.model.Document;

import java.util.List;

public interface LoadAllDocumentsPort {

    List<Document> loadAll();
}

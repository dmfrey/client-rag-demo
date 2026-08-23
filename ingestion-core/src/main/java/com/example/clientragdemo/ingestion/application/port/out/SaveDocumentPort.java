package com.example.clientragdemo.ingestion.application.port.out;

import com.example.clientragdemo.ingestion.application.domain.model.Document;

public interface SaveDocumentPort {

    Document save(Document document);
}

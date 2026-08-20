package com.example.clientragdemo.documents.application.port.out;

import com.example.clientragdemo.documents.application.domain.model.Document;

public interface SaveDocumentPort {

    Document save(Document document);
}

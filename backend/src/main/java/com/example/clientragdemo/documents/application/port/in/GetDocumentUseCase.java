package com.example.clientragdemo.documents.application.port.in;

import com.example.clientragdemo.documents.application.domain.model.Document;

public interface GetDocumentUseCase {

    Document execute(GetDocumentQuery query);

    record GetDocumentQuery(Long id) {}
}

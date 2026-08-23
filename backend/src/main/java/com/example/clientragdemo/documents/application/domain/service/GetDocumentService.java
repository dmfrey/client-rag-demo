package com.example.clientragdemo.documents.application.domain.service;

import com.example.clientragdemo.documents.application.port.in.GetDocumentUseCase;
import com.example.clientragdemo.ingestion.application.domain.model.Document;
import com.example.clientragdemo.ingestion.application.domain.service.DocumentNotFoundException;
import com.example.clientragdemo.ingestion.application.port.out.LoadDocumentByIdPort;
import org.springframework.stereotype.Service;

@Service
class GetDocumentService implements GetDocumentUseCase {

    private final LoadDocumentByIdPort loadDocumentByIdPort;

    GetDocumentService(LoadDocumentByIdPort loadDocumentByIdPort) {
        this.loadDocumentByIdPort = loadDocumentByIdPort;
    }

    @Override
    public Document execute(GetDocumentQuery query) {
        return loadDocumentByIdPort.loadById(query.id())
                .orElseThrow(() -> new DocumentNotFoundException(query.id()));
    }
}

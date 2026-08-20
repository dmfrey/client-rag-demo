package com.example.clientragdemo.documents.application.domain.service;

import com.example.clientragdemo.documents.application.domain.model.Document;
import com.example.clientragdemo.documents.application.port.in.ListDocumentsUseCase;
import com.example.clientragdemo.documents.application.port.out.LoadAllDocumentsPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class ListDocumentsService implements ListDocumentsUseCase {

    private final LoadAllDocumentsPort loadAllDocumentsPort;

    ListDocumentsService(LoadAllDocumentsPort loadAllDocumentsPort) {
        this.loadAllDocumentsPort = loadAllDocumentsPort;
    }

    @Override
    public List<Document> execute() {
        return loadAllDocumentsPort.loadAll();
    }
}
